package com.asg.shipping.demurragedetentionpayabletransfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new Demurrage/Detention Payable Transfer record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemurrageDetentionPayableTransferCreateDTO {

    private LocalDate transactionDate;

    @NotNull(message = "Line POID is required")
    private Long linePoid;

    @NotNull(message = "BL Type is required")
    @Pattern(regexp = "^(IMPORT|EXPORT)$", message = "BL Type must be IMPORT or EXPORT")
    private String blType;

    private LocalDate emptyFromDate;
    private LocalDate emptyToDate;

    @NotNull(message = "Payable GL POID is required")
    private Long payableGlPoid;

    @NotNull(message = "Income GL POID is required")
    private Long incomeGlPoid;

    // Detail tables
    private List<DemurrageDetentionTransferDetailDto> transferDetails;
    private List<DemurrageDetentionTransferBillDetailDto> billDetails;
}
