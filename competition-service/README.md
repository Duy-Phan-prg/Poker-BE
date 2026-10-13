# Competition service — Hoài Anh

Custom Room and tournaments (SPEC §7.4, §7.7, §9.4).
Implementation batches and review dependencies: [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

## Package structure

The backend follows the `game-service` layers under `com.msspoker.competitionservice`:

```text
competitionservice/
├── controller/         # HTTP and RabbitMQ entry points
├── dto/
│   ├── request/        # Validated requests and the CreateTable body sent to game
│   ├── response/       # JSON responses and bodies read from game/social
│   └── event/          # Consumed match.finished and published notification payloads
├── service/            # Application service interfaces
│   ├── event/          # Outbox publishing interface
│   └── impl/           # Spring-managed implementations
│       └── event/      # OutboxPublisherImpl
├── entity/             # JPA entities only
├── repository/         # Spring Data repositories only
├── validator/          # Room access and settings validation
├── mapper/             # MapStruct transformations
├── enums/              # Room, notification, game mode and error enums
├── constant/           # Shared constants and header names
├── exception/          # Exception factory and REST exception handler
├── config/             # Spring configuration and room defaults
└── util/               # UUID v7 generation
```

## Run with PostgreSQL and RabbitMQ in Docker

The service owns PostgreSQL `competition-db` (`competition-service/compose.yml`,
private `competition-net`). From the BE repository root:

```powershell
docker compose up -d competition-db rabbitmq
.\mvnw.cmd -pl competition-service -am -DskipTests package
java -jar competition-service/target/competition-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Dev uses PostgreSQL `127.0.0.1:54332/competition_db` (user `poker`, password
`poker_dev`), RabbitMQ `127.0.0.1:56729` and game-service at
`http://localhost:8082` (run it with its `dev` profile to start tables).
Container with the `dev` profile:

```powershell
docker compose -f docker-compose.yml -f competition-service/compose.dev.yml up -d --build competition-service
```

## Integration contract

All `/api/rooms/**` calls trust `X-User-Id` set by the gateway; a missing header
returns `401 COMP_UNAUTHORIZED`. Room codes are 6 characters and case-insensitive.

| Method | Endpoint | Behavior |
| --- | --- | --- |
| POST | `/api/rooms` | `{ visibility, password?, maxPlayers, smallBlind, bigBlind }` → `RoomResponse` |
| GET | `/api/rooms?page=0&size=20` | Waiting PUBLIC rooms, newest first, with `playerCount` |
| GET | `/api/rooms/me` | Own open room, or empty |
| GET | `/api/rooms/{code}` | Room detail; members only |
| POST | `/api/rooms/{code}/join` | Optional `{ password }`; rejoining is a no-op |
| POST | `/api/rooms/{code}/leave` | Owner leaving hands the room to the longest-waiting member |
| PUT | `/api/rooms/{code}/ready` | `{ ready }` |
| DELETE | `/api/rooms/{code}/members/{accountId}` | Owner kicks a member |
| PUT | `/api/rooms/{code}/owner` | `{ accountId }`; owner transfers the room |
| POST | `/api/rooms/{code}/invitations` | `{ accountId }`; friends only, sends `ROOM_INVITE` |
| POST | `/api/rooms/{code}/start` | Owner; everyone else ready → `CreateTable` CUSTOM → `PLAYING` with `tableId` |

`RoomResponse`: `id, roomCode, ownerId, visibility, hasPassword, maxPlayers,
smallBlind, bigBlind, status (WAITING/PLAYING/CLOSED), tableId, members[]` where a
member is `{ accountId, ready, owner, joinedAt }` in seat order.

Errors: `COMP_NOT_FOUND`, `COMP_FORBIDDEN` (not a member/owner, not friends,
restricted player), `COMP_WRONG_PASSWORD`, `COMP_ROOM_FULL`, `COMP_ALREADY_IN_ROOM`,
`COMP_CONFLICT` (room playing, not ready, too few players), `COMP_PLAYER_BUSY`,
`COMP_DEPENDENCY_UNAVAILABLE`, `COMP_INVALID_REQUEST`, `COMP_UNAUTHORIZED`.

Start sends `{ mode: CUSTOM, sourceId, playerIds (seat order), settings:
{ smallBlind, bigBlind, startingChips: 1000, turnTimeSeconds: 20 }, entryFee: 0 }`
to game-service `POST /internal/tables` with `X-Game-Service-Key` when
`GAME_SERVICE_KEY` is set. Each start uses a new `sourceId` stored on the room
before the call, so a retry after a timeout returns the same table.

### Tournaments (OPEN backend; ELITE invitations, schedule and prizes are GĐ2)

| Method | Endpoint | Behavior |
| --- | --- | --- |
| POST | `/api/admin/tournaments` | `X-User-Role: ADMIN`; `{ name, type, startAt (future), entryFee, slots?, rewardCoins?, rewardProductId? }`; ELITE is free |
| POST | `/api/admin/tournaments/{id}/start` | Seats registered players and opens the tables |
| POST | `/api/admin/tournaments/{id}/cancel` | Before start only; refunds every entry fee |
| GET | `/api/tournaments?page=0&size=20` | Tournaments by `startAt` with `entryCount` |
| GET | `/api/tournaments/{id}` | Detail with `entries[] { accountId, status, finalPlace }` and the bracket `tables[]` |
| POST | `/api/tournaments/{id}/register` | OPEN only, until `startAt`; debits the fee once |

Bracket: shuffled players fill ⌈n/8⌉ qualifier tables evenly; the top 2 of each
table reach one final table (32 players → 4 × 8 → 8, SPEC §2.4). Eight or fewer
players play the final directly; fewer than 4 cannot start. Tables are
`{ id, round (FINAL/QUALIFIER), tableIndex, status (PENDING/PLAYING/FINISHED),
tableId, seats[] { accountId, seatIndex, place } }`, final first.

Registration debits wallet-service `POST /internal/wallet/debit` with reason
`TOURNAMENT_ENTRY_FEE` and key `tournament-fee:{tournamentId}:{accountId}`; a
refusal returns `409 COMP_INSUFFICIENT_BALANCE`. Cancellation stores
`TOURNAMENT_REFUND` credits (key `tournament-refund:{tournamentId}:{accountId}`)
that are retried until the wallet accepts them.

Each tournament table calls CreateTable with `mode: TOURNAMENT`, `sourceId` = the
tournament table id, `settings: null` (game's TOURNAMENT Match Settings) and the
tournament fee as `entryFee`. Tables that game-service cannot open stay `PENDING`
and are retried every 2 seconds with the same source, then `TOURNAMENT_TABLE_READY`
is sent to their players with `data.tableId`.

Errors add `COMP_TOURNAMENT_FULL`, `COMP_INSUFFICIENT_BALANCE`, and
`COMP_FORBIDDEN` for non-admins and ELITE registration.

### Events on `poker.events`

- Consumes `match.finished` on durable queue `competition-service.match.finished`:
  CUSTOM results for the room's current `sourceId` return it to `WAITING` and clear
  ready flags; TOURNAMENT results record places, eliminate players outside the top
  2, open the final after the last qualifier and finish the tournament after the
  final. Redelivery and unknown sources change nothing.
- Publishes `notification.requested` (`ROOM_INVITE`, `ROOM_UPDATED` with
  `data.change`, `ROOM_STARTED` and `TOURNAMENT_TABLE_READY` with `data.tableId`)
  through the `competition_outbox` table, mandatory publishing and publisher
  confirms like game-service. Without a bound consumer queue they stay pending.

## Production configuration

- PostgreSQL: `COMPETITION_DB_URL`, `COMPETITION_DB_USER`, `COMPETITION_DB_PASSWORD`.
- RabbitMQ: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`.
- `GAME_SERVICE_URL`, `GAME_SERVICE_KEY`, `SOCIAL_SERVICE_URL`, `WALLET_SERVICE_URL`.
- Room defaults: `competition.room.min-players`, `max-players`, `starting-chips`,
  `turn-time-seconds`, `code-length`.
- Tournament format: `competition.tournament.slots`, `table-size`,
  `advance-per-table`, `min-players`.

In `dev` and `test`, friends and the wallet are mocked (everyone is a friend,
debits and credits succeed and are logged).

## Verification

```powershell
.\mvnw.cmd -pl competition-service -am clean verify
```

26 tests use test-only H2 and stub HTTP game/wallet servers; Docker is not required.
