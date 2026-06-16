package edu.scau.vms.util;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import edu.scau.vms.module.certificate.dto.CertificateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * 证书 PDF 生成器（iText 7）。
 * 使用 Adobe 标准 CJK 字体 STSong-Light + UniGB-UCS2-H（来自 font-asian，无须本地 TTF 文件）。
 */
@Slf4j
@Component
public class PdfGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final DeviceRgb COLOR_TITLE = new DeviceRgb(178, 34, 34);  // 深红
    private static final DeviceRgb COLOR_ACCENT = new DeviceRgb(102, 51, 0);  // 棕

    /**
     * 生成单张证书 PDF。
     * @param vo CertificateVO（已包含活动名/工时/状态等）
     * @param volunteerName 志愿者姓名
     * @param volunteerNo 志愿者展示编号（VOL-00007）
     * @return 完整 PDF 字节流
     */
    public byte[] generateCertificate(CertificateVO vo, String volunteerName, String volunteerNo) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(PageSize.A4);
            Document doc = new Document(pdf);
            doc.setMargins(60, 60, 60, 60);

            PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");

            // 顶部双线
            doc.add(new LineSeparator(new SolidLine(2f)).setMarginBottom(2));
            doc.add(new LineSeparator(new SolidLine(0.7f)).setMarginBottom(30));

            // 大标题
            doc.add(new Paragraph("志 愿 服 务 证 书")
                    .setFont(font).setFontSize(34).setBold()
                    .setFontColor(COLOR_TITLE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(8));

            doc.add(new Paragraph("CERTIFICATE OF VOLUNTEER SERVICE")
                    .setFont(font).setFontSize(11)
                    .setFontColor(COLOR_ACCENT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(40));

            // 证书编号 + 状态徽标
            Table headInfo = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100));
            headInfo.addCell(noBorder(new Paragraph("证书编号：" + formatCertNo(vo))
                    .setFont(font).setFontSize(11).setTextAlignment(TextAlignment.LEFT)));
            String status = vo.getStatus() == null ? "" : vo.getStatus();
            headInfo.addCell(noBorder(new Paragraph("证书状态：" + status)
                    .setFont(font).setFontSize(11)
                    .setFontColor("已失效".equals(status) ? ColorConstants.GRAY : COLOR_TITLE)
                    .setTextAlignment(TextAlignment.RIGHT)));
            doc.add(headInfo);
            doc.add(new Paragraph(" ").setFontSize(10));

            // 正文
            String body1 = "  兹  证  明  " + spaceOut(volunteerName) + "  同学";
            doc.add(new Paragraph(body1)
                    .setFont(font).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20).setMarginBottom(8));

            String volIdLine = volunteerNo == null ? "" : "（志愿者编号：" + volunteerNo + "）";
            if (!volIdLine.isEmpty()) {
                doc.add(new Paragraph(volIdLine)
                        .setFont(font).setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(28));
            }

            doc.add(new Paragraph("于以下时间参加了「" + nullSafe(vo.getActivityName()) + "」志愿服务活动：")
                    .setFont(font).setFontSize(13)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            // 时段
            String span = (vo.getStartTime() == null ? "" : vo.getStartTime().format(DATETIME_FMT))
                    + "  至  "
                    + (vo.getEndTime() == null ? "" : vo.getEndTime().format(DATETIME_FMT));
            doc.add(new Paragraph(span)
                    .setFont(font).setFontSize(12)
                    .setFontColor(COLOR_ACCENT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(28));

            // 工时方框
            int h = vo.getHours() == null ? 0 : vo.getHours();
            int m = vo.getMinutes() == null ? 0 : vo.getMinutes();
            String hours = h + " 小时 " + m + " 分钟";
            Table hoursBox = new Table(1).setWidth(UnitValue.createPercentValue(60))
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);
            Cell hoursCell = new Cell()
                    .add(new Paragraph("认证服务工时").setFont(font).setFontSize(11)
                            .setFontColor(COLOR_ACCENT).setTextAlignment(TextAlignment.CENTER))
                    .add(new Paragraph(hours).setFont(font).setFontSize(22).setBold()
                            .setFontColor(COLOR_TITLE).setTextAlignment(TextAlignment.CENTER))
                    .setBorder(new SolidBorder(COLOR_ACCENT, 1.2f))
                    .setPaddingTop(12).setPaddingBottom(12);
            hoursBox.addCell(hoursCell);
            doc.add(hoursBox);

            doc.add(new Paragraph(" ").setMarginBottom(40));

            // 落款
            String issuedDate = vo.getIssuedDate() == null ? "" : vo.getIssuedDate().format(DATE_FMT);
            doc.add(new Paragraph("特此证明。")
                    .setFont(font).setFontSize(13)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginTop(20).setMarginBottom(40));

            Table signTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .setWidth(UnitValue.createPercentValue(100));
            signTable.addCell(noBorder(new Paragraph(" ").setFont(font)));
            signTable.addCell(noBorder(new Paragraph("发证单位：志愿服务工时认证与活动管理系统\n发证日期：" + issuedDate)
                    .setFont(font).setFontSize(12)
                    .setTextAlignment(TextAlignment.RIGHT)));
            doc.add(signTable);

            // 底部水印备注
            doc.add(new Paragraph(" ").setMarginTop(80));
            doc.add(new LineSeparator(new SolidLine(0.5f)).setMarginBottom(4));
            doc.add(new Paragraph("本证书由系统自动生成，可通过证书编号在系统内查验真伪。")
                    .setFont(font).setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("[PdfGenerator] 生成证书 PDF 失败 certId={}", vo == null ? null : vo.getCertId(), e);
            throw new RuntimeException("证书 PDF 生成失败：" + e.getMessage(), e);
        }
    }

    private static Cell noBorder(Paragraph p) {
        return new Cell().add(p).setBorder(Border.NO_BORDER);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /** "张三" → "张  三"；让单字之间留宽，配合中文证书排版。 */
    private static String spaceOut(String name) {
        if (name == null || name.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            sb.append(name.charAt(i));
            if (i < name.length() - 1) sb.append(' ');
        }
        return sb.toString();
    }

    /** 兜底证书编号：actNo 不空就拼 actNo + 后 5 位 certId，否则用 CERT-{certId}。 */
    private static String formatCertNo(CertificateVO vo) {
        if (vo == null) return "CERT-UNKNOWN";
        if (vo.getActNo() != null && !vo.getActNo().isEmpty()) {
            return "CERT-" + vo.getActNo() + "-" + String.format("%05d", vo.getCertId() == null ? 0 : vo.getCertId());
        }
        return "CERT-" + String.format("%08d", vo.getCertId() == null ? 0 : vo.getCertId());
    }

}
