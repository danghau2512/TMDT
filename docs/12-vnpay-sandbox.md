# Demo VNPAY Sandbox — 05/10/2026

VNPAY Sandbox là môi trường thử nghiệm. Checkout mới có **COD / VNPAY_SANDBOX**; mỗi đơn của mỗi người bán có số tiền và lần thanh toán riêng. Các bản ghi `BANK_TRANSFER_SIMULATED` cũ giữ nguyên và vẫn xem/xử lý được theo luật cũ. Không có API hoàn tiền hoặc thanh toán production.

## 1. Cấu hình backend

File hiện tại: `config/application.local.properties`, đã ignore và không đóng gói vào WAR. Lượt này đã bổ sung những khóa VNPAY còn thiếu từ thông tin TEST được cung cấp; giữ nguyên các khóa DB/storage đã có. Không copy file mẫu ghi đè file local.

`AppConfig` chọn `-Dc2c.config` trước `APP_CONFIG_FILE`; đường dẫn tuyệt đối được khuyến nghị. Maven và Tomcat là hai tiến trình khác nhau. Với IntelliJ, giữ VM options hiện tại và dùng:

```text
-Dc2c.config="C:\Users\hausi\OneDrive\Desktop\TMDT\project\web\demo\config\application.local.properties"
```

| Khóa | Biến môi trường ưu tiên | Giá trị/điều kiện |
| --- | --- | --- |
| `vnpay.enabled` | `VNPAY_ENABLED` | `true`/`false`; mẫu mặc định false |
| `vnpay.tmnCode` | `VNPAY_TMN_CODE` | Merchant TEST, 8 ký tự chữ/số |
| `vnpay.hashSecret` | `VNPAY_HASH_SECRET` | Secret phía backend, không rỗng/giá trị mẫu, tối thiểu 16 ký tự; không đưa vào browser/log |
| `vnpay.payUrl` | `VNPAY_PAY_URL` | `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` |
| `vnpay.queryUrl` | `VNPAY_QUERY_URL` | `https://sandbox.vnpayment.vn/merchant_webapi/api/transaction` |
| `vnpay.returnUrl` | `VNPAY_RETURN_URL` | URL tuyệt đối HTTP(S), gồm đúng context và `/payments/vnpay/return` |
| `vnpay.ipnUrl` | `VNPAY_IPN_URL` | URL tuyệt đối HTTP(S), gồm đúng context và `/payments/vnpay/ipn`; cần đăng ký bên Sandbox |
| `vnpay.expiryMinutes` | `VNPAY_EXPIRY_MINUTES` | Số nguyên 5–30, mặc định 15 |
| `vnpay.tls.useWindowsRoot` | `VNPAY_TLS_USE_WINDOWS_ROOT` | `true`/`false`, mặc định false; true dùng kho CA Windows riêng cho HTTP client VNPAY |

Biến môi trường ưu tiên kể cả rỗng. Các URL callback không chứa query/fragment/userinfo. Code chỉ chấp nhận hai endpoint gateway Sandbox đã nêu, chặn cấu hình production.

Local IntelliJ đã chuẩn bị Return/IPN `http://localhost:8080/demo_war_exploded/payments/vnpay/...`. Nếu đổi WAR thành `c2c.war`, dùng `/c2c`; nếu đổi cổng, sửa cả hai URL. Truy cập website bằng **cùng hostname** với Return URL (không đổi qua lại localhost/127.0.0.1), để cookie đăng nhập theo trình duyệt còn hiệu lực khi quay về. Hết session: đăng nhập lại, vào đơn → lịch sử VNPAY → xem kết quả và kiểm tra; không kết luận thất bại chỉ vì mất session.

IPN URL trong file local **không tự đăng ký** với VNPAY, không gửi như tham số của pay URL. Cần cấu hình địa chỉ public HTTPS ở tài khoản Sandbox hoặc yêu cầu bên cấp merchant cấu hình. Nếu dùng tunnel: trỏ về đúng cổng/context, sửa IPN URL theo public hostname, xác minh callback từ VNPAY. Không mở toàn bộ tuyến payment; chỉ chính xác GET `/payments/vnpay/ipn` được miễn session/CSRF.

### HTTPS querydr trên Windows

Kiểm tra thực tế gặp `SSLHandshakeException` với truststore mặc định của JDK 17 trên máy này. Tomcat IntelliJ đang chạy có `c2c.config` đúng nhưng chưa có các VM options truststore đã hướng dẫn, nên lỗi vẫn xảy ra. Bản sửa ngày 05/10/2026 cho phép chọn kho CA Windows ngay trong cấu hình backend, chỉ áp dụng cho HTTP client VNPAY.

Trong **file local đang được Tomcat đọc**, thêm hoặc sửa duy nhất dòng sau (không copy file mẫu ghi đè credential hiện có):

