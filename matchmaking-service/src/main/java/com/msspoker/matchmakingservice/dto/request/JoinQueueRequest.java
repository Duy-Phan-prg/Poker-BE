package com.msspoker.matchmakingservice.dto.request;

import com.msspoker.matchmakingservice.enums.MatchMode;
import jakarta.validation.constraints.NotNull;

public record JoinQueueRequest(@NotNull MatchMode mode) {
}
