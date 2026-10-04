# Báo cáo M2 — Tài khoản và phân quyền

Ngày **04/10/2026**, Asia/Bangkok. Hoàn thành đăng ký, đăng nhập, đăng xuất POST, hồ sơ và phân quyền; chưa triển khai sản phẩm/giỏ/đơn/review/khiếu nại. Hướng dẫn thao tác ở [08](../08-tai-khoan-va-phan-quyen.md), setup/build/deploy ở [README](../../README.md).

## Kết quả

- Email là định danh login, normalize trim/lowercase và UNIQUE DB; hai đăng ký đồng thời chỉ tạo một tài khoản, bên còn lại nhận lỗi email trùng. Password BCrypt 2b/cost12 tương thích cả năm seed; không nhận role/status của client, luôn USER/ACTIVE.
- Login kiểm ACTIVE, sai mật khẩu/email không tồn tại/inactive cùng thông báo; đổi JSESSIONID và CSRF token; session chỉ CurrentUser(id,displayName,role), không email/hash/password. USER về home, ADMIN về trang quản trị tối thiểu.
- Hồ sơ chỉ sửa display_name/phone/public_contact; Service lấy actor từ session, khóa row trong transaction và kiểm ACTIVE. Email/role/status/hash không thay đổi; userId/id/role/status/email gửi thêm bị bỏ qua. Session cập nhật tên, request sau refresh ACTIVE/role từ DB.
- EncodingFilter chạy trước AccessFilter theo web.xml. AccessFilter chặn GET/POST các tuyến cá nhân/admin trên server, kiểm CSRF cho POST gồm register/login/logout/profile; cache no-store, HttpOnly/SameSite=Lax, timeout 30 phút. HTTPS cần cấu hình connector/proxy đúng để dùng Secure cookie.
- JSP tiếng Việt, lỗi cạnh trường, giữ dữ liệu không nhạy cảm đã escape, không điền lại password, không đưa hash vào view. POST thành công redirect 303, logout invalidate; replay cookie cũ không vào lại trang riêng.
- Dùng nguyên bảng users hiện có và constraint uq_users_email. Không cần migration mới, không sửa V001/schema/seed. Giữ file cấu hình local và database ứng dụng của người dùng.

## File tạo và sửa

Đường dẫn Java dưới `src/main/java/com/example/demo/`:

| Nhóm | File |
| --- | --- |
| Servlet mới | controller/auth/RegisterServlet.java, LoginServlet.java, LogoutServlet.java, ProfileServlet.java; controller/admin/AdminServlet.java; controller/AccountSupport.java |
| Service/DAO mới | service/UserService.java, AccountValidation.java; dao/UserDao.java |
| Model/DTO mới | model/CurrentUser.java, UserAccount.java, UserProfile.java; dto/RegisterForm.java, ProfileForm.java |
| Bảo mật/lỗi mới | security/SessionAuth.java, CsrfTokens.java; filter/AccessFilter.java; exception/FormException.java, AccountUnavailableException.java |
| Java sửa | config/Database.java; listener/ApplicationListener.java; filter/EncodingFilter.java; security/PasswordHasher.java |
| JSP mới | src/main/webapp/WEB-INF/views/auth/register.jsp, login.jsp, profile.jsp; admin/index.jsp; errors/message.jsp; layouts/navigation.jspf |
| UI/config sửa | src/main/webapp/WEB-INF/views/home.jsp; src/main/webapp/WEB-INF/web.xml; src/main/webapp/assets/css/startup.css |
| Test mới | src/test/java/com/example/demo/service/AccountValidationTest.java, UserServiceIT.java; security/CsrfTokensTest.java; controller/AccountHttpIT.java |
| Tài liệu sửa/tạo | AGENTS.md, README.md, docs/README.md, 02-yeu-cau-va-quy-tac.md, 03-kien-truc.md, 04-thiet-ke-database.md, 05-ke-hoach-trien-khai.md; docs/08-tai-khoan-va-phan-quyen.md và báo cáo này |

