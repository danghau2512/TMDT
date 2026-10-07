# TraoTay — Đồ cũ, giá trị mới.

**Xác minh người bán bằng QR (06/10/2026):** đã thay FPT.AI bằng ZXing cục bộ, upload đủ hai ảnh và tự điền sáu trường; không cần API key/mạng OCR. Giữ Admin đối chiếu/duyệt, quyền đăng tin và nhãn **Đã xác minh hồ sơ**. c2c_demo đã backup + áp V007, giữ hồ sơ/ảnh OCR cũ; **không chạy lại SQL/seed**. Build `.\mvnw.cmd -B verify`, Stop → Rebuild artifact → Run → Ctrl+F5. [Định dạng QR, cấu hình riêng tư và demo](docs/15-xac-minh-nguoi-ban.md), [file/kiểm tra thực tế](docs/reports/Xac-minh-nguoi-ban-QR.md).

**Giỏ hàng AJAX (05/10/2026):** thay nút “Cập nhật” bằng −/+; nhập số lượng rồi Enter hoặc rời ô cũng tự lưu. Số lượng, thành tiền, tổng giỏ và xóa sản phẩm cập nhật không tải lại trang. Có thông báo lỗi/kết nối và nút kiểm tra lại nếu chưa xác nhận trạng thái server. Build `.\mvnw.cmd -B verify`, Rebuild artifact rồi Stop/Run Tomcat và Ctrl+F5; **không cần SQL**. [Các file, demo và kiểm tra thực tế](docs/reports/Gio-hang-AJAX.md).

**Ảnh trang chủ (05/10/2026):** khu sản phẩm mới đăng dùng khung vuông lớn hơn, hiển thị trọn ảnh; desktop 3 cột/tablet 2/điện thoại 1. Ctrl+F5 sau cập nhật artifact, không cần SQL. [File và kiểm tra thực tế](docs/reports/Anh-san-pham-trang-chu.md).

**Gợi ý tìm kiếm (05/10/2026):** nhập từ khóa ở header để xem tối đa 8 sản phẩm công khai với ảnh, tên và giá; click hoặc ↑/↓ + Enter mở chi tiết. Có “Xem tất cả kết quả”, giữ tìm kiếm thường khi mất mạng/tắt JS. Rebuild artifact rồi Stop/Run Tomcat và Ctrl+F5, không cần SQL. [File, kiểm tra và ảnh desktop/mobile](docs/reports/Tim-kiem-goi-y.md).

Website C2C — Đồ án TMĐT nhóm 6. **Nhận diện và tiếng Việt (05/10/2026):** dùng nguyên logo `src/main/webapp/assets/images/Logo.png`, thống nhất typography và asset UTF-8, cập nhật header/footer/auth/title. Build `./mvnw.cmd -B verify`, Rebuild artifact rồi Stop/Run Tomcat và Ctrl+F5; không cần SQL/migration. Xem [chẩn đoán, file, ảnh và kiểm tra thực tế](docs/reports/TraoTay-tieng-Viet.md). Font Segoe UI/fallback có sẵn cục bộ; khẩu hiệu “Đồ cũ, giá trị mới.”.

**Bốn nâng cấp (05/10/2026):** trung tâm khiếu nại, đánh giá có ảnh, khai báo hàng cũ/snapshot và so sánh 2–3 sản phẩm đã triển khai. `c2c_demo` đã backup và áp V004 một lần, dữ liệu cũ giữ nguyên, không seed/demo vào DB ứng dụng. Rebuild artifact rồi Stop/Run Tomcat; không SOURCE V004 lại. [Hướng dẫn 5–7 phút](docs/14-demo-bon-nang-cap.md), [file/kiểm tra thực tế](docs/reports/Bon-nang-cap-trai-nghiem.md).


**Chat mua bán (05/10/2026):** đã có nút Chat với người bán, hộp thư và tin nhắn tự cập nhật. `c2c_demo` đã được backup và áp V003, giữ dữ liệu cũ; không chạy lại SQL/seed. Rebuild artifact rồi Stop/Run Tomcat. Xem [demo bằng hai cửa sổ](docs/13-demo-chat-mua-ban.md) và [báo cáo/ảnh/kiểm tra thực tế](docs/reports/Chat-mua-ban.md).

