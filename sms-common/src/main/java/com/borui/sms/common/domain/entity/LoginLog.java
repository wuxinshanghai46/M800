package com.borui.sms.common.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 用户类型：admin / customer */
    private String userType;

    /** 登录方式：password / sso / api_key */
    private String loginType;

    /** 登录 IP */
    private String ipAddress;

    /** User-Agent */
    private String userAgent;

    /** IP 归属地（可选，由 GeoIP 查询填充） */
    private String geoLocation;

    /** 登录结果：success / failed / locked */
    private String result;

    /** 失败原因 */
    private String failReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
