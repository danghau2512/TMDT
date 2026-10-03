# 01 — Khảo sát repository và môi trường

## Bằng chứng trong dự án

Thư mục làm việc: `C:\Users\hausi\OneDrive\Desktop\TMDT\project\web\demo`.

| Thành phần | Hiện trạng đã đọc |
| --- | --- |
| `pom.xml` | `com.example:demo:1.0-SNAPSHOT`, packaging WAR, source/target Java 17; Servlet API **6.1.0** scope provided; JUnit API/Engine **5.13.2** scope test; war-plugin **3.4.0** |
| `.mvn/wrapper/maven-wrapper.properties` | Maven Wrapper trỏ Maven **3.9.6**, wrapper **3.3.0**; có wrapper JAR và script Windows/Unix |
| `src/main/java/com/example/demo/HelloServlet.java` | Một Servlet `jakarta.servlet`, GET `/hello-servlet`, trả Hello World; không Service/DAO/Model |
| `src/main/webapp/index.jsp` | Hello World và liên kết tới Servlet; chưa có CSS/JS/Bootstrap hoặc giao diện thương mại |
| `src/main/webapp/WEB-INF/web.xml` | Jakarta web-app **6.0** |
| `.idea/misc.xml` | SDK tên `17`, languageLevel JDK_17 |
| `.idea/workspace.xml` | Run configuration tên **Tomcat 10.1.48**, artifact `demo:war exploded`, context `/demo_war_exploded`; không có đường dẫn server installation |
| `.gitignore` | Bỏ qua target và một số metadata IDE; chưa có quy tắc cho secret/upload của ứng dụng |
| Dữ liệu và nghiệp vụ | Không thấy SQL/migration, JDBC URL, JDBI, MySQL Connector, tài khoản, giỏ hàng, đơn, ảnh, test source hoặc docs/AGENTS trước lượt này |
| Git | `git status --short` và `git rev-parse --show-toplevel` đều trả “not a git repository”; đây là thư mục dự án Maven, chưa xác minh được lịch sử Git |

Đã liệt kê cả file ẩn, đọc các file nguồn/cấu hình liên quan và kiểm tra AGENTS.md ở các thư mục cha; không tìm thấy quy ước cha cần kế thừa.

## Môi trường đã xác minh

| Hạng mục | Kết quả | Giới hạn |
| --- | --- | --- |
| Java/Javac | Oracle JDK **17.0.12**, JAVA_HOME `C:\Program Files\Java\jdk-17` | Phiên bản cài hiện tại; không tự cập nhật |
| Maven PATH | **3.9.14**, chạy với Java 17 | Khác wrapper; dùng wrapper để nhóm tái lập build |
| Maven Wrapper | `./mvnw.cmd -version`: **3.9.6** | Đã chạy thành công |
| Tomcat | IDE ghi tên 10.1.48 | Chưa chạy `version.bat`, chưa deploy/HTTP; tên cấu hình không chứng minh runtime đang chạy đúng phiên bản |
| MySQL CLI | `mysql --version` không tìm thấy trên PATH | Không đồng nghĩa máy chưa cài MySQL |
| Dịch vụ DB | `wampmysqld64` và `wampmariadb64` đều Running | Hai engine khác nhau; dự án yêu cầu **MySQL**, không tự chuyển sang MariaDB |
| Binary MySQL | Service path `C:\wamp64\bin\mysql\mysql9.1.0\bin\mysqld.exe`; `mysqld.exe --version` trả **MySQL Community Server 9.1.0** | Chỉ xác minh binary của service, chưa lấy `SELECT VERSION()` qua kết nối |
| MySQL client ngoài PATH | Có `C:\wamp64\bin\mysql\mysql9.1.0\bin\mysql.exe` | Chưa dùng credential hoặc truy cập database |

Không có URL/schema/tài khoản DB trong repository. Vì chưa kết nối, **chưa biết database ngoài repository có dữ liệu hay không**; không tạo, sửa hoặc xóa schema nào.

