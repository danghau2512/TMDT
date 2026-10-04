# Nền tảng C2C — Đồ án TMĐT nhóm 6

**M1 và M2 đã triển khai:** nền tảng Servlet/JSP, SQL cho 22 bảng, MySQL/JDBI; đăng ký, đăng nhập/đăng xuất, hồ sơ và phân quyền USER/ADMIN. Đăng bán, giỏ hàng và xử lý đơn thuộc các mốc sau.

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
if (-not (Test-Path -LiteralPath .\config\application.local.properties)) {
    Copy-Item .\config\application.example.properties .\config\application.local.properties
}
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
notepad $env:APP_CONFIG_FILE
```

Chỉ copy khi chưa có file để giữ nguyên cấu hình đã điền. Sửa trong Notepad:

- `db.url`: host ở `127.0.0.1`, port ở `3306`; giữ tên database dự án `c2c_demo` hoặc sửa theo schema riêng đã tạo. URL mẫu: `jdbc:mysql://127.0.0.1:3306/c2c_demo?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8`.
- `db.username`: điền user MySQL thực tế có quyền DDL khi migrate. `c2c_app` chỉ là tên mẫu; ứng dụng không tự tạo user này.
- `db.password`: thay giá trị mẫu bằng mật khẩu của user MySQL; không dùng mật khẩu tài khoản demo của website. Không đưa mật khẩu vào lệnh, chat hoặc log.

File `*.local.properties` đã ignore, không được đóng gói vào WAR. Có thể lưu ngoài repository. File được đọc bằng UTF-8 và cú pháp Java Properties: nếu mật khẩu chứa dấu `\`, viết `\\`; khoảng trắng đầu giá trị cần escape bằng `\ `.

**Chọn file:** Java system property `c2c.config` ưu tiên hơn biến `APP_CONFIG_FILE`. Nếu không có cả hai, không file nào được đọc, kể cả `config/application.local.properties` đã tồn tại. Nên dùng đường dẫn tuyệt đối như lệnh `Resolve-Path` trên; đường dẫn tương đối được tính từ thư mục làm việc của tiến trình Java. Đường dẫn được đặt nhưng rỗng, sai hoặc không đọc được sẽ báo lỗi, không fallback. Chỉ có biến `DB_*` cũng đủ để cấu hình, không bắt buộc file khi đã đặt đủ ba biến bắt buộc.

| Khóa trong file | Biến môi trường ưu tiên | Điều kiện kiểm tra tại bộ đọc cấu hình |
| --- | --- | --- |
| `db.url` | `DB_URL` | Bắt buộc; bắt đầu bằng `jdbc:mysql://`, không chứa tham số `user`/`password` trong URL. Host, port, schema và khả năng kết nối được driver/MySQL kiểm tra khi thực hiện lệnh. |
| `db.username` | `DB_USERNAME` | Bắt buộc, không rỗng hoặc chỉ có khoảng trắng. |
| `db.password` | `DB_PASSWORD` | Phải có khóa/biến, không còn giá trị mẫu. Giá trị rỗng chỉ dùng khi user MySQL thực tế không có mật khẩu; không tự suy đoán điều này. |
| `db.pool.maximumSize` | `DB_POOL_MAXIMUM_SIZE` | Số nguyên 1–20; mặc định 5 nếu không đặt. |
| `db.pool.connectionTimeoutMs` | `DB_CONNECTION_TIMEOUT_MS` | Số nguyên 1000–30000 ms; mặc định 5000 nếu không đặt. |
| `app.diagnostics.enabled` | `APP_DIAGNOSTICS_ENABLED` | `true` hoặc `false`, không phân biệt hoa/thường; mặc định `false`. |

Biến môi trường ưu tiên hơn file **kể cả biến có giá trị rỗng**. Nếu đã sửa file mà lỗi vẫn xuất hiện, kiểm tra tên các biến đang được đặt trong terminal/IntelliJ/service; không in giá trị biến chứa secret. Không đặt credential trong JDBC URL. File mẫu không tự được load; code không load secret từ classpath.

Để kiểm tra trên máy phát triển, đặt `app.diagnostics.enabled=true`. `/health/db` chỉ trả lời khi cờ này bật **và request từ loopback**. Tắt khi chia sẻ/deploy qua proxy. Không cấu hình DB vẫn mở trang khởi động được; đó không phải kết nối DB thành công. Cấu hình dở dang báo lỗi, không fallback sang root/WAMP.

## 3. Chạy schema và seed

Cách khuyến nghị, sau khi đã tạo schema riêng và cấu hình user setup:

```powershell
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
.\mvnw.cmd -B compile exec:java '-Dexec.args=migrate'
.\mvnw.cmd -B compile exec:java '-Dexec.args=seed'
.\mvnw.cmd -B exec:java '-Dexec.args=check'
```

Tên wrapper chính xác là `.\mvnw.cmd` (không phải `.\mvnw\.cmd`); goal là `exec:java`. Chạy từ thư mục có `pom.xml`, sau khi lưu file cấu hình. `APP_CONFIG_FILE` trong ví dụ chỉ có hiệu lực cho terminal hiện tại và các tiến trình nó khởi chạy. Nếu mở terminal mới, đặt lại biến. Có thể chọn file qua tham số JVM của Maven thay cho biến này:

