package com.asg.shipping.importManifestUpdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralCargoRequestDto {
    private Long detRowId;
    private Long comodityPoid;
    private String cargoDescription;
    private Long quantity;
    private Long grsVolume;
    private Long grsWeight;
    private Long netVolume;
    private Long netWeight;
    private Long tareWeight;
    private Long noOfPacks;
    private String packUnit;
    private Long destinationPortPoid;
}
