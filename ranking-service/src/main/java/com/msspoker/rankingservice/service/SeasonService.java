package com.msspoker.rankingservice.service;

import com.msspoker.rankingservice.dto.response.SeasonResponse;
import com.msspoker.rankingservice.entity.SeasonEntity;

public interface SeasonService {
    SeasonEntity active();

    SeasonResponse current();
}
