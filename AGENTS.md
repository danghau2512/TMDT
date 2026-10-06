# Quy ước làm việc — Đồ án TMĐT nhóm 6

## Phạm vi và nguồn yêu cầu

- Dự án website C2C kết nối người mua và người bán. Một USER có thể vừa mua vừa bán; ADMIN quản lý nền tảng.
- Đọc `docs/README.md`, yêu cầu và thiết kế liên quan trước khi sửa. Yêu cầu gốc ở `docs/00-yeu-cau-goc.txt`.
- Không triển khai AI/MLOps, thanh toán production, hoàn tiền thật hoặc chức năng ngoài đồ án. VNPAY Sandbox được người dùng yêu cầu ngày 05/10/2026; chỉ TEST.
- Lượt khảo sát đầu chỉ tạo tài liệu; các lượt tiếp theo triển khai theo `docs/05-ke-hoach-trien-khai.md` và chỉ dẫn mới của người dùng.
- Không chạy SQL làm thay đổi database thật trong lượt thiết kế. Không suy đoán rằng database ngoài repository trống. Các lượt triển khai phải khảo sát schema, sao lưu dữ liệu liên quan và viết migration có phiên bản trước khi thay đổi.

## Công nghệ và cấu trúc

- Java 17, Maven Wrapper, WAR; Java Servlet + MySQL + JDBI; JSP/JSTL, HTML/CSS/JS và Bootstrap cơ bản.
- Giữ gốc package `com.example.demo`. Dùng package chữ thường `controller`, `service`, `dao`, `model`; đây là cách viết Java của các tầng Controller/Service/DAO/Model.
- Chỉ dùng `jakarta.servlet`, không trộn `javax.servlet`. M1 đã đồng bộ Tomcat 10.1.x + Servlet API 6.0.0 + web.xml 6.0, Java 17.
- Controller nhận HTTP, kiểm tra đầu vào, gọi Service, forward JSP/redirect. Service sở hữu nghiệp vụ và transaction. DAO chỉ truy vấn, không chứa luật nghiệp vụ. JSP không gọi DAO và không viết SQL/scriptlet nghiệp vụ.
- Không giữ JDBI Handle trong biến static/Servlet; các DAO trong một transaction cùng attach vào Handle do Service mở. Bind tham số; tên cột/sắp xếp động phải thuộc allowlist.
- Cấu hình DB/storage qua biến môi trường hoặc cấu hình cục bộ không commit; không ghi mật khẩu, dữ liệu cá nhân hay session token vào log/tài liệu. Không hard-code URL DB và credential trong Service.
- Đặt tên lớp Java PascalCase, biến/phương thức camelCase, bảng/cột snake_case. Mã trạng thái tiếng Anh theo thiết kế; nhãn giao diện và tài liệu tiếng Việt. Chú thích ngắn lý do của luật quan trọng.

## Bất biến nghiệp vụ

- Service kiểm tra quyền và ownership từ tài khoản đăng nhập cho mọi đọc/ghi riêng tư; không tin buyer_id/seller_id/role từ request.
- Giá và tổng tiền tính phía server bằng BigDecimal, lưu DECIMAL; cấm tự mua, số lượng âm và đặt hàng vượt tồn.
- Một đơn chỉ có một người bán. Một checkout nhiều người bán tạo nhiều đơn trong cùng transaction; lỗi một phần rollback toàn bộ.
- Tạo/hủy đơn, tồn kho, thanh toán và lịch sử liên quan phải nguyên tử; request lặp không tạo đơn/trừ hoặc hoàn kho lần hai.
- Không xóa cứng user/product/order/snapshot/lịch sử/bằng chứng đã liên quan giao dịch; ẩn, khóa hoặc ngừng hoạt động. Ảnh có snapshot phải giữ bytes bất biến.
- Phân biệt trạng thái đơn, thanh toán, khiếu nại và kiểm duyệt. Không cho ADMIN tùy ý nhảy trạng thái hoặc sửa snapshot.
- Review chỉ từ người mua của đơn COMPLETED, mỗi order_item một lần; nhãn “Đã mua qua hệ thống” do server xác định.
- Khiếu nại gắn đơn, cho phép cả giao dịch chưa hoàn thành; bằng chứng chỉ người có quyền được tải.
- Băm mật khẩu bằng thư viện chuyên dụng; CSRF cho request thay đổi, escape nội dung người dùng, kiểm tra upload và đổi session khi đăng nhập.

