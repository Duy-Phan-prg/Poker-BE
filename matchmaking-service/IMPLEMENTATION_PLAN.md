# Duy — kế hoạch triển khai matchmaking-service

Nguồn yêu cầu: `docs/SPEC.md` §3.1, §3.2, §5, §6.1, §6.2, §6.4,
§7.2, §7.3, §8, §9.3 và §12. Quy tắc chi tiết nằm tại
`matchmaking-service/RULE.md`; khi hai file mâu thuẫn, `docs/SPEC.md` có độ ưu tiên
cao nhất.

Phạm vi file này chỉ là `matchmaking-service` và FE `features/matchmaking`.
`social-service`, `chat-service` và `<ChatPanel>` là các đợt riêng, không đưa entity,
repository hoặc bảng của chúng vào Matchmaking.

## 1. Kết quả cuối cần đạt

### MVP

- Hàng chờ `NORMAL`: vào/hủy hàng chờ, ghép theo thời gian chờ, miễn phí, không gọi
  Ranking/Wallet và không có popup accept theo đề xuất hiện tại của SPEC.
- Hàng chờ `RANK`: lấy Elo/streak từ Ranking, ghép theo Elo, nới cửa sổ Elo khi
  chờ lâu và lấy số người tối thiểu từ config.
- Proposal Rank có hạn 10 giây; mọi người accept mới bắt đầu thu phí.
- Thu phí/hoàn phí qua Wallet bằng idempotency key; không tự sửa số dư.
- Tạo bàn qua Game `POST /internal/tables`; không tự tạo bàn hoặc chạy poker engine.
- Phát `notification.requested` loại `MATCH_FOUND`/`MATCH_READY` qua RabbitMQ để
  Social đẩy realtime.
- FE có màn chờ và popup chấp nhận trận.

### Giai đoạn 2

- Party: tạo, mời, kick, rời/chuyển chủ và chỉ chủ party được tìm trận Normal.
- Ghép party thiếu người với người lạ.

Không triển khai Custom Room, Tournament, Elo, ví, bạn bè hoặc chat trong module này.

## 2. Chuẩn đặt tên phải giữ thống nhất

| Thành phần | Quy ước | Ví dụ |
|---|---|---|
| Java package | chữ thường, theo tầng hiện có | `com.msspoker.matchmakingservice.service` |
| Java class | PascalCase, tên nói rõ vai trò | `RankMatchmakingService`, `ProposalExpiryWorker` |
| Method/field | camelCase, động từ trước | `joinQueue`, `reserveCandidates`, `acceptProposal` |
| REST | danh từ số nhiều, dưới `/api/matchmaking` | `/api/matchmaking/queues/rank` |
| DB table/column | `snake_case` | `match_proposal_players`, `accepted_at` |
| Enum | `UPPER_SNAKE_CASE` | `WAITING_ACCEPTANCE`, `REFUND_PENDING` |
| Error code | tiền tố `MM_` | `MM_ALREADY_QUEUED`, `MM_PROPOSAL_EXPIRED` |
| Redis key | `mm:v1:<loại>:<định danh>` | `mm:v1:queue:rank`, `mm:v1:cooldown:{accountId}` |
| Wallet idempotency | theo đúng SPEC | `rank-fee:{proposalId}:{accountId}` |
| Event | tên event chung của SPEC | exchange `poker.events`, type `notification.requested` |

Không đưa display name vào queue/proposal bền vững. Matchmaking chỉ lưu `accountId`;
tên và avatar thuộc Profile API. Không đổi tên endpoint/event liên service chỉ để đẹp
hơn nếu chưa cập nhật SPEC và được service liên quan review.

## 3. Thuật toán cơ sở cho MVP

### Rank

1. Khi join, kiểm tra cooldown, kiểm tra không có queue/proposal đang hoạt động và gọi
   Ranking lấy `{elo, winStreak, lossStreak}`.
2. Lưu payload tạm trong Redis Hash; đưa `accountId` vào Sorted Set với score là
   `joinedAt` epoch milliseconds.
