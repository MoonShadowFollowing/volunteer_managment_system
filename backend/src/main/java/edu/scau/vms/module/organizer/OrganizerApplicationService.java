package edu.scau.vms.module.organizer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.constant.AppStatus;
import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.constant.MsgType;
import edu.scau.vms.common.exception.BizException;
import edu.scau.vms.module.message.MessageService;
import edu.scau.vms.module.organizer.dto.ApplicationVO;
import edu.scau.vms.module.organizer.dto.SubmitApplicationRequest;
import edu.scau.vms.module.organizer.entity.OrganizerApplication;
import edu.scau.vms.module.organizer.mapper.OrganizerApplicationMapper;
import edu.scau.vms.module.user.UserService;
import edu.scau.vms.module.user.entity.User;
import edu.scau.vms.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 志愿者 → 组织者的资质申请。审核通过会把 users.is_organizer 置 true
@Service
@RequiredArgsConstructor
public class OrganizerApplicationService {

    private final OrganizerApplicationMapper appMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final MessageService messageService;

    @Transactional
    public Long submit(Long applicantId, SubmitApplicationRequest req) {
        User u = userMapper.selectById(applicantId);
        if (u == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        if (Boolean.TRUE.equals(u.getIsOrganizer())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "您已具备组织者资质，无需重复申请");
        }
        // 已有待审核申请就不重复提交
        Long pending = appMapper.selectCount(new LambdaQueryWrapper<OrganizerApplication>()
                .eq(OrganizerApplication::getApplicantId, applicantId)
                .eq(OrganizerApplication::getAuditStatus, AppStatus.PENDING));
        if (pending > 0) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "您已有一份待审核申请，请耐心等待");
        }
        OrganizerApplication app = new OrganizerApplication();
        app.setApplicantId(applicantId);
        app.setReason(req.getReason());
        app.setMaterialUrl(req.getMaterialUrl());
        app.setAuditStatus(AppStatus.PENDING);
        app.setSubmittedAt(LocalDateTime.now());
        appMapper.insert(app);
        return app.getAppId();
    }

    public PageResult<ApplicationVO> list(Long page, Long size, String status, String name, String userNo) {
        LambdaQueryWrapper<OrganizerApplication> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) qw.eq(OrganizerApplication::getAuditStatus, status);
        boolean hasName = name != null && !name.isBlank();
        boolean hasUserNo = userNo != null && !userNo.isBlank();
        if (hasName || hasUserNo) {
            LambdaQueryWrapper<User> uqw = new LambdaQueryWrapper<User>().select(User::getUserId);
            if (hasName) uqw.like(User::getName, name);
            if (hasUserNo) uqw.like(User::getUsername, userNo.replaceFirst("^(SUP|ADM|ORG|VOL)-", ""));
            List<Long> userIds = userMapper.selectList(uqw).stream().map(User::getUserId).toList();
            if (userIds.isEmpty()) {
                return PageResult.of(0L, List.of());
            }
            qw.in(OrganizerApplication::getApplicantId, userIds);
        }
        qw.orderByAsc(OrganizerApplication::getAuditStatus)
          .orderByDesc(OrganizerApplication::getSubmittedAt);
        Page<OrganizerApplication> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<OrganizerApplication> result = appMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), toVOs(result.getRecords()));
    }

    public PageResult<ApplicationVO> mine(Long applicantId, Long page, Long size) {
        LambdaQueryWrapper<OrganizerApplication> qw = new LambdaQueryWrapper<>();
        qw.eq(OrganizerApplication::getApplicantId, applicantId)
          .orderByDesc(OrganizerApplication::getSubmittedAt);
        Page<OrganizerApplication> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<OrganizerApplication> result = appMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), toVOs(result.getRecords()));
    }

    // 管理员审申请，通过的话顺手给用户开 organizer 资质
    @Transactional
    public void audit(Long appId, Long auditorId, boolean approve) {
        OrganizerApplication app = appMapper.selectById(appId);
        if (app == null) throw new BizException(ErrorCode.NOT_FOUND, "申请不存在");
        if (!AppStatus.PENDING.equals(app.getAuditStatus())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "申请已审核，不可再次操作");
        }
        app.setAuditStatus(approve ? AppStatus.APPROVED : AppStatus.REJECTED);
        app.setAuditorId(auditorId);
        app.setAuditedAt(LocalDateTime.now());
        appMapper.updateById(app);

        if (approve) {
            User u = userMapper.selectById(app.getApplicantId());
            if (u != null) {
                u.setIsOrganizer(true);
                userMapper.updateById(u);
            }
        }
        messageService.sendDirect(app.getApplicantId(), MsgType.QUAL_AUDIT,
                approve ? "组织者资质审核通过" : "组织者资质审核未通过",
                approve ? "恭喜！您已具备组织者资质，可在右上角切换为组织者发布活动。"
                        : "很抱歉，您的组织者申请未通过审核，可补充材料后再次提交。",
                "volunteer");
    }

    private List<ApplicationVO> toVOs(List<OrganizerApplication> list) {
        if (list.isEmpty()) return List.of();
        List<Long> userIds = list.stream().map(OrganizerApplication::getApplicantId).distinct().toList();
        List<Long> auditorIds = list.stream().map(OrganizerApplication::getAuditorId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, User> userMap = new HashMap<>();
        for (Long uid : userIds) {
            User u = userMapper.selectById(uid);
            if (u != null) userMap.put(uid, u);
        }
        for (Long uid : auditorIds) {
            if (userMap.containsKey(uid)) continue;
            User u = userMapper.selectById(uid);
            if (u != null) userMap.put(uid, u);
        }
        return list.stream().map(app -> {
            User u = userMap.get(app.getApplicantId());
            User auditor = app.getAuditorId() == null ? null : userMap.get(app.getAuditorId());
            return ApplicationVO.builder()
                    .appId(app.getAppId())
                    .applicantId(app.getApplicantId())
                    .applicantNo(u == null ? null : userService.toVO(u).getUserNo())
                    .applicantName(u == null ? null : u.getName())
                    .applicantPhone(u == null ? null : u.getPhone())
                    .reason(app.getReason())
                    .materialUrl(app.getMaterialUrl())
                    .submittedAt(app.getSubmittedAt())
                    .auditStatus(app.getAuditStatus())
                    .auditorId(app.getAuditorId())
                    .auditorName(auditor == null ? null : auditor.getName())
                    .auditedAt(app.getAuditedAt())
                    .build();
        }).toList();
    }
}
