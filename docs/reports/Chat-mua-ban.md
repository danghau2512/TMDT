# Bàn giao chat mua bán — 05/10/2026

## Kết quả

Đã triển khai chat văn bản trước khi mua từ UI → Servlet → Service → DAO/JDBI → MySQL, không yêu cầu đơn hàng. Nút chi tiết sản phẩm, tiếp tục sau login, mở lại cùng conversation, hộp thư/unread, thẻ sản phẩm, history/cursor, tin mới qua polling, gửi/retry và giao diện desktop/mobile đã có. Không thay framework hoặc chức năng giao dịch trước đó.

Chỉ hai người tham gia được đọc/gửi; cả Admin cũng bị chặn nếu không tham gia. Service lấy actor session, kiểm ACTIVE/ownership, bind tham số, khóa conversation trong transaction. UUID + UNIQUE chống trùng khi response thất lạc; read cursor chỉ tăng đến tin đã hiển thị. Khi sản phẩm ẩn, giữ history/title snapshot, bỏ link và ảnh public; tài khoản ngừng hoạt động không gửi được.

## File chính

Các đường dẫn tính từ gốc dự án:

| File tạo/sửa | Vai trò |
| --- | --- |
| `src/main/resources/db/migration/V003__buyer_seller_chat.sql` | Hai bảng, PK/FK/CHECK/UNIQUE/index, UTF-8 và UTC; không sửa bảng cũ |
| `src/main/java/com/example/demo/model/ChatMessage.java` | DTO hiển thị, ID string tránh mất chính xác ở JS |
| `src/main/java/com/example/demo/dao/ChatDao.java` | Inbox, thread, bind, send/retry/read cursor và unread |
| `src/main/java/com/example/demo/service/ChatService.java` | Quyền, ACTIVE, giới hạn, chống tự chat, transaction, dữ liệu public/ẩn |
| `src/main/java/com/example/demo/controller/chat/ChatServlet.java` | JSP/form/JSON endpoints; không nhận senderId |
| `config/ShopServices.java` (dưới package Java) | Khởi tạo ChatService dùng chung Database |
| `filter/AccessFilter.java`, `controller/AccountSupport.java` | Bảo vệ /messages, CSRF, JSON lỗi API và continuation allowlist |
| `controller/auth/LoginServlet.java` | Quay lại sản phẩm cần chat sau đổi session ID/login |
| `src/main/webapp/WEB-INF/views/chat/start.jsp`, `chat/inbox.jsp` | Trang xác nhận, hộp thư và khung tin nhắn; c:out |
| `views/catalog/detail.jsp`, `views/layouts/navigation.jspf`, `shop-start.jspf` (dưới WEB-INF) | Nút chat, nav/badge, assets cục bộ |
| `src/main/webapp/assets/css/chat.css`, `assets/js/messages.js` | Responsive, textContent, một polling khi visible, scroll/dedupe/retry/loading |
| `src/main/webapp/WEB-INF/web.xml` | DefaultServlet fileEncoding UTF-8, readonly=true, listings=false phù hợp Tomcat |
| `src/test/java/com/example/demo/service/ChatIT.java`, `controller/ChatHttpIT.java` | 3 service/MySQL + 1 hành trình HTTP ngắn |
| `README.md`, `database/README.md`, `docs/README.md`, `docs/02...`, `03...`, `04...`, `05...`, `13-demo-chat-mua-ban.md` | Quy tắc, schema, tiến độ, deploy/demo |
| `docs/screenshots/chat/inbox-1440.png`, `thread-390.png`, `inbox-390.png` | Chrome thật trên fixture riêng; không dữ liệu ứng dụng |

V001/V002/schema.sql/seed.sql và file local credential giữ nguyên. Không sửa cấu hình riêng IntelliJ, không thực hiện commit trong lượt này. Helper, dump, log và config test chỉ ở `.local-test/` đã ignore.

## Kiểm tra thực tế và sửa lỗi

Môi trường: Oracle Java 17.0.12, Maven Wrapper 3.9.6, Tomcat 10.1.48 loopback 18080, MySQL 9.1.0 riêng port13316/schema `c2c_chat_test`. Schema test được sao lưu trước, Flyway áp V001/V002/V003 và nạp fixture phát triển riêng; không dùng c2c_demo để test ghi.

| Lệnh/thao tác | Kết quả thực sự |
| --- | --- |
| `.\mvnw.cmd -B verify` | BUILD SUCCESS; 28 unit tests, 0 lỗi; WAR tạo thành công |
| `exec:java '-Dexec.args=migrate'` với config test | Lần đầu áp 3 migration, chạy lại trả 0 |
| `exec:java '-Dexec.args=seed'` với config test | Fixture demo nạp thành công, chỉ schema test |
| `verify -Pmysql-it '-Dit.test=ChatIT'` | 3 tests PASS, 0 skipped; send/read/privacy/hidden/inactive, retry/concurrent start, lịch sử 50+2 tin |
| `verify -Pmysql-it '-Dit.test=ChatHttpIT'` | 1 test PASS, 0 skipped; Tomcat/DB được xác minh cùng schema qua HttpTestTarget trước HTTP ghi |
| Chrome headless, hai browser contexts + tài khoản thứ ba | 12 kiểm tra PASS, không pageerror; xem checklist bên dưới |
| `node --check src/main/webapp/assets/js/messages.js` | Cú pháp JS hợp lệ |
| Survey/backup/apply bằng JDBI + mysqldump cục bộ | c2c_demo standalone V002 có 23 bảng; V003 thêm 2 bảng trống; fingerprint cả 23 bảng cũ không đổi |

