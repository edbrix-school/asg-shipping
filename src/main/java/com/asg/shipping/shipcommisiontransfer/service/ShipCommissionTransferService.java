package com.asg.shipping.shipcommisiontransfer.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.shipcommisiontransfer.dto.*;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Ship Commission Transfer operations
 */
public interface ShipCommissionTransferService {

    /**
     * Search Ship Commission Transfer records
     */
    Map<String, Object> searchShipCommissionTransfer(String docId, FilterRequestDto request, LocalDate startDate, LocalDate endDate, org.springframework.data.domain.Pageable pageable);

    /**
     * Get Ship Commission Transfer by ID
     */
    ShipCommissionTransferDto getShipCommissionTransfer(Long id);

    /**
     * Create new Ship Commission Transfer
     */
    ShipCommissionTransferDto createShipCommissionTransfer(ShipCommissionTransferCreateDTO dto);

    /**
     * Update existing Ship Commission Transfer
     */
    ShipCommissionTransferDto updateShipCommissionTransfer(Long id, ShipCommissionTransferUpdateDTO dto);

    /**
     * Delete Ship Commission Transfer (soft delete)
     */
    void deleteShipCommissionTransfer(Long id, DeleteReasonDto deleteReasonDto);

    /**
     * Calculate commission amounts
     */
    Map<String, Object> calculateCommission(Long transactionPoid, CalculateCommissionRequestDTO request);

    /**
     * Load commission data from voyage/manifest.
     *
     * @param voyageTransactionPoid the POID of the voyage (passed as P_TRANSACTION_POID_VOYAGE
     *                              to PROC_MATE_RCPT_EMPTY_MANIFEST)
     */
    Map<String, Object> loadFromVoyage(Long voyageTransactionPoid);

    /**
     * Insert commission data into PDA system
     */
    Map<String, Object> insertPdaCommission(Long transactionPoid);

    Map<String, String> getCurrencyExchangeForVoyage(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            Long voyageId
    );

    List<PdaFdaDtlResponseDTO> getPdaFdaDetails(Long transactionPoid);

    List<Object[]> getCommissionByVoyage(Long voyageTransactionPoid, Long transactionPoid);

    List<CommissionPendingResponseDTO> getCommissionPending(Long voyageTransactionPoid, CommissionPendingRequestDTO request);
}
