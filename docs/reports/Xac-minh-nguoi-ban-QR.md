# Chuyển xác minh người bán từ OCR sang QR — 06/10/2026

Đã thay FPT.AI bằng ZXing core/javase 3.5.4 đọc QR cục bộ trên cả hai ảnh. Giữ nền tảng Java 17/Jakarta Servlet/JDBI/JSP/TraoTay, duyệt hồ sơ, quyền đăng tin, privacy/CSRF và các nghiệp vụ mua bán. [Cách thao tác, định dạng, migration và demo](../15-xac-minh-nguoi-ban.md).

## Những thay đổi chính

- Hồ sơ mới bắt buộc mặt trước/mặt sau và sáu trường. Reader thử bốn góc xoay, thu thập nhiều QR từ cả hai mặt; parser nhận đúng bảy trường, bỏ CMND cũ ở vị trí thứ hai, kiểm tra ngày ddMMyyyy bằng lịch nghiêm ngặt. Không lưu raw QR hoặc mở URL. Ngày DB DATE, UI dd/MM/yyyy, Unicode NFC giữ tiếng Việt.
- QR ở một mặt đủ dùng; hai kết quả cùng sáu trường chấp nhận. Nhiều thông tin khác nhau: clear baseline, báo CONFLICT và không cho submit trước khi tải lại. QR không có/không hỗ trợ: phân biệt thông báo, nhập thủ công đủ sáu trường. Server xác định nguồn QR/MANUAL; không lấy nguồn/status/reviewer từ request.
- Giải mã ngoài Handle; QR result chỉ cập nhật đúng draft/current owner sau khi đọc. Submit/claim/decide giữ lock và transaction hiện có. Giữ baseline sáu trường tối thiểu để Admin thấy bản đọc và các chỉnh sửa. QR không tự duyệt, không đổi display_name.
- Giao diện hai ảnh/preview, nút Đọc thông tin từ QR, nguồn dữ liệu rõ ràng và form đặt trước phần đổi ảnh khi đã đọc. Nhãn đổi thành “Đã xác minh hồ sơ”; giải thích nhãn giữ nguyên. Admin đối chiếu hai ảnh với sáu trường/baseline. Mobile được kiểm tra 390px.
- Bỏ FptOcrClient/OcrResult/OcrConfig và test adapter cũ. VerificationConfig chỉ giữ root; khóa OCR cũ bỏ qua, không sửa file config local hay cấu hình VNPAY/DB. Bỏ giới hạn upload/ngày vốn dùng bảo vệ luồng OCR có quota; giữ giới hạn 5 MB/20 MP mỗi ảnh, xử lý QR khoảng 4 MP và guard request đang đọc.

## V007 và bảo toàn dữ liệu

Khảo sát c2c_demo loopback 3306/standalone V006 có 28 bảng. Full backup riêng tư `.local-test/c2c_demo-before-v007-20261006.sql`, kiểm tra fingerprint dòng/tất cả cột cũ trước/sau giống nhau. Áp V007 một lần, không sửa V001–V006/schema.sql, không reset/seed/approve người dùng, không xóa ảnh hiện có.

V007 thêm data_source, bốn trường bổ sung, QR status/start và baseline sáu trường vào bảng submissions; không tạo bảng mới. Hồ sơ cũ OCR thành công đánh dấu OCR_LEGACY, còn lại MANUAL_LEGACY; giữ nguyên cột/baseline OCR/quyết định. CHECK mới chỉ buộc đủ hai ảnh/sáu trường cho hồ sơ mới, cho phép xem/duyệt bản cũ thiếu trường. UI vẫn nhận ra baseline OCR của bản cũ có thể được gửi trong thời gian trước restart. 28 bảng nghiệp vụ (29 khi có Flyway history). c2c_demo hiện tại **không chạy lại V007**.

