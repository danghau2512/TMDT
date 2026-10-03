# 05 — Kế hoạch triển khai và tiêu chí hoàn thành

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

**Thực hiện:** form register/login/profile/logout; Auth/ProfileServlet → UserService → UserDao/users; hash mật khẩu, validate/duplicate email, session/CSRF, auth/admin filters. Seed admin không có mật khẩu mặc định công khai trong source.

**Hoàn thành khi:** đăng ký và login đúng/sai có thông báo; DB không có plaintext; email trùng bị chặn cả hai request đồng thời; logout làm session mất hiệu lực; user sửa được hồ sơ mình nhưng không đổi role/status. USER vào admin bằng URL trực tiếp bị chặn. Thay role/buyer_id/user_id trong request không nâng quyền.

**Kiểm tra:** form thực tế, session fixation/logout, XSS dữ liệu hồ sơ; Service tests về đăng nhập/ownership; xác minh hash và unique email trên MySQL test. Tài khoản được dùng cả mua và bán ở các mốc sau.

## M3 — Đăng bán, quản lý và kiểm duyệt

**Thực hiện:** màn hình seller list/create/edit/hide/stock và admin list/create/edit/hide/approve/reject/feature; ProductServlet → ProductService → DAO products/images/moderation/stock/assets/audit. Upload ngoài webroot, tin mới chờ duyệt, cập nhật kho dưới khóa, phiên bản form.

**Hoàn thành khi:** seller tạo đủ tên/danh mục/giá/mô tả/condition/quantity/ảnh; chỉ quản lý tin mình; admin tạo thay seller ACTIVE và kiểm duyệt; sửa nội dung trả lại PENDING; tin ẩn không công khai. Mark “đã bán” làm khả dụng 0, không tạo flag lệch kho; điều chỉnh stock có nhật ký. Tin không có ảnh/giá không hợp lệ bị chặn; lỗi upload không để DB trỏ file thiếu. Seller không tự gửi APPROVED.

**Kiểm tra:** user B sửa ID tin A bị chặn; file giả ảnh/traversal/quá cỡ bị từ chối; edit form cũ không ghi đè; admin can thiệp có audit/reason. Kiểm duyệt và history nguyên tử.

## M4 — Trang chủ, danh mục, tìm kiếm và chi tiết

**Thực hiện:** JSP/JSTL + Bootstrap cơ bản; catalog Servlet/Service/DAO; category mới/nổi bật, filter keyword/category/min-max/condition, sort và phân trang; chi tiết ảnh/thông tin/contact công khai.

**Hoàn thành khi:** trang chỉ công khai tin PUBLIC/APPROVED và seller/category ACTIVE; sản phẩm mới/nổi bật theo quy tắc 02; lọc kết hợp đúng, không thấy tin bị ẩn/chưa duyệt; giá âm/min > max báo lỗi; trang rỗng/không tồn tại có thông báo. Tin hết hàng hiện nhãn và không mua được. Contact công khai không lộ email đăng nhập/address.

**Kiểm tra:** các tổ hợp filter, phân trang và HTTP bằng context `/demo` hoặc `/demo_war_exploded`; truy vấn keyword chứa dấu nháy không gây SQL injection; XSS title/description được escape, giao diện dùng được trên mobile cơ bản.

## M5 — Giỏ hàng và tạo đơn an toàn

**Thực hiện:** cart/checkout JSP, Servlet/Service/DAO, cart/items/batches/orders/items/images/payments/history/stock. Trang xem lại giá và nhận hàng; atomic split theo seller, snapshot, key chống lặp, trừ khả dụng và payment ban đầu ngay lúc tạo. Dùng dữ liệu test để xem trang kết quả/danh sách đơn ban đầu.

**Hoàn thành khi:** add/remove/update/tổng đúng; cấm tự mua; thay giá/qty vượt giới hạn trong request không gian lận; giỏ nhiều seller sinh đúng số đơn có mã riêng và tổng riêng. Một item thiếu kho → không có bất kỳ đơn mới nào của batch, giỏ/kho không đổi. Double submit/retry không tạo lại batch. Snapshot không đổi sau sửa/gỡ ảnh tin. Hai buyer mua sản phẩm cuối chỉ một thành công.

