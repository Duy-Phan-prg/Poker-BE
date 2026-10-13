package com.msspoker.matchmakingservice.entity;

import com.msspoker.matchmakingservice.enums.ProposalPlayerStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "match_proposal_players")
public class MatchProposalPlayerEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id", nullable = false)
    private MatchProposalEntity proposal;
    private UUID accountId;
    private Integer eloSnapshot;
    private Instant queueJoinedAt;
    @Enumerated(EnumType.STRING) private ProposalPlayerStatus status;
    private Instant acceptedAt;
    private boolean feeCharged;
    private boolean feeRefunded;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isDeleted;
    @Version private long version;

    protected MatchProposalPlayerEntity() {
    }

    public static MatchProposalPlayerEntity reserved(UUID accountId, Instant joinedAt, Instant now) {
        MatchProposalPlayerEntity player = new MatchProposalPlayerEntity();
        player.id = UUID.randomUUID();
        player.accountId = accountId;
        player.queueJoinedAt = joinedAt;
        player.status = ProposalPlayerStatus.RESERVED;
        player.createdAt = now;
        player.updatedAt = now;
        return player;
    }

    void attachTo(MatchProposalEntity proposal) {
        this.proposal = proposal;
    }

    void matched() {
        status = ProposalPlayerStatus.MATCHED;
        updatedAt = Instant.now();
    }

    void requeued() {
        status = ProposalPlayerStatus.REQUEUED;
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public MatchProposalEntity getProposal() { return proposal; }
    public UUID getAccountId() { return accountId; }
    public Instant getQueueJoinedAt() { return queueJoinedAt; }
    public ProposalPlayerStatus getStatus() { return status; }
}
