SET NAMES utf8mb4;
USE sms_platform;
ALTER TABLE customer ADD COLUMN portal_account VARCHAR(100) DEFAULT '' COMMENT '客户端登录账号';
ALTER TABLE customer ADD COLUMN portal_password VARCHAR(100) DEFAULT '' COMMENT '客户端登录密码';
