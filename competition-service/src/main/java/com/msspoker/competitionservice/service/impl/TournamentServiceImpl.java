package com.msspoker.competitionservice.service.impl;

import com.msspoker.competitionservice.config.CompetitionProperties;
import com.msspoker.competitionservice.constant.CompetitionConstants;
import com.msspoker.competitionservice.dto.event.MatchFinishedEvent;
import com.msspoker.competitionservice.dto.event.PlacementEvent;
import com.msspoker.competitionservice.dto.request.CreateTournamentRequest;
import com.msspoker.competitionservice.dto.request.WalletTransactionRequest;
import com.msspoker.competitionservice.dto.response.PageResponse;
import com.msspoker.competitionservice.dto.response.TournamentResponse;
import com.msspoker.competitionservice.dto.response.TournamentSummaryResponse;
import com.msspoker.competitionservice.dto.response.TournamentTableResponse;
import com.msspoker.competitionservice.entity.TournamentEntity;
import com.msspoker.competitionservice.entity.TournamentEntryEntity;
import com.msspoker.competitionservice.entity.TournamentSeatEntity;
import com.msspoker.competitionservice.entity.TournamentTableEntity;
import com.msspoker.competitionservice.enums.EntryStatus;
import com.msspoker.competitionservice.enums.GameMode;
import com.msspoker.competitionservice.enums.TableStatus;
import com.msspoker.competitionservice.enums.TournamentRound;
import com.msspoker.competitionservice.enums.TournamentStatus;
import com.msspoker.competitionservice.enums.TournamentType;
import com.msspoker.competitionservice.exception.CompetitionExceptions;
import com.msspoker.competitionservice.mapper.TournamentMapper;
import com.msspoker.competitionservice.repository.TournamentEntryRepository;
import com.msspoker.competitionservice.repository.TournamentRepository;
import com.msspoker.competitionservice.repository.TournamentSeatRepository;
import com.msspoker.competitionservice.repository.TournamentTableRepository;
import com.msspoker.competitionservice.repository.WalletCreditRepository;
import com.msspoker.competitionservice.service.TournamentService;
import com.msspoker.competitionservice.service.TournamentTableLauncher;
import com.msspoker.competitionservice.service.WalletClient;
import com.msspoker.competitionservice.validator.TournamentResultValidator;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {
    private static final Sort UPCOMING_FIRST = Sort.by(Sort.Order.asc("startAt"), Sort.Order.asc("id"));
    private static final Set<EntryStatus> SEATED = EnumSet.of(EntryStatus.REGISTERED, EntryStatus.CONFIRMED);

    private final TournamentRepository tournaments;
    private final TournamentEntryRepository entries;
    private final TournamentTableRepository tables;
    private final TournamentSeatRepository seats;
    private final WalletCreditRepository credits;
    private final TournamentMapper mapper;
    private final TournamentResultValidator results;
    private final WalletClient wallet;
    private final TournamentTableLauncher launcher;
    private final CompetitionProperties properties;
    private final TransactionTemplate transactions;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    @Override
    public TournamentResponse create(UUID adminId, CreateTournamentRequest request) {
        CompetitionProperties.Tournament defaults = properties.getTournament();
        int slots = request.slots() == null ? defaults.getSlots() : request.slots();
        if (slots < 2 || slots > defaults.maxSlots()) throw CompetitionExceptions.invalidTournamentSettings(defaults.maxSlots());
        return view(tournaments.save(mapper.newTournament(request, slots, adminId, Instant.now())));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<TournamentSummaryResponse> list(int page, int size) {
        Page<TournamentEntity> result = tournaments.findByIsDeletedFalse(PageRequest.of(page, size, UPCOMING_FIRST));
        List<TournamentSummaryResponse> content = result.getContent().stream()
                .map(t -> mapper.summary(t, (int) entries.countByTournamentId(t.getId()))).toList();
        return new PageResponse<>(content, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    @Override
    public TournamentResponse get(UUID tournamentId) {
        return view(tournaments.findByIdAndIsDeletedFalse(tournamentId).orElseThrow(CompetitionExceptions::tournamentNotFound));
    }

    /**
     * The fee is charged under the tournament lock before the entry is stored. If storing fails, a
     * repeated registration reuses the idempotency key, so the wallet does not charge twice.
     */
    @Transactional
    @Override
    public TournamentResponse register(UUID tournamentId, UUID accountId) {
        TournamentEntity tournament = lock(tournamentId);
        if (entries.findByTournamentIdAndAccountId(tournamentId, accountId).isPresent()) return view(tournament);
        if (tournament.getType() == TournamentType.ELITE) throw CompetitionExceptions.inviteOnly();
        if (tournament.getStatus() != TournamentStatus.SCHEDULED || !Instant.now().isBefore(tournament.getStartAt())) {
            throw CompetitionExceptions.registrationClosed();
        }
        if (entries.countByTournamentId(tournamentId) >= tournament.getSlots()) throw CompetitionExceptions.tournamentFull();
        if (tournament.getEntryFee() > 0) {
            wallet.debit(new WalletTransactionRequest(accountId, tournament.getEntryFee(),
                    CompetitionConstants.TOURNAMENT_ENTRY_FEE, tournamentId.toString(),
                    String.format(CompetitionConstants.TOURNAMENT_FEE_KEY_FORMAT, tournamentId, accountId)));
        }
        entries.save(mapper.newEntry(tournamentId, accountId, EntryStatus.REGISTERED, Instant.now()));
        return view(tournament);
    }

    /** Seats are committed first; tables then open through the launcher, which retries on failure. */
    @Override
    public TournamentResponse start(UUID tournamentId) {
        transactions.executeWithoutResult(status -> seatPlayers(tournamentId));
        launcher.launchPending();
        return get(tournamentId);
    }

    @Transactional
    @Override
    public TournamentResponse cancel(UUID tournamentId) {
        TournamentEntity tournament = lock(tournamentId);
        if (tournament.getStatus() != TournamentStatus.SCHEDULED) throw CompetitionExceptions.tournamentNotScheduled();
        tournament.setStatus(TournamentStatus.CANCELLED);
        if (tournament.getEntryFee() > 0) {
            Instant now = Instant.now();
            for (TournamentEntryEntity entry : entries.findByTournamentIdOrderByRegisteredAtAscIdAsc(tournamentId)) {
                credits.save(mapper.newCredit(entry.getAccountId(), tournament.getEntryFee(),
                        CompetitionConstants.TOURNAMENT_REFUND, tournamentId.toString(),
                        String.format(CompetitionConstants.TOURNAMENT_REFUND_KEY_FORMAT, tournamentId, entry.getAccountId()), now));
            }
        }
        return view(tournament);
    }

    /** Idempotent: a table records its result once; the last qualifier result seats the final. */
    @Transactional
    @Override
    public void matchFinished(MatchFinishedEvent event) {
        if (event == null || event.mode() == null || event.sourceId() == null) throw CompetitionExceptions.invalidEvent();
        if (event.mode() != GameMode.TOURNAMENT) return;
        TournamentTableEntity table = tables.lockById(event.sourceId()).orElse(null);
        if (table == null || table.getStatus() == TableStatus.FINISHED) return;
        TournamentEntity tournament = lock(table.getTournamentId());
        List<TournamentSeatEntity> roster = seats.findByTournamentTableIdOrderBySeatIndex(table.getId());
        results.validate(event, roster);
        Map<UUID, Integer> places = event.placements().stream()
                .collect(Collectors.toMap(PlacementEvent::accountId, PlacementEvent::place));
        roster.forEach(seat -> seat.setPlace(places.get(seat.getAccountId())));
        table.setStatus(TableStatus.FINISHED);
        if (table.getTableId() == null) table.setTableId(event.tableId());
        table.setMatchId(event.matchId());
        Map<UUID, TournamentEntryEntity> players = entries.findByTournamentIdAndAccountIdIn(tournament.getId(),
                places.keySet()).stream().collect(Collectors.toMap(TournamentEntryEntity::getAccountId, Function.identity()));

        if (table.getRound() == TournamentRound.FINAL) {
            roster.forEach(seat -> players.get(seat.getAccountId()).setFinalPlace(seat.getPlace()));
            tournament.setStatus(TournamentStatus.FINISHED);
            return;
        }
        int advance = properties.getTournament().getAdvancePerTable();
        roster.stream().filter(seat -> seat.getPlace() > advance)
                .forEach(seat -> players.get(seat.getAccountId()).setStatus(EntryStatus.ELIMINATED));
        List<TournamentTableEntity> qualifiers = tables.findByTournamentIdAndRoundOrderByTableIndexAsc(
                tournament.getId(), TournamentRound.QUALIFIER);
        if (qualifiers.stream().allMatch(q -> q.getStatus() == TableStatus.FINISHED)) {
            Map<UUID, List<TournamentSeatEntity>> qualifierSeats = seats.findByTournamentTableIdIn(
                    qualifiers.stream().map(TournamentTableEntity::getId).toList()).stream()
                    .collect(Collectors.groupingBy(TournamentSeatEntity::getTournamentTableId));
            // Final seats: table 1's winner and runner-up, then table 2's, and so on.
            List<UUID> finalists = qualifiers.stream().flatMap(q -> qualifierSeats.get(q.getId()).stream()
                            .filter(seat -> seat.getPlace() <= advance)
                            .sorted(Comparator.comparing(TournamentSeatEntity::getPlace)))
                    .map(TournamentSeatEntity::getAccountId).toList();
            createTable(tournament.getId(), TournamentRound.FINAL, 1, finalists, Instant.now());
            tournament.setStatus(TournamentStatus.FINAL);
        }
    }

    /** Spreads shuffled players over ceil(n / tableSize) tables; one table means a direct final. */
    private void seatPlayers(UUID tournamentId) {
        TournamentEntity tournament = lock(tournamentId);
        if (tournament.getStatus() != TournamentStatus.SCHEDULED) throw CompetitionExceptions.tournamentNotScheduled();
        List<UUID> players = new ArrayList<>(entries.findByTournamentIdOrderByRegisteredAtAscIdAsc(tournamentId).stream()
                .filter(entry -> SEATED.contains(entry.getStatus())).map(TournamentEntryEntity::getAccountId).toList());
        CompetitionProperties.Tournament defaults = properties.getTournament();
        if (players.size() < defaults.getMinPlayers()) throw CompetitionExceptions.notEnoughPlayers(defaults.getMinPlayers());
        Collections.shuffle(players, random);
        int tableCount = Math.ceilDiv(players.size(), defaults.getTableSize());
        TournamentRound round = tableCount == 1 ? TournamentRound.FINAL : TournamentRound.QUALIFIER;
        List<List<UUID>> groups = new ArrayList<>();
        for (int i = 0; i < tableCount; i++) groups.add(new ArrayList<>());
        for (int i = 0; i < players.size(); i++) groups.get(i % tableCount).add(players.get(i));
        Instant now = Instant.now();
        for (int i = 0; i < tableCount; i++) createTable(tournamentId, round, i + 1, groups.get(i), now);
        tournament.setStatus(round == TournamentRound.FINAL ? TournamentStatus.FINAL : TournamentStatus.QUALIFYING);
    }

    private void createTable(UUID tournamentId, TournamentRound round, int tableIndex, List<UUID> players, Instant now) {
        TournamentTableEntity table = tables.save(mapper.newTable(tournamentId, round, tableIndex, now));
        for (int seat = 0; seat < players.size(); seat++) {
            seats.save(mapper.newSeat(table.getId(), players.get(seat), seat));
        }
    }

    private TournamentEntity lock(UUID tournamentId) {
        return tournaments.lockById(tournamentId).orElseThrow(CompetitionExceptions::tournamentNotFound);
    }

    private TournamentResponse view(TournamentEntity tournament) {
        List<TournamentTableEntity> bracket = tables.findByTournamentIdOrderByRoundAscTableIndexAsc(tournament.getId());
        Map<UUID, List<TournamentSeatEntity>> seatsByTable = bracket.isEmpty() ? Map.of()
                : seats.findByTournamentTableIdIn(bracket.stream().map(TournamentTableEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(TournamentSeatEntity::getTournamentTableId));
        List<TournamentTableResponse> tableViews = bracket.stream().map(table -> mapper.table(table,
                seatsByTable.getOrDefault(table.getId(), List.of()).stream()
                        .sorted(Comparator.comparingInt(TournamentSeatEntity::getSeatIndex)).map(mapper::seat).toList()))
                .toList();
        return mapper.tournament(tournament, entries.findByTournamentIdOrderByRegisteredAtAscIdAsc(tournament.getId())
                .stream().map(mapper::entry).toList(), tableViews);
    }
}
