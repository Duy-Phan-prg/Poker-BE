package com.msspoker.matchmakingservice.model;

import com.msspoker.matchmakingservice.enums.MatchMode;

import java.time.Instant;
import java.util.UUID;

public record QueueEntry(UUID accountId, MatchMode mode, Instant joinedAt) {
}
