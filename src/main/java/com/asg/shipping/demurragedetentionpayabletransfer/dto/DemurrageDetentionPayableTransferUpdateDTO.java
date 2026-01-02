package com.asg.shipping.demurragedetentionpayabletransfer.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for updating an existing Demurrage/Detention Payable Transfer record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemurrageDetentionPayableTransferUpdateDTO {

    private LocalDate transactionDate;
    private Long linePoid;

    @Pattern(regexp = "^(IMPORT|EXPORT)?$", message = "BL Type must be IMPORT or EXPORT")
    private String blType;

    private LocalDate emptyFromDate;
    private LocalDate emptyToDate;
    private Long payableGlPoid;
    private Long incomeGlPoid;

    // Detail tables
    private List<DemurrageDetentionTransferDetailDto> transferDetails;
    private List<DemurrageDetentionTransferBillDetailDto> billDetails;
}
