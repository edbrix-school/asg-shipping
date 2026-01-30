package com.asg.shipping.collectionhandover.service;

import com.asg.shipping.collectionhandover.dto.CollectionHandoverCreateDTO;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverDto;
import com.asg.shipping.collectionhandover.dto.CollectionHandoverUpdateDTO;

import java.util.Map;

/**
 * Service interface for Collection Handover operations
 */
public interface CollectionHandoverService {

    /**
     * Search/list collection handovers with filters and pagination
     * Uses DocumentSearchService for unified search functionality
     *
     * @param docId Document ID (e.g., "300-106" for Collection Handover)
     * @param request FilterRequestDto containing filters, operator, isDeleted
     * @param pageable Pagination and sorting information
     * @return Map containing paginated results and display fields
     */
    Map<String, Object> searchCollectionHandovers(String docId, com.asg.common.lib.dto.FilterRequestDto request, org.springframework.data.domain.Pageable pageable);

    /**
     * Get collection handover by ID
     *
     * @param id TRANSACTION_POID
     * @return CollectionHandoverDto with all detail records and LOV data
     */
    CollectionHandoverDto getCollectionHandover(Long id);

    /**
     * Create a new collection handover
     *
     * @param dto Collection handover creation data
     * @param groupPoid Group POID from UserContext
     * @param userPoid User POID from UserContext
     * @return Created CollectionHandoverDto
     */
    CollectionHandoverDto createCollectionHandover(CollectionHandoverCreateDTO dto, Long groupPoid, Long userPoid);

    /**
     * Update an existing collection handover
     *
     * @param id TRANSACTION_POID
     * @param dto Collection handover update data
     * @param groupPoid Group POID from UserContext
     * @param userPoid User POID from UserContext
     * @return Updated CollectionHandoverDto
     */
    CollectionHandoverDto updateCollectionHandover(Long id, CollectionHandoverUpdateDTO dto, Long groupPoid, Long userPoid);

    /**
     * Soft delete a collection handover
     *
     * @param id TRANSACTION_POID
     */
    void deleteCollectionHandover(Long id);

    /**
     * Toggle verify/receive status
     *
     * @param id TRANSACTION_POID
     * @param verifiedRcvd Y or N
     * @param mainOfcRemarks Optional remarks
     */
    void toggleVerifyStatus(Long id, String verifiedRcvd, String mainOfcRemarks);
    
    byte[] print(Long transactionPoid) throws Exception;
}

