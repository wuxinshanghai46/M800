package com.borui.sms.admin.smpp;

import java.util.Map;

/**
 * SMPP v3.4 command status (error code) definitions.
 * Maps integer command status codes to human-readable Chinese error descriptions
 * and configuration hints.
 */
public class SmppErrorCode {

    /** SMPP command status → {name, description, hint} */
    private static final Map<Integer, String[]> CODE_MAP = Map.ofEntries(
            Map.entry(0x00000000, new String[]{"ESME_ROK",           "成功",                                   ""}),
            Map.entry(0x00000001, new String[]{"ESME_RINVMSGLEN",    "消息长度无效",                           "检查发送消息的长度是否超出上限"}),
            Map.entry(0x00000002, new String[]{"ESME_RINVCMDLEN",    "命令长度无效",                           "协议实现异常，请联系技术支持"}),
            Map.entry(0x00000003, new String[]{"ESME_RINVCMDID",     "命令 ID 无效",                           "协议版本不兼容，请联系技术支持"}),
            Map.entry(0x00000004, new String[]{"ESME_RINVBNDSTS",    "当前绑定状态不允许该操作",               "通道已处于绑定状态，请先解绑再重试"}),
            Map.entry(0x00000005, new String[]{"ESME_RALYBND",       "已绑定（重复绑定）",                     "该 System ID 已有活跃连接，请先断开已有连接"}),
            Map.entry(0x00000006, new String[]{"ESME_RINVPRTFLG",    "优先级标志无效",                         "检查消息优先级参数配置"}),
            Map.entry(0x00000007, new String[]{"ESME_RINVREGDLVFLG", "注册投递标志无效",                       "检查 DLR 注册标志配置"}),
            Map.entry(0x00000008, new String[]{"ESME_RSYSERR",       "SMSC 系统错误",                          "供应商服务器内部错误，请联系供应商排查"}),
            Map.entry(0x0000000A, new String[]{"ESME_RINVSRCADR",    "源地址无效",                             "检查 Sender ID（SID）格式，国际格式需包含国家代码"}),
            Map.entry(0x0000000B, new String[]{"ESME_RINVDSTADR",    "目标地址无效",                           "检查手机号格式是否正确，国际格式应以 + 或国家代码开头"}),
            Map.entry(0x0000000C, new String[]{"ESME_RINVMSGID",     "消息 ID 无效",                           "检查消息 ID 格式"}),
            Map.entry(0x0000000D, new String[]{"ESME_RBINDFAIL",     "绑定失败",                               "请检查 System ID、Password 及 System Type 配置是否正确"}),
            Map.entry(0x0000000E, new String[]{"ESME_RINVPASWD",     "密码无效",                               "请检查 SMPP Password 配置是否正确"}),
            Map.entry(0x0000000F, new String[]{"ESME_RINVSYSID",     "System ID 无效",                         "请检查 SMPP System ID（账号）配置是否正确"}),
            Map.entry(0x00000011, new String[]{"ESME_RCANCELFAIL",   "取消消息失败",                           "消息已投递或不存在，无法取消"}),
            Map.entry(0x00000013, new String[]{"ESME_RREPLACEFAIL",  "替换消息失败",                           "消息已投递或不存在，无法替换"}),
            Map.entry(0x00000014, new String[]{"ESME_RMSGQFUL",      "SMSC 消息队列已满",                      "供应商队列繁忙，请降低发送 TPS 或稍后重试"}),
            Map.entry(0x00000015, new String[]{"ESME_RINVSERTYP",    "Service Type 无效",                      "检查 System Type 参数，部分供应商要求特定值"}),
            Map.entry(0x00000033, new String[]{"ESME_RINVNUMDESTS",  "目标地址数量超限",                       "批量发送时目标地址数超出供应商上限"}),
            Map.entry(0x00000040, new String[]{"ESME_RINVDESTFLAG",  "目标标志无效",                           "检查目标地址标志位配置"}),
            Map.entry(0x00000043, new String[]{"ESME_RINVESMCLASS",  "ESM 类型无效",                           "消息编码类型不被该供应商支持"}),
            Map.entry(0x00000045, new String[]{"ESME_RSUBMITFAIL",   "提交失败",                               "供应商拒绝消息提交，请检查配置并联系供应商"}),
            Map.entry(0x00000048, new String[]{"ESME_RINVSRCTON",    "源地址 TON 无效",                        "检查发送方 TON 配置（INTERNATIONAL/NATIONAL/ALPHANUMERIC）"}),
            Map.entry(0x00000049, new String[]{"ESME_RINVSRCNPI",    "源地址 NPI 无效",                        "检查发送方 NPI 配置（ISDN/UNKNOWN/NATIONAL）"}),
            Map.entry(0x00000050, new String[]{"ESME_RINVDSTTON",    "目标地址 TON 无效",                      "检查通道配置中的「目标地址 TON」，通常应为 INTERNATIONAL"}),
            Map.entry(0x00000051, new String[]{"ESME_RINVDSTNPI",    "目标地址 NPI 无效",                      "检查通道配置中的「目标地址 NPI」，通常应为 ISDN（E.164）"}),
            Map.entry(0x00000053, new String[]{"ESME_RINVSYSTYP",    "System Type 无效",                       "检查 System Type 字段，供应商要求特定值时需精确匹配"}),
            Map.entry(0x00000058, new String[]{"ESME_RTHROTTLED",    "发送频率超限（TPS 节流）",               "降低发送速率，检查通道 TPS 上限配置"}),
            Map.entry(0x00000061, new String[]{"ESME_RINVSCHED",     "调度时间无效",                           "检查定时发送时间格式"}),
            Map.entry(0x00000062, new String[]{"ESME_RINVEXPIRY",    "消息过期时间无效",                       "检查消息有效期配置"}),
            Map.entry(0x00000064, new String[]{"ESME_RX_T_APPN",     "临时应用错误",                           "供应商临时错误，请稍后重试；如持续出现请联系供应商"}),
            Map.entry(0x00000065, new String[]{"ESME_RX_P_APPN",     "永久应用错误",                           "供应商永久拒绝，请检查账号权限或联系供应商"}),
            Map.entry(0x00000066, new String[]{"ESME_RX_R_APPN",     "消息被拒绝",                             "号码或内容被供应商过滤，请检查号码格式和短信内容"}),
            Map.entry(0x000000C0, new String[]{"ESME_RINVOPTPARSTREAM", "可选参数格式错误",                    "协议参数格式异常，请联系技术支持"}),
            Map.entry(0x000000C1, new String[]{"ESME_ROPTPARNOTALLWD",  "不允许携带该可选参数",                "供应商不支持该可选参数，请联系供应商确认协议版本"}),
            Map.entry(0x000000C2, new String[]{"ESME_RINVPARLEN",    "参数长度无效",                           "协议参数长度不符，请联系技术支持"}),
            Map.entry(0x000000C3, new String[]{"ESME_RMISSINGOPTPARAM", "缺少必要的可选参数",                  "供应商要求携带特定参数，请联系技术支持"}),
            Map.entry(0x000000C4, new String[]{"ESME_RINVOPTPARAMVAL",  "可选参数值无效",                      "可选参数值格式或范围不正确，请联系技术支持"}),
            Map.entry(0x000000FE, new String[]{"ESME_RDELIVERYFAILURE", "投递失败",                            "消息提交成功但最终投递失败，请查看 DLR 详情"}),
            Map.entry(0x000000FF, new String[]{"ESME_RUNKNOWNERR",   "未知错误",                               "供应商返回未知错误，请联系供应商技术支持"})
    );

    /**
     * Resolve error description by command status code.
     *
     * @param commandStatus integer command status from NegativeResponseException
     * @return [errorCodeName, description, hint]
     */
    public static String[] resolve(int commandStatus) {
        String[] info = CODE_MAP.get(commandStatus);
        if (info != null) {
            return info;
        }
        String hex = "0x" + String.format("%08X", commandStatus);
        return new String[]{"UNKNOWN_" + hex, "未知错误码 " + hex, "请对照供应商 SMPP 接口文档查询该错误码含义"};
    }

    /**
     * Build a formatted error message string.
     */
    public static String formatError(int commandStatus) {
        String[] info = resolve(commandStatus);
        String hex = "0x" + String.format("%08X", commandStatus);
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(info[0]).append("] ").append(info[1]);
        sb.append(" (命令状态码: ").append(hex).append(")");
        if (!info[2].isEmpty()) {
            sb.append(" — ").append(info[2]);
        }
        return sb.toString();
    }

    private SmppErrorCode() {}
}
