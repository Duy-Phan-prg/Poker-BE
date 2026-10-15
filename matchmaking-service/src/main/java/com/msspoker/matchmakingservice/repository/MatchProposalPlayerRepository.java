package com.msspoker.matchmakingservice.repository;

import com.msspoker.matchmakingservice.entity.MatchProposalPlayerEntity;
import com.msspoker.matchmakingservice.enums.ProposalStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MatchProposalPlayerRepository extends JpaRepository<MatchProposalPlayerEntity, UUID> {
    @EntityGraph(attributePaths = "proposal")
    Optional<MatchProposalPlayerEntity> findFirstByAccountIdAndIsDeletedFalseAndProposal_StatusAndProposal_IsDeletedFalseOrderByCreatedAtDesc(
            UUID accountId,
            ProposalStatus status
    );

    @Query("""
            select (count(player) > 0)
            from MatchProposalPlayerEntity player
            where player.accountId = :accountId
              and player.isDeleted = false
              and player.proposal.isDeleted = false
              and player.proposal.status = com.msspoker.matchmakingservice.enums.ProposalStatus.CREATING_TABLE
            """)
    boolean existsActiveProposal(@Param("accountId") UUID accountId);
}
