package com.msspoker.rankingservice.repository;

import com.msspoker.rankingservice.entity.SeasonEntity;
import com.msspoker.rankingservice.enums.SeasonStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SeasonRepository extends JpaRepository<SeasonEntity, UUID> {
    Optional<SeasonEntity> findFirstByStatusAndIsDeletedFalse(SeasonStatus status);
}
