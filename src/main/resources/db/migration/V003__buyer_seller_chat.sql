-- Chat trước khi mua, độc lập order/complaint. Không thay dữ liệu/bảng cũ.
CREATE TABLE chat_conversations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT UNSIGNED NOT NULL,
    buyer_id BIGINT UNSIGNED NOT NULL,
    seller_id BIGINT UNSIGNED NOT NULL,
    product_title_snapshot VARCHAR(200) NOT NULL,
    buyer_read_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    seller_read_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_chat_product_pair UNIQUE (product_id,buyer_id,seller_id),
    CONSTRAINT fk_chat_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_chat_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    CONSTRAINT fk_chat_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT ck_chat_distinct CHECK (buyer_id <> seller_id),
    INDEX ix_chat_buyer_time (buyer_id,updated_at,id),
    INDEX ix_chat_seller_time (seller_id,updated_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE chat_messages (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT UNSIGNED NOT NULL,
    sender_id BIGINT UNSIGNED NOT NULL,
    client_nonce CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT uq_chat_message_retry UNIQUE (conversation_id,sender_id,client_nonce),
    CONSTRAINT ck_chat_body CHECK (CHAR_LENGTH(TRIM(body)) BETWEEN 1 AND 2000),
    INDEX ix_chat_message_cursor (conversation_id,id),
    INDEX ix_chat_message_unread (conversation_id,sender_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
