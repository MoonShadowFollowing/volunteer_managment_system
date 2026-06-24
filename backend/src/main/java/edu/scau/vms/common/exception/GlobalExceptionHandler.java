package edu.scau.vms.common.exception;

import edu.scau.vms.common.Result;
import edu.scau.vms.common.constant.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

// 全局异常拦截，每种异常对应一种返回形态
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常照原样回前端，HTTP 还是 200；fix 字段携带修正指引
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("BizException code={} msg={} fix={}", e.getCode(), e.getMessage(), e.getFix());
        return Result.fail(e.getCode(), e.getMessage(), e.getFix());
    }

    // @Valid 校验失败，把每个字段的错拼一块儿返回，方便前端定位
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", detail);
        return Result.fail(ErrorCode.PARAM_INVALID, detail);
    }

    // 一般 SecurityConfig 的 entryPoint 已经先拦了，这里是兜底
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuth(AuthenticationException e) {
        log.warn("AuthenticationException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(ErrorCode.UNAUTHORIZED, "未登录或Token已过期"));
    }

    // @PreAuthorize 拒掉的请求会跑到这里
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleDenied(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Result.fail(ErrorCode.FORBIDDEN, "权限不足"));
    }

    // 没接住的全部归 500，堆栈不外露
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.SERVER_ERROR, "服务器内部错误"));
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage();
    }
}
