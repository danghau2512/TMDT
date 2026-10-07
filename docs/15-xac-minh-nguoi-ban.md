# Xác minh hồ sơ người bán bằng QR — TraoTay

Cập nhật **06/10/2026**: thay OCR FPT.AI bằng **ZXing chạy cục bộ trên backend Java**, giữ hai ảnh, biểu mẫu, trạng thái, Admin duyệt, quyền đăng tin và bảo vệ giấy tờ. Không cần API key, quota dịch vụ hoặc mạng tới OCR. Không mở URL/thực thi nội dung QR. [Báo cáo và kiểm tra thực tế](reports/Xac-minh-nguoi-ban-QR.md).

## USER: tải hai ảnh và kiểm tra sáu trường

1. Bấm **Đăng bán** khi chưa duyệt → **Xác minh người bán**. Hoặc mở **Hồ sơ cá nhân → Xác minh người bán**.
2. Đồng ý lưu hai ảnh và thông tin riêng tư để Admin đối chiếu. Chọn **Mặt trước và Mặt sau** (cả hai bắt buộc). Xem trước ảnh JPEG/PNG; mỗi ảnh tối đa 5 MB, tối đa 20 megapixel. Chụp rõ vùng QR, đủ góc căn cước, tránh lóa/nhòe. Không cần ảnh QR thứ ba hay camera trực tiếp.
3. Bấm **Đọc thông tin từ QR**. Backend tìm QR trên cả hai mặt, có thử các góc xoay đơn giản. Sau khi đọc, trang chuyển đến biểu mẫu; phần **Đổi ảnh căn cước** được thu gọn, mở khi muốn chọn lại cả hai ảnh.
4. Kiểm tra **Số định danh, Họ và tên, Ngày sinh, Giới tính, Nơi cư trú, Ngày cấp**. Ngày hiển thị/nhập theo **dd/MM/yyyy**, lưu SQL DATE. Được sửa trước khi gửi; Admin thấy bản đọc ban đầu và những trường đã sửa.
5. Bấm **Xác nhận & gửi hồ sơ** → Chờ duyệt. QR thành công không tự duyệt. Không công khai sáu trường và không đổi display_name.

| Kết quả đọc | Xử lý |
| --- | --- |
| QR hợp lệ ở một mặt | Điền sáu trường từ kết quả hợp lệ |
| Hai mặt có cùng sáu trường | Chấp nhận, không tạo hồ sơ trùng trong lần upload |
| Nhiều QR hợp lệ khác thông tin | Không chọn một kết quả; báo mâu thuẫn, cần tải lại hai mặt cùng căn cước trước khi gửi |
| Có QR nhưng định dạng/dữ liệu không hỗ trợ | Báo rõ, biểu mẫu trống và nguồn **Nhập thủ công** |
| Không đọc được QR | Chụp lại hoặc nhập đủ sáu trường; vẫn phải Admin đối chiếu |

Bấm **Đọc lại QR trên hai ảnh** nếu cần. Khi đọc lại thất bại, các trường đang nhập thủ công được giữ nếu JavaScript bật. Nguồn **Đọc từ QR** chỉ do server xác định sau giải mã thành công; client không tự gửi nguồn hay trạng thái duyệt. Người nhập thủ công không được hiển thị đã đọc thành công.

## Định dạng QR hỗ trợ

Hỗ trợ chuỗi UTF-8 **đúng bảy trường**, phân cách bởi `|`:

```text
SO_CCCD|SO_CMND_CU|HO_TEN|NGAY_SINH|GIOI_TINH|NOI_CU_TRU|NGAY_CAP
```

Vị trí thứ hai là số CMND cũ (được rỗng), thứ ba mới là họ tên. CMND cũ không được lưu. Số định danh phải 12 chữ số, CMND cũ rỗng hoặc 9/12 chữ số; tên 2–120 ký tự, nơi cư trú 3–500 ký tự. Giới tính nhận Nam/Nữ/Khác không phân biệt hoa thường. Giữ dấu tiếng Việt, chuẩn hóa Unicode NFC/khoảng trắng hai đầu. Ngày QR dạng **ddMMyyyy**, kiểm tra lịch nghiêm ngặt; sinh từ năm 1900 đến hiện tại, cấp không trước sinh hoặc trong tương lai. Không đoán vị trí hoặc định dạng ngày khác.

