# 08 — Chạy và kiểm tra tài khoản M2

M2 có đăng ký, email login, logout POST, hồ sơ và trang Admin tối thiểu. Một USER dùng được cả ngữ cảnh mua/bán ở các mốc sau. Chưa có chức năng sản phẩm, giỏ hàng hoặc đơn.

## Chuẩn bị và triển khai trên Windows

Giữ cấu hình local hiện có, không copy mẫu đè lên. Bảng users trong V001 đã đủ cho M2, không có migration bổ sung. Nếu database chưa có cấu trúc hoặc fixture, thực hiện tạo schema/migrate/seed theo README và database/README; database có dữ liệu phải khảo sát/backup trước, không áp raw schema/reset. Seed chỉ cho môi trường demo và không ghi đè tài khoản đã tồn tại.

```powershell
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
.\mvnw.cmd -B compile exec:java '-Dexec.args=check'
.\mvnw.cmd -B clean verify
$env:CATALINA_HOME = 'D:\cong_cu_nen\apache-tomcat-10.1.48' # sửa theo máy
# Dừng Tomcat hiện tại trước khi thay WAR. Nếu có CATALINA_BASE riêng, dùng webapps của BASE.
Copy-Item .\target\demo-1.0-SNAPSHOT.war "$env:CATALINA_HOME\webapps\c2c.war"
& "$env:CATALINA_HOME\bin\catalina.bat" run
```

Tomcat phải nhận APP_CONFIG_FILE từ chính terminal khởi chạy hoặc Run Configuration IntelliJ. Cấu hình secret không được đóng gói trong WAR. URL dưới đây giả định http://localhost:8080/c2c; đổi cổng/context thực tế. HTTP local dùng cookie HttpOnly/SameSite=Lax; HTTPS cần connector/proxy đúng để cookie Secure được áp dụng. Session hết hạn sau 30 phút không hoạt động.

Nếu chạy IntelliJ và gặp trang “Chức năng tài khoản tạm thời không khả dụng” dù Maven check đã đạt: mở Run → Edit Configurations → Tomcat → Server → VM options, thêm `-Dc2c.config="C:\Users\hausi\OneDrive\Desktop\TMDT\project\web\demo\config\application.local.properties"` (sửa đường dẫn theo máy, giữ option cũ), rồi Stop/Run lại. Không cần copy lại file local hoặc reset database. Context cấu hình Tomcat người dùng đang dùng là `/demo_war_exploded`. Xem [báo cáo khắc phục 503](reports/M2-khac-phuc-tomcat-config.md).

## Tuyến và thành phần

| URL / phương thức | Servlet | JSP / quyền |
| --- | --- | --- |
| GET, POST `/register` | controller/auth/RegisterServlet | views/auth/register.jsp; công khai, POST CSRF, role cố định USER |
| GET, POST `/login` | controller/auth/LoginServlet | views/auth/login.jsp; công khai, POST CSRF |
| POST `/logout` | controller/auth/LogoutServlet | Hủy session → `/home?notice=logged-out`; GET không logout |
| GET, POST `/account/profile` | controller/auth/ProfileServlet | views/auth/profile.jsp; chủ session ACTIVE |
| GET `/admin`, `/admin/` | controller/admin/AdminServlet | views/admin/index.jsp; chỉ ADMIN |
| GET `/`, `/home` | HomeServlet qua index.jsp khi root | views/home.jsp, hiển thị người dùng và form logout |
| Mọi request thường | EncodingFilter → AccessFilter | Đăng nhập/quyền/CSRF; errors/message.jsp cho lỗi an toàn |

JSP nằm dưới src/main/webapp/WEB-INF, fragment chung views/layouts/navigation.jspf; CSS kế thừa assets/css/startup.css. Filter chặn cả GET/POST tuyến riêng account/admin/seller/buyer/cart/checkout; các tuyến mốc sau chưa có Servlet nên vẫn 404 nếu đã đủ quyền. Không nhận `returnUrl` từ browser để redirect ra ngoài.

## Quy tắc biểu mẫu

Email đăng nhập được trim/lowercase và bảo vệ bằng UNIQUE DB. Họ tên 2–100 ký tự, phone tùy chọn 9–15 chữ số có thể có +; mật khẩu ít nhất 8 ký tự Unicode, tối đa 72 byte UTF-8, xác nhận khớp. Password không trim và không điền lại khi lỗi. Hồ sơ chỉ đổi họ tên/phone/liên hệ công khai (255 ký tự), email readonly; role/status/ID client bị bỏ qua. Liên hệ công khai là text tự nguyện, không render thành HTML.

