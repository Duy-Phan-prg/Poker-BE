# Matchmaking Service — bằng chứng nghiên cứu và phạm vi áp dụng

File này không coi blog hoặc documentation của hãng là “paper nghiên cứu”. Nguồn được
chia thành ba mức để không đánh tráo bằng chứng:

1. **Empirical paper**: có dữ liệu game/người chơi thật.
2. **Theoretical paper**: có mô hình toán hoặc chứng minh, nhưng không phải dữ liệu
   Poker-BE.
3. **Engineering reference**: tài liệu chính thức để chọn cấu trúc triển khai; không
   chứng minh thuật toán matchmaking tốt hơn.

`docs/SPEC.md` vẫn là yêu cầu nghiệp vụ cao nhất. Paper giúp chọn và giải thích cách
làm, không được dùng để tự ý thay đổi ownership hoặc contract trong SPEC.

## 1. Paper có dữ liệu game/người chơi thật

### 1.1. Véron, Marin và Monnet — 28 triệu game sessions

**Paper:** *Matchmaking in Multi-player On-line Games: Studying User Traces to
Improve the User Experience*, NOSSDAV 2014.

- DOI: https://doi.org/10.1145/2578260.2578265
- Bản PDF của tác giả:
  https://pages.lip6.fr/Olivier.Marin/Publis/nossdav14.pdf
- Dữ liệu: public traces từ máy chủ League of Legends, hơn **28 triệu game sessions**.
- Paper cho biết họ công bố database để nghiên cứu; cần kiểm tra lại khả năng tải tại
  thời điểm triển khai vì liên kết/dịch vụ cũ có thể không còn hoạt động.

**Điều paper hỗ trợ:**

- Rank/skill ảnh hưởng đến chất lượng ghép và thời gian chờ.
- Có trade-off thực tế: thu hẹp điều kiện để tăng độ tương đồng sẽ làm một số nhóm,
  đặc biệt vùng rank hiếm, chờ lâu hơn.
- Cần đo server response, queue time và matching precision từ trace thật thay vì chỉ
  chọn tham số theo cảm giác.

**Giới hạn khi áp dụng:** đây là LoL hai đội, không phải Poker nhiều người. Paper không
chứng minh rằng cửa sổ `100 Elo`, tăng `50` mỗi `15 giây` là đúng cho Poker-BE.

### 1.2. Claypool và cộng sự — dữ liệu khách quan + cảm nhận người chơi

**Paper:** *Surrender at 20? Matchmaking in League of Legends*, IEEE Games,
Entertainment, Media Conference, 2015.

- Trang tác giả: https://web.cs.wpi.edu/~claypool/papers/lol-matchmaking/
- PDF: https://web.cs.wpi.edu/~claypool/papers/lol-matchmaking/paper.pdf
- Dữ liệu: user study hơn **50 trận**, gồm rank/queue time và khảo sát cảm nhận.
- Kết quả được báo cáo: median queue time dưới 60 giây trong mẫu; hơn 80% trận có
  average team rank lệch không quá một rank, nhưng cảm nhận công bằng của người chơi
  không hoàn toàn khớp với chỉ số rank.

**Điều paper hỗ trợ:** không nên dùng mỗi Elo spread để tuyên bố trận “tốt”. Sau MVP
cần thêm outcome metric và feedback/acceptance metric nếu nhóm muốn đánh giá trải
nghiệm.

**Giới hạn:** mẫu nhỏ và là game 5v5. Không dùng kết quả này để suy ra tham số queue
cụ thể cho Poker-BE.

### 1.3. Chen và cộng sự — EOMM trên dữ liệu thật của EA

**Paper:** *EOMM: An Engagement Optimized Matchmaking Framework*, WWW 2017.

- DOI: https://doi.org/10.1145/3038912.3052559
- PDF: https://fuxiailab.github.io/EOMM/WWW2017_EOMM.pdf
- Dữ liệu: mô phỏng dựa trên dữ liệu thật của một PvP game của EA; dataset gốc không
  được paper công bố để Poker-BE tái lập độc lập.
- Paper báo cáo churn risk thay đổi theo chuỗi kết quả gần đây và dùng player state,
  skill model, churn prediction cùng graph matching.

**Điều có thể học:** win/loss streak có thể là một tín hiệu trạng thái động; phải đo
định lượng hiệu quả của policy.

**Không áp dụng:** Poker-BE không tối ưu churn, spending hoặc cố tình tạo trận lệch để
giữ chân người chơi. SPEC chỉ yêu cầu Elo và chuỗi thắng; streak trong MVP chỉ là
tie-break có giới hạn và cần metric kiểm chứng.

## 2. Paper mô hình hóa fairness và waiting time

