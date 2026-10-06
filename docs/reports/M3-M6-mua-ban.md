# Báo cáo M3–M6 — Luồng mua bán C2C

Ngày thực hiện: **04/10/2026**. Phạm vi theo yêu cầu lượt này: triển khai xuyên suốt danh mục, đăng bán/kiểm duyệt, giỏ/checkout, thanh toán mô phỏng và đơn hàng. Giữ Java 17/Jakarta Servlet 6/Tomcat 10.1, JSP/JSTL, MySQL, JDBI/HikariCP, các chức năng M2, cấu hình cục bộ và dữ liệu hiện có.

## Kết quả chức năng

| Phần | Đã triển khai |
| --- | --- |
| Catalog | Home mua bán, danh mục, keyword/khoảng giá/condition, 12 tin/trang, thẻ/chi tiết/contact công khai, ảnh mặc định, trạng thái rỗng/hết kho. Chỉ PUBLIC + APPROVED của seller/category ACTIVE. |
| Seller | Tạo/sửa/ẩn tin, kho và ledger, version chống form cũ; 0–5 ảnh JPEG/PNG, bytes được giải mã/encode lại, lưu ngoài WAR. Sửa nội dung/giá/ảnh trả PENDING. |
| Admin | Tin toàn nền tảng, tạo thay seller ACTIVE được chọn rõ, sửa/ẩn/approve/reject có reason và audit; lọc tin chờ duyệt. |
| Cart | Thêm/cập nhật/xóa, giá và tổng hiện tại từ DB, chỉ giỏ của mình, cấm tự mua/không công khai/hết kho. |
| Checkout | Người nhận/điện thoại/địa chỉ/ghi chú; COD hoặc BANK_TRANSFER_SIMULATED; tính BigDecimal phía server, nhóm một order/seller; batch/key/hash chống gửi lặp; snapshot title/description/condition/category/price/images và contact; giữ kho, history/payment, xóa giỏ cùng transaction. |
| Payment | PAY chuyển khoản mô phỏng cho người tham gia hợp lệ, bấm lặp không ghi thêm; COD PAID cùng DELIVERED; hủy UNPAID/PENDING_CONFIRMATION → VOIDED, hủy PAID → REFUND_SIMULATED. |
| Orders | Buyer/seller/admin list/detail/lọc status, snapshot/nhận hàng/tổng/payment/timelines; Service kiểm ownership/transition. Seller confirm/ship/deliver; buyer nhận để complete; hủy hợp lệ hoàn kho đúng một lần. Admin thao tác có lý do/audit và không tự COMPLETE thay buyer. |

Filter M2 tiếp tục refresh ACTIVE/role, phân quyền route và CSRF; Service lấy lại actor từ DB, không tin role/seller/buyer từ browser. Snapshot ảnh cũ được giữ và đọc theo quyền đơn, không theo quyền của tin hiện tại. Giá/phí/tổng/status từ client không làm nguồn nghiệp vụ. Không mở chức năng review/khiếu nại.

## File tạo và sửa

Đường dẫn trong bảng tính từ root dự án `C:\Users\hausi\OneDrive\Desktop\TMDT\project\web\demo`; package gốc Java là `src/main/java/com/example/demo/`.

