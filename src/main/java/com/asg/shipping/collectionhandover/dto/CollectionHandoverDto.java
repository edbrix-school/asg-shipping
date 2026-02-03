package com.asg.shipping.collectionhandover.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Collection Handover
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionHandoverDto {

    private Long transactionPoid;
    private LocalDateTime transactionDate;
    private Long groupPoid;
    private Long companyPoid;
    private String docRef;
    private String locationCode;
    private BigDecimal cashAmount;
    private BigDecimal chequeAmount;
    private BigDecimal outstandingAmount;
    private BigDecimal totalAmount;
    private Integer noofChqs;
    private String locRemarks;
    private String verifiedRcvd;
    private String mainOfcRemarks;
    private String deleted;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    // Detail records
    private List<CollectionHandoverDetailDto> details;
}

