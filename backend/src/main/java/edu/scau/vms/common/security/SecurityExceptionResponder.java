package edu.scau.vms.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.vms.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 工具：把鉴权异常以 {@link Result} JSON 写回，HTTP 状态码由调用方指定。 */
public final class SecurityExceptionResponder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SecurityExceptionResponder() {}

    public static void write(HttpServletResponse res, int httpStatus, int bizCode, String msg) throws IOException {
        res.setStatus(httpStatus);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write(MAPPER.writeValueAsString(Result.fail(bizCode, msg)));
    }
}
