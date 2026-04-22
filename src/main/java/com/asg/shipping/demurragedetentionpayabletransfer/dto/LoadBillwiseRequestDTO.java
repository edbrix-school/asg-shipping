package com.asg.shipping.demurragedetentionpayabletransfer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    // Required for pre-create flow (loadBillwiseDataBeforeCreate) to determine GL_CODE
    private String blType;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SelectedContainer {
        @NotNull(message = "Detail row ID is required")
        private Long detRowId;

        @NotNull(message = "Manifest transaction POID is required")
        private Long mainfestTransactionPoid;

        private String containerNo;

        private String blNumber;

        // Required for pre-create flow to compute DrAmt/CrAmt
        private BigDecimal totalPayableAmount;
        private BigDecimal totalIncomeAmount;
    }
}
