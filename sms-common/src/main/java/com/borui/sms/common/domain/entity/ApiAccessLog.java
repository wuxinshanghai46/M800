package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("api_access_log")
public class ApiAccessLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户 ID */
    private Long customerId;

    /** 请求接口路径 */
    private String apiPath;

    /** HTTP 方法 */
    private String method;

    /** 请求来源 IP */
    private String requestIp;

    /** 脱敏后的请求参数（JSON 字符串） */
    private String requestParams;

    /** HTTP 响应码 */
    private Integer responseCode;

    /** 接口耗时（毫秒） */
    private Integer responseTimeMs;

    /** 业务错误码 */
    private String errorCode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
