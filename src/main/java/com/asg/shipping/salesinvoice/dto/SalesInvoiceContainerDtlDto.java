package com.asg.shipping.salesinvoice.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for Sales Invoice Container Detail
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesInvoiceContainerDtlDto {

    private Long detRowId;
    private Long blPoid;
    private LovGetListDto blDet;
    private String containerSocYn;
    private String containerNo;
    private LocalDate dmFrmDate;
    private LocalDate dmToDate;
    private Integer dmDays;
    private BigDecimal dmChargeAmt;
    private Integer freeDays;
    private String equipmentIsoType;
    private String dlvFormPrinted;
    private String rtnFormPrinted;
    private LocalDate emptyIn;
    private Long cntTaxPoid;
    private LovGetListDto cntTaxDet;
    private BigDecimal cntTaxPercentage;
    private BigDecimal cntTaxAmount;
}

