package com.asg.shipping.vesselmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.shipping.vesselmaster.dto.VesselMasterCreateDTO;
import com.asg.shipping.vesselmaster.dto.VesselMasterDto;
import com.asg.shipping.vesselmaster.dto.VesselMasterUpdateDTO;

import java.util.Map;

/**
 * Service interface for Vessel Master operations
 */
public interface VesselMasterService {

    /**
     * Search/list vessels with filters and pagination
     * Uses DocumentSearchService for unified search functionality
     *
     * @param request FilterRequestDto containing filters, operator, isDeleted
     * @param pageable Pagination and sorting information
     * @return Map containing paginated results and display fields
     */
    Map<String, Object> searchVessels(com.asg.common.lib.dto.FilterRequestDto request, org.springframework.data.domain.Pageable pageable);

    /**
     * Get vessel by ID
     *
     * @param id Vessel POID
     * @return VesselMasterDto
     */
    VesselMasterDto getVessel(Long id);

    /**
     * Create a new vessel
     *
     * @param dto Vessel creation data
     * @return Created VesselMasterDto
     */
    VesselMasterDto createVessel(VesselMasterCreateDTO dto);

    /**
     * Update an existing vessel
     *
     * @param id Vessel POID
     * @param dto Vessel update data
     * @return Updated VesselMasterDto
     */
    VesselMasterDto updateVessel(Long id, VesselMasterUpdateDTO dto);

    /**
     * Toggle active status of a vessel
     *
     * @param id Vessel POID
     */
    void toggleActive(Long id);

    /**
     * Soft delete a vessel
     *
     * @param id Vessel POID
     * @param deleteReasonDto Optional delete reason for auditing
     */
    void deleteVessel(Long id, DeleteReasonDto deleteReasonDto);
}

