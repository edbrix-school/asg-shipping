package com.asg.shipping.demurragedetentionpayabletransfer.service;

import com.asg.common.lib.dto.DeleteReasonDto;
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
    void deleteDemurrageDetentionPayableTransfer(Long id, Long companyPoid, Long groupPoid, DeleteReasonDto deleteReasonDto);

    /**
     * Process and load available containers based on Line and BL Type (BEFORE create)
     */
    Map<String, Object> processDataBeforeCreate(ProcessDataRequestDTO request);

    /**
     * Load bill-wise settlement data for selected containers (BEFORE create)
     */
    Map<String, Object> loadBillwiseDataBeforeCreate(LoadBillwiseRequestDTO request);

    /**
     * Update principal extra days for containers (calls stored procedure)
     */
    void updatePrincipalDays(UpdateFreeDaysRequestDTO request);

    /**
     * Process and load available containers into detail table (AFTER create - deprecated)
     */
    @Deprecated
    DemurrageDetentionPayableTransferDto processData(Long id, ProcessDataRequestDTO request);

    /**
     * Load bill-wise settlement data for selected containers (AFTER create - deprecated)
     */
    @Deprecated
    DemurrageDetentionPayableTransferDto loadBillwiseData(Long id, LoadBillwiseRequestDTO request);
/**
     * Update free days for selected containers (AFTER create - deprecated)
     */
    @Deprecated
    void updateFreeDays(Long id, UpdateFreeDaysRequestDTO request);

    /**
     * Get auto-populated GL accounts when Line and BL Type are provided
     */
    Map<String, Object> getAutoPopulatedGlAccounts(Long linePoid, String blType, Long groupPoid);
}
