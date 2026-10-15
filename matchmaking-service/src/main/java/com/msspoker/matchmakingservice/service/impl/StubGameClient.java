package com.msspoker.matchmakingservice.service.impl;

import com.msspoker.matchmakingservice.dto.request.CreateTableRequest;
import com.msspoker.matchmakingservice.dto.response.TableCreatedResponse;
import com.msspoker.matchmakingservice.service.GameClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "matchmaking.game.stub-enabled", havingValue = "true")
public class StubGameClient implements GameClient {
    @Override
    public TableCreatedResponse createTable(CreateTableRequest request) {
        return new TableCreatedResponse(UUID.randomUUID(), UUID.randomUUID());
    }

    @Override
    public Optional<TableCreatedResponse> activeTable(UUID accountId) {
        return Optional.empty();
    }
}
