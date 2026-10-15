package com.msspoker.matchmakingservice.service;

import com.msspoker.matchmakingservice.dto.request.CreateTableRequest;
import com.msspoker.matchmakingservice.dto.response.TableCreatedResponse;

import java.util.Optional;
import java.util.UUID;

public interface GameClient {
    TableCreatedResponse createTable(CreateTableRequest request);

    Optional<TableCreatedResponse> activeTable(UUID accountId);
}
