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

// 消息分两类：业务通知（receiver 实化）+ 系统公告（一条原始 + 给每个目标实化一条）
// 实化的好处是 is_read 能精准到人，缺点就是公告会膨胀（200 人就 200 条）
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    // 个人收件箱按身份过滤：志愿者只看自己份的 + 全员的，组织者那栏同理
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

    // 历史公告：只看那条 receiver_id=null 的原始记录，实化条不重复展示
    public PageResult<MessageVO> notices(Long page, Long size) {
        LambdaQueryWrapper<Message> qw = new LambdaQueryWrapper<>();
        qw.isNull(Message::getReceiverId)
          .eq(Message::getMsgType, MsgType.SYS_NOTICE)
          .orderByDesc(Message::getSendTime);
        Page<Message> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<Message> result = messageMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), result.getRecords().stream().map(this::toVO).toList());
    }

    // 管理员发公告：先存一条 receiver=null 的原始留底，再给每个目标用户复制一条实化的
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

    // 业务通知（报名审核、工时更新等）单点投递，scope 给前端用来区分身份栏
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

    // 按 targets 列表算出要发给哪些人，没勾就空集
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