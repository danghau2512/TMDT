-- Chỉ tạo database tên riêng cho dự án; không xóa/sửa database khác.
-- Quyền CREATE cần tài khoản setup. Kiểm tra trước nếu c2c_demo đã tồn tại.
CREATE DATABASE IF NOT EXISTS c2c_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