## Quy trình và kiểm tra

- Hoàn thành từng mốc xuyên suốt UI → Controller → Service → DAO → database; không chuyển mốc khi tiêu chí còn thiếu mà không báo rõ.
- Ưu tiên giao diện có sẵn; các trang JSP/JSTL dùng shell marketplace, CSS/JS và assets cục bộ, không thêm CDN/framework frontend chỉ để trang trí.
- Dùng `./mvnw.cmd -B verify` trên Windows (hoặc `./mvnw -B verify` trên Unix). Build thành công không đồng nghĩa test nghiệp vụ hay deploy thành công.
- Test phù hợp với thay đổi; transaction/cạnh tranh tồn kho phải thử trên MySQL/InnoDB thật trong schema test riêng, không dùng dữ liệu thật. Không viết test chỉ lặp lại implementation.
- Không sửa `.idea/workspace.xml` để chia sẻ cấu hình riêng của máy. Không commit target, file upload thật hay secrets. Thư mục này hiện chưa là Git repository; không tự khẳng định đã commit/diff.
- Sau mỗi phần báo file thay đổi, lệnh và thao tác kiểm tra, kết quả thực tế, giới hạn còn lại và giải thích nghiệp vụ để trình bày với giáo viên. Cập nhật tài liệu khi đổi quyết định.
- Các giả định ở `docs/02-yeu-cau-va-quy-tac.md` là đề xuất thiết kế, không mô tả tính năng đã có. Nếu người dùng điều chỉnh, cập nhật đồng bộ schema, quy tắc và kế hoạch.
- M1 đã có 22 bảng nghiệp vụ + Flyway history, không tự migrate/seed khi deploy. V001 đã áp không sửa; thêm migration có phiên bản. schema.sql chỉ cho schema trống; DB có dữ liệu phải khảo sát/backup và mapping trước.
- Seed chỉ là fixture phát triển với password demo công khai theo README; không dùng trên production. PasswordHasher BCrypt 2b/cost 12 đã chuẩn bị ở M1 để seed và M2 dùng chung; không phải chức năng đăng nhập đã triển khai.
- Profile `mysql-it` chỉ dùng schema hậu tố `_test` và `C2C_IT_ALLOWED=true`; cấu hình APP_CONFIG_FILE riêng. File local/credential và `.local-test/` phải giữ ngoài commit. M1 không xác minh credential WAMP của người dùng.
- M2 đã có đăng ký/email login/logout/profile, UserService/UserDao, AccessFilter và CSRF. Session CurrentUser không chứa email/hash/mật khẩu; Filter refresh ACTIVE/role từ DB. Email readonly trong hồ sơ, DTO không nhận ID/role/status. C2C_HTTP_BASE chỉ trỏ Tomcat loopback deploy cùng cấu hình schema test; không chạy kiểm thử ghi trên DB ứng dụng.
- M3–M6 đã có luồng mua bán demo qua ProductServlet/ProductService, CartCheckoutServlet/CartService/OrderService, OrdersServlet và MediaServlet. Báo cáo docs/reports/M3-M6-mua-ban.md; M7/M8 cập nhật bên dưới. Không tự seed, approve hoặc thay schema khi deploy.
- UPLOAD_ROOT/upload.root tuyệt đối ngoài WAR; mặc định user.home/.c2c-demo/uploads. Tin 0–5 ảnh, thiếu dùng ảnh mặc định; snapshot dùng asset bất biến. Buyer được PAY chuyển khoản mô phỏng đúng đơn; COD PAID cùng DELIVERED. Không nhận trạng thái tùy ý từ request.
- CommerceIT và ShopHttpIT chạy riêng bằng -Dit.test trên schema fixture *_test có backup. mysql-it mặc định giữ bộ M1/M2; bộ đó giả định 4 sản phẩm seed nên không chạy sau fixture thương mại trên cùng schema. Không dùng DB ứng dụng cho test ghi.
- M7/M8 đã có ReviewServlet/ReviewService/ReviewDao, ComplaintServlet/ComplaintService/ComplaintDao và evidence MediaServlet. Báo cáo hiện tại docs/reports/M7-M8-danh-gia-khieu-nai.md; kịch bản docs/10-demo-danh-gia-khieu-nai.md. Một điểm chung lưu cả hai cột rating của V001; chưa có review edit/delete/moderation hay điểm seller riêng trên UI.
- Complaint một hồ sơ/order kể cả đã xử lý; giữ UNIQUE. Tạo/bổ sung/PROCESS/RESOLVE/REOPEN khóa actor → order → complaint. RECEIVED/PROCESSING chặn COMPLETE; đóng hồ sơ không tự complete/cancel/refund. ORDER_CANCELLED chỉ ghi nhận order đã CANCELLED. Evidence chỉ buyer/Admin, không public catalog. 5 ảnh/lần, 20 ảnh/hồ sơ, nội dung 5.000 ký tự.
- ReputationIT và FeedbackHttpIT là kiểm tra M7/M8 ngắn, chạy riêng trên schema *_test; HTTP dùng guard cùng DB test hiện có. Không dùng dữ liệu ứng dụng, không báo kiểm trình duyệt nếu chỉ chạy HTTP.
- Giao diện đã có brand.jspf, icons.jspf, workspace.jspf, startup.css và marketplace.js dùng chung; tên thương hiệu ở brand.jspf. JS chỉ progressive enhancement, không quyết giá/quyền/trạng thái hoặc tự POST. Báo cáo docs/reports/UI-hoan-thien-giao-dien.md; checklist docs/11-checklist-giao-dien.md. Screenshot fixture ở docs/screenshots/ui, không phải dữ liệu ứng dụng. Giữ ảnh sản phẩm từ storage, không tự thay bằng ảnh trang trí.

