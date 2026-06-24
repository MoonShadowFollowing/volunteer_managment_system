package edu.scau.vms.module.integration.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SyncResult {
    private int created;
    private int updated;
    private int total;
}
