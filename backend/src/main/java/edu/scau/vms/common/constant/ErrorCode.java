package edu.scau.vms.common.constant;

/**
 * 错误码分段：
 *   1xxx 通用 / 系统
 *   2xxx 鉴权与权限
 *   3xxx 业务规则
 *   4xxx 参数与数据校验
 */
public final class ErrorCode {

    private ErrorCode() {}

    public static final int SERVER_ERROR = 1000;
    public static final int NOT_FOUND = 1001;

    public static final int UNAUTHORIZED = 2000;
    public static final int FORBIDDEN = 2001;
    public static final int TOKEN_EXPIRED = 2002;
    public static final int INVALID_CREDENTIALS = 2003;
    public static final int TOKEN_INVALID = 2004;

    public static final int BIZ_CONFLICT = 3000;
    public static final int ACTIVITY_NOT_AUDITED = 3001;
    public static final int CERT_INVALID = 3002;
    public static final int ADMIN_CANNOT_REVOKE_ORGANIZER = 3003;

    public static final int PARAM_INVALID = 4000;
}