19 lớp Java mới; không đổi namespace Servlet/dependency/build tool. Giữ HomeServlet/HelloServlet và CSS cũ rồi bổ sung. `target/` và `.local-test/` là đầu ra kiểm thử/backup/runtime được ignore. Không sửa .idea/workspace.xml, không có Git metadata nên chưa commit.

## Khảo sát và quyết định

Bảng users đã có email unique, hash, tên/phone/public_contact, role/status và timestamp; đủ M2, không ALTER. Chốt email readonly ở hồ sơ vì thay email cần cơ chế xác minh riêng; phone/contact tùy chọn theo thiết kế. Password tối thiểu 8 ký tự Unicode, tối đa 72 byte UTF-8; BCrypt không được cắt chuỗi dài. Giới hạn và email ASCII thông thường đã ghi ở 02/08.

Kế hoạch M2 có câu “seed admin không có mật khẩu mặc định công khai trong source” chưa khớp fixture M1 đã công khai. Đã sửa kế hoạch: seed demo dùng mật khẩu fixture trong README, chỉ cho demo; không nhúng credential thật. Không thêm quản trị người dùng/đổi mật khẩu/email verification ngoài phạm vi.

Hạ tầng được sửa hai điểm phát hiện qua kiểm tra: Database chỉ định MySQL driver class để tạo pool mới sau khi lifecycle đã deregister driver trong cùng JVM test; cấu hình cookie Servlet 6 phải dùng attribute-name/attribute-value. Thứ tự encoding/auth/CSRF đặt rõ trong web.xml; EncodingFilter bỏ annotation để không đăng ký trùng.

## Môi trường và lệnh thực tế

Java17.0.12, Wrapper Maven3.9.6, MySQL9.1.0 binary WAMP nhưng datadir riêng `.local-test/mysql-data`, bind loopback **13316**; schema mới **c2c_m2_test**. Tomcat10.1.48, CATALINA_BASE `.local-test/tomcat`, HTTP **18080**, context `/c2c`. File test riêng `.local-test/m2.local.properties`, không dùng config/application.local.properties của người dùng. Trước migration/tests có dump `.local-test/m2-before-tests.sql`; schema test ban đầu trống. Không tác động service WAMP3306/MariaDB hoặc database ứng dụng.

| Kiểm tra/lệnh | Kết quả thực tế |
| --- | --- |
| `.\mvnw.cmd -B verify` sau code ban đầu | Compile/WAR thành công, 13 unit tests M1 đạt. |
| Build sau thêm unit test | Test compile lỗi biến lambda chưa effectively final; sửa tên biến và chạy lại. |
| mysqld --no-defaults, datadir riêng/port13316; SELECT VERSION/SHOW DATABASES | Instance riêng chạy MySQL9.1.0; chưa có schema c2c_m2_test trước CREATE. |
| CREATE DATABASE IF NOT EXISTS c2c_m2_test; mysqldump --result-file trong .local-test | Tạo schema test, backup trước thay đổi; không DROP/reset. |
| `APP_CONFIG_FILE=<file test>`, `C2C_IT_ALLOWED=true`, `.\mvnw.cmd -B -Pmysql-it verify` lần đầu | 20 unit/11 DB tests đạt; UserServiceIT bị chặn vì driver đã deregister trong lớp trước. Sửa Database driver initialization. |
| Deploy WAR lần đầu lên Tomcat test | web.xml cookie attribute sai cú pháp, context 404. Đọc XSD Servlet6 trong servlet-api.jar Tomcat; sửa attribute-name/value, build/deploy lại. |
| Profile mysql-it sau sửa driver/cookie, chưa bật HTTP | BUILD SUCCESS, 20 unit + 18 integration (11 DB + 7 UserService); HTTP không chạy vì chưa đặt C2C_HTTP_BASE. |
| Deploy WAR cuối, HTTP `/c2c/login` | 200, JSP tiếng Việt và CSRF token; cookie HttpOnly/SameSite=Lax. |
| `C2C_HTTP_BASE=http://127.0.0.1:18080/c2c`, `.\mvnw.cmd -B -Pmysql-it verify` | **BUILD SUCCESS; 20 unit + 24 integration (11 DB + 7 Service + 6 HTTP), không fail/error/skip.** |
| MySQL metadata và fixture sau toàn bộ test | 23 bảng vật lý (22 nghiệp vụ + Flyway), 5 user seed; 0 fixture email HTTP/Service còn lại. |
| Rà SQL/Controller/log | DAO bind input, Controller không đọc id/role/status để update; không javax.servlet, không log password/hash/email trong auth. CLI demo-hashes M1 giữ nguyên, chỉ sinh fixture hash. |
| `.\mvnw.cmd -B clean verify` cuối lượt | BUILD SUCCESS, 20 unit tests đạt; tạo lại sạch target/demo-1.0-SNAPSHOT.war. |
| Catalina stop, mysqladmin shutdown trên port test và kiểm listener/service | Đã dừng Tomcat18080/MySQL13316 riêng; không còn listener test. Các service WAMP MySQL/MariaDB vẫn chạy. |

