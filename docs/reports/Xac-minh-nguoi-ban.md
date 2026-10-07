# Kết quả xác minh hồ sơ người bán — 06/10/2026

**Báo cáo lịch sử luồng OCR trước khi đổi yêu cầu. Hiện đã dùng QR local:** [báo cáo mới](Xac-minh-nguoi-ban-QR.md), [hướng dẫn hiện tại](../15-xac-minh-nguoi-ban.md). Phần còn thiếu API key bên dưới là trạng thái của lượt OCR cũ, không áp dụng luồng QR hiện tại.

Đã triển khai UI → Servlet → Service → DAO/JDBI → MySQL, adapter FPT.AI backend và fallback nhập thủ công. Giữ Java 17/Jakarta Servlet/JSP, TraoTay/Logo.png/UTF-8/CSRF và nghiệp vụ hiện có. USER vừa mua vừa bán; hồ sơ chỉ cấp quyền tạo/gửi duyệt/công khai tin, không thay kiểm duyệt nội dung sản phẩm. Hướng dẫn thao tác, migration, cấu hình và demo ở [15 — Xác minh](../15-xac-minh-nguoi-ban.md).

## Database và quyết định

- Khảo sát đúng loopback 3306/schema c2c_demo standalone V004: 26 bảng, chưa có bảng xác minh, API key OCR chưa cấu hình. Không đọc/in credential hoặc đưa dữ liệu hàng vào báo cáo.
- Full backup riêng tư `.local-test/c2c-demo-before-verification-20261006.sql` trước V005. Fingerprint số dòng và toàn bộ cột cũ của 26 bảng trước/sau giống nhau; V005 thêm hai bảng trống, không seed/approve người dùng. Không sửa V001–V004/schema.sql.
- Khi rà soát CHECK, bổ sung V006 thay vì sửa V005 đã áp: bắt buộc lý do từ chối không NULL và trường của hồ sơ đã gửi. Full backup `.local-test/c2c-demo-before-v006-20261006.sql`, đối chiếu 28 bảng trước/sau không đổi dữ liệu. Hiện 28 bảng nghiệp vụ, 29 ở schema có Flyway history. Không SOURCE lại V005/V006 trên c2c_demo hiện tại.
- seller_profiles có một dòng/tài khoản và pointer composite FK đúng chủ submission. Submission lưu bản DRAFT/PENDING/APPROVED/REJECTED/SUPERSEDED/PURGED, consent, dữ liệu tối thiểu, reviewer và thời điểm. Nhãn public chỉ boolean; họ tên pháp lý không thay display_name. Xóa giấy tờ xóa bytes/PII của các phiên bản, giữ metadata quyết định.
- Admin cần nhận xử lý trước khi xem ảnh/PII; chỉ Admin đã nhận mới quyết định. Không cấp quyền đọc chat chung cho Admin. Thay hồ sơ đã duyệt ngừng nhãn/quyền đăng tin mới nhưng giữ sản phẩm công khai/đơn cũ. Service khóa actor → profile → submission; cập nhật status/pointer cùng transaction. ProductService kiểm hồ sơ trong transaction tạo/gửi duyệt/công khai tin, kể cả đường Admin.

## File chính

