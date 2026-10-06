# Báo cáo — Hoàn thiện giao diện marketplace

Ngày 04/10/2026. Triển khai thực tế trên JSP/CSS/JavaScript, giữ Servlet/Jakarta, MySQL/JDBI/Hikari, routes, POST, session, phân quyền, CSRF và các quy tắc M1–M8. Không sửa SQL, cấu hình/credential, upload thật hoặc dữ liệu ứng dụng. Đây là phần giao diện và demo của M9, không tuyên bố đã hoàn tất mọi tiêu chí tích hợp/migration của M9.

## Thiết kế và các trang nâng cấp

- Teal, nền sáng, card trắng, góc 12–16px, viền/shadow nhẹ; font hệ thống hỗ trợ tiếng Việt. SVG icon/ảnh dự phòng/banner cục bộ, không phụ thuộc Internet khi trình chiếu.
- Layout dùng chung: header hai hàng, tìm kiếm nổi bật, giỏ, đăng bán, menu theo tài khoản/role; đánh dấu trang hiện tại. Footer chỉ trỏ routes đang có. Sidebar Admin và tài khoản chuyển thành thanh cuộn nội bộ trên mobile.
- Home có banner minh họa, CTA khám phá/đăng bán, danh mục và sản phẩm mới lấy từ DB, ba điểm hỗ trợ giao dịch. Không thêm lượt mua, sao hoặc thống kê giả.
- Catalog: card ảnh tỷ lệ 4:3, title hai dòng, giá VNĐ, tình trạng/seller/hết hàng; kết quả thực tế từ query count hiện có, bộ lọc/xóa lọc/phân trang và empty state.
- Detail: ảnh lớn object-fit contain, thumbnail chọn ảnh, phóng lớn; khối seller riêng, mô tả/review riêng, thống kê và sao từ review thật.
- Cart: ảnh/tên/seller/số lượng/POST cập nhật-xóa giữ nguyên, khối tóm tắt và CTA. Checkout chia thông tin nhận hàng/tóm tắt có ảnh, phương thức mô phỏng, hướng dẫn tách đơn. Trang thành công có icon, mã đơn và điều hướng.
- Login/register/profile cùng shell, form rõ nhãn/lỗi; hiện/ẩn password không đổi giá trị hoặc tự điền credential. Không hiển thị password demo công khai.
- Seller form chia ba nhóm, preview ảnh; bảng tin đăng và badge nhất quán. Đơn có mã/status ở đầu, snapshot và timeline, nút giữ đúng điều kiện server.
- Review dùng radio 1–5 trực quan, accessible qua bàn phím, vẫn gửi field `rating`. Complaint dùng thumbnail evidence có phóng lớn, lịch sử/tiến độ/kết luận theo khối; quyền MediaServlet giữ nguyên.
- Admin có sidebar/tiles điều hướng không kèm số liệu giả; bảng sản phẩm/đơn/khiếu nại, lọc/badge/lý do giữ nguyên, vùng cuộn riêng khi hẹp.
- Progressive enhancement: JS chỉ menu, password toggle, thumbnail/lightbox, preview/fallback; không tự POST, không tính tiền hay quyết quyền. Không có JS thì links/form/radio vẫn dùng được; ảnh thumbnail/evidence mở trực tiếp qua URL hiện có.

## File chính thay đổi

| Nhóm | File (từ gốc dự án) |
| --- | --- |
| Theme | Viết lại `src/main/webapp/assets/css/startup.css` theo tokens/layout/components/responsive; tạo `assets/js/marketplace.js` |
| Layout | Tạo `WEB-INF/views/layouts/brand.jspf`, `icons.jspf`, `workspace.jspf`; sửa `shop-start.jspf`, `shop-end.jspf`, `shop-tags.jspf`, `navigation.jspf` (cùng gốc webapp) |
| Assets | Tạo `assets/images/community-hero.svg`, sửa `product-placeholder.svg`; XML/Unicode entity bảo đảm chữ Việt không lỗi trong SVG |
| Catalog/home | `WEB-INF/views/home.jsp`, `catalog/list.jsp`, `cards.jspf`, `detail.jsp`, `reviews.jspf` |
| Commerce/account | `buyer/cart.jsp`, `checkout.jsp`, `receipt.jsp`, `auth/login.jsp`, `register.jsp`, `profile.jsp`, `seller/products.jsp`, `product-form.jsp`, `orders/list.jsp`, `detail.jsp` (cùng gốc views) |
| Reputation/admin/errors | `reviews/form.jsp`, `complaints/list.jsp`, `detail.jsp`, `admin/index.jsp`, `errors/message.jsp`; form complaint và fragment snapshot tiếp tục hưởng CSS/layout dùng chung |
| View data | `src/main/java/com/example/demo/service/ProductService.java`: dùng count hiện có một lần rồi đưa `resultCount` sang view; không đổi query/filter/luật nghiệp vụ |
| Docs | README, AGENTS, docs/README, kế hoạch; checklist 11, báo cáo này, ảnh `docs/screenshots/ui/` |

Không thêm dependency/frontend framework, không đổi DAO/transaction/schema, không tạo bộ test mới trong source. Không sửa `.idea/workspace.xml`. Thư mục chưa là Git repository, chưa commit.

## Kiểm tra thực tế

