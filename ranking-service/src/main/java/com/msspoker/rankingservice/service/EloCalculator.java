package com.msspoker.rankingservice.service;

import com.msspoker.rankingservice.enums.Tier;

public interface EloCalculator {
    int delta(int place);

    int apply(int elo, int delta);

    Tier tier(int elo);

    Tier displayTier(Tier tier, long position);

    boolean extendsWinStreak(int place);

    long reward(int place, long entryFee);
}