| Nhóm | File | Vai trò |
| --- | --- | --- |
| Java cấu hình sửa | config/AppConfig.java; listener/ApplicationListener.java | UPLOAD_ROOT/upload.root, khởi tạo ShopServices từ Database |
| Java cấu hình tạo | config/ShopServices.java | Các Service/storage dùng chung, không chia sẻ Handle |
| Java Controller sửa | controller/HomeServlet.java; filter/AccessFilter.java | Catalog home, thông báo lỗi mua bán/upload và giữ auth/CSRF |
| Java Controller tạo | controller/ShopWeb.java; controller/catalog/ProductServlet.java; controller/buyer/CartCheckoutServlet.java; controller/orders/OrdersServlet.java; controller/media/MediaServlet.java | Parse/validate form, GET/JSP, POST/redirect, multipart, ảnh theo quyền |
| Service tạo | service/ProductService.java; service/CartService.java; service/OrderService.java; service/ShopRules.java | Luật quyền, kho, moderation, snapshot, tiền, transaction, lỗi an toàn |
| DAO tạo | dao/CatalogDao.java; dao/CartDao.java; dao/OrderDao.java; dao/MediaDao.java; dao/AuditDao.java | Bind SQL trên Handle của Service, giữ snapshot/history/audit |
| DTO/Model/Exception tạo | dto/ProductForm.java; dto/CatalogFilter.java; dto/CheckoutForm.java; model/CartSummary.java; model/StoredImage.java; exception/ShopException.java | Dữ liệu form/view/media và lỗi HTTP có thông báo an toàn |
| Storage tạo | storage/ImageStorage.java | Kiểm ảnh thật, kích thước/dung lượng, UUID file và retention ngoài WAR |
| Views tạo | src/main/webapp/WEB-INF/views/layouts/shop-start.jspf, shop-tags.jspf, shop-end.jspf; catalog/list.jsp, detail.jsp, cards.jspf; seller/products.jsp, product-form.jsp; buyer/cart.jsp, checkout.jsp, receipt.jsp; orders/list.jsp, detail.jsp | Giao diện Việt thống nhất, escape đầu vào, form CSRF, empty/error/success, timelines |
| Views/assets sửa | src/main/webapp/WEB-INF/views/home.jsp; layouts/navigation.jspf; admin/index.jsp; src/main/webapp/assets/css/startup.css | Home mua bán, điều hướng, admin dashboard, CSS responsive |
| Asset tạo | src/main/webapp/assets/images/product-placeholder.svg | Ảnh mặc định; không phải định dạng upload được chấp nhận |
| Cấu hình/build sửa | config/application.example.properties; pom.xml | Hướng dẫn storage ngoài WAR; mysql-it mặc định giữ M1/M2, thương mại chạy riêng bằng -Dit.test |
| Test tạo | src/test/java/com/example/demo/service/CommerceIT.java; controller/ShopHttpIT.java; controller/HttpTestTarget.java | 3 kịch bản MySQL, 1 hành trình HTTP, xác minh Tomcat cùng DB test trước HTTP ghi |
| Test sửa | src/test/java/com/example/demo/controller/AccountHttpIT.java | Thêm kiểm tra cùng DB test trước chạy bộ HTTP M2 |
| Docs sửa | README.md; AGENTS.md; docs/README.md; docs/02-yeu-cau-va-quy-tac.md; docs/03-kien-truc.md; docs/04-thiet-ke-database.md; docs/05-ke-hoach-trien-khai.md | Trạng thái, quyết định, kiến trúc, cấu hình/deploy/test và tiến độ |
| Docs tạo | docs/09-demo-mua-ban.md; docs/reports/M3-M6-mua-ban.md | Demo 5–7 phút, routes/tài khoản, báo cáo thực tế |

**Không sửa** V001/schema.sql/seed.sql, credential local, dữ liệu WAMP, .idea/workspace.xml. Không cần migration mới vì 22 bảng hỗ trợ đầy đủ. Thư mục này chưa là Git repository; không tạo commit hoặc khẳng định có git diff. WAR/runtime/test backup chỉ là artifact local, không để vào commit.

## Quyết định và transaction

