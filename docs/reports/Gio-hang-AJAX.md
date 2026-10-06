# Giỏ hàng với nút −/+ và AJAX — 05/10/2026

Đã thay ô số lượng + nút “Cập nhật” bằng bộ điều khiển −/số lượng/+. Nút − dừng ở 1; nút + dừng tại số lượng còn bán hoặc 999. Người dùng có thể nhập trực tiếp rồi Enter/rời ô. Giữ ảnh sản phẩm, người bán và phong cách TraoTay hiện có. Xóa khỏi giỏ cũng dùng AJAX; khi xóa món cuối, hiển thị giỏ trống ngay.

## Luồng và bảo vệ dữ liệu

- POST `/cart/update`, `/cart/remove` với `Accept: application/json` trả CartResponse UTF-8. GET `/cart` cùng header trả trạng thái giỏ hiện tại. Request HTML vẫn dùng form/303 redirect; tắt JavaScript vẫn bấm được −/+ và xóa.
- Người mua lấy từ session. AccessFilter refresh ACTIVE, kiểm CSRF và trả lỗi JSON cho AJAX. Service giữ kiểm tra chủ giỏ, cấm tự mua, sản phẩm công khai và số lượng/tồn kho; DAO hiện có bind tham số, dùng cùng Handle của transaction.
- `changeAndView` đọc lại giỏ trong cùng transaction thay đổi. Giá, thành tiền và tổng tiền do server tính bằng BigDecimal; client chỉ định dạng/hiển thị dữ liệu được trả lại, không quyết định giá hay checkout.
- JSON chỉ chứa `items` và `total`; mỗi item có `id`, `quantity`, `price`, `lineTotal`, `maxQuantity`, `available`, `canChangeQuantity`. Không trả email/hash/thông tin bảng sản phẩm riêng tư. Giữ cơ chế checkout, quote, tồn kho và thanh toán cũ.
- Mỗi lần chỉ có một POST đang chạy; khóa các thao tác số lượng/xóa tạm thời. Nếu POST thất bại, đọc lại giỏ vì server có thể đã ghi trước khi mất response. Nếu đọc lại cũng thất bại, khóa sửa/checkout và hiện “Kiểm tra lại giỏ hàng”. Không tự gửi lại POST. Nếu cửa sổ khác thêm một món chưa có trên trang, yêu cầu tải lại để không checkout món chưa hiển thị.
- Nội dung thông báo được gán bằng textContent; JSP vẫn escape nội dung người bán/sản phẩm. Không có polling hoặc thay schema, không chạy migration/seed.

## File chính

| File | Thay đổi |
| --- | --- |
| `src/main/webapp/WEB-INF/views/buyer/cart.jsp` | Nút −/+, bỏ Cập nhật, các vùng tổng tiền/trạng thái/giỏ trống cho AJAX |
| `src/main/webapp/assets/js/cart-ajax.js` | Fetch POST/GET, cập nhật DOM, xử lý lỗi và đồng bộ lại |
| `src/main/webapp/assets/css/cart-ajax.css` | Stepper, thông báo và bố cục responsive |
| `src/main/webapp/WEB-INF/views/layouts/shop-start.jspf` | Nạp assets local có version cache; JS chỉ hoạt động trên trang giỏ |
| `src/main/java/com/example/demo/controller/buyer/CartCheckoutServlet.java` | HTML/JSON dùng chung Service, hỗ trợ adjust cho form không JS |
| `src/main/java/com/example/demo/service/CartService.java` | Snapshot sau thay đổi cùng transaction, giới hạn/điều kiện điều chỉnh |
| `src/main/java/com/example/demo/model/CartResponse.java` | DTO allowlist cho JSON giỏ |
| `src/main/java/com/example/demo/controller/AccountSupport.java` | Nhận diện request JSON giỏ và trả lỗi tương ứng |
| `src/main/java/com/example/demo/filter/AccessFilter.java` | Lỗi hết session riêng cho AJAX giỏ; giữ CSRF/quyền cũ |

