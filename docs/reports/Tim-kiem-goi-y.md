# Gợi ý sản phẩm khi nhập từ khóa — 05/10/2026

Ô tìm kiếm trên header TraoTay đã có dropdown bên dưới: ảnh, tên và giá tiền Việt Nam. Bấm một dòng hoặc dùng ↑/↓ rồi Enter để mở trang chi tiết; Escape/bấm ngoài đóng dropdown. Có liên kết “Xem tất cả kết quả”, trạng thái đang tìm/chưa có kết quả/lỗi kết nối. Nút Tìm kiếm và form HTML vẫn hoạt động khi JavaScript bị tắt hoặc tải gợi ý thất bại.

## Luồng và giới hạn

`search-suggestions.js → GET /products/suggestions?keyword=... → SearchSuggestionsServlet → ProductService.suggestions → CatalogDao.suggestions/JDBI → MySQL`.

- Trim từ khóa, tối đa 100 ký tự theo giới hạn catalog hiện có; rỗng trả `products: []`. Client chờ 250 ms sau khi gõ, xử lý nhập tiếng Việt/IME, hủy request cũ và bỏ phản hồi không còn đúng từ khóa; timeout 8 giây.
- Tối đa 8 sản phẩm, ưu tiên tên bắt đầu từ khóa, rồi tên chứa từ khóa, sau đó mô tả; trong mỗi nhóm ưu tiên tin mới. Tái sử dụng tìm literal bằng LOCATE như catalog, bind `:keyword`; `%`/`_` không thành wildcard. Collation MySQL hiện có hỗ trợ tìm có/không dấu, đã kiểm trên schema test; không đổi charset/collation.
- Điều kiện chung `CatalogDao.PUBLIC`: PUBLIC + APPROVED, seller ACTIVE, category ACTIVE. Không cho admin/seller xem tin riêng qua API công khai này. Chi tiết/ảnh vẫn qua endpoint kiểm quyền hiện có.
- DTO `ProductSuggestion` chỉ có `id`, `title`, `price`, `imagePath`, `productPath`; không trả contact/rejection_reason/p.*. ID string tránh mất chính xác trên JS; giá BigDecimal từ server, chỉ định dạng bằng Intl trên trình duyệt.
- Ảnh đầu tiên theo sort_order, thiếu/lỗi dùng placeholder cục bộ. URL nối với context path, không hardcode `/demo_war_exploded`. Tên và thông báo dựng bằng textContent, không innerHTML. Dropdown combobox/listbox có aria-expanded/activedescendant/live announcement; phần announcement được clip riêng, không làm thay đổi chiều rộng ô tìm kiếm.
- Chỉ GET đọc, không đổi login/CSRF/quyền/giỏ hàng và không ghi database. Không cần migration/seed.

## File tạo/sửa

| File từ gốc repository | Vai trò |
| --- | --- |
| `src/main/java/com/example/demo/controller/catalog/SearchSuggestionsServlet.java` | GET JSON UTF-8 và lỗi 400/503 |
| `src/main/java/com/example/demo/model/ProductSuggestion.java` | DTO công khai |
| `src/main/java/com/example/demo/service/ProductService.java` | Validate từ khóa, Handle qua Database.read, map DTO |
| `src/main/java/com/example/demo/dao/CatalogDao.java` | Truy vấn công khai có bind, giới hạn 8 |
| `src/main/java/com/example/demo/controller/AccountSupport.java` | Giữ lỗi phát sinh từ Filter là JSON cho đúng endpoint này |
| `src/main/webapp/WEB-INF/views/layouts/navigation.jspf` | Combobox/dropdown trong header chung |
| `src/main/webapp/WEB-INF/views/layouts/shop-start.jspf` | Nạp CSS/JS local version `20261005-search1` |
| `src/main/webapp/assets/js/search-suggestions.js` | AJAX/debounce/cancel/render/chọn bằng bàn phím |
| `src/main/webapp/assets/css/search-suggestions.css` | Desktop/mobile, typography và announcement dành cho screen reader |
| `docs/screenshots/search/` | Ảnh fixture test thực tế 1366/390 |
| `README.md`, `docs/README.md`, `docs/02-yeu-cau-va-quy-tac.md`, `docs/03-kien-truc.md`, `AGENTS.md` và báo cáo này | Hướng dẫn và quyết định |