**Cập nhật 05/10/2026:** checkout mới dùng COD hoặc VNPAY Sandbox theo từng đơn. Đọc [hướng dẫn cấu hình/migration/HTTPS/demo VNPAY](docs/12-vnpay-sandbox.md) trước khi chạy. `c2c_demo` standalone đã được nâng V002 sau backup; không chạy V002/seed lại. Rebuild và restart Tomcat để nhận Java/config mới.

**Khắc phục HTTPS VNPAY trên máy Windows hiện tại:** đã bật `vnpay.tls.useWindowsRoot=true` trong file local, chỉ dùng kho CA Windows cho client VNPAY. Rebuild artifact rồi **Stop/Run Tomcat**. Kiểm tra độc lập: chọn `APP_CONFIG_FILE`, chạy `.\mvnw.cmd -B compile exec:java '-Dexec.args=vnpay-check'`. HTTPS đã kiểm tra thành công, HTTP 405 với HEAD; không thay đổi database/thanh toán. Xem [hướng dẫn chi tiết](docs/12-vnpay-sandbox.md#https-querydr-trên-windows) và [báo cáo](docs/reports/VNPAY-khac-phuc-HTTPS.md).


**M1–M8 đã có luồng demo:** tài khoản, mua bán, snapshot, COD/VNPAY Sandbox, đánh giá theo đơn và khiếu nại riêng tư. Xem [demo mua bán 5–7 phút](docs/09-demo-mua-ban.md), [demo thêm M7–M8 2–3 phút](docs/10-demo-danh-gia-khieu-nai.md) và [báo cáo mới](docs/reports/M7-M8-danh-gia-khieu-nai.md).

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

- `migrate` trên schema trống chạy V001 + V002, tạo 23 bảng nghiệp vụ và `flyway_schema_history`. Chạy lại trả số migration 0 nếu không có migration mới. Không baseline tự động database có bảng sẵn, không clean/reset; không tự chạy khi deploy.
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

`clean verify` chạy **27 unit tests**, không cần DB, gồm cấu hình, BCrypt, validation tài khoản, CSRF và phân biệt nguyên nhân account service không sẵn sàng. Integration test cần schema riêng hậu tố `_test`, config trỏ đúng schema và user có quyền DDL/DML:

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

Servlet gọi Service; Service mở Handle qua Database và attach DAO; DAO dùng JDBI bind/PreparedStatement tới MySQL. Callback kết thúc đóng Handle, trả connection về pool. `Database.transaction(...)` mở READ COMMITTED, các DAO cùng Handle; lỗi rollback toàn transaction. ProductService/CartService/OrderService dùng transaction nghiệp vụ; tạo/hủy đơn đồng bộ kho, thanh toán và history. [JDBI transactions](https://jdbi.org/releases/3.55.0/#_transactions).

## Kết quả và giới hạn

Đã build WAR, chạy 7 unit + 11 integration tests và HTTP smoke trên Tomcat 10.1.48/MySQL 9.1.0 riêng. [Báo cáo M1/danh sách file](docs/reports/M1-nen-tang.md) có lệnh/kết quả. MySQL thử port 13316/datadir riêng; Tomcat thử port 18080/CATALINA_BASE riêng; cả hai được dừng sau kiểm tra. Chưa thiết lập `c2c_demo` trên WAMP 3306 vì chưa có credential dự án; chưa thử MySQL 8.4 thực tế. Dữ liệu WAMP không bị thay đổi.

Cập nhật 04/10/2026: [khắc phục cấu hình migrate](docs/reports/M1-khac-phuc-cau-hinh.md), build WAR với 13 unit tests đạt; xác minh lỗi thiếu selector/file/password qua CLI. File local mới tạo vẫn cần điền credential; các lệnh migrate kiểm tra lỗi dừng trước kết nối, chưa chạy SQL hoặc kiểm thử MySQL/Tomcat lại trong lượt này.

Xác minh tiếp cùng ngày sau khi người dùng sửa file local: đặt APP_CONFIG_FILE rồi chạy `compile exec:java '-Dexec.args=check'` thành công, `JDBI SELECT 1 = 1`. Chưa chạy migrate/seed hoặc kiểm tra các bảng trên database này. Nếu gặp thông báo “Chưa có cấu hình DB” dù file đã được điền, hãy đặt APP_CONFIG_FILE trong chính terminal đang chạy Maven theo mục 3; biến của terminal/tiến trình khác không tự truyền sang.

M2 ngày 04/10/2026: **20 unit + 24 integration/HTTP tests đạt** trên MySQL 9.1.0:13316/schema c2c_m2_test và Tomcat 10.1.48:18080 riêng. Kiểm cả đổi mã phiên, logout/replay, XSS, CSRF, role/status cập nhật và ownership hồ sơ. Không sửa database ứng dụng hoặc file local của người dùng; M2 không cần migration mới. [Báo cáo M2/danh sách file](docs/reports/M2-tai-khoan.md) ghi lệnh, kết quả và giới hạn.

Thiết kế: [docs/README.md](docs/README.md). Các đoạn kết quả phía trên lưu lịch sử M1/M2. Kết quả mới và cách chạy luồng mua bán ở phần dưới.

## Chạy bản demo mua bán M3–M6 trên Windows

Giữ nguyên file local và credential đã dùng ở M2. Không cần migration mới: 22 bảng hiện có đủ dùng; V001/schema.sql/seed.sql không bị sửa. Nếu DB đã nhập schema.sql mà chưa có Flyway history, không tự baseline/reset/chạy schema lại; ứng dụng vẫn dùng schema đúng cấu trúc. Chỉ migrate/seed theo phần chuẩn bị phía trên khi tạo **schema trống riêng**, hoặc khi history/schema đã được đối chiếu và sao lưu.

```powershell
# Terminal Maven; không đưa password vào command.
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
.\mvnw.cmd -B compile exec:java '-Dexec.args=check'
.\mvnw.cmd -B verify
```

WAR tạo ở `target/demo-1.0-SNAPSHOT.war`. Redeploy WAR này trên Tomcat 10.1, hoặc cập nhật artifact WAR exploded trong IntelliJ rồi restart Tomcat. Cấu hình **riêng cho tiến trình Tomcat** bằng APP_CONFIG_FILE hoặc VM option `-Dc2c.config=C:/duong-dan-tuyet-doi/config/application.local.properties`. Maven check thành công không chứng minh Tomcat đã nhận cùng cấu hình. Với IntelliJ, mở `http://localhost:8080/demo_war_exploded/home`; context thực tế có thể khác theo Deployment.

Ảnh JPEG/PNG lưu ngoài WAR, không vào source/target. Tùy chọn trong file local:

```properties
# Dùng / trên Windows, account chạy Tomcat phải có quyền đọc/ghi.
upload.root=C:/c2c-data/uploads
```

Biến `UPLOAD_ROOT` ưu tiên hơn khóa file; mặc định `user.home/.c2c-demo/uploads`. Vị trí phải tuyệt đối. Sao lưu DB và thư mục ảnh cùng nhau; khi đổi storage root phải chuyển nguyên bytes/file cũ để snapshot không mất ảnh. Không lưu ảnh hoặc cấu hình thật vào repository. Không tự migrate, seed hoặc approve sản phẩm lúc deploy.

Các trang chính: `/home`, `/products`, `/categories`, `/products/detail?id=ID`, `/cart`, `/checkout`, `/buyer/orders`, `/seller/products`, `/seller/products/new`, `/seller/orders`, `/admin/products`, `/admin/orders`. Dùng prefix context của WAR. Admin chọn seller ACTIVE khi tạo thay; seller thường không chọn/gán lại chủ tin. Các POST chỉ gửi qua biểu mẫu có CSRF.

Fixture M1/M2 giữ nguyên: `admin@c2c.example`, `seller1@c2c.example`, `seller2@c2c.example`, `buyer1@c2c.example`, `buyer2@c2c.example`, password demo công khai `C2cDemo!2026` nếu chưa đổi. **Seed giữ tin HIDDEN/PENDING:** trước demo, Admin sửa chế độ PUBLIC rồi duyệt có lý do, hoặc seller đăng mới. Có ảnh mặc định nên không cần dữ liệu ảnh giả. Xem [kịch bản 5–7 phút và quyền/trạng thái](docs/09-demo-mua-ban.md).

## Kiểm tra bản mua bán

`.\mvnw.cmd -B verify` chạy 22 unit tests và tạo WAR. Kiểm tra ghi chỉ dùng schema `_test` dành riêng, đã migrate/seed và sao lưu, với file APP_CONFIG_FILE riêng. Không dùng `application.local.properties` của ứng dụng cho kiểm tra ghi. Deploy Tomcat loopback riêng với **cùng file test**, không trỏ vào Tomcat chứa dữ liệu người dùng.

```powershell
$env:APP_CONFIG_FILE = 'C:/duong-dan/file-test.local.properties'
$env:C2C_IT_ALLOWED = 'true'
$env:C2C_HTTP_BASE = 'http://127.0.0.1:18080/c2c'
# Bộ M1/M2 chạy trước trên schema fixture sạch (giả định 4 products seed).
.\mvnw.cmd -B verify -Pmysql-it
# Các kịch bản thương mại chạy riêng, để lại tin/đơn fixture trong schema test.
.\mvnw.cmd -B verify -Pmysql-it '-Dit.test=CommerceIT,ShopHttpIT'
```

Nếu chưa có Tomcat test, bỏ C2C_HTTP_BASE: HTTP test bị skip, không được báo đã kiểm HTTP. CommerceIT cần CREATE/DROP TRIGGER trong **schema test riêng** để ép lỗi tại seller thứ hai và kiểm rollback; script thử nghiệm không thay database ứng dụng. HttpTestTarget xác minh Tomcat đọc được tài khoản nonce chỉ tạo trong DB test trước các HTTP ghi. Với schema test đã chứa fixture thương mại, không chạy lại DatabaseIT (giả định số dòng seed); chọn một schema test mới và deploy lại cùng config. Không DROP/reset schema đang có dữ liệu.

Lượt triển khai đã chạy trên MySQL 9.1.0/InnoDB riêng và Tomcat 10.1.48: 22 unit, 24 kiểm tra M1/M2 và 4 kiểm tra mới (3 Service/MySQL + 1 hành trình HTTP). Báo cáo [M3–M6](docs/reports/M3-M6-mua-ban.md) phân biệt kết quả, lỗi đã sửa và giới hạn. Không xác nhận deploy mới trên IntelliJ/WAMP của người dùng.

Các giới hạn tại thời điểm M3–M6 được giữ trong báo cáo lịch sử; M7–M8 đã bổ sung đánh giá/khiếu nại bên dưới. Thu/hoàn tiền thật, tự hết hạn đơn, stress đầy đủ, MySQL 8.4 và kiểm UI trên mọi kích thước vẫn chưa thực hiện.

## M7–M8: Đánh giá và khiếu nại

Không cần migration mới: đã đọc cấu trúc thực tế trên `c2c_demo`, các bảng reviews/complaints/evidence/messages/history và ràng buộc khớp V001. Giữ DB, file local, storage và credential của bạn; không chạy lại schema.sql hoặc reset dữ liệu. Build `.\mvnw.cmd -B verify`, redeploy `target/demo-1.0-SNAPSHOT.war` trên Tomcat 10.1 rồi restart với APP_CONFIG_FILE / `-Dc2c.config` đang dùng. Chưa xác nhận bản WAR này trên IntelliJ của bạn.

| Chức năng | Route |
| --- | --- |
| Form đánh giá từ dòng đơn COMPLETED | GET /buyer/reviews/new?orderId=ID&itemId=ID |
| Gửi đánh giá | POST /buyer/reviews/create |
| Danh sách/form/chi tiết khiếu nại buyer | GET /buyer/complaints, /buyer/complaints/new?orderId=ID, /buyer/complaints/detail?id=ID |
| Gửi/bổ sung khiếu nại kèm ảnh | POST /buyer/complaints/create, /buyer/complaints/supplement |
| Admin list/filter/detail/xử lý | GET /admin/complaints?status=RECEIVED, /admin/complaints/detail?id=ID; POST /admin/complaints/action |
| Ảnh minh chứng có quyền | GET /media/complaint-evidence?complaintId=ID&asset=ID |

Thêm prefix context Tomcat, ví dụ `http://localhost:8080/demo_war_exploded`. Dùng liên kết từ chi tiết đơn và Hồ sơ/Quản trị; các POST có CSRF qua form. Buyer chỉ đánh giá dòng thuộc đơn của mình đã COMPLETED, một lần/dòng. Form có số sao chung 1–5 và nhận xét; V001 giữ hai cột rating bắt buộc nên lưu cùng điểm, chưa có UI điểm seller riêng. Đã gửi thì chi tiết đơn hiển thị nội dung, catalog công khai chỉ hiển thị review VISIBLE từ đơn hoàn thành, nhãn xác thực do server quyết định.

Mỗi đơn **một hồ sơ khiếu nại**, kể cả đã xử lý; gửi lại dẫn về hồ sơ đó. Buyer phản ánh ở mọi trạng thái đơn, bổ sung khi mở. Admin RECEIVED → PROCESSING → RESOLVED với phản hồi và kết quả; có thể mở lại và giữ lịch sử kết luận trước. Nội dung 5.000 ký tự; ảnh tùy chọn dùng storage M3, 5 ảnh/lần, 20 ảnh/hồ sơ. Ảnh chỉ buyer của hồ sơ/Admin tải; seller và khách không được đọc.

RECEIVED/PROCESSING chặn buyer COMPLETE dưới cùng khóa order. RESOLVED mở lại khả năng xác nhận nếu đơn đã DELIVERED/PAID; **không** tự hoàn thành đơn. Complaint sau COMPLETED không đảo trạng thái. Đóng hồ sơ không tự hủy/hoàn kho/đổi payment; kết quả “Đơn đã được hủy” chỉ dùng khi đơn đã CANCELLED qua luồng đơn hàng. Review edit/delete/response/moderation và thanh toán thật ngoài phạm vi.

Tài khoản fixture vẫn dùng theo demo M3: admin@c2c.example, seller1@c2c.example, buyer1@c2c.example; password công khai `C2cDemo!2026` nếu bạn chưa đổi. Chuẩn bị một đơn COMPLETED để review và một đơn chưa hoàn thành để complaint; không tự thêm dữ liệu giả vào DB ứng dụng khi deploy.

Kiểm tra M7/M8 trên cấu hình và Tomcat **test riêng** đã migrate/seed, có hậu tố `_test` và C2C_IT_ALLOWED=true:

```powershell
$env:APP_CONFIG_FILE = 'C:/duong-dan/file-test.local.properties'
$env:C2C_IT_ALLOWED = 'true'
$env:C2C_HTTP_BASE = 'http://127.0.0.1:18080/c2c'
.\mvnw.cmd -B verify -Pmysql-it '-Dit.test=ReputationIT,FeedbackHttpIT'
```

ReputationIT có 3 kịch bản Service/MySQL, FeedbackHttpIT có 1 hành trình HTTP, tái sử dụng guard/browser sẵn có. mysql-it mặc định vẫn chỉ M1/M2 trên fixture sạch; có thể chạy lại commerce riêng như phần trước. HTTP bị skip nếu thiếu C2C_HTTP_BASE. Xem [báo cáo M7–M8](docs/reports/M7-M8-danh-gia-khieu-nai.md) cho lệnh, kết quả thực tế và giới hạn; chưa kiểm trình duyệt thủ công. Commit gợi ý: `feat: bổ sung đánh giá theo đơn và xử lý khiếu nại C2C`.

## Giao diện marketplace và trình chiếu

Giao diện hiện tại dùng teal, nền sáng, shell header/footer chung, menu theo vai trò và sidebar quản lý; các trang catalog, tài khoản, mua bán, review/khiếu nại và Admin cùng hệ thống card/form/badge. Font/SVG/CSS/JS được lưu hoặc có sẵn cục bộ, không cần CDN.

- [Báo cáo giao diện và ảnh desktop/mobile](docs/reports/UI-hoan-thien-giao-dien.md).
- [Checklist build/redeploy và chuẩn bị ảnh demo](docs/11-checklist-giao-dien.md).
- Tên thương hiệu: `src/main/webapp/WEB-INF/views/layouts/brand.jspf`; theme: `assets/css/startup.css`; tương tác bổ sung: `assets/js/marketplace.js` (từ gốc webapp).

Build ` .\mvnw.cmd -B verify `, redeploy WAR trên Tomcat 10.1 rồi restart/reload ứng dụng. Giữ cấu hình DB/storage hiện có; **không cần migration hoặc seed**. Mở `/home` trong context đang dùng và nhấn **Ctrl+F5** nếu còn asset cũ. CSS/JS có version query trong `shop-start.jspf` để hỗ trợ cập nhật cache.

Ảnh chụp báo cáo dùng fixture của schema test riêng. Ảnh minh họa được ghi rõ; dữ liệu và upload của ứng dụng không thay đổi. Để demo catalog đẹp trên máy bạn, chuẩn bị ảnh chụp đúng sản phẩm, upload qua form seller và duyệt lại tin khi cần. Không có số sao/lượt mua/thống kê trang trí giả.
