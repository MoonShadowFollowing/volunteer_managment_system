package edu.scau.vms.module.message.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MessageVO {

    private Long msgId;
    private String type;
    private String title;
    private String content;

    /** 收到时间（前端表头叫"时间"） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;

    @JsonProperty("isRead")
    private boolean isRead;

    /** 公告专用：通知范围（拆数组返） */
    private List<String> targets;
}
