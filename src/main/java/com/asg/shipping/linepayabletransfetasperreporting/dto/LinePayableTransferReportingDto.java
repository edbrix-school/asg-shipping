package com.asg.shipping.linepayabletransfetasperreporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Line Payable Transfer As Per Reporting
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinePayableTransferReportingDto {

    private Long transactionPoid;
    private Long groupPoid;
    private Long companyPoid;
    private LocalDate transactionDate;
    private Long linePoid;
    private String lineName;
    private String lineCode;
    private String blType;
    private LocalDate reportStartDate;
    private LocalDate reportEndDate;
    private String docRef;
    private String deleted;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    // Detail records
    private List<LinePayableTransferReportingDtlDto> details;
}
