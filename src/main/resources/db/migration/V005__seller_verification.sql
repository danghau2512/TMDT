-- Hồ sơ riêng tư; không đánh dấu người dùng cũ là đã duyệt.
CREATE TABLE seller_profiles (
 user_id BIGINT UNSIGNED PRIMARY KEY,
 status VARCHAR(20) NOT NULL DEFAULT 'NOT_SUBMITTED',
 current_submission_id BIGINT UNSIGNED NULL,
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_seller_profile_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT ck_seller_profile_status CHECK (status IN ('NOT_SUBMITTED','PENDING','APPROVED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE seller_verification_submissions (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT UNSIGNED NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 full_name VARCHAR(120) NULL,
 id_number VARCHAR(12) NULL,
 ocr_name VARCHAR(120) NULL,
 ocr_number VARCHAR(12) NULL,
 ocr_status VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
 ocr_attempts INT NOT NULL DEFAULT 0,
 ocr_started_at DATETIME(6) NULL,
 card_type VARCHAR(30) NULL,
 front_key VARCHAR(40) NULL,
 front_mime VARCHAR(20) NULL,
 back_key VARCHAR(40) NULL,
 back_mime VARCHAR(20) NULL,
 consent_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 ocr_consent_at DATETIME(6) NULL,
 consent_version VARCHAR(20) NOT NULL DEFAULT '20261006-v1',
 submitted_at DATETIME(6) NULL,
 reviewer_id BIGINT UNSIGNED NULL,
 reviewed_at DATETIME(6) NULL,
 rejection_reason VARCHAR(1000) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_seller_submission_owner (user_id,id),
 KEY ix_seller_review_queue (status,submitted_at,id),
 CONSTRAINT fk_seller_submission_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT fk_seller_submission_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
 CONSTRAINT ck_seller_submission_status CHECK (status IN ('DRAFT','PENDING','APPROVED','REJECTED','SUPERSEDED','PURGED')),
 CONSTRAINT ck_seller_submission_ocr CHECK (ocr_status IN ('MANUAL','NOT_CONFIGURED','READING','SUCCESS','FAILED')),
 CONSTRAINT ck_seller_submission_number CHECK (id_number IS NULL OR id_number REGEXP '^([0-9]{9}|[0-9]{12})$'),
 CONSTRAINT ck_seller_submission_decision CHECK (status NOT IN ('APPROVED','REJECTED') OR (reviewer_id IS NOT NULL AND reviewed_at IS NOT NULL)),
 CONSTRAINT ck_seller_submission_rejection CHECK (status <> 'REJECTED' OR CHAR_LENGTH(TRIM(rejection_reason)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
ALTER TABLE seller_profiles ADD CONSTRAINT fk_seller_profile_current
 FOREIGN KEY (user_id,current_submission_id) REFERENCES seller_verification_submissions(user_id,id);
