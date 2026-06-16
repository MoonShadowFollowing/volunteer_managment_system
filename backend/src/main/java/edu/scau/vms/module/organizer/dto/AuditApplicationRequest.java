package edu.scau.vms.module.organizer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditApplicationRequest {

    @NotNull
    private Boolean approve;
}
