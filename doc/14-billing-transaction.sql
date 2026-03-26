-- =====================================================================
-- 14-billing-transaction.sql
-- 账单流水表 + 客户回调配置表
-- =====================================================================

-- 账单流水（充值/扣费/退款记录）
CREATE TABLE IF NOT EXISTS billing_transaction (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT          NOT NULL,
    type        VARCHAR(20)     NOT NULL COMMENT 'RECHARGE / DEDUCT / REFUND',
    amount      DECIMAL(12, 4)  NOT NULL,
    balance_after DECIMAL(12, 4) NOT NULL,
    remark      VARCHAR(255),
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_customer_created (customer_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单流水';

-- 客户回调配置（DLR / MO webhook）
CREATE TABLE IF NOT EXISTS customer_callback_config (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT          NOT NULL UNIQUE,
    dlr_url     VARCHAR(500),
    dlr_secret  VARCHAR(128),
    mo_url      VARCHAR(500),
    mo_secret   VARCHAR(128),
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_customer_id (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户回调配置';
