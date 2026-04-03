SET NAMES utf8mb4;
USE sms_platform;
ALTER TABLE sensitive_word ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'GLOBAL' COMMENT 'GLOBAL/CUSTOMER';
ALTER TABLE sensitive_word ADD COLUMN customer_id BIGINT DEFAULT NULL COMMENT 'customer_id for CUSTOMER scope';
ALTER TABLE sensitive_word ADD COLUMN country_code VARCHAR(5) DEFAULT NULL COMMENT 'country filter';
ALTER TABLE blacklist ADD COLUMN country_code VARCHAR(5) DEFAULT NULL COMMENT 'country filter';
