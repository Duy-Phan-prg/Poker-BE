# matchmaking-service

Service ghép trận tại port `8083`. Trạng thái hiện tại: hàng chờ `NORMAL` đã có code;
`RANK`, proposal accept và Party chưa được bật.

## Swagger / OpenAPI

Khi service đang chạy và `MATCHMAKING_SWAGGER_ENABLED=true` (mặc định ở local):

- Swagger UI: `http://localhost:8083/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8083/v3/api-docs`

Swagger test trực tiếp service nên phải nhập `X-User-Id`; trong luồng thật header này do Gateway
lấy từ JWT và gắn vào request. Swagger hiện mô tả bốn API của flow `NORMAL`. Nó chưa
bao phủ Rank, proposal accept/decline/timeout, Wallet, notification/outbox, Party hoặc kiểm thử
E2E qua Gateway. Có Swagger không đồng nghĩa các phần còn thiếu này đã được triển khai.

Phân nhóm nghiệp vụ: `Normal matchmaking` là nhóm API hiện có; `Rank matchmaking` sẽ có endpoint
riêng khi Đợt 3–5 hoàn thành. `Custom room` thuộc `competition-service`, không thuộc Swagger của
Matchmaking.

## API Normal

Danh tính luôn lấy từ header `X-User-Id` do Gateway gắn.

```http
POST /api/matchmaking/normal/join
X-User-Id: <account UUID>
```

```http
GET /api/matchmaking/normal/status
X-User-Id: <account UUID>
```

```http
DELETE /api/matchmaking/normal/leave
X-User-Id: <account UUID>
```

```http
GET /api/matchmaking/normal/reconnect
X-User-Id: <account UUID>
```

Join bị từ chối nếu account đã ở Redis queue, có proposal `CREATING_TABLE` trong PostgreSQL hoặc
Game service báo vẫn đang ở một bàn active. Leave xóa account khỏi Redis queue và reset countdown
nếu số người còn lại thấp hơn `minPlayers`.

Khi tạo bàn thành công, Matchmaking lưu `IN_GAME`, `proposalId`, `tableId`, `matchId`, host,
port và WebSocket path vào Redis theo từng account. Reconnect chỉ trả
`reconnectable=true` khi Redis có `IN_GAME` và Game service vẫn xác nhận đúng bàn đó. Record
Redis bị stale sẽ được xóa và API trả `reconnectable=false`.

Trạng thái có thể là `NONE`, `WAITING`, `MATCHING`, `CREATING_TABLE`, `READY` hoặc
`FAILED`. Normal không gọi Ranking/Wallet và không có popup accept.

## Cách hoạt động

1. Join ghi entry vào Redis Sorted Set theo `joinedAt`.
2. Worker lấy số người cũ nhất theo `MATCHMAKING_NORMAL_MIN_PLAYERS`.
3. Lua reserve cả nhóm nguyên tử để hai worker không lấy trùng account.
4. PostgreSQL lưu proposal `CREATING_TABLE` và danh sách player.
5. Gọi `POST game-service/internal/tables` với `mode=NORMAL`, `entryFee=0` và
   `sourceId=proposalId`.
6. Thành công chuyển `READY`; lỗi 4xx chắc chắn thì `FAILED` và requeue.
7. Timeout/5xx giữ `CREATING_TABLE`; recovery retry cùng `sourceId` để không tạo hai bàn.

## Biến môi trường chính

| Biến | Mặc định |
|---|---|
| `SERVER_PORT` | `8083` |
| `MATCHMAKING_NORMAL_MIN_PLAYERS` | `2` |
| `MATCHMAKING_NORMAL_MAX_PLAYERS` | `6` |
| `MATCHMAKING_NORMAL_FILL_TIMEOUT` | `10s` |
| `GAME_PUBLIC_HOST` | `localhost` |
| `GAME_PUBLIC_PORT` | `8082` |
| `GAME_WEBSOCKET_PATH` | `/ws/game` |
| `GAME_SESSION_TTL` | `24h` |
| `MATCHMAKING_FINDER_DELAY_MS` | `1000` |
| `MATCHMAKING_QUEUE_ENTRY_TTL` | `24h` |
| `MATCHMAKING_RESERVATION_TTL` | `2m` |
| `MATCHMAKING_GAME_TIMEOUT` | `3s` |
| `MATCHMAKING_GAME_STUB_ENABLED` | `false` |

Stub Game chỉ dùng khi phát triển độc lập:

```powershell
$env:MATCHMAKING_GAME_STUB_ENABLED='true'
```

## Build/test

Yêu cầu Java 21:

```powershell
.\mvnw.cmd -pl matchmaking-service -am clean verify
```

Nếu Windows đang trỏ `java` về Java 8, đặt `JAVA_HOME` tới JDK/JBR 21 trước khi chạy.

## Docker

```powershell
docker compose -f docker/compose.infra.yml -f matchmaking-service/compose.yml up -d --build
```

Compose dùng PostgreSQL `matchmaking-db`, Redis riêng `matchmaking-redis` và RabbitMQ
chung từ `docker/compose.infra.yml`.
