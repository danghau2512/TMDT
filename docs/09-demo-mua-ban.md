# 09 — Demo mua bán C2C (M3–M6)

**Thanh toán mới 05/10/2026:** thay lựa chọn chuyển khoản mô phỏng trong kịch bản này bằng [VNPAY Sandbox theo từng đơn](12-vnpay-sandbox.md); COD giữ nguyên. PAY mô phỏng chỉ còn cho đơn cũ; không có nút giả PAID cho VNPAY.


## Chuẩn bị trước buổi trình bày

Build WAR bằng `.\mvnw.cmd -B verify`, redeploy trên Tomcat 10.1 với cấu hình MySQL đã dùng ở M2. Cấu hình trong terminal Maven không tự truyền vào IntelliJ/Tomcat: giữ VM option `-Dc2c.config=C:/duong-dan/config/application.local.properties` hoặc APP_CONFIG_FILE trong Run Configuration của Tomcat, rồi restart. Không tạo lại database hay chạy schema.sql trên dữ liệu cũ.

Ảnh nằm ngoài WAR: tùy chọn `upload.root=C:/c2c-data/uploads` trong file local hoặc biến UPLOAD_ROOT. Mặc định thư mục `.c2c-demo/uploads` dưới user.home của account chạy Tomcat. Giữ nguyên thư mục này khi redeploy, sao lưu cùng DB. Nếu đổi vị trí, chuyển nguyên các file đã có, không chỉ đổi config.

Ví dụ base URL IntelliJ: `http://localhost:8080/demo_war_exploded`. Nếu deploy WAR tên demo.war thì base là `http://localhost:8080/demo`. Tất cả đường dẫn phía dưới cộng vào base này.

Tài khoản fixture (chỉ dùng demo), mật khẩu chung **C2cDemo!2026**:

| Vai trò | Email |
| --- | --- |
| Admin | admin@c2c.example |
| Seller A | seller1@c2c.example |
| Seller B | seller2@c2c.example |
| Buyer | buyer1@c2c.example |
| Buyer phụ | buyer2@c2c.example |

Nếu bạn đã thay password demo, dùng password hiện tại; seed không ghi đè. USER vừa mua vừa bán, không có role SELLER riêng. Dùng các profile trình duyệt khác nhau hoặc các trình duyệt khác nhau để giữ phiên riêng; các tab thường cùng browser chia sẻ đăng nhập.

Seed cũ có 4 sản phẩm HIDDEN/PENDING. Catalog rỗng lúc đầu là đúng quy tắc. Trước demo, vào Admin → Quản lý sản phẩm, sửa 2 tin của 2 seller thành “Công khai sau khi được duyệt”, giữ kho >= 3, nhập lý do rồi lưu và duyệt có lý do. Chuẩn bị một tin khác còn PENDING để minh họa. Có thể đăng sản phẩm mới qua seller thay vì sửa fixture. Thiếu ảnh dùng ảnh mặc định; không cần fake metadata hoặc chỉnh trực tiếp SQL. Đừng công khai sản phẩm thật của người dùng chỉ để demo.

## Các trang

| Use case | Route GET |
| --- | --- |
| Trang chủ / danh mục / tìm kiếm | /home, /categories, /products |
| Bộ lọc ví dụ | /products?keyword=sach&category=510003&min=1000&max=500000&condition=USED&page=1 |
| Chi tiết tin | /products/detail?id=ID |
| Giỏ / checkout / kết quả | /cart, /checkout, /checkout/success?batch=ID |
| Quản lý tin cá nhân | /seller/products, /seller/products/new, /seller/products/edit?id=ID |
| Đơn mua / bán | /buyer/orders, /seller/orders |
| Chi tiết đơn | /buyer/orders/detail?id=ID, /seller/orders/detail?id=ID |
| Admin | /admin, /admin/products, /admin/products?status=PENDING, /admin/products/new, /admin/products/edit?id=ID, /admin/orders, /admin/orders/detail?id=ID |

Các thao tác ghi là POST qua form có CSRF: `/cart/add`, `/cart/update`, `/cart/remove`, `/checkout`, `/seller/products/save`, `/seller/products/action`, `/admin/products/save`, `/admin/products/action`, `/{buyer|seller|admin}/orders/action`. Không đổi trạng thái bằng GET.

## Kịch bản 5–7 phút

