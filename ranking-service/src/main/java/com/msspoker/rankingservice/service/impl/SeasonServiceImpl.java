package com.msspoker.rankingservice.service.impl;

import com.msspoker.rankingservice.dto.response.SeasonResponse;
import com.msspoker.rankingservice.entity.SeasonEntity;
import com.msspoker.rankingservice.enums.SeasonStatus;
import com.msspoker.rankingservice.exception.RankingExceptions;
import com.msspoker.rankingservice.mapper.RankingMapper;
import com.msspoker.rankingservice.repository.SeasonRepository;
import com.msspoker.rankingservice.service.SeasonService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeasonServiceImpl implements SeasonService {
    private final SeasonRepository repository;
    private final RankingMapper mapper;

    @Transactional(readOnly = true)
    @Override
    public SeasonEntity active() {
        return repository.findFirstByStatusAndIsDeletedFalse(SeasonStatus.ACTIVE)
                .orElseThrow(RankingExceptions::noActiveSeason);
    }

    @Transactional(readOnly = true)
    @Override
    public SeasonResponse current() {
        return mapper.season(active());
    }
}