### 2.1. Wu — Dynamic Matching with Teams

**Paper:** Qingyun Wu, *Dynamic Matching with Teams*, Operations Research Letters,
50(5), 2022, trang 618–622.

- DOI: https://doi.org/10.1016/j.orl.2022.09.002
- PDF tác giả: https://qingyunwu.com/research/Dynamic%20Matching%20With%20Teams.pdf
- Phương pháp: mô hình dynamic matching/Markov decision process cho người chơi đến
  tuần tự; mục tiêu cân bằng độ lệch đội và chi phí chờ.

**Điều paper hỗ trợ:** ghép ngay khi đủ người không luôn tối ưu. Chờ để pool “dày” hơn
có thể tăng fairness, nhưng phải đánh đổi waiting cost. Vì vậy Matchmaking phải theo
dõi đồng thời `queue_wait_seconds` và `matched_skill_spread`.

**Giới hạn:** kết quả là mô hình lý thuyết và hai đội; không cung cấp trực tiếp công
thức cửa sổ Elo cho Poker-BE.

Graph liên quan trên Connected Papers:
https://www.connectedpapers.com/main/43627a7730008045a41a7c6d756dc538df38fc95

### 2.2. Xu, Yu và Wu — queueing theory cho matchmaking

**Paper:** *Rule Designs for Optimal Online Game Matchmaking*, Chinese Control
Conference 2021.

- DOI: https://doi.org/10.23919/CCC52363.2021.9549977
- Preprint: https://arxiv.org/abs/1911.11852
- PDF tác giả:
  https://mingkuan.taichi.graphics/publication/2021-matchmaking/matchmaking.pdf
- Phương pháp: Continuous-Time Markov Chain/queueing theory cho game 2v2; phân tích
  expected waiting time và variance giữa các service order/rule design.

**Điều paper hỗ trợ:** mean wait chưa đủ; cần đo cả phân phối/variance hoặc p95 để
tránh một nhóm bị “đói”. FIFO/tuổi ticket là một tiêu chí cần giữ khi tối ưu pool.

**Giới hạn:** mô hình 2v2 và giả định arrival process; không chứng minh oldest-anchor
là duy nhất hoặc tối ưu cho Poker nhiều người.

### 2.3. Emek, Kutten và Wattenhofer — matching có chi phí trì hoãn

**Paper:** *Online Matching: Haste Makes Waste!*, ACM STOC 2016.

- Preprint: https://arxiv.org/abs/1603.03024
- Phương pháp: min-cost perfect matching with delays; tổng cost gồm khoảng cách giữa
  request được ghép và thời gian mỗi request phải đợi.

**Điều paper hỗ trợ:** quality distance và waiting time phải nằm trong cùng bài toán,
không thể tối ưu độc lập. Đây là cơ sở lý thuyết cho việc cửa sổ skill thay đổi theo
tuổi ticket.

**Giới hạn:** metric-space matching tổng quát, không phải thí nghiệm game Poker.

## 3. Paper về skill rating

### 3.1. TrueSkill và TrueSkill 2

1. Herbrich, Minka, Graepel — *TrueSkill: A Bayesian Skill Rating System*, 2006:
   https://www.microsoft.com/en-us/research/wp-content/uploads/2006/01/TR-2006-80.pdf
2. Minka, Cleven, Zaykov — *TrueSkill 2*, 2018:
   https://www.microsoft.com/en-us/research/wp-content/uploads/2018/03/trueskill2.pdf

Hai paper này nghiên cứu **ước lượng skill**, không phải Redis queue, Saga hay Outbox.
Chúng cho thấy skill nên được biểu diễn cùng độ bất định và có thể dùng để ước lượng
match quality.

**Quyết định của dự án:** không cài TrueSkill trong Matchmaking vì:

- SPEC giao Elo cho `ranking-service` và cấm Matchmaking tự tính Elo.
- Thêm TrueSkill tạo nguồn skill thứ hai, dễ mâu thuẫn với BXH và contract hiện tại.
- Chưa có dataset Poker-BE để huấn luyện/kiểm định mô hình mới.

Matchmaking chỉ đọc snapshot `{elo, winStreak, lossStreak}` từ Ranking. Nếu sau này
đổi rating model thì thay tại Ranking và version contract, không nhét model vào queue.

## 4. Paper về giao dịch phân tán

### 4.1. Saga cho debit/refund

**Paper:** Hector Garcia-Molina và Kenneth Salem, *Sagas*, ACM SIGMOD 1987.

- DOI: https://doi.org/10.1145/38713.38742
- Ý chính áp dụng: một giao dịch dài được tách thành các bước; mỗi bước đã commit có
  compensating transaction nếu bước sau thất bại.

