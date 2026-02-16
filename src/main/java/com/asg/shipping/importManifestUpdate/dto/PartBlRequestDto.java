package com.asg.shipping.importManifestUpdate.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartBlRequestDto {

    private Long detRowId;
    private String partBlNumber;
    private String shipperName;
    private String consigneeName;
    private String containerNo;
    private String cargoDescription;
    private Long comodityPoid;
    private Long netWeight;
    private Long netVolume;
    private Long noOfPacks;
    private String packUnit;
    private String actionType;
}
