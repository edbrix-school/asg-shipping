package com.asg.shipping.linepayabletransfetasperreporting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating Line Payable Transfer As Per Reporting record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinePayableTransferReportingCreateDTO {

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @NotNull(message = "Line POID is required")
    private Long linePoid;

    @NotNull(message = "BL type is required")
    private String blType;

    @NotNull(message = "Report start date is required")
    private LocalDate reportStartDate;

    @NotNull(message = "Report end date is required")
    private LocalDate reportEndDate;

    private String docRef;

    private List<LinePayableTransferReportingDtlDto> details;
}
