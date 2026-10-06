# Báo cáo M7–M8 — Đánh giá theo đơn và khiếu nại

Ngày thực hiện: 04/10/2026. Đã hoàn thành luồng JSP → Servlet → Service → DAO → JDBI → MySQL trong phạm vi yêu cầu. Không triển khai chức năng nghiệp vụ ngoài M7–M8.

## Chức năng đã triển khai

- Buyer đánh giá từng dòng đơn COMPLETED bằng số sao 1–5 và nhận xét; Service kiểm tra tài khoản ACTIVE, ownership, trạng thái và quan hệ dòng đơn. UNIQUE của database cùng khóa transaction chống gửi trùng. Chi tiết đơn hiển thị đánh giá đã gửi.
- Chi tiết sản phẩm công khai hiển thị trung bình, tổng lượt, tên người đánh giá, nhận xét, thời gian và nhãn “Đã mua qua hệ thống”; chỉ lấy review VISIBLE thuộc đơn COMPLETED. Nội dung được escape.
- Buyer gửi khiếu nại cho đơn của mình ở mọi trạng thái, chọn một trong bốn lý do, nhập mô tả và ảnh tùy chọn; theo dõi danh sách, phản hồi, kết luận và bổ sung thông tin/ảnh khi hồ sơ còn mở.
- Admin lọc danh sách, xem buyer/seller, snapshot và ảnh tại lúc mua, trạng thái/thanh toán/lịch sử đơn, bằng chứng và timeline hồ sơ. Có chuyển sang xử lý, phản hồi, kết thúc với kết quả bắt buộc và mở lại có lý do; lưu actor/thời điểm/lịch sử.
- Bằng chứng dùng ImageStorage và endpoint riêng, chỉ buyer của hồ sơ hoặc Admin được tải. Seller/người dùng khác nhận 404, khách chưa đăng nhập nhận 401; không đưa ảnh vào catalog.
- RECEIVED/PROCESSING chặn buyer xác nhận hoàn thành đơn DELIVERED; UI giải thích rõ. RESOLVED cho phép buyer tự xác nhận nếu các điều kiện khác hợp lệ. Đóng/mở lại khiếu nại không tự sửa trạng thái đơn, thanh toán hoặc tồn kho; đơn COMPLETED không bị đảo trạng thái.
- Tận dụng session, phân quyền, CSRF, validation ảnh và layout hiện có; thêm liên kết trong hồ sơ, điều hướng buyer và trang Admin.

## Khảo sát database và quyết định

Đã đọc thiết kế, V001 và khảo sát **chỉ đọc** `SHOW CREATE TABLE` trên MySQL ứng dụng cho sáu bảng `reviews`, `complaints`, `complaint_evidence`, `complaint_messages`, `complaint_status_history`, `media_assets`. Cấu trúc thực tế đáp ứng M7–M8; **không có migration mới**, không sửa V001/schema/seed, không ghi dữ liệu vào database ứng dụng và không thay cấu hình cục bộ của người dùng.

1. Schema bắt buộc cả `product_rating` và `seller_rating`. Form theo yêu cầu có một số sao chung: lưu cùng giá trị vào hai cột, thống kê sản phẩm bằng `product_rating`. Chưa cung cấp điểm seller riêng trên UI.
2. Thiết kế hiện có UNIQUE `complaints.order_id`: một hồ sơ cho mỗi đơn trong toàn bộ vòng đời, kể cả đã đóng. Gửi lại dẫn về hồ sơ cũ; buyer bổ sung lúc mở, Admin mở lại để xử lý tiếp. Giữ quy tắc đã thống nhất thay vì tạo hồ sơ thứ hai.
3. Form cung cấp NOT_AS_DESCRIBED, NOT_RECEIVED, DAMAGED, OTHER; tự tạo tiêu đề từ lý do. Nội dung tạo hồ sơ 10–5.000 ký tự; bổ sung/phản hồi/kết luận 1–5.000. Tối đa 5 ảnh/lần, 20 ảnh/hồ sơ; áp dụng giới hạn ảnh hiện có.
4. Kết quả ORDER_CANCELLED chỉ ghi nhận khi đơn đã CANCELLED; thao tác xử lý khiếu nại không tự hủy đơn/hoàn kho. Lịch sử kết luận cũ được giữ khi mở lại; các cột kết luận hiện tại được xóa đúng CHECK của schema.
5. Service khóa actor rồi order rồi complaint, dùng cùng Handle trong transaction. Điều kiện hoàn thành và việc mở hồ sơ cùng khóa order để không bỏ sót khi thao tác đồng thời. DAO bind tham số; quyền không lấy từ request.

