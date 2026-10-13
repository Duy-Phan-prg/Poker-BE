package com.msspoker.matchmakingservice.service.impl;

import com.msspoker.matchmakingservice.entity.MatchProposalEntity;
import com.msspoker.matchmakingservice.entity.MatchProposalPlayerEntity;
import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.enums.ProposalStatus;
import com.msspoker.matchmakingservice.model.QueueEntry;
import com.msspoker.matchmakingservice.repository.MatchProposalRepository;
import com.msspoker.matchmakingservice.service.ProposalPersistenceService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProposalPersistenceServiceImpl implements ProposalPersistenceService {
    private final MatchProposalRepository proposals;
    private final Clock clock;

    public ProposalPersistenceServiceImpl(MatchProposalRepository proposals, Clock clock) {
        this.proposals = proposals;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void createNormal(UUID proposalId, List<QueueEntry> entries) {
        Instant now = clock.instant();
        MatchProposalEntity proposal = MatchProposalEntity.normal(proposalId, now);
        entries.forEach(entry -> proposal.addPlayer(
                MatchProposalPlayerEntity.reserved(entry.accountId(), entry.joinedAt(), now)
        ));
        proposals.save(proposal);
    }

    @Override
    @Transactional
    public void ready(UUID proposalId, UUID tableId, UUID matchId) {
        proposal(proposalId).ready(tableId, matchId, clock.instant());
    }

    @Override
    @Transactional
    public void failed(UUID proposalId, String failureCode) {
        proposal(proposalId).fail(failureCode, clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchProposalEntity> recoverableNormal(Instant updatedBefore) {
        return proposals.findTop20ByModeAndStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                MatchMode.NORMAL,
                ProposalStatus.CREATING_TABLE,
                updatedBefore
        );
    }

    private MatchProposalEntity proposal(UUID id) {
        return proposals.findById(id).orElseThrow(() -> new EntityNotFoundException("Proposal not found: " + id));
    }
}
