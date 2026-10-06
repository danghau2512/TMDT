# Chat trước khi mua — 05/10/2026

Chat văn bản gắn sản phẩm và hai tài khoản, không cần đơn hàng. USER vẫn vừa mua vừa bán; buyer_id trong cuộc trò chuyện chỉ là người bắt đầu hỏi sản phẩm đó. Admin không có quyền đọc chat của người khác.

## Chạy trên máy hiện tại

`c2c_demo` trên WAMP 3306 đã được khảo sát, sao lưu và **áp V003 một lần** trong lượt này. Hai bảng `chat_conversations`, `chat_messages` đang trống; toàn bộ dữ liệu của 23 bảng trước đó được đối chiếu và giữ nguyên. Không chạy lại V003, schema.sql hoặc seed trên database này. Backup đầy đủ tại `.local-test/c2c-demo-before-chat-20261005.sql` chứa dữ liệu riêng tư; giữ cục bộ, không commit/chia sẻ.

```powershell
.\mvnw.cmd -B verify
```

WAR: `target/demo-1.0-SNAPSHOT.war`. IntelliJ: rebuild artifact đang dùng rồi **Stop → Run Tomcat** với `-Dc2c.config` trỏ file local hiện có. Không copy cấu hình mẫu ghi đè credential. Deploy WAR thủ công như README; ví dụ `c2c.war` có context `/c2c`. Tomcat không tự migrate/seed. Cấu hình DB/storage/VNPAY hiện có được giữ nguyên, chat không cần khóa cấu hình mới.

## Cài đặt khác và migration

Migration mới: `src/main/resources/db/migration/V003__buyer_seller_chat.sql`. Không sửa V001, V002 hoặc schema.sql (baseline M1).

**Schema có Flyway history hợp lệ hoặc schema trống riêng cho dự án:** khảo sát/sao lưu trước, chọn file cấu hình đúng schema, chạy:

```powershell
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
.\mvnw.cmd -B compile exec:java '-Dexec.args=migrate'
```

Flyway áp các phiên bản còn thiếu; schema trống đi V001 → V002 → V003. Chạy lại khi đã đủ phiên bản trả 0. Seed chỉ dành cho môi trường phát triển mới, không cần seed thêm dữ liệu chat; người dùng bắt đầu trao đổi qua UI.

**Schema standalone đã ở V002, chưa có Flyway history:** khảo sát `SHOW TABLES`, `SHOW CREATE TABLE users`, `products`, xác minh chưa có hai bảng chat; sao lưu đầy đủ theo hướng dẫn database/VNPAY. Không dùng migrate để auto-baseline schema đang có dữ liệu. Chỉ sau khi mapping đúng mới mở MySQL client với user có quyền CREATE/REFERENCES:

```powershell
$mysqlExe = 'C:\wamp64\bin\mysql\mysql9.1.0\bin\mysql.exe'
& $mysqlExe --protocol=TCP --host=127.0.0.1 --port=3306 --user=YOUR_SETUP_USER --password --default-character-set=utf8mb4 c2c_demo
```

Trong MySQL client, từ thư mục dự án:

```sql
SHOW TABLES LIKE 'chat_%';
-- Chỉ SOURCE khi đã backup, đúng standalone V002 và cả hai bảng chat chưa tồn tại:
SOURCE src/main/resources/db/migration/V003__buyer_seller_chat.sql;
SHOW CREATE TABLE chat_conversations;
SHOW CREATE TABLE chat_messages;
```

V003 không dùng DROP, reset hoặc IF NOT EXISTS che giấu lệch schema. DDL có implicit commit: nếu dở dang, dừng và khảo sát từng bảng, viết sửa bổ sung phù hợp; không SOURCE lại mù quáng. Database hiện tại đã nâng cấp nên không cần các lệnh SOURCE trên.

## Demo bằng hai cửa sổ (3–4 phút)

Dùng cửa sổ thường cho người mua, cửa sổ ẩn danh cho người bán để tách cookie/session. Không dùng hai tab cùng profile để đăng nhập hai tài khoản vì session sẽ dùng chung. Trên database phát triển có fixture demo, dùng `buyer1@c2c.example` và `seller1@c2c.example`, mật khẩu demo công khai `C2cDemo!2026`; không phải credential MySQL. Không nạp lại seed vào database đang dùng chỉ để có tài khoản này. Có thể dùng hai tài khoản tự đăng ký.

