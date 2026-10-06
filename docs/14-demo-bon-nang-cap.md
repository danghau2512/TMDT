# Demo 4 nâng cấp — khoảng 5–7 phút

Ngày 05/10/2026. Giữ Java 17, Servlet/JSP/JSTL, JDBI/MySQL và toàn bộ luồng mua bán, COD, VNPAY Sandbox, chat.

## Chạy phiên bản mới

**Máy hiện tại:** `c2c_demo` đã được sao lưu toàn bộ và áp **V004** một lần trong lượt này. Giá trị/số dòng ở các cột cũ của 25 bảng được đối chiếu trước/sau và giữ nguyên; `review_images` mới trống. Không seed hoặc ghi dữ liệu demo vào database ứng dụng. Không SOURCE V004 lần nữa, không baseline Flyway cho schema standalone này.

```powershell
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
.\mvnw.cmd -B verify
.\mvnw.cmd -B exec:java '-Dexec.args=check'
```

WAR ở `target/demo-1.0-SNAPSHOT.war`. Với IntelliJ/Tomcat đang dùng: rebuild artifact, **Stop/Run Tomcat**, giữ VM option `-Dc2c.config=` trỏ file local hiện có. Nếu deploy WAR thủ công: dừng đúng Tomcat của ứng dụng, thay WAR, khởi động lại và truy cập theo context thực tế (ví dụ `/demo-1.0-SNAPSHOT/products`). Không dùng file cấu hình test cho ứng dụng. Storage ngoài WAR được giữ nguyên.

## Migration trên máy/schema khác

1. Kiểm tra đúng schema, `SHOW TABLES`, Flyway history và `SHOW CREATE TABLE` của products, product_images, order_items, order_item_images, media_assets, reviews. Sao lưu database và storage trước nâng cấp. V004 cần cấu trúc V001–V003 đầy đủ.
2. Nếu schema **được Flyway quản lý**, chọn file cấu hình của schema đó và chạy `./mvnw.cmd -B compile exec:java '-Dexec.args=migrate'`; chạy lại trả 0, không seed khi deploy.
3. Nếu standalone đã áp V001–V003, kiểm tra chưa có `review_images`/các cột mới rồi áp riêng V004 bằng MySQL client của đúng instance. Không chạy migrate để tự baseline, không áp lại V001/V002/V003.

```powershell
& 'C:\wamp64\bin\mysql\mysql9.1.0\bin\mysql.exe' --protocol=TCP --host=127.0.0.1 --port=3306 --user=YOUR_SETUP_USER --password --default-character-set=utf8mb4
```

Trong client, từ thư mục gốc repository:

```sql
USE c2c_demo;
SHOW TABLES LIKE 'review_images';
SHOW COLUMNS FROM products LIKE 'appearance_code';
-- Chỉ SOURCE khi cả hai kiểm tra xác nhận V004 chưa áp và đã sao lưu:
SOURCE src/main/resources/db/migration/V004__reviews_condition_comparison.sql;
SHOW CREATE TABLE review_images;
```

Credential nhập cục bộ, không đặt trong lệnh/tài liệu. MySQL DDL có implicit commit; nếu script dừng giữa chừng, đối chiếu cấu trúc với từng lệnh V004 để bổ sung phần thiếu, không SOURCE lại toàn file hay xóa bảng. Bản sao lưu hiện tại nằm ngoài commit trong `.local-test/c2c-demo-before-upgrades-20261005.sql`.

## Chuẩn bị dữ liệu qua giao diện

Mở ba cửa sổ/profile trình duyệt độc lập: Admin, người bán, người mua. Có thể dùng cửa sổ thường và ẩn danh cho hai tài khoản, profile trình duyệt khác cho Admin. Các tài khoản seed nếu **đã tồn tại trên schema demo riêng**: `admin@c2c.example`, `seller1@c2c.example`, `buyer1@c2c.example`, mật khẩu fixture công khai `C2cDemo!2026`. Trên database của bạn hãy dùng tài khoản hiện có; không chạy seed chỉ để có tài khoản này.

- Người bán đăng 2–3 sản phẩm cùng danh mục, giá khác nhau và có tồn kho. Điền ngoại hình, hoạt động, lỗi đã biết, sửa chữa, phụ kiện; tải ảnh JPEG/PNG thật và đánh dấu ảnh khuyết điểm. Admin duyệt từng tin. Chọn “Không áp dụng” với ngành hàng không phù hợp, không tự đánh dấu “không lỗi” cho dữ liệu cũ.
- Người mua đặt một đơn COD, seller CONFIRM → SHIP → DELIVER, buyer xác nhận nhận hàng để COMPLETED. Chưa gửi đánh giá cho dòng đơn này.
- Chuẩn bị một đơn khác cho khiếu nại. Không dùng hồ sơ đang RECEIVED/PROCESSING cho bước hoàn thành đơn: quy tắc cũ chặn COMPLETE.
- Chuẩn bị 1–3 ảnh JPEG/PNG thực tế, mỗi ảnh ≤5 MB. Một tin/đơn cũ không khai báo giúp minh họa “Chưa cung cấp”. Ảnh trong báo cáo là fixture test, không phải ảnh sản phẩm ứng dụng.

