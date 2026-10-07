# 05 — Kế hoạch triển khai và tiêu chí hoàn thành

**Điều chỉnh xác minh QR 06/10/2026:** đã thay adapter FPT bằng ZXing 3.5.4, parser sáu trường từ bảy vị trí, đọc trước/sau/xoay, xử lý same/conflict/not found/unsupported và manual, UI/Admin/source/edit audit/nhãn mới. V007 backup/áp một lần, giữ hồ sơ/ảnh cũ, không API key. Build 34 unit, 4 SellerVerificationIT, 15 regression IT/HTTP, Chrome 16 nhóm và UI cuối 1366/390 đạt. **Chưa thử căn cước thật**, QR đã kiểm tra đều dữ liệu giả; người dùng thử hai ảnh thật trên máy theo [15](15-xac-minh-nguoi-ban.md). Rebuild/restart Tomcat, không chạy lại SQL. [Báo cáo](reports/Xac-minh-nguoi-ban-QR.md).

**Mở rộng chat được yêu cầu 05/10/2026:** hoàn thành UI/Servlet/Service/DAO/MySQL, V003, CSRF/ownership/read cursor, polling và hướng dẫn demo. Kiểm tra: 28 unit, 3 ChatIT, 1 ChatHttpIT và hai phiên Chrome thật 1440/390px trên schema test riêng. V003 đã áp vào standalone c2c_demo sau backup, đối chiếu 23 bảng cũ không đổi. Bước còn lại ở máy người dùng: rebuild artifact/restart Tomcat IntelliJ; chưa chạy chat trên dữ liệu ứng dụng. [Báo cáo](reports/Chat-mua-ban.md).

**Tiến độ mở rộng 05/10/2026:** triển khai checkout COD/VNPAY Sandbox, attempt theo order, IPN/Return/querydr, retry và refund pending; V002 đã kiểm tra trên MySQL riêng và áp lên c2c_demo sau backup. Đã xác nhận thật một success và một hủy qua querydr Sandbox; IPN public chưa kiểm. Theo dõi kiểm tra gateway/IPN thực tế trong [báo cáo VNPAY](reports/VNPAY-sandbox.md); kịch bản [demo 12](12-vnpay-sandbox.md). Không chuyển sang production/refund API.


Các mốc thực hiện **ở những lượt tiếp theo** theo yêu cầu người dùng; không lập trình toàn bộ ngay trong lượt thiết kế. Mỗi mốc phải đi xuyên suốt giao diện, Servlet, Service, DAO và schema liên quan. M1 là ngoại lệ hạ tầng: có trang kiểm tra tối thiểu thay cho tính năng thương mại.

## Điều kiện bắt đầu

- Xác định installation/version Tomcat thực tế và chọn bộ Tomcat 10.1/Servlet 6.0 như đề xuất hoặc đồng bộ phương án Tomcat 11.
- Có MySQL host/port/schema/user/credential qua cấu hình cục bộ/môi trường. Không gửi password vào docs; xác nhận dùng WAMP 9.1 hiện có hay schema trên MySQL 8.4 mục tiêu. Không chọn nhầm service MariaDB.
- Khi có database sẵn, khảo sát/sao lưu trước migration; chuẩn bị schema test riêng và thư mục upload ngoài WAR. Không cần chốt thương hiệu/ngành hàng để triển khai hàng hóa thông thường.
- Đọc các giả định 02 khi triển khai chức năng bị ảnh hưởng. Nếu người dùng chưa điều chỉnh, dùng đề xuất được ghi rõ; thay đổi quy tắc phải cập nhật 02/04 và test tương ứng.

## M1 — Cấu trúc, schema và JDBI

**Tiến độ 03/10/2026:** đã triển khai và kiểm tra trên môi trường riêng: 7 unit + 11 integration tests, MySQL 9.1.0 (22 bảng nghiệp vụ + Flyway history, 46 FK/52 CHECK), seed chạy lại, JDBI SELECT 1, Tomcat 10.1.48/HTTP/JSP tiếng Việt. [Báo cáo M1](reports/M1-nen-tang.md) và README có hướng dẫn. Chưa cấu hình c2c_demo trên WAMP 3306 vì thiếu credential dự án; chưa kiểm trên MySQL 8.4 thực tế. Không triển khai M2 trở đi.

