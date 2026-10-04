# 07 — Cấu hình môi trường sau M1

M1 đã có AppConfig đọc file ngoài WAR/biến môi trường, JDBI/pool, script và trang khởi động. Hướng dẫn chi tiết ở [README](../README.md), [database/README](../database/README.md) và [báo cáo M1](reports/M1-nen-tang.md).

Máy đã xác minh JDK 17.0.12, wrapper 3.9.6, installation Tomcat 10.1.48 ở `D:\cong_cu_nen\apache-tomcat-10.1.48`. WAMP có MySQL 9.1.0/port cấu hình 3306 và service MariaDB khác; chưa có credential/schema ứng dụng trên WAMP. SQL/JDBI/HTTP được thử bằng MySQL instance riêng port 13316/schema c2c_m1_test/datadir `.local-test/mysql-data`, Tomcat CATALINA_BASE riêng port 18080. Không truy cập hoặc sửa dữ liệu WAMP.

## Thông tin còn cần trên máy người dùng

- Host/port của MySQL đã chọn; tên schema riêng (đề xuất c2c_demo); user/password setup có quyền DDL và user ứng dụng có quyền DML.
- Nếu tên schema đã có dữ liệu: backup, khảo sát và mapping trước; không chạy schema trống/auto baseline/reset.
- Đường dẫn Tomcat/CATALINA_BASE, cổng HTTP và context; APP_CONFIG_FILE/biến môi trường phải truyền vào tiến trình Tomcat/IntelliJ/service.
- MySQL 8.4 LTS chưa được kiểm thử trực tiếp; WAMP 9.1 có thể dùng sau setup schema riêng. Không hạ cấp instance WAMP tại chỗ.

## Cấu hình được hỗ trợ

| Biến | Khóa properties | Công dụng |
| --- | --- | --- |
| APP_CONFIG_FILE | Không có | Chọn file cục bộ; Java system property c2c.config ưu tiên hơn. Nên dùng đường dẫn tuyệt đối; tương đối tính từ working directory Java |
| DB_URL | db.url | JDBC MySQL URL, không chứa credential |
| DB_USERNAME | db.username | User DB |
| DB_PASSWORD | db.password | Secret; không log/commit |
| DB_POOL_MAXIMUM_SIZE | db.pool.maximumSize | 1–20, mặc định 5 |
| DB_CONNECTION_TIMEOUT_MS | db.pool.connectionTimeoutMs | 1000–30000 ms, mặc định 5000 |
| APP_DIAGNOSTICS_ENABLED | app.diagnostics.enabled | false mặc định; chỉ bật khi dev/loopback |

Environment ưu tiên file. File mẫu `config/application.example.properties` không tự được đọc; copy thành `.local.properties` đã ignore hoặc lưu ngoài repository. Không cấu hình DB: trang khởi động vẫn chạy, health chỉ báo NOT_CONFIGURED khi bật diagnostics. Config thiếu một phần/sai báo lỗi cấu hình; không fallback tới credential mặc định. Health query thất bại trả UNAVAILABLE/503 và không tiết lộ cause/URL/password.

Ngày 04/10/2026: khắc phục thông báo chung của DatabaseTool. Lệnh CLI cần đủ ba khóa `db.url`, `db.username`, `db.password` (hoặc biến tương ứng); file local cũng phải được chọn qua APP_CONFIG_FILE/c2c.config. Nếu selector được đặt nhưng rỗng hoặc file không đọc được, lỗi nêu đường dẫn/selector. Lỗi khóa/numeric/range/boolean chỉ nêu tên khóa và điều kiện, không in giá trị hay cause. Hướng dẫn migrate và bảng điều kiện đầy đủ tại [README mục 2–3](../README.md). File local được tạo từ mẫu nếu thiếu, không ghi đè cấu hình cũ; còn cần người dùng điền credential MySQL thật.

Không chạy migration/seed khi deploy. Thực hiện bằng DatabaseTool với user setup riêng, sau đó đổi sang user DML. Kết nối đặt session UTC; UI UTF-8. Password demo BCrypt 2b/cost 12 chỉ dùng fixture phát triển; chưa có chức năng login.
