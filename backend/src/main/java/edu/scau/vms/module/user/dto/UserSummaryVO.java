package edu.scau.vms.module.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserSummaryVO {

    private Long userId;
    private String username;
    private String name;
    private String role;
    private String phone;
    private String userNo;   // 展示编号：USR-{5位}/ADM-{5位}/ORG-{5位}/VOL-{5位}（按是否管理员/组织者优先标）
    @JsonProperty("isOrganizer")
    private boolean isOrganizer;
    @JsonProperty("isAdmin")
    private boolean isAdmin;

    /** 组织者视角：已发布活动数（仅在 organizers 列表返回） */
    private Long actCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
