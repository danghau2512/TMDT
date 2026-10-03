# Báo cáo M1 — Nền tảng Servlet, database và JDBI

Ngày: **03/10/2026**, Asia/Bangkok. M1 thực hiện theo yêu cầu triển khai thực tế; kết quả khảo sát trước giữ ở 01/06. Chưa làm nghiệp vụ M2 trở đi.

## Những gì đã triển khai

- Java 17 + Maven Wrapper/WAR; đồng bộ Servlet API 6.0.0 với Tomcat 10.1/web.xml 6.0, JSTL Jakarta. Giữ HelloServlet làm smoke endpoint.
- HomeServlet → HealthService → HealthDao → JDBI/MySQL, DatabaseHealth Model; trang JSP UTF-8/context-aware, EncodingFilter. `/health/db` tắt mặc định, chỉ dev flag + loopback, không lộ DB URL/credential/SQL cause.
- AppConfig đọc biến môi trường ưu tiên properties ngoài WAR; cấu hình partial/placeholder bị từ chối. Database quản lý một pool/Jdbi, mỗi callback Handle riêng, transaction READ COMMITTED dùng nhiều DAO cùng Handle. Listener đóng pool/JDBC thread và driver lúc undeploy.
- schema.sql/V001: 22 bảng nghiệp vụ; 46 FK/52 CHECK/index, snapshot ảnh/nội dung, lịch sử, thanh toán mô phỏng, review/complaint và stock ledger. Flyway có history thứ 23, không auto baseline/clean; CLI migrate/seed/check, không chạy SQL khi deploy.
- Seed 5 user (1 admin), 4 category/4 product của 2 seller, hash BCrypt 2b/cost12/salt riêng; 4 INITIAL + 4 moderation events. HIDDEN/PENDING vì chưa có ảnh thật; không fake file hoặc tạo giao dịch không có nguồn.

## File thay đổi

| Nhóm | File |
| --- | --- |
| Build/cấu hình | pom.xml, .gitignore, config/application.example.properties |
| Hạ tầng | config/AppConfig.java, DatabaseConfig.java, Database.java, ApplicationRuntime.java, JdbcLifecycle.java |
| Setup DB | config/DatabaseTool.java, SchemaManager.java; database/create-database.sql, database/README.md |
| Luồng HTTP | controller/HomeServlet.java, DatabaseHealthServlet.java; service/HealthService.java; dao/HealthDao.java; model/DatabaseHealth.java |
| Lifecycle/hỗ trợ | listener/ApplicationListener.java, filter/EncodingFilter.java, security/LocalDiagnostics.java, PasswordHasher.java |
| UI | src/main/webapp/index.jsp, WEB-INF/web.xml, WEB-INF/views/home.jsp, assets/css/startup.css |
| SQL | src/main/resources/db/schema.sql, seed.sql, migration/V001__initial_schema.sql |
| Test | src/test/java/com/example/demo/config/AppConfigTest.java, SchemaFilesTest.java, DatabaseIT.java; security/PasswordHasherTest.java; service/HealthServiceTest.java |
| Tài liệu | AGENTS.md, README.md, docs/README.md, docs/03-kien-truc.md, 04-thiet-ke-database.md, 05-ke-hoach-trien-khai.md, 07-chuan-bi-moi-truong.md, báo cáo này |

Các đường dẫn Java trong bảng nằm dưới `src/main/java/com/example/demo/`; test có prefix như ghi. HelloServlet/Maven Wrapper/metadata IDE/yêu cầu gốc không sửa. `.local-test/` và target là đầu ra thử/build được ignore, không phải source/credential để commit. Không có Git metadata nên chưa tạo commit.

## Môi trường kiểm tra thực tế

Tomcat installation 10.1.48, JVM 17.0.12, wrapper Maven 3.9.6. MySQL binary 9.1.0 ở WAMP được dùng để khởi tạo **datadir mới riêng**, không dùng datadir hoặc credential của service WAMP. Instance thử chỉ bind 127.0.0.1:13316, schema `c2c_m1_test`. Root rỗng chỉ thuộc instance mới dùng kiểm thử; không phải credential mặc định ứng dụng và không được thử với service WAMP. Tomcat thử CATALINA_BASE riêng `.local-test/tomcat`, connector loopback 18080, shutdown 18005.

## Lệnh và kết quả

