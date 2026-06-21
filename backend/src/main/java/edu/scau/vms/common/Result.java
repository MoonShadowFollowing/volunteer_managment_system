package edu.scau.vms.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

// 全项目接口的统一返回壳子。axios 拦截器会自动拆 data 出来
// code=0 成功，非 0 业务失败（HTTP 状态码还是 200）；认证类失败走 401/403
@Data
@Schema(description = "统一响应包装")
public class Result<T> {

    @Schema(description = "业务状态码，0=成功")
    private int code;

    @Schema(description = "提示信息")
    private String msg;

    @Schema(description = "业务数据")
    private T data;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.msg = "ok";
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
