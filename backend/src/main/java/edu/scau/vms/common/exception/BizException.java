package edu.scau.vms.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final String fix;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
        this.fix = null;
    }

    public BizException(int code, String msg, String fix) {
        super(msg);
        this.code = code;
        this.fix = fix;
    }
}
