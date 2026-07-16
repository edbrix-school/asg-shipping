package com.asg.shipping.demurragedetentionpayabletransfer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for updating free days for containers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFreeDaysRequestDTO {

    @NotNull(message = "Container updates are required")
    @Valid
    private List<ContainerFreeDaysUpdate> containerUpdates;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContainerFreeDaysUpdate {
        private Long transactionPoid;
        
        @NotNull(message = "Detail row ID is required")
        private Long detRowId;

        @NotNull(message = "Manifest transaction POID is required")
        private Long mainfestTransactionPoid;

        @NotNull(message = "Container number is required")
        private String containerNo;

        @NotNull(message = "Extra free days principal is required")
        private BigDecimal extraFreeDaysPrnpls;
    }
}
