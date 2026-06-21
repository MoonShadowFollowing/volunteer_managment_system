package edu.scau.vms.common.exception;

import lombok.Getter;

// 业务级异常：能预料、要回给前端看的那种
// 抛出去会被 GlobalExceptionHandler 包成 Result.fail(code, msg)，HTTP 还是 200
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
