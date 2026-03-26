-- =============================================
-- 10-schema-cleanup.sql
-- 数据模型关系清理（基线版本）
-- 适配 MySQL 8.0.32（不使用 IF EXISTS）
-- =============================================

-- Issue 1: 移除冗余 country_id（统一使用 country_code 作为关联键）
ALTER TABLE vendor_country DROP COLUMN country_id;
ALTER TABLE channel DROP COLUMN country_id;
-- customer_country 需先删除包含 country_id 的唯一键，再删列，再重建唯一键
ALTER TABLE customer_country DROP INDEX uk_cc;
ALTER TABLE customer_country DROP COLUMN country_id;
ALTER TABLE customer_country ADD UNIQUE INDEX uk_cc (customer_id, country_code);

-- Issue 3: sid_replace_rule.target_channel (VARCHAR) → target_channel_id (BIGINT)
ALTER TABLE sid_replace_rule DROP COLUMN target_channel;
ALTER TABLE sid_replace_rule ADD COLUMN target_channel_id BIGINT NULL COMMENT '目标通道ID' AFTER original_sid;

-- Issue 4: 移除冗余的反范式字段（通过 JOIN 查询获取）
ALTER TABLE mo_record DROP COLUMN channel_name;
ALTER TABLE alert_record DROP COLUMN rule_name;

-- Issue 5: template_variable 增加 template_id 外键关联
ALTER TABLE template_variable ADD COLUMN template_id BIGINT NULL COMMENT '所属模板ID' AFTER id;
CREATE INDEX idx_template_variable_template_id ON template_variable(template_id);

-- Issue 6: vendor_country 唯一约束修复（DROP COLUMN country_id 后旧UK退化为仅 vendor_id）
ALTER TABLE vendor_country DROP INDEX uk_vendor_country;
ALTER TABLE vendor_country ADD UNIQUE INDEX uk_vendor_country (vendor_id, country_code);

-- Issue 7: 补充缺失索引
ALTER TABLE message ADD INDEX idx_msg_country (country_code);
ALTER TABLE message ADD INDEX idx_msg_channel (channel_id);
ALTER TABLE sid_replace_rule ADD INDEX idx_srr_target_channel (target_channel_id);
ALTER TABLE sid_info ADD UNIQUE INDEX uk_sid_vendor_country (sid, vendor_id, country_code);
