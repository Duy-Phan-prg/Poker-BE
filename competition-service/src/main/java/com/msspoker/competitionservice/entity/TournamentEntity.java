package com.msspoker.competitionservice.entity;

import com.msspoker.competitionservice.enums.TournamentStatus;
import com.msspoker.competitionservice.enums.TournamentType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
public class TournamentEntity {
    @Id private UUID id;
    private String name;
    @Enumerated(EnumType.STRING) private TournamentType type;
    private Instant startAt;
    private long entryFee;
    private int slots;
    @Enumerated(EnumType.STRING) private TournamentStatus status;
    /** ELITE coin prize; OPEN prizes come from the entry fee pool (GĐ2). */
    private long rewardCoins;
    /** Exclusive skin granted to the ELITE winner (GĐ2). */
    private UUID rewardProductId;
    private UUID createdBy;
    private Instant createdAt;
    private boolean isDeleted;
}