1. `.\mvnw.cmd -B verify`: BUILD SUCCESS, **22 unit test đạt**, tạo WAR.
2. Deploy Tomcat **10.1.48** riêng ở loopback 18080/context `/c2c`. MySQL **9.1.0** riêng ở 13316, schema **c2c_feedback_test** có từ M7–M8, khảo sát 23 bảng (22 nghiệp vụ + Flyway history). Backup `.local-test/ui-before-tests.sql` trước các thao tác ghi test. Không dùng WAMP 3306/database ứng dụng để ghi.
3. APP_CONFIG_FILE riêng, C2C_IT_ALLOWED=true và C2C_HTTP_BASE riêng; chạy `.\mvnw.cmd -B verify -Pmysql-it '-Dit.test=AccountHttpIT,ShopHttpIT,FeedbackHttpIT'`: **8 HTTP integration test đạt** (6 + 1 + 1), cùng 22 unit. Đã chạy lại sau thay đổi cuối trên WAR được redeploy: BUILD SUCCESS lúc **15:37:01**, không failure/error/skipped. Tổng lượt giao diện này có **30 test case Maven khác nhau đạt**; không đếm lần chạy lại thành test mới.
4. Công cụ CUA/node_repl gặp lỗi khởi tạo `failed to write kernel assets`; thay bằng **Chrome headless thật** qua Playwright có sẵn, dùng script tạm trong `.local-test/`, không cài hạ tầng mới.
5. Chrome kiểm tra **24 trang × 2 viewport (1440 và 390px) = 48 lượt**, HTTP 200, document.scrollWidth không vượt viewport, không ảnh hỏng hoặc JavaScript pageerror. Có ảnh menu mobile riêng. Các trang gồm home/catalog/empty/login/register/detail/cart/checkout/receipt/profile/buyer orders/complaints/form/detail/review/seller products/form/orders/Admin và các bảng/detail complaint.
6. Đã thao tác qua trình duyệt: tạo và Admin duyệt bốn fixture có minh họa, tìm bằng header → sản phẩm → phóng ảnh → thêm hai món → cart → COD checkout → receipt; seller xác nhận/giao, buyer hoàn thành → chọn radio 5 sao/gửi review → chi tiết sản phẩm có nhãn xác thực. Kiểm password toggle, preview file và menu mobile/Escape. Mọi ghi này chỉ ở schema test có backup.
7. Đã **xem trực tiếp ảnh chụp** desktop/mobile của home, detail, cart, checkout, review và Admin complaint/products. Phát hiện và sửa mã hóa SVG; bảng quản lý có nền/card và vùng cuộn riêng. Không dùng build để suy ra chất lượng hình thức.
8. Sau redeploy WAR cuối, chạy Chrome đọc thêm **16 trang × 2 viewport = 32 lượt**, HTTP 200 và không tràn ngang. Kiểm thêm USER bị từ chối `/admin` không hiện sidebar Admin; ảnh cố tình lỗi chuyển sang fallback; bảng mobile có scrollWidth lớn hơn clientWidth trong khối; login vẫn dùng được khi tắt JavaScript. Script lần đầu chờ sai redirect Admin (`/home` thay vì `/admin`) được sửa; đây là lỗi script kiểm tra, không phải lỗi đăng nhập ứng dụng.

Các log/script/results đầy đủ ở `.local-test/ui-*`, giữ ngoài commit. Ảnh chọn lọc dưới đây là ảnh render thật từ Tomcat/Chrome, với fixture giả; hình minh họa sản phẩm được đánh dấu, không phải ảnh thật. Những kết quả test từ lượt M7–M8 không được cộng vào số test của lượt giao diện.

## Ảnh bàn giao

| Trang | Desktop | Mobile |
| --- | --- | --- |
| Trang chủ (khung trình chiếu) | [1440px](../screenshots/ui/home-overview-1440.png) | [390px](../screenshots/ui/home-overview-390.png) |
| Chi tiết có review thật qua fixture order | [1440px](../screenshots/ui/product-reviewed-1440.png) | [390px](../screenshots/ui/product-reviewed-390.png) |
| Giỏ hàng | [1440px](../screenshots/ui/cart-1440.png) | [390px](../screenshots/ui/cart-390.png) |
| Checkout | [1440px](../screenshots/ui/checkout-1440.png) | [390px](../screenshots/ui/checkout-390.png) |
| Admin | [Tổng quan](../screenshots/ui/admin-1440.png) | [Menu tài khoản](../screenshots/ui/account-menu-390.png) |
| Tin chờ duyệt | [Bảng quản lý](../screenshots/ui/admin-products-1440.png) | Bảng có vùng cuộn riêng ở mobile |

## Bàn giao và giới hạn

Làm theo [build/redeploy và checklist 11](../11-checklist-giao-dien.md), dùng **Ctrl+F5** khi cache cũ. Không cần SQL/migration/seed. Tomcat ứng dụng của người dùng chưa được tự redeploy; cần restart/redeploy để thấy bản mới. Đã dừng MySQL/Tomcat test riêng sau xác minh; port 3306 và 8080 của ứng dụng vẫn đang lắng nghe, không thay cấu hình hoặc chủ động restart chúng.

Ảnh sản phẩm của bạn phụ thuộc upload hiện tại; không tự thêm ảnh vào dữ liệu người dùng. Chuẩn bị ảnh chụp đúng sản phẩm và duyệt lại tin khi sửa ảnh trước buổi demo. Bốn fixture minh họa trong ảnh báo cáo chỉ nằm ở test schema, không được seed vào ứng dụng.

Đã kiểm Chrome headless ở hai viewport, chưa kiểm điện thoại thật, Safari/Firefox hoặc mọi tổ hợp nội dung dài. Test scrollWidth không thay thế rà soát trực quan mọi hàng dữ liệu; bảng/menu dài cuộn trong vùng riêng. Form thanh toán giữ select COD/chuyển khoản hiện có; giỏ hiển thị tên seller từng dòng và vẫn tách đơn theo nghiệp vụ hiện có.

Commit đề xuất: `Hoàn thiện giao diện marketplace Chợ C2C cho desktop và mobile`.
