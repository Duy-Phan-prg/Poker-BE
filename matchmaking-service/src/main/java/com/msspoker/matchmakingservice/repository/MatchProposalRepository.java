package com.msspoker.matchmakingservice.repository;

import com.msspoker.matchmakingservice.entity.MatchProposalEntity;
import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.enums.ProposalStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MatchProposalRepository extends JpaRepository<MatchProposalEntity, UUID> {
    @EntityGraph(attributePaths = "players")
    List<MatchProposalEntity> findTop20ByModeAndStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            MatchMode mode,
            ProposalStatus status,
            Instant updatedBefore
    );
}