```powershell
$configPath = (Resolve-Path .\config\application.local.properties).Path
.\mvnw.cmd -B compile exec:java "-Dc2c.config=$configPath" '-Dexec.args=migrate'
```

Tham số trên chỉ chọn đường dẫn file; `DB_*` vẫn ưu tiên các giá trị trong file. Lỗi cấu hình sẽ nêu khóa thiếu/sai hoặc đường dẫn file không đọc được, không in giá trị cấu hình. Lỗi kết nối sau khi cấu hình hợp lệ vẫn được che nguyên nhân chứa credential; kiểm tra MySQL đang chạy, host/port, schema đã tạo và quyền của user theo mục 1. Không tự thử tài khoản root hay mật khẩu rỗng.

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

USER vừa mua vừa bán; seller/buyer chỉ là tên fixture. **M2 đã có login UI bằng email**; các tài khoản demo đăng nhập được sau khi schema/seed đã chạy trên database phát triển. Seed có 4 danh mục/4 sản phẩm của 2 seller, stock 3/5/4/1, 4 INITIAL ledger và 4 sự kiện PENDING. Tin HIDDEN/PENDING vì chưa có ảnh thật; bổ sung ảnh/duyệt ở M3. Chưa seed đơn/review/khiếu nại không có nguồn giao dịch.

## 4. Build và deploy Tomcat

```powershell
.\mvnw.cmd -B clean verify
$env:CATALINA_HOME = 'D:\cong_cu_nen\apache-tomcat-10.1.48' # sửa theo máy bạn
# Nếu có CATALINA_BASE riêng, deploy vào CATALINA_BASE\webapps.
Copy-Item target\demo-1.0-SNAPSHOT.war "$env:CATALINA_HOME\webapps\c2c.war"
& "$env:CATALINA_HOME\bin\catalina.bat" run
```

Chạy Tomcat trong PowerShell đã có APP_CONFIG_FILE hoặc biến DB. Với IntelliJ, đặt biến trong Run Configuration, deploy WAR/exploded; không sửa `.idea/workspace.xml` để chia sẻ secrets. Với Windows service, cấu hình môi trường cho service và restart; biến của terminal không tự truyền tới service.

Nếu CLI `check` thành công nhưng trang tài khoản trả 503 khi chạy IntelliJ: vào **Run → Edit Configurations → Tomcat → Server → VM options**, giữ option cũ và thêm đường dẫn file, không thêm password:

```text
-Dc2c.config="C:\Users\hausi\OneDrive\Desktop\TMDT\project\web\demo\config\application.local.properties"
```

Hoặc đặt biến APP_CONFIG_FILE trong Environment variables của cấu hình Tomcat. **Stop rồi Run lại Tomcat**, không chỉ reload trang hoặc cập nhật WAR. Biến trong PowerShell Maven không tự truyền vào IntelliJ đã mở. Log server có mã DATABASE_NOT_CONFIGURED khi thiếu DB config, DATABASE_CONFIGURATION_INVALID khi file/khóa sai; browser vẫn chỉ hiển thị thông báo an toàn. Context IntelliJ có thể là `/demo_war_exploded`.

Theo cổng Tomcat thực tế (mặc định 8080), mở:

