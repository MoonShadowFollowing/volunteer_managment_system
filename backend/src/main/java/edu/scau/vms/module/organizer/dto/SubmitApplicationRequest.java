package edu.scau.vms.module.organizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitApplicationRequest {

    @NotBlank
    @Size(max = 500)
    private String reason;

    @NotBlank
    @Size(max = 255)
    private String materialUrl;
}
