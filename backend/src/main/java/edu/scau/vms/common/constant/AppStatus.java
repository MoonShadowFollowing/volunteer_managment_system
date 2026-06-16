package edu.scau.vms.common.constant;

/** 组织者申请审核状态（organizer_applications.audit_status） */
public final class AppStatus {

    private AppStatus() {}

    public static final String PENDING  = "待审核";
    public static final String APPROVED = "已通过";
    public static final String REJECTED = "已拒绝";
}
