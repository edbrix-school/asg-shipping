package com.asg.shipping.demurragedetentionpayabletransfer.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.*;

import java.util.Map;

/**
 * Service interface for Demurrage/Detention Payable Transfer operations
 */
public interface DemurrageDetentionPayableTransferService {

    /**
     * Search/list Demurrage/Detention Payable Transfer records
     */
    Map<String, Object> searchDemurrageDetentionPayableTransfer(String docId, FilterRequestDto request, org.springframework.data.domain.Pageable pageable);

    /**
     * Get a single Demurrage/Detention Payable Transfer record by ID
     */
    DemurrageDetentionPayableTransferDto getDemurrageDetentionPayableTransfer(Long id);

    /**
     * Create a new Demurrage/Detention Payable Transfer record
     */
    DemurrageDetentionPayableTransferDto createDemurrageDetentionPayableTransfer(
            DemurrageDetentionPayableTransferCreateDTO dto, Long companyPoid, Long groupPoid);

    /**
     * Update an existing Demurrage/Detention Payable Transfer record
     */
    DemurrageDetentionPayableTransferDto updateDemurrageDetentionPayableTransfer(
            Long id, DemurrageDetentionPayableTransferUpdateDTO dto, Long companyPoid, Long groupPoid);

    /**
     * Delete (soft delete) a Demurrage/Detention Payable Transfer record
     */
    void deleteDemurrageDetentionPayableTransfer(Long id, Long companyPoid, Long groupPoid);

    /**
     * Process and load available containers into detail table
     */
    DemurrageDetentionPayableTransferDto processData(Long id, ProcessDataRequestDTO request);

    /**
     * Update free days for selected containers and call stored procedure
     */
    void updateFreeDays(Long id, UpdateFreeDaysRequestDTO request);
}
