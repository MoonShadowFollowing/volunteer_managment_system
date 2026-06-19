package edu.scau.vms.module.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.constant.MsgType;
import edu.scau.vms.common.constant.Role;
import edu.scau.vms.module.message.dto.MessageVO;
import edu.scau.vms.module.message.dto.NoticeRequest;
import edu.scau.vms.module.message.entity.Message;
import edu.scau.vms.module.message.mapper.MessageMapper;
import edu.scau.vms.module.user.entity.User;
import edu.scau.vms.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    /** 个人收件箱，按身份隔离：只显示 targetScope 匹配当前角色或无隔离标记的消息 */
    public PageResult<MessageVO> mine(Long userId, Long page, Long size, String type, String role) {
        LambdaQueryWrapper<Message> qw = new LambdaQueryWrapper<>();
        qw.eq(Message::getReceiverId, userId);
        if (type != null && !type.isBlank()) qw.eq(Message::getMsgType, type);
        if (role != null && !role.isBlank()) {
            qw.and(w -> w.eq(Message::getTargetScope, role).or().isNull(Message::getTargetScope));
        }
        qw.orderByDesc(Message::getSendTime);
        Page<Message> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<Message> result = messageMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), result.getRecords().stream().map(this::toVO).toList());
    }

    /** 历史公告列表（receiver_id 为 NULL 的原始公告） */
    public PageResult<MessageVO> notices(Long page, Long size) {
        LambdaQueryWrapper<Message> qw = new LambdaQueryWrapper<>();
        qw.isNull(Message::getReceiverId)
          .eq(Message::getMsgType, MsgType.SYS_NOTICE)
          .orderByDesc(Message::getSendTime);
        Page<Message> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<Message> result = messageMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), result.getRecords().stream().map(this::toVO).toList());
    }

    /** 管理员发公告：保留 1 条原始 + 实化到每个目标用户 */
    @Transactional
    public void broadcast(NoticeRequest req) {
        LocalDateTime now = LocalDateTime.now();
        String rawScope = String.join(",", req.getTargets());
        String roleScope = computeScope(req.getTargets());

        Message origin = new Message();
        origin.setMsgType(MsgType.SYS_NOTICE);
        origin.setTitle(req.getTitle());
        origin.setContent(req.getContent());
        origin.setTargetScope(rawScope);
        origin.setSendTime(now);
        origin.setIsRead(false);
        messageMapper.insert(origin);

        for (User u : findTargetUsers(req.getTargets())) {
            Message m = new Message();
            m.setMsgType(MsgType.SYS_NOTICE);
            m.setTitle(req.getTitle());
            m.setContent(req.getContent());
            m.setReceiverId(u.getUserId());
            m.setTargetScope(roleScope);
            m.setSendTime(now);
            m.setIsRead(false);
            messageMapper.insert(m);
        }
    }

    /** 业务通知：直送某一用户，targetScope 控制身份隔离 */
    public void sendDirect(Long receiverId, String type, String title, String content, String scope) {
        Message m = new Message();
        m.setMsgType(type);
        m.setTitle(title);
        m.setContent(content);
        m.setReceiverId(receiverId);
        m.setTargetScope(scope);
        m.setSendTime(LocalDateTime.now());
        m.setIsRead(false);
        messageMapper.insert(m);
    }

    public int countUnread(Long userId, String scope) {
        return messageMapper.countUnread(userId, scope);
    }

    public void markAllRead(Long userId, String scope) {
        messageMapper.markAllRead(userId, scope);
    }

    private String computeScope(List<String> targets) {
        boolean wantVol = targets.contains("全体志愿者");
        boolean wantOrg = targets.contains("全体组织者");
        if (wantVol && !wantOrg) return "volunteer";
        if (wantOrg && !wantVol) return "organizer";
        return null;
    }

    private List<User> findTargetUsers(List<String> targets) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        boolean wantVol = targets.contains("全体志愿者");
        boolean wantOrg = targets.contains("全体组织者");
        if (wantVol && !wantOrg) {
            qw.eq(User::getRole, Role.VOLUNTEER);
        } else if (wantOrg && !wantVol) {
            qw.eq(User::getIsOrganizer, true);
        } else if (wantVol && wantOrg) {
            qw.eq(User::getRole, Role.VOLUNTEER).or().eq(User::getIsOrganizer, true);
        } else {
            return List.of();
        }
        return userMapper.selectList(qw);
    }

    private MessageVO toVO(Message m) {
        List<String> targets = m.getTargetScope() == null
                ? List.of()
                : Arrays.stream(m.getTargetScope().split(",")).filter(s -> !s.isBlank()).collect(Collectors.toList());
        return MessageVO.builder()
                .msgId(m.getMsgId())
                .type(m.getMsgType())
                .title(m.getTitle())
                .content(m.getContent())
                .time(m.getSendTime())
                .isRead(Boolean.TRUE.equals(m.getIsRead()))
                .targets(targets)
                .build();
    }
}