**Thực hiện:** đồng bộ Servlet 6.0, pin compiler/surefire/dependencies; config/pool/JDBI/SqlObjectPlugin/lifecycle; migration có phiên bản và seed dữ liệu giả. Tạo schema tổng thể theo 04, rà PK/FK/CHECK/index. Bổ sung JSTL và layout JSP nền, trang kiểm tra kết nối chỉ ở dev/test (không lộ URL/credential). Tạo config mẫu và ignore secrets/upload. Chưa làm auth hay CRUD thương mại.

**Hoàn thành khi: .\mvnw.cmd -B verify chạy thành công; WAR deploy trên Tomcat mục tiêu; JSP hiển thị tiếng Việt; truy vấn DB bằng JDBI thành công và handle được đóng.** Migration trên schema test tạo đủ 22 bảng, ràng buộc invalid input bị từ chối; chạy lại migration runner không chạy V001 lần hai. Seed có category/user demo phù hợp, ghi rõ giả lập. Có hướng dẫn run và lỗi thiếu config dễ hiểu, không in secret.

**Kiểm tra:** build/tests, HTTP smoke, DB metadata/constraint trên môi trường riêng đã đạt; cấu hình DB chạy ứng dụng của người dùng còn cần thực hiện theo README trước mốc nghiệp vụ. Không nhận build đơn thuần làm bằng chứng kết nối; chưa chuyển M2 trong lượt này.

## M2 — Tài khoản và phân quyền

**Tiến độ 04/10/2026:** đã hoàn thành tài khoản và phân quyền, 20 unit + 24 integration/HTTP tests đạt trên MySQL/Tomcat test riêng; xem [báo cáo M2](reports/M2-tai-khoan.md). Database người dùng và V001 được giữ nguyên; không cần migration bổ sung. HTTP test đã kiểm đăng ký/đăng nhập/profile/logout, seed USER/ADMIN, session fixation/replay, CSRF, XSS và giả mạo role/ID. Chưa triển khai M3.

**Thực hiện:** form register/login/profile/logout; Register/Login/Logout/ProfileServlet → UserService → UserDao/users; BCrypt 2b/cost12, validate/duplicate email, session/CSRF, AccessFilter kiểm đăng nhập/admin. Seed admin dùng mật khẩu fixture công khai đã ghi README từ M1, chỉ dành cho demo; không phải credential production. Điều chỉnh câu cũ về seed để thống nhất với dữ liệu demo đã triển khai.

**Hoàn thành khi:** đăng ký và login đúng/sai có thông báo; DB không có plaintext; email trùng bị chặn cả hai request đồng thời; logout làm session mất hiệu lực; user sửa được hồ sơ mình nhưng không đổi role/status. USER vào admin bằng URL trực tiếp bị chặn. Thay role/buyer_id/user_id trong request không nâng quyền.

**Kiểm tra:** form thực tế, session fixation/logout, XSS dữ liệu hồ sơ; Service tests về đăng nhập/ownership; xác minh hash và unique email trên MySQL test. Tài khoản được dùng cả mua và bán ở các mốc sau.

## M3 — Đăng bán, quản lý và kiểm duyệt

**Tiến độ 04/10/2026:** đã có UI/Servlet/Service/DAO tạo, sửa, ẩn, cập nhật kho, ảnh và admin duyệt/từ chối có lý do/audit. Tin sửa nội dung chờ duyệt lại. Theo yêu cầu lượt này, thiếu ảnh dùng ảnh mặc định; không triển khai gắn “nổi bật”. Xem [báo cáo M3–M6](reports/M3-M6-mua-ban.md).

