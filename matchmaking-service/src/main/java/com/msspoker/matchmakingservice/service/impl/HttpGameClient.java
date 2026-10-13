package com.msspoker.matchmakingservice.service.impl;

import com.msspoker.matchmakingservice.config.MatchmakingProperties;
import com.msspoker.matchmakingservice.dto.request.CreateTableRequest;
import com.msspoker.matchmakingservice.dto.response.TableCreatedResponse;
import com.msspoker.matchmakingservice.exception.GameCallRejectedException;
import com.msspoker.matchmakingservice.exception.GameCallUnknownException;
import com.msspoker.matchmakingservice.service.GameClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "matchmaking.game.stub-enabled", havingValue = "false", matchIfMissing = true)
public class HttpGameClient implements GameClient {
    private final RestClient game;

    public HttpGameClient(
            @Value("${matchmaking.game-url:http://localhost:8082}") String gameUrl,
            MatchmakingProperties properties
    ) {
        Duration timeout = properties.getGame().getTimeout();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build()
        );
        factory.setReadTimeout(timeout);
        this.game = RestClient.builder().baseUrl(gameUrl).requestFactory(factory).build();
    }

    @Override
    public TableCreatedResponse createTable(CreateTableRequest request) {
        try {
            TableCreatedResponse response = game.post()
                    .uri("/internal/tables")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TableCreatedResponse.class);
            if (response == null || response.tableId() == null || response.matchId() == null) {
                throw new GameCallUnknownException("Game trả response tạo bàn không hợp lệ", null);
            }
            return response;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new GameCallRejectedException("Game từ chối yêu cầu tạo bàn", exception);
            }
            throw new GameCallUnknownException("Chưa xác định Game đã tạo bàn hay chưa", exception);
        } catch (GameCallUnknownException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new GameCallUnknownException("Không kết nối được game-service", exception);
        }
    }
}
