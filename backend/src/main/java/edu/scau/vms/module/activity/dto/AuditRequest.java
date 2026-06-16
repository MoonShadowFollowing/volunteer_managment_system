package edu.scau.vms.module.activity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditRequest {

    @NotNull
    private Boolean approve;
}
