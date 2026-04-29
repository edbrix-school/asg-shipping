package com.asg.shipping.demurragedetentionpayabletransfer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @NotNull(message = "BL Type is required for bill-wise data loading")
    @Pattern(regexp = "^(IMPORT|EXPORT)$", message = "BL Type must be IMPORT or EXPORT")
    private String blType;

    @NotEmpty(message = "At least one container must be selected")
    private List<SelectedContainer> selectedContainers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SelectedContainer {
        private Long detRowId;

        @NotNull(message = "Manifest transaction POID is required")
        private Long mainfestTransactionPoid;

        private String containerNo;
        private String blNumber;

        // Optional for the pre-create load-billwise flow.
        private BigDecimal totalPayableAmount;
        private BigDecimal totalIncomeAmount;
        private String isSelect;
    }
}
