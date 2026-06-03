package com.asg.shipping.importmanifestbl.dto;

import com.asg.shipping.common.dto.LovItem;
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
    private String containerNo;
    private LovItem containerNoDet;
    private String cargoDescription;

    private Long commodityPoid;
    private LovItem commodityDet;
    private Long packageDetails;
    private String packUnit;

    private Long netWeight;
    private Long netVolume;
    private String actionType;
}
