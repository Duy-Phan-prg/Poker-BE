package com.msspoker.matchmakingservice.dto.response;

import com.msspoker.matchmakingservice.enums.MatchMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Thông tin để client nối lại bàn Normal đang hoạt động")
public record NormalReconnectResponse(
        @Schema(description = "Có bàn đang hoạt động để nối lại hay không", example = "true") boolean reconnectable,
        @Schema(description = "Trạng thái lưu trong Redis", allowableValues = {"IN_GAME", "NONE"}, example = "IN_GAME") String status,
        @Schema(description = "Mode của API reconnect", example = "NORMAL") MatchMode mode,
        @Schema(description = "Proposal đã tạo bàn") UUID proposalId,
        @Schema(description = "ID bàn đang hoạt động") UUID tableId,
        @Schema(description = "ID trận đang hoạt động") UUID matchId,
        @Schema(description = "Host/IP public để client nối lại", example = "localhost") String host,
        @Schema(description = "Port public để client nối lại", example = "8082") Integer port,
        @Schema(description = "Đường dẫn WebSocket của Game", example = "/ws/game") String websocketPath
) {
    public static NormalReconnectResponse none() {
        return new NormalReconnectResponse(false, "NONE", MatchMode.NORMAL, null, null, null, null, null, null);
    }

    public static NormalReconnectResponse active(
            UUID proposalId,
            UUID tableId,
            UUID matchId,
            String host,
            int port,
            String websocketPath
    ) {
        return new NormalReconnectResponse(
                true, "IN_GAME", MatchMode.NORMAL, proposalId, tableId, matchId, host, port, websocketPath
        );
    }
}
