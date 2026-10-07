-- V005 đã áp: bổ sung ràng buộc mới, không thay checksum migration cũ.
-- CHECK SQL cho phép UNKNOWN (NULL), vì vậy lý do từ chối phải kiểm IS NOT NULL.
ALTER TABLE seller_verification_submissions
 ADD CONSTRAINT ck_seller_rejection_required CHECK
 (status <> 'REJECTED' OR (rejection_reason IS NOT NULL AND CHAR_LENGTH(TRIM(rejection_reason)) >= 5)),
 ADD CONSTRAINT ck_seller_submitted_fields CHECK
 (status NOT IN ('PENDING','APPROVED','REJECTED') OR
  (full_name IS NOT NULL AND CHAR_LENGTH(TRIM(full_name)) >= 2 AND id_number IS NOT NULL AND front_key IS NOT NULL AND front_mime IS NOT NULL));
