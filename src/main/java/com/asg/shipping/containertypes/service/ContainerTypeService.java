package com.asg.shipping.containertypes.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.containertypes.dto.ContainerTypeCreateDTO;
import com.asg.shipping.containertypes.dto.ContainerTypeDto;
import com.asg.shipping.containertypes.dto.ContainerTypeUpdateDTO;

import java.util.Map;

/**
 * Service interface for Container Type operations
 */
public interface ContainerTypeService {

    /**
     * Search/list container types with filters and pagination
     * Uses DocumentSearchService for unified search functionality
     *
     * @param docId Document ID (e.g., "100-002" for Container Type)
     * @param request FilterRequestDto containing filters, operator, isDeleted
     * @param pageable Pagination and sorting information
     * @return Map containing paginated results and display fields
     */
    Map<String, Object> searchContainerTypes(String docId, FilterRequestDto request, org.springframework.data.domain.Pageable pageable);

    /**
     * Get container type by ID
     *
     * @param id Container Type POID
     * @return ContainerTypeDto
     */
    ContainerTypeDto getContainerType(Long id);

    /**
     * Create a new container type
     *
     * @param dto Container type creation data
     * @param groupPoid Group POID from UserContext
     * @param userPoid User POID from UserContext
     * @return Created ContainerTypeDto
     */
    ContainerTypeDto createContainerType(ContainerTypeCreateDTO dto, Long groupPoid, Long userPoid);

    /**
     * Update an existing container type
     *
     * @param id Container Type POID
     * @param dto Container type update data
     * @param groupPoid Group POID from UserContext
     * @param userPoid User POID from UserContext
     * @return Updated ContainerTypeDto
     */
    ContainerTypeDto updateContainerType(Long id, ContainerTypeUpdateDTO dto, Long groupPoid, Long userPoid);

    /**
     * Toggle active status of a container type
     *
     * @param id Container Type POID
     */
    void toggleActive(Long id);

    /**
     * Soft delete a container type
     *
     * @param id Container Type POID
     */
   // void deleteContainerType(Long id);
}


