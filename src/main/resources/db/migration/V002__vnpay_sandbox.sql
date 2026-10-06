-- Apply once after V001; additive upgrade retains every legacy payment and history row.
ALTER TABLE payments DROP CHECK ck_payments_method, DROP CHECK ck_payments_status, DROP CHECK ck_payments_time,
 ADD CONSTRAINT ck_payments_method CHECK (method IN ('COD','BANK_TRANSFER_SIMULATED','VNPAY_SANDBOX')),
 ADD CONSTRAINT ck_payments_status CHECK (status IN ('UNPAID','PENDING_CONFIRMATION','PAID','VOIDED','REFUND_SIMULATED','REFUND_PENDING')),
 ADD CONSTRAINT ck_payments_time CHECK (
  (status IN ('PAID','REFUND_PENDING') AND paid_at IS NOT NULL AND refunded_at IS NULL)
  OR (status='REFUND_SIMULATED' AND paid_at IS NOT NULL AND refunded_at IS NOT NULL)
  OR (status IN ('UNPAID','PENDING_CONFIRMATION','VOIDED') AND paid_at IS NULL AND refunded_at IS NULL)),
 ADD CONSTRAINT ck_payments_sandbox_refund CHECK (method <> 'VNPAY_SANDBOX' OR status <> 'REFUND_SIMULATED');

ALTER TABLE payment_status_history DROP CHECK ck_payment_history_from, DROP CHECK ck_payment_history_to,
 DROP CHECK ck_payment_history_role, MODIFY actor_id BIGINT UNSIGNED NULL,
 ADD CONSTRAINT ck_payment_history_from CHECK (from_status IS NULL OR from_status IN ('UNPAID','PENDING_CONFIRMATION','PAID','VOIDED','REFUND_SIMULATED','REFUND_PENDING')),
 ADD CONSTRAINT ck_payment_history_to CHECK (to_status IN ('UNPAID','PENDING_CONFIRMATION','PAID','VOIDED','REFUND_SIMULATED','REFUND_PENDING')),
 ADD CONSTRAINT ck_payment_history_role CHECK (actor_role_snapshot IN ('USER','ADMIN','SYSTEM')),
 ADD CONSTRAINT ck_payment_history_system CHECK ((actor_id IS NULL AND actor_role_snapshot='SYSTEM') OR (actor_id IS NOT NULL AND actor_role_snapshot IN ('USER','ADMIN')));

CREATE TABLE vnpay_attempts (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 order_id BIGINT UNSIGNED NOT NULL,
 txn_ref CHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 merchant_code CHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 expected_amount DECIMAL(20,2) NOT NULL,
 status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
 vnp_create_date CHAR(14) CHARACTER SET ascii NOT NULL,
 vnp_expire_date CHAR(14) CHARACTER SET ascii NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 client_ip VARCHAR(45) CHARACTER SET ascii NOT NULL,
 return_url VARCHAR(255) NOT NULL,
 gateway_transaction_no VARCHAR(15) CHARACTER SET ascii COLLATE ascii_bin NULL,
 bank_code VARCHAR(20) CHARACTER SET ascii NULL,
 gateway_pay_date CHAR(14) CHARACTER SET ascii NULL,
 response_code VARCHAR(2) CHARACTER SET ascii NULL,
 transaction_status VARCHAR(2) CHARACTER SET ascii NULL,
 result_source VARCHAR(8) NULL,
 last_query_at DATETIME(6) NULL,
 confirmed_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 active_order_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN status='PENDING' THEN order_id ELSE NULL END) STORED,
 CONSTRAINT fk_vnpay_order FOREIGN KEY (order_id) REFERENCES orders(id),
 CONSTRAINT uq_vnpay_ref UNIQUE (txn_ref),
 CONSTRAINT uq_vnpay_active UNIQUE (active_order_id),
 CONSTRAINT uq_vnpay_gateway UNIQUE (merchant_code,gateway_transaction_no),
 CONSTRAINT ck_vnpay_status CHECK (status IN ('PENDING','FAILED','EXPIRED','SUCCEEDED','NEEDS_REVIEW','SUPERSEDED')),
 CONSTRAINT ck_vnpay_amount CHECK (expected_amount>0 AND expected_amount*100<=999999999999),
 CONSTRAINT ck_vnpay_source CHECK (result_source IS NULL OR result_source IN ('IPN','QUERYDR')),
 INDEX ix_vnpay_order_time (order_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
