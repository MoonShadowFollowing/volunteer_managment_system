package edu.scau.vms.module.certificate;

import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.Result;
import edu.scau.vms.common.constant.CertStatus;
import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.exception.BizException;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.certificate.dto.CertificateVO;
import edu.scau.vms.module.user.entity.User;
import edu.scau.vms.module.user.mapper.UserMapper;
import edu.scau.vms.module.user.UserService;
import edu.scau.vms.util.PdfGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "Certificate", description = "志愿服务证书 FR-05")
@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;
    private final PdfGenerator pdfGenerator;
    private final UserMapper userMapper;
    private final UserService userService;

    @Operation(summary = "我的证书")
    @GetMapping("/mine")
    public Result<PageResult<CertificateVO>> mine(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String activityName) {
        return Result.ok(certificateService.mine(me.userId(), page, pageSize, activityName));
    }

    @Operation(summary = "证书 PDF 下载（iText 7）")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<ByteArrayResource> downloadPdf(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable Long id) {
        CertificateVO vo = certificateService.getOwnedById(id, me.userId());
        if (CertStatus.INVALID.equals(vo.getStatus())) {
            throw new BizException(ErrorCode.CERT_INVALID, "该证书已失效，无法下载");
        }
        User user = userMapper.selectById(me.userId());
        String volunteerName = user == null ? "" : user.getName();
        String volunteerNo = user == null ? "" : userService.formatUserNo(user);

        byte[] pdf = pdfGenerator.generateCertificate(vo, volunteerName, volunteerNo);
        ByteArrayResource resource = new ByteArrayResource(pdf);

        String fileName = "VMS-Certificate-" + id + ".pdf";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