1. **0:00–1:00 — Người bán:** mở Tin của tôi → Đăng bán; điền tên/danh mục/giá/tình trạng/kho, chọn JPEG/PNG hoặc để mặc định. Tin mới PENDING; mở catalog chứng minh chưa xuất hiện. Nêu USER có thể vừa bán vừa mua.
2. **1:00–2:00 — Admin:** mở Tin chờ duyệt, duyệt tin với lý do. Có thể minh họa từ chối một tin khác rồi seller thấy lý do. Khi Admin đăng thay, biểu mẫu bắt chọn người bán ACTIVE và lý do, không nhận seller tùy ý từ người dùng thường.
3. **2:00–3:00 — Buyer:** tìm/lọc sản phẩm, xem liên hệ công khai; thêm mỗi seller một món vào giỏ, cập nhật số lượng. Checkout nhập người nhận/địa chỉ/ghi chú, chọn chuyển khoản mô phỏng. Trang kết quả có **2 mã đơn**, mỗi mã một seller, tổng tính trên server và kho đã được giữ.
4. **3:00–4:00 — Buyer/seller:** mở đơn A → Thanh toán mô phỏng; seller Xác nhận đơn → Bắt đầu giao → Ghi nhận đã giao; buyer Xác nhận đã nhận hàng. Trạng thái đơn COMPLETED và payment PAID độc lập, timeline ghi mỗi thao tác. Đổi seller account để xem đúng đơn của từng người.
5. **4:00–5:00 — Hủy đơn B:** buyer nhập lý do rồi hủy khi PENDING/CONFIRMED. Kho được hoàn đúng một lần; chưa paid thì VOIDED, đã paid thì REFUND_SIMULATED. Gửi lại thao tác không hoàn/trừ kho lần nữa. Không hủy đơn đã SHIPPED.
6. **5:00–6:00 — Snapshot:** seller sửa tên/giá/ảnh của tin A. Tin chờ duyệt lại; mở đơn cũ vẫn thấy tên/giá/mô tả/ảnh lúc đặt, kể cả khi ảnh tin hiện tại bị thay. Với COD, mô tả/hoặc dùng đơn chuẩn bị trước: chỉ DELIVERED mới ghi PAID, buyer vẫn phải xác nhận để COMPLETED.
7. **6:00–7:00 — Quyền và kết luận:** thử người khác mở URL chỉnh tin/chi tiết đơn, bị từ chối; tin hết kho không mua, tự mua bị chặn. Nêu bằng chứng kiểm tra hai buyer tranh tồn cuối và lỗi ở seller thứ hai được rollback trên MySQL riêng. Review/khiếu nại là mốc tiếp, không trình bày như đã có.

## Giải thích với giáo viên

Servlet đọc form và trả JSP/redirect; Filter kiểm phiên và CSRF. Service lấy tài khoản ACTIVE từ DB, kiểm ownership/trạng thái, tính tiền bằng BigDecimal và mở transaction. Các DAO dùng cùng Handle JDBI, bind tham số tới MySQL/InnoDB. Handle đóng khi callback kết thúc.

Checkout khóa giỏ và sản phẩm theo ID; tạo batch, một đơn mỗi seller, snapshot, payment, lịch sử và sổ kho rồi xóa giỏ trong cùng transaction. Một lỗi làm tất cả rollback. Key của lần checkout chống gửi lặp. Hủy khóa đơn, chỉ release kho khi trạng thái hợp lệ và chưa hủy; unique ledger và history hỗ trợ truy vết. Ảnh cũ giữ file bất biến, endpoint snapshot kiểm quyền theo đơn.

## Khi gặp lỗi

- 503 tài khoản/mua bán: kiểm cấu hình **của tiến trình Tomcat**, kết nối MySQL và 22 bảng, không chỉ cấu hình Maven. Dùng lệnh check trong README; restart/redeploy WAR mới. Không bật log chứa credential hoặc session.
- Không có sản phẩm: kiểm PUBLIC + APPROVED, seller/category ACTIVE. Stock 0 vẫn xem được nhưng không thêm giỏ.
- 409 khi sửa/đặt: tin/kho/giỏ/giá đã thay đổi; mở lại biểu mẫu hoặc xem lại checkout. Không gửi lại version cũ để ghi đè.
- Lỗi ảnh: chỉ JPEG/PNG thật, <= 5 MB và <= 20 megapixel; account chạy Tomcat cần quyền ghi thư mục upload. Khi bỏ trống file lúc sửa, ảnh cũ được giữ.
- Review/khiếu nại, vận chuyển/thu tiền thật, hoàn tiền thật và tự hết hạn đơn chưa có. Đơn chờ được giữ kho đến khi xử lý/hủy thủ công.