| Kiểm tra đã chạy | Kết quả |
| --- | --- |
| version.bat với CATALINA_HOME hợp lệ; mvnw.cmd -version | Tomcat 10.1.48/JDK17.0.12; Maven Wrapper3.9.6 |
| mysqld --no-defaults --initialize-insecure, datadir riêng; server bind loopback port13316 | Khởi tạo/chạy instance test, SELECT VERSION() = 9.1.0 |
| CREATE DATABASE c2c_m1_test trên instance test | Schema riêng tạo thành công |
| `.\mvnw.cmd -B compile exec:java '-Dexec.args=demo-hashes'` | Sinh 5 BCrypt salt riêng để seed |
| `compile exec:java -Dexec.args=migrate` | Lần đầu 1 migration, 23 bảng gồm Flyway history; lần sau 0 |
| `compile exec:java -Dexec.args=seed` | 5 user/4 category/4 product/4 INITIAL/4 moderation; không nhân đôi khi chạy lại |
| `exec:java -Dexec.args=check` | JDBI SELECT 1 = 1 |
| `.\mvnw.cmd -B -Pmysql-it verify` với APP_CONFIG_FILE test và C2C_IT_ALLOWED=true | **BUILD SUCCESS; 7 unit + 11 integration, 0 fail/error/skip** |
| `.\mvnw.cmd -B clean verify` trên source cuối | **BUILD SUCCESS; 7 unit**, WAR tạo lại sạch; integration chỉ chạy với profile riêng |
| information_schema | 22 bảng nghiệp vụ InnoDB + history; 46 FK, 52 CHECK |
| Deploy c2c.war trên Tomcat test | Khởi động thành công, JSP/JSTL tiếng Việt |
| HTTP `/c2c/`, `/c2c/hello-servlet`, `/c2c/assets/css/startup.css` | 200, root có nội dung tiếng Việt đúng |
| HTTP `/c2c/health/db` khi bật diagnostics | 200 CONNECTED, SELECT 1 = 1 |
| HTTP `/c2c/WEB-INF/views/home.jsp` | 404, JSP được bảo vệ |
| Deploy lại WAR cuối với diagnostics=false | Root 200/tiếng Việt; `/health/db` 404 và link chẩn đoán được ẩn |
| Thử migrate `c2c_guard_test` có bảng/1 bản ghi legacy_probe nhưng không có history | Bị từ chối như dự kiến; giữ nguyên 1 bảng/1 bản ghi, không tự baseline/drop |
| Kiểm nội dung WAR và tài liệu | Schema/seed/V001 có trong WEB-INF/classes; không đóng gói Servlet API; link tài liệu nội bộ không hỏng |
| Catalina stop và mysqladmin shutdown trên port test; kiểm service WAMP | Tiến trình test dừng; MySQL/MariaDB WAMP vẫn Running |

Integration đã kiểm: migration chạy lại, bind chuỗi dấu nháy/SQL như dữ liệu, Handle đóng/pool trả connection, hai DAO thấy cùng transaction và rollback/commit, seed không reset hồ sơ/kho, cả 5 hash xác minh, giá/condition/quantity/FK sai bị từ chối, snapshot không đổi khi tin sửa và review sai buyer bị FK chặn, hold/release duplicate/cân bằng sai bị chặn. Đây là kiểm tra hạ tầng/schema, **chưa kiểm nghiệp vụ nhiều người đặt hàng đồng thời** vì chưa có OrderService.

## Lỗi phát hiện và đã sửa

- Test compile lỗi overload assertEquals với kiểu generic Integer: đã ép kiểu rõ, test compile/chạy lại đạt.
- CLI ban đầu cảnh báo thread `mysql-cj-abandoned-connection-cleanup` còn sống: thêm JdbcLifecycle shutdown/deregister driver cùng classloader; chạy lại migrate/check không còn cảnh báo.
- Tiếng Việt console Windows Cp1252 có thể sai dấu; dùng terminal/JVM UTF-8. Giao diện HTTP và dữ liệu bind/JDBI đã kiểm đúng tiếng Việt.

## Cách tự chạy và giới hạn

Theo README: tạo schema riêng → cấu hình file local/môi trường → migrate → seed → check → build WAR → deploy Tomcat → mở root và health. Dùng user setup cho DDL rồi user DML cho web. Khi chạy lại dùng migration runner; raw schema chỉ cho schema trống. DB có dữ liệu phải backup/mapping, không DROP/reset/auto baseline; hiện không có legacy schema để viết ALTER cụ thể.

Chưa có credential `c2c_demo` trên WAMP3306 của người dùng; chưa tạo/sửa DB này. MySQL8.4 mục tiêu chưa kiểm thực tế. Tin demo chưa có ảnh nên chưa công khai; login và các nghiệp vụ chưa làm. Helper PasswordHasher chuẩn bị ở M1 vì seed cần đúng hash policy, không phải triển khai auth trước phạm vi.

Sau kiểm tra, instance MySQL và Tomcat riêng được dừng; datadir/config test vẫn được ignore để lưu bằng chứng. Không đóng dịch vụ WAMP của người dùng. WAR bàn giao là `target/demo-1.0-SNAPSHOT.war`.

## Giải thích để trình bày

Browser gửi GET tới Servlet; Controller gọi Service, Service mở Handle qua Database, attach DAO; DAO dùng JDBI truy vấn MySQL bằng tham số bind. Kết quả Model chuyển sang JSP; Handle đóng sau callback, pool đóng khi ứng dụng undeploy. Khi cần nhiều thao tác nhất quán, Service dùng Database.transaction và attach mọi DAO vào cùng Handle, lỗi rollback toàn bộ. M1 kiểm chứng đường kết nối/lifecycle, schema hỗ trợ nghiệp vụ tiếp theo nhưng chưa thực hiện chúng.

Commit đề xuất: `feat: triển khai nền tảng Servlet, schema C2C và kết nối JDBI`.
