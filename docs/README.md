# Hồ sơ thiết kế website C2C — Nhóm 6

Ngày khảo sát: **03/10/2026**, múi giờ Asia/Bangkok. Đề tài: “Thiết kế mô hình nền tảng kết nối người mua – người bán”. Chưa chốt thương hiệu/ngành hàng.

**Trạng thái: M1 và M2 đã triển khai nền tảng, schema/JDBI, tài khoản và phân quyền; M3 trở đi chưa triển khai.** M2 đạt 20 unit + 24 integration/HTTP tests trên MySQL 9.1.0 riêng và Tomcat 10.1.48. Báo cáo hiện tại ở [M2](reports/M2-tai-khoan.md); M1/01/06 lưu kết quả các lượt trước.

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

Các quyết định chính để đọc trước:

1. USER vừa mua vừa bán; quyền bán dựa trên chủ sở hữu, không tạo hai loại tài khoản loại trừ nhau.
2. Mỗi người bán có một đơn riêng; checkout nhiều người bán thành công hoặc thất bại toàn bộ.
3. Trừ **số lượng khả dụng** khi tạo đơn; hủy trước giao hoàn lại đúng một lần. Chưa có hết hạn giữ hàng tự động.
4. Lưu tên, giá, mô tả, tình trạng và toàn bộ ảnh tại lúc đặt; file ảnh cũ không bị ghi đè/xóa.
5. Chỉ người mua xác nhận nhận hàng để hoàn thành; review sau hoàn thành, khiếu nại có thể mở trước đó.
6. Đề xuất giữ Tomcat 10.1.x và đồng bộ Servlet API về 6.0.0 ở M1. MySQL mục tiêu 8.4 LTS; máy hiện có binary MySQL 9.1.0 trong WAMP, có thể dùng schema riêng nếu kiểm tra tương thích đạt.

Quy ước cộng tác nằm trong [AGENTS.md](../AGENTS.md). Cách cấu hình SQL/build/deploy nằm trong [README](../README.md) và [hướng dẫn database](../database/README.md). Bước tiếp theo là M3 khi được người dùng yêu cầu.

Lượt khảo sát trước chỉ thiết kế; lượt hiện tại người dùng đã yêu cầu triển khai M1 thực tế. AppConfig hiện đọc biến môi trường/file cấu hình cục bộ ngoài WAR. Chưa có schema/credential `c2c_demo` trên WAMP của người dùng; instance thử có datadir riêng và không thay đổi dữ liệu WAMP.
