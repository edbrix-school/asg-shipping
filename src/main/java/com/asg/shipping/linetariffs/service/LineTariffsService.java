package com.asg.shipping.linetariffs.service;

import com.asg.shipping.linetariffs.dto.CopyTariffRequestDTO;
import com.asg.shipping.linetariffs.dto.LineTariffCreateDTO;
import com.asg.shipping.linetariffs.dto.LineTariffDto;
import com.asg.shipping.linetariffs.dto.LineTariffUpdateDTO;

import java.time.LocalDate;
import java.util.Map;

/**
 * Service interface for Line Tariffs operations
 */
public interface LineTariffsService {

    /**
     * Search/list line tariffs with filters and pagination
     * Uses DocumentSearchService for unified search functionality
     *
     * @param docId Document ID (e.g., "100-050" for Line Tariffs)
     * @param request FilterRequestDto containing filters, operator, isDeleted
     * @param pageable Pagination and sorting information
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @return Map containing paginated results and display fields
     */
    Map<String, Object> searchLineTariffs(String docId, com.asg.common.lib.dto.FilterRequestDto request, org.springframework.data.domain.Pageable pageable, LocalDate startDate, LocalDate endDate);

    /**
     * Get line tariff by ID
     *
     * @param id TRANSACTION_POID
     * @return LineTariffDto with all detail records and LOV data
     */
    LineTariffDto getLineTariff(Long id);

    /**
     * Create a new line tariff
     *
     * @param dto Line tariff creation data
     * @param groupPoid Group POID from UserContext
     * @param userPoid User POID from UserContext
     * @return Created LineTariffDto
     */
    LineTariffDto createLineTariff(LineTariffCreateDTO dto, Long groupPoid, Long userPoid);

    /**
     * Update an existing line tariff
     *
     * @param id TRANSACTION_POID
     * @param dto Line tariff update data
     * @param groupPoid Group POID from UserContext
     * @param userPoid User POID from UserContext
     * @return Updated LineTariffDto
     */
    LineTariffDto updateLineTariff(Long id, LineTariffUpdateDTO dto, Long groupPoid, Long userPoid);

    /**
     * Soft delete a line tariff using document delete service
     *
     * @param id TRANSACTION_POID
     * @param deleteReasonDto Delete reason information
     */
    void deleteLineTariff(Long id, com.asg.common.lib.dto.DeleteReasonDto deleteReasonDto);

    /**
     * Copy existing tariff to new period
     *
     * @param id Source TRANSACTION_POID
     * @param request Copy request with new period
     * @param groupPoid Group POID from UserContext
     * @param userPoid User POID from UserContext
     * @return Newly created LineTariffDto
     */
    LineTariffDto copyLineTariff(Long id, CopyTariffRequestDTO request, Long groupPoid, Long userPoid);
}

