package com.msspoker.rankingservice.exception;

import com.msspoker.rankingservice.enums.RankingError;

import org.springframework.http.HttpStatus;

public final class RankingExceptions {
    private RankingExceptions() {
    }

    public static IllegalArgumentException invalidEvent() {
        return new IllegalArgumentException("Event không hợp lệ.");
    }

    public static IllegalArgumentException invalidMatchResult() {
        return new IllegalArgumentException("Kết quả trận không hợp lệ: thứ hạng phải duy nhất từ 1 đến số người chơi.");
    }

    public static IllegalArgumentException invalidPlace() {
        return new IllegalArgumentException("Thứ hạng phải bắt đầu từ 1.");
    }

    public static RankingApiException noActiveSeason() {
        return new RankingApiException(RankingError.RANK_NOT_FOUND, HttpStatus.NOT_FOUND, "Chưa có mùa giải đang diễn ra.");
    }

    public static RankingApiException notFound() {
        return new RankingApiException(RankingError.RANK_NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên.");
    }

    public static RankingApiException unauthorized() {
        return new RankingApiException(RankingError.RANK_UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
    }

    public static RankingApiException invalidRequest() {
        return new RankingApiException(RankingError.RANK_INVALID_REQUEST, HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ. Vui lòng kiểm tra thông tin gửi lên.");
    }

    public static RankingApiException internalError() {
        return new RankingApiException(RankingError.RANK_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xử lý yêu cầu lúc này. Vui lòng thử lại.");
    }
}
