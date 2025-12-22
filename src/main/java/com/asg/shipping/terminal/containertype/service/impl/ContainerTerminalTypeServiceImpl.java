package com.asg.shipping.terminal.containertype.service.impl;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.terminal.containertype.dto.ContainerTerminalTypeRequest;
import com.asg.shipping.terminal.containertype.dto.ContainerTerminalTypeResponse;
import com.asg.shipping.terminal.containertype.entity.ContainerTerminalTypeEntity;
import com.asg.shipping.terminal.containertype.reposiory.ContainerTerminalTypeRepository;
import com.asg.shipping.terminal.containertype.service.ContainerTerminalTypeService;
import com.asg.shipping.terminal.containertype.util.ContainerTerminalTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerTerminalTypeServiceImpl implements ContainerTerminalTypeService {

    private final ContainerTerminalTypeRepository repository;
    private final ContainerTerminalTypeMapper mapper;
    private final DocumentSearchService documentService;
    private final LoggingService loggingService;

    @Override
    public Map<String, Object> listContainerTerminalTypes(
            String docId,
            FilterRequestDto request,
            Pageable pageable) {
        log.info("Listing container terminal types with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "CONTAINER_TMNL_TYPE_NAME",   // label
                "CONTAINER_TMNL_TYPE_POID"    // value
        );

        Page<Map<String, Object>> page =
                new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public ContainerTerminalTypeResponse getById(Long poid, Long groupPoid) {
        log.info("Getting container terminal type with id: {}", poid);

        ContainerTerminalTypeEntity entity =
                repository.findByContainerTerminalTypePoidAndGroupPoid(poid, groupPoid)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "ContainerTerminalType",
                                "containerTerminalTypePoid",
                                poid
                        ));
        log.info("Successfully retrieved container terminal type with id: {}", poid);

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public ContainerTerminalTypeResponse create(
            ContainerTerminalTypeRequest request,
            Long groupPoid,
            String userId, String docId) {
        log.info("Creating container terminal type with code: {}, groupId: {}, userId: {}", request.getContainerTerminalTypeCode(), groupPoid, userId);

        String code = request.getContainerTerminalTypeCode().trim().toUpperCase();
        String name = request.getContainerTerminalTypeName().trim();

        if (repository.existsByContainerTerminalTypeCodeAndGroupPoid(code, groupPoid)) {
            throw new ValidationException(
                    "Container terminal type code already exists: " + code
            );
        }
        if (repository.existsByContainerTerminalTypeNameAndGroupPoid(name, groupPoid)) {
            throw new ValidationException(
                    "Container terminal type name already exists: " + name
            );
        }

        ContainerTerminalTypeEntity entity =
                mapper.toEntity(request, groupPoid, userId);

        ContainerTerminalTypeEntity saved = repository.save(entity);
        String key = saved.getContainerTerminalTypePoid().toString();

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);

        loggingService.logChanges(null, saved, ContainerTerminalTypeEntity.class, docId, key, LogDetailsEnum.CREATED, "CONTAINER_TMNL_TYPE_POID");

        log.info("Successfully created container terminal type with id: {}", saved.getContainerTerminalTypePoid());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ContainerTerminalTypeResponse update(
            Long poid,
            ContainerTerminalTypeRequest request,
            Long groupPoid,
            String userId, String docId) {
        log.info("Updating container terminal type with id: {}, groupId: {}, userId: {}", poid, groupPoid, userId);
        ContainerTerminalTypeEntity entity =
                repository.findByContainerTerminalTypePoidAndGroupPoid(poid, groupPoid)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "ContainerTerminalType",
                                "containerTerminalTypePoid",
                                poid
                        ));

        String newName = request.getContainerTerminalTypeName().trim();

        if (!newName.equals(entity.getContainerTerminalTypeName())
                && repository.existsByContainerTerminalTypeNameAndGroupPoid(newName, groupPoid)) {
            throw new ValidationException(
                    "Container terminal type name already exists: " + newName
            );
        }

        ContainerTerminalTypeEntity oldEntity = new ContainerTerminalTypeEntity();
        BeanUtils.copyProperties(entity, oldEntity);

        mapper.updateEntity(entity, request, userId);
        repository.save(entity);
        String key = entity.getContainerTerminalTypePoid().toString();

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, docId, key);

        loggingService.logChanges(oldEntity, entity, ContainerTerminalTypeEntity.class, docId, key, LogDetailsEnum.MODIFIED, "CONTAINER_TMNL_TYPE_POID");

        log.info("Successfully updated container terminal type with id: {}", poid);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void delete(
            Long poid,
            Long groupPoid,
            String userId) {
        log.info("Deleting container terminal type with id: {}", poid);

        ContainerTerminalTypeEntity entity =
                repository.findByContainerTerminalTypePoidAndGroupPoid(poid, groupPoid)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "ContainerTerminalType",
                                "containerTerminalTypePoid",
                                poid
                        ));


        entity.setActive("N");
        entity.setDeleted("Y");
        entity.setLastModifiedBy(userId);
        entity.setLastModifiedDate(LocalDateTime.now());
        repository.save(entity);
        log.info("Successfully deleted container terminal type with id: {}", poid);

    }

}