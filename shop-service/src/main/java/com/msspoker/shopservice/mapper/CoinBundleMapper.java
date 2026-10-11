package com.msspoker.shopservice.mapper;

import com.msspoker.shopservice.dto.response.CoinBundleResponse;
import com.msspoker.shopservice.entity.CoinBundle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CoinBundleMapper {

    @Mapping(target = "totalVipCoin", expression = "java(bundle.getTotalVipCoin())")
    CoinBundleResponse toCoinBundleResponse(CoinBundle bundle);

    List<CoinBundleResponse> toCoinBundleResponseList(List<CoinBundle> bundles);
}
