package com.asg.shipping.linepayabletransfetasperreporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for updating Line Payable Transfer As Per Reporting record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinePayableTransferReportingUpdateDTO {

    private LocalDate transactionDate;
    private Long linePoid;
    private String blType;
    private LocalDate reportStartDate;
    private LocalDate reportEndDate;
    private String docRef;

    private List<LinePayableTransferReportingDtlDto> details;
}
