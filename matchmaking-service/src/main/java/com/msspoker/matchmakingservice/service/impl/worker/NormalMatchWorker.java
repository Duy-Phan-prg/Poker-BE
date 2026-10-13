package com.msspoker.matchmakingservice.service.impl.worker;

import com.msspoker.matchmakingservice.config.MatchmakingProperties;
import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.model.QueueEntry;
import com.msspoker.matchmakingservice.service.NormalMatchCoordinator;
import com.msspoker.matchmakingservice.service.QueueStore;
import com.msspoker.matchmakingservice.util.MatchmakingIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "matchmaking.finder.enabled", havingValue = "true", matchIfMissing = true)
public class NormalMatchWorker {
    private static final Logger log = LoggerFactory.getLogger(NormalMatchWorker.class);

    private final QueueStore queueStore;
    private final NormalMatchCoordinator coordinator;
    private final MatchmakingProperties properties;

    public NormalMatchWorker(
            QueueStore queueStore,
            NormalMatchCoordinator coordinator,
            MatchmakingProperties properties
    ) {
        this.queueStore = queueStore;
        this.coordinator = coordinator;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${matchmaking.finder.fixed-delay-ms:1000}")
    public void find() {
        Optional<String> lock = queueStore.acquireFinderLock(MatchMode.NORMAL);
        if (lock.isEmpty()) return;

        try {
            int playerCount = properties.getNormal().getMinPlayers();
            List<QueueEntry> entries = queueStore.oldest(MatchMode.NORMAL, playerCount);
            if (entries.size() < playerCount) return;

            UUID proposalId = MatchmakingIds.newProposalId();
            if (!queueStore.reserve(MatchMode.NORMAL, entries, proposalId)) return;
            coordinator.start(proposalId, entries);
        } catch (RuntimeException exception) {
            log.error("Normal match finder failed", exception);
        } finally {
            queueStore.releaseFinderLock(MatchMode.NORMAL, lock.get());
        }
    }
}