Schema test c2c_upgrades_test/MySQL riêng 13316 cũng backup trước migrate V007; dùng Flyway history hợp lệ. Các kiểm tra ghi tiếp theo chỉ ở schema test, không dữ liệu ứng dụng.

## File đã thay đổi

| Nhóm | File |
| --- | --- |
| Dependency | pom.xml — ZXing core/javase 3.5.4 |
| Migration | src/main/resources/db/migration/V007__seller_verification_qr.sql; database/migrations bản giống byte |
| Config | VerificationConfig.java (mới), AppConfig.java, ShopServices.java, ApplicationListener.java, config/application.example.properties; bỏ OcrConfig.java |
| Parser/reader/DTO | verification/IdentityParser.java, IdentityQrReader.java; model/IdentityDetails.java, dto/VerificationForm.java (mới); bỏ FptOcrClient.java/OcrResult.java |
| Backend | SellerVerificationService.java, SellerVerificationDao.java, SellerVerificationServlet.java — hai ảnh, sáu trường, source/baseline, legacy, purge mở rộng |
| UI | views/verification/own.jsp, detail.jsp; seller-verification.css/js; shop-start.jspf cache version qr1 |
| Nhãn | catalog/detail.jsp, auth/profile.jsp, chat/inbox.jsp |
| Kiểm tra | IdentityQrReaderTest, QrFixtures (mới), SellerVerificationIT, SellerTestProfiles và ChatIT fixture độc lập; bỏ FptOcrClientTest |
| Tài liệu | README, docs/README, docs/02–05, hướng dẫn 15, database/README, AGENTS, ghi chú lịch sử báo cáo OCR và báo cáo này |

Hồ sơ/ảnh thật không được đưa vào fixture. Ảnh docs/screenshots/verification-qr là QR giả, chữ DEMO, các trường được che. Script/helper/result/backup giữ .local-test ignored.

## Kiểm tra thực tế

- `./mvnw.cmd -B clean verify` và `verify`: BUILD SUCCESS, **34 unit** (28 cũ + 6 QR/config). Parser giữ dấu/bảy vị trí/CMND rỗng/ngày leap; không đoán URL hoặc ngày sai; decoder PNG/JPEG, QR trước/sau/xoay, cùng/mâu thuẫn kể cả hai QR một ảnh; NOT_FOUND khác UNSUPPORTED; config OCR cũ bị bỏ qua, private root vẫn được kiểm tra.
- **4 SellerVerificationIT đạt trên MySQL thật**: manual hai ảnh/sáu trường, quyền/duyệt/đăng tin/giữ tin cũ/đổi hồ sơ/từ chối/gửi lại/purge; QR thật chứa dữ liệu giả, DATE, audit trường sửa, CONFLICT server chặn; OCR legacy xem/duyệt được; constraint hồ sơ/lý do. Không coi fixture OCR legacy là gọi OCR thật.
- **15 regression IT/HTTP cũ đạt**: CommerceIT 3, ChatIT 3, ReputationIT 3, UpgradesIT 3; ShopHttpIT/FeedbackHttpIT/ChatHttpIT mỗi bộ 1. Dùng fixture đã được Admin duyệt qua Service, cập nhật helper hai ảnh/sáu trường và ChatIT không phụ thuộc bộ khác. Lượt đầu gặp giới hạn 10 upload từ cơ chế OCR cũ; đã bỏ theo luồng QR không còn quota, chạy lại đạt. Không dùng profile mặc định M1/M2 seed assumptions trên fixture schema này.
- Tomcat 10.1.48 loopback 18080/context c2c, Java 17, guard xác nhận cùng schema test qua JDBI/HTTP; SELECT 1=1. Chrome headless thực tế **16 nhóm kiểm tra đạt**: hai ảnh bắt buộc, QR trước/sau xoay/same/conflict, autofill sáu trường và tiếng Việt/ngày, invalid date 400, ownership ảnh 404, source/audit chỉnh sửa, từ chối/gửi lại/duyệt/đăng tin và kiểm duyệt riêng, nhãn sản phẩm/chat, URL QR unsupported/manual, không QR nhập thủ công và retry giữ nội dung, các route cũ và không lỗi JS.
- Kiểm tra UI cuối sau đổi bố cục: form đến đúng anchor sau đọc, phần đổi ảnh mở/đóng được, 1366/390 không tràn ngang. Đã xem ảnh desktop/mobile của form/Admin. Không kiểm trực quan mọi trang website.
- `jar tf target/demo-1.0-SNAPSHOT.war`: có core-3.5.4.jar/javase-3.5.4.jar, không còn FptOcrClient/OcrConfig. Source xác minh không còn HttpClient, api_key hay endpoint FPT; luồng mới không gọi FPT.AI. VNPAY vẫn giữ client riêng hiện có.
- Sau thử nghiệm đã purge giấy tờ giả của các tài khoản fixture qua Service trên đúng schema test; kiểm tra không còn dòng có key ảnh/số giấy tờ/baseline QR riêng tư. Backup test giữ trong .local-test ignored. Dừng Tomcat 18080 và MySQL 13316; không dừng dịch vụ 8080/3306 hay xóa hồ sơ/ảnh của c2c_demo.

