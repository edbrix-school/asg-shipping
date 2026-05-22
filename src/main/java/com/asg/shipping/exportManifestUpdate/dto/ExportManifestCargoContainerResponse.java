package com.asg.shipping.exportManifestUpdate.dto;

import lombok.Data;
import java.util.List;

/**
 * Response DTO containing Container Details and Cargo Description
 */
@Data
public class ExportManifestCargoContainerResponse {
    private List<ContainerDetailDto> containerDetails;
    private List<CargoDescriptionDto> cargoDescription;
    private List<CargoMarksDto> cargoMarks;
}
