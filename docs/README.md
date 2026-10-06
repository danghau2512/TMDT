# Hồ sơ thiết kế website C2C — Nhóm 6

**Giỏ hàng AJAX (05/10/2026):** [nút −/+, đồng bộ tổng tiền và kiểm tra thực tế](reports/Gio-hang-AJAX.md). Tái sử dụng CartService/CartDao và CSRF; không thay schema hoặc checkout.

**Ảnh sản phẩm trang chủ (05/10/2026):** [khung vuông/contain và responsive 3–2–1 cột](reports/Anh-san-pham-trang-chu.md), chỉ styling home, không thay ảnh upload/database.

**Gợi ý tìm kiếm (05/10/2026):** [triển khai, kiểm tra và demo](reports/Tim-kiem-goi-y.md). AJAX ngay dưới ô header, tối đa 8 sản phẩm public có ảnh/tên/giá, click vào chi tiết; không migration.

**Nhận diện TraoTay và tiếng Việt (05/10/2026):** [báo cáo, ảnh và cách redeploy](reports/TraoTay-tieng-Viet.md). Dùng logo người dùng `assets/images/Logo.png`, khẩu hiệu “Đồ cũ, giá trị mới.”; chuẩn hóa typography, encoding/cache asset và thông báo lỗi mạng chat. Không migration/database rewrite.

**Bốn nâng cấp mới (05/10/2026):** [demo khiếu nại/ảnh đánh giá/khai báo/so sánh](14-demo-bon-nang-cap.md), [báo cáo và ảnh](reports/Bon-nang-cap-trai-nghiem.md). V004 thêm review_images và các cột nullable/flag snapshot; hiện 26 bảng nghiệp vụ, 27 nếu có Flyway history. c2c_demo standalone đã backup + nâng V004, không chạy lại migration/seed.


**Chat trước khi mua (05/10/2026):** [hướng dẫn hai tài khoản](13-demo-chat-mua-ban.md), [báo cáo](reports/Chat-mua-ban.md). V003 thêm hai bảng, hiện 25 bảng nghiệp vụ (26 nếu có Flyway history). Không dùng bảng phản hồi khiếu nại để chat; dữ liệu trước đó được giữ nguyên.

**Cập nhật 05/10/2026:** thêm [VNPAY Sandbox](12-vnpay-sandbox.md) và [báo cáo thực tế](reports/VNPAY-sandbox.md), migration V002 và bảng thứ 23 `vnpay_attempts`; checkout ngừng tạo chuyển khoản mô phỏng mới. Các báo cáo trước giữ kết quả lịch sử tại thời điểm thực hiện.

Khắc phục lỗi HTTPS trên Windows: [báo cáo truststore và kiểm tra chỉ đọc](reports/VNPAY-khac-phuc-HTTPS.md). Cấu hình mới `vnpay.tls.useWindowsRoot` áp dụng riêng cho client VNPAY, cần rebuild/restart Tomcat.


Ngày khảo sát: **03/10/2026**, múi giờ Asia/Bangkok. Đề tài: “Thiết kế mô hình nền tảng kết nối người mua – người bán”. Chưa chốt thương hiệu/ngành hàng.

**Trạng thái 04/10/2026: M1–M8 đã có mua bán, đánh giá theo đơn và khiếu nại.** Tận dụng 22 bảng hiện có, không cần migration mới. Xem [báo cáo M7–M8](reports/M7-M8-danh-gia-khieu-nai.md), [demo thêm 2–3 phút](10-demo-danh-gia-khieu-nai.md). Các báo cáo M1/M2/M3–M6 và khảo sát lưu kết quả lịch sử.