HTTP test gọi Servlet/JSP thật qua HttpClient cookie jar, không mock server. Đã kiểm:

- Register hợp lệ, duplicate email khác hoa/thường, form sai, password không được điền lại, role ADMIN/status injected bị bỏ qua.
- Login đúng/sai/unknown/inactive; seed USER/ADMIN đúng điều hướng, name xuất hiện. JSESSIONID thay đổi, cookie trước login không vào profile.
- Guest GET/POST admin/profile bị redirect; USER GET/POST admin và admin path con bị 403. Role đổi trong DB test có hiệu lực ở request sau.
- Profile đổi hợp lệ, input sai không ghi, ID người khác/email/role/status injected không đổi người khác hoặc quyền; XSS tên/contact escape, hash không có trong HTML, session hiển thị tên mới.
- CSRF thiếu/sai/pre-login token bị chặn; GET logout không hủy session; POST logout hủy, replay cookie sau logout không vào profile.

Service test thêm hai request register đồng thời; DB unique bảo vệ, không plaintext; inactive không authenticate/update và Handle trả pool. Unit tests kiểm validation UTF-8/Unicode, format/length, token session/rotation. Test DB M1 tiếp tục kiểm schema/constraint, transaction, snapshot, ledger và seed chạy lại.

## Giới hạn và cách chạy trên máy người dùng

Chỉ kiểm trên MySQL9.1/Tomcat10.1 loopback riêng; chưa kiểm MySQL8.4, HTTPS/proxy hoặc trình duyệt tương tác thủ công. Giao diện JSP/HTTP và XSS escaping đã kiểm qua HTTP thật. Không chạy chức năng M2 trên c2c_demo của người dùng, không migrate/seed database đó trong lượt này. Không có email verification/reset password/quản trị user, rate limiting hoặc nghiệp vụ M3 trở đi; đây không phải các chức năng được yêu cầu cho M2.

Theo README/08: chọn APP_CONFIG_FILE → check → migrate/seed nếu database phát triển cần setup theo điều kiện an toàn → clean verify → deploy WAR mới trên Tomcat10.1 với cùng config → mở register/login/profile/admin. Fixture seed dùng password công khai dành cho demo. Schema test phải riêng và cùng cấu hình với Tomcat test nếu chạy HTTP suite.

## Luồng để trình bày

Browser → Encoding/AccessFilter → LoginServlet → UserService → UserDao → JDBI → MySQL. Service xác minh BCrypt/ACTIVE; Servlet đổi session ID và lưu CurrentUser. Request sau Filter refresh trạng thái/quyền từ DB, kiểm CSRF trước POST; ProfileService khóa row của actor trong cùng Handle transaction, DAO chỉ update các cột được phép. JSP dùng DTO không hash và c:out. Logout POST invalidate; Handle đóng sau callback và pool đóng khi undeploy.

Commit đề xuất: `feat: triển khai tài khoản, hồ sơ và phân quyền USER ADMIN`.
