package com.msspoker.shopservice.service;

import com.msspoker.shopservice.dto.response.CoinBundleResponse;
import com.msspoker.shopservice.entity.CoinBundle;

import java.util.List;
import java.util.UUID;

public interface CoinBundleService {

    List<CoinBundleResponse> getActiveBundles();

    CoinBundle getBundleEntity(UUID id);
}
