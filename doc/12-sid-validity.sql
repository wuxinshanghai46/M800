-- Sprint 12: Add validity_months to sid_info, support FIXED/VIRTUAL sid types
ALTER TABLE sid_info
    ADD COLUMN validity_months INT NULL COMMENT '有效期（月），仅 FIXED/VIRTUAL 类型需填' AFTER country_code;

-- Update comment to reflect new sid types
ALTER TABLE sid_info
    MODIFY COLUMN sid_type VARCHAR(20) NOT NULL DEFAULT 'ALPHA' COMMENT 'ALPHA/NUMERIC/FIXED/VIRTUAL';
