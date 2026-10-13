# matchmaking-service

Service ghép trận tại port `8083`. Trạng thái hiện tại: hàng chờ `NORMAL` đã có code;
`RANK`, proposal accept và Party chưa được bật.

## API Normal

Danh tính luôn lấy từ header `X-User-Id` do Gateway gắn.

```http
POST /api/matchmaking/queue
X-User-Id: <account UUID>
Content-Type: application/json

{"mode":"NORMAL"}
```

```http
GET /api/matchmaking/status
X-User-Id: <account UUID>
```

```http
DELETE /api/matchmaking/queue
X-User-Id: <account UUID>
```

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
