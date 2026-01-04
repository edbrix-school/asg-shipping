package com.asg.shipping.demurragedetentionpayabletransfer.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for SHIP_DEM_DETN_TRANSFER_HDR
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemurrageDetentionPayableTransferDto {

    private Long transactionPoid;
    private Long groupPoid;
    private Long companyPoid;
    private String docRef;
    private LocalDate transactionDate;
    private Long linePoid;
    private LovGetListDto linePoidDet; // LOV data
    private String blType;
    private LocalDate emptyFromDate;
    private LocalDate emptyToDate;
    private Long payableGlPoid;
    private LovGetListDto payableGlPoidDet; // LOV data
    private Long incomeGlPoid;
    private LovGetListDto incomeGlPoidDet; // LOV data
    private String deleted;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;

    // Detail tables
    private List<DemurrageDetentionTransferDetailDto> transferDetails;
    private List<DemurrageDetentionTransferBillDetailDto> billDetails;
}
