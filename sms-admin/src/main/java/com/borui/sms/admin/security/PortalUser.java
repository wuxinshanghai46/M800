package com.borui.sms.admin.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PortalUser {
    private Long customerId;
    private String companyName;
    private String account;
}
