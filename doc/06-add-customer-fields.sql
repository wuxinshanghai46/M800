SET NAMES utf8mb4;
USE sms_platform;
ALTER TABLE customer ADD COLUMN allowed_sms_attributes VARCHAR(50) DEFAULT '1,2,3,4' COMMENT '允许的短信属性，逗号分隔: 1=OTP,2=事务,3=通知,4=营销';
ALTER TABLE customer ADD COLUMN address VARCHAR(500) DEFAULT '' COMMENT '客户地址';
CREATE TABLE IF NOT EXISTS customer_sid (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT NOT NULL,
    sid_id          BIGINT NOT NULL COMMENT 'sid_info.id',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_customer_sid (customer_id, sid_id)
) ENGINE=InnoDB COMMENT='客户-SID授权关系';
