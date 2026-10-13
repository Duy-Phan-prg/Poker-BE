package com.msspoker.matchmakingservice.mapper;

import com.msspoker.matchmakingservice.dto.response.QueueStatusResponse;
import com.msspoker.matchmakingservice.entity.MatchProposalPlayerEntity;
import com.msspoker.matchmakingservice.model.QueueEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface QueueStatusMapper {
    @Mapping(target = "status", constant = "WAITING")
    @Mapping(target = "mode", source = "entry.mode")
    @Mapping(target = "joinedAt", source = "entry.joinedAt")
    @Mapping(target = "position", source = "position")
    @Mapping(target = "proposalId", ignore = true)
    @Mapping(target = "tableId", ignore = true)
    @Mapping(target = "matchId", ignore = true)
    QueueStatusResponse waiting(QueueEntry entry, Long position);

    @Mapping(target = "status", constant = "MATCHING")
    @Mapping(target = "mode", source = "entry.mode")
    @Mapping(target = "joinedAt", source = "entry.joinedAt")
    @Mapping(target = "position", ignore = true)
    @Mapping(target = "proposalId", source = "proposalId")
    @Mapping(target = "tableId", ignore = true)
    @Mapping(target = "matchId", ignore = true)
    QueueStatusResponse matching(QueueEntry entry, UUID proposalId);

    @Mapping(target = "status", source = "proposal.status")
    @Mapping(target = "mode", source = "proposal.mode")
    @Mapping(target = "joinedAt", source = "queueJoinedAt")
    @Mapping(target = "position", ignore = true)
    @Mapping(target = "proposalId", source = "proposal.id")
    @Mapping(target = "tableId", source = "proposal.tableId")
    @Mapping(target = "matchId", source = "proposal.matchId")
    QueueStatusResponse proposal(MatchProposalPlayerEntity player);
}
