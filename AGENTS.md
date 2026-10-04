# Quy ước làm việc — Đồ án TMĐT nhóm 6

## Phạm vi và nguồn yêu cầu

- Dự án website C2C kết nối người mua và người bán. Một USER có thể vừa mua vừa bán; ADMIN quản lý nền tảng.
- Đọc `docs/README.md`, yêu cầu và thiết kế liên quan trước khi sửa. Yêu cầu gốc ở `docs/00-yeu-cau-goc.txt`.
- Không triển khai AI/MLOps, cổng thanh toán thật, hoàn tiền thật hoặc chức năng ngoài đồ án.
- Lượt khảo sát đầu chỉ tạo tài liệu; các lượt tiếp theo triển khai theo `docs/05-ke-hoach-trien-khai.md` và chỉ dẫn mới của người dùng.
- Không chạy SQL làm thay đổi database thật trong lượt thiết kế. Không suy đoán rằng database ngoài repository trống. Các lượt triển khai phải khảo sát schema, sao lưu dữ liệu liên quan và viết migration có phiên bản trước khi thay đổi.

## Công nghệ và cấu trúc

- Java 17, Maven Wrapper, WAR; Java Servlet + MySQL + JDBI; JSP/JSTL, HTML/CSS/JS và Bootstrap cơ bản.
- Giữ gốc package `com.example.demo`. Dùng package chữ thường `controller`, `service`, `dao`, `model`; đây là cách viết Java của các tầng Controller/Service/DAO/Model.
- Chỉ dùng `jakarta.servlet`, không trộn `javax.servlet`. M1 đã đồng bộ Tomcat 10.1.x + Servlet API 6.0.0 + web.xml 6.0, Java 17.
- Controller nhận HTTP, kiểm tra đầu vào, gọi Service, forward JSP/redirect. Service sở hữu nghiệp vụ và transaction. DAO chỉ truy vấn, không chứa luật nghiệp vụ. JSP không gọi DAO và không viết SQL/scriptlet nghiệp vụ.
- Không giữ JDBI Handle trong biến static/Servlet; các DAO trong một transaction cùng attach vào Handle do Service mở. Bind tham số; tên cột/sắp xếp động phải thuộc allowlist.
- Cấu hình DB/storage qua biến môi trường hoặc cấu hình cục bộ không commit; không ghi mật khẩu, dữ liệu cá nhân hay session token vào log/tài liệu. Không hard-code URL DB và credential trong Service.
- Đặt tên lớp Java PascalCase, biến/phương thức camelCase, bảng/cột snake_case. Mã trạng thái tiếng Anh theo thiết kế; nhãn giao diện và tài liệu tiếng Việt. Chú thích ngắn lý do của luật quan trọng.

## Bất biến nghiệp vụ

- Service kiểm tra quyền và ownership từ tài khoản đăng nhập cho mọi đọc/ghi riêng tư; không tin buyer_id/seller_id/role từ request.
- Giá và tổng tiền tính phía server bằng BigDecimal, lưu DECIMAL; cấm tự mua, số lượng âm và đặt hàng vượt tồn.
- Một đơn chỉ có một người bán. Một checkout nhiều người bán tạo nhiều đơn trong cùng transaction; lỗi một phần rollback toàn bộ.
- Tạo/hủy đơn, tồn kho, thanh toán và lịch sử liên quan phải nguyên tử; request lặp không tạo đơn/trừ hoặc hoàn kho lần hai.
- Không xóa cứng user/product/order/snapshot/lịch sử/bằng chứng đã liên quan giao dịch; ẩn, khóa hoặc ngừng hoạt động. Ảnh có snapshot phải giữ bytes bất biến.
- Phân biệt trạng thái đơn, thanh toán, khiếu nại và kiểm duyệt. Không cho ADMIN tùy ý nhảy trạng thái hoặc sửa snapshot.
- Review chỉ từ người mua của đơn COMPLETED, mỗi order_item một lần; nhãn “Đã mua qua hệ thống” do server xác định.
- Khiếu nại gắn đơn, cho phép cả giao dịch chưa hoàn thành; bằng chứng chỉ người có quyền được tải.
- Băm mật khẩu bằng thư viện chuyên dụng; CSRF cho request thay đổi, escape nội dung người dùng, kiểm tra upload và đổi session khi đăng nhập.

## Quy trình và kiểm tra

- Hoàn thành từng mốc xuyên suốt UI → Controller → Service → DAO → database; không chuyển mốc khi tiêu chí còn thiếu mà không báo rõ.
- Ưu tiên giao diện có sẵn; hiện tại chỉ có Hello World nên xây JSP/JSTL đơn giản theo cấu trúc thiết kế.
- Dùng `./mvnw.cmd -B verify` trên Windows (hoặc `./mvnw -B verify` trên Unix). Build thành công không đồng nghĩa test nghiệp vụ hay deploy thành công.
- Test phù hợp với thay đổi; transaction/cạnh tranh tồn kho phải thử trên MySQL/InnoDB thật trong schema test riêng, không dùng dữ liệu thật. Không viết test chỉ lặp lại implementation.
- Không sửa `.idea/workspace.xml` để chia sẻ cấu hình riêng của máy. Không commit target, file upload thật hay secrets. Thư mục này hiện chưa là Git repository; không tự khẳng định đã commit/diff.
- Sau mỗi phần báo file thay đổi, lệnh và thao tác kiểm tra, kết quả thực tế, giới hạn còn lại và giải thích nghiệp vụ để trình bày với giáo viên. Cập nhật tài liệu khi đổi quyết định.
- Các giả định ở `docs/02-yeu-cau-va-quy-tac.md` là đề xuất thiết kế, không mô tả tính năng đã có. Nếu người dùng điều chỉnh, cập nhật đồng bộ schema, quy tắc và kế hoạch.
- M1 đã có 22 bảng nghiệp vụ + Flyway history, không tự migrate/seed khi deploy. V001 đã áp không sửa; thêm migration có phiên bản. schema.sql chỉ cho schema trống; DB có dữ liệu phải khảo sát/backup và mapping trước.
- Seed chỉ là fixture phát triển với password demo công khai theo README; không dùng trên production. PasswordHasher BCrypt 2b/cost 12 đã chuẩn bị ở M1 để seed và M2 dùng chung; không phải chức năng đăng nhập đã triển khai.
- Profile `mysql-it` chỉ dùng schema hậu tố `_test` và `C2C_IT_ALLOWED=true`; cấu hình APP_CONFIG_FILE riêng. File local/credential và `.local-test/` phải giữ ngoài commit. M1 không xác minh credential WAMP của người dùng.
- M2 đã có đăng ký/email login/logout/profile, UserService/UserDao, AccessFilter và CSRF. Session CurrentUser không chứa email/hash/mật khẩu; Filter refresh ACTIVE/role từ DB. Email readonly trong hồ sơ, DTO không nhận ID/role/status. C2C_HTTP_BASE chỉ trỏ Tomcat loopback deploy cùng cấu hình schema test; không chạy kiểm thử ghi trên DB ứng dụng. Báo cáo hiện tại docs/reports/M2-tai-khoan.md; M3 trở đi chưa triển khai.
