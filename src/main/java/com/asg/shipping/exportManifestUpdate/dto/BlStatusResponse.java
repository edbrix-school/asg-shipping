package com.asg.shipping.exportManifestUpdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for BL status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlStatusResponse {
    private String status;
    private String displayInfo;
    private StatusDetails statusDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusDetails {
        private String jobNo;
        private String line;
        private String vessel;
        private String voyageNo;
        private String arrivalDt;
        private String sailDt;
        private String blNo;
        private String jobStatus;
    }
}
