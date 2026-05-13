package com.asg.shipping.shippingmanifestcorrector.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.shippingmanifestcorrector.dto.ContainerReprintResponse;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorBlAutoPopulateDto;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorBlAutoPopulateRequest;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorChargeDtlDto;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorCreateDTO;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorDto;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorUpdateDTO;
import com.asg.shipping.shippingmanifestcorrector.dto.ValidateRefundAmountRequest;
import com.asg.shipping.shippingmanifestcorrector.dto.ValidateRefundAmountResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
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
    Map<String, Object> searchManifestCorrector(String docId, FilterRequestDto request, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Delete (soft delete) a Shipping Manifest Corrector record
     */
    void deleteManifestCorrector(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    /**
     * Load demurrage refund charges for a BL
     */
//    Demurrage Refund functionality has been dropped in the latest development
//    List<ManifestCorrectorChargeDtlDto> loadDemurrageRefundCharges(Long transactionPoid, Long blPoid);

    /**
     * Validate refund amounts for a container
     */
    ValidateRefundAmountResponse validateRefundAmounts(ValidateRefundAmountRequest request);

    /**
     * Auto-fill DO reprint charges for a BL
     */
    List<ManifestCorrectorChargeDtlDto> autoFillDoReprint(String blNumber);

    /**
     * Auto-fill container reprint charges and containers for a BL
     */
    ContainerReprintResponse autoFillContainerReprint(String blNumber);

    /**
     * Auto-fill BL reprint charges for a BL
     */
    List<ManifestCorrectorChargeDtlDto> autoFillBlReprint(String blNumber);

    /**
     * Auto-fill DEM refund charges for a BL
     */
    List<ManifestCorrectorChargeDtlDto> autoFillDemRefund(String blNumber);

    /**
     * Auto-populate header fields after BL browse.
     */
    ManifestCorrectorBlAutoPopulateDto autoPopulateFromBlBrowse(String blNumber, ManifestCorrectorBlAutoPopulateRequest request);
}
