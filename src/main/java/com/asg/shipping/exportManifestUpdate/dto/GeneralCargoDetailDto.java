package com.asg.shipping.exportManifestUpdate.dto;

import com.asg.shipping.common.dto.LovItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for General Cargo Details
 */
@Data
public class GeneralCargoDetailDto {
    private Long detRowId;
    private String actionType; // CREATED, UPDATED, DELETED, or empty for no changes
    private Long comodityPoid;
    private LovItem comodityDet; // LOV: COMODITY
    private String cargoDescription;
    private BigDecimal quantity;
    private BigDecimal grsVolume;
    private BigDecimal grsWeight;
    private BigDecimal netVolume;
    private BigDecimal netWeight;
    private BigDecimal tareWeight;
    private BigDecimal noOfPacks;
    private String packUnit;
    private Long destinationPortPoid;
    private LovItem destinationPortDet; // LOV: PORT_MASTER
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
}

