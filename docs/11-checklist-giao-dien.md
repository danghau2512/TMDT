# 11 — Giao diện marketplace và kiểm tra trước thuyết trình

Bản giao diện ngày 04/10/2026 dùng teal `#0F766E`, nhấn vàng cam `#F59E0B`, nền `#F6F7F9`, font Segoe UI/system có tiếng Việt. Không tải font/CSS/icon từ CDN. Tên thương hiệu dùng chung tại `src/main/webapp/WEB-INF/views/layouts/brand.jspf`.

## Build và nạp bản mới

```powershell
.\mvnw.cmd -B verify
```

Redeploy `target/demo-1.0-SNAPSHOT.war` trên Tomcat 10.1 hiện có rồi restart/reload ứng dụng. Với IntelliJ, cập nhật đúng artifact WAR/exploded và chạy lại cấu hình Tomcat. Giữ APP_CONFIG_FILE hoặc VM option `-Dc2c.config`, URL MySQL và upload.root hiện tại; không cần migrate/seed cho thay đổi này.

Mở context hiện có rồi vào `/home`. Nhấn **Ctrl+F5** nếu CSS/JS cũ còn trong cache. Link asset dùng `?v=20261004-ui1`; tăng phiên bản này ở `shop-start.jspf` khi chỉnh asset trong tương lai. Nếu vẫn cũ, kiểm tra artifact/context đang deploy, không chỉ refresh trang.

## Kiểm tra trình chiếu

| Trang | Thao tác và điều cần nhìn |
| --- | --- |
| Home/catalog | Banner, danh mục lấy từ DB, ảnh/title/giá, tìm kiếm, xóa lọc, phân trang, tìm từ khóa không tồn tại |
| Chi tiết sản phẩm | Ảnh lớn, thumbnail, click ảnh phóng lớn, seller/contact, giá/kho, thêm giỏ, mô tả và đánh giá thật |
| Cart/checkout | Thay số lượng/xóa, tổng tiền, tên/điện thoại/địa chỉ, COD hoặc chuyển khoản mô phỏng, trang thành công và link mã đơn |
| Login/register/profile | Lỗi nhập liệu, giữ tên/email/phone khi lỗi, hiện/ẩn mật khẩu, không có credential demo trên trang đăng nhập |
| Buyer/seller | Menu tài khoản, sidebar/thanh điều hướng mobile, đơn theo trạng thái, badge, snapshot/timeline và nút đúng trạng thái |
| Đăng sản phẩm | Ba nhóm thông tin, preview JPEG/PNG, validation server không thay đổi, submit multipart có CSRF |
| Review/complaint | Chọn sao bằng chuột hoặc phím, gửi nhận xét, form lý do/nội dung/ảnh, evidence phóng lớn chỉ theo quyền |
| Admin | Sidebar, tin chờ duyệt/hồ sơ mới, bảng cuộn trong khối ở mobile, lý do bắt buộc, đơn/snapshot/lịch sử/phản hồi |

Trong Chrome DevTools, thử width 1440 và 390px; kéo ngang **bên trong bảng/menu** khi nội dung dài, không để cả trang tràn ngang. Thử Tab/Shift+Tab và Escape trên menu/ảnh. Kiểm tra ảnh chưa có và ảnh tải lỗi. Kiểm tra thêm trên điện thoại thật trước khi trình chiếu nếu có điều kiện.

## Ảnh và dữ liệu demo

Ảnh sản phẩm của ứng dụng vẫn lấy từ upload/storage hiện tại. Không thay ảnh/tin/dữ liệu của người dùng. Tin chưa có ảnh dùng SVG ghi rõ “Người bán chưa thêm ảnh”; ảnh lỗi có fallback. Banner là minh họa trang chủ, không phải ảnh sản phẩm.

Ảnh trong [báo cáo giao diện](reports/UI-hoan-thien-giao-dien.md) được chụp từ Tomcat thật với schema test riêng. Bốn món có ảnh là **fixture với hình minh họa**, được tạo/duyệt qua các form hiện có, không nạp vào database ứng dụng. Khi demo trên database của bạn, chuẩn bị ảnh chụp đúng món đồ, đủ sáng, cùng nền, rồi upload qua form seller; không dùng hình minh họa làm ảnh chụp thật. JPEG/PNG tối đa 5 MB/ảnh, tối đa 5 ảnh/tin theo luật hiện có. Tin mới/sửa ảnh cần duyệt lại.

Kịch bản nghiệp vụ vẫn ở [demo mua bán](09-demo-mua-ban.md) và [demo đánh giá/khiếu nại](10-demo-danh-gia-khieu-nai.md).