## File tạo hoặc sửa

Đường dẫn dưới đây tính từ gốc repository.

| Nhóm | File |
| --- | --- |
| Java mới | `src/main/java/com/example/demo/dao/ReviewDao.java`, `dao/ComplaintDao.java`, `service/ReviewService.java`, `service/ComplaintService.java`, `model/ComplaintResult.java`, `controller/buyer/ReviewServlet.java`, `controller/buyer/ComplaintServlet.java` (các đường dẫn rút gọn cùng gốc package) |
| Java tích hợp | `config/ShopServices.java`, `dao/MediaDao.java`, `dao/AuditDao.java`, `service/ProductService.java`, `service/OrderService.java`, `controller/media/MediaServlet.java` |
| View/fragment mới | `src/main/webapp/WEB-INF/views/reviews/form.jsp`, `complaints/form.jsp`, `complaints/list.jsp`, `complaints/detail.jsp`, `layouts/feedback-tags.jspf`, `orders/snapshot-items.jspf`, `catalog/reviews.jspf` (đường dẫn rút gọn cùng gốc views) |
| View/CSS tích hợp | `WEB-INF/views/orders/detail.jsp`, `catalog/detail.jsp`, `layouts/shop-start.jspf`, `layouts/navigation.jspf`, `admin/index.jsp`, `auth/profile.jsp`, `assets/css/startup.css` (cùng gốc webapp) |
| Test | Tạo `src/test/java/com/example/demo/service/ReputationIT.java`, `controller/FeedbackHttpIT.java`; sửa `controller/ShopHttpIT.java` để tái sử dụng helper multipart theo đường dẫn |
| Tài liệu mới | Báo cáo này; `docs/10-demo-danh-gia-khieu-nai.md` |
| Tài liệu cập nhật | `AGENTS.md`, `README.md`, `docs/README.md`, `docs/02-yeu-cau-va-quy-tac.md`, `docs/03-kien-truc.md`, `docs/04-thiet-ke-database.md`, `docs/05-ke-hoach-trien-khai.md` |

Không đổi dependency, Maven Wrapper, pom.xml hoặc SQL. Các file chạy thử, cấu hình riêng, ảnh, backup và log nằm trong `.local-test/` đã bị ignore.

## Kiểm tra đã thực sự chạy

Môi trường: Java 17, Maven Wrapper 3.9.6, MySQL **9.1.0**, Tomcat **10.1.48**. Không khẳng định đã kiểm thử MySQL 8.4.

- Khảo sát DDL bằng mysql CLI trên WAMP loopback 3306/schema ứng dụng, chỉ đọc.
- Chạy MySQL riêng ở loopback 13316; kiểm tra schema `c2c_feedback_test` chưa tồn tại rồi tạo mới utf8mb4. Không DROP/reset database. Cấu hình APP_CONFIG_FILE riêng; không dùng database ứng dụng cho test ghi.
- Chạy ` .\mvnw.cmd -B compile exec:java '-Dexec.args=migrate' ` và ` .\mvnw.cmd -B exec:java '-Dexec.args=seed' ` trên schema test: thành công, V001 được áp dụng. Backup bằng mysqldump `--single-transaction` trước kiểm thử; file `.local-test/feedback-before-tests.sql`.
- Chạy ` .\mvnw.cmd -B verify `: **22 unit test đạt**, tạo WAR.
- Deploy WAR trên Tomcat riêng loopback 18080, context `/c2c`, cấu hình cùng schema test. Đặt `C2C_IT_ALLOWED=true`, `C2C_HTTP_BASE=http://127.0.0.1:18080/c2c` và APP_CONFIG_FILE test.
- Chạy ` .\mvnw.cmd -B verify -Pmysql-it ` trước khi tạo fixture mua bán: **24 integration test M1–M2 đạt** (DatabaseIT 11, UserServiceIT 7, AccountHttpIT 6).
- Chạy ` .\mvnw.cmd -B verify -Pmysql-it '-Dit.test=CommerceIT,ShopHttpIT,ReputationIT,FeedbackHttpIT' `: **8 integration test đạt**, cùng 22 unit test.
- Sau thay đổi cuối, build/redeploy rồi chạy ` .\mvnw.cmd -B verify -Pmysql-it '-Dit.test=AccountHttpIT,CommerceIT,ShopHttpIT,ReputationIT,FeedbackHttpIT' `: **14 integration test đạt**, cùng 22 unit test; BUILD SUCCESS lúc 13:45:30 ngày 04/10/2026.

