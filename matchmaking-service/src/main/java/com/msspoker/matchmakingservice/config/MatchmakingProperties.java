package com.msspoker.matchmakingservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@ConfigurationProperties(prefix = "matchmaking")
public class MatchmakingProperties {
    @Valid private final Normal normal = new Normal();
    @Valid private final Queue queue = new Queue();
    @Valid private final Finder finder = new Finder();
    @Valid private final Game game = new Game();
    @Valid private final Recovery recovery = new Recovery();

    public Normal getNormal() { return normal; }
    public Queue getQueue() { return queue; }
    public Finder getFinder() { return finder; }
    public Game getGame() { return game; }
    public Recovery getRecovery() { return recovery; }

    public static class Normal {
        @Min(2) @Max(6) private int minPlayers = 2;
        public int getMinPlayers() { return minPlayers; }
        public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }
    }

    public static class Queue {
        @NotNull private Duration entryTtl = Duration.ofHours(24);
        @NotNull private Duration reservationTtl = Duration.ofMinutes(2);
        public Duration getEntryTtl() { return entryTtl; }
        public void setEntryTtl(Duration entryTtl) { this.entryTtl = entryTtl; }
        public Duration getReservationTtl() { return reservationTtl; }
        public void setReservationTtl(Duration reservationTtl) { this.reservationTtl = reservationTtl; }
    }

    public static class Finder {
        private boolean enabled = true;
        @Min(100) private long fixedDelayMs = 1000;
        @NotNull private Duration lockTtl = Duration.ofSeconds(5);
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getFixedDelayMs() { return fixedDelayMs; }
        public void setFixedDelayMs(long fixedDelayMs) { this.fixedDelayMs = fixedDelayMs; }
        public Duration getLockTtl() { return lockTtl; }
        public void setLockTtl(Duration lockTtl) { this.lockTtl = lockTtl; }
    }

    public static class Game {
        @NotNull private Duration timeout = Duration.ofSeconds(3);
        private boolean stubEnabled;
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
        public boolean isStubEnabled() { return stubEnabled; }
        public void setStubEnabled(boolean stubEnabled) { this.stubEnabled = stubEnabled; }
    }

    public static class Recovery {
        @Min(1000) private long fixedDelayMs = 5000;
        @NotNull private Duration retryAfter = Duration.ofSeconds(10);
        public long getFixedDelayMs() { return fixedDelayMs; }
        public void setFixedDelayMs(long fixedDelayMs) { this.fixedDelayMs = fixedDelayMs; }
        public Duration getRetryAfter() { return retryAfter; }
        public void setRetryAfter(Duration retryAfter) { this.retryAfter = retryAfter; }
    }
}
