package com.asg.shipping.linemasterthirdparty.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.linemasterthirdparty.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Service interface for Line Master Third Party operations
 */
public interface LineMasterThirdPartyService {

    /**
     * Search/list third party lines with filters and pagination
     * All results are filtered by LINE_TYPE = 'THIRD_PARTY'
     *
     * @param request Filter request containing filters, operator, isDeleted
     * @param pageable Pageable object containing page number, size, and sort
     * @return Map containing paginated results
     */
    Map<String, Object> searchThirdPartyLines(FilterRequestDto request, Pageable pageable);

    /**
     * Get third party line by ID
     * Validates that LINE_TYPE = 'THIRD_PARTY'
     *
     * @param id Line POID
     * @return LineMasterThirdPartyDto
     */
    LineMasterThirdPartyDto getThirdPartyLine(Long id);

    /**
     * Create a new third party line
     * Automatically sets LINE_TYPE = 'THIRD_PARTY'
     *
     * @param dto Line creation data
     * @return Created LineMasterThirdPartyDto
     */
    LineMasterThirdPartyDto createThirdPartyLine(LineMasterThirdPartyCreateDTO dto);

    /**
     * Update an existing third party line
     * Validates that LINE_TYPE = 'THIRD_PARTY' and ensures it remains 'THIRD_PARTY'
     *
     * @param id Line POID
     * @param dto Line update data
     * @return Updated LineMasterThirdPartyDto
     */
    LineMasterThirdPartyDto updateThirdPartyLine(Long id, LineMasterThirdPartyUpdateDTO dto);

    /**
     * Toggle active status of a third party line
     * Validates that LINE_TYPE = 'THIRD_PARTY'
     *
     * @param id Line POID
     */
    void toggleActive(Long id);

    /**
     * Soft delete a third party line
     * Validates that LINE_TYPE = 'THIRD_PARTY'
     *
     * @param id Line POID
     */
    void deleteThirdPartyLine(Long id);
}

