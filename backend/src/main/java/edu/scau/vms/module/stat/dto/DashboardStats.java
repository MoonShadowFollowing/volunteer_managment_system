package edu.scau.vms.module.stat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/** 按角色返回不同字段。前端只关心需要的几个字段。 */
@Data
@Builder
public class DashboardStats {

    /** 角色：volunteer/organizer/admin/superadmin */
    private String role;

    /** 所有数值聚合到 metrics 里，方便扩展 */
    private Map<String, Object> metrics;
}
