# TraoTay — tiếng Việt và nhận diện giao diện

Ngày thực hiện: 05/10/2026. Giữ Java Servlet/JSP/JSTL, MySQL/JDBI, teal và vàng cam. Lượt này sửa giao diện, không thêm migration, không sửa dữ liệu/charset của database ứng dụng.

## Chẩn đoán thực tế

“Hiện” được tạo bởi `assets/js/marketplace.js`, không lấy từ database. Đã đọc JSP/fragment, chuỗi JS, Servlet/Service, EncodingFilter và web.xml; quét strict UTF-8 các file Java/JSP/JSPF/JS/CSS/XML liên quan. Không phát hiện byte UTF-8 không hợp lệ hoặc mẫu mojibake thường gặp trong source.

Trước khi sửa, GET `/demo_war_exploded/login` và JavaScript trên Tomcat IDE cổng 8080 đều trả `charset=UTF-8`; byte JavaScript khớp source và chứa đúng “Hiện”, “Ẩn”. Chrome mới kiểm tra nút được **Hiện → Ẩn → Hiện**. Vì vậy **không tái hiện được lỗi chữ cũ trong lượt này**, không kết luận source hoặc database đang hỏng và không dùng đổi font để che lỗi.

Nguyên nhân đã được xác minh trong lần kiểm tra trước là DefaultServlet trên Tomcat Windows đọc asset bằng Cp1252 rồi trả UTF-8; xem [báo cáo chat](Chat-mua-ban.md). `fileEncoding=UTF-8` đã tồn tại trong source và được giữ lại. Tuy nhiên các URL shell vẫn dùng phiên bản `20261005-vnpay1`/`chat1`/`u1` từ trước; cache hoặc deployment cũ có thể tiếp tục dùng asset lỗi. Lượt này không có bằng chứng về cache trong cửa sổ trình duyệt người dùng, nên không khẳng định đó là nguyên nhân duy nhất của ảnh cũ.

Đã bổ sung default request/response UTF-8 trong descriptor, giữ EncodingFilter đứng trước AccessFilter, giữ charset HTML/JSON rõ ràng và đổi toàn bộ phiên bản CSS/JS shell sang `20261005-traotay1`. Không chuyển mã hàng loạt, không thay chuỗi tiếng Việt đang đúng bằng Unicode escape. Sau deploy, response JavaScript tiếp tục khớp source UTF-8; 59 response JSON chat/so sánh quan sát trong hành trình đầu đều khai báo UTF-8.

Một lỗi thông báo khác được tìm thấy: Fetch mất kết nối trả TypeError “Failed to fetch” bằng tiếng Anh. `messages.js` nay đổi trường hợp này sang **“Không thể kết nối. Vui lòng thử lại.”**; giữ AbortError, thông báo server, nonce và bản nháp theo cơ chế cũ. Đã ngắt riêng request gửi tin trong trình duyệt test để xác minh thông báo và nội dung vẫn còn.

## Nhận diện và chữ

Logo gốc: **`src/main/webapp/assets/images/Logo.png`**, PNG RGBA 2172 × 724, tỷ lệ 3:1, có khoảng trống trong ảnh. Dùng nguyên bytes, không vẽ lại, cắt ảnh, thêm nền/khung/bóng hoặc chữ cạnh logo. So sánh byte logo nguồn và bản deploy đạt.

`brand.jspf` khai báo TraoTay, khẩu hiệu **“Đồ cũ, giá trị mới.”** và URL logo bằng `c:url`. Header/footer và phần giới thiệu đăng nhập/đăng ký dùng ảnh này, link về `/home` theo context path, alt “TraoTay — Trang chủ”. Header rộng 180 px, 150 px trên màn hình nhỏ và 135 px ở 390 px; footer 210 px ở desktop/135 px ở mobile, auth intro 245 px, height auto. Khẩu hiệu header ẩn trên mobile; auth intro cũng giữ cơ chế ẩn mobile hiện có. Tiêu đề trình duyệt, display-name và giới thiệu trang chủ đã cập nhật; package/database/context và định danh kỹ thuật giữ nguyên.

