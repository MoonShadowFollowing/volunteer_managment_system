package edu.scau.vms.module.certificate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.exception.BizException;
import edu.scau.vms.module.activity.entity.Activity;
import edu.scau.vms.module.activity.mapper.ActivityMapper;
import edu.scau.vms.module.certificate.dto.CertificateVO;
import edu.scau.vms.module.certificate.entity.Certificate;
import edu.scau.vms.module.certificate.mapper.CertificateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private static final DateTimeFormatter ACT_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CertificateMapper certificateMapper;
    private final ActivityMapper activityMapper;

    public PageResult<CertificateVO> mine(Long volunteerId, Long page, Long size, String activityName) {
        LambdaQueryWrapper<Certificate> qw = new LambdaQueryWrapper<>();
        qw.eq(Certificate::getVolunteerId, volunteerId).orderByDesc(Certificate::getIssuedDate);
        Page<Certificate> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<Certificate> result = certificateMapper.selectPage(p, qw);

        List<CertificateVO> vos = toVOs(result.getRecords());
        if (activityName != null && !activityName.isBlank()) {
            vos = vos.stream().filter(v -> v.getActivityName() != null && v.getActivityName().contains(activityName)).toList();
        }
        return PageResult.of(result.getTotal(), vos);
    }

    /**
     * 取单个证书的 VO，并校验归属志愿者。S5 PDF 下载使用。
     */
    public CertificateVO getOwnedById(Long certId, Long volunteerId) {
        Certificate c = certificateMapper.selectById(certId);
        if (c == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "证书不存在");
        }
        if (!c.getVolunteerId().equals(volunteerId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问该证书");
        }
        List<CertificateVO> vos = toVOs(List.of(c));
        return vos.get(0);
    }

    private List<CertificateVO> toVOs(List<Certificate> list) {
        if (list.isEmpty()) return List.of();
        List<Long> activityIds = list.stream().map(Certificate::getActivityId).distinct().toList();
        Map<Long, Activity> actMap = new HashMap<>();
        for (Long aid : activityIds) {
            Activity a = activityMapper.selectById(aid);
            if (a != null) actMap.put(aid, a);
        }
        return list.stream().map(c -> {
            Activity a = actMap.get(c.getActivityId());
            String actNo = a == null ? null : a.getStartTime().format(ACT_NO_FMT) + String.format("%04d", a.getActivityId());
            return CertificateVO.builder()
                    .certId(c.getCertId())
                    .certNo("CERT-" + (actNo == null ? "UNKNOWN" : actNo) + "-" + String.format("%05d", c.getCertId()))
                    .certName(c.getTitle())
                    .activityId(c.getActivityId())
                    .actNo(a == null ? null : a.getStartTime().format(ACT_NO_FMT) + String.format("%04d", a.getActivityId()))
                    .activityName(a == null ? null : a.getTitle())
                    .startTime(c.getStartTime())
                    .endTime(c.getEndTime())
                    .hours(c.getCertHours())
                    .minutes(c.getCertMinutes())
                    .issuedDate(c.getIssuedDate())
                    .status(c.getStatus())
                    .build();
        }).collect(Collectors.toList());
    }
}
