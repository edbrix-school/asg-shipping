package com.asg.shipping.demurragedetentionpayabletransfer.controller;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;

import com.asg.shipping.common.ApiResponse;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.*;
import com.asg.shipping.demurragedetentionpayabletransfer.service.DemurrageDetentionPayableTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;

/**
 * REST Controller for Demurrage/Detention Payable Transfer operations
 */
@RestController
@RequestMapping("/v1/demurrage-detention-payable-transfer")
@RequiredArgsConstructor
@Slf4j
public class DemurrageDetentionPayableTransferController {

    private final DemurrageDetentionPayableTransferService service;
    private final LoggingService loggingService;

    /**
     * Search/list Demurrage/Detention Payable Transfer records
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchDemurrageDetentionPayableTransfer(
            @RequestBody FilterRequestDto request,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Search request for demurrage detention payable transfer");
        String docId = "100-151";
        Map<String, Object> result = service.searchDemurrageDetentionPayableTransfer(docId, request, pageable);
        return ApiResponse.success("Demurrage/Detention Payable Transfer records retrieved successfully", result);
    }

    /**
     * Get a single Demurrage/Detention Payable Transfer record by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDemurrageDetentionPayableTransfer(
            @PathVariable Long id) {
        log.info("Get request for id: {}", id);
        DemurrageDetentionPayableTransferDto result = service.getDemurrageDetentionPayableTransfer(id);
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());
        return ApiResponse.success("Demurrage/Detention Payable Transfer retrieved successfully", result);
    }

    /**
     * Create a new Demurrage/Detention Payable Transfer record
     */
    @PostMapping
    public ResponseEntity<?> createDemurrageDetentionPayableTransfer(
            @Valid @RequestBody DemurrageDetentionPayableTransferCreateDTO dto) {
        log.info("Create request");
        Long companyPoid = getCompanyPoid();
        Long groupPoid = getGroupPoid();
        DemurrageDetentionPayableTransferDto result = service.createDemurrageDetentionPayableTransfer(dto, companyPoid, groupPoid);
        return ApiResponse.success("Demurrage/Detention Payable Transfer created successfully", result);
    }

    /**
     * Update an existing Demurrage/Detention Payable Transfer record
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDemurrageDetentionPayableTransfer(
            @PathVariable Long id,
            @Valid @RequestBody DemurrageDetentionPayableTransferUpdateDTO dto) {
        log.info("Update request for id: {}", id);
        Long companyPoid = getCompanyPoid();
        Long groupPoid = getGroupPoid();
        DemurrageDetentionPayableTransferDto result = service.updateDemurrageDetentionPayableTransfer(id, dto, companyPoid, groupPoid);
        return ApiResponse.success("Demurrage/Detention Payable Transfer updated successfully", result);
    }

    /**
     * Delete (soft delete) a Demurrage/Detention Payable Transfer record
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDemurrageDetentionPayableTransfer(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) DeleteReasonDto deleteReasonDto) {
        log.info("Delete request for id: {}", id);
        Long companyPoid = getCompanyPoid();
        Long groupPoid = getGroupPoid();
        service.deleteDemurrageDetentionPayableTransfer(id, companyPoid, groupPoid, deleteReasonDto);
        return ApiResponse.success("Demurrage/Detention Payable Transfer deleted successfully");
    }

    /**
     * Process and load available containers based on Line and BL Type (BEFORE create)
     * This is a query operation that returns available containers without saving to database
     */
    @PostMapping("/process-data")
    public ResponseEntity<?> processData(
            @Valid @RequestBody ProcessDataRequestDTO request) {
        log.info("Process data request for line: {}, blType: {}", request.getLinePoid(), request.getBlType());
        Map<String, Object> result = service.processDataBeforeCreate(request);
        return ApiResponse.success("Available containers loaded successfully", result);
    }

    /**
     * Load bill-wise settlement data for selected containers (BEFORE create)
     * This is a query operation that returns bill-wise data for checked containers
     */
    @PostMapping("/load-billwise")
    public ResponseEntity<?> loadBillwiseData(
            @Valid @RequestBody LoadBillwiseRequestDTO request) {
        log.info("Load billwise data request for {} containers", request.getSelectedContainers().size());
        Map<String, Object> result = service.loadBillwiseDataBeforeCreate(request);
        return ApiResponse.success("Bill-wise settlement data loaded successfully", result);
    }

    /**
     * Update principal extra days for containers (calls stored procedure)
     * This updates the PP Extra Days field in the BL Manifest
     */
    @PostMapping("/update-principal-days")
    public ResponseEntity<?> updatePrincipalDays(
            @Valid @RequestBody UpdateFreeDaysRequestDTO request) {
        log.info("Update principal days request for {} containers", request.getContainerUpdates().size());
        service.updatePrincipalDays(request);
        return ApiResponse.success("Principal extra days updated successfully");
    }

    /**
     * Get GL accounts directly via stored procedure
     */
    @GetMapping("/get-gl-accounts-direct")
    public ResponseEntity<?> getGlAccountsDirect(
            @RequestParam Long linePoid,
            @RequestParam String blType) {
        log.info("Get GL accounts direct for line: {}, blType: {}", linePoid, blType);
        Map<String, Object> result = service.getGlAccountsDirect(linePoid, blType);
        return ApiResponse.success("GL accounts retrieved successfully", result);
    }

}