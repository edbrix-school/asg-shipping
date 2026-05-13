package com.asg.shipping.shippingmanifestcorrector.dto;

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
    private Long transactionPoid;
    private Long consigneePoid;
    private String issueType;
    private Long notifyPoid;
    private String shipperEdiName;
    private String blType;
    private String holdCanDo;
    private String holdReason;
    private Long payableGlPoid;
    private Long incomeGlPoid;
    private Long placeOfDeliveryPoid;
    private Long placeOfReceiptPoid;
    private String blPlaceReceipt;
    private String blPlaceLoad;
    private String blFinalDestination;
    private String blPlaceDischargeDesc;
    private Long portOfLoadingPoid;
    private Long portOfDischargePoid;
    private Long voyageTransactionPoid;
}