POST thành công trả 303; form sai trả 400 kèm lỗi gần trường và dữ liệu không nhạy cảm đã escape. Sai quyền hoặc CSRF trả 403. DB/schema chưa sẵn sàng trả 503 với thông báo chung, không có SQL/hash/credential. CSRF thiếu/sai cần mở lại trang để lấy token. Login chuyển USER về home, ADMIN về admin, refresh token và mã phiên; logout vô hiệu session cũ.

## Kiểm tra thủ công

1. Mở `/register`, đăng ký email mới; kiểm trường trống/email sai/phone sai/xác nhận khác. Thành công chuyển login với thông báo; thử trùng email và chữ hoa để nhận lỗi.
2. Đăng nhập đúng/sai/email chưa đăng ký. Sau seed trên demo, dùng admin@c2c.example hoặc buyer1@c2c.example với mật khẩu fixture `C2cDemo!2026`; USER về home, ADMIN về admin. Mật khẩu này không phải mật khẩu MySQL.
3. Trình duyệt chưa login vào profile/admin phải chuyển login. USER vào admin trực tiếp hoặc POST admin bị 403. Đăng ký kèm role=ADMIN/status khác vẫn USER/ACTIVE.
4. Cập nhật hồ sơ, thêm userId/id của người khác hoặc role/status/email qua DevTools; chỉ hồ sơ chủ session thay đổi và các trường bị cấm giữ nguyên. Nhập `<script>alert(1)</script>` ở tên: hiển thị text, không chạy script.
5. Xóa hoặc thay csrfToken trong POST register/login/profile/logout: 403 và không ghi. Login đổi JSESSIONID/token; không chia sẻ các giá trị đó vào chat/log. Cookie HttpOnly/SameSite=Lax.
6. Logout POST rồi vào lại profile/admin: chuyển login; replay cookie cũ không khôi phục đăng nhập. GET logout không hủy phiên. Nếu người có quyền đổi status/role trực tiếp trong DB test, request sau refresh quyền/ngừng phiên tương ứng; không thử việc này trên tài khoản thật.

## Test tự động

Build thường có 22 unit tests, không cần MySQL. Profile mysql-it chạy 11 test hạ tầng + 7 UserService tests; bắt buộc cấu hình database riêng hậu tố `_test`, chỉ có seed demo và user có DDL/DML. Chạy migrate/seed trên schema test qua runner, không dùng c2c_demo.

```powershell
$env:APP_CONFIG_FILE = 'C:\duong-dan\c2c_test.local.properties' # sửa, schema *_test
$env:C2C_IT_ALLOWED = 'true'
.\mvnw.cmd -B -Pmysql-it verify
```

Để thêm 6 HTTP tests, build/deploy WAR lên Tomcat test loopback dùng **chính cấu hình database test trên**, khởi động Tomcat với APP_CONFIG_FILE đó, rồi:

```powershell
$env:C2C_HTTP_BASE = 'http://127.0.0.1:8080/c2c' # đúng cổng/context Tomcat test
.\mvnw.cmd -B -Pmysql-it verify
```

HTTP tests không chạy nếu thiếu C2C_HTTP_BASE. Tests tạo fixture reserved email và xóa chính fixture sau mỗi test; kiểm role/ID/CSRF/XSS, seed login, status/role refresh, fixation, logout/replay bằng HttpClient cookie thật. Không chạy song song các bộ test trên cùng schema hoặc schema có dữ liệu người dùng. Đã chạy 20 + 24 tests đạt trên MySQL9.1/Tomcat10.1 riêng; xem báo cáo M2.

## Giải thích với giáo viên

Servlet nhận email/mật khẩu và gọi UserService; Service normalize, dùng UserDao bind email qua JDBI, xác minh BCrypt và ACTIVE. Session chỉ giữ ID/tên/quyền, đổi ID chống dùng lại phiên cũ. Request sau đi qua AccessFilter, đọc lại trạng thái/quyền từ MySQL rồi kiểm CSRF trước POST. UserService lấy actor từ session khi sửa hồ sơ, khóa user trong transaction; DAO chỉ update các cột được phép. JSP nhận DTO không có hash và escape dữ liệu. Mỗi callback JDBI đóng Handle, connection trả về pool.
