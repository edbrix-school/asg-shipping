package com.asg.shipping.importmanifestupdate.dto;

import com.asg.shipping.common.dto.LovItem;
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
    private LovItem containerNoDet;
    private String cargoDescription;
    private Long comodityPoid;
    private LovItem comodityDet;
    private Long netWeight;
    private Long netVolume;
    private Long noOfPacks;
    private String packUnit;
    private String actionType;
}