1. Yêu cầu mới cho ảnh mặc định: 0–5 ảnh, thay đề xuất cũ bắt buộc có ảnh. Giữ seed HIDDEN/PENDING, không tự duyệt hoặc công khai dữ liệu cũ.
2. Buyer được PAY **chuyển khoản mô phỏng** đúng đơn PENDING/CONFIRMED theo yêu cầu demo. Service quyết PAID và lưu actor/history; đây không phải xác nhận tiền thật. COD chỉ paid khi ghi nhận đã giao.
3. Ghi chú checkout lưu vào reason của order history khởi tạo, hiển thị ở timeline. Tránh thêm cột khi schema đủ dùng; snapshot recipient/address giữ trên orders.
4. Checkout khóa actor rồi cart, khóa products tăng dần, đọc lại giá và eligibility; fingerprint quote chứa cart version/qty/product version/price. Kiểm toàn giỏ trước ghi; sau đó batch/orders/snapshot/payment/history/stock/cart cùng transaction READ COMMITTED. Retry cùng key/hash trả batch đã commit, kể cả giỏ đã rỗng.
5. DAO được tạo trong callback và nhận cùng Handle. JDBI đóng Handle sau callback; Hikari trả connection về pool. Hủy khóa order/payment và product tăng dần; status CANCELLED ngăn release lần hai, ledger unique củng cố ràng buộc. Ship không trừ kho lần hai. Admin audit chỉ lưu trường quản trị cần thiết, không ghi người nhận/địa chỉ/email/password/session vào log.
6. Ảnh mới được chuẩn bị ngoài DB, nếu request/transaction lỗi thì dọn đúng file vừa tạo. Khi thành công, asset/file giữ bất biến; thay tin chỉ gỡ product_images, không xóa file cũ. Cần sao lưu DB và storage đồng bộ; chưa có cleanup job hoặc transaction filesystem phân tán.

## Môi trường và lệnh kiểm tra thực tế

MySQL 9.1.0/InnoDB instance riêng: loopback port 13316, datadir `.local-test/mysql-data`; tạo schema **c2c_shop_test mới** sau SHOW DATABASES xác minh tên chưa có. Không dùng WAMP 3306. File test riêng `.local-test/shop.local.properties`, ảnh test `.local-test/shop-images`; không in credential. Tomcat 10.1.48 riêng, CATALINA_BASE `.local-test/tomcat`, loopback 18080, WAR context `/c2c`. Không đổi cấu hình hoặc restart Tomcat IntelliJ của người dùng.

| Lệnh/thao tác | Kết quả thực tế |
| --- | --- |
| SHOW DATABASES LIKE 'c2c_shop_test'; CREATE DATABASE ... utf8mb4 | Tên chưa có, tạo schema test riêng |
| APP_CONFIG_FILE test + `.\mvnw.cmd -B exec:java '-Dexec.args=migrate'` | SUCCESS, áp V001 1 migration |
| `.\mvnw.cmd -B exec:java '-Dexec.args=seed'` | SUCCESS, seed phát triển giữ nguyên dữ liệu |
| mysqldump --single-transaction c2c_shop_test | Backup trước test: `.local-test/shop-before-tests.sql`; chứa fixture, không đưa vào repo |
| `.\mvnw.cmd -B verify` | SUCCESS, 22 unit, WAR Java 17; build lại sau các sửa cần thiết |
| `verify -Pmysql-it '-Dit.test=DatabaseIT,UserServiceIT,AccountHttpIT'` | SUCCESS, 24 tests (11 DB + 7 Service tài khoản + 6 HTTP) trước thêm fixture thương mại; 22 unit cùng lượt đạt |
| `verify -Pmysql-it '-Dit.test=CommerceIT'` | SUCCESS, 3 kịch bản Service/MySQL: nhiều seller và vòng đời bank/COD; 2 buyer tranh tồn cuối; ép lỗi payment seller thứ hai kiểm rollback rồi retry |
| Deploy WAR mới trên Tomcat test, GET và POST hành trình ShopHttpIT | Đăng seller → multipart ảnh → Admin approve → catalog → cart → checkout/retry → PAY/retry → seller confirm/ship/deliver → buyer complete. Chi tiết/snapshot/private media và UTF-8 đạt |
| `verify -Pmysql-it '-Dit.test=AccountHttpIT,CommerceIT,ShopHttpIT'` | SUCCESS, 10 integration/HTTP (6 M2 + 3 Commerce + 1 Shop HTTP), cùng 22 unit đạt. Rerun sau sửa form/UTF-8/guard cùng schema |

