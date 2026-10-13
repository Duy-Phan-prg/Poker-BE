package com.msspoker.matchmakingservice.service;

import com.msspoker.matchmakingservice.dto.response.QueueStatusResponse;
import com.msspoker.matchmakingservice.enums.MatchMode;

import java.util.UUID;

public interface MatchmakingQueueService {
    QueueStatusResponse join(UUID accountId, MatchMode mode);
    void cancel(UUID accountId);
    QueueStatusResponse status(UUID accountId);
}