- [Trang khởi động](http://localhost:8080/c2c/): Servlet → JSP/JSTL, tiếng Việt.
- [DB health](http://localhost:8080/c2c/health/db): 200/CONNECTED khi SELECT 1 thành công; 503 nếu thiếu/không kết nối DB; 404 khi diagnostics tắt hoặc request ngoài loopback.
- [Servlet mẫu](http://localhost:8080/c2c/hello-servlet): giữ nguyên mẫu ban đầu.
- [Đăng ký](http://localhost:8080/c2c/register), [đăng nhập](http://localhost:8080/c2c/login), [hồ sơ](http://localhost:8080/c2c/account/profile), [quản trị tối thiểu](http://localhost:8080/c2c/admin).

Context có thể là `/demo_war_exploded` trong IntelliJ; link/assets tính theo context. JSP trong WEB-INF không truy cập trực tiếp.

## 5. Kiểm thử

`clean verify` chạy **22 unit tests**, không cần DB, gồm cấu hình, BCrypt, validation tài khoản, CSRF và phân biệt nguyên nhân account service không sẵn sàng. Integration test cần schema riêng hậu tố `_test`, config trỏ đúng schema và user có quyền DDL/DML:

```powershell
# APP_CONFIG_FILE trỏ cấu hình schema test, ví dụ c2c_m1_test.
$env:C2C_IT_ALLOWED = 'true'
.\mvnw.cmd -B -Pmysql-it verify
```

Profile migrate/seed schema test; chạy 11 test hạ tầng và 7 test UserService, gồm đăng ký đồng thời, đăng nhập seed, tài khoản INACTIVE và cập nhật hồ sơ. Không chạy trên `c2c_demo` hoặc dữ liệu thật. Build thường không chạy integration. Nếu console lỗi dấu, dùng UTF-8 và `MAVEN_OPTS=-Dfile.encoding=UTF-8`; UI/DB dùng UTF-8.

Để chạy thêm 6 test HTTP, deploy WAR lên Tomcat loopback **với cùng file cấu hình schema test**, rồi trong terminal Maven:

```powershell
$env:C2C_HTTP_BASE = 'http://127.0.0.1:8080/c2c' # đổi cổng/context theo Tomcat test
.\mvnw.cmd -B -Pmysql-it verify
```

Nếu không đặt C2C_HTTP_BASE, nhóm HTTP không chạy. Các test đăng ký fixture và xóa đúng fixture của mình sau kiểm tra; dùng database test riêng chỉ có seed, không đặt profile test lên database ứng dụng. Chi tiết thao tác thủ công và tuyến tài khoản ở [hướng dẫn M2](docs/08-tai-khoan-va-phan-quyen.md).

## Cấu trúc và luồng M1

```text
controller/ HomeServlet, DatabaseHealthServlet nhận HTTP
service/    HealthService điều phối kiểm tra
dao/        HealthDao: SELECT 1 và ví dụ bind
model/      DatabaseHealth
config/     AppConfig, Database, SchemaManager, DatabaseTool, JdbcLifecycle
controller/auth/ RegisterServlet, LoginServlet, LogoutServlet, ProfileServlet
controller/admin/ AdminServlet
service/    UserService, AccountValidation
dao/        UserDao
model/      CurrentUser (session), UserAccount (nội bộ), UserProfile (view)
dto/        RegisterForm, ProfileForm
exception/  FormException, AccountUnavailableException
listener/   ApplicationListener khởi tạo/đóng pool
filter/     EncodingFilter, AccessFilter (đăng nhập/quyền/CSRF)
security/   LocalDiagnostics, PasswordHasher, SessionAuth, CsrfTokens
resources/db/ schema.sql, seed.sql, migration/V001__initial_schema.sql
webapp/     index.jsp, WEB-INF/views/home.jsp, assets/css/startup.css
```

Servlet gọi Service; Service mở Handle qua Database và attach DAO; DAO dùng JDBI bind/PreparedStatement tới MySQL. Callback kết thúc đóng Handle, trả connection về pool. `Database.transaction(...)` mở READ COMMITTED, các DAO cùng Handle; lỗi rollback toàn transaction. Chưa có transaction nghiệp vụ tạo/hủy đơn. [JDBI transactions](https://jdbi.org/releases/3.55.0/#_transactions).

## Kết quả và giới hạn

Đã build WAR, chạy 7 unit + 11 integration tests và HTTP smoke trên Tomcat 10.1.48/MySQL 9.1.0 riêng. [Báo cáo M1/danh sách file](docs/reports/M1-nen-tang.md) có lệnh/kết quả. MySQL thử port 13316/datadir riêng; Tomcat thử port 18080/CATALINA_BASE riêng; cả hai được dừng sau kiểm tra. Chưa thiết lập `c2c_demo` trên WAMP 3306 vì chưa có credential dự án; chưa thử MySQL 8.4 thực tế. Dữ liệu WAMP không bị thay đổi.

Cập nhật 04/10/2026: [khắc phục cấu hình migrate](docs/reports/M1-khac-phuc-cau-hinh.md), build WAR với 13 unit tests đạt; xác minh lỗi thiếu selector/file/password qua CLI. File local mới tạo vẫn cần điền credential; các lệnh migrate kiểm tra lỗi dừng trước kết nối, chưa chạy SQL hoặc kiểm thử MySQL/Tomcat lại trong lượt này.

Xác minh tiếp cùng ngày sau khi người dùng sửa file local: đặt APP_CONFIG_FILE rồi chạy `compile exec:java '-Dexec.args=check'` thành công, `JDBI SELECT 1 = 1`. Chưa chạy migrate/seed hoặc kiểm tra các bảng trên database này. Nếu gặp thông báo “Chưa có cấu hình DB” dù file đã được điền, hãy đặt APP_CONFIG_FILE trong chính terminal đang chạy Maven theo mục 3; biến của terminal/tiến trình khác không tự truyền sang.

M2 ngày 04/10/2026: **20 unit + 24 integration/HTTP tests đạt** trên MySQL 9.1.0:13316/schema c2c_m2_test và Tomcat 10.1.48:18080 riêng. Kiểm cả đổi mã phiên, logout/replay, XSS, CSRF, role/status cập nhật và ownership hồ sơ. Không sửa database ứng dụng hoặc file local của người dùng; M2 không cần migration mới. [Báo cáo M2/danh sách file](docs/reports/M2-tai-khoan.md) ghi lệnh, kết quả và giới hạn.

Thiết kế: [docs/README.md](docs/README.md). M3 trở đi chưa triển khai. Commit đề xuất: `feat: triển khai tài khoản, hồ sơ và phân quyền USER ADMIN`.
