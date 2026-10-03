# 06 — Kết quả kiểm tra lượt khảo sát/thiết kế

Ngày thực hiện: **03/10/2026**, Asia/Bangkok.

## File được tạo

- `AGENTS.md`: quy ước và bất biến nghiệp vụ cho các lượt tiếp theo.
- `docs/README.md`: mục lục và các quyết định chính.
- `docs/00-yeu-cau-goc.txt`: bản sao nguyên văn tệp người dùng.
- `docs/01-khao-sat-repository.md`: khảo sát có bằng chứng và phiên bản.
- `docs/02-yeu-cau-va-quy-tac.md`: phạm vi, vai trò, giả định, state machine.
- `docs/03-kien-truc.md`: tầng ứng dụng, thư mục và bảo mật.
- `docs/04-thiet-ke-database.md`: 22 bảng, khóa, snapshot và transaction.
- `docs/05-ke-hoach-trien-khai.md`: 9 mốc/tiêu chí có thể kiểm tra.
- `docs/06-kiem-tra-luot-dau.md`: báo cáo này.
- `docs/07-chuan-bi-moi-truong.md`: điều kiện để triển khai M1, đã biết chưa có schema riêng.
- `README.md` và `config/application.example.properties`: hướng dẫn/bản mẫu cấu hình, chưa có code đọc cấu hình.

Không sửa `pom.xml`, Java/JSP/web.xml, wrapper, `.gitignore` hoặc cấu hình IDE; `target/` phát sinh từ build và đã thuộc ignore. Không thực thi SQL hoặc dùng credential DB. Không có Git metadata nên không tạo commit và không thể dùng git diff làm bằng chứng thay đổi.

## Kiểm tra thực tế

| Kiểm tra/lệnh | Kết quả |
| --- | --- |
| Liệt kê source và file ẩn; đọc pom, Java, JSP, web.xml, wrapper/IDE | PASS: xác định bộ khung Hello World và không có cấu hình/schema DB trong repository |
| Kiểm AGENTS ở thư mục cha | Không thấy quy ước cha |
| `git status --short`, `git rev-parse --show-toplevel` | Thất bại: not a git repository; ghi nhận giới hạn, không phải lỗi build |
| `java -version`, `javac -version` | JDK 17.0.12 |
| `mvn -version` | Maven PATH 3.9.14, Java 17 |
| `.\mvnw.cmd -version` | Wrapper Maven 3.9.6, Java 17 |
| `mysql --version` | Không tìm thấy trên PATH; đã kiểm tiếp service/binary thay vì kết luận chưa cài |
| `Get-Service` và `Get-CimInstance Win32_Service` lọc tên WAMP | `wampmysqld64`, `wampmariadb64` Running; biết path MySQL riêng |
| `C:\wamp64\bin\mysql\mysql9.1.0\bin\mysqld.exe --version` | MySQL Community Server 9.1.0, không khởi động thêm service |
| `mvn -B -DskipTests=false verify` | **BUILD SUCCESS**, WAR được tạo, không có test source |
| `.\mvnw.cmd -B verify` | **BUILD SUCCESS**, Maven 3.9.6; không có test nghiệp vụ |
| `.\mvnw.cmd -B clean verify` sau hoàn lại thay đổi triển khai tạm thời | **BUILD SUCCESS**, biên dịch duy nhất HelloServlet, WAR chỉ chứa bộ khung ban đầu |
| SHA-256 các file source/build config/wrapper trước và sau tài liệu | PASS: 8 file được theo dõi không đổi |
| So sánh hash tệp yêu cầu gốc và bản sao docs | PASS: bản sao nguyên văn |
| Kiểm link nội bộ và mục từ điển dữ liệu | PASS: đích tài liệu tồn tại, đủ 22 bảng đánh số |

Build tạo `target/demo-1.0-SNAPSHOT.war`. Maven PATH và wrapper dùng default plugin khác nhau; đã ghi kế hoạch pin plugin tại M1, chưa sửa pom trong lượt này.

## Những phần chưa kiểm tra được

- Chưa deploy Tomcat, chưa mở trang JSP qua HTTP; runtime Tomcat mới có tên trong cấu hình IDE, chưa xác minh installation.
- Chưa kết nối MySQL, chưa biết schema/port/credential/dữ liệu hiện có; phiên bản xác minh từ binary, chưa phải query server.
- Chưa chạy migration/DDL, chưa kiểm constraint trên engine, chưa có test auth/order/concurrency/upload. Các tiêu chí ở kế hoạch là **việc cần làm**, không phải kết quả PASS.
- Thiết kế MySQL 8.4/JDBI/JSTL là bộ mục tiêu; chưa xác minh tích hợp dependency mới vì lượt này giữ nguyên code.
- Chưa chốt thương hiệu/ngành hàng; phí giao 0, không tự hết hạn giữ hàng và các quy tắc còn thiếu được ghi như giả định đề xuất ở 02.

## Diễn giải để trình bày với giáo viên

Một người vừa mua vừa bán được biểu diễn bởi một tài khoản và hai quan hệ buyer/seller trong đơn. Khi một giỏ chứa nhiều seller, Service chia thành đơn bán riêng nhưng tạo trong một transaction để không có giao dịch nửa chừng. Lưu snapshot trong item giúp biết chính xác nội dung tin tại lúc đặt, kể cả seller sửa sau đó; giữ ảnh bất biến để bằng chứng còn dùng được. Lịch sử ghi ai/thời gian/trạng thái, thanh toán mô phỏng có lịch sử riêng. Review chỉ sau hoàn thành, khiếu nại tiếp nhận cả trước hoàn thành. Tồn khả dụng được trừ/hoàn dưới khóa và nhật ký, cần kiểm thử đồng thời thật ở lượt triển khai.

Bước tiếp theo: M1 trong kế hoạch, với thông tin Tomcat installation và DB/schema/credential qua môi trường cục bộ. Việc chưa truy cập được DB không cản trở hoàn thành tài liệu thiết kế của lượt này.

## Điều chỉnh phạm vi cuối lượt

Người dùng gửi yêu cầu triển khai M1 rồi trả lời làm rõ rằng lượt hiện tại chỉ khảo sát/thiết kế và chưa kiểm thử database thật. Đã hoàn lại các lớp hạ tầng/JSP/CSS/pom/ignore bắt đầu tạm thời; SHA-256 source và build config được đối chiếu lại với bản gốc. Build tạm thời `-DskipTests package` đã tải dependency vào Maven cache; cache không thuộc source dự án. Sau khi hoàn lại, đã clean/build lại để WAR không chứa lớp triển khai đã gỡ. Không chạy SQL, không tạo DB/schema, không khởi động thêm MySQL hay thử SELECT 1.

Khảo sát bổ sung thấy IntelliJ đăng ký Tomcat ở `D:\cong_cu_nen\apache-tomcat-10.1.48`. Gọi version.bat bằng đường dẫn đầy đủ lúc chưa đặt CATALINA_HOME trả lỗi biến môi trường; đây là kiểm tra phiên bản, không phải deploy. Cần xác minh lại với CATALINA_HOME đúng ở M1.
