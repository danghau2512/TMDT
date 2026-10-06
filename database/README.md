# Script và migration MySQL

**Trạng thái mới 05/10/2026:** V004 đã áp sau backup trên c2c_demo standalone V003; giữ nguyên 25 bảng cũ, review_images trống. Hiện 26 bảng nghiệp vụ (27 với Flyway history). Không SOURCE V004 lại, không auto baseline/migrate/seed trên schema standalone. [Hướng dẫn migration mới](../docs/14-demo-bon-nang-cap.md), [bằng chứng](../docs/reports/Bon-nang-cap-trai-nghiem.md). Các số lượng M1 bên dưới là baseline lịch sử.


**Các phiên bản sau baseline:** V002 thêm VNPAY, V003 thêm chat. `schema.sql` vẫn là baseline V001 (22 bảng); cài mới bằng Flyway áp cả ba phiên bản cho 25 bảng nghiệp vụ + history. `c2c_demo` standalone đã nâng V002/V003 sau backup, không chạy SOURCE hoặc seed lại. Đọc [hướng dẫn chat/migration](../docs/13-demo-chat-mua-ban.md) trước khi nâng một bản cài khác.

**Nâng cấp VNPAY (05/10/2026):** dùng V002 sau V001. Schema do Flyway quản lý chạy migrate sau backup; schema standalone không có history dùng quy trình SOURCE một lần đã mapping. `c2c_demo` của máy này đã áp V002, không chạy lại. [Quy trình đầy đủ](../docs/12-vnpay-sandbox.md#2-migration-và-dữ-liệu).


| File | Công dụng |
| --- | --- |
| `database/create-database.sql` | Tạo c2c_demo nếu chưa tồn tại, không drop/reset |
| `src/main/resources/db/schema.sql` | 22 bảng cho schema **trống** |
| `src/main/resources/db/migration/V001__initial_schema.sql` | Bản V001 tương đương schema.sql tại M1 |
| `src/main/resources/db/seed.sql` | Fixture demo, không ghi đè dữ liệu |

Không nhúng credential DB. InnoDB, utf8mb4_0900_ai_ci, DECIMAL(20,2), DATETIME(6)/UTC. FK mặc định RESTRICT/NO ACTION, không cascade giao dịch. Luật liên bảng như review phải COMPLETED/seller đúng item/tổng bằng sum do Service kiểm ở các mốc sau.

## Khuyến nghị: DatabaseTool

Tạo schema riêng và thiết lập APP_CONFIG_FILE theo README gốc; chạy `compile exec:java -Dexec.args=migrate`, `seed`, `check`. Flyway ghi checksum/version trong `flyway_schema_history` (bảng hạ tầng thứ 23, vẫn 22 bảng nghiệp vụ).

Không baseline tự động, không clean; schema có bảng nhưng chưa có history bị từ chối thay vì tự coi là tương thích. Migration dùng credential DDL; web dùng credential DML. Seed dùng transaction/named lock, guard ID seller và rollback khi lỗi. CLI/web không in JDBC cause chứa credential; lỗi phải kiểm endpoint/quyền/version/schema cục bộ.

## Chạy SQL trực tiếp

Chỉ dành schema mới trống chưa dùng Flyway. Mở MySQL client từ repository root:

```powershell
$mysqlExe = 'C:\wamp64\bin\mysql\mysql9.1.0\bin\mysql.exe'
& $mysqlExe --protocol=TCP --host=127.0.0.1 --port=3306 --user=YOUR_SETUP_USER --password --default-character-set=utf8mb4
```

```sql
SOURCE database/create-database.sql;
USE c2c_demo;
SHOW TABLES;
-- Chỉ SOURCE schema nếu SHOW TABLES trống.
SOURCE src/main/resources/db/schema.sql;
SHOW TABLES;
```

Seed an toàn nhất qua DatabaseTool seed, kể cả khi schema tạo thủ công. Nạp trực tiếp cần bắt đầu transaction; chỉ COMMIT khi **không có lỗi**, lỗi thì ROLLBACK. Client interactive có thể tiếp tục sau lỗi nên không commit mù quáng:

```sql
START TRANSACTION;
SOURCE src/main/resources/db/seed.sql;
-- Thành công hoàn toàn mới COMMIT; lỗi dùng ROLLBACK.
COMMIT;
```

Seed chạy lại giữ dữ liệu đã có. Kiểm ID fixture và seller trước khi dùng trên schema có dữ liệu khác; không chạy thủ công đồng thời. Không dùng --force/REPLACE/DROP/TRUNCATE để vượt lỗi. Khi trùng ID user/category, script lỗi và transaction rollback; ID product bị seller khác chiếm thì DatabaseTool từ chối, script không thêm ledger cho seller không đúng fixture.

Schema chạy lại báo bảng tồn tại, không IF NOT EXISTS che schema lệch. Nếu áp schema thủ công thì không chạy V001 lại. Chuyển sang Flyway cần so sánh schema với V001, backup và lập baseline có người phụ trách xác nhận; M1 không cung cấp lệnh baseline tự động.

## Database có dữ liệu

Khảo sát SHOW CREATE TABLE/engine/charset/FK/index/count/orphan/duplicate/snapshot và backup có thể khôi phục. Không chạy schema trống. Có mapping cấu trúc thực tế mới viết V002/V003 ALTER/backfill/constraint và thử bản sao. Chưa có schema legacy dự án nên không tạo script ALTER giả định.

V001 đã áp thì không sửa checksum/nội dung để chạy lại; thêm migration mới. schema.sql là baseline tại M1; nếu thay baseline về sau phải cập nhật test tương ứng, không sửa V001 đã publish. MySQL DDL implicit commit: lỗi giữa chừng có thể giữ bảng đã tạo; khảo sát/khắc phục trên schema riêng, không tự reset dữ liệu.

## Kiểm tra

```sql
SELECT VERSION(), DATABASE(), @@session.time_zone;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE();
SELECT email,role,status FROM users ORDER BY id;
SELECT seller_id,COUNT(*) FROM products GROUP BY seller_id;
SELECT COUNT(*) FROM stock_movements;
```

Qua Flyway: 23 bảng; raw schema: 22. Seed mới: 5 user, 4 category, 4 product, 4 initial ledger, 4 moderation event; tin HIDDEN/PENDING. Xem README gốc cho credential demo; chưa có login UI.
