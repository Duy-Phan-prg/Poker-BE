package com.msspoker.matchmakingservice.entity;

import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.enums.ProposalStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "match_proposals")
public class MatchProposalEntity {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) private MatchMode mode;
    @Enumerated(EnumType.STRING) private ProposalStatus status;
    private long entryFee;
    private Integer eloMin;
    private Integer eloMax;
    private Instant acceptDeadline;
    private UUID tableId;
    private UUID matchId;
    private String failureCode;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isDeleted;
    @Version private long version;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchProposalPlayerEntity> players = new ArrayList<>();

    protected MatchProposalEntity() {
    }

    public static MatchProposalEntity normal(UUID id, Instant now) {
        MatchProposalEntity proposal = new MatchProposalEntity();
        proposal.id = id;
        proposal.mode = MatchMode.NORMAL;
        proposal.status = ProposalStatus.CREATING_TABLE;
        proposal.entryFee = 0;
        proposal.createdAt = now;
        proposal.updatedAt = now;
        return proposal;
    }

    public void addPlayer(MatchProposalPlayerEntity player) {
        players.add(player);
        player.attachTo(this);
    }

    public void ready(UUID tableId, UUID matchId, Instant now) {
        this.status = ProposalStatus.READY;
        this.tableId = tableId;
        this.matchId = matchId;
        this.failureCode = null;
        this.updatedAt = now;
        players.forEach(MatchProposalPlayerEntity::matched);
    }

    public void fail(String failureCode, Instant now) {
        this.status = ProposalStatus.FAILED;
        this.failureCode = failureCode;
        this.updatedAt = now;
        players.forEach(MatchProposalPlayerEntity::requeued);
    }

    public UUID getId() { return id; }
    public MatchMode getMode() { return mode; }
    public ProposalStatus getStatus() { return status; }
    public long getEntryFee() { return entryFee; }
    public UUID getTableId() { return tableId; }
    public UUID getMatchId() { return matchId; }
    public String getFailureCode() { return failureCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<MatchProposalPlayerEntity> getPlayers() { return players; }
}