## Kịch bản trình chiếu

| Thời gian | Thao tác | Lợi ích thể hiện |
| --- | --- | --- |
| 0:00–1:20 | Seller mở/sửa khai báo, xem thẻ tình trạng, lỗi nổi bật và ảnh khuyết điểm sau Admin duyệt | Người mua biết rõ đặc điểm hàng cũ trước khi trao đổi; không phải chứng nhận kiểm định |
| 1:20–2:40 | Chọn 2 rồi 3 sản phẩm từ catalog/detail, mở bảng, bật “Chỉ hiện điểm khác nhau”, bỏ/thay một sản phẩm | Đối chiếu giá/tình trạng/phụ kiện; giá thấp nhất chỉ là thông tin, không khẳng định tốt nhất |
| 2:40–3:50 | Từ đơn COMPLETED, chọn sao/nhận xét và 2 ảnh; bỏ 1 ảnh trước gửi. Mở sản phẩm, lọc sao/có ảnh, phóng lớn | Đánh giá xuất phát từ giao dịch thật, ảnh giúp người mua sau tham khảo |
| 3:50–5:40 | Buyer gửi khiếu nại theo đơn + ảnh, xem thẻ tổng quan. Admin tìm mã đơn, PROCESS, phản hồi rồi RESOLVE. Buyer mở lại xem kết quả | Hồ sơ hỗ trợ có tiến độ, bằng chứng riêng và lịch sử người thực hiện; không tự thay tiền/kho/đơn |
| 5:40–6:30 | Mở tin/đơn cũ, xem “Chưa cung cấp”; sửa khai báo tin rồi mở snapshot đơn trước đó | Dữ liệu cũ vẫn dùng được, thông tin tại lúc mua không bị sửa theo tin hiện tại |

## Quy tắc và giới hạn

- Review chỉ buyer của đơn COMPLETED, một review/order_item. 0–3 ảnh; comment 1–2.000 ký tự. Backend kiểm byte ảnh và encode lại bằng ImageStorage; rollback dọn file chưa được tham chiếu. Sau lỗi form phải chọn lại file ảnh do giới hạn trình duyệt.
- Thống kê và phân bố sao tính tất cả review VISIBLE từ đơn hợp lệ; bộ lọc có 8 review/trang. Media review chỉ công khai khi review/đơn/sản phẩm hiện được phép hiển thị; ảnh khiếu nại không dùng chung quyền này.
- Khai báo gồm ba mã lựa chọn và ba trường text ≤1.000 ký tự. Bỏ trống lưu NULL, không suy ra không lỗi. Đổi khai báo/đánh dấu ảnh làm tin chờ duyệt lại và tăng listing_version; checkout lưu bản sao cả text/code/flag ảnh trong transaction hiện có.
- So sánh cần JS để chọn và localStorage nếu trình duyệt cho phép; chỉ lưu ID. Server trả thông tin công khai hiện tại, tối đa 3 sản phẩm, bảng chỉ mở khi có ≥2 cùng danh mục. Tin ẩn/không được xem báo không còn hiển thị và có thể bỏ. Điểm giao dịch seller tổng hợp từ seller_rating của các review VISIBLE/COMPLETED ở tin còn công khai. Hai cột rating hiện dùng cùng điểm chung; giao diện ghi rõ chưa có thang điểm người bán độc lập.
- Thanh chọn chỉ hiện khi khám phá sản phẩm; trang so sánh có nút bỏ trên từng cột. Trên mobile cuộn bảng ngang, cột tiêu chí cố định. Không che chat, form tài khoản hoặc hồ sơ khiếu nại bằng thanh chọn.
- Khiếu nại vẫn một hồ sơ/order; buyer bổ sung khi mở, Admin có thể mở lại với lý do. Danh sách giới hạn 200 hồ sơ, thẻ thống kê tính toàn bộ phạm vi tài khoản. Timeline chỉ dùng lịch sử thật; tên hiển thị actor đọc từ tài khoản hiện tại, role của phản hồi giữ snapshot.

[Báo cáo kiểm tra và file chính](reports/Bon-nang-cap-trai-nghiem.md).
