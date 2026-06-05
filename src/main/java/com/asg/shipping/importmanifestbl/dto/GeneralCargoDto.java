package com.asg.shipping.importmanifestbl.dto;

import com.asg.shipping.common.dto.LovItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralCargoDto {

    private Long detRowId;
    private String description;
    private Long commodityPoid;
    private LovItem commodityDet;

    private Long volume;
    private Long grossWeight;
    private Long netWeight;
    private Long tareWeight;

    private Long packs;
    private String unit;
    private Long quantity;

    private Long destinationPortPoid;
    private LovItem destinationPortDet;
    private String actionType;
}
