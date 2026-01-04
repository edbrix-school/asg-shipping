package com.asg.shipping.demurragedetentionpayabletransfer.controller;

import com.asg.common.lib.dto.FilterRequestDto;
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

    /**
     * Search/list Demurrage/Detention Payable Transfer records
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchDemurrageDetentionPayableTransfer(
            @RequestBody FilterRequestDto request,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Search request for demurrage detention payable transfer");
        // Document ID for Demurrage/Detention Payable Transfer is hardcoded
        String docId = "100-151";
        Map<String, Object> result = service.searchDemurrageDetentionPayableTransfer(docId, request, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Get a single Demurrage/Detention Payable Transfer record by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DemurrageDetentionPayableTransferDto> getDemurrageDetentionPayableTransfer(
            @PathVariable Long id) {
        log.info("Get request for id: {}", id);
        DemurrageDetentionPayableTransferDto result = service.getDemurrageDetentionPayableTransfer(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Create a new Demurrage/Detention Payable Transfer record
     */
    @PostMapping
    public ResponseEntity<DemurrageDetentionPayableTransferDto> createDemurrageDetentionPayableTransfer(
            @Valid @RequestBody DemurrageDetentionPayableTransferCreateDTO dto) {
        log.info("Create request");
        Long companyPoid = getCompanyPoid();
        Long groupPoid = getGroupPoid();
        DemurrageDetentionPayableTransferDto result = service.createDemurrageDetentionPayableTransfer(dto, companyPoid, groupPoid);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Update an existing Demurrage/Detention Payable Transfer record
     */
    @PutMapping("/{id}")
    public ResponseEntity<DemurrageDetentionPayableTransferDto> updateDemurrageDetentionPayableTransfer(
            @PathVariable Long id,
            @Valid @RequestBody DemurrageDetentionPayableTransferUpdateDTO dto) {
        log.info("Update request for id: {}", id);
        Long companyPoid = getCompanyPoid();
        Long groupPoid = getGroupPoid();
        DemurrageDetentionPayableTransferDto result = service.updateDemurrageDetentionPayableTransfer(id, dto, companyPoid, groupPoid);
        return ResponseEntity.ok(result);
    }

    /**
     * Delete (soft delete) a Demurrage/Detention Payable Transfer record
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDemurrageDetentionPayableTransfer(@PathVariable Long id) {
        log.info("Delete request for id: {}", id);
        Long companyPoid = getCompanyPoid();
        Long groupPoid = getGroupPoid();
        service.deleteDemurrageDetentionPayableTransfer(id, companyPoid, groupPoid);
        return ResponseEntity.ok(Map.of("message", "Demurrage/Detention Payable Transfer deleted successfully"));
    }

    /**
     * Process and load available containers into detail table
     */
    @PostMapping("/{id}/process-data")
    public ResponseEntity<DemurrageDetentionPayableTransferDto> processData(
            @PathVariable Long id,
            @RequestBody(required = false) ProcessDataRequestDTO request) {
        log.info("Process data request for id: {}", id);
        if (request == null) {
            request = new ProcessDataRequestDTO();
        }
        DemurrageDetentionPayableTransferDto result = service.processData(id, request);
        return ResponseEntity.ok(result);
    }

    /**
     * Update free days for selected containers and call stored procedure
     */
    @PostMapping("/{id}/update-free-days")
    public ResponseEntity<Map<String, String>> updateFreeDays(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFreeDaysRequestDTO request) {
        log.info("Update free days request for id: {}", id);
        service.updateFreeDays(id, request);
        return ResponseEntity.ok(Map.of("message", "Free days updated successfully"));
    }

}