- VNPAY Sandbox thay phương thức chuyển khoản cho checkout mới; giữ COD và BANK_TRANSFER_SIMULATED cũ. V002 thêm vnpay_attempts + REFUND_PENDING + history SYSTEM. Không sửa V001/schema.sql; không tự migrate/seed khi deploy.
- Return chỉ checksum/merchant + đọc DB; chỉ exact GET /payments/vnpay/ipn miễn session/CSRF. Create/query buyer-only + CSRF; querydr ngoài Handle, response có checksum trước xác nhận, API00 và TxnStatus00 mới PAID. Ký querydr pipe theo tài liệu, không canonical URL.
- Canceled late success và hủy VNPAY PAID cần REFUND_PENDING, không REFUND_SIMULATED hay khôi phục kho. Một pending attempt/order, reuse URL khi bấm lặp, retry failed/expired giữ lịch sử. IPN/query dùng chung apply transaction.
- c2c_demo hiện nhập standalone V001, không Flyway history; đã backup + áp V002 thủ công ngày 05/10/2026, không SOURCE lại hoặc auto baseline. Report docs/reports/VNPAY-sandbox.md; hướng dẫn docs/12-vnpay-sandbox.md. Test ghi chỉ c2c_vnpay_test trên instance riêng13316, không DB ứng dụng. Secrets backend local ignored, không log hash/request/querydr body.