3. Worker chọn người chờ lâu nhất làm anchor. Cửa sổ hợp lệ là:

   `allowedEloGap = initialGap + floor(waitSeconds / expansionInterval) * gapIncrement`

4. Lọc ứng viên trong cửa sổ Elo của anchor. Elo là tiêu chí chính; streak chỉ là
   tiêu chí phụ để sắp ứng viên có Elo tương đương, không tự biến đổi Elo.
5. Đủ `minPlayers` thì reserve nguyên tử các account. Chỉ một worker được tạo proposal
   cho cùng một account.
6. Ghi proposal và players vào PostgreSQL, đặt deadline 10 giây, phát `MATCH_FOUND`.
7. Tất cả accept: debit từng người, gọi Game, lưu `tableId`/`matchId`, phát
   `MATCH_READY`.
8. Decline/timeout/lỗi: thực hiện compensation, cooldown đúng đối tượng và requeue
   người hợp lệ với `joinedAt` cũ để không làm mất thứ tự chờ.

### Normal

1. Lưu queue bằng Redis, ưu tiên `joinedAt`.
2. Đủ `minPlayers` thì reserve nguyên tử.
3. Gọi Game ngay, không Ranking, không Wallet, không proposal accept.
4. Game lỗi thì release reservation và requeue giữ thời điểm chờ cũ.

Các giá trị `minPlayers`, `initialGap`, `gapIncrement`, `expansionInterval`,
`acceptTimeout` và `cooldown` đều nằm trong `MatchmakingProperties`; không hard-code
trong service/worker.

## 4. Các đợt triển khai

Mỗi đợt phải có test và `mvn -pl matchmaking-service -am clean verify` xanh trước khi
sang đợt tiếp theo.

### Đợt 0 — nền module

- Dependency JPA/PostgreSQL, Flyway, Redis, RabbitMQ, Validation và H2 cho test
  context/JPA đơn giản. Redis/concurrency phải test bằng Testcontainers.
- `application.properties` đọc biến Compose; RabbitMQ dùng broker chung.
- `compose.yml` có PostgreSQL/Redis riêng, không tạo RabbitMQ riêng.
- Migration V1 tạo `match_proposals`, `match_proposal_players`,
  `wallet_operations`, `matchmaking_outbox`.
- Chuẩn lỗi `MatchmakingError`, `MatchmakingApiException`,
  `MatchmakingExceptionHandler`.

Tiêu chí: context test chạy và Flyway validate schema thành công.

### Đợt 1 — domain và persistence

- Tạo enum: `MatchMode`, `ProposalStatus`, `ProposalPlayerStatus`,
  `WalletOperationType`, `WalletOperationStatus`.
- Tạo entity/repository đúng V1; dùng optimistic locking qua `version`.
- Tạo DTO mapper riêng; controller không trả entity JPA.
- Bổ sung repository query tìm proposal hết hạn và wallet operation cần retry.

Tiêu chí: repository test chứng minh unique player/proposal, optimistic lock và các
check constraint quan trọng.

### Đợt 2 — queue core và Normal end-to-end

- `MatchmakingProperties` có validate cho toàn bộ tham số.
- `QueueStore` interface và `RedisQueueStore` implementation.
- Join/cancel/status API; một account chỉ ở một queue/proposal hoạt động.
- Lua script reserve/release/requeue nguyên tử.
- `NormalMatchWorker` lấy nhóm theo thời gian chờ và gọi `GameClient`.
- `GameClient` production và stub dev/test theo interface.

Tiêu chí: hai worker chạy đồng thời không lấy trùng account; Normal không gọi
Ranking/Wallet; Game lỗi thì người chơi trở lại queue đúng thứ tự.

### Đợt 3 — admission và thuật toán Rank

