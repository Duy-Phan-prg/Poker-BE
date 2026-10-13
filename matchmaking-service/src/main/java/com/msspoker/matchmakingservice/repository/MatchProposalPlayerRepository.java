package com.msspoker.matchmakingservice.repository;

import com.msspoker.matchmakingservice.entity.MatchProposalPlayerEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchProposalPlayerRepository extends JpaRepository<MatchProposalPlayerEntity, UUID> {
    @EntityGraph(attributePaths = "proposal")
    Optional<MatchProposalPlayerEntity> findFirstByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