```properties
vnpay.tls.useWindowsRoot=true
```

`config/application.local.properties` trên máy hiện tại đã được bật khóa này; các khóa DB/storage/merchant/secret giữ nguyên. Biến môi trường `VNPAY_TLS_USE_WINDOWS_ROOT` nếu có sẽ ưu tiên hơn file. Rebuild artifact, **Stop rồi Run Tomcat lại** để nạp Java và cấu hình mới; refresh trang hoặc chỉ redeploy JSP không đủ. Giữ VM option `-Dc2c.config` trỏ đến file local. Không cần thêm VM options truststore khi dùng khóa mới.

Kiểm tra riêng kết nối, không mở database, không gửi secret/querydr hoặc tạo giao dịch:

```powershell
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
.\mvnw.cmd -B compile exec:java '-Dexec.args=vnpay-check'
```

Kết quả đã chạy: **VNPAY HTTPS OK, HTTP 405**. Lệnh dùng HEAD không có body; endpoint trả 405 vì không hỗ trợ HEAD, nhưng Java đã xác thực được TLS và hostname rồi nhận phản hồi HTTP. Điều này chỉ chứng minh kết nối HTTPS; không chứng minh querydr nghiệp vụ thành công hoặc đơn đã PAID. Sau restart Tomcat, vào đơn hiện có → **Kiểm tra kết quả thanh toán** (tôn trọng thời gian chờ 30 giây). Không tạo lần thanh toán mới chỉ để xử lý lỗi chứng chỉ.

Khi false hoặc không có khóa, client dùng truststore mặc định của JVM. Nếu đã chọn cách cấu hình JVM trước đây, các VM options sau vẫn là một phương án khác cho Tomcat Windows:

```text
-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NUL
```

Hoặc cho Tomcat chạy từ PowerShell (giữ các JAVA_OPTS khác):

```powershell
$env:JAVA_OPTS = "$env:JAVA_OPTS -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NUL"
```

Windows-ROOT qua SunMSCAPI dùng kho CA của tài khoản Windows chạy Tomcat. Client tạo SSLContext riêng với TrustManagerFactory; vẫn kiểm tra chuỗi chứng chỉ và hostname HTTPS, không đổi SSLContext mặc định toàn JVM. Nếu OS/provider không hỗ trợ hoặc kho CA rỗng, ứng dụng báo lỗi cấu hình an toàn; không fallback sang trust-all. Trên OS/JDK khác giữ false và dùng JDK/truststore có CA hợp lệ. Không tự import chứng chỉ không rõ nguồn, không tắt kiểm tra hostname và không đặt credential/secret vào VM options. Nếu dùng tài khoản Windows khác để chạy Tomcat, cần kiểm tra lại kho CA của tài khoản đó.

