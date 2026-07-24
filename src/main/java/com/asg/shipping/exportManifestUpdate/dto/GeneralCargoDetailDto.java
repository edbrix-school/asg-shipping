package com.asg.shipping.exportManifestUpdate.dto;

import com.asg.shipping.common.dto.LovItem;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for General Cargo Details.
 *
 * String @Size limits mirror the column widths on SHIP_BL_MANIFEST_GENERAL_DTL.
 */
@Data
public class GeneralCargoDetailDto {
    private Long detRowId;
    private Long comodityPoid;
    private LovItem comodityDet; // LOV: COMODITY

    @Size(max = 200, message = "Cargo Description must not exceed 200 characters")
    private String cargoDescription;
    private BigDecimal quantity;
    private BigDecimal grsVolume;
    private BigDecimal grsWeight;
    private BigDecimal netVolume;
    private BigDecimal netWeight;
    private BigDecimal tareWeight;
    private BigDecimal noOfPacks;

    @Size(max = 6, message = "Pack Unit must not exceed 6 characters")
    private String packUnit;
    private Long destinationPortPoid;
    private LovItem destinationPortDet; // LOV: PORT_MASTER
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private ActionType actionType;
}

