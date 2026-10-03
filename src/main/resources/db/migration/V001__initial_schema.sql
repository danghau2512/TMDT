-- Schema mới, MySQL >= 8.4, InnoDB. Chỉ chạy trên schema trống dành riêng dự án.
-- Không DROP/IF NOT EXISTS: không âm thầm chấp nhận schema cũ lệch thiết kế.
-- V001__initial_schema.sql là bản migration giống file này tại M1.
SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NULL,
    public_contact VARCHAR(255) NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER','ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE categories (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_categories_slug UNIQUE (slug),
    CONSTRAINT ck_categories_status CHECK (status IN ('ACTIVE','INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE media_assets (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uploaded_by BIGINT UNSIGNED NOT NULL,
    storage_key VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(64) NOT NULL,
    byte_size BIGINT UNSIGNED NOT NULL,
    sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    width INT UNSIGNED NOT NULL,
    height INT UNSIGNED NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_assets_key UNIQUE (storage_key),
    CONSTRAINT fk_assets_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id),
    CONSTRAINT ck_assets_size CHECK (byte_size > 0 AND width > 0 AND height > 0),
    CONSTRAINT ck_assets_mime CHECK (mime_type IN ('image/jpeg','image/png','image/webp')),
    CONSTRAINT ck_assets_purpose CHECK (purpose IN ('PRODUCT_IMAGE','COMPLAINT_EVIDENCE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE products (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT UNSIGNED NOT NULL,
    category_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    price DECIMAL(20,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    condition_code VARCHAR(32) NOT NULL,
    stock_quantity INT UNSIGNED NOT NULL DEFAULT 0,
    visibility VARCHAR(32) NOT NULL DEFAULT 'HIDDEN',
    moderation_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    listing_version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    stock_version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT ck_products_price CHECK (price > 0 AND price <= 999999999999 AND price = ROUND(price,0)),
    CONSTRAINT ck_products_currency CHECK (currency = 'VND'),
    CONSTRAINT ck_products_condition CHECK (condition_code IN ('NEW','LIKE_NEW','USED')),
    CONSTRAINT ck_products_visibility CHECK (visibility IN ('PUBLIC','HIDDEN')),
    CONSTRAINT ck_products_moderation CHECK (moderation_status IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT ck_products_featured CHECK (is_featured IN (0,1)),
    CONSTRAINT ck_products_versions CHECK (listing_version > 0 AND stock_version > 0),
    INDEX ix_products_seller (seller_id,updated_at,id),
    INDEX ix_products_public (visibility,moderation_status,created_at,id),
    INDEX ix_products_category_price (category_id,visibility,moderation_status,price,id),
    INDEX ix_products_featured (is_featured,visibility,moderation_status,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_images (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT UNSIGNED NOT NULL,
    asset_id BIGINT UNSIGNED NOT NULL,
    sort_order INT UNSIGNED NOT NULL,
    alt_text VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_product_images_asset FOREIGN KEY (asset_id) REFERENCES media_assets(id),
    CONSTRAINT uq_product_images_order UNIQUE (product_id,sort_order),
    CONSTRAINT uq_product_images_asset UNIQUE (product_id,asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_moderation_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT UNSIGNED NOT NULL,
    actor_id BIGINT UNSIGNED NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    reason TEXT NULL,
    listing_version BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_moderation_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_moderation_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT ck_moderation_from CHECK (from_status IS NULL OR from_status IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT ck_moderation_to CHECK (to_status IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT ck_moderation_reason CHECK (to_status <> 'REJECTED' OR (reason IS NOT NULL AND CHAR_LENGTH(TRIM(reason)) > 0)),
    INDEX ix_moderation_time (product_id,created_at,id),
    INDEX ix_moderation_version (product_id,listing_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE carts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT ck_carts_version CHECK (version > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cart_items (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    quantity INT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_cart_items_product UNIQUE (cart_id,product_id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id),
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity BETWEEN 1 AND 999)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE checkout_batches (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    batch_code CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    buyer_id BIGINT UNSIGNED NOT NULL,
    idempotency_key CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_batches_code UNIQUE (batch_code),
    CONSTRAINT uq_batches_request UNIQUE (buyer_id,idempotency_key),
    CONSTRAINT uq_batches_buyer UNIQUE (id,buyer_id),
    CONSTRAINT fk_batches_buyer FOREIGN KEY (buyer_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE orders (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_code CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    checkout_batch_id BIGINT UNSIGNED NOT NULL,
    buyer_id BIGINT UNSIGNED NOT NULL,
    seller_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    shipping_address VARCHAR(500) NOT NULL,
    buyer_name_snapshot VARCHAR(100) NOT NULL,
    seller_name_snapshot VARCHAR(100) NOT NULL,
    seller_contact_snapshot VARCHAR(255) NULL,
    subtotal DECIMAL(20,2) NOT NULL,
    shipping_fee DECIMAL(20,2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(20,2) NOT NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    completed_at DATETIME(6) NULL,
    cancelled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_orders_code UNIQUE (order_code),
    CONSTRAINT uq_orders_batch_seller UNIQUE (checkout_batch_id,seller_id),
    CONSTRAINT uq_orders_buyer UNIQUE (id,buyer_id),
    CONSTRAINT fk_orders_batch_buyer FOREIGN KEY (checkout_batch_id,buyer_id) REFERENCES checkout_batches(id,buyer_id),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    CONSTRAINT fk_orders_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT ck_orders_participants CHECK (buyer_id <> seller_id),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING','CONFIRMED','SHIPPED','DELIVERED','COMPLETED','CANCELLED')),
    CONSTRAINT ck_orders_currency CHECK (currency = 'VND'),
    CONSTRAINT ck_orders_total CHECK (subtotal > 0 AND shipping_fee >= 0 AND grand_total = subtotal + shipping_fee),
    CONSTRAINT ck_orders_terminal CHECK ((status = 'COMPLETED' AND completed_at IS NOT NULL AND cancelled_at IS NULL)
        OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND completed_at IS NULL)
        OR (status NOT IN ('COMPLETED','CANCELLED') AND completed_at IS NULL AND cancelled_at IS NULL)),
    INDEX ix_orders_buyer_time (buyer_id,created_at,id),
    INDEX ix_orders_seller_status (seller_id,status,created_at,id),
    INDEX ix_orders_status_time (status,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_items (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    product_listing_version BIGINT UNSIGNED NOT NULL,
    product_name_snapshot VARCHAR(200) NOT NULL,
    description_snapshot TEXT NOT NULL,
    condition_snapshot VARCHAR(32) NOT NULL,
    category_name_snapshot VARCHAR(100) NOT NULL,
    unit_price DECIMAL(20,2) NOT NULL,
    quantity INT UNSIGNED NOT NULL,
    line_total DECIMAL(20,2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_order_items_product UNIQUE (order_id,product_id),
    CONSTRAINT uq_order_items_order UNIQUE (id,order_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_order_items_price CHECK (unit_price > 0 AND unit_price = ROUND(unit_price,0)),
    CONSTRAINT ck_order_items_quantity CHECK (quantity BETWEEN 1 AND 999),
    CONSTRAINT ck_order_items_total CHECK (line_total = unit_price * quantity),
    CONSTRAINT ck_order_items_condition CHECK (condition_snapshot IN ('NEW','LIKE_NEW','USED')),
    INDEX ix_order_items_product (product_id,order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_item_images (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT UNSIGNED NOT NULL,
    asset_id BIGINT UNSIGNED NOT NULL,
    sort_order INT UNSIGNED NOT NULL,
    alt_text_snapshot VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_item_images_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT fk_item_images_asset FOREIGN KEY (asset_id) REFERENCES media_assets(id),
    CONSTRAINT uq_item_images_order UNIQUE (order_item_id,sort_order),
    CONSTRAINT uq_item_images_asset UNIQUE (order_item_id,asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_status_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    actor_id BIGINT UNSIGNED NOT NULL,
    actor_role_snapshot VARCHAR(32) NOT NULL,
    reason TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_order_history_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_history_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT ck_order_history_from CHECK (from_status IS NULL OR from_status IN ('PENDING','CONFIRMED','SHIPPED','DELIVERED','COMPLETED','CANCELLED')),
    CONSTRAINT ck_order_history_to CHECK (to_status IN ('PENDING','CONFIRMED','SHIPPED','DELIVERED','COMPLETED','CANCELLED')),
    CONSTRAINT ck_order_history_role CHECK (actor_role_snapshot IN ('USER','ADMIN')),
    INDEX ix_order_history_time (order_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    method VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(20,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    is_simulated BOOLEAN NOT NULL DEFAULT TRUE,
    reference_code CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    paid_at DATETIME(6) NULL,
    refunded_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_payments_order UNIQUE (order_id),
    CONSTRAINT uq_payments_reference UNIQUE (reference_code),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT ck_payments_method CHECK (method IN ('COD','BANK_TRANSFER_SIMULATED')),
    CONSTRAINT ck_payments_status CHECK (status IN ('UNPAID','PENDING_CONFIRMATION','PAID','VOIDED','REFUND_SIMULATED')),
    CONSTRAINT ck_payments_amount CHECK (amount > 0 AND currency = 'VND' AND is_simulated = 1),
    CONSTRAINT ck_payments_time CHECK ((status = 'PAID' AND paid_at IS NOT NULL AND refunded_at IS NULL)
        OR (status = 'REFUND_SIMULATED' AND paid_at IS NOT NULL AND refunded_at IS NOT NULL)
        OR (status IN ('UNPAID','PENDING_CONFIRMATION','VOIDED') AND paid_at IS NULL AND refunded_at IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE payment_status_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT UNSIGNED NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    actor_id BIGINT UNSIGNED NOT NULL,
    actor_role_snapshot VARCHAR(32) NOT NULL,
    note TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_payment_history_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT fk_payment_history_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT ck_payment_history_from CHECK (from_status IS NULL OR from_status IN ('UNPAID','PENDING_CONFIRMATION','PAID','VOIDED','REFUND_SIMULATED')),
    CONSTRAINT ck_payment_history_to CHECK (to_status IN ('UNPAID','PENDING_CONFIRMATION','PAID','VOIDED','REFUND_SIMULATED')),
    CONSTRAINT ck_payment_history_role CHECK (actor_role_snapshot IN ('USER','ADMIN')),
    INDEX ix_payment_history_time (payment_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE stock_movements (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT UNSIGNED NOT NULL,
    order_item_id BIGINT UNSIGNED NULL,
    movement_type VARCHAR(32) NOT NULL,
    quantity_delta BIGINT NOT NULL,
    quantity_before INT UNSIGNED NOT NULL,
    quantity_after INT UNSIGNED NOT NULL,
    actor_id BIGINT UNSIGNED NOT NULL,
    reason TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_stock_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT fk_stock_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT uq_stock_item_type UNIQUE (order_item_id,movement_type),
    CONSTRAINT ck_stock_balance CHECK (CAST(quantity_after AS SIGNED) = CAST(quantity_before AS SIGNED) + quantity_delta),
    CONSTRAINT ck_stock_type CHECK (
        (movement_type = 'INITIAL' AND order_item_id IS NULL AND quantity_before = 0 AND quantity_delta >= 0)
        OR (movement_type = 'ADJUSTMENT' AND order_item_id IS NULL AND quantity_delta <> 0 AND reason IS NOT NULL)
        OR (movement_type = 'ORDER_HOLD' AND order_item_id IS NOT NULL AND quantity_delta < 0)
        OR (movement_type = 'CANCEL_RELEASE' AND order_item_id IS NOT NULL AND quantity_delta > 0)),
    INDEX ix_stock_product_time (product_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reviews (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT UNSIGNED NOT NULL,
    order_id BIGINT UNSIGNED NOT NULL,
    buyer_id BIGINT UNSIGNED NOT NULL,
    product_rating TINYINT UNSIGNED NOT NULL,
    seller_rating TINYINT UNSIGNED NOT NULL,
    comment VARCHAR(2000) NOT NULL,
    visibility VARCHAR(32) NOT NULL DEFAULT 'VISIBLE',
    hidden_reason VARCHAR(500) NULL,
    hidden_by BIGINT UNSIGNED NULL,
    hidden_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_reviews_item UNIQUE (order_item_id),
    CONSTRAINT fk_reviews_item_order FOREIGN KEY (order_item_id,order_id) REFERENCES order_items(id,order_id),
    CONSTRAINT fk_reviews_order_buyer FOREIGN KEY (order_id,buyer_id) REFERENCES orders(id,buyer_id),
    CONSTRAINT fk_reviews_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    CONSTRAINT fk_reviews_hidden_by FOREIGN KEY (hidden_by) REFERENCES users(id),
    CONSTRAINT ck_reviews_rating CHECK (product_rating BETWEEN 1 AND 5 AND seller_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_visibility CHECK ((visibility = 'VISIBLE' AND hidden_by IS NULL AND hidden_at IS NULL AND hidden_reason IS NULL)
        OR (visibility = 'HIDDEN' AND hidden_by IS NOT NULL AND hidden_at IS NOT NULL AND hidden_reason IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE complaints (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    buyer_id BIGINT UNSIGNED NOT NULL,
    order_item_id BIGINT UNSIGNED NULL,
    reason_code VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    assigned_admin_id BIGINT UNSIGNED NULL,
    resolution_code VARCHAR(32) NULL,
    resolution_summary TEXT NULL,
    resolved_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_complaints_order UNIQUE (order_id),
    CONSTRAINT fk_complaints_order_buyer FOREIGN KEY (order_id,buyer_id) REFERENCES orders(id,buyer_id),
    CONSTRAINT fk_complaints_item_order FOREIGN KEY (order_item_id,order_id) REFERENCES order_items(id,order_id),
    CONSTRAINT fk_complaints_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    CONSTRAINT fk_complaints_admin FOREIGN KEY (assigned_admin_id) REFERENCES users(id),
    CONSTRAINT ck_complaints_reason CHECK (reason_code IN ('NOT_AS_DESCRIBED','DAMAGED','NOT_RECEIVED','PAYMENT','OTHER')),
    CONSTRAINT ck_complaints_status CHECK (status IN ('RECEIVED','PROCESSING','RESOLVED')),
    CONSTRAINT ck_complaints_resolution CHECK (resolution_code IS NULL OR resolution_code IN ('SELLER_CONTACTED','ORDER_CANCELLED','NO_ACTION','OTHER')),
    CONSTRAINT ck_complaints_resolved CHECK ((status = 'RESOLVED' AND resolution_code IS NOT NULL AND resolution_summary IS NOT NULL AND resolved_at IS NOT NULL)
        OR (status <> 'RESOLVED' AND resolution_code IS NULL AND resolution_summary IS NULL AND resolved_at IS NULL)),
    INDEX ix_complaints_status_time (status,created_at,id),
    INDEX ix_complaints_buyer_time (buyer_id,created_at,id),
    INDEX ix_complaints_admin_status (assigned_admin_id,status,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE complaint_evidence (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT UNSIGNED NOT NULL,
    asset_id BIGINT UNSIGNED NOT NULL,
    uploaded_by BIGINT UNSIGNED NOT NULL,
    caption VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_evidence_asset UNIQUE (complaint_id,asset_id),
    CONSTRAINT fk_evidence_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id),
    CONSTRAINT fk_evidence_asset FOREIGN KEY (asset_id) REFERENCES media_assets(id),
    CONSTRAINT fk_evidence_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id),
    INDEX ix_evidence_time (complaint_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE complaint_messages (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT UNSIGNED NOT NULL,
    author_id BIGINT UNSIGNED NOT NULL,
    author_role_snapshot VARCHAR(32) NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    body TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_messages_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id),
    CONSTRAINT fk_messages_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT ck_messages_role CHECK (author_role_snapshot IN ('USER','ADMIN')),
    CONSTRAINT ck_messages_type CHECK (message_type IN ('BUYER_MESSAGE','ADMIN_RESPONSE','RESOLUTION','REOPEN')),
    INDEX ix_messages_time (complaint_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE complaint_status_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT UNSIGNED NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    actor_id BIGINT UNSIGNED NOT NULL,
    reason TEXT NULL,
    resolution_code_snapshot VARCHAR(32) NULL,
    resolution_summary_snapshot TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_complaint_history_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id),
    CONSTRAINT fk_complaint_history_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT ck_complaint_history_from CHECK (from_status IS NULL OR from_status IN ('RECEIVED','PROCESSING','RESOLVED')),
    CONSTRAINT ck_complaint_history_to CHECK (to_status IN ('RECEIVED','PROCESSING','RESOLVED')),
    CONSTRAINT ck_complaint_history_resolution CHECK (resolution_code_snapshot IS NULL OR resolution_code_snapshot IN ('SELLER_CONTACTED','ORDER_CANCELLED','NO_ACTION','OTHER')),
    INDEX ix_complaint_history_time (complaint_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_audit_log (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT UNSIGNED NOT NULL,
    action VARCHAR(64) NOT NULL,
    product_id BIGINT UNSIGNED NULL,
    order_id BIGINT UNSIGNED NULL,
    complaint_id BIGINT UNSIGNED NULL,
    review_id BIGINT UNSIGNED NULL,
    reason VARCHAR(1000) NOT NULL,
    before_data JSON NULL,
    after_data JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT fk_audit_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_audit_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_audit_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id),
    CONSTRAINT fk_audit_review FOREIGN KEY (review_id) REFERENCES reviews(id),
    CONSTRAINT ck_audit_target CHECK ((product_id IS NOT NULL) + (order_id IS NOT NULL) + (complaint_id IS NOT NULL) + (review_id IS NOT NULL) = 1),
    CONSTRAINT ck_audit_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    INDEX ix_audit_actor_time (actor_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
