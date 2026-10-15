package com.msspoker.matchmakingservice.service.impl.worker;

import com.msspoker.matchmakingservice.config.MatchmakingProperties;
import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.model.QueueEntry;
import com.msspoker.matchmakingservice.service.NormalMatchCoordinator;
import com.msspoker.matchmakingservice.service.QueueStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NormalMatchWorkerTest {
    private static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");

    private final QueueStore queueStore = mock(QueueStore.class);
    private final NormalMatchCoordinator coordinator = mock(NormalMatchCoordinator.class);
    private final MatchmakingProperties properties = new MatchmakingProperties();
    private final NormalMatchWorker worker = new NormalMatchWorker(
            queueStore,
            coordinator,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void resetsCountdownBelowMinimumPlayers() {
        lock();
        when(queueStore.oldest(MatchMode.NORMAL, 6)).thenReturn(List.of(entry(1)));

        worker.find();

        verify(queueStore).cancelCountdown(MatchMode.NORMAL);
        verify(queueStore, never()).reserve(any(), anyList(), any());
        verifyNoInteractions(coordinator);
    }

    @Test
    void startsTenSecondCountdownAtMinimumPlayers() {
        lock();
        List<QueueEntry> entries = List.of(entry(1), entry(2));
        when(queueStore.oldest(MatchMode.NORMAL, 6)).thenReturn(entries);
        when(queueStore.countdownDeadline(MatchMode.NORMAL)).thenReturn(Optional.empty());

        worker.find();

        verify(queueStore).startCountdown(MatchMode.NORMAL, NOW.plusSeconds(10));
        verify(queueStore, never()).reserve(any(), anyList(), any());
        verifyNoInteractions(coordinator);
    }

    @Test
    void waitsWhileCountdownHasNotExpired() {
        lock();
        when(queueStore.oldest(MatchMode.NORMAL, 6)).thenReturn(List.of(entry(1), entry(2), entry(3)));
        when(queueStore.countdownDeadline(MatchMode.NORMAL)).thenReturn(Optional.of(NOW.plusSeconds(1)));

        worker.find();

        verify(queueStore, never()).reserve(any(), anyList(), any());
        verifyNoInteractions(coordinator);
    }

    @Test
    void startsImmediatelyWhenSixPlayersAreWaiting() {
        lock();
        List<QueueEntry> entries = List.of(entry(1), entry(2), entry(3), entry(4), entry(5), entry(6));
        when(queueStore.oldest(MatchMode.NORMAL, 6)).thenReturn(entries);
        when(queueStore.reserve(eq(MatchMode.NORMAL), eq(entries), any(UUID.class))).thenReturn(true);

        worker.find();

        verify(queueStore).cancelCountdown(MatchMode.NORMAL);
        verify(coordinator).start(any(UUID.class), eq(entries));
    }

    @Test
    void startsWithCurrentPlayersWhenCountdownExpires() {
        lock();
        List<QueueEntry> entries = List.of(entry(1), entry(2), entry(3));
        when(queueStore.oldest(MatchMode.NORMAL, 6)).thenReturn(entries);
        when(queueStore.countdownDeadline(MatchMode.NORMAL)).thenReturn(Optional.of(NOW));
        when(queueStore.reserve(eq(MatchMode.NORMAL), eq(entries), any(UUID.class))).thenReturn(true);

        worker.find();

        verify(queueStore).cancelCountdown(MatchMode.NORMAL);
        verify(coordinator).start(any(UUID.class), eq(entries));
    }

    private void lock() {
        when(queueStore.acquireFinderLock(MatchMode.NORMAL)).thenReturn(Optional.of("lock"));
    }

    private QueueEntry entry(int seconds) {
        return new QueueEntry(UUID.randomUUID(), MatchMode.NORMAL, Instant.EPOCH.plusSeconds(seconds));
    }
}