Cơ chế tham khảo: [Oracle JDK 17 — SunMSCAPI/Windows-ROOT](https://docs.oracle.com/en/java/javase/17/security/oracle-providers.html), [HttpClient.Builder.sslContext](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.Builder.html). Kết quả và file sửa ở [báo cáo khắc phục HTTPS](reports/VNPAY-khac-phuc-HTTPS.md).

## 2. Migration và dữ liệu

`V001` và `schema.sql` giữ nguyên. **V002__vnpay_sandbox.sql** mở rộng phương thức/trạng thái, bổ sung `REFUND_PENDING`, actor `SYSTEM` cho lịch sử gateway và bảng `vnpay_attempts`. Sau V002: 23 bảng nghiệp vụ; thêm Flyway history thành 24 bảng vật lý nếu schema do Flyway quản lý. `is_simulated=1` vẫn đúng vì đây là Sandbox, không phải tiền production.

DDL MySQL có implicit commit. Sao lưu/khảo sát trước khi nâng cấp; không DROP/reset, không sửa checksum V001, không seed lại để thử gateway. UNIQUE active_order_id giới hạn một attempt PENDING/đơn; UNIQUE TxnRef và merchant/transaction_no chống trùng tham chiếu/gateway transaction.

### Schema có Flyway history

Kiểm tra V001 đã thành công, chưa có V002, DDL phù hợp; sao lưu trước. Chạy:

```powershell
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
.\mvnw.cmd -B compile exec:java '-Dexec.args=migrate'
.\mvnw.cmd -B exec:java '-Dexec.args=check'
```

Schema trống: migrate thực hiện V001 + V002, sau đó seed chỉ cho database phát triển. Chạy migrate lại trả 0 khi đã cập nhật. Không chạy standalone schema.sql rồi migrate trên cùng schema.

### Schema nhập schema.sql, không có Flyway history

**`c2c_demo` hiện tại thuộc trường hợp này. V002 đã được áp một lần trong lượt này** sau khảo sát, sao lưu và kiểm tra bảo toàn dữ liệu. Không cần chạy V002 lại và không chạy seed. Backup cục bộ: `.local-test/c2c-demo-before-vnpay-20261005.sql`; file chứa dữ liệu riêng tư, không commit/chia sẻ.

Với một bản cài khác còn ở standalone V001: đối chiếu `SHOW CREATE TABLE payments`, `payment_status_history` với V001 và các constraint được thay trong V002; kiểm tra chưa có vnpay_attempts; sao lưu đầy đủ. Ví dụ trong PowerShell:

```powershell
$mysqlExe = 'C:\wamp64\bin\mysql\mysql9.1.0\bin\mysql.exe'
$dumpExe = 'C:\wamp64\bin\mysql\mysql9.1.0\bin\mysqldump.exe'
& $dumpExe --protocol=TCP --host=127.0.0.1 --port=3306 --user=YOUR_SETUP_USER --password --single-transaction --routines --triggers --result-file=C:/c2c-backup/before-vnpay.sql c2c_demo
# Kiểm tra backup thành công trước khi mở client và áp DDL.
& $mysqlExe --protocol=TCP --host=127.0.0.1 --port=3306 --user=YOUR_SETUP_USER --password --default-character-set=utf8mb4 c2c_demo
```

Trong MySQL client, tại thư mục repository:

```sql
SHOW TABLES LIKE 'flyway_schema_history';
SHOW TABLES LIKE 'vnpay_attempts';
SHOW CREATE TABLE payments;
SHOW CREATE TABLE payment_status_history;
-- Chỉ SOURCE một lần khi mapping đã đúng và V002 chưa được áp:
SOURCE src/main/resources/db/migration/V002__vnpay_sandbox.sql;
SHOW CREATE TABLE vnpay_attempts;
```

Không auto baseline schema đang có dữ liệu. Sau nâng cấp thủ công, Flyway migrate vẫn từ chối schema không có history; cần một lượt mapping/adoption lịch sử riêng trước khi chuyển schema này sang Flyway. Không tự thêm/sửa dòng history để giả rằng V001/V002 đã chạy qua Flyway. Nếu DDL lỗi giữa chừng, khảo sát trạng thái từng ALTER/TABLE rồi lập sửa bổ sung; không SOURCE lại mù quáng. DatabaseTool `check` vẫn dùng được.

## 3. Build/deploy

```powershell
$env:APP_CONFIG_FILE = (Resolve-Path .\config\application.local.properties).Path
.\mvnw.cmd -B verify
.\mvnw.cmd -B exec:java '-Dexec.args=check'
```

WAR: `target/demo-1.0-SNAPSHOT.war`. IntelliJ: rebuild artifact, **Stop rồi Run Tomcat lại** với cùng file cấu hình. Không chỉ refresh JSP vì các lớp Java/config đọc khi khởi động. Deploy thủ công: copy WAR vào CATALINA_BASE/webapps/c2c.war rồi restart Tomcat, sửa callback thành `/c2c`. Ứng dụng không tự migrate/seed khi deploy.

## 4. Kịch bản demo 3–5 phút

1. Đăng nhập buyer bằng tài khoản demo trong README. Sản phẩm cần PUBLIC/APPROVED và còn kho; Admin duyệt theo luồng có sẵn, không tự approve/seed lúc deploy.
2. Cho hai sản phẩm của hai seller vào giỏ → checkout **VNPAY Sandbox**. Trang kết quả có hai đơn và hai nút thanh toán, mỗi đơn thu đúng tổng tiền đã lưu.
3. Bấm nút của một đơn → VNPAY Sandbox → thẻ nội địa → NCB. Dùng thẻ **TEST chính thức**, không dùng thẻ thật:

| Trường | Thành công | Không đủ số dư |
| --- | --- | --- |
| Số thẻ NCB | `9704198526191432198` | `9704195798459170488` |
| Tên | `NGUYEN VAN A` | `NGUYEN VAN A` |
| Phát hành | `07/15` | `07/15` |
| OTP khi được yêu cầu | `123456` | Theo hướng dẫn Sandbox |

Nguồn: [thông tin thẻ TEST VNPAY](https://sandbox.vnpayment.vn/apis/vnpay-demo/) đọc ngày 05/10/2026; kiểm tra lại nguồn nếu dữ liệu test thay đổi. Có thể bấm “Hủy thanh toán” tại gateway để thử hủy. “Hủy thanh toán” không tự hủy đơn hàng hoặc hoàn kho.

4. Đồng ý điều khoản Sandbox → OTP → quay về website. Return chỉ kiểm chữ ký/merchant và hiển thị DB. Nếu IPN chưa tới: **Đang xác nhận thanh toán** là đúng, không phải đã PAID.
5. Bấm **Kiểm tra kết quả thanh toán**: backend gửi querydr, dùng CreateDate của lần pay ban đầu, kiểm chữ ký response và merchant/ref/amount/type/status. Chỉ API00 **và** TransactionStatus00 hợp lệ mới PAID. Query thất bại/kết nối/chữ ký sai giữ thanh toán nguyên trạng. Mỗi 30 giây mới được kiểm tra; gateway còn có giới hạn riêng, mã 94 cần đợi vài phút. Response lỗi API có thể không có checksum: chỉ hiển thị thông báo mã lỗi, không dùng để ghi kết quả tài chính.
6. Mở chi tiết đơn: xem mã giao dịch gateway/ngân hàng/ngày/mã kết quả và lịch sử SYSTEM. Đơn thứ hai vẫn độc lập. Seller confirm → ship (VNPAY phải PAID) → deliver; buyer complete → review như cũ.
7. Đơn failed/expired được thanh toán lại với ref mới, giữ lịch sử; bấm lặp lúc pending tái dùng cùng URL/ref. Đơn PAID/CANCELLED bị chặn trả trước mới. COD thu cùng DELIVERED như trước.

## 5. Trạng thái và xử lý bất thường

| Tình huống | Kết quả |
| --- | --- |
| Callback sai checksum/merchant | IPN97, không ghi payment |
| Không tìm thấy ref hợp lệ | IPN01 |
| Sai amount so với attempt/payment/order | IPN04, không ghi payment |
| Thành công00/00 | IPN00; attempt SUCCEEDED, payment PAID, một history SYSTEM |
| Callback terminal trùng | IPN02, không nhân history/kho |
| Thất bại/hủy xác định (TxnStatus02/11) | Attempt FAILED, payment vẫn chờ; cho thử lại |
| Chưa hoàn tất (01) | Giữ pending, không suy ra PAID từ API00 |
| Hết hạn (TxnStatus08 hoặc giờ expiry đã qua) | Create lần sau ghi EXPIRED rồi ref mới; không tự hủy đơn/hoàn kho |
| Thành công muộn sau CANCELLED | Lưu gateway; NEEDS_REVIEW + REFUND_PENDING; không khôi phục đơn/kho |
| Hủy VNPAY đã PAID hợp lệ | REFUND_PENDING, chưa hoàn tiền qua gateway; hoàn kho đúng một lần |
| Hai attempt cùng báo thành công | Thu đầu xác nhận đơn, thu sau lưu NEEDS_REVIEW; cần đối chiếu thủ công |
| 04/07 hoặc kết quả mâu thuẫn | NEEDS_REVIEW, chặn thanh toán thêm |

Chưa có màn hình đối soát/đóng NEEDS_REVIEW hoặc API refund. Admin đọc lịch sử và đối chiếu Sandbox; không sửa snapshot, không tự đổi payment bằng nút giả PAID. Order/payment/attempt là ba trạng thái riêng. Callback không chuyển đơn CONFIRMED/COMPLETED và không chạm tồn kho.

## 6. Luồng kỹ thuật để trình bày

Servlet nhận POST có session/CSRF → Service kiểm buyer sở hữu đơn → DAO/JDBI khóa actor → order → payment → attempt trên cùng Handle/transaction → lưu attempt → ký URL và redirect. IPN checksum hoặc querydr response checksum → cùng Service áp kết quả nguyên tử, khóa order/payment/attempt, ghi history SYSTEM. Return GET chỉ đọc DB. HTTP querydr chạy ngoài transaction để không giữ khóa trong lúc đợi mạng.

Ký pay/callback: tham số sort tăng, encode US_ASCII một lần theo Java sample, HMACSHA512, không gửi SecureHashType. Ký querydr: thứ tự field với dấu `|`, không dùng canonical URL. Money BigDecimal ×100 chính xác; thời gian VNPAY Asia/Ho_Chi_Minh, expiry lưu UTC theo cấu hình JDBC. Client không quyết amount/owner/status.

Nguồn chính thức: [pay/IPN](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html), [ký HMAC và Java sample](https://sandbox.vnpayment.vn/apis/docs/chuyen-doi-thuat-toan/changeTypeHash.html), [querydr](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr&refund.html). Kết quả kiểm tra thực tế và danh sách file ở [báo cáo](reports/VNPAY-sandbox.md).

Bảng trạng thái chi tiết còn có 08 (quá thời gian) và 11 (bị hủy) ở mục 2.5.7.2 của [Techspec PAY 2.1.0 chính thức](https://sandbox.vnpayment.vn/apis/files/VNPAY%20Payment%20Gateway_Techspec%20Post%20method%202.1.0-VN.pdf), trang 36. Sandbox thực tế đã trả 11 khi xác nhận hủy; Service ánh xạ thành FAILED để cho phép thử lại sau xác minh.
