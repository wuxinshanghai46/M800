package com.borui.sms.admin.vo.resp;

import com.borui.sms.common.domain.entity.*;
import lombok.Data;
import java.util.List;

@Data
public class CustomerDetailResp {
    private Customer customer;
    private CustomerAccount account;
    private List<CustomerCountry> countries;
    private List<CustomerCountryPrice> prices;
    private List<CustomerApiCredential> credentials;
    private List<SidInfo> sids;
}
