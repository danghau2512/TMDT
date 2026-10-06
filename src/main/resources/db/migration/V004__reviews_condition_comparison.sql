-- Additive upgrade only. Back up and inspect an existing standalone schema first.
-- NULL means not supplied; no inference about old products or orders.
ALTER TABLE products
 ADD appearance_code VARCHAR(24) NULL,
 ADD operation_code VARCHAR(24) NULL,
 ADD known_defects VARCHAR(1000) NULL,
 ADD repair_code VARCHAR(24) NULL,
 ADD repair_details VARCHAR(1000) NULL,
 ADD accessories VARCHAR(1000) NULL,
 ADD CONSTRAINT ck_product_appearance CHECK (appearance_code IN ('LIKE_NEW','LIGHT_SCRATCHES','WORN','NOT_APPLICABLE')),
 ADD CONSTRAINT ck_product_operation CHECK (operation_code IN ('NORMAL','FAULTY','NEEDS_REPAIR','NOT_APPLICABLE')),
 ADD CONSTRAINT ck_product_repair CHECK (repair_code IN ('NEVER','REPAIRED','UNKNOWN','NOT_APPLICABLE'));
ALTER TABLE order_items
 ADD appearance_snapshot VARCHAR(24) NULL,
 ADD operation_snapshot VARCHAR(24) NULL,
 ADD known_defects_snapshot VARCHAR(1000) NULL,
 ADD repair_snapshot VARCHAR(24) NULL,
 ADD repair_details_snapshot VARCHAR(1000) NULL,
 ADD accessories_snapshot VARCHAR(1000) NULL;
ALTER TABLE product_images ADD is_defect BOOLEAN NOT NULL DEFAULT FALSE,
 ADD CONSTRAINT ck_product_image_defect CHECK (is_defect IN (0,1));
ALTER TABLE order_item_images ADD is_defect_snapshot BOOLEAN NOT NULL DEFAULT FALSE,
 ADD CONSTRAINT ck_order_image_defect CHECK (is_defect_snapshot IN (0,1));
ALTER TABLE media_assets DROP CHECK ck_assets_purpose,
 ADD CONSTRAINT ck_assets_purpose CHECK (purpose IN ('PRODUCT_IMAGE','COMPLAINT_EVIDENCE','REVIEW_IMAGE'));
CREATE TABLE review_images (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
 review_id BIGINT UNSIGNED NOT NULL,
 asset_id BIGINT UNSIGNED NOT NULL,
 sort_order TINYINT UNSIGNED NOT NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY (id),
 UNIQUE KEY uq_review_image_order (review_id,sort_order),
 UNIQUE KEY uq_review_image_asset (review_id,asset_id),
 KEY ix_review_image_asset (asset_id),
 CONSTRAINT fk_review_image_review FOREIGN KEY (review_id) REFERENCES reviews(id),
 CONSTRAINT fk_review_image_asset FOREIGN KEY (asset_id) REFERENCES media_assets(id),
 CONSTRAINT ck_review_image_order CHECK (sort_order BETWEEN 0 AND 2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