Tổng cộng **54 test case khác nhau đã đạt** trong lượt này: 22 unit + 24 M1–M2 + 4 mua bán + 4 M7–M8; không đếm lần chạy lại thành test mới. Báo cáo XML không có failure/error/skipped. WAR cuối `target/demo-1.0-SNAPSHOT.war`, 12.134.793 bytes; Maven biên dịch 66 file Java chính.

ReputationIT kiểm quyền, trạng thái, rating sai, dòng đơn sai, gửi trùng, thống kê; khiếu nại/ảnh/bổ sung, chuyển trạng thái, kết luận/mở lại/lịch sử, chặn hoàn thành và không tự đổi payment/order. Có một case hai thao tác tạo khiếu nại/xác nhận hoàn thành đồng thời trên MySQL/InnoDB.

FeedbackHttpIT kiểm HTTP/session/CSRF/JSP: form lỗi, review hiển thị và escape, nhãn xác thực, multipart ảnh không hợp lệ/hợp lệ, trùng dẫn hồ sơ cũ, quyền ảnh, snapshot không đổi khi seller sửa sản phẩm, Admin xử lý, buyer theo dõi, chặn/cho phép hoàn thành và mở lại sau COMPLETED. AccountHttpIT, CommerceIT, ShopHttpIT được chạy lại để kiểm tích hợp.

**Không thực hiện kiểm thử bằng trình duyệt GUI.** Các kiểm tra UI ở đây là HTTP và nội dung HTML do Tomcat/JSP trả về; chưa đánh giá bố cục trực quan trên các kích thước màn hình. Case đồng thời không chứng minh mọi cách xen kẽ hoặc kiểm thử tải. Không mở rộng hạ tầng test.

## Chạy và demo trên máy người dùng

1. Giữ cấu hình MySQL/upload hiện tại, chọn file bằng APP_CONFIG_FILE hoặc `-Dc2c.config` theo README; tài khoản database phải có quyền đọc/ghi các bảng đã có. Không gửi/in mật khẩu.
2. Chạy `.\mvnw.cmd -B verify` tại gốc dự án.
3. Redeploy `target/demo-1.0-SNAPSHOT.war` bằng cấu hình Tomcat 10.1 hiện có, restart/reload ứng dụng để nạp các lớp mới. Phiên Tomcat ứng dụng đang chạy không được tự redeploy trong lượt này.
4. **Không cần chạy migrate hoặc seed cho bản cập nhật M7–M8** nếu đã có schema M1–M6. Không reset database có dữ liệu.
5. Theo [kịch bản demo 2–3 phút](../10-demo-danh-gia-khieu-nai.md): dùng một đơn COMPLETED để review và một đơn DELIVERED để minh họa khiếu nại chặn hoàn thành. Lấy ID qua link UI; URL có prefix context của Tomcat.

Các trang chính: `/buyer/reviews/new`, `/buyer/complaints`, `/admin/complaints`, `/admin/complaints/detail`; ảnh `/media/complaint-evidence` cần session/quyền phù hợp. Thông tin fixture và hướng dẫn môi trường nằm trong README.

## Giới hạn và giải thích khi trình bày

Chưa có sửa/xóa/ẩn/phản hồi review, điểm seller riêng, hồ sơ thứ hai cho cùng đơn hoặc thanh toán/hoàn tiền thật. Form chưa chọn riêng dòng đơn hoặc lý do PAYMENT. Danh sách review hiển thị tối đa 100 gần nhất (thống kê tính toàn bộ); danh sách khiếu nại tối đa 200 gần nhất. Cơ chế lưu ảnh dùng filesystem hiện có; không bổ sung cơ chế khôi phục file mồ côi nếu tiến trình bị dừng đột ngột.

Đánh giá tăng độ tin cậy vì gắn với giao dịch đã hoàn thành và đúng người mua. Khiếu nại giúp đối chiếu snapshot tại thời điểm mua với bằng chứng riêng tư và lịch sử xử lý có người chịu trách nhiệm. Servlet nhận HTTP, Service kiểm quyền/nghiệp vụ và điều phối transaction, DAO truy vấn bind qua JDBI; MySQL giữ ràng buộc và lịch sử. JSP chỉ trình bày dữ liệu đã kiểm quyền.

Thông điệp commit đề xuất: `Hoàn thiện M7–M8: đánh giá theo đơn và xử lý khiếu nại có minh chứng`.

Thư mục chưa là Git repository; chưa tạo commit. MySQL/Tomcat kiểm thử riêng được dừng sau kiểm tra, giữ schema/backup/báo cáo trong môi trường test để đối chiếu; WAMP và Tomcat ứng dụng hiện có không bị dừng.