- `RankingClient` lấy Elo/streak; `RestrictionClient` kiểm tra cấm nếu contract đã sẵn.
- Rank join từ chối khi cooldown, đang queued/proposed hoặc dependency trả lỗi.
- Worker anchor người chờ lâu nhất, widening theo config, streak chỉ tie-break.
- Ghi metric wait time và Elo spread của nhóm được chọn.

Tiêu chí: unit test biên cửa sổ Elo, mỗi lần expansion, min players và fairness cho
người chờ lâu; không có class nào tự tính/cập nhật Elo.

### Đợt 4 — proposal accept 10 giây

- Tạo proposal transactionally sau khi reserve.
- API `GET /api/matchmaking/proposals/{id}`,
  `POST .../{id}/accept`, `POST .../{id}/decline`.
- Accept/decline idempotent; account ngoài proposal nhận `403/404` phù hợp.
- `ProposalExpiryWorker` chuyển proposal hết hạn đúng một lần.
- Outbox tạo `MATCH_FOUND` cùng transaction với proposal.

Tiêu chí: race accept-vs-timeout chỉ có một trạng thái cuối; accept sau deadline không
thành công; người timeout/decline bị cooldown theo config.

### Đợt 5 — Wallet saga và Game

- `WalletClient` gọi debit/credit với timeout và retry có giới hạn.
- Ghi `wallet_operations` trước khi gọi dependency; dùng key
  `rank-fee:{proposalId}:{accountId}` và `rank-refund:{proposalId}:{accountId}`.
- Nếu một debit thất bại, hoàn tiền các debit đã thành công.
- Chỉ gọi Game sau khi toàn bộ debit thành công.
- Game lỗi chắc chắn: refund toàn bộ. Game timeout/không rõ kết quả: không refund mù;
  đánh dấu reconciliation pending và kiểm tra lại bằng contract đã thống nhất.

Tiêu chí: test mọi điểm lỗi sau từng debit, retry không trừ/hoàn hai lần và tiến trình
khôi phục được sau khi restart.

### Đợt 6 — event/outbox và quan sát hệ thống

- `OutboxPublisher` lấy batch pending, publish `notification.requested` lên
  `poker.events`, chỉ đánh dấu `published_at` sau publisher confirm.
- Retry exponential backoff có jitter; event có `eventId`, `occurredAt`, type và data.
- Health check phân biệt PostgreSQL/Redis/RabbitMQ và các dependency outbound.
- Metric tối thiểu: queue size, wait p50/p95, Elo spread p50/p95, proposal accept/
  decline/timeout, refund pending, outbox pending/oldest age, duplicate reservation.

Tiêu chí: restart giữa DB commit và publish không mất event; publish trùng có cùng
`eventId` để consumer deduplicate.

### Đợt 7 — FE và kiểm thử tích hợp

- `features/matchmaking`: API client, query/mutation hooks, màn queue và trạng thái lỗi.
- Popup accept Rank hiển thị countdown theo `acceptDeadline` từ server, không tự lấy
  thời điểm mở popup làm deadline.
- Nhận `MATCH_FOUND`/`MATCH_READY` qua kênh realtime của Social; polling chỉ là
  fallback tạm cho dev.
- E2E: Normal thành công, Rank thành công, decline, timeout, debit lỗi, Game lỗi,
  dependency timeout và reconnect.

Tiêu chí: FE build/lint/test xanh; luồng thật chạy qua Gateway, RabbitMQ, PostgreSQL
và Redis bằng Docker Compose.

### Đợt 8 — GĐ2 Party

Chỉ bắt đầu sau khi MVP ổn định và contract Party được cập nhật trong SPEC. Không trộn
Party vào queue core trước đợt này.

## 5. Dependency contract và mock

| Dependency | Contract production | Mock dev/test |
|---|---|---|
| Ranking | `GET /internal/ranking/{accountId}` | Elo/streak bằng `0` |
| Wallet | balance/debit/credit | thành công, vẫn deduplicate key |
| Game | `POST /internal/tables` | UUID ngẫu nhiên; tool dev có thể finish sau 10 giây |
| Identity | restrictions/penalty | không bị phạt |

