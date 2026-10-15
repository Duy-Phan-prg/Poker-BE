package com.msspoker.matchmakingservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI matchmakingOpenApi() {
        return new OpenAPI()
                .addTagsItem(new Tag()
                        .name("Normal matchmaking")
                        .description("Đã có API: join, leave, status và reconnect."))
                .addTagsItem(new Tag()
                        .name("Rank matchmaking")
                        .description("Chưa triển khai: Elo matching, accept/decline, Wallet và cooldown."))
                .info(new Info()
                        .title("MS Poker Matchmaking API")
                        .version("Normal MVP")
                        .description("API kiểm thử hàng chờ ghép trận. Custom Room thuộc competition-service, "
                                + "không thuộc Matchmaking. RANK, accept/decline, Wallet, notification/outbox, "
                                + "Party và E2E qua Gateway chưa hoàn tất.")
                        .license(new License().name("Internal use only")));
    }
}
