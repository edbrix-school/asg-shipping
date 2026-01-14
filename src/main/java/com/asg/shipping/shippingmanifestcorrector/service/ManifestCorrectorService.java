package com.asg.shipping.shippingmanifestcorrector.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.shippingmanifestcorrector.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Shipping Manifest Corrector operations
 */
public interface ManifestCorrectorService {

    /**
     * Create a new Shipping Manifest Corrector record
     */
    ManifestCorrectorDto createManifestCorrector(ManifestCorrectorCreateDTO createDTO);

    /**
     * Update an existing Shipping Manifest Corrector record
     */
    ManifestCorrectorDto updateManifestCorrector(Long transactionPoid, ManifestCorrectorUpdateDTO updateDTO);

    /**
     * Get Shipping Manifest Corrector record by transaction POID
     */
    ManifestCorrectorDto getManifestCorrectorById(Long transactionPoid);

    /**
     * Search Shipping Manifest Corrector records
     */
    Map<String, Object> searchManifestCorrector(String docId, FilterRequestDto request, Pageable pageable);

    /**
     * Delete (soft delete) a Shipping Manifest Corrector record
     */
    void deleteManifestCorrector(Long transactionPoid);

    /**
     * Load demurrage refund charges for a BL
     */
//    Demurrage Refund functionality has been dropped in the latest development
//    List<ManifestCorrectorChargeDtlDto> loadDemurrageRefundCharges(Long transactionPoid, Long blPoid);

    /**
     * Validate refund amounts for a container
     */
    ValidateRefundAmountResponse validateRefundAmounts(ValidateRefundAmountRequest request);
}