Mock được chọn bằng Spring profile/bean, không viết `if (dev)` trong business logic và
không fallback âm thầm sang mock ở production.

## 6. Tiến độ hiện tại

- [x] Đợt 0: dependency, cấu hình, Compose, Flyway V1 và exception chuẩn.
- [x] Đợt 1 — phạm vi Normal: entity/repository proposal, DTO và MapStruct mapper;
  context JPA/Flyway chạy qua. Domain Wallet/Rank sẽ bổ sung ở Đợt 3–5.
- [ ] Đợt 2 — phần code đã hoàn thành:
  - [x] API join/cancel/status lấy danh tính từ `X-User-Id`.
  - [x] Trạng thái `NONE`, `WAITING`, `MATCHING`, `CREATING_TABLE`, `READY`, `FAILED`.
  - [x] Redis Sorted Set ưu tiên `joinedAt`; lưu payload queue riêng.
  - [x] Lua reserve nguyên tử để nhiều worker không lấy trùng account.
  - [x] `NormalMatchWorker` lấy nhóm đủ `minPlayers` từ config.
  - [x] Lưu proposal và players vào PostgreSQL trước khi gọi Game.
  - [x] `HttpGameClient` gọi Game với `sourceId = proposalId`; có `StubGameClient` cho dev/test.
  - [x] Game từ chối chắc chắn thì đánh dấu `FAILED` và requeue giữ `joinedAt` cũ.
  - [x] Timeout/5xx giữ `CREATING_TABLE`; recovery retry cùng `sourceId`.
  - [x] Unit/context test và Testcontainers Redis concurrency: 10 test đều pass.
  - [ ] Chạy E2E với PostgreSQL, Redis và Game service thật trong Docker.
- [ ] Đợt 3: Rank admission và matching worker.
- [ ] Đợt 4: proposal accept/decline/timeout.
- [ ] Đợt 5: Wallet saga và Game.
- [ ] Đợt 6: outbox/event/observability.
- [ ] Đợt 7: FE và E2E.
- [ ] Đợt 8: Party GĐ2.

### Cập nhật 2026-10-14 — MapStruct và cấu trúc package

- [x] Thêm MapStruct `1.6.3` và `mapstruct-processor` vào `pom.xml`, cùng phiên bản với
  `game-service`, `ranking-service` và `competition-service`.
- [x] Chuyển `QueueStatusMapper` từ class `@Component` viết tay thành interface
  `@Mapper(componentModel = SPRING)`. Implementation được sinh khi compile tại
  `target/generated-sources/annotations`; không commit file generated.
- [x] Cập nhật unit test lấy mapper bằng `Mappers.getMapper(QueueStatusMapper.class)`.
- [x] Xóa hai package rỗng còn sót `service/queue` và `service/worker`. Implementation worker
  hiện nằm đúng tại `service/impl/worker`; các interface nằm trực tiếp trong `service`.
- [x] Chạy `mvn -pl matchmaking-service -am clean test`: 10 test, 0 failure, 0 error,
  bao gồm context/Flyway, queue service, coordinator, worker và Redis concurrency.
- [ ] Đợt 2 vẫn chưa hoàn tất: còn E2E với PostgreSQL và Game service thật trong Docker.

## 7. Thứ tự code ngay bây giờ

1. Hoàn tất Đợt 2 bằng E2E Normal qua Docker với PostgreSQL, Redis và Game service thật.
2. Kiểm tra DB có proposal/players đúng và retry cùng `sourceId` không tạo hai bàn.
3. Sau khi E2E Normal xanh, bắt đầu Đợt 3: Ranking client → Rank admission → Elo widening
   → Rank worker.
4. Sau Rank matching mới triển khai proposal accept 10 giây, Wallet saga và outbox.

Lý do: Normal kiểm chứng queue, reservation và Game integration mà chưa kéo theo Elo,
deadline và compensation tiền. Khi lõi đó ổn, Rank chỉ bổ sung admission, selection,
proposal và saga lên cùng nền.
