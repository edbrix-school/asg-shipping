package com.asg.shipping.lineprincipalmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.shipping.lineprincipalmaster.dto.*;

import java.util.Map;

/**
 * Service interface for Line Principal Master operations
 */
public interface LinePrincipalMasterService {

    /**
     * Search/list lines with filters and pagination
     * Uses DocumentSearchService for unified search functionality
     *
     * @param request FilterRequestDto containing filters, operator, isDeleted
     * @param pageable Pagination and sorting information
     * @return Map containing paginated results and display fields
     */
    Map<String, Object> searchLines(com.asg.common.lib.dto.FilterRequestDto request, org.springframework.data.domain.Pageable pageable);

    /**
     * Get line by ID including charges
     *
     * @param id Line POID
     * @return LinePrincipalMasterDto
     */
    LinePrincipalMasterDto getLine(Long id);

    /**
     * Create a new line with optional charges
     *
     * @param dto Line creation data
     * @return Created LinePrincipalMasterDto
     */
    LinePrincipalMasterDto createLine(LinePrincipalMasterCreateDTO dto);

    /**
     * Update an existing line and its charges
     *
     * @param id Line POID
     * @param dto Line update data
     * @return Updated LinePrincipalMasterDto
     */
    LinePrincipalMasterDto updateLine(Long id, LinePrincipalMasterUpdateDTO dto);

    /**
     * Toggle active status of a line
     *
     * @param id Line POID
     */
    void toggleActive(Long id);

    /**
     * Soft delete a line
     *
     * @param id Line POID
     */
    void deleteLine(Long id, DeleteReasonDto deleteReasonDto);

    /**
     * Copy charges from another line using COPY_LINE_Type_Charges procedure
     *
     * @param id Target Line POID (line to copy charges INTO)
     * @param request Copy charges request with source line POID (line to copy FROM)
     * @return Updated LinePrincipalMasterDto with refreshed charges
     */
    CopyChargesRequestDto copyCharges(Long id, CopyChargesRequestDto request);

    /**
     * Create GL master and sub accounts for a line
     *
     * @param id Line POID
     */
    void createGlMaster(Long id);
}

