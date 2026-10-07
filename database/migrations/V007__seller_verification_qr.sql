-- Giữ ảnh, hồ sơ và baseline OCR cũ. Chỉ phân loại nguồn trong cột mới.
ALTER TABLE seller_verification_submissions
 ADD COLUMN data_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL_LEGACY',
 ADD COLUMN birth_date DATE NULL,
 ADD COLUMN gender VARCHAR(10) NULL,
 ADD COLUMN residence VARCHAR(500) NULL,
 ADD COLUMN issue_date DATE NULL,
 ADD COLUMN qr_status VARCHAR(20) NOT NULL DEFAULT 'NOT_READ',
 ADD COLUMN qr_started_at DATETIME(6) NULL,
 ADD COLUMN qr_full_name VARCHAR(120) NULL,
 ADD COLUMN qr_id_number VARCHAR(12) NULL,
 ADD COLUMN qr_birth_date DATE NULL,
 ADD COLUMN qr_gender VARCHAR(10) NULL,
 ADD COLUMN qr_residence VARCHAR(500) NULL,
 ADD COLUMN qr_issue_date DATE NULL;
UPDATE seller_verification_submissions SET data_source='OCR_LEGACY' WHERE ocr_status='SUCCESS';
ALTER TABLE seller_verification_submissions
 ADD CONSTRAINT ck_seller_qr_source CHECK (data_source IN ('MANUAL','QR','OCR_LEGACY','MANUAL_LEGACY')),
 ADD CONSTRAINT ck_seller_qr_status CHECK (qr_status IN ('NOT_READ','READING','SUCCESS','NOT_FOUND','UNSUPPORTED','CONFLICT')),
 ADD CONSTRAINT ck_seller_new_identity CHECK
 (status NOT IN ('PENDING','APPROVED','REJECTED') OR data_source IN ('OCR_LEGACY','MANUAL_LEGACY') OR
  (birth_date IS NOT NULL AND gender IS NOT NULL AND gender IN ('Nam','Nữ','Khác')
   AND residence IS NOT NULL AND CHAR_LENGTH(TRIM(residence)) >= 3 AND issue_date IS NOT NULL
   AND issue_date >= birth_date AND back_key IS NOT NULL AND back_mime IS NOT NULL AND CHAR_LENGTH(id_number)=12)),
 ADD CONSTRAINT ck_seller_qr_baseline CHECK
 (qr_status <> 'SUCCESS' OR (qr_full_name IS NOT NULL AND qr_id_number IS NOT NULL AND qr_birth_date IS NOT NULL
  AND qr_gender IS NOT NULL AND qr_residence IS NOT NULL AND qr_issue_date IS NOT NULL));
