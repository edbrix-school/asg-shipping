package com.asg.shipping.exportManifestUpdate.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * Combined Request DTO for Export Manifest BL and Details
 */
@Data
public class ExportManifestUpdateRequest {

    @Valid
    private ExportManifestBlRequest header;

    private List<GeneralCargoDetailDto> generalCargoDetails;

    private List<ContainerDetailDto> containerDetails;

    private List<CargoDescriptionDto> cargoDescription;

    private List<CargoMarksDto> cargoMarks;

    // Simple single-string alternatives — FE can pass just a plain text value
    // and it will be saved as a single record in SHIP_BL_MANIFEST_CARGO_DTL.
    // The list-based properties above remain available for the full row-level flow.
    private String simpleCargoDescription;

    private String simpleCargoMarks;

    private List<ChargeDetailDto> chargeDetails;
}
