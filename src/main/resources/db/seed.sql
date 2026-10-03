-- Chỉ dữ liệu demo. BCrypt 2b/cost 12, salt riêng; mật khẩu demo: C2cDemo!2026.
-- Tool seed chạy toàn script trong transaction + named lock.
-- Không INSERT IGNORE/REPLACE/UPDATE: xung đột ngoài điều kiện sẽ báo lỗi và rollback.
-- ID 500001..500005 (user), 510001..510004 (category), 520001..520004 (product) dành cho seed.
-- Nếu ID bị tài nguyên khác chiếm, script thất bại thay vì ghi đè.

INSERT INTO users (id,email,password_hash,display_name,public_contact,role)
SELECT 500001,'admin@c2c.example','$2b$12$ZBPbUDMomjrN4GAwFuSu3eStiZTXjOo/Lh33eRS4KM1iIECv6rju.','Quản trị demo',NULL,'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='admin@c2c.example');
INSERT INTO users (id,email,password_hash,display_name,public_contact,role)
SELECT 500002,'seller1@c2c.example','$2b$12$5UQsxYf1VC9SRK0ay3cnq.deqzUSu2L6CeSV04zBOIXG5aX5fvNwG','Người bán An','Liên hệ người bán An qua đồ án','USER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='seller1@c2c.example');
INSERT INTO users (id,email,password_hash,display_name,public_contact,role)
SELECT 500003,'seller2@c2c.example','$2b$12$r473IYt0.ZaknepoFTEpWOr1oYqvD7beG8XV9UvOhmDotl3cFxHPy','Người bán Bình','Liên hệ người bán Bình qua đồ án','USER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='seller2@c2c.example');
INSERT INTO users (id,email,password_hash,display_name,role)
SELECT 500004,'buyer1@c2c.example','$2b$12$86rW6IGMj1Can/izPt..OepxFbwoZ64PRSCvZJW.416ijTy80ST5m','Người mua Chi','USER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='buyer1@c2c.example');
INSERT INTO users (id,email,password_hash,display_name,role)
SELECT 500005,'buyer2@c2c.example','$2b$12$ufT9J4cZWM1nGrWeHYlft.5lxsM5xG65hmLdPbxu1ovwKKgE1qNIm','Người mua Dũng','USER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='buyer2@c2c.example');

INSERT INTO categories (id,name,slug,sort_order)
SELECT 510001,'Đồ điện tử','do-dien-tu',1 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug='do-dien-tu');
INSERT INTO categories (id,name,slug,sort_order)
SELECT 510002,'Sách và học tập','sach-hoc-tap',2 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug='sach-hoc-tap');
INSERT INTO categories (id,name,slug,sort_order)
SELECT 510003,'Đồ gia dụng','do-gia-dung',3 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug='do-gia-dung');
INSERT INTO categories (id,name,slug,sort_order)
SELECT 510004,'Thời trang','thoi-trang',4 WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug='thoi-trang');

-- Chưa có ảnh thật ở M1: giữ HIDDEN/PENDING, bổ sung ảnh và kiểm duyệt ở M3.
INSERT INTO products (id,seller_id,category_id,title,description,price,condition_code,stock_quantity)
SELECT 520001,u.id,c.id,'Tai nghe demo','Tai nghe đã qua sử dụng, dùng cho dữ liệu thử nghiệm.',200000,'USED',3
FROM users u JOIN categories c ON c.slug='do-dien-tu'
WHERE u.email='seller1@c2c.example' AND NOT EXISTS (SELECT 1 FROM products WHERE id=520001);
INSERT INTO products (id,seller_id,category_id,title,description,price,condition_code,stock_quantity)
SELECT 520002,u.id,c.id,'Sách thương mại điện tử','Sách tham khảo đồ án, dữ liệu mẫu tiếng Việt.',85000,'LIKE_NEW',5
FROM users u JOIN categories c ON c.slug='sach-hoc-tap'
WHERE u.email='seller1@c2c.example' AND NOT EXISTS (SELECT 1 FROM products WHERE id=520002);
INSERT INTO products (id,seller_id,category_id,title,description,price,condition_code,stock_quantity)
SELECT 520003,u.id,c.id,'Đèn bàn học demo','Đèn bàn mới, sản phẩm mẫu của người bán thứ hai.',120000,'NEW',4
FROM users u JOIN categories c ON c.slug='do-gia-dung'
WHERE u.email='seller2@c2c.example' AND NOT EXISTS (SELECT 1 FROM products WHERE id=520003);
INSERT INTO products (id,seller_id,category_id,title,description,price,condition_code,stock_quantity)
SELECT 520004,u.id,c.id,'Balo demo','Balo đã qua sử dụng, sản phẩm độc bản để kiểm thử tồn cuối.',150000,'USED',1
FROM users u JOIN categories c ON c.slug='thoi-trang'
WHERE u.email='seller2@c2c.example' AND NOT EXISTS (SELECT 1 FROM products WHERE id=520004);

INSERT INTO stock_movements (product_id,movement_type,quantity_delta,quantity_before,quantity_after,actor_id,reason)
SELECT p.id,'INITIAL',p.stock_quantity,0,p.stock_quantity,p.seller_id,'Khởi tạo khả dụng từ dữ liệu demo'
FROM products p WHERE p.id BETWEEN 520001 AND 520004
AND ((p.id IN (520001,520002) AND p.seller_id=(SELECT id FROM users WHERE email='seller1@c2c.example'))
  OR (p.id IN (520003,520004) AND p.seller_id=(SELECT id FROM users WHERE email='seller2@c2c.example')))
AND NOT EXISTS (SELECT 1 FROM stock_movements s WHERE s.product_id=p.id AND s.movement_type='INITIAL');
INSERT INTO product_moderation_events (product_id,actor_id,from_status,to_status,reason,listing_version)
SELECT p.id,p.seller_id,NULL,'PENDING','Tin demo cần bổ sung ảnh trước khi duyệt',p.listing_version
FROM products p WHERE p.id BETWEEN 520001 AND 520004
AND ((p.id IN (520001,520002) AND p.seller_id=(SELECT id FROM users WHERE email='seller1@c2c.example'))
  OR (p.id IN (520003,520004) AND p.seller_id=(SELECT id FROM users WHERE email='seller2@c2c.example')))
AND NOT EXISTS (SELECT 1 FROM product_moderation_events e WHERE e.product_id=p.id);