| Nhóm | File / vai trò |
| --- | --- |
| SQL | `src/main/resources/db/migration/V005__seller_verification.sql`, `V006__verification_required_fields.sql`; bản byte giống nhau ở `database/migrations/` |
| Config | `OcrConfig.java`, `AppConfig.java`, `ShopServices.java`, `ApplicationListener.java`, `config/application.example.properties`, `.gitignore` — key backend, root riêng, không auto migrate |
| OCR | `verification/FptOcrClient.java`, `OcrResult.java` — multipart/image/api_key, allowlist trường, không redirect, timeout/giới hạn response, không log JSON/key |
| Backend | `dao/SellerVerificationDao.java`, `service/SellerVerificationService.java`, `controller/seller/SellerVerificationServlet.java` — bản hồ sơ, quyền/consent/upload/review/purge và private media |
| Đăng tin | `ProductService.java`, `ProductServlet.java` — kiểm hồ sơ server; xử lý POST không có multipart/ảnh để tránh lỗi 500 |
| Nhãn | `CatalogDao.java`, `ChatDao.java`, `ChatService.java`, `ProfileServlet.java`; `catalog/detail.jsp`, `chat/inbox.jsp`, `auth/profile.jsp`, `messages.js` — trạng thái server, không lộ giấy tờ |
| UI | `views/verification/own.jsp`, `queue.jsp`, `detail.jsp`, `tags.jspf`, `images.jspf`; `seller-verification.css/js`; shell `shop-start.jspf`, `workspace.jspf`, `admin/index.jsp` |
| Hỗ trợ | `ImageStorage.java` — xóa giấy tờ có báo lỗi, vẫn giữ xử lý ảnh/asset sản phẩm cũ; `AccessFilter.java` — thông báo giới hạn hai mặt ảnh |
| Test | `FptOcrClientTest`, `SellerVerificationIT`, `SellerTestProfiles`; fixtures của CommerceIT/UpgradesIT/ReputationIT/ShopHttpIT/FeedbackHttpIT/ChatHttpIT thêm điều kiện hồ sơ giả đã duyệt qua Service, chỉ schema *_test |
| Docs | README, docs/README, docs/02–05, database/README, AGENTS, hướng dẫn 15 và báo cáo này |

Ảnh: docs/screenshots/verification — trang bắt đầu 1366/390, Admin đối chiếu 1366/390 đã che trường cá nhân. Giấy tờ giả tự ghi DEMO, không có căn cước thật trong source/seed. Script và kết quả thử riêng tư ở .local-test ignored.

## Kiểm tra đã thực sự chạy

- `.\mvnw.cmd -B verify`: BUILD SUCCESS, **31 unit test đạt**, WAR tạo thành công. Ba kiểm tra mới: HTTP multipart/allowlist với server phản hồi mẫu, xử lý thiếu khóa/kết quả OCR lỗi, từ chối endpoint ngoài nhà cung cấp/root trùng ảnh sản phẩm. Server mẫu chỉ kiểm protocol, không thực hiện nhận diện.
- MySQL 9.1.0 instance riêng 13316, schema c2c_upgrades_test được backup trước V005/V006; Flyway chạy từng migration mới. Guard đã xác nhận HTTP Tomcat 18080 và JDBI cùng DB test, SELECT 1=1. Không dùng c2c_demo cho các kiểm tra ghi.
- `verify -Pmysql-it '-Dit.test=SellerVerificationIT'`: **3 test đạt** — chưa duyệt không tạo tin; thiếu khóa thủ công; quyền owner/Admin nhận xử lý; duyệt/đăng tin/kiểm duyệt riêng; đổi giấy tờ/từ chối/gửi lại/giữ sản phẩm cũ/purge; OCR baseline và thông tin đã sửa không giả được từ POST; CHECK không chấp nhận lý do NULL/trường thiếu.
- Bộ cũ chạy lại riêng: **CommerceIT 3**, **ChatIT 3**, **ReputationIT 3**, **UpgradesIT 3**, **ShopHttpIT 1**, **FeedbackHttpIT 1**, **ChatHttpIT 1** đều đạt. Bao gồm checkout/stock/snapshot, chat và đánh giá/khiếu nại; fixtures được duyệt giả qua Service trước đăng tin. Không chạy bộ M1/M2 seed assumptions trên schema đã có các fixture này.
- Chrome headless trên Tomcat 10.1.48, Java 17; **20 nhóm kiểm tra trình duyệt đạt** trong .local-test/verification-browser.cjs: chuyển trang từ Đăng bán, chặn POST/status giả, consent/upload/preview, manual thiếu key, CSRF/trường, gửi → nhận xử lý → từ chối → gửi lại → duyệt, không cho người thứ ba/Admin khác xem ảnh, tự điền từ **baseline OCR giả chỉ trong DB test**, so sánh trường đã sửa, đăng tin PENDING, nhãn sản phẩm/profile/chat, đổi giấy tờ chặn mới và giữ cũ, purge, GET smoke các trang chính, không lỗi JS.
- Kiểm tra bổ sung UI cuối: trình chọn ảnh tiếng Việt bằng click thực tế, preview, từ chối file giả PNG không hợp lệ; desktop 1366 × 980/mobile 390 × 844 không tràn ngang. Đã xem screenshot để xác nhận bố cục.
- Sau kiểm tra, purge qua Service trên đúng schema test: không còn trường giấy tờ/ảnh có key trong các submission; dừng Tomcat 18080 và MySQL 13316. Không dừng hoặc sửa cấu hình dịch vụ 8080/3306 của người dùng. Bản sao lưu test/ứng dụng vẫn giữ riêng tư trong .local-test theo hướng dẫn lưu giữ.
- Lỗi tìm thấy và sửa: POST sản phẩm urlencoded bị gọi getParts và trả 500; đã chỉ parse multipart khi có và chặn hồ sơ trước lưu. EXISTS MySQL trả Long, JSP boolean gây 500; đã so sánh số rõ ràng. Kiểm tra chạy lại đã qua các trang này.