Giữ **Segoe UI** có sẵn trên Windows, hỗ trợ tiếng Việt; fallback Arial/system-ui/sans-serif, không tải font/CDN. Font chung 15 px, line-height 1.65; tiêu đề 1.28–1.35, eyebrow 1.75 và giảm letter-spacing. Các weight 650/750/800 ở CSS nền được chuẩn hóa về 600/700. Font áp dụng cho body/form controls; không có rule đổi font toàn bộ icon, hệ thống icon SVG giữ nguyên.

Logo/search/actions ở header giữ cùng hàng trên desktop và hàng đầu mobile; search xuống hàng mobile theo bố cục sẵn có. Menu tài khoản mobile dùng biểu tượng với `aria-label`, tránh chữ tên bị ellipsis. Nút password được căn giữa trong input, chừa padding phải 82 px, có nhãn/pressed state hiện có; đã đo bounds và bấm thử cả password lẫn confirmation.

## File chính

Các đường dẫn bên dưới tính từ gốc repository:

| File | Thay đổi |
| --- | --- |
| `src/main/webapp/WEB-INF/views/layouts/brand.jspf` | Tên, khẩu hiệu, URL logo dùng chung |
| `src/main/webapp/WEB-INF/views/layouts/navigation.jspf` | Logo header, khẩu hiệu, nhãn menu tài khoản |
| `src/main/webapp/WEB-INF/views/layouts/shop-start.jspf` | Title, phiên bản asset, CSS TraoTay |
| `src/main/webapp/WEB-INF/views/layouts/shop-end.jspf` | Logo/footer và khẩu hiệu |
| `src/main/webapp/WEB-INF/views/auth/login.jsp`, `register.jsp` | Logo và giới thiệu auth |
| `src/main/webapp/WEB-INF/views/home.jsp` | Giới thiệu TraoTay |
| `src/main/webapp/assets/css/traotay.css` | File mới: typography/logo/header/password/mobile |
| `src/main/webapp/assets/css/startup.css`, `upgrades.css` | Chuẩn hóa font weight |
| `src/main/webapp/assets/js/messages.js` | Thông báo lỗi mạng tiếng Việt |
| `src/main/webapp/WEB-INF/web.xml` | Display-name và encoding mặc định |
| `README.md`, `docs/README.md`, `docs/05-ke-hoach-trien-khai.md`, `AGENTS.md` | Nhận diện, hướng dẫn và kết quả thực tế |
| `docs/screenshots/traotay/` | Ảnh Chrome thật trên fixture test, không phải dữ liệu ứng dụng |

Logo là file người dùng đã thêm, không được chỉnh sửa trong lượt này. Không sửa logic đăng nhập, CSRF, phân quyền, transaction hay migration.

## Kiểm tra đã chạy

