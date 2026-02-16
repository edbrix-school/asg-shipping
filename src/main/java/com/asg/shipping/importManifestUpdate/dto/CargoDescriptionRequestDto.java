package com.asg.shipping.importManifestUpdate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoDescriptionRequestDto {
    private Long detRowId;
    private String descriptionType; // DESCRIPTION / MARKS
    private String cargoDescription;
    private Long recordOrder;
    private String actionType;
}
