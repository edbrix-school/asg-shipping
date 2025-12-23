package com.asg.shipping.vesseltypemaster.service;



import com.asg.shipping.vesseltypemaster.dto.VesselTypeCreateDTO;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeDto;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeUpdateDTO;

import java.util.Map;

/**
 * Service interface for Vessel Type operations
 */
public interface VesselTypeService {

    /**
     * Search/list vessel types with filters and pagination
     * Uses DocumentSearchService for unified search functionality
     *
     * @param docId Document ID (e.g., "100-003" for Vessel Type Master)
     * @param request FilterRequestDto containing filters, operator, isDeleted
     * @param pageable Pagination and sorting information
     * @return Map containing paginated results and display fields
     */
    Map<String, Object> searchVesselTypes(String docId, com.asg.common.lib.dto.FilterRequestDto request, org.springframework.data.domain.Pageable pageable);

    /**
     * Get vessel type by ID
     *
     * @param id Vessel Type POID
     * @return VesselTypeDto
     */
    VesselTypeDto getVesselType(Long id);

    /**
     * Create a new vessel type
     *
     * @param dto Vessel type creation data
     * @param groupPoid Group POID from UserContext
     * @param userPoid User POID from UserContext
     * @return Created VesselTypeDto
     */
    VesselTypeDto createVesselType(VesselTypeCreateDTO dto, Long groupPoid, Long userPoid);

    /**
     * Update an existing vessel type
     *
     * @param id Vessel Type POID
     * @param dto Vessel type update data
     * @param groupPoid Group POID from UserContext
     * @param userPoid User POID from UserContext
     * @return Updated VesselTypeDto
     */
    VesselTypeDto updateVesselType(Long id, VesselTypeUpdateDTO dto, Long groupPoid, Long userPoid);

    /**
     * Toggle active status of a vessel type
     *
     * @param id Vessel Type POID
     */
    void toggleActive(Long id);

    /**
     * Soft delete a vessel type
     *
     * @param id Vessel Type POID
     */
    void deleteVesselType(Long id);
}
