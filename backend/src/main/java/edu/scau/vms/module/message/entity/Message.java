package edu.scau.vms.module.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {

    @TableId(value = "msg_id", type = IdType.AUTO)
    private Long msgId;

    private String msgType;

    private String title;

    private String content;

    private Long receiverId;

    private String targetScope;

    private LocalDateTime sendTime;

    private Boolean isRead;
}
