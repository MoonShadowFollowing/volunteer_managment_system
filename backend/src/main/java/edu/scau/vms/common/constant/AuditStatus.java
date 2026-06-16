package edu.scau.vms.common.constant;

/** 活动审核状态（activities.audit_status） */
public final class AuditStatus {

    private AuditStatus() {}

    public static final String PENDING  = "待审核";
    public static final String APPROVED = "审核通过";
    public static final String REJECTED = "审核不通过";
}
