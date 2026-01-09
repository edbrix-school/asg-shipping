package com.asg.shipping.shippingmanifestcorrector.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating Shipping Manifest Corrector record
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManifestCorrectorCreateDTO {

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @NotNull(message = "BL number is required")
    private String blNumber;

    private String doReprint;
    private String containerReprint;
    private String returnReprint;
    private String blReprint;
    private String issueType;
    private Long consigneePoid;
    private Long notifyPoid;
    private String shipperEdiName;
    private String remarks;
    private String blType;
    private String demRefund;
    private String holdCanDo;
    private String holdReason;
    private Long payableGlPoid;
    private Long incomeGlPoid;
    private String payingTo;
    private String demPayType;
    private Long demCustomerPoid;
    private Long placeOfDeliveryPoid;
    private Long placeOfReceiptPoid;
    private String blPlaceReceipt;
    private String blPlaceLoad;
    private String blFinalDestination;
    private String blPlaceDischargeDesc;
    private Long portOfLoadingPoid;
    private Long portOfDischargePoid;
    private Long voyageTransactionPoid;

    private List<ManifestCorrectorChargeDtlDto> chargesDetails;
    private List<ManifestCorrectorContainerDtlDto> containerDetails;
}

