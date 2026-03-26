package com.borui.sms.admin.vo.resp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginResp {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
    private String roleName;
    private List<String> permissions;
}
