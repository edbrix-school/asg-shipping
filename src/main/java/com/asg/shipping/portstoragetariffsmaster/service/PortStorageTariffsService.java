package com.asg.shipping.portstoragetariffsmaster.service;


import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.shipping.portstoragetariffsmaster.dto.PortStorageTariffCreateDTO;
import com.asg.shipping.portstoragetariffsmaster.dto.PortStorageTariffDto;
import com.asg.shipping.portstoragetariffsmaster.dto.PortStorageTariffUpdateDTO;

import java.util.Map;

/**
 * Service interface for Port Storage Tariffs operations
 */
public interface PortStorageTariffsService {

    /**
     * Search/list tariffs with filters and pagination
     * Uses DocumentSearchService for unified search functionality
     *
     * @param docId Document ID (e.g., "100-060" for Port Storage Tariffs)
     * @param request FilterRequestDto containing filters, operator, isDeleted
     * @param pageable Pagination and sorting information
     * @return Map containing paginated results and display fields
     */
    Map<String, Object> searchTariffs(String docId, com.asg.common.lib.dto.FilterRequestDto request, org.springframework.data.domain.Pageable pageable);

    /**
     * Get tariff by ID
     *
     * @param id Transaction POID
     * @return PortStorageTariffDto with nested details
     */
    PortStorageTariffDto getTariff(Long id);

    /**
     * Create a new tariff
     *
     * @param dto Tariff creation data (header + optional details)
     * @param groupPoid Group POID from UserContext
     * @param userPoid User POID from UserContext
     * @param companyPoid Company POID from UserContext
     * @return Created PortStorageTariffDto
     */
    PortStorageTariffDto createTariff(PortStorageTariffCreateDTO dto, Long groupPoid, Long userPoid, Long companyPoid);

    /**
     * Update an existing tariff
     *
     * @param id Transaction POID
     * @param dto Tariff update data (header + details)
     * @param groupPoid Group POID from UserContext
     * @param userPoid User POID from UserContext
     * @param companyPoid Company POID from UserContext
     * @return Updated PortStorageTariffDto
     */
    PortStorageTariffDto updateTariff(Long id, PortStorageTariffUpdateDTO dto, Long groupPoid, Long userPoid, Long companyPoid);

    /**
     * Soft delete a tariff
     *
     * @param groupPoid Group POID from UserContext
     * @param tariffId Transaction POID
     * @param companyPoid Company POID from UserContext
     * @param deleteReasonDto Delete reason information
     */
    void deleteTariff(Long groupPoid, Long tariffId, Long companyPoid, DeleteReasonDto deleteReasonDto);
}
