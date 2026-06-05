package com.asg.shipping.containertypes.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.containertypes.dto.ContainerTypeCreateDTO;
import com.asg.shipping.containertypes.dto.ContainerTypeDto;
import com.asg.shipping.containertypes.dto.ContainerTypeUpdateDTO;
import com.asg.shipping.containertypes.entity.ShipContainerTypeMaster;
import com.asg.shipping.containertypes.repository.ShipContainerTypeMasterRepository;
import com.asg.shipping.containertypes.util.ContainerTypeMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Service implementation for Container Type operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerTypeServiceImpl implements ContainerTypeService {

    @Autowired
    private DocumentSearchService documentService;

    @Autowired
    private ContainerTypeMapper mapper;

    @Autowired
    private ShipContainerTypeMasterRepository containerTypeRepository;

    private final LoggingService loggingService;

    @Override
    @Transactional
    public Map<String, Object> searchContainerTypes(String docId, FilterRequestDto request, Pageable pageable) {
        log.info("Searching container types with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        // Resolve filter components from FilterRequestDto
        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        // Call documentService.search with docId, filters, operator, pageable, isDeleted
        // Label field: "CONTAINER_TYPE_NAME" (display field)
        // Value field: "CONTAINER_TYPE_POID" (primary key)
        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "CONTAINER_TYPE_NAME",   // label field for display
                "CONTAINER_TYPE_POID"    // value field (primary key)
        );

        // Convert RawSearchResult to Page
        Page<Map<String, Object>> page = new PageImpl<>(
                raw.records(),
                pageable,
                raw.totalRecords()
        );

        // Wrap with pagination and display fields
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional
    public ContainerTypeDto getContainerType(Long id) {
        log.info("Getting container type with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipContainerTypeMaster containerType = containerTypeRepository.findByContainerTypePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Container Type", "containerTypePoid", id.toString()));

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());

        log.info("Successfully retrieved container type with id: {}", id);
        return mapper.mapToDto(containerType);
    }

    @Override
    @Transactional
    public ContainerTypeDto createContainerType(ContainerTypeCreateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Creating container type with code: {}, groupId: {}, userPoid: {}", dto.getContainerTypeCode(), groupPoid, userPoid);

        // Validate required fields and uniqueness
        validateContainerTypeCreateDTO(dto, groupPoid);

        // Create entity
        ShipContainerTypeMaster containerType = new ShipContainerTypeMaster();
        mapper.mapCreateDTOToEntity(dto, containerType, groupPoid, userPoid);

        // Save entity
        ShipContainerTypeMaster saved = containerTypeRepository.save(containerType);

        String key = saved.getContainerTypePoid().toString();
        String docId = UserContext.getDocumentId();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);

        log.info("Successfully created container type with id: {}", saved.getContainerTypePoid());
        return mapper.mapToDto(saved);
    }

    @Override
    @Transactional
    public ContainerTypeDto updateContainerType(Long id, ContainerTypeUpdateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Updating container type with id: {}, groupId: {}, userPoid: {}", id, groupPoid, userPoid);

        // Find existing container type
        ShipContainerTypeMaster containerType = containerTypeRepository.findByContainerTypePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Container Type", "containerTypePoid", id.toString()));

        // Validate update DTO
        validateContainerTypeUpdateDTO(dto, groupPoid, id);

        ShipContainerTypeMaster oldEntity = new ShipContainerTypeMaster();
        BeanUtils.copyProperties(containerType, oldEntity);

        // Update entity
        mapper.mapUpdateDTOToEntity(dto, containerType, groupPoid, userPoid);

        // Save entity
        ShipContainerTypeMaster saved = containerTypeRepository.save(containerType);

        String key = saved.getContainerTypePoid().toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldEntity, saved, ShipContainerTypeMaster.class, docId, key, LogDetailsEnum.MODIFIED, "CONTAINER_TYPE_POID");

        log.info("Successfully updated container type with id: {}", id);
        return mapper.mapToDto(saved);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        log.info("Toggling active status for container type with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipContainerTypeMaster containerType = containerTypeRepository.findByContainerTypePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Container Type", "containerTypePoid", id.toString()));

        // Toggle active status
        String currentActive = containerType.getActive();
        if ("Y".equals(currentActive)) {
            containerType.setActive("N");
        } else {
            containerType.setActive("Y");
        }

        containerType.setLastModifiedBy(getCurrentUser());
        containerType.setLastModifiedDate(LocalDateTime.now());

        containerTypeRepository.save(containerType);

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), id.toString());
        String logDetail = String.format("KeyId = CONTAINER_TYPE_POID:%s", id);
        String tableName = ShipContainerTypeMaster.class.getAnnotation(jakarta.persistence.Table.class).name();
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), id.toString(), "Active",
                currentActive, containerType.getActive(), logDetail, tableName);

        log.info("Successfully toggled active status for container type with id: {} to {}", id, containerType.getActive());
    }

    @Override
    @Transactional
    public void deleteContainerType(Long id) {
        log.info("Deleting container type with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipContainerTypeMaster containerType = containerTypeRepository.findByContainerTypePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Container Type", "containerTypePoid", id.toString()));

        // Check if already deleted (idempotent)
        if ("Y".equals(containerType.getDeleted())) {
            log.info("Container type with id: {} is already deleted", id);
            return;
        }

        // Soft delete
        containerType.setDeleted("Y");
        containerType.setActive("N");
        containerType.setLastModifiedBy(getCurrentUser());
        containerType.setLastModifiedDate(LocalDateTime.now());

        containerTypeRepository.save(containerType);

        String docId = UserContext.getDocumentId();
        String key = id.toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, docId, key);

        loggingService.logSimpleFieldChange(ShipContainerTypeMaster.class, docId, key, "deleted", "N", "Y", "ContainerType soft deleted");
        loggingService.logSimpleFieldChange(ShipContainerTypeMaster.class, docId, key, "active", "Y", "N", "ContainerType soft deleted");


        log.info("Successfully deleted container type with id: {}", id);
    }

    /**
     * Validate ContainerTypeCreateDTO
     */
    private void validateContainerTypeCreateDTO(ContainerTypeCreateDTO dto, Long groupPoid) {
        // Check if container type code already exists irrespective of group as per db DDL
        if (containerTypeRepository.existsByContainerTypeCode(dto.getContainerTypeCode())) {
            throw new ValidationException("Container type code already exists for this group");
        }

        // Check if container type name already exists for this group
        if (containerTypeRepository.existsByContainerTypeName(dto.getContainerTypeName())) {
            throw new ValidationException("Container type name already exists for this group");
        }
    }

    /**
     * Validate ContainerTypeUpdateDTO
     */
    private void validateContainerTypeUpdateDTO(ContainerTypeUpdateDTO dto, Long groupPoid, Long excludeContainerTypePoid) {
        // Check if container type code already exists (excluding current container type)
        if (containerTypeRepository.existsByContainerTypeCodeExcludingPoid(dto.getContainerTypeCode(), excludeContainerTypePoid)) {
            throw new ValidationException("Container type code already exists");
        }
        
        // Check if container type name already exists for this group (excluding current container type)
        if (containerTypeRepository.existsByContainerTypeNameExcludingPoid(dto.getContainerTypeName(), excludeContainerTypePoid)) {
            throw new ValidationException("Container type name already exists for this group");
        }
    }
}


