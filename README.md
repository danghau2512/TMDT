# Nền tảng C2C — Đồ án TMĐT nhóm 6

**M1 đã triển khai:** Java Servlet/JSP, SQL cho 22 bảng, seed demo, MySQL/JDBI/HikariCP, migration và trang khởi động. Chưa có chức năng đăng ký/đăng nhập, đăng bán, giỏ hàng hay xử lý đơn.

## Môi trường

| Thành phần | Phiên bản dùng |
| --- | --- |
| Java / Maven | Java 17 (máy kiểm tra 17.0.12); Wrapper Maven 3.9.6, WAR |
| Tomcat / Servlet | Tomcat 10.1.x / jakarta.servlet-api 6.0.0 (provided); kiểm tra trên 10.1.48 |
| MySQL | Mục tiêu 8.4 LTS; đã thử trên instance riêng MySQL 9.1.0 |
| JDBI / pool / driver | JDBI 3.55.0, HikariCP 7.0.2, Connector/J 9.5.0 |
| View / hash / migration | JSP/JSTL 3.0.x, BCrypt 2b cost 12, Flyway 11.8.2 |

Chỉ dùng `jakarta.servlet`. [Tomcat 10.1 thực hiện Servlet 6.0](https://tomcat.apache.org/tomcat-10.1-doc/); [Connector/J 9.5 hỗ trợ MySQL 8.0 trở lên](https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-5-0.html).

## 1. Chuẩn bị MySQL trên Windows

Mở PowerShell ở thư mục dự án. Nếu dùng WAMP đã khảo sát:

```powershell
$mysqlExe = 'C:\wamp64\bin\mysql\mysql9.1.0\bin\mysql.exe'
& $mysqlExe --protocol=TCP --host=127.0.0.1 --port=3306 --user=YOUR_SETUP_USER --password --default-character-set=utf8mb4
```

Thay `YOUR_SETUP_USER` bằng user có quyền setup. `--password` yêu cầu nhập kín, không đặt mật khẩu trong lệnh. Chọn đúng MySQL, không dùng service MariaDB. Trong MySQL client:

```sql
SELECT VERSION();
SHOW DATABASES LIKE 'c2c_demo';
SOURCE database/create-database.sql;
USE c2c_demo;
SHOW TABLES;
```

Script chỉ CREATE DATABASE IF NOT EXISTS, không DROP/reset. Nếu `c2c_demo` có dữ liệu, dừng việc áp schema trống, sao lưu/khảo sát theo [hướng dẫn migration](database/README.md). Chuẩn bị user ứng dụng riêng có quyền SELECT/INSERT/UPDATE/DELETE trên `c2c_demo.*`; dùng credential setup có quyền DDL khi chạy migration, sau đó đổi cấu hình sang user ứng dụng. Tạo user/mật khẩu DB cục bộ bởi người có quyền; không có tài khoản DB thật được nhúng trong dự án.

## 2. Cấu hình cục bộ

```powershell
Copy-Item config\application.example.properties config\application.local.properties
$env:APP_CONFIG_FILE = (Resolve-Path config\application.local.properties).Path
notepad $env:APP_CONFIG_FILE
```

Sửa `db.url`, `db.username`, `db.password` bằng thông tin DB của bạn. File `*.local.properties` đã ignore, không được đóng gói vào WAR. Có thể lưu ngoài repository; APP_CONFIG_FILE phải là đường dẫn tuyệt đối.

Biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_MAXIMUM_SIZE`, `DB_CONNECTION_TIMEOUT_MS`, `APP_DIAGNOSTICS_ENABLED` ưu tiên hơn file. Không đặt credential trong JDBC URL. File mẫu không tự được load; code không load secret từ classpath.

Để kiểm tra trên máy phát triển, đặt `app.diagnostics.enabled=true`. `/health/db` chỉ trả lời khi cờ này bật **và request từ loopback**. Tắt khi chia sẻ/deploy qua proxy. Không cấu hình DB vẫn mở trang khởi động được; đó không phải kết nối DB thành công. Cấu hình dở dang báo lỗi, không fallback sang root/WAMP.

## 3. Chạy schema và seed

Cách khuyến nghị, sau khi đã tạo schema riêng và cấu hình user setup:

```powershell
.\mvnw.cmd -B compile exec:java '-Dexec.args=migrate'
.\mvnw.cmd -B compile exec:java '-Dexec.args=seed'
.\mvnw.cmd -B exec:java '-Dexec.args=check'
```

- `migrate` chạy V001, tạo 22 bảng nghiệp vụ và `flyway_schema_history`. Chạy lại trả số migration 0 nếu không có migration mới. Không baseline tự động database có bảng sẵn, không clean/reset; không tự chạy khi deploy.
- `seed` chạy [seed.sql](src/main/resources/db/seed.sql) trong transaction + named lock. Chạy lại không nhân đôi, không reset hồ sơ/giá/kho/hash đã tồn tại. ID fixture ghi rõ trong script; xung đột cần khảo sát, không REPLACE/INSERT IGNORE.
- `check` in `JDBI SELECT 1 = 1` khi thành công. Lỗi trả exit code thất bại với thông báo không chứa credential.

[schema.sql](src/main/resources/db/schema.sql) là script standalone cho schema trống, giống V001 tại M1. Nếu chạy bằng SOURCE, xem [database/README.md](database/README.md); không chạy cả schema standalone và V001 trên cùng schema. MySQL DDL có implicit commit; lỗi giữa chừng không tự rollback toàn schema, không DROP để chạy lại.

## Tài khoản demo

**Chỉ dành cho database phát triển/demo**, mật khẩu chung `C2cDemo!2026`; mỗi hash có salt khác, BCrypt `$2b$12$`. Không có thông tin đăng nhập thật.

| Email | Role/ngữ cảnh |
| --- | --- |
| admin@c2c.example | ADMIN |
| seller1@c2c.example | USER, người bán An |
| seller2@c2c.example | USER, người bán Bình |
| buyer1@c2c.example | USER, người mua Chi |
| buyer2@c2c.example | USER, người mua Dũng |

USER vừa mua vừa bán; seller/buyer chỉ là tên fixture. **M1 chưa có login UI**; các tài khoản này phục vụ M2 trở đi. Seed có 4 danh mục/4 sản phẩm của 2 seller, stock 3/5/4/1, 4 INITIAL ledger và 4 sự kiện PENDING. Tin HIDDEN/PENDING vì chưa có ảnh thật; bổ sung ảnh/duyệt ở M3. Chưa seed đơn/review/khiếu nại không có nguồn giao dịch.

## 4. Build và deploy Tomcat

```powershell
.\mvnw.cmd -B clean verify
$env:CATALINA_HOME = 'D:\cong_cu_nen\apache-tomcat-10.1.48' # sửa theo máy bạn
# Nếu có CATALINA_BASE riêng, deploy vào CATALINA_BASE\webapps.
Copy-Item target\demo-1.0-SNAPSHOT.war "$env:CATALINA_HOME\webapps\c2c.war"
& "$env:CATALINA_HOME\bin\catalina.bat" run
```

Chạy Tomcat trong PowerShell đã có APP_CONFIG_FILE hoặc biến DB. Với IntelliJ, đặt biến trong Run Configuration, deploy WAR/exploded; không sửa `.idea/workspace.xml` để chia sẻ secrets. Với Windows service, cấu hình môi trường cho service và restart; biến của terminal không tự truyền tới service.

Theo cổng Tomcat thực tế (mặc định 8080), mở:

- [Trang khởi động](http://localhost:8080/c2c/): Servlet → JSP/JSTL, tiếng Việt.
- [DB health](http://localhost:8080/c2c/health/db): 200/CONNECTED khi SELECT 1 thành công; 503 nếu thiếu/không kết nối DB; 404 khi diagnostics tắt hoặc request ngoài loopback.
- [Servlet mẫu](http://localhost:8080/c2c/hello-servlet): giữ nguyên mẫu ban đầu.

Context có thể là `/demo_war_exploded` trong IntelliJ; link/assets tính theo context. JSP trong WEB-INF không truy cập trực tiếp.

## 5. Kiểm thử

`clean verify` chạy **7 unit tests**, không cần DB. Integration test cần schema riêng hậu tố `_test`, config trỏ đúng schema và user có quyền DDL/DML:

```powershell
# APP_CONFIG_FILE trỏ cấu hình schema test, ví dụ c2c_m1_test.
$env:C2C_IT_ALLOWED = 'true'
.\mvnw.cmd -B -Pmysql-it verify
```

Profile migrate/seed schema test; kiểm 22 bảng/FK/constraint, bind, Handle đóng, transaction nhiều DAO, seed chạy lại, hash demo, snapshot và sổ kho. Không chạy trên `c2c_demo` hoặc dữ liệu thật. Build thường không chạy integration. Nếu console lỗi dấu, dùng UTF-8 và `MAVEN_OPTS=-Dfile.encoding=UTF-8`; UI/DB dùng UTF-8.

## Cấu trúc và luồng M1

```text
controller/ HomeServlet, DatabaseHealthServlet nhận HTTP
service/    HealthService điều phối kiểm tra
dao/        HealthDao: SELECT 1 và ví dụ bind
model/      DatabaseHealth
config/     AppConfig, Database, SchemaManager, DatabaseTool, JdbcLifecycle
listener/   ApplicationListener khởi tạo/đóng pool
filter/     EncodingFilter
security/   LocalDiagnostics, PasswordHasher
resources/db/ schema.sql, seed.sql, migration/V001__initial_schema.sql
webapp/     index.jsp, WEB-INF/views/home.jsp, assets/css/startup.css
```

Servlet gọi Service; Service mở Handle qua Database và attach DAO; DAO dùng JDBI bind/PreparedStatement tới MySQL. Callback kết thúc đóng Handle, trả connection về pool. `Database.transaction(...)` mở READ COMMITTED, các DAO cùng Handle; lỗi rollback toàn transaction. Chưa có transaction nghiệp vụ tạo/hủy đơn. [JDBI transactions](https://jdbi.org/releases/3.55.0/#_transactions).

## Kết quả và giới hạn

Đã build WAR, chạy 7 unit + 11 integration tests và HTTP smoke trên Tomcat 10.1.48/MySQL 9.1.0 riêng. [Báo cáo M1/danh sách file](docs/reports/M1-nen-tang.md) có lệnh/kết quả. MySQL thử port 13316/datadir riêng; Tomcat thử port 18080/CATALINA_BASE riêng; cả hai được dừng sau kiểm tra. Chưa thiết lập `c2c_demo` trên WAMP 3306 vì chưa có credential dự án; chưa thử MySQL 8.4 thực tế. Dữ liệu WAMP không bị thay đổi.

Thiết kế: [docs/README.md](docs/README.md). M2 chưa triển khai. Commit đề xuất: `feat: triển khai nền tảng Servlet, schema C2C và kết nối JDBI`.
