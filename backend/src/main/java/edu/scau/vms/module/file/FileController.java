package edu.scau.vms.module.file;

import edu.scau.vms.common.Result;
import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.exception.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 文件上传 + 静态读取。文件落 ${vms.upload.dir}/yyyy/MM/<uuid>.<ext>
// 写文件之前要 normalize + startsWith(root) 双检，挡掉 ../ 这种穿越
// 目前就组织者申请的证明材料在用，以后多了再说
@Tag(name = "File", description = "文件上传/静态访问")
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf");
    private static final DateTimeFormatter DIR_FMT = DateTimeFormatter.ofPattern("yyyy/MM");

    private static final Map<String, String> MIME_BY_EXT = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp",
            "pdf", "application/pdf");

    @Value("${vms.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${vms.upload.max-mb:5}")
    private long maxMb;

    @Operation(summary = "上传单文件（图片/PDF，≤5MB）")
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "文件为空",
                    "请选择一个文件后再上传。支持的格式：jpg、png、gif、webp、pdf。");
        }
        if (file.getSize() > maxMb * 1024 * 1024) {
            throw new BizException(ErrorCode.PARAM_INVALID, "文件超过 " + maxMb + "MB 限制",
                    "请压缩文件或选择更小的文件上传。当前限制为 " + maxMb + "MB。");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = extensionOf(original).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "仅支持 " + String.join("/", ALLOWED_EXT) + " 格式",
                    "请将文件转换为支持的格式后重新上传：jpg、png、gif、webp、pdf。");
        }

        String subDir = LocalDate.now().format(DIR_FMT);
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relative = subDir + "/" + name;

        try {
            Path root = resolveRoot();
            Path dir = root.resolve(subDir).normalize();
            Files.createDirectories(dir);
            Path dest = dir.resolve(name).normalize();
            // normalize 之后还要再确认一遍是不是真在 root 下，挡 ../ 攻击
            if (!dest.startsWith(root)) {
                throw new BizException(ErrorCode.PARAM_INVALID, "非法文件路径",
                        "文件路径异常，请重新选择文件后上传。");
            }
            file.transferTo(dest);
            log.info("[FileUpload] saved {} → {} ({} bytes)", original, dest, file.getSize());
        } catch (IOException e) {
            log.error("[FileUpload] 保存失败 {}", original, e);
            throw new BizException(ErrorCode.SERVER_ERROR, "文件保存失败",
                    "服务器存储异常，请稍后重试。如持续失败，请联系系统管理员。");
        }

        String url = "/api/files/static/" + relative;
        return Result.ok(Map.of(
                "url", url,
                "filename", name,
                "originalName", original,
                "size", file.getSize()));
    }

    @Operation(summary = "下载/在线查看上传过的文件（公开）")
    @GetMapping("/static/**")
    public ResponseEntity<FileSystemResource> staticFile(HttpServletRequest req, HttpServletResponse res) {
        String full = req.getRequestURI();
        String prefix = req.getContextPath() + "/api/files/static/";
        if (!full.startsWith(prefix)) {
            return ResponseEntity.notFound().build();
        }
        String rel = full.substring(prefix.length());
        // 三个常见的恶意路径形态都挡一遍，再保险也不嫌多
        if (rel.contains("..") || rel.startsWith("/") || rel.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        Path root = resolveRoot();
        Path file = root.resolve(rel).normalize();
        if (!file.startsWith(root) || !Files.exists(file) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        String ext = extensionOf(file.getFileName().toString()).toLowerCase(Locale.ROOT);
        MediaType type = MediaType.parseMediaType(
                MIME_BY_EXT.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM_VALUE));

        String fileName = file.getFileName().toString();
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded)
                .body(new FileSystemResource(file.toFile()));
    }

    private Path resolveRoot() {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new BizException(ErrorCode.SERVER_ERROR, "上传目录不可用：" + root,
                    "服务器存储配置异常，请联系系统管理员检查上传目录权限。");
        }
        return root;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return (dot < 0 || dot == name.length() - 1) ? "" : name.substring(dot + 1);
    }
}