**Thực hiện:** màn hình seller list/create/edit/hide/stock và admin list/create/edit/hide/approve/reject; ProductServlet → ProductService → DAO products/images/moderation/stock/assets/audit. Upload ngoài webroot, tin mới chờ duyệt, cập nhật kho dưới khóa, phiên bản form.

**Hoàn thành khi:** seller tạo đủ tên/danh mục/giá/mô tả/condition/quantity, ảnh tùy chọn; chỉ quản lý tin mình; admin tạo thay seller ACTIVE và kiểm duyệt; sửa nội dung trả lại PENDING; tin ẩn không công khai. Mark “đã bán” làm khả dụng 0, không tạo flag lệch kho; điều chỉnh stock có nhật ký. Giá không hợp lệ bị chặn; ảnh thiếu dùng ảnh mặc định, file ảnh không hợp lệ bị từ chối. Seller không tự gửi APPROVED.

**Kiểm tra:** user B sửa ID tin A bị chặn; file giả ảnh/traversal/quá cỡ bị từ chối; edit form cũ không ghi đè; admin can thiệp có audit/reason. Kiểm duyệt và history nguyên tử.

## M4 — Trang chủ, danh mục, tìm kiếm và chi tiết

**Tiến độ 04/10/2026:** đã có trang chủ mua bán, danh mục, từ khóa/khoảng giá/tình trạng, phân trang 12 tin và chi tiết/contact công khai. Có giao diện rỗng/hết hàng; chỉ PUBLIC/APPROVED của seller/category ACTIVE. Chưa có mục nổi bật hoặc lựa chọn sắp xếp, không nằm trong yêu cầu lượt demo này.

**Thực hiện:** JSP/JSTL + Bootstrap cơ bản; catalog Servlet/Service/DAO; danh mục và tin mới, filter keyword/category/min-max/condition và phân trang; chi tiết ảnh/thông tin/contact công khai.

**Hoàn thành khi:** trang chỉ công khai tin PUBLIC/APPROVED và seller/category ACTIVE; sản phẩm mới sắp theo thời gian; lọc kết hợp đúng, không thấy tin bị ẩn/chưa duyệt; giá âm/min > max báo lỗi; trang rỗng/không tồn tại có thông báo. Tin hết hàng hiện nhãn và không mua được. Contact công khai không lộ email đăng nhập/address.

**Kiểm tra:** các tổ hợp filter, phân trang và HTTP bằng context `/demo` hoặc `/demo_war_exploded`; truy vấn keyword chứa dấu nháy không gây SQL injection; XSS title/description được escape, giao diện dùng được trên mobile cơ bản.

## M5 — Giỏ hàng và tạo đơn an toàn

**Tiến độ 04/10/2026:** đã có thêm/sửa/xóa giỏ, xem lại giá, checkout, nhóm theo seller, snapshot và chống gửi lặp. Đã chạy checkout hai seller, cạnh tranh tồn cuối bằng hai transaction, lỗi cưỡng bức payment của seller thứ hai để chứng minh rollback. Ghi chú nhận hàng nằm ở history khởi tạo; không cần migration.

**Thực hiện:** cart/checkout JSP, Servlet/Service/DAO, cart/items/batches/orders/items/images/payments/history/stock. Trang xem lại giá và nhận hàng; atomic split theo seller, snapshot, key chống lặp, trừ khả dụng và payment ban đầu ngay lúc tạo. Dùng dữ liệu test để xem trang kết quả/danh sách đơn ban đầu.

**Hoàn thành khi:** add/remove/update/tổng đúng; cấm tự mua; thay giá/qty vượt giới hạn trong request không gian lận; giỏ nhiều seller sinh đúng số đơn có mã riêng và tổng riêng. Một item thiếu kho → không có bất kỳ đơn mới nào của batch, giỏ/kho không đổi. Double submit/retry không tạo lại batch. Snapshot không đổi sau sửa/gỡ ảnh tin. Hai buyer mua sản phẩm cuối chỉ một thành công.

