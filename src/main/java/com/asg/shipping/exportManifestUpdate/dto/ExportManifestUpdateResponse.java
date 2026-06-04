package com.asg.shipping.exportManifestUpdate.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;


/**
 * Combined Response DTO for Export Manifest BL and Details
 */
@Data
public class ExportManifestUpdateResponse {
    
    private ExportManifestBlResponse header;
    
    private List<GeneralCargoDetailDto> generalCargoDetails;
    
    private List<ContainerDetailDto> containerDetails;
    
    private List<CargoDescriptionDto> cargoDescription;

    private List<CargoMarksDto> cargoMarks;

    // Simple single-string responses — echoes back the saved text
    private String simpleCargoDescription;

    private String simpleCargoMarks;

    private String displayTopInfoExportBLS;

    private Map<String, Object> chargeDetails;
}
