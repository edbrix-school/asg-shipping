package com.asg.shipping.importmanifestupdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlStatusResponseDto {
    private StatusDetails status;
    private Boolean hasDo;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StatusDetails {
        private String jobNo;
        private String line;
        private String vessel;
        private String voyageNo;
        private String port;
        private String arrivalDt;
        private String blNo;
        private String doStatus;
        private String canStatus;
    }
}
