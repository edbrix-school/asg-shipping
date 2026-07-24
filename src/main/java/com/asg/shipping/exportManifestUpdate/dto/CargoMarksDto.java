package com.asg.shipping.exportManifestUpdate.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for Cargo Marks (DESCRIPTION_TYPE='MARKS').
 *
 * String @Size limits mirror the column widths on SHIP_BL_MANIFEST_CARGO_DTL.
 */
@Data
public class CargoMarksDto {
    private Long detRowId;

    @Size(max = 1000, message = "Marks Description must not exceed 1000 characters")
    private String cargoDescription;

    @Size(max = 10, message = "Description Type must not exceed 10 characters")
    private String descriptionType;
    private Long recordOrder;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private ActionType actionType;
}

