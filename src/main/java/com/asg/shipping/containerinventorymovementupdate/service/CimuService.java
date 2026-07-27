package com.asg.shipping.containerinventorymovementupdate.service;

import com.asg.shipping.containerinventorymovementupdate.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface CimuService {
    SuggestContainerResponse suggestContainers(String query);

    QueryCimuResponse queryScreenData(QueryCimuRequest request);

    UpdateCimuResponse updateContainerData(UpdateCimuRequest request);

    SocUpdateResponse socUpdate(SocUpdateRequest request);

    DemurrageCalculateResponse calculateDemurrage(DemurrageCalculateRequest request);

    // --- SRS enhancement: Container Inspection XL Upload (merged into CIMU service) ---
    InspectionUploadResponse importFile(MultipartFile file);

    InspectionLoadResponse loadContainerDetails();

    OpenExportManifestResponse openExportManifestData(OpenExportManifestRequest request);

}


