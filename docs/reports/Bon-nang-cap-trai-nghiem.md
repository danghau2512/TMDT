# Bốn nâng cấp trải nghiệm C2C — 05/10/2026

Đã mở rộng chức năng hiện có xuyên suốt JSP/Servlet → Service → DAO/JDBI → MySQL, không thêm framework/dependency. COD, VNPAY Sandbox và chat giữ luồng xử lý cũ. [Chạy và demo 5–7 phút](../14-demo-bon-nang-cap.md).

## File chính

| Nhóm | File tạo/sửa trong lượt này |
| --- | --- |
| Database | `src/main/resources/db/migration/V004__reviews_condition_comparison.sql` — thêm review_images, nullable khai báo ở products/order_items, flag ảnh khuyết điểm, mở CHECK purpose REVIEW_IMAGE. Giữ nguyên V001/V002/V003/schema.sql/seed.sql |
| Khiếu nại | ComplaintDao, ComplaintService, controller/buyer/ComplaintServlet; views/complaints/list.jsp, detail.jsp — thống kê theo quyền, tìm mã đơn bind, snapshot đại diện, progress, tên actor/history, kết luận nổi bật |
| Đánh giá | ReviewDao, ReviewService, controller/buyer/ReviewServlet, controller/media/MediaServlet; views/reviews/form.jsp, catalog/reviews.jspf — multipart/ảnh có quyền public, số liệu thật, lọc sao/ảnh, 8 review/trang |
| Khai báo/snapshot | dto/ConditionForm.java (mới), dto/ProductForm.java, service/ConditionDeclaration.java (mới), ProductService, CatalogDao, OrderDao, controller/catalog/ProductServlet; views/seller/condition-form.jspf (mới), product-form.jsp, catalog/condition.jspf (mới), detail.jsp, orders/snapshot-items.jspf |
| So sánh | controller/catalog/CompareServlet.java (mới), ProductService.compare; views/catalog/compare.jsp (mới), cards.jspf, detail.jsp — public DTO allowlist, sản phẩm bị ẩn không lộ nội dung, cùng danh mục, min2/max3, cart/chat POST CSRF |
| Assets/shell | assets/css/upgrades.css và assets/js/upgrades.js (mới); marketplace.js tránh preview trùng; layouts/shop-start.jspf/shop-tags.jspf dùng assets và nhãn chung; AccessFilter thông báo đúng tối đa 3 ảnh với review |
| Kiểm tra | service/UpgradesIT.java (3 focused checks, mới); config/DatabaseIT.java cập nhật danh sách bảng theo migrations thay hard-code 23. Không mở rộng bộ kiểm thử lớn |
| Tài liệu | README, database/README, AGENTS, docs/README/02/03/04/05 và hướng dẫn/báo cáo này; screenshots/upgrades là dữ liệu fixture test |

## Database thực tế

- Đã khảo sát `SHOW CREATE TABLE` sáu bảng liên quan trên c2c_demo/MySQL 9.1.0, xác nhận standalone V003 không Flyway history. Sao lưu toàn bộ bằng mysqldump single-transaction vào file ignored `.local-test/c2c-demo-before-upgrades-20261005.sql` (65.243 bytes).
- Sau rehearsal trên schema test riêng mới, áp V004 **một lần** trên c2c_demo. Kiểm SHA-256 dữ liệu của từng cột cũ, từng bảng và số dòng trước/sau: **25 bảng cũ giữ nguyên**, review_images mới trống. Hiện **26 bảng nghiệp vụ**, hoặc 27 với Flyway history trên schema do Flyway quản lý.
- Không seed, tạo tài khoản/tin/đơn/review/complaint demo trên DB ứng dụng. Không sửa cấu hình local, credential hoặc storage người dùng. Backup/column fingerprints/SHOW CREATE và helper chạy nằm trong `.local-test/`, không commit.

## Kiểm tra thực sự đã chạy

