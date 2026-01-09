package com.asg.shipping.shippingmanifestcorrector.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Shipping Manifest Corrector
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManifestCorrectorDto {

    private Long transactionPoid;
    private LocalDate transactionDate;
    private String docRef;
    private String blNumber;
    private LovGetListDto blNumberDet;
    private String doReprint;
    private String containerReprint;
    private String returnReprint;
    private String blReprint;
    private String issueType;
    private LovGetListDto issueTypeDet;
    private Long consigneePoid;
    private LovGetListDto consigneeDet;
    private Long notifyPoid;
    private LovGetListDto notifyDet;
    private String shipperEdiName;
    private String remarks;
    private String blType;
    private String demRefund;
    private String holdCanDo;
    private String holdReason;
    private LovGetListDto holdReasonDet;
    private Long payableGlPoid;
    private Long incomeGlPoid;
    private String payingTo;
    private String demPayType;
    private Long demCustomerPoid;
    private Long placeOfDeliveryPoid;
    private LovGetListDto placeOfDeliveryDet;
    private Long placeOfReceiptPoid;
    private LovGetListDto placeOfReceiptDet;
    private String blPlaceDelivery;
    private String blPlaceLoad;
    private String blFinalDestination;
    private String blPlaceDischargeDesc;
    private Long portOfLoadingPoid;
    private LovGetListDto portOfLoadingDet;
    private Long portOfDischargePoid;
    private LovGetListDto portOfDischargeDet;
    private Long voyageTransactionPoid;
    private LovGetListDto voyageTransactionDet;
    private String voyageNumber;
    private String deleted;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private Long companyPoid;
    private LovGetListDto companyDet;

    // Detail records
    private List<ManifestCorrectorChargeDtlDto> chargesDetails;
    private List<ManifestCorrectorContainerDtlDto> containerDetails;
}

