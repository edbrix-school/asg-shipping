package com.asg.shipping.containerinventorymovementupdate.controller;

import com.asg.common.lib.annotation.AllowedAction;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.common.lib.security.util.UserContext;
import static com.asg.shipping.common.ApiResponse.success;
import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.DemurrageCalculateRequest;
import com.asg.shipping.containerinventorymovementupdate.dto.DemurrageCalculateResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.InspectionLoadResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.InspectionUploadResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.OpenExportManifestRequest;
import com.asg.shipping.containerinventorymovementupdate.dto.OpenExportManifestResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.QueryCimuRequest;
import com.asg.shipping.containerinventorymovementupdate.dto.QueryCimuResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.SocUpdateRequest;
import com.asg.shipping.containerinventorymovementupdate.dto.SocUpdateResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.SuggestContainerResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.UpdateCimuRequest;
import com.asg.shipping.containerinventorymovementupdate.dto.UpdateCimuResponse;
import com.asg.shipping.containerinventorymovementupdate.service.CimuService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/v1/container-inventory-movement-update")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CimuController {

    private final CimuService cimuService;

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @GetMapping("/containers/suggest")
    public ResponseEntity<?> suggestContainers(
            @RequestParam("query") @NotBlank String query,
            @RequestHeader("X-Document-Id") String docId
    ) {
        log.info("CIMU suggestContainers | docId={} actionRequested={} userId={} userPoid={} groupPoid={} companyPoid={} query={}",
                docId, UserContext.getActionRequested(), UserContext.getUserId(), UserContext.getUserPoid(),
                UserContext.getGroupPoid(), UserContext.getCompanyPoid(), query);
        SuggestContainerResponse response = cimuService.suggestContainers(query);
        return success("Suggestions fetched successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/query")
    public ResponseEntity<?> query(
            @Valid @RequestBody QueryCimuRequest request,
            @RequestHeader("X-Document-Id") String docId
    ) {
        log.info("CIMU query | docId={} actionRequested={} userId={} userPoid={} groupPoid={} companyPoid={} containerNo={} blNumber={}",
                docId, UserContext.getActionRequested(), UserContext.getUserId(), UserContext.getUserPoid(),
                UserContext.getGroupPoid(), UserContext.getCompanyPoid(), request.getContainerNo(), request.getBlNumber());
        QueryCimuResponse response = cimuService.queryScreenData(request);
        if (response.getErrorMessage() != null) {
            return ApiResponse.badRequest(response.getErrorMessage());
        }
        return success("Container data fetched successfully", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/update")
    public ResponseEntity<?> update(
            @Valid @RequestBody UpdateCimuRequest request,
            @RequestHeader("X-Document-Id") String docId
    ) {
        log.info("CIMU update | docId={} actionRequested={} userId={} userPoid={} transactionPoid={} containerNo={} applyAll={}",
                docId, UserContext.getActionRequested(), UserContext.getUserId(), UserContext.getUserPoid(),
                request.getTransactionPoid(), request.getContainerNo(), request.getApplyToAllContainers());
        UpdateCimuResponse response = cimuService.updateContainerData(request);
        return success("Update processed", response);
    }

    @AllowedAction(UserRolesRightsEnum.EDIT)
    @PostMapping("/soc-update")
    public ResponseEntity<?> socUpdate(
            @Valid @RequestBody SocUpdateRequest request,
            @RequestHeader("X-Document-Id") String docId
    ) {
        log.info("CIMU soc-update | docId={} actionRequested={} userId={} userPoid={} blNumber={}",
                docId, UserContext.getActionRequested(), UserContext.getUserId(), UserContext.getUserPoid(),
                request.getBlNumber());
        SocUpdateResponse response = cimuService.socUpdate(request);
        return success("SOC update processed", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/demurrage/calculate")
    public ResponseEntity<?> calculateDemurrage(
            @Valid @RequestBody DemurrageCalculateRequest request,
            @RequestHeader("X-Document-Id") String docId
    ) {
        log.info("CIMU demurrage/calculate | docId={} actionRequested={} userId={} userPoid={} transactionPoid={} containerNo={} demDt={}",
                docId, UserContext.getActionRequested(), UserContext.getUserId(), UserContext.getUserPoid(),
                request.getTransactionPoid(), request.getContainerNo(), request.getDemDt());
        DemurrageCalculateResponse response = cimuService.calculateDemurrage(request);
        return success("Demurrage calculated successfully", response);
    }

    // -------------------- SRS enhancement: Container Inspection XL Upload (merged here) --------------------

    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping(value = "/container-inspection/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Document-Id") String docId
    ) {
        log.info("Container Inspection import | docId={} fileName={} size={}",
                docId, file.getOriginalFilename(), file.getSize());

        InspectionUploadResponse response = cimuService.importFile(file);
        return success("File imported successfully", response);
    }


    @AllowedAction(UserRolesRightsEnum.CREATE)
    @PostMapping("/container-inspection/load")
    public ResponseEntity<?> loadContainerDetails(
            @RequestHeader("X-Document-Id") String docId
    ) {
        log.info("Container Inspection load | docId={}", docId);

        InspectionLoadResponse response = cimuService.loadContainerDetails();
        return success("Container details loaded", response);
    }

    @AllowedAction(UserRolesRightsEnum.VIEW)
    @PostMapping("/open-export-manifest")
    public ResponseEntity<?> openExportManifestData(
            @Valid @RequestBody OpenExportManifestRequest request,
            @RequestHeader("X-Document-Id") String docId
    ) {
        log.info("CIMU open-export-manifest | docId={} actionRequested={} userId={} userPoid={} exportBlNumber={} companyPoid={}",
                docId, UserContext.getActionRequested(), UserContext.getUserId(), UserContext.getUserPoid(),
                request.getExportBlNumber(), request.getCompanyPoid());
        OpenExportManifestResponse response = cimuService.openExportManifestData(request);
        return success("Export manifest data fetched successfully", response);
    }

}