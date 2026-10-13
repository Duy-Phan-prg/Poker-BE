package com.msspoker.matchmakingservice.service.impl;

import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.enums.MatchmakingError;
import com.msspoker.matchmakingservice.exception.MatchmakingApiException;
import com.msspoker.matchmakingservice.mapper.QueueStatusMapper;
import com.msspoker.matchmakingservice.model.QueueEntry;
import com.msspoker.matchmakingservice.repository.MatchProposalPlayerRepository;
import com.msspoker.matchmakingservice.service.QueueStore;
import com.msspoker.matchmakingservice.validator.QueueModeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchmakingQueueServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-10-14T00:00:00Z");

    @Mock private QueueStore queueStore;
    @Mock private MatchProposalPlayerRepository players;
    private MatchmakingQueueServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MatchmakingQueueServiceImpl(
                queueStore,
                players,
                Clock.fixed(NOW, ZoneOffset.UTC),
                new QueueModeValidator(),
                Mappers.getMapper(QueueStatusMapper.class)
        );
    }

    @Test
    void joinsNormalQueueWithServerTime() {
        UUID accountId = UUID.randomUUID();
        when(queueStore.find(accountId)).thenReturn(Optional.empty());
        when(queueStore.enqueue(any())).thenReturn(true);
        when(queueStore.position(accountId, MatchMode.NORMAL)).thenReturn(Optional.of(1L));

        var response = service.join(accountId, MatchMode.NORMAL);

        assertThat(response.status()).isEqualTo("WAITING");
        assertThat(response.mode()).isEqualTo(MatchMode.NORMAL);
        assertThat(response.joinedAt()).isEqualTo(NOW);
        assertThat(response.position()).isEqualTo(1L);
        verify(queueStore).enqueue(new QueueEntry(accountId, MatchMode.NORMAL, NOW));
    }

    @Test
    void doesNotExposeRankBeforeItIsImplemented() {
        assertThatThrownBy(() -> service.join(UUID.randomUUID(), MatchMode.RANK))
                .isInstanceOfSatisfying(MatchmakingApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(MatchmakingError.MM_MODE_NOT_AVAILABLE));
        verifyNoInteractions(queueStore, players);
    }

    @Test
    void cannotCancelReservedPlayer() {
        UUID accountId = UUID.randomUUID();
        QueueEntry entry = new QueueEntry(accountId, MatchMode.NORMAL, NOW);
        when(queueStore.find(accountId)).thenReturn(Optional.of(entry));
        when(queueStore.cancel(accountId, MatchMode.NORMAL)).thenReturn(QueueStore.CancelResult.RESERVED);

        assertThatThrownBy(() -> service.cancel(accountId))
                .isInstanceOfSatisfying(MatchmakingApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(MatchmakingError.MM_MATCH_IN_PROGRESS));
    }
}
