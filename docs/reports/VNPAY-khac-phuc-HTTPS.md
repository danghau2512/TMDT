# Khắc phục HTTPS VNPAY — 05/10/2026

## Nguyên nhân và thay đổi

Người dùng gặp thông báo Java chưa xác thực chứng chỉ HTTPS khi bấm kiểm tra thanh toán. Khảo sát chỉ các cờ cấu hình của tiến trình Java: Tomcat IntelliJ đang chạy bằng `C:/Program Files/Java/jdk-17/bin/java.exe`, có `c2c.config` trỏ đúng `config/application.local.properties`, nhưng chưa có `javax.net.ssl.trustStoreType=Windows-ROOT` và `javax.net.ssl.trustStore=NUL`. Không xuất toàn bộ command line hoặc giá trị credential. Các VM options hướng dẫn ở lượt trước chưa được áp vào JVM Tomcat này.

Bổ sung `vnpay.tls.useWindowsRoot` / `VNPAY_TLS_USE_WINDOWS_ROOT`, mặc định false để tương thích OS khác. Khi true, client VNPAY nạp `Windows-ROOT` qua SunMSCAPI, tạo TrustManagerFactory/SSLContext riêng và vẫn xác thực hostname HTTPS. Không thay đổi truststore của các client khác trong JVM; không trust-all, không bỏ xác thực hoặc tự import chứng chỉ. Thiếu provider/kho CA hợp lệ thì báo lỗi cấu hình an toàn, không kết nối bằng cơ chế không xác thực.

Đã bật true trong file local hiện tại; giữ các khóa DB/storage/VNPAY credential đã điền. File local tiếp tục bị ignore, không đóng gói WAR. Không sửa `.idea/workspace.xml`, không cài CA toàn máy, không dừng tiến trình Tomcat do IntelliJ quản lý.

## File tạo/sửa

Các đường dẫn tính từ gốc dự án:

| File | Nội dung |
| --- | --- |
| `src/main/java/com/example/demo/config/VnpayConfig.java` | Khóa truststore có validation, env ưu tiên file; constructor cũ giữ mặc định false |
| `src/main/java/com/example/demo/payment/VnpayTls.java` | Tạo SSLContext Windows-ROOT riêng; lỗi an toàn |
| `src/main/java/com/example/demo/payment/VnpayHttpClient.java` | Client chọn SSLContext, xác thực hostname; HEAD kiểm tra TLS không payload |
| `src/main/java/com/example/demo/config/DatabaseTool.java` | Lệnh vnpay-check đi nhánh riêng, không khởi tạo Database/JDBI |
| `src/test/java/com/example/demo/payment/VnpayProtocolTest.java` | Mặc định, env override, khóa không hợp lệ và không tiết lộ giá trị |
| `config/application.example.properties` | Khóa mới mặc định false và ghi chú Windows |
| `config/application.local.properties` (cục bộ, không commit) | Bật true, giữ credential khác |
| `README.md`, `docs/README.md`, `docs/12-vnpay-sandbox.md`, báo cáo này | Cách kiểm tra, rebuild/restart, bằng chứng và giới hạn |

## Kiểm tra thực tế

Chạy trong PowerShell tại gốc dự án:

```powershell
.\mvnw.cmd -B verify
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
.\mvnw.cmd -B exec:java '-Dexec.args=vnpay-check'
```

- `verify`: **BUILD SUCCESS**, 28 unit tests, 0 failures/errors/skipped; đã tạo `target/demo-1.0-SNAPSHOT.war`.
- `vnpay-check`: **BUILD SUCCESS**, **VNPAY HTTPS OK, HTTP 405**. HEAD tới query endpoint, không body, không chữ ký/secret/dữ liệu thanh toán. HTTP 405 xác nhận đã qua TLS và nhận HTTP dù endpoint không hỗ trợ HEAD. Client thực tế dùng khóa local true, không cần thêm các VM options truststore.
- `.\mvnw.cmd -v`: Maven 3.9.6, Oracle Java 17.0.12 tại `C:/Program Files/Java/jdk-17`, cùng JDK với Tomcat đã khảo sát. Kiểm tra danh sách entry WAR: có lớp `VnpayTls`, không có `application.local.properties`.
- Không chạy SQL, không mở DB/JDBI trong kiểm tra TLS, không gửi truy vấn tài chính, không sửa trạng thái đơn/thanh toán hoặc tạo thanh toán mới.
- Không chạy lại integration tests ghi dữ liệu, không kiểm lại browser/payment trên Tomcat đang chạy bản cũ. 39 integration/HTTP tests và giao dịch Sandbox ở báo cáo trước là kết quả lịch sử, không phải kiểm tra của lượt sửa này.

## Bước cần thực hiện trong IntelliJ

1. Rebuild artifact `demo:war exploded` (hoặc artifact WAR đang deploy) từ source mới; Maven đã build WAR nhưng không tự thay artifact đang chạy trong IntelliJ.
2. **Stop rồi Run Tomcat lại**, giữ `-Dc2c.config` trỏ file local hiện tại. Java/config được đọc khi khởi động.
3. Mở đơn đang gặp lỗi và bấm **Kiểm tra kết quả thanh toán** sau thời gian chờ tối thiểu. Service vẫn chỉ xác nhận PAID sau response có chữ ký và các điều kiện merchant/ref/amount/status hợp lệ; kết nối được không tự đổi trạng thái.

Nếu vẫn lỗi: chạy vnpay-check với cùng file/JDK và tài khoản Windows của Tomcat, kiểm tra biến môi trường override và kho CA Windows. Khi CLI thành công mà Tomcat còn thông báo cũ, xác minh artifact mới đã deploy và JVM đã restart. Nếu cả hai lỗi, kiểm tra mạng/proxy và CA tin cậy, không tắt HTTPS hoặc thanh toán lại để thử chứng chỉ.

Thông điệp commit gợi ý (chưa commit; thư mục chưa là Git repository): `Khắc phục HTTPS VNPAY bằng truststore Windows và thêm kiểm tra TLS an toàn`.
