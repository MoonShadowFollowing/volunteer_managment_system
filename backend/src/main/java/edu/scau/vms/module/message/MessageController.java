package edu.scau.vms.module.message;

import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.Result;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.message.dto.MessageVO;
import edu.scau.vms.module.message.dto.NoticeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Message", description = "消息与公告 FR-10")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "我的消息（含公告实化）")
    @GetMapping("/mine")
    public Result<PageResult<MessageVO>> mine(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String role) {
        return Result.ok(messageService.mine(me.userId(), page, pageSize, type, role));
    }

    @Operation(summary = "历史公告列表")
    @GetMapping("/notices")
    @PreAuthorize("hasAuthority('ADM')")
    public Result<PageResult<MessageVO>> notices(
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.ok(messageService.notices(page, pageSize));
    }

    @Operation(summary = "管理员发公告")
    @PostMapping("/notice")
    @PreAuthorize("hasAuthority('ADM')")
    public Result<Void> notice(@Valid @RequestBody NoticeRequest req) {
        messageService.broadcast(req);
        return Result.ok();
    }

    @Operation(summary = "未读消息数")
    @GetMapping("/unread")
    public Result<Integer> unread(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false) String role) {
        return Result.ok(messageService.countUnread(me.userId(), role));
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> readAll(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false) String role) {
        messageService.markAllRead(me.userId(), role);
        return Result.ok();
    }
}