**Kiểm tra bắt buộc trên MySQL/InnoDB:** tồn cuối=1 với 2 transaction đồng thời; lỗi cưỡng bức ở seller thứ hai để kiểm rollback; mất response rồi retry cùng key; cùng key nhưng payload khác trả conflict; sửa listing/cart version giữa xem lại và submit. Payment/history ban đầu phải có trong cùng transaction.

**Phụ thuộc M6:** giao/nhận/hủy/xác nhận thanh toán sẽ hoàn thiện ở M6; không dùng lý do “làm kho ở mốc sau” để bỏ nguyên tử tạo đơn của M5.

## M6 — Xử lý đơn, thanh toán và hoàn kho

**Thực hiện:** buyer/seller/admin order list/detail, timeline, status actions; seller/admin xác nhận bank mô phỏng; buyer hủy/receive; Service transaction đồng bộ order/payment/stock/history/audit. Có form lý do can thiệp và nhãn mô phỏng.

**Hoàn thành khi:** toàn bộ chuyển ở 02 đúng vai trò/điều kiện; buyer chỉ thấy đơn mua, seller chỉ đơn bán; không SHIPPED trước CONFIRMED; bank chưa PAID không giao; COD được ghi PAID cùng DELIVERED. Chỉ buyer từ DELIVERED xác nhận COMPLETED; hủy trước giao hoàn kho/payment đúng, gửi hủy hai lần không hoàn hai lần. Từ SHIPPED không hủy trực tiếp. Admin không nhảy state tùy ý.

**Kiểm tra:** chuyển trái phép/đơn người khác qua URL/POST; đua CONFIRMED→SHIPPED và →CANCELLED; rollback lỗi history/payment/stock; hủy paid ghi REFUND_SIMULATED; không thu tiền hoặc hoàn tiền thật. Trước M8, phần guard khiếu nại có thể dùng DAO trên bảng chưa có hồ sơ; M8 kiểm tra tích hợp guard thực tế.

## M7 — Đánh giá theo đơn

**Thực hiện:** form review ở chi tiết đơn COMPLETED, ProductDetail/Seller rating display; ReviewServlet/Service/DAO/reviews; nhãn “Đã mua qua hệ thống”; admin ẩn nội dung không phù hợp có audit.

**Hoàn thành khi:** buyer đúng order/item được đăng sao sản phẩm/seller 1–5 một lần; chưa nhận/chưa COMPLETED bị chặn; seller/khách/admin không tạo thay buyer. Product/seller trung bình và số review chỉ tính VISIBLE hợp lệ. Nội dung review được escape, duplicate hai tab chỉ có một row.

**Kiểm tra:** fake order_id/item_id/buyer_id, duplicate submit và DB unique; review đơn cũ vẫn gắn đúng snapshot sau đổi tin/ẩn tin. Không gắn nhãn xác thực dựa trên form client.

## M8 — Khiếu nại và bằng chứng

**Thực hiện:** buyer tạo/xem/bổ sung, admin queue/detail/timeline/messages/process/resolve/reopen; ComplaintServlet/Service/DAO, storage và bảng liên quan; view đối chiếu order/snapshot/history/payment/evidence. Guard đơn có complaint mở không COMPLETED.

**Hoàn thành khi:** buyer mở được hồ sơ PENDING/SHIPPED/COMPLETED/CANCELLED của mình; item phải đúng đơn; một hồ sơ/order, nhiều bổ sung; tiến độ RECEIVED/PROCESSING/RESOLVED và phản hồi/kết luận được lưu. Mở lại giữ kết quả trước. Người ngoài/seller không đọc ảnh evidence qua ID trực tiếp. Không tự sửa kho/tiền khi đổi status complaint; resolution ORDER_CANCELLED dùng hủy hợp lệ cùng transaction.

**Kiểm tra:** đua complaint create và buyer receive dưới khóa order; upload hỏng/rollback; admin kết luận thiếu phản hồi/lý do bị chặn; mở lại sau RESOLVED; cập nhật tinh chỉnh tin không đổi evidence/snapshot. Không có hoàn tiền thật.

## M9 — Tích hợp, hướng dẫn và demo

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