28 unit tests là bộ sẵn có, không phải 28 test chat. Tổng kiểm tra mới riêng chat: **3 integration Service + 1 HTTP**; không chạy lại toàn bộ bộ thương mại/VNPAY hoặc giả rằng build là deploy thành công. HTTP dùng guard `C2C_IT_ALLOWED=true`, schema `_test`, Tomcat loopback và cùng config test.

Hai lần kiểm tra browser đầu dừng vì chữ tiếng Việt của JS bị mojibake trên Tomcat Windows Cp1252. Đã sửa DefaultServlet fileEncoding UTF-8 theo [tài liệu Tomcat](https://tomcat.apache.org/tomcat-10.1-doc/default-servlet.html), rebuild/restart test Tomcat và chạy lại thành công. Đã xem ảnh thực tế rồi chỉnh CSS tránh padding section và label chiếm ô composer từ style chung; chạy lại Chrome 1440/390 đạt. Fixture helper Java cũng được chạy UTF-8; không thay dữ liệu ứng dụng để sửa fixture.

Checklist Chrome đã chạy:

1. Khách từ sản phẩm → login → quay đúng sản phẩm → mở chat, chưa có order.
2. Buyer gửi văn bản tiếng Việt/emoji và nội dung giống script: hiện dạng chữ, không thực thi.
3. Seller ở inbox nhận preview qua polling; unread tăng đúng một tin, mở thread thì giảm.
4. Seller trả lời; buyer thấy phản hồi không có main-frame navigation hoặc tin trùng.
5. Reload và logout/login lại giữ lịch sử; từ sản phẩm mở lại cùng conversation ID.
6. Tài khoản thứ ba nhận 404 cả trang và AJAX. Admin bị chặn trong ChatIT/ChatHttpIT.
7. Mô phỏng mất response sau khi server đã lưu: bản nháp vẫn còn, retry lưu/hiện đúng một tin.
8. Tin đến khi cuộn lên không kéo xuống; nút Có tin mới đưa đến cuối.
9. Ảnh từ storage đúng; 1440/390px không horizontal overflow/broken images. Đã xem ảnh, không chỉ đo HTTP.
10. Visibility hidden + event dừng vòng polling; không phát request API trong 5,2 giây tiếp theo.
11. Không pageerror trong hành trình cuối. Chưa thử mọi loại browser hoặc load test.

## Database và cách chạy

`c2c_demo` hiện có **25 bảng nghiệp vụ, không có Flyway history**. Backup đầy đủ `.local-test/c2c-demo-before-chat-20261005.sql` giữ kín. Không reset, không seed, không chạy V003 lần hai; V003 DDL chỉ tạo bảng mới, không UPDATE/DELETE dữ liệu cũ. Nếu cài khác có schema lệch/dở dang, khảo sát và migration bổ sung thay vì sửa migration đã áp.

Maven đã tạo `target/demo-1.0-SNAPSHOT.war`; cần rebuild artifact và Stop/Run **Tomcat IntelliJ của người dùng** để nhận source mới. Chưa deploy chat lên tiến trình Tomcat ứng dụng hoặc thử ghi vào c2c_demo; deploy/Chrome được kiểm trên Tomcat test riêng. Hai cấu hình không được trộn. Hướng dẫn thao tác Windows, migration và demo thường/ẩn danh ở [13 — Chat](../13-demo-chat-mua-ban.md).

Kiểm tra WAR cuối: có ChatServlet/V003, không có file local config hoặc .local-test. Kiểm tra Git read-only: config local, .local-test và .idea/workspace.xml không được track; không thực hiện commit. Đã dừng Tomcat test18080/MySQL test13316 sau kiểm tra, giữ WAMP3306 hoạt động và giữ dữ liệu test để đối chiếu.

## Giới hạn và giải thích đồ án

Polling 4 giây, badge ngoài hộp thư 20 giây; không push/WebSocket. Inbox 100 cuộc gần nhất, 50 tin mỗi lượt tải, history lưu lâu dài. Chỉ văn bản; không attachments, typing, email, gọi điện, sửa/xóa, moderation hoặc Admin đọc chat. Tab không mở chat không tự đánh dấu tin đã đọc. Browser visibility được mô phỏng bằng event trong kiểm tra tự động, chưa click chuyển tab trong trình duyệt có giao diện.

Servlet nhận session/CSRF → Service kiểm đúng hai người và ACTIVE, mở transaction → DAO trên cùng Handle bind SQL → MySQL lưu tin/read cursor. JSP chỉ hiển thị; JavaScript lấy tin sau cursor, textContent, chống trùng và giữ vị trí cuộn/bản nháp. Product public check dùng chung quy tắc catalog; chat không mở rộng quyền xem tin/ảnh bị ẩn, không ảnh hưởng đơn/tiền/kho.

Commit gợi ý: `Bổ sung chat mua bán theo sản phẩm, hộp thư và tin nhắn tự cập nhật`.
