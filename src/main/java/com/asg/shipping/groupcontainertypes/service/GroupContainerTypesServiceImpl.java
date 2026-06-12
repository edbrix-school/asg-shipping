package com.asg.shipping.groupcontainertypes.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.groupcontainertypes.dto.ContainerGroupCreateDTO;
import com.asg.shipping.groupcontainertypes.dto.ContainerGroupDto;
import com.asg.shipping.groupcontainertypes.dto.ContainerGroupUpdateDTO;
import com.asg.shipping.groupcontainertypes.entity.ShipContainerTypeGrpMaster;
import com.asg.shipping.groupcontainertypes.repository.ShipContainerTypeGrpMasterRepository;
import com.asg.shipping.groupcontainertypes.util.ContainerGroupMapper;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupContainerTypesServiceImpl implements GroupContainerTypesService{

    @Autowired
    private DocumentSearchService documentService;

    @Autowired
    private ContainerGroupMapper mapper;

    @Autowired
    private ShipContainerTypeGrpMasterRepository containerGroupRepository;

    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;

    @Override
    @Transactional
    public Map<String, Object> searchContainerGroups(String docId, FilterRequestDto request, Pageable pageable) {
        log.info("Searching container groups with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        // Resolve filter components from FilterRequestDto

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        // Call documentService.search with docId, filters, operator, pageable, isDeleted
        // Label field: "CONTAINER_GRP_NAME" (display field)
        // Value field: "CONTAINER_GRP_POID" (primary key)
        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "CONTAINER_GRP_NAME",   // label field for display
                "CONTAINER_GRP_POID"    // value field (primary key)
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
    public ContainerGroupDto getContainerGroup(Long id) {
        log.info("Getting container group with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipContainerTypeGrpMaster containerGroup = containerGroupRepository.findByContainerGrpPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Container Group", "containerGrpPoid", id.toString()));

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());

        log.info("Successfully retrieved container group with id: {}", id);
        return mapper.mapToDto(containerGroup);
    }


    @Override
    @Transactional
    public ContainerGroupDto createContainerGroup(ContainerGroupCreateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Creating container group with code: {}, groupId: {}, userPoid: {}", dto.getContainerGrpCode(), groupPoid, userPoid);

        // Validate required fields and uniqueness
        validateContainerGroupCreateDTO(dto, groupPoid);

        // Create entity
        ShipContainerTypeGrpMaster containerGroup = new ShipContainerTypeGrpMaster();
        mapper.mapCreateDTOToEntity(dto, containerGroup, groupPoid, userPoid);

        // Save entity
        ShipContainerTypeGrpMaster saved = containerGroupRepository.save(containerGroup);

        String docId = UserContext.getDocumentId();
        String key = saved.getContainerGrpPoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);

        log.info("Successfully created container group with id: {}", saved.getContainerGrpPoid());
        return mapper.mapToDto(saved);
    }

    @Override
    @Transactional
    public ContainerGroupDto updateContainerGroup(Long id, ContainerGroupUpdateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Updating container group with id: {}, groupId: {}, userPoid: {}", id, groupPoid, userPoid);

        // Find existing container group
        ShipContainerTypeGrpMaster containerGroup = containerGroupRepository.findByContainerGrpPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Container Group", "containerGrpPoid", id.toString()));

        // Validate update DTO
        validateContainerGroupUpdateDTO(dto, groupPoid, id);

        ShipContainerTypeGrpMaster oldEntity = new ShipContainerTypeGrpMaster();
        BeanUtils.copyProperties(containerGroup, oldEntity);

        // Update entity
        mapper.mapUpdateDTOToEntity(dto, containerGroup, groupPoid, userPoid);

        // Save entity
        ShipContainerTypeGrpMaster saved = containerGroupRepository.save(containerGroup);

        String docId = UserContext.getDocumentId();
        String key = saved.getContainerGrpPoid().toString();
        loggingService.logChanges(oldEntity, saved, ShipContainerTypeGrpMaster.class, docId, key, LogDetailsEnum.MODIFIED, "CONTAINER_GRP_POID");


        log.info("Successfully updated container group with id: {}", id);
        return mapper.mapToDto(saved);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        log.info("Toggling active status for container group with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipContainerTypeGrpMaster containerGroup = containerGroupRepository.findByContainerGrpPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Container Group", "containerGrpPoid", id.toString()));

        // Toggle active status
        String currentActive = containerGroup.getActive();
        if ("Y".equals(currentActive)) {
            containerGroup.setActive("N");
        } else {
            containerGroup.setActive("Y");
        }

        containerGroup.setLastModifiedBy(getCurrentUser());
        containerGroup.setLastModifiedDate(LocalDateTime.now());

        containerGroupRepository.save(containerGroup);

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), id.toString());
        String logDetail = String.format("KeyId = CONTAINER_GRP_POID:%s", id);
        String tableName = ShipContainerTypeGrpMaster.class.getAnnotation(jakarta.persistence.Table.class).name();
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), id.toString(), "Active", currentActive, containerGroup.getActive(), logDetail, tableName);

        log.info("Successfully toggled active status for container group with id: {} to {}", id, containerGroup.getActive());
    }

    @Override
    @Transactional
    public void deleteContainerGroup(Long id, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting container group with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipContainerTypeGrpMaster containerGroup = containerGroupRepository.findByContainerGrpPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Container Group", "containerGrpPoid", id.toString()));

        documentDeleteService.deleteDocument(
                id,
                "SHIP_CONTAINER_TYPE_GRP_MASTER",
                "CONTAINER_GRP_POID",
                deleteReasonDto,
                null
        );

        log.info("Successfully deleted container group with id: {}", id);
    }

    private void validateContainerGroupCreateDTO(ContainerGroupCreateDTO dto, Long groupPoid) {
        // Check if container group code already exists per group
        if (containerGroupRepository.existsByContainerGrpCodeAndGroupPoid(dto.getContainerGrpCode(), groupPoid)) {
            throw new ValidationException("Container group code already exists");
        }

        // Check if container group name already exists per group
        if (containerGroupRepository.existsByContainerGrpNameAndGroupPoid(dto.getContainerGrpName(), groupPoid)) {
            throw new ValidationException("Container group name already exists");
        }
    }

    private void validateContainerGroupUpdateDTO(ContainerGroupUpdateDTO dto, Long groupPoid, Long excludeContainerGrpPoid) {
        // Check if container group code already exists per group (excluding current container group)
        if (containerGroupRepository.existsByContainerGrpCodeAndGroupPoidExcludingPoid(dto.getContainerGrpCode(), groupPoid, excludeContainerGrpPoid)) {
            throw new ValidationException("Container group code already exists");
        }

        // Check if container group name already exists per group (excluding current container group)
        if (containerGroupRepository.existsByContainerGrpNameAndGroupPoidExcludingPoid(dto.getContainerGrpName(), groupPoid, excludeContainerGrpPoid)) {
            throw new ValidationException("Container group name already exists");
        }
    }

}
