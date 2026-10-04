# Khắc phục cấu hình DatabaseTool — 04/10/2026

Nguyên nhân kiểm tra tại repository: chưa có `config/application.local.properties`; APP_CONFIG_FILE và các biến DB không có trong môi trường tiến trình kiểm tra. File mẫu không tự được đọc. DatabaseTool trước đây che mọi lỗi cấu hình bằng một thông báo chung. Không suy ra trạng thái biến trong terminal/IntelliJ khác của người dùng.

Đã tạo file local từ mẫu chỉ khi chưa tồn tại; không điền hay suy đoán credential thật. Host/port mẫu 127.0.0.1:3306, schema c2c_demo, username mẫu c2c_app cần đối chiếu MySQL thực tế. Mật khẩu mẫu còn chưa được thay; vì vậy migrate bị chặn trước khi tạo pool/kết nối. File local thuộc quy tắc ignore `/config/*.local.properties`, ngoài resources/WAR.

## Thay đổi

- `src/main/java/com/example/demo/config/ConfigurationException.java`: loại lỗi chỉ chứa thông báo cấu hình an toàn.
- `AppConfig.java`: báo selector rỗng, đường dẫn không đọc được, properties sai cú pháp, khóa số/boolean sai; không chuyển tiếp giá trị/cause. Thứ tự chọn file: c2c.config > APP_CONFIG_FILE; biến môi trường từng khóa > file. Không có file mặc định.
- `DatabaseConfig.java`: thông báo riêng cho URL, username, password, pool size và timeout; giữ điều kiện giá trị hiện có.
- `DatabaseTool.java`: chuyển tiếp thông báo cấu hình an toàn, hướng dẫn chọn file nếu thiếu toàn bộ DB; tiếp tục che lỗi kết nối.
- `src/test/java/com/example/demo/config/AppConfigTest.java`: thêm 6 test cho lựa chọn file, path lỗi, cú pháp, numeric/boolean/range, secret redaction và giá trị rỗng.
- `SchemaFilesTest.java`: bỏ qua whitespace ngoài cùng/CRLF khi so sánh standalone với V001. Build đầu thất bại vì schema.sql có bốn khoảng trắng trước chú thích đầu; hai file còn lại giống nhau. Giữ nguyên cả hai script SQL, không sửa migration đã áp.
- `config/application.example.properties`: chú thích nơi điền host/port/schema/username/password và escape backslash.
- `config/application.local.properties`: file riêng được tạo từ mẫu, không commit.
- `README.md`, `docs/07-chuan-bi-moi-truong.md`, `docs/README.md`, báo cáo này: hướng dẫn copy không ghi đè, lựa chọn file, bảng điều kiện và lệnh PowerShell thực tế.

## Kiểm tra thực tế

| Lệnh | Kết quả |
| --- | --- |
| `.\mvnw.cmd -B clean verify` lần đầu | Compile đạt, 13 tests chạy; 1 test so sánh schema nguyên văn thất bại do whitespace nêu trên. |
| `.\mvnw.cmd -B clean verify` sau sửa test | BUILD SUCCESS, 13 tests đạt; tạo `target/demo-1.0-SNAPSHOT.war`. |
| `.\mvnw.cmd -B compile exec:java '-Dexec.args=migrate'` không selector/DB env | Exit 1, nêu chưa có DB, hướng dẫn APP_CONFIG_FILE/c2c.config và ba khóa bắt buộc. |
| Lệnh trên với APP_CONFIG_FILE chọn file local mẫu | Exit 1, nêu DB_PASSWORD / db.password chưa thiết lập, không in giá trị. |
| Lệnh trên với `"-Dc2c.config=$localConfigPath"` chọn file local mẫu | Exit 1, cùng lỗi db.password; xác minh Maven exec:java nhận được system property. |
| Lệnh trên với `'-Dc2c.config=config/missing-config-check.properties'` | Exit 1, nêu đường dẫn tuyệt đối của file không đọc được. |

Các lần migrate trên là kiểm tra lỗi trước kết nối; không chạy SQL, không xác minh credential/schema/quyền MySQL. Không chạy lại integration hoặc deploy Tomcat trong lượt khắc phục này. Console môi trường kiểm tra làm lỗi dấu tiếng Việt, nhưng tên khóa/đường dẫn đọc được; có thể thêm `-Dfile.encoding=UTF-8` vào MAVEN_OPTS trước khi khởi động Maven theo README. Test xác minh thông báo tiếng Việt/secret ở mức chuỗi Java.

## Cách tiếp tục trên máy

Mở file local bằng Notepad, xác nhận host/port, schema c2c_demo đã tạo và user có quyền DDL; điền mật khẩu MySQL trong file, không trong command line. Chạy từ thư mục có pom.xml:

```powershell
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
notepad $env:APP_CONFIG_FILE
# Lưu file và đóng Notepad trước khi chạy:
.\mvnw.cmd -B compile exec:java '-Dexec.args=migrate'
.\mvnw.cmd -B exec:java '-Dexec.args=check'
```

Nếu có DB_* trong terminal/service, chúng ghi đè file kể cả khi rỗng; kiểm tra tên biến mà không xuất giá trị. Lệnh dùng `.\mvnw.cmd` và goal `exec:java`. Hướng dẫn tạo schema và điều kiện migrate database đã có dữ liệu nằm ở README/database README; không DROP/reset.

Commit đề xuất: `fix: làm rõ cấu hình MySQL và lỗi migrate an toàn`.

## Xác minh sau khi người dùng sửa file local — 04/10/2026

Người dùng đã sửa credential trong file local nhưng chạy check mà chưa chọn file. Kiểm tra chỉ xác nhận file có đủ ba khóa và password được khai báo rỗng, không xuất giá trị credential; không sửa hay ghi đè file local. APP_CONFIG_FILE và các biến DB chưa có trong môi trường tiến trình kiểm tra.

Đã chạy:

```powershell
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
.\mvnw.cmd -B compile exec:java '-Dexec.args=check'
```

Kết quả thực tế: `JDBI SELECT 1 = 1`, `BUILD SUCCESS`, exit code 0. File local hiện đủ để kết nối MySQL. Lỗi người dùng gửi là thiếu selector, không phải lỗi xác thực password. Biến được đặt trong tiến trình kiểm tra của agent không tự xuất hiện trong terminal người dùng; cần chạy hai dòng trên trong terminal của người dùng và đặt lại nếu mở terminal mới.

Lượt xác minh này chỉ chạy SELECT 1, không migrate/seed và không thay đổi dữ liệu. Chưa kiểm tra cấu trúc 22 bảng hoặc dữ liệu mẫu trên database được cấu hình. Source và file local được giữ nguyên; chỉ bổ sung kết quả vào báo cáo và README.
