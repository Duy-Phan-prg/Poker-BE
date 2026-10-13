package com.msspoker.competitionservice.dto.response;

import com.msspoker.competitionservice.enums.EntryStatus;

import java.time.Instant;
import java.util.UUID;

public record TournamentEntryResponse(UUID accountId, EntryStatus status, Integer finalPlace, Instant registeredAt) {
}
