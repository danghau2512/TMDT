# 10 — Demo bổ sung đánh giá và khiếu nại (2–3 phút)

## Chuẩn bị

Build `.\mvnw.cmd -B verify`, redeploy WAR và restart Tomcat 10.1 dùng cấu hình/DB/storage M3 đã có. **Không cần migration hoặc seed mới.** Mở phiên buyer1, seller1, admin riêng theo [demo mua bán](09-demo-mua-ban.md). Password fixture `C2cDemo!2026` nếu chưa đổi; giữ password hiện tại nếu đã thay.

Chuẩn bị một đơn COMPLETED chưa review và một đơn PENDING/SHIPPED/DELIVERED của buyer1. Sản phẩm phải PUBLIC/APPROVED nếu muốn xem review trên catalog. Chuẩn bị một JPEG/PNG nhỏ làm evidence. Với case chặn hoàn thành, seller đưa đơn thứ hai tới DELIVERED bằng luồng giao hiện có; COD tự PAID lúc giao. Dùng hai đơn khác nhau để trình bày rõ hai thời điểm.

## Kịch bản

1. **0:00–0:45 — Đánh giá:** buyer mở Đơn mua → chi tiết đơn COMPLETED → Đánh giá dưới sản phẩm. Chọn 5 sao và nhận xét, gửi. Chi tiết đơn hiện nội dung đã gửi; mở chi tiết sản phẩm công khai thấy số sao trung bình, số lượt, tên/thời gian và nhãn **Đã mua qua hệ thống**. Một dòng đơn chỉ gửi một lần; đơn khác cho cùng sản phẩm vẫn có thể review khi hoàn thành.
2. **0:45–1:30 — Khiếu nại:** buyer mở đơn chưa hoàn thành → Gửi khiếu nại, chọn lý do và mô tả, chọn ảnh. Hồ sơ **Đã tiếp nhận**; mở Khiếu nại của tôi hoặc Hồ sơ → Theo dõi khiếu nại. Bấm gửi lại cùng đơn dẫn về hồ sơ cũ. Ảnh riêng tư không hiển thị trong catalog, seller/khách không tải được qua URL.
3. **1:30–2:15 — Admin:** Quản trị → Quản lý khiếu nại → hồ sơ. Đối chiếu snapshot/ảnh lúc đặt, trạng thái đơn/payment và các lịch sử. Chọn **Chuyển sang đang xử lý**, nhập phản hồi và lưu. Sau đó **Kết thúc xử lý**, chọn kết quả, nhập phản hồi + kết luận. Buyer thấy phản hồi, trạng thái **Đã xử lý** và kết quả.
4. **2:15–3:00 — Tích hợp:** khi hồ sơ RECEIVED/PROCESSING, đơn DELIVERED chưa được COMPLETE và UI giải thích lý do. Sau RESOLVED, buyer trở lại đơn và tự xác nhận nhận hàng. Đóng hồ sơ không tự complete, đổi payment hoặc hoàn kho. Admin mở lại có lý do, kết luận trước vẫn trong timeline; khi đơn đã COMPLETED thì việc mở lại không đảo đơn về trạng thái cũ.

## Routes (cộng prefix context Tomcat)

- `/buyer/reviews/new?orderId=ID&itemId=ID`, POST `/buyer/reviews/create`.
- `/buyer/complaints`, `/buyer/complaints/new?orderId=ID`, `/buyer/complaints/detail?id=ID`; POST create/supplement cùng prefix.
- `/admin/complaints?status=RECEIVED`, `/admin/complaints/detail?id=ID`; POST `/admin/complaints/action`.
- `/media/complaint-evidence?complaintId=ID&asset=ID`: chỉ buyer hồ sơ/Admin, không phải link public.

Lấy ID qua các liên kết UI, không tự sửa buyerId/sellerId/status trong form. Các POST cần session và CSRF. Complaint có nội dung 10–5.000 ký tự, ảnh tùy chọn tối đa 5/lần, 20/hồ sơ, 5 MB và 20 megapixel/ảnh. Thông báo lỗi nhập liệu hiển thị ngay trên form.

## Giải thích với giáo viên

Đánh giá gắn **dòng đơn đã hoàn thành**, do đúng buyer gửi; Service kiểm lại quyền và trạng thái, UNIQUE chống trùng. Nhãn xác thực và thống kê VISIBLE do server tính, giúp người mua sau đánh giá độ tin cậy. Form dùng một số sao chung, giữ hai cột rating của schema; không có điểm seller riêng trên UI.

Khiếu nại gắn đơn và đối chiếu **snapshot lúc mua**, tránh việc seller sửa mô tả/giá làm sai bằng chứng. Evidence giữ file riêng có quyền, nội dung/tiến độ/actor/thời gian được lưu. Cùng khóa order giúp khiếu nại mở và xác nhận nhận hàng không bỏ sót nhau. Kết luận chỉ ghi nhận cách xử lý; thu tiền, hoàn tiền, vận chuyển thật nằm ngoài đồ án.

Chưa có sửa/xóa/ẩn/phản hồi review, buyer mở hồ sơ thứ hai cho cùng đơn hoặc chức năng xử lý tiền thật. Muốn xử lý tiếp khi hồ sơ cũ đã đóng thì Admin mở lại; lịch sử giữ nguyên.
