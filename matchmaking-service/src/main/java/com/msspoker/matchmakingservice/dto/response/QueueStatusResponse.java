package com.msspoker.matchmakingservice.dto.response;

import com.msspoker.matchmakingservice.enums.MatchMode;

import java.time.Instant;
import java.util.UUID;

public record QueueStatusResponse(
        String status,
        MatchMode mode,
        Instant joinedAt,
        Long position,
        UUID proposalId,
        UUID tableId,
        UUID matchId
) {
    public static QueueStatusResponse none() {
        return new QueueStatusResponse("NONE", null, null, null, null, null, null);
    }
}
