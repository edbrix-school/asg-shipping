package com.asg.shipping.demurragedetentionpayabletransfer.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for SHIP_DEM_DETN_TRANSFER_DTL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemurrageDetentionTransferDetailDto {

    private Long detRowId;
    private Long mainfestTransactionPoid;
    private LovGetListDto mainfestTransactionPoidDet; // LOV data
    private Long linePoid;
    private LovGetListDto linePoidDet; // LOV data
    private String jobNo;
    private String consignee;
    private String notify;
    private String blNumber;
    private String containerNo;
    private LocalDate sailDate;
    private LocalDate arrivalDate;
    private LocalDate emptyIn;
    private String equipmentIsoType;
    private LovGetListDto equipmentIsoTypeDet; // LOV data
    private BigDecimal demurrageAcutal;
    private BigDecimal extraFreeDays;
    private BigDecimal extraFreeDaysPrnpls;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalCollectedDays;
    private BigDecimal totalCollectedAmt;
    private BigDecimal totalShortExcessAmount;
    private BigDecimal totalPayableAmount;
    private BigDecimal totalIncomeAmount;
    private BigDecimal netIncomeAmt;
    private String isSelect;
}