## Vấn đề phiên bản và bộ công nghệ đề xuất

Servlet API 6.1.0 hiện tại không khớp runtime mục tiêu theo IDE. Tomcat 10.1 thực hiện Servlet 6.0/Pages 3.1; Tomcat 11 thực hiện Servlet 6.1 và yêu cầu Java 17. Đây là lệch hợp đồng compile/runtime, dù Servlet mẫu có thể chưa dùng API mới. [Tài liệu Tomcat 10.1](https://tomcat.apache.org/tomcat-10.1-doc/), [Migration Tomcat 11](https://tomcat.apache.org/migration-11.0.html).

| Hạng mục | Chọn cho lượt triển khai | Hành động M1 |
| --- | --- | --- |
| JDK | **17 LTS**, dùng bản vá 17 được nhóm thống nhất | Giữ target 17; pin compiler plugin/release 17 |
| Container | **Tomcat 10.1.x** | Xác minh installation/version thực tế; thống nhất bản vá cùng nhóm |
| Servlet | `jakarta.servlet-api` **6.0.0**, provided; web.xml **6.0** | Sửa pom từ 6.1.0 xuống 6.0.0; không dùng javax.servlet |
| JSP/JSTL | JSP **3.1** do Tomcat cung cấp; Jakarta Tags/JSTL **3.0.x** | Thêm API và implementation JSTL; dùng tag URI `jakarta.tags.core` |
| Database | **MySQL 8.4 LTS**, InnoDB, utf8mb4 | Đây là đề xuất môi trường tái lập, chưa cài. WAMP 9.1.0 hiện có có thể dùng schema demo/test riêng sau xác minh; không hạ cấp DB tại chỗ |
| JDBI | `jdbi3-core` và `jdbi3-sqlobject` **3.55.0** | Pin cùng phiên bản, Java 17, cài SqlObjectPlugin; kiểm tra dependency/build ở M1 |
| JDBC/pool | `com.mysql:mysql-connector-j` và HikariCP bản hỗ trợ Java 17/MySQL mục tiêu | Kiểm tra tài liệu, pin bản cụ thể khi thêm dependency; hiện chưa có dependency này |
| Build/test | Wrapper Maven **3.9.6**, JUnit **5.13.2** | Pin compiler/surefire để không phụ thuộc default khác nhau giữa Maven PATH và wrapper |

JDBI 3.55.0 có tài liệu chính thức cho Java 17 và transaction qua Handle; JSTL 3.0 có API/implementation riêng. [JDBI 3.55.0](https://jdbi.org/releases/3.55.0/), [Jakarta Tags 3.0](https://jakarta.ee/specifications/tags/3.0/). Nhánh MySQL 8.4 có [release notes chính thức](https://dev.mysql.com/doc/relnotes/mysql/8.4/en/). Không tuyên bố các dependency đề xuất đã được chạy cùng nhau.

Nếu nhóm quyết định giữ Servlet 6.1.0 thì phải chuyển đồng bộ sang Tomcat 11/web.xml 6.1/JSP tương ứng; đó là phương án thay thế, không trộn hai bộ cấu hình. Phương án chính giữ Tomcat 10.1 để sát cấu hình hiện có.

## Build ban đầu

`mvn -B -DskipTests=false verify` và `./mvnw.cmd -B verify` đều **BUILD SUCCESS**, tạo `target/demo-1.0-SNAPSHOT.war`. Không có test source nên chưa có test nghiệp vụ nào được chạy. Build không xác minh Servlet/JSP hoạt động trong Tomcat, không xác minh kết nối DB hoặc đồng thời tồn kho.

Chỉ tạo AGENTS/docs trong lượt này; không đổi pom, Java/JSP/web.xml, wrapper, cấu hình IDE hay dữ liệu DB. `target/` là đầu ra build tự sinh.
