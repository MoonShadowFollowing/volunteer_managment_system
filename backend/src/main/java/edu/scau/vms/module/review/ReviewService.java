package edu.scau.vms.module.review;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.constant.AttendStatus;
import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.constant.ReviewerRole;
import edu.scau.vms.common.exception.BizException;
import edu.scau.vms.module.activity.entity.Activity;
import edu.scau.vms.module.activity.mapper.ActivityMapper;
import edu.scau.vms.module.attendance.entity.Attendance;
import edu.scau.vms.module.attendance.mapper.AttendanceMapper;
import edu.scau.vms.module.review.dto.ReviewVO;
import edu.scau.vms.module.review.dto.SubmitReviewRequest;
import edu.scau.vms.module.review.entity.ActivityReview;
import edu.scau.vms.module.review.mapper.ActivityReviewMapper;
import edu.scau.vms.module.user.entity.User;
import edu.scau.vms.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 活动评价：志愿者评组织者、组织者评签退过的志愿者，双向打分
// 防重复评价就靠 DB 那个 UNIQUE 兜底，Service 层再多查一次只是给个友好提示
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ActivityReviewMapper reviewMapper;
    private final ActivityMapper activityMapper;
    private final AttendanceMapper attendanceMapper;
    private final UserMapper userMapper;

    @Transactional
    public Long submit(Long reviewerId, SubmitReviewRequest req) {
        Activity a = activityMapper.selectById(req.getActivityId());
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        // 自己评自己就别玩了
        if (reviewerId.equals(req.getTargetId())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不能评价自己");
        }
        User target = userMapper.selectById(req.getTargetId());
        if (target == null) throw new BizException(ErrorCode.NOT_FOUND, "被评价用户不存在");

        // 角色靠 reviewerId 是不是这个活动的 organizer 判，前端传啥都不信
        boolean reviewerIsOrganizer = a.getOrganizerId().equals(reviewerId);
        String role;
        if (reviewerIsOrganizer) {
            // 组织者评志愿者：那位志愿者得真的在我这活动签过退
            boolean signed = attendanceMapper.selectCount(
                    new LambdaQueryWrapper<Attendance>()
                            .eq(Attendance::getActivityId, req.getActivityId())
                            .eq(Attendance::getVolunteerId, req.getTargetId())
                            .eq(Attendance::getStatus, AttendStatus.CHECKED_OUT)) > 0;
            if (!signed) {
                throw new BizException(ErrorCode.BIZ_CONFLICT, "该志愿者尚未在此活动签退，暂不能评价");
            }
            role = ReviewerRole.ORGANIZER;
        } else {
            // 志愿者评组织者：target 必须就是这场活动的 organizer，不能跨场乱评
            if (!req.getTargetId().equals(a.getOrganizerId())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "志愿者只能评价该活动的组织者");
            }
            // 而且我自己得签退过，没参加完就别打分了
            boolean signed = attendanceMapper.selectCount(
                    new LambdaQueryWrapper<Attendance>()
                            .eq(Attendance::getActivityId, req.getActivityId())
                            .eq(Attendance::getVolunteerId, reviewerId)
                            .eq(Attendance::getStatus, AttendStatus.CHECKED_OUT)) > 0;
            if (!signed) {
                throw new BizException(ErrorCode.BIZ_CONFLICT, "您尚未在此活动签退，暂不能评价");
            }
            role = ReviewerRole.VOLUNTEER;
        }

        // 防重复：DB 上 (activity_id, reviewer_id, target_id) 有 UNIQUE，
        // 这里先查一次是为了给用户一个能看懂的提示，免得抛 SQL 异常
        Long dup = reviewMapper.selectCount(
                new LambdaQueryWrapper<ActivityReview>()
                        .eq(ActivityReview::getActivityId, req.getActivityId())
                        .eq(ActivityReview::getReviewerId, reviewerId)
                        .eq(ActivityReview::getTargetId, req.getTargetId()));
        if (dup > 0) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "您已对该对象在此活动评价过，不能重复评价");
        }

        ActivityReview r = new ActivityReview();
        r.setActivityId(req.getActivityId());
        r.setReviewerId(reviewerId);
        r.setReviewerRole(role);
        r.setTargetId(req.getTargetId());
        r.setRating(req.getRating());
        r.setComment(req.getComment());
        reviewMapper.insert(r);
        return r.getReviewId();
    }

    public PageResult<ReviewVO> mine(Long reviewerId, Long page, Long size) {
        LambdaQueryWrapper<ActivityReview> qw = new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getReviewerId, reviewerId)
                .orderByDesc(ActivityReview::getCreatedAt);
        Page<ActivityReview> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<ActivityReview> result = reviewMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), toVOs(result.getRecords()));
    }

    public PageResult<ReviewVO> received(Long targetId, Long page, Long size) {
        LambdaQueryWrapper<ActivityReview> qw = new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getTargetId, targetId)
                .orderByDesc(ActivityReview::getCreatedAt);
        Page<ActivityReview> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<ActivityReview> result = reviewMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), toVOs(result.getRecords()));
    }

    public PageResult<ReviewVO> byActivity(Long activityId, Long page, Long size) {
        LambdaQueryWrapper<ActivityReview> qw = new LambdaQueryWrapper<ActivityReview>()
                .eq(ActivityReview::getActivityId, activityId)
                .orderByDesc(ActivityReview::getCreatedAt);
        Page<ActivityReview> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<ActivityReview> result = reviewMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), toVOs(result.getRecords()));
    }

    // 平均分 + 条数：activityId 传 null 就是看这人在系统里被评的全部平均
    public Map<String, Object> summary(Long targetId, Long activityId) {
        QueryWrapper<ActivityReview> qw = new QueryWrapper<ActivityReview>()
                .select("COALESCE(AVG(rating), 0) AS avg_rating", "COUNT(*) AS cnt")
                .eq("target_id", targetId);
        if (activityId != null) qw.eq("activity_id", activityId);
        List<Map<String, Object>> rows = reviewMapper.selectMaps(qw);
        Map<String, Object> out = new HashMap<>();
        if (rows.isEmpty()) {
            // 理论上聚合查询不会返回空，但保险起见兜一下
            out.put("avgRating", 0.0);
            out.put("count", 0);
        } else {
            Object avg = rows.get(0).get("avg_rating");
            Object cnt = rows.get(0).get("cnt");
            // 平均分保留两位，前端直接显示
            BigDecimal avgBd = avg == null
                    ? BigDecimal.ZERO
                    : new BigDecimal(avg.toString()).setScale(2, RoundingMode.HALF_UP);
            out.put("avgRating", avgBd);
            out.put("count", cnt == null ? 0 : Long.parseLong(cnt.toString()));
        }
        return out;
    }

    // entity → VO：用 batchIds 一次性把姓名/活动名拉回来，别让前端再调接口拼
    private List<ReviewVO> toVOs(List<ActivityReview> list) {
        if (list == null || list.isEmpty()) return List.of();
        Set<Long> uids = new HashSet<>();
        Set<Long> aids = new HashSet<>();
        for (ActivityReview r : list) {
            uids.add(r.getReviewerId());
            uids.add(r.getTargetId());
            aids.add(r.getActivityId());
        }
        Map<Long, String> uname = new HashMap<>();
        if (!uids.isEmpty()) {
            userMapper.selectBatchIds(uids).forEach(u -> uname.put(u.getUserId(), u.getName()));
        }
        Map<Long, String> aname = new HashMap<>();
        if (!aids.isEmpty()) {
            activityMapper.selectBatchIds(aids).forEach(a -> aname.put(a.getActivityId(), a.getTitle()));
        }
        List<ReviewVO> vos = new ArrayList<>(list.size());
        for (ActivityReview r : list) {
            vos.add(ReviewVO.builder()
                    .reviewId(r.getReviewId())
                    .activityId(r.getActivityId())
                    .activityName(aname.get(r.getActivityId()))
                    .reviewerId(r.getReviewerId())
                    .reviewerName(uname.get(r.getReviewerId()))
                    .reviewerRole(r.getReviewerRole())
                    .targetId(r.getTargetId())
                    .targetName(uname.get(r.getTargetId()))
                    .rating(r.getRating())
                    .comment(r.getComment())
                    .createdAt(r.getCreatedAt())
                    .build());
        }
        return vos;
    }
}