- Bốn nâng cấp trải nghiệm đã có (05/10/2026): Complaint center/cards/stats/order search/actor history; review0–3 ảnh JPEG/PNG 5 MB, public filter8/trang; ConditionForm khai báo hàng cũ nullable + defect flags và checkout snapshot; CompareServlet public2–3cùng danh mục, localStorageIDs only/cart/chat giữ CSRF. Không gán mặc định không lỗi cho tin/đơn cũ. Điểm seller là seller_rating chung từ các review công khai hợp lệ, không thang uy tín độc lập.
- V004 thêm review_images và nullable declaration/flag snapshots; giữ V001–V003/schema.sql. c2c_demo standalone V003 đã full backup `.local-test/c2c-demo-before-upgrades-20261005.sql` và áp V004 một lần, 25 bảng cũ giữ nguyên, review_images trống, không seed/fixture ứng dụng. Hiện 26 bảng nghiệp vụ. Hướng dẫn docs/14-demo-bon-nang-cap.md và report docs/reports/Bon-nang-cap-trai-nghiem.md. Test ghi riêng c2c_upgrades_test/13316 + backup; UpgradesIT/FeedbackHttpIT/Chromecontexts, không chạy bộ M1 seed assumptions sau fixtures trên cùng schema.

- Nhận diện hiện tại là TraoTay, khẩu hiệu “Đồ cũ, giá trị mới.” (05/10/2026). Logo người dùng assets/images/Logo.png chứa sẵn chữ, giữ bytes/tỷ lệ/transparency; brand.jspf + c:url dùng chung, không thêm chữ cạnh ảnh. traotay.css giữ Segoe UI/fallback local, không đổi font icon. Shell CSS/JS version 20261005-traotay1; EncodingFilter trước AccessFilter, DefaultServlet fileEncoding và default request/response UTF-8. Lượt UI không cần migration/seed; report docs/reports/TraoTay-tieng-Viet.md, ảnh test docs/screenshots/traotay. Không tái hiện lỗi Hiện cũ trên 8080 đầu lượt; không quy lỗi DB hoặc sửa dữ liệu khi chưa xác minh.
- Gợi ý tìm kiếm header đã có (05/10/2026): GET /products/suggestions → ProductService → CatalogDao PUBLIC/JDBI bind, tối đa 8, từ khóa 100 ký tự, DTO ProductSuggestion 5 field công khai/ID string. search-suggestions.js debounce250ms/cancel/keyboard/textContent và search-suggestions.css local version search1. Không migration, không thay tìm kiếm thường; báo cáo docs/reports/Tim-kiem-goi-y.md. Test chỉ đọc c2c_upgrades_test/13316, Chrome1366/390; không kiểm tải lớn.
- Ảnh sản phẩm home đã đổi (05/10/2026): wrapper .home-products, khung vuông contain/padding, 3 cột desktop/2 tablet/1 <=480px. Chỉ scope home, không đổi catalog/ảnh gốc. traotay.css version 20261005-home-images1. Build28 unit PASS, Chrome đọc 8080 1366/900/390 không POST; preview ảnh thật giữ .local-test ignored, report docs/reports/Anh-san-pham-trang-chu.md.
- Giỏ hàng AJAX đã có (05/10/2026): cart.jsp + cart-ajax.js/css local version cart1; nút −/+, nhập/Enter/rời ô và xóa không reload. CartCheckoutServlet trả CartResponse allowlist, CartService.changeAndView snapshot cùng transaction; giữ ownership/ACTIVE/CSRF/stock/BigDecimal. GET JSON đọc lại khi POST mất response, khóa sửa/checkout nếu chưa đồng bộ; HTML fallback vẫn redirect. Không migration/ghi DB ứng dụng. Report docs/reports/Gio-hang-AJAX.md, ảnh fixture docs/screenshots/cart-ajax; 28 unit và 19 kiểm tra tập trung Chrome/MySQL test đạt, không full regression.
