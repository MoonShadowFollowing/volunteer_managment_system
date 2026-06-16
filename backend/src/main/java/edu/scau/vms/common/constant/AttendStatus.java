package edu.scau.vms.common.constant;

/** 签到状态（attendance.status） */
public final class AttendStatus {

    private AttendStatus() {}

    public static final String NOT_CHECKED_IN  = "未签到";
    public static final String CHECKED_IN      = "已签到";
    public static final String CHECKED_OUT     = "已签退";
    public static final String MISSED_CHECKOUT = "漏签退";
    public static final String ABNORMAL        = "异常";
}