Lệnh chính đã dùng (APP_CONFIG_FILE/C2C_IT_ALLOWED chọn riêng schema test; C2C_HTTP_BASE chọn Tomcat test):

```powershell
.\mvnw.cmd -B verify
.\mvnw.cmd -B compile exec:java '-Dexec.args=migrate'
.\mvnw.cmd -B verify -Pmysql-it '-Dit.test=SellerVerificationIT,CommerceIT'
.\mvnw.cmd -B verify -Pmysql-it '-Dit.test=ChatIT,UpgradesIT,ReputationIT'
.\mvnw.cmd -B verify -Pmysql-it '-Dit.test=ShopHttpIT,FeedbackHttpIT,ChatHttpIT'
```

Migration standalone c2c_demo dùng helper guard/backup/fingerprint rồi createScript của JDBI trên đúng schema; không dùng lệnh Flyway migrate cho standalone. Các helper bỏ cause chứa SQL/config khỏi thông báo.

## Phần còn cần kiểm tra

**Chưa có API key FPT.AI trên cấu hình hiện tại**, nên chưa gửi ảnh/nhận diện giấy tờ thật qua nhà cung cấp. Adapter gọi HTTP, schema, UI/manual, fallback và so sánh baseline đã kiểm tra độc lập. Cần key có quyền/quota cho endpoint, mạng HTTPS của tiến trình Tomcat, consent và ảnh mặt trước phù hợp để thử OCR thật. Không kết luận OCR thật thành công từ JSON mẫu hoặc fixture DB.

Không thực hiện nhận diện khuôn mặt/liveness/chip/VNeID, huấn luyện AI, tự động duyệt, thanh toán production hoặc xác thực với nhà nước. Không kiểm tải lớn, mọi kiểu ảnh hỏng hay toàn bộ VNPAY Sandbox trong lượt này; kiểm tra trực quan tập trung các màn hình xác minh và các điểm nhãn. Chưa có chuyển Admin phụ trách, quy trình lưu giữ/xóa bên nhà cung cấp hoặc job dọn backup tự động; dùng hướng dẫn xóa sau demo.

Người dùng cần **Stop Tomcat → Rebuild artifact → Run → Ctrl+F5** để nhận Servlet/Service/asset mới. Không chạy lại SQL trên c2c_demo hiện tại. Giữ credential cục bộ; thêm OCR key kín nếu muốn đọc tự động. [Cách thao tác và demo ngắn](../15-xac-minh-nguoi-ban.md).

Gợi ý commit: `Bổ sung xác minh hồ sơ người bán, OCR FPT.AI và duyệt hồ sơ riêng tư`.
