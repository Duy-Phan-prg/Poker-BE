package com.msspoker.matchmakingservice.service.impl;

import com.msspoker.matchmakingservice.dto.response.QueueStatusResponse;
import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.enums.MatchmakingError;
import com.msspoker.matchmakingservice.exception.MatchmakingApiException;
import com.msspoker.matchmakingservice.mapper.QueueStatusMapper;
import com.msspoker.matchmakingservice.model.QueueEntry;
import com.msspoker.matchmakingservice.repository.MatchProposalPlayerRepository;
import com.msspoker.matchmakingservice.service.MatchmakingQueueService;
import com.msspoker.matchmakingservice.service.QueueStore;
import com.msspoker.matchmakingservice.validator.QueueModeValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class MatchmakingQueueServiceImpl implements MatchmakingQueueService {
    private final QueueStore queueStore;
    private final MatchProposalPlayerRepository players;
    private final Clock clock;
    private final QueueModeValidator modeValidator;
    private final QueueStatusMapper statusMapper;

    public MatchmakingQueueServiceImpl(
            QueueStore queueStore,
            MatchProposalPlayerRepository players,
            Clock clock,
            QueueModeValidator modeValidator,
            QueueStatusMapper statusMapper
    ) {
        this.queueStore = queueStore;
        this.players = players;
        this.clock = clock;
        this.modeValidator = modeValidator;
        this.statusMapper = statusMapper;
    }

    @Override
    public QueueStatusResponse join(UUID accountId, MatchMode mode) {
        modeValidator.requireAvailable(mode);

        Optional<QueueEntry> existing = queueStore.find(accountId);
        if (existing.isPresent()) {
            throw new MatchmakingApiException(
                    MatchmakingError.MM_ALREADY_QUEUED,
                    HttpStatus.CONFLICT,
                    "Tài khoản đã ở trong hàng chờ."
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
    public void cancel(UUID accountId) {
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
    }

    @Override
    public QueueStatusResponse status(UUID accountId) {
        Optional<QueueEntry> current = queueStore.find(accountId);
        if (current.isPresent()) {
            QueueEntry entry = current.get();
            Optional<UUID> reservation = queueStore.reservation(accountId);
            if (reservation.isPresent()) {
                return statusMapper.matching(entry, reservation.get());
            }
            return waiting(entry);
        }

        return players.findFirstByAccountIdOrderByCreatedAtDesc(accountId)
                .map(statusMapper::proposal)
                .orElseGet(QueueStatusResponse::none);
    }

    private QueueStatusResponse waiting(QueueEntry entry) {
        return statusMapper.waiting(
                entry,
                queueStore.position(entry.accountId(), entry.mode()).orElse(null)
        );
    }
}
