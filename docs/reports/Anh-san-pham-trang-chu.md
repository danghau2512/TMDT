# Ảnh sản phẩm trang chủ — 05/10/2026

Nguyên nhân: khu sản phẩm mới đăng dùng 4 cột, khung ảnh 4:3 và `object-fit: cover`. Ảnh gốc vuông bị cắt phần trên/dưới; ở desktop 1366 px khung đo thực tế khoảng 286×214 px.

Đã bọc khu sản phẩm mới đăng bằng `.home-products`, dùng khung vuông 1:1 với `contain`, căn giữa và padding nhẹ. Desktop 3 cột, tablet 2 cột, điện thoại ≤480 px 1 cột. Giữ tỷ lệ ảnh, tên/giá/stock tag, nút so sánh và link chi tiết. Chỉ áp dụng trang chủ, không đổi CSS danh sách catalog/chi tiết/logo, không xử lý lại file ảnh hoặc sửa dữ liệu.

File sửa:

- `src/main/webapp/WEB-INF/views/home.jsp`: wrapper cho phạm vi styling.
- `src/main/webapp/assets/css/traotay.css`: kích thước, contain và responsive.
- `src/main/webapp/WEB-INF/views/layouts/shop-start.jspf`: CSS version `20261005-home-images1` để tải bản mới.
- `README.md`, `docs/README.md`, `AGENTS.md` và báo cáo này: hướng dẫn/ghi nhận kết quả.

Kiểm tra thực tế:

- `./mvnw.cmd -B verify`: BUILD SUCCESS, 28 unit tests đạt, WAR cuối lúc 22:39 ngày 05/10/2026.
- Chrome thật headless đọc trang chủ trên Tomcat IDE 8080 sau build cập nhật exploded resources, chỉ GET, không login/POST/SQL ghi. 1366 px: 3 cột, khung khoảng **387×387**; 900 px: 2 cột, **406×406**; 390 px: 1 cột, **356×356**. Không tràn ngang, ảnh tải được và computed fit là contain; nút cùng hàng căn thẳng, bấm ảnh mở đúng chi tiết. Catalog vẫn dùng styling cũ. Không pageerror; đã xem ảnh desktop/mobile thực tế.
- Ảnh preview đọc từ sản phẩm thực tế giữ tại `.local-test/home-photo-1366.png`, `home-photo-900.png`, `home-photo-390.png`; ảnh toàn grid cũng ở `.local-test/`, không đưa ảnh/dữ liệu thật vào tài liệu versioned. Chưa kiểm mọi trình duyệt hoặc luồng nghiệp vụ vì lượt này chỉ đổi CSS/JSP.

Ảnh nguồn quan sát có kích thước 400×400 và 200×200; CSS sửa khung/cách hiển thị, không bổ sung chi tiết cho ảnh nguồn độ phân giải thấp. Có thể upload ảnh rõ hơn bằng chức năng có sẵn nếu cần.

Để thấy thay đổi: Ctrl+F5 ở trang chủ. Nếu artifact của IDE chưa cập nhật, Stop Tomcat → Rebuild `demo:war exploded` → Run. Hoặc build và redeploy WAR theo README. Không cần SQL/migrate/seed.

Commit gợi ý: `style: tăng khung ảnh sản phẩm trang chủ và hiển thị trọn ảnh`.