| Tài liệu | Nội dung |
| --- | --- |
| [Yêu cầu gốc](00-yeu-cau-goc.txt) | Bản sao nguyên văn đặc tả người dùng |
| [01 — Khảo sát](01-khao-sat-repository.md) | Code, cấu hình, môi trường, lệch phiên bản và giới hạn xác minh |
| [02 — Yêu cầu và quy tắc](02-yeu-cau-va-quy-tac.md) | Vai trò, phạm vi, giả định, trạng thái và quyền thao tác |
| [03 — Kiến trúc](03-kien-truc.md) | Các tầng, cấu trúc dự án, HTTP, bảo mật và cấu hình |
| [04 — Database](04-thiet-ke-database.md) | ERD, 22 bảng, PK/FK, constraint, snapshot và transaction |
| [05 — Kế hoạch](05-ke-hoach-trien-khai.md) | 9 mốc triển khai và tiêu chí nghiệm thu |
| [06 — Kiểm tra lượt đầu](06-kiem-tra-luot-dau.md) | Kiểm tra đã chạy, kết quả và điều kiện tiếp theo |
| [07 — Chuẩn bị môi trường](07-chuan-bi-moi-truong.md) | Thông tin MySQL/schema/credential/Tomcat cần bổ sung và cấu hình mẫu |
| [Báo cáo M1](reports/M1-nen-tang.md) | File thay đổi, kiểm thử thực tế, cách chạy và giới hạn |
| [Khắc phục cấu hình M1](reports/M1-khac-phuc-cau-hinh.md) | Lựa chọn file local, lỗi migrate, 13 unit tests và bước cần điền credential |
| [08 — Tài khoản và phân quyền](08-tai-khoan-va-phan-quyen.md) | Chạy M2, tuyến Servlet/JSP/Filter, quy tắc và checklist kiểm tra |
| [Báo cáo M2](reports/M2-tai-khoan.md) | File thay đổi, 20 unit + 24 integration/HTTP tests và giới hạn |
| [Khắc phục 503 Tomcat](reports/M2-khac-phuc-tomcat-config.md) | Thiếu cấu hình DB trong IntelliJ, cách đặt VM options và 22 unit tests |
| [09 — Demo mua bán](09-demo-mua-ban.md) | Đường dẫn, tài khoản, chuẩn bị và kịch bản 5–7 phút |
| [Báo cáo M3–M6](reports/M3-M6-mua-ban.md) | Source/UI đã triển khai, kiểm tra thực tế và giới hạn |
| [10 — Demo đánh giá/khiếu nại](10-demo-danh-gia-khieu-nai.md) | Routes, quy tắc và kịch bản thêm 2–3 phút |
| [Báo cáo M7–M8](reports/M7-M8-danh-gia-khieu-nai.md) | File thay đổi, khảo sát schema thực tế, kiểm tra và giới hạn |

Các quyết định chính để đọc trước:

Giao diện marketplace đã được nâng cấp cho desktop/mobile: [báo cáo và ảnh thực tế](reports/UI-hoan-thien-giao-dien.md), [checklist build/redeploy/trình chiếu](11-checklist-giao-dien.md). Giữ nghiệp vụ M1–M8, không có migration mới; đây là phần giao diện/demo của M9.

1. USER vừa mua vừa bán; quyền bán dựa trên chủ sở hữu, không tạo hai loại tài khoản loại trừ nhau.
2. Mỗi người bán có một đơn riêng; checkout nhiều người bán thành công hoặc thất bại toàn bộ.
3. Trừ **số lượng khả dụng** khi tạo đơn; hủy trước giao hoàn lại đúng một lần. Chưa có hết hạn giữ hàng tự động.
4. Lưu tên, giá, mô tả, tình trạng và toàn bộ ảnh tại lúc đặt; file ảnh cũ không bị ghi đè/xóa.
5. Chỉ người mua xác nhận nhận hàng để hoàn thành; review sau hoàn thành, khiếu nại có thể mở trước đó.
6. Đề xuất giữ Tomcat 10.1.x và đồng bộ Servlet API về 6.0.0 ở M1. MySQL mục tiêu 8.4 LTS; máy hiện có binary MySQL 9.1.0 trong WAMP, có thể dùng schema riêng nếu kiểm tra tương thích đạt.

Quy ước cộng tác nằm trong [AGENTS.md](../AGENTS.md). Cách cấu hình SQL/build/deploy nằm trong [README](../README.md) và [hướng dẫn database](../database/README.md). Tích hợp M9 hoặc mở rộng khác chỉ thực hiện khi người dùng yêu cầu.

Các lượt khảo sát và M1/M2 lưu trạng thái tại thời điểm thực hiện. Lượt mua bán giữ nguyên cấu hình cục bộ, schema và dữ liệu WAMP của người dùng; kiểm tra ghi chỉ chạy trên instance MySQL riêng và schema `c2c_shop_test`, đã sao lưu trước kiểm tra. AppConfig đọc file ngoài WAR; Maven và Tomcat cần được cấu hình riêng.
