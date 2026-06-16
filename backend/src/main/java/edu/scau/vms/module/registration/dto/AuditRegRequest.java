package edu.scau.vms.module.registration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditRegRequest {

    @NotNull
    private Boolean approve;
}
