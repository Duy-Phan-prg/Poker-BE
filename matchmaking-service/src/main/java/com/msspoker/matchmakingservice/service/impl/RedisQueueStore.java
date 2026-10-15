package com.msspoker.matchmakingservice.service.impl;

import com.msspoker.matchmakingservice.config.MatchmakingProperties;
import com.msspoker.matchmakingservice.constant.MatchmakingConstants;
import com.msspoker.matchmakingservice.enums.MatchMode;
import com.msspoker.matchmakingservice.model.QueueEntry;
import com.msspoker.matchmakingservice.model.InGameSession;
import com.msspoker.matchmakingservice.service.QueueStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class RedisQueueStore implements QueueStore {
    private static final DefaultRedisScript<Long> ENQUEUE = script("redis/enqueue.lua");
    private static final DefaultRedisScript<Long> CANCEL = script("redis/cancel.lua");
    private static final DefaultRedisScript<Long> RESERVE = script("redis/reserve.lua");
    private static final DefaultRedisScript<Long> COMPLETE = script("redis/complete.lua");
    private static final DefaultRedisScript<Long> REQUEUE = script("redis/requeue.lua");
    private static final DefaultRedisScript<Long> RELEASE_LOCK = script("redis/release-lock.lua");

    private final StringRedisTemplate redis;
    private final MatchmakingProperties properties;

    public RedisQueueStore(
            StringRedisTemplate redis,
            MatchmakingProperties properties
    ) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public boolean enqueue(QueueEntry entry) {
        Long result = redis.execute(
                ENQUEUE,
                List.of(queueKey(entry.mode()), entryKey(entry.accountId())),
                entry.accountId().toString(),
                Long.toString(entry.joinedAt().toEpochMilli()),
                write(entry),
                Long.toString(properties.getQueue().getEntryTtl().toMillis())
        );
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public CancelResult cancel(UUID accountId, MatchMode mode) {
        Long result = redis.execute(
                CANCEL,
                List.of(queueKey(mode), entryKey(accountId), reservationKey(accountId)),
                accountId.toString()
        );
        if (Long.valueOf(-1L).equals(result)) return CancelResult.RESERVED;
        if (Long.valueOf(1L).equals(result)) return CancelResult.CANCELLED;
        return CancelResult.NOT_FOUND;
    }

    @Override
    public Optional<QueueEntry> find(UUID accountId) {
        String value = redis.opsForValue().get(entryKey(accountId));
        if (value == null) return Optional.empty();
        try {
            String[] parts = value.split("\\|", -1);
            return Optional.of(new QueueEntry(
                    accountId,
                    MatchMode.valueOf(parts[0]),
                    Instant.ofEpochMilli(Long.parseLong(parts[1]))
            ));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Queue entry bị hỏng: " + accountId, exception);
        }
    }

    @Override
    public Optional<Long> position(UUID accountId, MatchMode mode) {
        Long rank = redis.opsForZSet().rank(queueKey(mode), accountId.toString());
        return rank == null ? Optional.empty() : Optional.of(rank + 1);
    }

    @Override
    public Optional<UUID> reservation(UUID accountId) {
        String value = redis.opsForValue().get(reservationKey(accountId));
        if (value == null) return Optional.empty();
        return Optional.of(UUID.fromString(value));
    }

    @Override
    public List<QueueEntry> oldest(MatchMode mode, int limit) {
        Set<String> ids = redis.opsForZSet().range(queueKey(mode), 0, limit - 1L);
        if (ids == null || ids.isEmpty()) return List.of();

        List<QueueEntry> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            UUID accountId = UUID.fromString(id);
            Optional<QueueEntry> entry = find(accountId);
            if (entry.isPresent()) {
                result.add(entry.get());
            } else {
                redis.opsForZSet().remove(queueKey(mode), id);
            }
        }
        return result;
    }

    @Override
    public long size(MatchMode mode) {
        Long size = redis.opsForZSet().zCard(queueKey(mode));
        return size == null ? 0 : size;
    }

    @Override
    public Optional<Instant> countdownDeadline(MatchMode mode) {
        String value = redis.opsForValue().get(countdownKey(mode));
        if (value == null) return Optional.empty();
        try {
            return Optional.of(Instant.ofEpochMilli(Long.parseLong(value)));
        } catch (NumberFormatException exception) {
            redis.delete(countdownKey(mode));
            throw new IllegalStateException("Countdown queue bị hỏng: " + mode, exception);
        }
    }

    @Override
    public Instant startCountdown(MatchMode mode, Instant deadline) {
        String key = countdownKey(mode);
        redis.opsForValue().setIfAbsent(key, Long.toString(deadline.toEpochMilli()), properties.getQueue().getEntryTtl());
        return countdownDeadline(mode).orElse(deadline);
    }

    @Override
    public void cancelCountdown(MatchMode mode) {
        redis.delete(countdownKey(mode));
    }

    @Override
    public boolean reserve(MatchMode mode, List<QueueEntry> entries, UUID reservationId) {
        int count = entries.size();
        List<String> keys = new ArrayList<>(1 + count * 2);
        keys.add(queueKey(mode));
        entries.forEach(entry -> keys.add(entryKey(entry.accountId())));
        entries.forEach(entry -> keys.add(reservationKey(entry.accountId())));

        List<String> arguments = new ArrayList<>(3 + count);
        arguments.add(Integer.toString(count));
        arguments.add(reservationId.toString());
        arguments.add(Long.toString(properties.getQueue().getReservationTtl().toMillis()));
        entries.forEach(entry -> arguments.add(entry.accountId().toString()));

        Long result = redis.execute(RESERVE, keys, arguments.toArray());
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public void complete(List<QueueEntry> entries, InGameSession session) {
        List<String> keys = new ArrayList<>(entries.size() * 3);
        for (QueueEntry entry : entries) {
            keys.add(entryKey(entry.accountId()));
            keys.add(reservationKey(entry.accountId()));
            keys.add(inGameKey(entry.accountId()));
        }
        List<String> arguments = new ArrayList<>(3);
        arguments.add(Integer.toString(entries.size()));
        arguments.add(Long.toString(properties.getGame().getSessionTtl().toMillis()));
        arguments.add(write(session));
        redis.execute(COMPLETE, keys, arguments.toArray());
    }

    @Override
    public Optional<InGameSession> inGame(UUID accountId) {
        String value = redis.opsForValue().get(inGameKey(accountId));
        if (value == null) return Optional.empty();
        try {
            String[] parts = value.split("\\|", -1);
            if (parts.length != 8 || !"IN_GAME".equals(parts[0])) {
                throw new IllegalArgumentException("Sai định dạng IN_GAME");
            }
            return Optional.of(new InGameSession(
                    MatchMode.valueOf(parts[1]),
                    UUID.fromString(parts[2]),
                    UUID.fromString(parts[3]),
                    UUID.fromString(parts[4]),
                    parts[5],
                    Integer.parseInt(parts[6]),
                    parts[7]
            ));
        } catch (RuntimeException exception) {
            redis.delete(inGameKey(accountId));
            throw new IllegalStateException("IN_GAME session bị hỏng: " + accountId, exception);
        }
    }

    @Override
    public void clearInGame(UUID accountId) {
        redis.delete(inGameKey(accountId));
    }

    @Override
    public void requeue(List<QueueEntry> entries) {
        if (entries.isEmpty()) return;
        MatchMode mode = entries.getFirst().mode();
        if (entries.stream().anyMatch(entry -> entry.mode() != mode)) {
            throw new IllegalArgumentException("Không thể requeue nhiều mode trong một operation");
        }
        List<String> keys = new ArrayList<>(1 + entries.size() * 2);
        keys.add(queueKey(mode));
        entries.forEach(entry -> {
            keys.add(entryKey(entry.accountId()));
            keys.add(reservationKey(entry.accountId()));
        });
        List<String> arguments = new ArrayList<>(2 + entries.size() * 3);
        arguments.add(Integer.toString(entries.size()));
        arguments.add(Long.toString(properties.getQueue().getEntryTtl().toMillis()));
        entries.forEach(entry -> {
            arguments.add(entry.accountId().toString());
            arguments.add(Long.toString(entry.joinedAt().toEpochMilli()));
            arguments.add(write(entry));
        });
        redis.execute(REQUEUE, keys, arguments.toArray());
    }

    @Override
    public Optional<String> acquireFinderLock(MatchMode mode) {
        String token = UUID.randomUUID().toString();
        Duration ttl = properties.getFinder().getLockTtl();
        Boolean acquired = redis.opsForValue().setIfAbsent(lockKey(mode), token, ttl);
        return Boolean.TRUE.equals(acquired) ? Optional.of(token) : Optional.empty();
    }

    @Override
    public void releaseFinderLock(MatchMode mode, String token) {
        redis.execute(RELEASE_LOCK, List.of(lockKey(mode)), token);
    }

    private String write(QueueEntry entry) {
        return entry.mode().name() + "|" + entry.joinedAt().toEpochMilli();
    }

    private String write(InGameSession session) {
        return String.join("|",
                "IN_GAME",
                session.mode().name(),
                session.proposalId().toString(),
                session.tableId().toString(),
                session.matchId().toString(),
                session.host(),
                Integer.toString(session.port()),
                session.websocketPath()
        );
    }

    private String queueKey(MatchMode mode) { return MatchmakingConstants.REDIS_PREFIX + "queue:" + mode.name(); }
    private String entryKey(UUID accountId) { return MatchmakingConstants.REDIS_PREFIX + "entry:" + accountId; }
    private String reservationKey(UUID accountId) { return MatchmakingConstants.REDIS_PREFIX + "reservation:" + accountId; }
    private String lockKey(MatchMode mode) { return MatchmakingConstants.REDIS_PREFIX + "lock:finder:" + mode.name(); }
    private String countdownKey(MatchMode mode) { return MatchmakingConstants.REDIS_PREFIX + "countdown:" + mode.name(); }
    private String inGameKey(UUID accountId) { return MatchmakingConstants.REDIS_PREFIX + "in-game:" + accountId; }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        return script;
    }
}