## Kiểm tra thực tế

- `./mvnw.cmd -B verify`: BUILD SUCCESS, **28 unit tests**, 0 failure/error/skip, WAR cuối lúc 22:22 ngày 05/10/2026. Không thêm bộ unit test chỉ lặp lại implementation.
- Helper Java chỉ đọc trên MySQL/InnoDB riêng 13316, `c2c_upgrades_test`, kiểm `C2C_IT_ALLOWED`/endpoint/schema: từ khóa rỗng, quá dài, có/không dấu, tối đa 8, literal injection/wildcard, DTO và đường dẫn; đối chiếu điều kiện public, loại **15 tin fixture không công khai**. Đạt. Không chạy SQL đổi dữ liệu/schema, không cần backup mới cho thao tác chỉ đọc.
- Deploy WAR lên Tomcat 10.1.48 riêng cổng 18080/context `/c2c`; Chrome thật headless 1366×980 và 390×844. API anonymous/JSON UTF-8, field allowlist, empty/oversize/injection; ảnh tải thành công, tên/giá, debounce một request khi gõ nhanh; click/↑↓/Enter/Escape, không kết quả/xóa từ khóa, xem tất cả và form tìm kiếm thường đều đạt.
- Dùng response mock có delay để kiểm phản hồi cũ không ghi đè; mock tên chứa HTML để kiểm textContent không thực thi; ngắt riêng request gợi ý để kiểm thông báo tiếng Việt và nút Tìm kiếm vẫn dùng được. Những trường hợp mock này không được ghi vào DB. Tìm kiếm thường khi tắt JS đạt.
- Sau xem ảnh đầu, sửa CSS cho live announcement không chiếm diện tích input; rebuild/redeploy rồi chạy lại kiểm tra, thêm assertion chiều rộng/chiều cao input và announcement 1 px. **6 nhóm browser kiểm tra đạt, không pageerror**; đã xem cả hai PNG cuối.

Ảnh: [Desktop 1366](../screenshots/search/suggestions-1366.png), [mobile 390](../screenshots/search/suggestions-390.png). Đây là fixture test, có thể có nhiều tin khác ID trùng tên do các lượt kiểm thử trước; truy vấn không nhân bản một sản phẩm theo số ảnh.

Giới hạn: chưa kiểm tải lớn, mọi trình duyệt hoặc toàn bộ luồng nghiệp vụ. Với quy mô đồ án dùng LOCATE có giới hạn là phù hợp; dataset lớn cần đánh giá index/full-text riêng. Không tự restart Tomcat IDE 8080; các tiến trình thử nghiệm được dừng sau kiểm tra.

## Build và demo

```powershell
.\mvnw.cmd -B verify
```

Stop Tomcat, Rebuild artifact `demo:war exploded` trong IntelliJ, rồi Run Tomcat 10.1 và Ctrl+F5. Hoặc deploy `target/demo-1.0-SNAPSHOT.war` bằng quy trình hiện có; giữ file cấu hình DB/storage và context. **Không chạy SQL/migrate/seed**.

Mở trang chủ, gõ một phần tên của sản phẩm đã được duyệt công khai trong DB của bạn. Gợi ý có ảnh/tên/giá; bấm vào dòng để xem chi tiết. Nếu không có sản phẩm public khớp thì hiện “Chưa tìm thấy sản phẩm phù hợp.”. Có thể dùng ↑/↓ + Enter hoặc “Xem tất cả kết quả”.

Commit gợi ý: `feat: thêm gợi ý tìm kiếm sản phẩm với ảnh và giá`.