- `./mvnw.cmd -B verify`: **BUILD SUCCESS, 28 unit tests, 0 failure/error/skip**. Build cuối lúc 21:42 ngày 05/10/2026 tạo `target/demo-1.0-SNAPSHOT.war`.
- Deploy WAR cuối lên Tomcat 10.1.48 riêng ở `http://127.0.0.1:18080/c2c`; MySQL/InnoDB cổng 13316, schema `c2c_upgrades_test`. Sao lưu test schema trước thao tác. Helper Java kiểm tra endpoint/schema `_test`, chứng minh Tomcat đọc cùng DB qua nonce account tạm, xóa nonce sau đó; JDBI `SELECT 1 = 1`. Các helper/cấu hình/backup trong `.local-test/` không commit.
- Chrome headless thật, desktop **1366 × 980**, mobile **390 × 844**: hành trình đầu đạt 7 nhóm kiểm tra trên **46 lượt mở trang**, không phải 46 trang khác nhau; không có pageerror.
- Đã kiểm trang chủ, đăng nhập/đăng ký; catalog/detail; giỏ trống và sau đó giỏ có sản phẩm; đơn mua/list/detail; đánh giá đã gửi và review công khai; khiếu nại/list/detail; chat; so sánh; Admin dashboard/products/complaints; seller list/form/orders. Có assertion HTTP 200, title, logo tự nhiên/context path, page không tràn ngang.
- Bấm Hiện/Ẩn cho các trường mật khẩu; đăng nhập sai và đăng ký đầu vào không hợp lệ qua server; render notice đăng ký thành công bằng query parameter (không tạo tài khoản demo mới).
- Hai context buyer/seller gửi và trả lời chat tiếng Việt qua AJAX, buyer nhận qua polling, không lặp message; ngắt request gửi để kiểm lỗi và giữ draft; mở lại lịch sử. Chỉ ghi trên test schema, không gửi tin vào DB ứng dụng.
- Mở/đóng modal ảnh review, lọc 4 sao; toggle chỉ khác biệt trên bảng so sánh; kiểm JSON UTF-8. Đã xem trực tiếp PNG đại diện và chỉnh menu mobile sau xem ảnh.
- Sau chỉnh menu, rebuild/redeploy và chạy kiểm tra cuối: 6 lượt home/login/register + 26 lượt buyer/Admin, modal/filter, menu, giỏ có sản phẩm, chat history/error draft; asset JavaScript byte-for-byte và logo byte-for-byte đạt, không pageerror. Chụp lại auth state Hiện/Ẩn, lỗi và notice ở cả hai kích thước.

Ví dụ ảnh đã xem: [Trang chủ desktop](../screenshots/traotay/home-1366.png), [đăng nhập mobile](../screenshots/traotay/login-390.png), [nút Ẩn](../screenshots/traotay/login-password-visible-390.png), [giỏ mobile](../screenshots/traotay/cart-390.png), [chat lỗi mạng/draft](../screenshots/traotay/chat-send-error-390.png), [modal review](../screenshots/traotay/review-modal-390.png), [Admin](../screenshots/traotay/admin-390.png).

Đây là kiểm tra tập trung về hiển thị và tương tác, **không kiểm toàn bộ nhánh nghiệp vụ, tải/cạnh tranh, thanh toán VNPAY hoặc mọi trình duyệt**. Dữ liệu tên/ảnh trong screenshots là fixture test. Tomcat IDE cổng 8080 chỉ được đọc để chẩn đoán đầu lượt; không dừng/restart tiến trình IDE của người dùng. Các helper MySQL/Tomcat riêng được dừng sau kiểm tra.

## Xem giao diện mới trên máy

Từ thư mục dự án, PowerShell:

```powershell
.\mvnw.cmd -B verify
```

Với IntelliJ/Tomcat: Stop Tomcat, Build → Build Artifacts → `demo:war exploded` → Rebuild, rồi Run lại Tomcat 10.1. Giữ VM options/file config DB/storage hiện có. Nếu deploy WAR thủ công, thay WAR trong appBase của Tomcat bằng `target/demo-1.0-SNAPSHOT.war` khi ứng dụng đã dừng, rồi khởi động lại theo cách đang dùng; giữ context cũ.

Mở `/home`, `/login`, `/register` trong context hiện có và **Ctrl+F5** một lần. DevTools Network phải thấy `marketplace.js?v=20261005-traotay1`, `traotay.css?v=20261005-traotay1`; response JS/HTML/JSON dùng UTF-8. Nếu còn chữ lỗi chỉ ở trình duyệt cũ, kiểm lại asset được tải và deployment trước khi sửa dữ liệu. Không chạy migrate/seed vì lượt này không đổi schema.

Commit gợi ý: `fix: đồng bộ UTF-8 và nhận diện TraoTay trên giao diện`.
