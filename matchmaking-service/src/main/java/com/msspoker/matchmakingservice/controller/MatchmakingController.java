package com.msspoker.matchmakingservice.controller;

import com.msspoker.matchmakingservice.constant.MatchmakingConstants;
import com.msspoker.matchmakingservice.dto.request.JoinQueueRequest;
import com.msspoker.matchmakingservice.dto.response.QueueStatusResponse;
import com.msspoker.matchmakingservice.service.MatchmakingQueueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {
    private final MatchmakingQueueService queues;

    public MatchmakingController(MatchmakingQueueService queues) {
        this.queues = queues;
    }

    @PostMapping("/queue")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueStatusResponse join(
            @RequestHeader(MatchmakingConstants.USER_ID_HEADER) UUID accountId,
            @Valid @RequestBody JoinQueueRequest request
    ) {
        return queues.join(accountId, request.mode());
    }

    @DeleteMapping("/queue")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@RequestHeader(MatchmakingConstants.USER_ID_HEADER) UUID accountId) {
        queues.cancel(accountId);
    }

    @GetMapping("/status")
    public QueueStatusResponse status(@RequestHeader(MatchmakingConstants.USER_ID_HEADER) UUID accountId) {
        return queues.status(accountId);
    }
}
