package com.asg.shipping.shippingmanifestcorrector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Shipping Manifest Corrector Container Detail
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManifestCorrectorContainerDtlDto {

    private String actionType;
    private Long detRowId;
    private String containerNumber;
    private String containerType;
    private String equipmentIsoType;
    private String isSelectedDlv;
    private String isSelectedRtn;
}
