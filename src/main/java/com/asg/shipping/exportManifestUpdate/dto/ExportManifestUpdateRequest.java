package com.asg.shipping.exportManifestUpdate.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Combined Request DTO for Export Manifest BL and Details.
 *
 * The FE sends all header fields flat at the root level (not nested under "header").
 * @JsonUnwrapped tells Jackson to map those root-level fields into the ExportManifestBlRequest object.
 *
 * @Valid cascades bean-validation (e.g. @Size limits) into the header and every detail
 * row so oversized values are rejected with a 400 before reaching the database.
 */
@Data
public class ExportManifestUpdateRequest {

    @Valid
    @JsonUnwrapped
    private ExportManifestBlRequest header;

    @Valid
    private List<GeneralCargoDetailDto> generalCargoDetails;

    @Valid
    private List<ContainerDetailDto> containerDetails;

    @Valid
    private List<CargoDescriptionDto> cargoDescription;

    @Valid
    private List<CargoMarksDto> cargoMarks;

    // Simple single-string alternatives — FE can pass just a plain text value
    // and it will be saved as a single record in SHIP_BL_MANIFEST_CARGO_DTL.
    // The list-based properties above remain available for the full row-level flow.
    @Size(max = 1000, message = "Cargo Description must not exceed 1000 characters")
    private String simpleCargoDescription;

    @Size(max = 1000, message = "Cargo Marks must not exceed 1000 characters")
    private String simpleCargoMarks;

    @Valid
    private List<ChargeDetailDto> chargeDetails;
}
