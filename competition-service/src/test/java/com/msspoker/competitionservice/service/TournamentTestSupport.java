package com.msspoker.competitionservice.service;

import com.msspoker.competitionservice.dto.event.MatchFinishedEvent;
import com.msspoker.competitionservice.dto.event.PlacementEvent;
import com.msspoker.competitionservice.dto.request.CreateTournamentRequest;
import com.msspoker.competitionservice.entity.TournamentSeatEntity;
import com.msspoker.competitionservice.entity.TournamentTableEntity;
import com.msspoker.competitionservice.enums.GameMode;
import com.msspoker.competitionservice.enums.TournamentType;
import com.msspoker.competitionservice.repository.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/** Shared fixtures for tournament tests that run against the test H2 database. */
public final class TournamentTestSupport {
    private TournamentTestSupport() {
    }

    public static void clean(TournamentSeatRepository seats, TournamentTableRepository tables,
                             TournamentEntryRepository entries, TournamentRepository tournaments,
                             WalletCreditRepository credits, OutboxRepository outbox) {
        seats.deleteAll();
        tables.deleteAll();
        entries.deleteAll();
        tournaments.deleteAll();
        credits.deleteAll();
        outbox.deleteAll();
    }

    public static CreateTournamentRequest open(long entryFee, Integer slots) {
        return new CreateTournamentRequest("Giải Mở tuần", TournamentType.OPEN,
                Instant.now().plus(1, ChronoUnit.HOURS), entryFee, slots, null, null);
    }

    /** Seat order is the finishing order: seat 0 wins. */
    public static MatchFinishedEvent finished(TournamentTableEntity table, List<TournamentSeatEntity> seats) {
        List<PlacementEvent> placements = IntStream.range(0, seats.size())
                .mapToObj(i -> new PlacementEvent(seats.get(i).getAccountId(), i + 1, i == 0 ? 8000 : 0)).toList();
        return new MatchFinishedEvent(UUID.randomUUID(), table.getTableId(), GameMode.TOURNAMENT, table.getId(), 0,
                placements, Instant.now().minusSeconds(600), Instant.now());
    }
}
