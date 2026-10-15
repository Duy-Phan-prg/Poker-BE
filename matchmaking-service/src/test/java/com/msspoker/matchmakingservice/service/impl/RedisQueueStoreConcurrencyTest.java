package com.msspoker.matchmakingservice.service.impl;

import com.msspoker.matchmakingservice.config.MatchmakingProperties;
import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.model.QueueEntry;
import com.msspoker.matchmakingservice.model.InGameSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RedisQueueStoreConcurrencyTest {
    private static GenericContainer<?> redis;
    private static LettuceConnectionFactory connectionFactory;
    private static RedisQueueStore store;

    @BeforeAll
    static void startRedis() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is required for the Redis concurrency test");

        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        redis.start();

        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();

        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        store = new RedisQueueStore(template, new MatchmakingProperties());
    }

    @AfterAll
    static void stopRedis() {
        if (connectionFactory != null) connectionFactory.destroy();
        if (redis != null) redis.stop();
    }

    @Test
    void onlyOneWorkerCanReserveTheSameAccounts() throws Exception {
        List<QueueEntry> entries = List.of(
                new QueueEntry(UUID.randomUUID(), MatchMode.NORMAL, Instant.EPOCH.plusSeconds(1)),
                new QueueEntry(UUID.randomUUID(), MatchMode.NORMAL, Instant.EPOCH.plusSeconds(2))
        );
        entries.forEach(entry -> assertThat(store.enqueue(entry)).isTrue());

        UUID firstProposal = UUID.randomUUID();
        UUID secondProposal = UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = workers.submit(() -> reserveTogether(entries, firstProposal, ready, start));
            Future<Boolean> second = workers.submit(() -> reserveTogether(entries, secondProposal, ready, start));
            ready.await();
            start.countDown();

            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);

            UUID winningProposal = store.reservation(entries.getFirst().accountId()).orElseThrow();
            assertThat(winningProposal).isIn(firstProposal, secondProposal);
            assertThat(store.reservation(entries.getLast().accountId())).contains(winningProposal);
            assertThat(store.oldest(MatchMode.NORMAL, 2)).isEmpty();
        } finally {
            workers.shutdownNow();
            InGameSession session = new InGameSession(
                    MatchMode.NORMAL,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "localhost",
                    8082,
                    "/ws/game"
            );
            store.complete(entries, session);
            assertThat(store.inGame(entries.getFirst().accountId())).contains(session);
            assertThat(store.inGame(entries.getLast().accountId())).contains(session);
            entries.forEach(entry -> store.clearInGame(entry.accountId()));
        }
    }

    private boolean reserveTogether(
            List<QueueEntry> entries,
            UUID proposalId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        return store.reserve(MatchMode.NORMAL, entries, proposalId);
    }
}