Flow Rank được mô hình hóa như sau:

```text
reserve -> accept all -> debit P1 -> ... -> debit Pn -> create table
                             failure
                                |
                                v
                 refund every successful debit -> requeue
```

`wallet_operations` phải lưu trạng thái bền vững để service restart vẫn tiếp tục
compensation. Đây mới là áp dụng Saga; chỉ `catch` exception rồi gọi refund một lần
không đủ.

## 5. Engineering references — không phải paper matchmaking

Các nguồn sau chỉ giải thích công nghệ/pattern triển khai:

| Quyết định kỹ thuật | Nguồn chính thức | Bằng chứng mà nguồn cung cấp |
|---|---|---|
| Redis Sorted Set | https://redis.io/docs/latest/develop/data-types/sorted-sets/ | Member duy nhất, có score và được giữ theo thứ tự |
| Atomic reservation bằng Lua | https://redis.io/docs/latest/develop/programmability/eval-intro/ | Nhiều thao tác Redis được thực thi nguyên tử trong script |
| Idempotent retry | https://aws.amazon.com/builders-library/making-retries-safe-with-idempotent-APIs/ | Cùng một ý định retry phải dùng cùng client request ID |
| Transactional outbox | https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html | Business state và event record được giữ nhất quán trong DB |
| Publisher confirm | https://www.rabbitmq.com/docs/confirms | Broker xác nhận publish; khác với consumer acknowledgement |

Vì vậy phát biểu đúng là:

- Chọn Sorted Set vì đặc tính dữ liệu phù hợp queue có thứ tự, **không phải vì một
  paper chứng minh Sorted Set tối ưu cho matchmaking**.
- Chọn Outbox/confirm vì cần độ tin cậy event, **không phải để tăng chất lượng ghép**.
- Idempotency key theo SPEC; tài liệu Amazon giải thích cách retry an toàn.

## 6. Bảng truy nguyên quyết định

| Quyết định trong code | Bắt buộc từ SPEC | Paper hỗ trợ | Phần chưa được chứng minh |
|---|---|---|---|
| Ghép Rank theo Elo + streak | Có | Véron 2014; EOMM 2017 | Trọng số streak cho Poker-BE |
| Nới Elo khi chờ lâu | Có | Wu 2022; Emek 2016 | Hàm nới và tham số cụ thể |
| Ưu tiên người chờ lâu | Đề xuất thiết kế | Xu 2021 | Oldest-anchor có tối ưu với traffic thật hay không |
| Proposal 10 giây | Có | Không cần paper | 10 giây có phù hợp UX thật hay không |
| Cooldown 2 phút | Đề xuất trong SPEC | Chưa có bằng chứng phù hợp | Ảnh hưởng abandon/requeue |
| Debit/refund | Có | Saga 1987 | Retry/timeout contract thực tế của Wallet |
| Redis Sorted Set | Không chỉ định cấu trúc | Không | Benchmark với traffic Poker-BE |
| Outbox RabbitMQ | Không chỉ định pattern | Không phải matchmaking research | Throughput/retry interval phù hợp |

## 7. Dữ liệu Poker-BE phải tự thu thập

Paper ngoài không thể cho tham số tối ưu vì Poker-BE có số người/trận, phân phối Elo,
traffic và UX riêng. Trước khi chốt `initialEloGap`, `gapIncrement` và
`expansionInterval`, cần log ẩn danh:

```text
queue_entry_id
mode
elo_bucket
win_streak_bucket
joined_at
matched_at / cancelled_at
proposal_result
matched_elo_min / matched_elo_max
table_created
match_duration
finish_position
```

Không log display name, access token hoặc dữ liệu nhạy cảm.

Metric tối thiểu:

- queue wait p50/p90/p95/p99 theo mode và Elo bucket;
- tỷ lệ cancel trước khi được ghép;
- Elo spread p50/p95 của nhóm đã ghép;
- proposal accept/decline/timeout rate;
- tỷ lệ debit/refund/reconciliation;
- tỷ lệ người chơi quay lại queue sau trận.

Quy trình tuning:

1. Khởi đầu bằng config bảo thủ, ghi rõ là baseline kỹ thuật.
2. Replay traffic hoặc chạy discrete-event simulation bằng distribution đã thu được.
3. So sánh nhiều policy trên cùng input: fixed window, linear widening, step widening.
4. Chọn policy theo Pareto giữa wait p95 và Elo spread p95.
5. A/B test nhỏ nếu dự án có đủ người dùng; rollback được bằng config.

Các số `100 Elo`, `+50/15 giây` hiện tại **không phải kết quả paper và không phải dữ
liệu Poker-BE**. Chúng chỉ được dùng làm baseline dev cho đến khi có trace thật.