1. Người bán có tin PUBLIC/APPROVED, tài khoản và danh mục ACTIVE. Nếu cần tin demo mới: đăng sản phẩm theo UI, Admin duyệt bình thường; chat không tự duyệt tin.
2. Người mua mở chi tiết sản phẩm → **Chat với người bán**. Nếu chưa đăng nhập, vào login, sau thành công quay về màn hình xác nhận bắt đầu chat của đúng sản phẩm. Không phải đặt đơn trước.
3. Gửi “Sản phẩm còn không? Tình trạng hiện tại thế nào?”. Hộp thư người bán tự cập nhật trong khoảng 4 giây; cuộc mới lên đầu, hiện số chưa đọc.
4. Người bán mở cuộc trò chuyện: số chưa đọc giảm sau khi hiển thị tin. Nhập câu trả lời; người mua thấy tin mới mà không tải lại toàn trang. Hai màu phân biệt tin của mình/người kia.
5. Người mua trở lại sản phẩm và bấm chat lần nữa: cùng mã cuộc trò chuyện, không tạo bản mới. Reload hoặc logout/login: lịch sử còn nguyên.
6. Mở một tài khoản thứ ba (hoặc Admin) và thử URL `/messages?id=...`: không đọc được chat. AJAX cũng bị kiểm quyền, không chỉ giấu nút UI.
7. Khi cuộn lên đọc tin cũ, tin đến không kéo xuống; bấm **Có tin mới · Xem ngay** để đến cuối. Nếu gửi lỗi, nội dung còn trong ô nhập; thử lại cùng bản nháp không tạo trùng khi server đã lưu nhưng phản hồi bị mất.

## Quy tắc và endpoint

- Tin nhắn 1–2.000 ký tự Unicode sau trim, không nhận rỗng hoặc NUL; chỉ văn bản. Lưu UTC DATETIME(6), hiển thị giờ Việt Nam.
- Không chat chính mình. Một cặp product/buyer/seller chỉ một conversation. Tin gửi lấy actor từ session và kiểm ACTIVE lại ở Service; senderId trong request không được dùng.
- POST start/send/read có CSRF. GET inbox/api chỉ đọc; client POST read đến ID đã tải khi đang nhìn cuộc trò chuyện, tránh đánh dấu nhầm tin mới vừa tới.
- UNIQUE conversation/sender/client_nonce chống lưu trùng khi retry. Sửa bản nháp dùng nonce mới; server từ chối nonce cũ với nội dung khác.
- Tin ẩn/pending/rejected, seller/category ngừng hoạt động: giữ lịch sử và tên sản phẩm lúc bắt đầu chat, dùng ảnh mặc định và bỏ link chi tiết. Không mở đường tải ảnh hoặc xem tin vượt quyền. Nếu đối tác ngừng hoạt động, ô gửi bị khóa nhưng người đang ACTIVE vẫn đọc lịch sử.
- Inbox hiển thị tối đa 100 cuộc gần nhất; lịch sử tải 50 tin/lần, có nút xem tin trước. Badge tổng chưa đọc tính trên toàn bộ cuộc của tài khoản.
- Trang chat dùng một vòng polling 4 giây, dừng khi tab ẩn; badge trên trang khác cập nhật khoảng 20 giây. Không WebSocket, ảnh/file, typing, email, sửa/xóa tin hoặc quản trị đọc chat.
- Không JavaScript: gửi qua form vẫn dùng được, mở lại trang để thấy tin mới; có nút đánh dấu đã đọc. AJAX là cách demo được ưu tiên.

| Endpoint | Cách dùng |
| --- | --- |
| GET `/messages`, `?id=...` | Hộp thư/JSP, kiểm participant khi chọn thread |
| GET `/messages/start?productId=...` | Xác nhận sau login; không tạo dữ liệu |
| POST `/messages/start` | Suy ra seller từ sản phẩm, tạo hoặc mở lại chat |
| GET `/messages/api` | Danh sách và số chưa đọc |
| GET `/messages/api?conversationId=...&cursor=...` | Tin mới sau cursor |
| GET `/messages/api?conversationId=...&direction=older&cursor=...` | Tin cũ trước cursor; 0 là 50 tin gần nhất |
| GET `/messages/unread` | Tổng chưa đọc của session |
| POST `/messages/send` | conversationId, body, nonce, csrfToken |
| POST `/messages/read` | conversationId, through, csrfToken |

API trả JSON lỗi có HTTP 400/401/403/404/409/503 tương ứng; không redirect AJAX sang login rồi làm mất bản nháp. JSP dùng c:out, DOM dùng textContent, không render HTML từ tin nhắn. Tomcat DefaultServlet đã đặt fileEncoding UTF-8 để JavaScript tiếng Việt không bị đọc theo Cp1252 trên Windows.

Giải thích với giáo viên: Servlet nhận HTTP và session → Service kiểm ACTIVE/quyền tham gia, quyết định transaction → ChatDao bind tham số qua JDBI → MySQL lưu conversation/message/read cursor. JSP tạo UI ban đầu; JavaScript tải các tin sau ID cuối, loại trùng và POST mốc đã đọc. Khóa conversation giúp tin trong một thread có thứ tự commit nhất quán; UUID retry và UNIQUE bảo vệ khi bấm/gửi lại. Không gọi DAO từ JSP, không giữ Handle qua request.

Kết quả thực tế, ảnh và lệnh kiểm tra tại [báo cáo chat](reports/Chat-mua-ban.md).