Đây là một định dạng được hỗ trợ, không đại diện cho mọi mẫu căn cước. QR URL, JSON, dữ liệu ký/mã hóa, trường thiếu/thừa hoặc ngày sai được chuyển sang thông báo chưa hỗ trợ và nhập thủ công. Không tự truy cập liên kết QR. So sánh sáu trường sau chuẩn hóa; khác nhau thì yêu cầu kiểm tra ảnh. Không lưu toàn bộ chuỗi QR.

Ảnh lớn được thu nhỏ khi giải mã để vùng xử lý tối đa khoảng 4 megapixel; thử 0/90/180/270 độ. Chưa có xử lý chuyên sâu ảnh mờ/lóa/méo, ảnh mã quá nhỏ hoặc các định dạng mới ngoài bảy trường. Khi thử căn cước thật trên máy, chụp rõ mã ở cả hai mặt và đối chiếu cả sáu trường; chưa đọc được thì dùng thủ công, không xem QR như bằng chứng chính chủ.

ZXing core/javase **3.5.4**, phù hợp Java 17; tham khảo [release chính thức](https://github.com/zxing/zxing/releases/tag/zxing-3.5.4) và [API QRCodeMultiReader](https://zxing.github.io/zxing/apidocs/com/google/zxing/multi/qrcode/QRCodeMultiReader.html). Chỉ dùng thư viện Java, không đưa API key/SDK OCR vào trình duyệt.

## ADMIN và nhãn hồ sơ

ADMIN vào **Hồ sơ người bán → Chờ duyệt → Xem hồ sơ → Nhận xử lý hồ sơ**. Giữ quyền Admin phụ trách như trước; chủ hồ sơ và Admin đã nhận mới xem ảnh/PII. Hai ảnh nằm cạnh sáu trường, có nguồn dữ liệu và bản đọc ban đầu. Nhãn **Người dùng đã sửa** theo từng trường. Đối chiếu trước khi **Chấp thuận** hoặc **Từ chối & yêu cầu bổ sung**, lý do ít nhất 5 ký tự. Lưu reviewer/thời điểm, không cho USER tự duyệt.

USER xem lý do từ chối, chọn lại hai ảnh, sửa rồi gửi phiên bản mới. Khi được duyệt, mở Đăng bán: tin sản phẩm vẫn PENDING, cần Admin duyệt nội dung riêng. Nhãn công khai đổi thành **“Đã xác minh hồ sơ”**, tooltip **“Giấy tờ trong hồ sơ đã được quản trị viên kiểm tra”** tại sản phẩm/profile/chat. Chỉ công khai boolean và tên hiển thị.

Một USER vẫn vừa mua vừa bán. Mua hàng, chat, xử lý đơn cũ không cần hồ sơ mới. Đổi giấy tờ đã duyệt tạm ngừng quyền đăng tin mới/nhãn đến khi được duyệt lại; không tự ẩn tin cũ, hủy đơn hoặc đổi thanh toán.

Đây là duyệt hồ sơ của nền tảng đồ án. Không xác thực với Bộ Công an/VNeID, không chứng minh người tải ảnh là chủ căn cước hoặc bảo đảm giao dịch không gian lận. Không face/liveness/chip/AI training.

## Database: V007, giữ hồ sơ OCR cũ

**Máy hiện tại:** c2c_demo standalone V006 đã backup và áp **V007 một lần ngày 06/10/2026**. Không chạy lại V001–V007, không seed/baseline/reset. V007 tái sử dụng hai bảng seller_profiles/submissions, không thêm bảng (vẫn 28 bảng nghiệp vụ).

V007 thêm birth_date/gender/residence/issue_date, data_source, qr_status/qr_started_at và baseline sáu trường. Ngày dùng DATE, text UTF-8. CHECK bắt buộc sáu trường/hai ảnh cho hồ sơ mới đã gửi; miễn điều kiện bổ sung cho hồ sơ cũ. Cột OCR/ảnh/thông tin/quyết định cũ giữ nguyên, nguồn thành OCR_LEGACY hoặc MANUAL_LEGACY, không chuyển thành QR. Hồ sơ cũ vẫn xem và duyệt được dù thiếu mặt sau/trường mới; tải hai ảnh mới để đi vào luồng QR. Không tự duyệt người dùng hoặc yêu cầu duyệt lại hồ sơ cũ chỉ vì migration.

Trên máy khác **standalone V006**: khảo sát, backup DB và giữ riêng ảnh trước khi áp bản còn thiếu. Trong MySQL client ở gốc repository:

```sql
USE c2c_demo;
SHOW COLUMNS FROM seller_verification_submissions LIKE 'data_source';
-- Chỉ SOURCE nếu đúng standalone V006, chưa có V007 và đã backup:
SOURCE database/migrations/V007__seller_verification_qr.sql;
```

Với schema **đã được Flyway quản lý**, chọn file cấu hình đúng schema rồi:

```powershell
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
.\mvnw.cmd -B compile exec:java '-Dexec.args=migrate'
```

Không dùng Flyway migrate/baseline cho c2c_demo standalone hiện tại. Nếu bảng/cột đã tồn tại hoặc migration dở dang, dừng để đối chiếu; không DROP/reset. Bản Flyway ở `src/main/resources/db/migration/V007__seller_verification_qr.sql`, bản standalone giống byte ở `database/migrations`. Deploy không tự migrate/seed. Schema mới đầy đủ dùng Flyway V001 → V007; schema.sql vẫn là V001 bất biến.

## Cấu hình và build/deploy

Giữ cấu hình MySQL, upload và VNPAY đã điền. Luồng xác minh không đọc OCR_API_KEY/OCR_ENDPOINT/ocr.apiKey/ocr.endpoint nữa; các khóa cũ trong file local được bỏ qua, không cần điền và không tự xóa/ghi đè file của bạn. Đã bỏ adapter FPT khỏi source/WAR.

Khóa duy nhất liên quan lưu giấy tờ:

```properties
verification.root=
```

Hoặc biến **VERIFICATION_ROOT**. Bỏ trống thì dùng thư mục seller-documents cạnh upload.root; phải là đường dẫn tuyệt đối, riêng biệt với upload.root, ngoài WAR/public. Ví dụ `C:/c2c-private/seller-documents`. Giữ đường dẫn cũ nếu đã có giấy tờ, không đổi root rồi khiến ảnh cũ mất truy cập. Cấp quyền đọc/ghi cho tài khoản chạy Tomcat; không public thư mục này.

```powershell
.\mvnw.cmd -B verify
```

WAR: target/demo-1.0-SNAPSHOT.war. Trong IntelliJ: **Stop Tomcat → Rebuild artifact → Run → Ctrl+F5**. Cần restart vì Config/Service/dependency đã đổi. Tomcat vẫn dùng APP_CONFIG_FILE hoặc -Dc2c.config trong Run Configuration; biến terminal không tự truyền sang JVM Tomcat. Nếu build từ bản OCR cũ, có thể `clean verify` để loại class cũ khỏi target.

## Bảo vệ và xóa sau demo

Ảnh riêng qua endpoint có session/ownership/Admin phụ trách, CSRF cho upload/read/submit/claim/decide/purge, HTML escape, no-store; key file server tạo, không dùng path/tên client. Không log chuỗi QR/sáu trường/ảnh, không đưa giấy tờ thật vào seed/test/Git/URL/báo cáo.

Sau demo USER mở **Xóa giấy tờ đã gửi** và xác nhận: xóa bytes, sáu trường, baseline QR và OCR cũ của mọi phiên bản; giữ metadata quyết định/consent, sản phẩm và đơn cũ. Không tự xóa hồ sơ/ảnh đang có khi migrate. Backup chứa thông tin cá nhân cần giữ riêng; thao tác xóa trên app không xóa backup hoặc bản đã gửi tới dịch vụ cũ. Không gửi mới ra OCR. Che cả thông tin và QR khi chụp căn cước thật cho báo cáo; ảnh test trong docs là QR/dữ liệu giả có nhãn DEMO.

## Demo ngắn

1. USER chưa duyệt bấm Đăng bán → tải đủ hai ảnh → Đọc thông tin từ QR.
2. Kiểm tra/sửa sáu trường → Xác nhận & gửi hồ sơ → Chờ duyệt.
3. ADMIN ở cửa sổ thường/ẩn danh khác nhận xử lý → thấy ảnh, nguồn QR và trường đã sửa → Chấp thuận.
4. USER đăng sản phẩm → ADMIN duyệt nội dung → người mua thấy nhãn “Đã xác minh hồ sơ” ở chi tiết/chat.
5. Tùy chọn: từ chối kèm lý do/gửi lại, QR không hỗ trợ nhập thủ công, hai QR mâu thuẫn cần tải lại. Kết thúc xóa giấy tờ demo.

Đã kiểm tra parser/decoder/UI với QR dữ liệu giả, chưa thử căn cước thật. [Báo cáo file, lệnh và giới hạn](reports/Xac-minh-nguoi-ban-QR.md).