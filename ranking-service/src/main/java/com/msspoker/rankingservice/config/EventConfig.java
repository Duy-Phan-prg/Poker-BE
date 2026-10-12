package com.msspoker.rankingservice.config;

import com.msspoker.rankingservice.constant.RankingEventConstants;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "ranking.events.enabled", havingValue = "true", matchIfMissing = true)
public class EventConfig {
    @Bean
    public TopicExchange pokerEvents() {
        return new TopicExchange(RankingEventConstants.EXCHANGE, true, false);
    }
}
