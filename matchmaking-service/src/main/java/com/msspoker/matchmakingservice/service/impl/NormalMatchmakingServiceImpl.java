package com.msspoker.matchmakingservice.service.impl;

import com.msspoker.matchmakingservice.config.MatchmakingProperties;
import com.msspoker.matchmakingservice.dto.response.QueueStatusResponse;
import com.msspoker.matchmakingservice.dto.response.NormalReconnectResponse;
import com.msspoker.matchmakingservice.dto.response.TableCreatedResponse;
import com.msspoker.matchmakingservice.entity.MatchProposalPlayerEntity;
import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.enums.MatchmakingError;
import com.msspoker.matchmakingservice.enums.ProposalStatus;
import com.msspoker.matchmakingservice.exception.MatchmakingApiException;
import com.msspoker.matchmakingservice.mapper.QueueStatusMapper;
import com.msspoker.matchmakingservice.model.QueueEntry;
import com.msspoker.matchmakingservice.model.InGameSession;
import com.msspoker.matchmakingservice.repository.MatchProposalPlayerRepository;
import com.msspoker.matchmakingservice.service.NormalMatchmakingService;
import com.msspoker.matchmakingservice.service.GameClient;
import com.msspoker.matchmakingservice.service.QueueStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class NormalMatchmakingServiceImpl implements NormalMatchmakingService {
    private final QueueStore queueStore;
    private final MatchProposalPlayerRepository players;
    private final Clock clock;
    private final QueueStatusMapper statusMapper;
    private final MatchmakingProperties properties;
    private final GameClient game;

    public NormalMatchmakingServiceImpl(
            QueueStore queueStore,
            MatchProposalPlayerRepository players,
            Clock clock,
            QueueStatusMapper statusMapper,
            MatchmakingProperties properties,
            GameClient game
    ) {
        this.queueStore = queueStore;
        this.players = players;
        this.clock = clock;
        this.statusMapper = statusMapper;
        this.properties = properties;
        this.game = game;
    }

    @Override
    public QueueStatusResponse join(UUID accountId) {
        MatchMode mode = MatchMode.NORMAL;
        Optional<QueueEntry> existing = queueStore.find(accountId);
        if (existing.isPresent()) {
            throw new MatchmakingApiException(
                    MatchmakingError.MM_ALREADY_QUEUED,
                    HttpStatus.CONFLICT,
                    "Tài khoản đã ở trong hàng chờ."
            );
        }

        if (players.existsActiveProposal(accountId) || game.activeTable(accountId).isPresent()) {
            throw new MatchmakingApiException(
                    MatchmakingError.MM_MATCH_IN_PROGRESS,
                    HttpStatus.CONFLICT,
                    "Tài khoản đang trong quá trình tạo bàn hoặc đang ở một trận khác."
            );
        }

        Instant joinedAt = clock.instant();
        QueueEntry entry = new QueueEntry(accountId, mode, joinedAt);
        if (!queueStore.enqueue(entry)) {
            throw new MatchmakingApiException(
                    MatchmakingError.MM_ALREADY_QUEUED,
                    HttpStatus.CONFLICT,
                    "Tài khoản đã ở trong hàng chờ."
            );
        }
        return waiting(entry);
    }

    @Override
    public void leave(UUID accountId) {
        if (players.existsActiveProposal(accountId)) {
            throw new MatchmakingApiException(
                    MatchmakingError.MM_MATCH_IN_PROGRESS,
                    HttpStatus.CONFLICT,
                    "Hệ thống đang tạo bàn cho trận này, không thể rời hàng chờ."
            );
        }

        QueueEntry entry = queueStore.find(accountId).orElseThrow(() -> new MatchmakingApiException(
                MatchmakingError.MM_NOT_QUEUED,
                HttpStatus.NOT_FOUND,
                "Tài khoản không ở trong hàng chờ."
        ));

        QueueStore.CancelResult result = queueStore.cancel(accountId, entry.mode());
        if (result == QueueStore.CancelResult.RESERVED) {
            throw new MatchmakingApiException(
                    MatchmakingError.MM_MATCH_IN_PROGRESS,
                    HttpStatus.CONFLICT,
                    "Hệ thống đang tạo bàn cho trận này, không thể hủy hàng chờ."
            );
        }
        if (result == QueueStore.CancelResult.NOT_FOUND) {
            throw new MatchmakingApiException(
                    MatchmakingError.MM_NOT_QUEUED,
                    HttpStatus.NOT_FOUND,
                    "Tài khoản không ở trong hàng chờ."
            );
        }
        if (queueStore.size(entry.mode()) < properties.getNormal().getMinPlayers()) {
            queueStore.cancelCountdown(entry.mode());
        }
    }

    @Override
    public QueueStatusResponse status(UUID accountId) {
        Optional<MatchProposalPlayerEntity> activeProposal =
                players.findFirstByAccountIdAndIsDeletedFalseAndProposal_StatusAndProposal_IsDeletedFalseOrderByCreatedAtDesc(
                        accountId,
                        ProposalStatus.CREATING_TABLE
                );
        if (activeProposal.isPresent()) {
            return statusMapper.proposal(activeProposal.get());
        }

        Optional<InGameSession> inGame = queueStore.inGame(accountId);
        if (inGame.isPresent()) {
            InGameSession session = inGame.get();
            Optional<TableCreatedResponse> activeTable = game.activeTable(accountId);
            if (activeTable.isPresent()
                    && session.tableId().equals(activeTable.get().tableId())
                    && session.matchId().equals(activeTable.get().matchId())) {
                return statusMapper.inGame(session);
            }
            queueStore.clearInGame(accountId);
        }

        Optional<QueueEntry> current = queueStore.find(accountId);
        if (current.isPresent()) {
            QueueEntry entry = current.get();
            Optional<UUID> reservation = queueStore.reservation(accountId);
            if (reservation.isPresent()) {
                return statusMapper.matching(entry, reservation.get());
            }
            return waiting(entry);
        }

        return QueueStatusResponse.none();
    }

    @Override
    public NormalReconnectResponse reconnect(UUID accountId) {
        Optional<InGameSession> storedSession = queueStore.inGame(accountId);
        if (storedSession.isEmpty()) {
            return NormalReconnectResponse.none();
        }

        InGameSession session = storedSession.get();
        Optional<TableCreatedResponse> activeTable = game.activeTable(accountId);
        if (activeTable.isEmpty()
                || !session.tableId().equals(activeTable.get().tableId())
                || !session.matchId().equals(activeTable.get().matchId())) {
            queueStore.clearInGame(accountId);
            return NormalReconnectResponse.none();
        }

        return NormalReconnectResponse.active(
                session.proposalId(),
                session.tableId(),
                session.matchId(),
                session.host(),
                session.port(),
                session.websocketPath()
        );
    }

    private QueueStatusResponse waiting(QueueEntry entry) {
        Optional<Instant> deadline = queueStore.countdownDeadline(entry.mode());
        return statusMapper.waiting(
                entry,
                deadline.isPresent() ? "COUNTDOWN" : "WAITING",
                queueStore.position(entry.accountId(), entry.mode()).orElse(null),
                queueStore.size(entry.mode()),
                properties.getNormal().getMinPlayers(),
                properties.getNormal().getMaxPlayers(),
                deadline.orElse(null)
        );
    }
}
