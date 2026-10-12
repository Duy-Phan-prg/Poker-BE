# Ranking service — Hoài Anh

Elo, rank tiers, leaderboard, seasons and tutorial reward (SPEC §9.4).
Implementation batches and review dependencies: [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

## Package structure

The backend follows the `game-service` layers under `com.msspoker.rankingservice`:

```text
rankingservice/
├── controller/         # HTTP entry points
├── dto/
│   ├── request/        # Validated request records
│   ├── response/       # JSON responses and pagination
│   └── event/          # RabbitMQ event envelopes and payloads
├── service/            # Application service interfaces
│   └── impl/           # Spring-managed implementations
├── entity/             # JPA entities only
├── repository/         # Spring Data repositories only
├── validator/          # Business validation
├── mapper/             # MapStruct transformations
├── enums/              # Tier, season status and error enums
├── constant/           # Shared constants and header names
├── exception/          # Exception factory and REST exception handler
├── config/             # Spring configuration
└── util/               # UUID v7 generation
```

## Run with PostgreSQL and RabbitMQ in Docker

The service owns PostgreSQL `ranking-db`, declared in `ranking-service/compose.yml`
on the private `ranking-net` network. RabbitMQ is the shared broker in
`docker/compose.infra.yml`. The root `docker-compose.yml` does not include the
per-service files yet, so start them explicitly from the BE repository root:

```powershell
docker compose -p poker-be -f docker/compose.infra.yml -f ranking-service/compose.yml up -d ranking-db rabbitmq
.\mvnw.cmd -pl ranking-service -am -DskipTests package
java -jar ranking-service/target/ranking-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Dev uses PostgreSQL `127.0.0.1:54331/ranking_db` (user `poker`, local password
`poker_dev`) and RabbitMQ `127.0.0.1:56729` (`guest`/`guest`). When changing the
local password, set `RANKING_DB_PASSWORD` for Compose and `RANKING_DEV_DB_PASSWORD`
to the same value for the Java process. Flyway creates the schema; Hibernate
validates it. H2 is a test-scope dependency only.

To run the application container with the `dev` profile:

```powershell
docker compose -p poker-be -f docker/compose.infra.yml -f ranking-service/compose.yml -f ranking-service/compose.dev.yml up -d --build ranking-service
```

## Production configuration

- PostgreSQL: `RANKING_DB_URL`, `RANKING_DB_USER`, `RANKING_DB_PASSWORD`.
- RabbitMQ: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`.

## Verification

```powershell
.\mvnw.cmd -pl ranking-service -am clean verify
```

Tests use test-only H2 and do not require Docker.