**Kiểm tra bắt buộc trên MySQL/InnoDB:** tồn cuối=1 với 2 transaction đồng thời; lỗi cưỡng bức ở seller thứ hai để kiểm rollback; mất response rồi retry cùng key; cùng key nhưng payload khác trả conflict; sửa listing/cart version giữa xem lại và submit. Payment/history ban đầu phải có trong cùng transaction.

**Phụ thuộc M6:** giao/nhận/hủy/xác nhận thanh toán sẽ hoàn thiện ở M6; không dùng lý do “làm kho ở mốc sau” để bỏ nguyên tử tạo đơn của M5.

## M6 — Xử lý đơn, thanh toán và hoàn kho

**Tiến độ 04/10/2026:** đã có danh sách/chi tiết/nhật ký/thao tác buyer/seller/admin, chuyển khoản mô phỏng qua nút PAY và COD ghi PAID khi DELIVERED. Đã kiểm hoàn thành, hủy paid/refund và COD/void, hủy/PAY/checkout lặp. Buyer được bấm PAY theo yêu cầu demo mới, Service quyết định trạng thái. Chưa stress toàn bộ cặp thao tác hủy/giao đồng thời; M7/M8 vẫn chưa triển khai.

**Thực hiện:** buyer/seller/admin order list/detail, timeline, status actions; buyer/seller/admin thanh toán bank mô phỏng đúng đơn; buyer hủy/receive; Service transaction đồng bộ order/payment/stock/history/audit. Có form lý do can thiệp và nhãn mô phỏng.

**Hoàn thành khi:** toàn bộ chuyển ở 02 đúng vai trò/điều kiện; buyer chỉ thấy đơn mua, seller chỉ đơn bán; không SHIPPED trước CONFIRMED; bank chưa PAID không giao; COD được ghi PAID cùng DELIVERED. Chỉ buyer từ DELIVERED xác nhận COMPLETED; hủy trước giao hoàn kho/payment đúng, gửi hủy hai lần không hoàn hai lần. Từ SHIPPED không hủy trực tiếp. Admin không nhảy state tùy ý.

**Kiểm tra:** chuyển trái phép/đơn người khác qua URL/POST; đua CONFIRMED→SHIPPED và →CANCELLED; rollback lỗi history/payment/stock; hủy paid ghi REFUND_SIMULATED; không thu tiền hoặc hoàn tiền thật. Trước M8, phần guard khiếu nại có thể dùng DAO trên bảng chưa có hồ sơ; M8 kiểm tra tích hợp guard thực tế.

## M7 — Đánh giá theo đơn

**Tiến độ 04/10/2026:** đã triển khai form số sao/nhận xét từ dòng đơn COMPLETED, buyer ownership, kiểm trùng + UNIQUE; hiển thị nội dung đã gửi và trung bình/list review VISIBLE trên chi tiết sản phẩm với nhãn xác thực. Theo phạm vi mới: một điểm chung, chưa có UI điểm seller riêng hay admin ẩn/sửa/xóa/phản hồi review. Xem [báo cáo M7–M8](reports/M7-M8-danh-gia-khieu-nai.md).

**Thực hiện:** form review ở chi tiết đơn COMPLETED, ProductDetail rating display; ReviewServlet/Service/DAO/reviews; nhãn “Đã mua qua hệ thống”. Các mở rộng điểm seller riêng/ẩn review ngoài phạm vi lượt hiện tại.

**Hoàn thành khi:** buyer đúng order/item được đăng số sao chung 1–5 một lần; chưa COMPLETED bị chặn; seller/khách/admin không tạo thay buyer. Trung bình sản phẩm và số review chỉ tính VISIBLE hợp lệ. Nội dung review được escape, gửi lại không tạo row mới.

**Kiểm tra:** fake order_id/item_id/buyer_id, duplicate submit và DB unique; review đơn cũ vẫn gắn đúng snapshot sau đổi tin/ẩn tin. Không gắn nhãn xác thực dựa trên form client.

## M8 — Khiếu nại và bằng chứng