Lệnh chính (biến môi trường chọn cấu hình test, C2C_HTTP_BASE chỉ loopback test):

```powershell
.\mvnw.cmd -B clean verify
.\mvnw.cmd -B dependency:build-classpath '-Dmdep.outputFile=.local-test/qr-classpath.txt'
.\mvnw.cmd -B verify -Pmysql-it '-Dit.test=SellerVerificationIT'
.\mvnw.cmd -B verify -Pmysql-it '-Dit.test=CommerceIT,ChatIT,ReputationIT,UpgradesIT,ShopHttpIT,FeedbackHttpIT,ChatHttpIT'
```

Standalone V007 chạy bằng helper guard/backup/fingerprint rồi JDBI createScript; Flyway migrate chỉ schema test có history. Trình duyệt dùng .local-test/qr-browser.cjs và qr-final-ui.cjs, Java helper tạo QR giả bằng ZXing Writer. Không đưa credential/request nhạy cảm vào output.

## Giới hạn và việc trên máy người dùng

**Chưa thử ảnh căn cước thật.** Đã giải mã QR chứa dữ liệu giả, không chỉ giả lập DTO. Định dạng hỗ trợ hiện là bảy trường `|`, hai ngày ddMMyyyy; định dạng khác hoặc ảnh khó đọc chuyển sang thủ công. Không triển khai liveness/camera/face/chip/VNeID hoặc xác thực chính chủ. QR thành công chỉ hỗ trợ nhập liệu, quyết định vẫn của Admin.

Đã cập nhật nguồn/SQL/WAR, chưa restart Tomcat 8080 của người dùng; cần **Stop → Rebuild artifact → Run → Ctrl+F5**. Không cần API key, không chạy lại V007 trên c2c_demo hiện tại. Giữ private root cũ để ảnh cũ vẫn đọc được. Dùng hướng dẫn 15 thử hai ảnh thật trên máy, đối chiếu sáu trường rồi xóa giấy tờ sau demo; che cả QR trong báo cáo thật. Chưa thử tải lớn, ảnh mờ/lóa nặng, toàn bộ VNPAY Sandbox hoặc chức năng chuyển Admin phụ trách.

[Ảnh upload desktop](../screenshots/verification-qr/01-upload-1366.png) · [Form mobile đã che](../screenshots/verification-qr/02-form-redacted-390.png) · [Admin đối chiếu đã che](../screenshots/verification-qr/03-admin-redacted-1366.png).

Gợi ý commit: `Thay OCR bằng đọc QR cục bộ và hoàn thiện hồ sơ người bán sáu trường`.
