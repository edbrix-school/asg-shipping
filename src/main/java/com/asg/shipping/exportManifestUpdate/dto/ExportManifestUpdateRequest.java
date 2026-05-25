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

    private List<ChargeDetailDto> chargeDetails;
}
