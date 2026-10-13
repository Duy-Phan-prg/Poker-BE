package com.msspoker.matchmakingservice.service.impl;

import com.msspoker.matchmakingservice.dto.response.TableCreatedResponse;
import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.exception.GameCallRejectedException;
import com.msspoker.matchmakingservice.exception.GameCallUnknownException;
import com.msspoker.matchmakingservice.model.QueueEntry;
import com.msspoker.matchmakingservice.service.GameClient;
import com.msspoker.matchmakingservice.service.ProposalPersistenceService;
import com.msspoker.matchmakingservice.service.QueueStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NormalMatchCoordinatorImplTest {
    private final ProposalPersistenceService persistence = mock(ProposalPersistenceService.class);
    private final GameClient game = mock(GameClient.class);
    private final QueueStore queueStore = mock(QueueStore.class);
    private final NormalMatchCoordinatorImpl coordinator = new NormalMatchCoordinatorImpl(persistence, game, queueStore);

    @Test
    void marksReadyAndRemovesQueueStateWhenGameCreatesTable() {
        UUID proposalId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        List<QueueEntry> entries = entries();
        when(game.createTable(any())).thenReturn(new TableCreatedResponse(tableId, matchId));

        coordinator.start(proposalId, entries);

        verify(persistence).createNormal(proposalId, entries);
        verify(persistence).ready(proposalId, tableId, matchId);
        verify(queueStore).complete(entries);
        verify(queueStore, never()).requeue(anyList());
    }

    @Test
    void requeuesWhenGameDefinitelyRejectsRequest() {
        UUID proposalId = UUID.randomUUID();
        List<QueueEntry> entries = entries();
        when(game.createTable(any())).thenThrow(new GameCallRejectedException("bad request", null));

        coordinator.start(proposalId, entries);

        verify(persistence).failed(proposalId, "GAME_REJECTED");
        verify(queueStore).requeue(entries);
    }

    @Test
    void keepsReservationWhenGameOutcomeIsUnknown() {
        UUID proposalId = UUID.randomUUID();
        List<QueueEntry> entries = entries();
        when(game.createTable(any())).thenThrow(new GameCallUnknownException("timeout", null));

        coordinator.start(proposalId, entries);

        verify(persistence, never()).failed(any(), any());
        verify(queueStore, never()).requeue(anyList());
        verify(queueStore, never()).complete(anyList());
    }

    private List<QueueEntry> entries() {
        return List.of(
                new QueueEntry(UUID.randomUUID(), MatchMode.NORMAL, Instant.EPOCH),
                new QueueEntry(UUID.randomUUID(), MatchMode.NORMAL, Instant.EPOCH.plusSeconds(1))
        );
    }
}
