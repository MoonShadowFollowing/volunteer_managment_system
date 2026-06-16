package edu.scau.vms.common.constant;

/** 报名审核状态（registrations.audit_status） */
public final class RegStatus {

    private RegStatus() {}

    public static final String PENDING   = "待审核";
    public static final String APPROVED  = "审核通过";
    public static final String REJECTED  = "审核拒绝";
    public static final String CANCELLED = "已取消";
}
