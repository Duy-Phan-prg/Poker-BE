package com.msspoker.rankingservice.exception;

import com.msspoker.rankingservice.enums.RankingError;

import org.springframework.http.HttpStatus;

public final class RankingExceptions {
    private RankingExceptions() {
    }

    public static RankingApiException notFound() {
        return new RankingApiException(RankingError.RANK_NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên.");
    }

    public static RankingApiException invalidRequest() {
        return new RankingApiException(RankingError.RANK_INVALID_REQUEST, HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ. Vui lòng kiểm tra thông tin gửi lên.");
    }

    public static RankingApiException internalError() {
        return new RankingApiException(RankingError.RANK_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xử lý yêu cầu lúc này. Vui lòng thử lại.");
    }
}
