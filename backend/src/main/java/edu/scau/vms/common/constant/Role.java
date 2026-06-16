package edu.scau.vms.common.constant;

/** 用户角色枚举值（与 users.role 字段对齐） */
public final class Role {

    private Role() {}

    public static final String VOLUNTEER  = "volunteer";
    public static final String ORGANIZER  = "organizer";
    public static final String ADMIN      = "admin";
    public static final String SUPERADMIN = "superadmin";
}
