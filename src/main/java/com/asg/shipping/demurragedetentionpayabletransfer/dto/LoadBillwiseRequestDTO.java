package com.asg.shipping.demurragedetentionpayabletransfer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for loading bill-wise settlement data for selected containers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadBillwiseRequestDTO {

    @NotEmpty(message = "At least one container must be selected")
    private List<SelectedContainer> selectedContainers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SelectedContainer {
        @NotNull(message = "Detail row ID is required")
        private Long detRowId;

        @NotNull(message = "Manifest transaction POID is required")
        private Long mainfestTransactionPoid;

        @NotNull(message = "Container number is required")
        private String containerNo;

        private String blNumber;
    }
}
