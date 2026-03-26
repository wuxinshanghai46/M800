package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 请求追踪 ID，与日志系统联动 */
    private String traceId;

    /** 操作人 ID（admin user id 或 customer id） */
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 操作人类型：admin / customer */
    private String operatorType;

    /** 操作模块 */
    private String module;

    /** 操作动作，如 CREATE / UPDATE / DELETE / FREEZE */
    private String action;

    /** 目标类型，如 Customer / Channel / Vendor */
    private String targetType;

    /** 目标 ID */
    private String targetId;

    /** 目标名称 */
    private String targetName;

    /** 操作摘要（人可读描述） */
    private String summary;

    /** 变更前快照（仅关键字段，JSON 字符串） */
    private String beforeData;

    /** 变更后快照（JSON 字符串） */
    private String afterData;

    /** 操作来源 IP */
    private String ip;

    /** User-Agent */
    private String userAgent;

    /** 操作结果：success / failed */
    @TableField("op_result")
    private String result;

    /** 失败原因 */
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