**Tiến độ 04/10/2026:** đã có buyer tạo/list/detail/bổ sung, admin filter/detail/respond/process/resolve/reopen, snapshot/timelines và evidence riêng tư. Một hồ sơ/order, duplicate dẫn về hồ sơ cũ; đóng/mở không đổi order/payment/kho. Đã kiểm guard complete và cuộc đua create/complete trên MySQL riêng; giữ COMPLETED khi khiếu nại mở sau đó. Không cần migration mới. [Demo thêm 2–3 phút](10-demo-danh-gia-khieu-nai.md).

**Thực hiện:** buyer tạo/xem/bổ sung, admin queue/detail/timeline/messages/process/resolve/reopen; ComplaintServlet/Service/DAO, storage và bảng liên quan; view đối chiếu order/snapshot/history/payment/evidence. Guard đơn có complaint mở không COMPLETED.

**Hoàn thành khi:** buyer mở được hồ sơ ở mọi trạng thái đơn của mình; một hồ sơ/order phản ánh cả đơn, nhiều bổ sung; tiến độ RECEIVED/PROCESSING/RESOLVED và phản hồi/kết luận được lưu. Mở lại giữ kết quả trước. Người ngoài/seller không đọc ảnh evidence qua ID trực tiếp. Không tự sửa kho/tiền khi đổi status complaint; ORDER_CANCELLED chỉ ghi nhận đơn đã hủy hợp lệ qua luồng đơn hàng.

**Kiểm tra:** đua complaint create và buyer receive dưới khóa order; upload hỏng/rollback; admin kết luận thiếu phản hồi/lý do bị chặn; mở lại sau RESOLVED; cập nhật tinh chỉnh tin không đổi evidence/snapshot. Không có hoàn tiền thật.

## M9 — Tích hợp, hướng dẫn và demo

**Tiến độ giao diện 04/10/2026:** đã triển khai shell marketplace, home/catalog/detail, form tài khoản/seller, cart/checkout/receipt, đơn/review/complaint và bảng/sidebar Admin. Đã chụp/xem ảnh Chrome thật ở 1440/390px và chạy luồng form trên schema test riêng; xem [báo cáo UI](reports/UI-hoan-thien-giao-dien.md) và [checklist 11](11-checklist-giao-dien.md). Không sửa schema/dữ liệu ứng dụng. Phần giao diện đã bàn giao; các tiêu chí rehearsal/migration/toàn bộ M9 bên dưới không được xem là hoàn thành chỉ bởi lượt làm UI.

**Thực hiện:** chạy toàn hành trình với dữ liệu giả gồm admin, ít nhất hai seller, hai buyer; gom fix, README run/config/demo, seed không phá dữ liệu, ảnh/timeline/bằng chứng để trình bày. Hoàn thiện trang lỗi, trạng thái rỗng, mobile cơ bản.

**Hoàn thành khi:** clean build + deploy + kiểm thử tích hợp đúng stack; checklist an toàn quyền server/CSRF/XSS/upload/SQL bind; migration từ schema test trống và rehearsal có dữ liệu không mất đơn/ảnh; không còn lỗi blocker; các hạn chế MVP được ghi rõ. Không báo “đã test đồng thời” nếu chỉ click tuần tự.

Kịch bản trình bày gợi ý:

1. Seller đăng hai sản phẩm; admin duyệt; buyer tìm/lọc xem contact.
2. Buyer thêm hàng của hai seller và checkout: chứng minh hai order code, hai payment, tổng tính server.
3. Seller đổi tên/giá/ảnh của một tin; mở đơn cũ chứng minh snapshot vẫn nguyên.
4. Hủy một đơn trước giao; xem stock release/history/payment void hoặc mô phỏng refund; thử hủy lại.
5. Đơn còn lại: confirm → bank xác nhận hoặc COD → ship → deliver; buyer nhận → complete → review xác thực.
6. Một đơn khác đang giao bị phản ánh; buyer gửi ảnh, admin đối chiếu snapshot/timeline, phản hồi và giải quyết; buyer thấy tiến độ.
7. Trình bày bằng kết quả test tồn cuối/rollback/ownership vì thao tác UI một người không đủ chứng minh an toàn đồng thời.

