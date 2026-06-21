package edu.scau.vms.module.review;

import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.Result;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.review.dto.ReviewVO;
import edu.scau.vms.module.review.dto.SubmitReviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 活动评价 5 个接口，鉴权统一靠 SecurityConfig + Service 里再校验
@Tag(name = "Review", description = "活动评价（双向打分）")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "提交一条评价")
    @PostMapping
    public Result<Map<String, Long>> submit(@AuthenticationPrincipal UserPrincipal me,
                                            @Valid @RequestBody SubmitReviewRequest req) {
        Long reviewId = reviewService.submit(me.userId(), req);
        return Result.ok(Map.of("reviewId", reviewId));
    }

    @Operation(summary = "我打过的评价")
    @GetMapping("/mine")
    public Result<PageResult<ReviewVO>> mine(@AuthenticationPrincipal UserPrincipal me,
                                             @RequestParam(required = false, defaultValue = "1") Long page,
                                             @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.ok(reviewService.mine(me.userId(), page, pageSize));
    }

    @Operation(summary = "别人对我的评价")
    @GetMapping("/received")
    public Result<PageResult<ReviewVO>> received(@AuthenticationPrincipal UserPrincipal me,
                                                 @RequestParam(required = false, defaultValue = "1") Long page,
                                                 @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.ok(reviewService.received(me.userId(), page, pageSize));
    }

    @Operation(summary = "某活动下所有评价")
    @GetMapping
    public Result<PageResult<ReviewVO>> byActivity(@RequestParam Long activityId,
                                                   @RequestParam(required = false, defaultValue = "1") Long page,
                                                   @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.ok(reviewService.byActivity(activityId, page, pageSize));
    }

    // activityId 选填，不传就是全平台平均
    @Operation(summary = "某人的平均分 + 条数")
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam Long targetId,
                                               @RequestParam(required = false) Long activityId) {
        return Result.ok(reviewService.summary(targetId, activityId));
    }
}
