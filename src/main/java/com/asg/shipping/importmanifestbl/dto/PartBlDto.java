package com.asg.shipping.importmanifestbl.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PartBlDto {

    private Long detRowId;
    private String partBlNumber;
    private String shipperName;
    private String consigneeName;

    private Long containerPoid;
    private String cargoDescription;

    private Long commodityPoid;
    private String packageDetails;
    private String packUnit;

    private Double netWeight;
    private Double netVolume;
    private String actionType;
}
