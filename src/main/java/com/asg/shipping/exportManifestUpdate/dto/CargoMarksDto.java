package com.asg.shipping.exportManifestUpdate.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for Cargo Marks (DESCRIPTION_TYPE='MARKS')
 */
@Data
public class CargoMarksDto {
    private Long detRowId;
    private String cargoDescription;
    private String descriptionType;
    private Long recordOrder;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String actionType;
}

