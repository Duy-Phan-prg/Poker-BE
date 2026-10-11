package com.msspoker.shopservice.service.impl;

import com.msspoker.shopservice.dto.response.CoinBundleResponse;
import com.msspoker.shopservice.entity.CoinBundle;
import com.msspoker.shopservice.exception.OrderExceptions;
import com.msspoker.shopservice.mapper.CoinBundleMapper;
import com.msspoker.shopservice.repository.CoinBundleRepository;
import com.msspoker.shopservice.service.CoinBundleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoinBundleServiceImpl implements CoinBundleService {

    private final CoinBundleRepository coinBundleRepository;
    private final CoinBundleMapper coinBundleMapper;

    @Override
    @Cacheable(value = "coin_bundles", key = "'active_bundles'")
    public List<CoinBundleResponse> getActiveBundles() {
        log.debug("Fetching active coin bundles from DB");
        List<CoinBundle> bundles = coinBundleRepository.findByIsActiveTrueOrderBySortOrderAsc();
        return coinBundleMapper.toCoinBundleResponseList(bundles);
    }

    @Override
    public CoinBundle getBundleEntity(UUID id) {
        log.debug("Fetching coin bundle entity by id={}", id);
        return coinBundleRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(OrderExceptions::bundleNotFound);
    }
}
