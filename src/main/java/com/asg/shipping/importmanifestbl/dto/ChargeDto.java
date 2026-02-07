package com.asg.shipping.importmanifestbl.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChargeDto {

    private Long detRowId;
    private Long chargePoid;
    private String printGroup;
    private Long chargeTypePoid;
    private Long basisPoid;

    private String currencyCode;
    private Long rate;

    private Long quantity;
    private Long buy;
    private Long buyAmount;

    private Long sell;
    private Long sellAmount;

    private Long taxPercentage;
    private Long taxAmount;
    private Long gain;

    private Long freightTypePoid;
    private Long paidAtPortPoid;

    private String chargeDescription;
    private Long taxPoid;
    private Long discountPercentage;

    private LocalDate demurrageChargesTillDate;
    private String actionType;


}
