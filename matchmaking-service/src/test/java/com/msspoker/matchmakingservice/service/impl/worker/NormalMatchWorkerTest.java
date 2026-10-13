package com.msspoker.matchmakingservice.service.impl.worker;

import com.msspoker.matchmakingservice.config.MatchmakingProperties;
import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.model.QueueEntry;
import com.msspoker.matchmakingservice.service.NormalMatchCoordinator;
import com.msspoker.matchmakingservice.service.QueueStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NormalMatchWorkerTest {
    private final QueueStore queueStore = mock(QueueStore.class);
    private final NormalMatchCoordinator coordinator = mock(NormalMatchCoordinator.class);
    private final MatchmakingProperties properties = new MatchmakingProperties();
    private final NormalMatchWorker worker = new NormalMatchWorker(queueStore, coordinator, properties);

    @Test
    void reservesOldestPlayersAndStartsMatch() {
        List<QueueEntry> entries = List.of(entry(1), entry(2));
        when(queueStore.acquireFinderLock(MatchMode.NORMAL)).thenReturn(Optional.of("lock"));
        when(queueStore.oldest(MatchMode.NORMAL, 2)).thenReturn(entries);
        when(queueStore.reserve(eq(MatchMode.NORMAL), eq(entries), any(UUID.class))).thenReturn(true);

        worker.find();

        verify(coordinator).start(any(UUID.class), eq(entries));
        verify(queueStore).releaseFinderLock(MatchMode.NORMAL, "lock");
    }

    @Test
    void doesNothingUntilMinimumPlayersAreWaiting() {
        when(queueStore.acquireFinderLock(MatchMode.NORMAL)).thenReturn(Optional.of("lock"));
        when(queueStore.oldest(MatchMode.NORMAL, 2)).thenReturn(List.of(entry(1)));

        worker.find();

        verify(queueStore, never()).reserve(any(), anyList(), any());
        verifyNoInteractions(coordinator);
        verify(queueStore).releaseFinderLock(MatchMode.NORMAL, "lock");
    }

    private QueueEntry entry(int seconds) {
        return new QueueEntry(UUID.randomUUID(), MatchMode.NORMAL, Instant.EPOCH.plusSeconds(seconds));
    }
}
