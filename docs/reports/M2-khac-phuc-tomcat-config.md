# Khắc phục lỗi tài khoản 503 trên IntelliJ — 04/10/2026

## Nguyên nhân đã khảo sát

Tomcat10.1.48 đang lắng nghe cổng8080, IntelliJ Run Configuration tên “Tomcat 10.1.48”, context `/demo_war_exploded`, docBase target/demo-1.0-SNAPSHOT. Run Configuration không có JVM property c2c.config hoặc các biến APP_CONFIG_FILE/DB_URL/DB_USERNAME/DB_PASSWORD. Command line tiến trình Java cũng không có -Dc2c.config. Terminal Maven đã chọn file local không tự truyền biến sang IntelliJ/Tomcat chạy riêng.

Maven check dùng config/application.local.properties vẫn trả JDBI SELECT 1 = 1. Metadata đọc trên c2c_demo xác nhận 22 bảng, có users với 8 cột tài khoản được DAO đọc. GET home của Tomcat trả 200, GET login có CSRF; POST login với email không hợp lệ/password rỗng và CSRF hợp lệ vẫn trả 503. UserService có mặt sẽ trả lỗi input 400 cho trường hợp này mà không truy vấn DB; 503 chứng minh Service không được khởi tạo vì Tomcat không nhận cấu hình.

Không đọc ra/log mật khẩu hoặc session token. Không sửa database, file cấu hình local hay .idea/workspace.xml; chỉ đọc các dấu hiệu cấu hình IntelliJ. Không reset/áp schema hoặc chạy seed.

## Cách sửa Run Configuration

Trong IntelliJ, Run → Edit Configurations → Tomcat 10.1.48 → Server → VM options, giữ option cũ và thêm:

```text
-Dc2c.config="C:\Users\hausi\OneDrive\Desktop\TMDT\project\web\demo\config\application.local.properties"
```

Hoặc đặt APP_CONFIG_FILE cùng đường dẫn ở Environment variables. Không cần cả hai; c2c.config ưu tiên. Stop/Run lại Tomcat để Listener khởi tạo UserService, không chỉ reload trang. Mở http://localhost:8080/demo_war_exploded/login để thử lại. File local vẫn giữ credential người dùng đã điền; không đặt password vào VM options. Agent không chỉnh .idea/workspace.xml theo quy ước AGENTS.md; bước Run Configuration cần thực hiện trong IntelliJ của người dùng.

## Thay đổi và kiểm tra

- exception/AccountUnavailableException.java: reason cố định để log, public message an toàn giữ nguyên.
- controller/AccountSupport.java: phân biệt DATABASE_NOT_CONFIGURED, DATABASE_CONFIGURATION_INVALID, SERVICE_NOT_INITIALIZED; lỗi SQL/service dùng DATABASE_OPERATION_FAILED mặc định.
- listener/ApplicationListener.java: log hướng dẫn APP_CONFIG_FILE/c2c.config khi thiếu cấu hình; lỗi ConfigurationException đã sanitize được log để nêu khóa/file.
- filter/AccessFilter.java: log mã nguyên nhân, không log cause/SQL/bind/credential.
- src/test/java/com/example/demo/controller/AccountSupportTest.java: hai regression tests cho thiếu config/invalid config/chưa initialize; không cần DB.
- README.md, docs/08-tai-khoan-va-phan-quyen.md, báo cáo này: VM options, restart và context thực tế.

Lệnh thực tế: Maven compile exec:java -Dexec.args=check với APP_CONFIG_FILE → BUILD SUCCESS/SELECT1=1. Metadata SELECT chỉ đọc. HTTP probe không đăng ký/update/login thành công và không ghi DB. Maven verify → 22 unit tests đạt, WAR mới được tạo. Chưa chạy lại integration ghi dữ liệu hoặc đăng nhập thực tế trên database người dùng. Tomcat hiện còn giữ cấu hình cũ đến khi người dùng thêm option và Stop/Run lại; chưa khẳng định lỗi trang đang chạy đã hết.

Commit đề xuất: `fix: làm rõ lỗi thiếu cấu hình MySQL trong Tomcat IntelliJ`.
