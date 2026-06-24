package edu.scau.vms.module.integration.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EduValidateResult {
    private boolean valid;
    private String studentId;
    private String name;
}