README, docs/README, docs/03-kien-truc và AGENTS được cập nhật. Hai ảnh dưới đây là fixture ở schema test, không phải dữ liệu giỏ của người dùng.

## Kiểm tra thực tế

- Chạy `.\mvnw.cmd -B verify`: BUILD SUCCESS, 28 kiểm thử hiện có đạt, tạo `target/demo-1.0-SNAPSHOT.war`. Build chạy lại sau chỉnh CSS và tiếp tục thành công. Không thêm bộ unit test lặp implementation.
- Sao lưu **c2c_upgrades_test** trước thử nghiệm vào `.local-test/c2c-upgrades-before-cart-20261005.sql`. Dùng MySQL 9.1.0 riêng tại loopback 13316, Tomcat 10.1.48 riêng tại 18080/context `/c2c`, Java 17. Guard xác nhận HTTP và JDBI cùng schema test; SELECT 1 trả 1. Không ghi hoặc thay schema **c2c_demo**.
- Chrome headless thực tế, 1366 × 980 và 390 × 844. Hai script thử tập trung `.local-test/cart-browser.cjs`, `.local-test/cart-edge.cjs` chạy bằng Node/Playwright cục bộ; 19 kiểm tra đạt:
  - −/+, nhập rồi Enter, thành tiền/tổng đúng với hai sản phẩm; không đổi document/tải lại trang; không còn nút Cập nhật.
  - Giới hạn 1, số lượng không hợp lệ phục hồi số đã lưu, vượt tồn trả 409 không ghi, nút + khóa tại tồn và giảm lại được.
  - Thiếu CSRF trả 403 JSON; khách trả 401 JSON; tài khoản khác gửi productId/buyerId không sửa được giỏ chủ sở hữu (404).
  - Lỗi mạng trước khi ghi giữ số cũ; mất response sau khi đã ghi đọc lại số mới, không POST lặp. Mất cả POST/GET khóa thao tác/checkout; nút kiểm tra lại khôi phục khi có mạng.
  - Bấm nhanh chỉ tạo một POST đang chạy; JSON chỉ chứa allowlist; reload còn số lượng; xóa một/món cuối qua AJAX đúng tổng/giỏ trống; tắt JS vẫn dùng nút −/+.
  - Kiểm tra screenshot desktop/mobile, không tràn ngang trang và không có lỗi JavaScript.
- Chỉ kiểm tra phạm vi giỏ và các lỗi HTTP liên quan. Không chạy lại toàn bộ nghiệp vụ đặt hàng/VNPAY hoặc kiểm tải lớn. Tomcat/MySQL thử nghiệm được dừng sau kiểm tra; Tomcat 8080 và MySQL 3306 của người dùng giữ nguyên.

## Build/deploy và demo

```powershell
.\mvnw.cmd -B verify
```

Trong IntelliJ, **Stop Tomcat → Build/Rebuild artifact đang deploy → Run Tomcat → Ctrl+F5**. Cần restart vì Servlet/Service đã thay đổi; chỉ copy CSS/JS chưa đủ. Nếu deploy WAR, thay artifact bằng `target/demo-1.0-SNAPSHOT.war` rồi khởi động Tomcat theo README. Giữ cấu hình MySQL/upload hiện có; **không chạy SQL**.

Đăng nhập → thêm hai món vào giỏ → bấm +/−, nhập số rồi Enter → quan sát số lượng/thành tiền/tổng đổi không reload → xóa một món rồi món cuối. Có thể bật Offline trong DevTools, thử tăng số lượng, bật lại mạng và bấm “Kiểm tra lại giỏ hàng”. Giỏ không giữ trước tồn kho; server vẫn kiểm tra lại khi đặt hàng.

[Ảnh desktop 1366 px](../screenshots/cart-ajax/cart-1366.png) · [Ảnh điện thoại 390 px](../screenshots/cart-ajax/cart-390.png).

Gợi ý commit: `Cải tiến giỏ hàng với nút tăng giảm số lượng và cập nhật AJAX`.
