package com.asg.shipping.shippingmanifestcorrector.dto;

import com.asg.common.lib.dto.LovGetListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for BL after browse auto-population.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManifestCorrectorBlAutoPopulateDto {

    private Long blPoid;
    private LovGetListDto blDet;
    private Long transactionPoid;
    private Long consigneePoid;
    private LovGetListDto consigneeDet;
    private String issueType;
    private LovGetListDto issueTypeDet;
    private Long notifyPoid;
    private LovGetListDto notifyDet;
    private String shipperEdiName;
    private String blType;
    private LovGetListDto blTypeDet;
    private String holdCanDo;
    private String holdReason;
    private LovGetListDto holdReasonDet;
    private Long payableGlPoid;
    private LovGetListDto payableGlDet;
    private Long incomeGlPoid;
    private LovGetListDto incomeGlDet;
    private Long placeOfDeliveryPoid;
    private LovGetListDto placeOfDeliveryDet;
    private Long placeOfReceiptPoid;
    private LovGetListDto placeOfReceiptDet;
    private String blPlaceReceipt;
    private String blPlaceLoad;
    private String blFinalDestination;
    private String blPlaceDischargeDesc;
    private Long portOfLoadingPoid;
    private LovGetListDto portOfLoadingDet;
    private Long portOfDischargePoid;
    private LovGetListDto portOfDischargeDet;
    private Long voyageTransactionPoid;
    private LovGetListDto voyageTransactionDet;
}
