package com.msspoker.matchmakingservice.service;

import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.model.QueueEntry;
import com.msspoker.matchmakingservice.model.InGameSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueueStore {
    boolean enqueue(QueueEntry entry);

    CancelResult cancel(UUID accountId, MatchMode mode);

    Optional<QueueEntry> find(UUID accountId);

    Optional<Long> position(UUID accountId, MatchMode mode);

    Optional<UUID> reservation(UUID accountId);

    List<QueueEntry> oldest(MatchMode mode, int limit);

    long size(MatchMode mode);

    Optional<Instant> countdownDeadline(MatchMode mode);

    Instant startCountdown(MatchMode mode, Instant deadline);

    void cancelCountdown(MatchMode mode);

    boolean reserve(MatchMode mode, List<QueueEntry> entries, UUID reservationId);

    void complete(List<QueueEntry> entries, InGameSession session);

    Optional<InGameSession> inGame(UUID accountId);

    void clearInGame(UUID accountId);

    void requeue(List<QueueEntry> entries);

    Optional<String> acquireFinderLock(MatchMode mode);

    void releaseFinderLock(MatchMode mode, String token);

    enum CancelResult {
        CANCELLED,
        NOT_FOUND,
        RESERVED
    }
}