| Kiểm tra/lệnh | Kết quả |
| --- | --- |
| `./mvnw.cmd -B verify` | BUILD SUCCESS, 28 unit tests, WAR được tạo |
| V001–V004 bằng SchemaManager trên c2c_upgrades_test, MySQL/InnoDB instance loopback 13316 | 4 migrations; chạy migrate lại = 0; JDBI SELECT 1 = 1. Seed fixture chỉ schema test, có backup trước fixture |
| `./mvnw.cmd -B verify -Pmysql-it '-Dit.test=UpgradesIT'` với C2C_IT_ALLOWED=true/file test | 3 tests PASS: snapshot text/flag ảnh bất biến sau sửa tin, legacy NULL; review COMPLETED/buyer/once + ảnh/summary/filter/privacy; complaint summary/search/actor/ownership + compare public/cùng danh mục |
| `./mvnw.cmd -B verify -Pmysql-it '-Dit.test=FeedbackHttpIT'`, C2C_HTTP_BASE=loopback18080/c2c | 1 HTTP journey PASS, không skip. Guard chứng minh Tomcat dùng đúng DB test trước các POST; review/khiếu nại/upload byte lỗi/private evidence/CSRF/trạng thái cũ giữ hoạt động |
| Chrome headless thật, ba browser contexts buyer/seller/Admin trên Tomcat 10.1.48 test | 7 nhóm kiểm tra giao diện chính PASS, 0 JavaScript page errors: complaint→process/resolve/supplement; admin search/stats; multipart review/preview remove/star-photo filter/modal; seller create/edit/defect mark/approve/detail; legacy pages; compare2/3/remove/cart/chat; responsive390 |
| Chrome kiểm tra cuối sau chỉnh giao diện và điểm seller | 4 nhóm PASS, 0 JS errors: DTO không chứa trường riêng/điểm thật/max3/POST thiếu CSRF bị 403; bỏ tin ẩn kể cả bảng chưa hợp lệ; thời gian Việt Nam/thanh chọn không che trang riêng; screenshot cuối gồm seller form 390/1366 |
| `./mvnw.cmd -B compile exec:java '-Dexec.args=check'` với file local ứng dụng | BUILD SUCCESS, JDBI SELECT 1 = 1 trên DB ứng dụng; chỉ đọc, không tạo dữ liệu demo |
| Chụp và xem PNG 1366/390 | Đã kiểm hình thức complaint center/detail, review/detail sản phẩm, seller form, comparison. Không page overflow ở bốn màn hình mobile; bảng compare cuộn trong container riêng |

Kết quả build không thay thế kiểm nghiệp vụ; các hàng ở trên phân biệt rõ. Fixture có ảnh minh họa thử ngoài storage ứng dụng. Ảnh thực tế: [so sánh desktop](../screenshots/upgrades/compare-1366.png), [mobile](../screenshots/upgrades/compare-390.png), [khiếu nại](../screenshots/upgrades/complaint-center-1366.png), [review](../screenshots/upgrades/product-reviews-1366.png), [seller](../screenshots/upgrades/seller-form-1366.png).

## Giải thích khi thuyết trình

Servlet nhận form/bộ lọc và lấy CurrentUser từ session, không nhận quyền từ client. Service kiểm ACTIVE, ownership, trạng thái và điều phối một transaction; DAO bind giá trị trên Handle của transaction; JDBI lấy connection từ pool, MySQL lưu dữ liệu. JSP chỉ nhận DTO/map được phép và escape nội dung. Assets JS hỗ trợ preview/modal/chọn so sánh, không quyết quyền hoặc giá.

Review được xác thực từ quan hệ item/đơn/buyer và COMPLETED; không phải nhãn người dùng tự chọn. Complaint là hồ sơ hỗ trợ riêng tư, đóng hồ sơ không tự hoàn tất/hủy/refund đơn. Snapshot lưu khai báo/ảnh tại lúc đặt để seller sửa tin không thay thông tin đã mua. So sánh dùng giá/khai báo hiện tại công khai, giúp chọn hàng chứ không bảo chứng chất lượng.

## Giới hạn và bước trên máy người dùng

Rebuild artifact và Stop/Run Tomcat IDE để nhận phiên bản mới; kiểm thử deploy thực tế ở Tomcat riêng 18080, không tự dừng/thay deploy IDE 8080 đang chạy của người dùng. Không thử thanh toán VNPAY mới hoặc hoàn tiền trong lượt này. Chưa kiểm tải/exhaustive/hết mọi trình duyệt; chưa có thang điểm seller độc lập, sửa/xóa review hoặc quản trị review. Chọn lại file ảnh sau lỗi form; lịch sử/timestamp gốc UTC giữ trong DB, giao diện mới JS định dạng Việt Nam UTC+7. Không tự backfill khai báo cho tin/đơn cũ.

Commit gợi ý: `Bổ sung trung tâm khiếu nại, đánh giá có ảnh, khai báo hàng cũ và so sánh sản phẩm`.