## Báo cáo sau mỗi mốc

Mỗi báo cáo trong `docs/reports/` (tạo khi triển khai) và trả lời chat phải có:

| Nội dung | Yêu cầu bằng chứng |
| --- | --- |
| File thay đổi | Đường dẫn + mục đích; phân biệt source/migration/config/test/docs |
| Cách chạy | JDK/Tomcat/DB, tên biến môi trường, wrapper command, deploy/context và tài khoản dữ liệu giả; không password thật |
| Cách kiểm tra | Từng luồng thao tác, input, expected result |
| Kết quả thực tế | Lệnh đã chạy, PASS/FAIL, số test khi có; chưa chạy ghi rõ lý do; không biến checklist dự kiến thành kết quả |
| Vấn đề còn tồn tại | Lỗi/giới hạn/dependency chưa đạt, ảnh hưởng tới mốc sau |
| Giải thích để bảo vệ đồ án | HTTP đi qua Servlet → Service → DAO → MySQL; transaction/quyền/snapshot giải quyết vấn đề gì trong use case cụ thể |

Không ước lượng ngày hoàn thành khi chưa có lịch nhóm/điều kiện DB. Các mốc theo phụ thuộc, có thể chia nhỏ một mốc theo use case nhưng mỗi phần vẫn cần chạy được xuyên suốt.

## Mở rộng trải nghiệm đã triển khai — 05/10/2026

Bốn nâng cấp đã có UI → Servlet → Service → DAO → MySQL: trung tâm khiếu nại với thống kê, tìm mã đơn, tiến độ và lịch sử người thực hiện; đánh giá với 0–3 ảnh, phân bố sao, lọc và phân trang; khai báo hàng cũ, ảnh khuyết điểm và snapshot bất biến; so sánh 2–3 sản phẩm công khai cùng danh mục với cart/chat. V004 bổ sung cấu trúc, c2c_demo đã backup và áp một lần; dữ liệu cũ giữ nguyên, không ghi demo.

Đã chạy verify: 28 unit/WAR, UpgradesIT: 3 PASS trên MySQL/InnoDB cổng 13316, schema c2c_upgrades_test, FeedbackHttpIT: 1 PASS trên Tomcat 18080 có guard cùng DB; Chrome thật, 3 contexts, 7 nhóm luồng PASS và xem PNG 1366/390. Chưa kiểm tải hoặc exhaustive, chưa tự redeploy Tomcat IDE 8080; người dùng rebuild artifact rồi Stop/Run. Không kiểm VNPAY mới trong lượt này. Xem [demo](14-demo-bon-nang-cap.md) và [báo cáo](reports/Bon-nang-cap-trai-nghiem.md).

## Nhận diện TraoTay và tiếng Việt — 05/10/2026

Đã cập nhật logo gốc/header/footer/auth/title, khẩu hiệu, typography local và bố cục nút mật khẩu/menu mobile. Giữ UTF-8 ở JSP/Filter/JSON/DefaultServlet, thêm default request/response encoding và đổi version asset để nhận bản mới. Thông báo mạng lỗi chat nay bằng tiếng Việt; không đổi nghiệp vụ hoặc database.

Build cuối 28 unit PASS/WAR; Tomcat 10.1.48 + Chrome thật 1366/390 trên schema c2c_upgrades_test riêng có backup và guard. Hành trình đầu 46 lượt mở trang, sau chỉnh menu kiểm lại shell cuối/giỏ có dữ liệu/modal/UTF-8 và chụp auth sau tương tác. Đã xem ảnh đại diện, không báo exhaustive hay kiểm VNPAY. Đầu lượt 8080 trả UTF-8 đúng và Hiện/Ẩn đúng, chưa tái hiện lỗi ảnh cũ; chỉ đọc 8080, không dừng tiến trình IDE. Người dùng Rebuild artifact rồi Stop/Run Tomcat, Ctrl+F5. Xem [báo cáo, file và ảnh](reports/TraoTay-tieng-Viet.md).
