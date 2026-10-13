package com.msspoker.competitionservice.repository;

import com.msspoker.competitionservice.entity.TournamentEntity;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TournamentRepository extends JpaRepository<TournamentEntity, UUID> {
    Page<TournamentEntity> findByIsDeletedFalse(Pageable pageable);

    Optional<TournamentEntity> findByIdAndIsDeletedFalse(UUID id);

    /** Serializes registration, start, cancellation and result handling of one tournament. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TournamentEntity t where t.id = :id and t.isDeleted = false")
    Optional<TournamentEntity> lockById(UUID id);
}
