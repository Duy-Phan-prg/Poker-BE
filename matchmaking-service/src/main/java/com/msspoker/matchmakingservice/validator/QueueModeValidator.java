package com.msspoker.matchmakingservice.validator;

import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.enums.MatchmakingError;
import com.msspoker.matchmakingservice.exception.MatchmakingApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class QueueModeValidator {
    public void requireAvailable(MatchMode mode) {
        if (mode != MatchMode.NORMAL) {
            throw new MatchmakingApiException(
                    MatchmakingError.MM_MODE_NOT_AVAILABLE,
                    HttpStatus.NOT_IMPLEMENTED,
                    "Hàng chờ Rank chưa được bật."
            );
        }
    }
}
