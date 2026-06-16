package edu.scau.vms.common.exception;

import lombok.Getter;

/**
 * 业务异常：可控的、需要回传给前端的错误。
 * 抛出后由 GlobalExceptionHandler 统一包装为 Result.fail(code, msg) 返回 200。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
