package com.msspoker.matchmakingservice.service;

import com.msspoker.matchmakingservice.dto.request.CreateTableRequest;
import com.msspoker.matchmakingservice.dto.response.TableCreatedResponse;

public interface GameClient {
    TableCreatedResponse createTable(CreateTableRequest request);
}