Tổng **50 test cases khác nhau đã chạy đạt** qua các lệnh: 22 unit + 24 M1/M2 integration/HTTP + 4 mới. Không cộng số lần chạy lặp thành số test mới. Không gọi build thành bằng chứng deploy; HTTP được chạy trên Tomcat thật và kho trên MySQL/InnoDB thật.

Đã phát hiện và sửa lỗi UTF-8 của JSP/JSPF include: ban đầu HTTP đi đủ vòng đời nhưng nhãn “Hoàn thành” bị mojibake; khai báo encoding riêng từng file rồi chạy lại đạt. Đã sửa link giỏ/URL ảnh snapshot theo đúng asset + orderId, escape version form khi trả lỗi, chặn giá có exponent quá lớn, kiểm category không phải số và ảnh giả trả 400. UI user-supplied title/description được escape. HttpTestTarget tạo tài khoản nonce trong schema test và kiểm Tomcat đọc đúng trước HTTP ghi, không dựa riêng vào base URL loopback.

Đã kiểm trên backend/HTTP: tự mua bị từ chối, kho 0 bị từ chối, ownership tin/đơn/receipt/media, USER không vào Admin, POST thiếu CSRF, payload tên/mô tả không render script, malformed ảnh, phiên bản lỗi trong form, hai seller/tổng/snapshot, ảnh đơn vẫn tồn tại sau thay ảnh tin, paid refund/cancel repeat và unpaid COD void, COD PAID đúng lúc giao. Chưa thử tất cả cặp trạng thái/biên input/upload bằng browser thủ công.

## Cách chạy trên máy người dùng và giới hạn

- Theo [README](../../README.md), giữ file local M2, đặt selector cho Maven **và Tomcat**, chạy check/verify rồi redeploy/restart. Không chạy schema.sql/reset trên DB có dữ liệu. Bản mua bán không cần migrate bổ sung.
- Dùng [hướng dẫn demo](../09-demo-mua-ban.md) để chuẩn bị tin PUBLIC/APPROVED của hai seller, mở các phiên browser riêng, đặt/hủy/giao/nhận. Không có seed thương mại tự động để tránh đổi dữ liệu hiện có; tạo đơn trạng thái khác nhau qua đúng Service/UI.
- Chưa xác nhận WAR mới trên IntelliJ/WAMP của người dùng, MySQL 8.4 thực tế, kiểm UI bằng trình duyệt ở mọi kích thước, load/stress/deadlock mọi cặp thao tác hoặc failure máy giữa DB commit và filesystem. Bản test private được dừng sau kiểm tra; không phải dịch vụ demo lâu dài.
- Chưa có review/khiếu nại UI (M7/M8), chọn tin nổi bật/sort tùy ý, tự timeout giữ hàng, xóa ảnh mồ côi, phí giao hàng thực hoặc thanh toán/hoàn tiền thật. Guard complete khi complaint mở có truy vấn sẵn; phải test luồng khiếu nại thực ở M8.
- Catalog phân trang 12; danh sách quản lý tin/đơn giới hạn 200 dòng gần nhất để demo. Chưa có phân trang back office. Đơn chờ phải xử lý/hủy thủ công; không có job nền.

Giải thích bảo vệ đồ án: Servlet nhận HTTP → Service kiểm người thao tác, trạng thái và transaction → các DAO cùng Handle JDBI bind SQL → MySQL khóa và commit/rollback → Servlet redirect/JSP. Snapshot bảo vệ thông tin giao dịch khỏi việc người bán đổi tin; batch theo seller và idempotency tránh đơn/trừ kho lặp; payment riêng chứng minh mô hình COD/chuyển khoản mô phỏng.

Commit gợi ý: `feat: hoàn thiện luồng mua bán C2C, kiểm duyệt tin và thanh toán mô phỏng`.
