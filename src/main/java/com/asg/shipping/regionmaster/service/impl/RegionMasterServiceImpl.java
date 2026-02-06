package com.asg.shipping.regionmaster.service.impl;

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
import com.asg.shipping.regionmaster.dto.RegionMasterRequest;
import com.asg.shipping.regionmaster.dto.RegionMasterResponse;
import com.asg.shipping.regionmaster.entity.ShipRegionMasterEntity;
import com.asg.shipping.regionmaster.repository.ShipRegionMasterRepository;
import com.asg.shipping.regionmaster.service.RegionMasterService;
import com.asg.shipping.regionmaster.util.RegionMasterMapper;
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
public class RegionMasterServiceImpl implements RegionMasterService {

    private final ShipRegionMasterRepository repository;
    private final RegionMasterMapper mapper;
    private final DocumentSearchService documentService;
    private final LoggingService loggingService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listRegionMasters(
            String docId,
            FilterRequestDto request,
            Pageable pageable) {
        log.info("Listing region masters with docId: {}, page: {}, size: {}", 
                docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "REGION_NAME",   // label field for display
                "REGION_POID"    // value field (primary key)
        );

        Page<Map<String, Object>> page =
                new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        
        log.info("Successfully retrieved {} region masters", raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public RegionMasterResponse getById(Long regionPoid, Long groupPoid) {
        log.info("Getting region master with id: {}, groupPoid: {}", regionPoid, groupPoid);

        ShipRegionMasterEntity entity =
                repository.findByRegionPoidAndGroupPoid(regionPoid, groupPoid)
                        .orElseThrow(() -> {
                            log.error("Region master not found with id: {}, groupPoid: {}", regionPoid, groupPoid);
                            return new ResourceNotFoundException(
                                    "RegionMaster",
                                    "regionPoid",
                                    regionPoid
                            );
                        });
        
        log.info("Successfully retrieved region master with id: {}", regionPoid);

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), regionPoid.toString());

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public RegionMasterResponse create(
            RegionMasterRequest request,
            Long groupPoid,
            String userId,
            String docId) {
        log.info("Creating region master with code: {}, name: {}, groupPoid: {}, userId: {}", 
                request.getRegionCode(), request.getRegionName(), groupPoid, userId);

        String code = request.getRegionCode().trim();
        String name = request.getRegionName().trim();

        // Check uniqueness of region code
        if (repository.existsByRegionCodeAndGroupPoidAndDeletedNot(code, groupPoid, "Y")) {
            log.error("Region code already exists: {} for groupPoid: {}", code, groupPoid);
            throw new ValidationException(
                    "Region code already exists: " + code
            );
        }

        // Check uniqueness of region name
        if (repository.existsByRegionNameAndGroupPoidAndDeletedNot(name, groupPoid, "Y")) {
            log.error("Region name already exists: {} for groupPoid: {}", name, groupPoid);
            throw new ValidationException(
                    "Region name already exists: " + name
            );
        }

        ShipRegionMasterEntity entity = mapper.toEntity(request, groupPoid, userId);

        ShipRegionMasterEntity saved = repository.save(entity);
        String key = saved.getRegionPoid().toString();

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);

        log.info("Successfully created region master with id: {}", saved.getRegionPoid());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RegionMasterResponse update(
            Long regionPoid,
            RegionMasterRequest request,
            Long groupPoid,
            String userId,
            String docId) {
        log.info("Updating region master with id: {}, groupPoid: {}, userId: {}", 
                regionPoid, groupPoid, userId);
        
        ShipRegionMasterEntity entity =
                repository.findByRegionPoidAndGroupPoid(regionPoid, groupPoid)
                        .orElseThrow(() -> {
                            log.error("Region master not found with id: {}, groupPoid: {}", regionPoid, groupPoid);
                            return new ResourceNotFoundException(
                                    "RegionMaster",
                                    "regionPoid",
                                    regionPoid
                            );
                        });

        String newCode = request.getRegionCode() != null ? request.getRegionCode().trim() : null;
        String newName = request.getRegionName() != null ? request.getRegionName().trim() : null;

        // Check uniqueness of region code if changed
        if (newCode != null && !newCode.equals(entity.getRegionCode())
                && repository.existsByRegionCodeAndGroupPoidAndDeletedNotAndRegionPoidNot(
                        newCode, groupPoid, "Y", regionPoid)) {
            log.error("Region code already exists: {} for groupPoid: {}", newCode, groupPoid);
            throw new ValidationException(
                    "Region code already exists: " + newCode
            );
        }

        // Check uniqueness of region name if changed
        if (newName != null && !newName.equals(entity.getRegionName())
                && repository.existsByRegionNameAndGroupPoidAndDeletedNotAndRegionPoidNot(
                        newName, groupPoid, "Y", regionPoid)) {
            log.error("Region name already exists: {} for groupPoid: {}", newName, groupPoid);
            throw new ValidationException(
                    "Region name already exists: " + newName
            );
        }

        ShipRegionMasterEntity oldEntity = new ShipRegionMasterEntity();
        BeanUtils.copyProperties(entity, oldEntity);

        mapper.updateEntity(entity, request, userId);
        repository.save(entity);
        String key = entity.getRegionPoid().toString();

        loggingService.logChanges(oldEntity, entity, ShipRegionMasterEntity.class, docId, key,
                LogDetailsEnum.MODIFIED, "REGION_POID");

        log.info("Successfully updated region master with id: {}", regionPoid);
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void toggleActiveStatus(
            Long regionPoid,
            Long groupPoid,
            String userId) {
        log.info("Toggling active status for region master with id: {}, groupPoid: {}, userId: {}", 
                regionPoid, groupPoid, userId);

        ShipRegionMasterEntity entity =
                repository.findByRegionPoidAndGroupPoid(regionPoid, groupPoid)
                        .orElseThrow(() -> {
                            log.error("Region master not found with id: {}, groupPoid: {}", regionPoid, groupPoid);
                            return new ResourceNotFoundException(
                                    "RegionMaster",
                                    "regionPoid",
                                    regionPoid
                            );
                        });

        String currentActive = entity.getActive();
        String newActive = (currentActive == null || "N".equals(currentActive)) ? "Y" : "N";
        
        entity.setActive(newActive);
        entity.setLastModifiedBy(userId);
        entity.setLastModifiedDate(LocalDateTime.now());
        
        repository.save(entity);

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), regionPoid.toString());
        String logDetail = String.format("KeyId = REGION_POID:%s", regionPoid);
        String tableName = ShipRegionMasterEntity.class.getAnnotation(jakarta.persistence.Table.class).name();
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), regionPoid.toString(), "Active", currentActive, entity.getActive(), logDetail, tableName);

        log.info("Successfully toggled active status for region master with id: {} from {} to {}", 
                regionPoid, currentActive, newActive);
    }

    @Override
    @Transactional
    public void delete(
            Long regionPoid,
            Long groupPoid,
            String userId) {
        log.info("Deleting region master with id: {}, groupPoid: {}, userId: {}", 
                regionPoid, groupPoid, userId);

        ShipRegionMasterEntity entity =
                repository.findByRegionPoidAndGroupPoid(regionPoid, groupPoid)
                        .orElseThrow(() -> {
                            log.error("Region master not found with id: {}, groupPoid: {}", regionPoid, groupPoid);
                            return new ResourceNotFoundException(
                                    "RegionMaster",
                                    "regionPoid",
                                    regionPoid
                            );
                        });

        // Check if already deleted (idempotent operation)
        if ("Y".equals(entity.getDeleted())) {
            log.info("Region master with id: {} is already deleted", regionPoid);
            return;
        }

        entity.setActive("N");
        entity.setDeleted("Y");
        entity.setLastModifiedBy(userId);
        entity.setLastModifiedDate(LocalDateTime.now());
        
        repository.save(entity);

        String docId = UserContext.getDocumentId();
        String key = regionPoid.toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, docId, key);

        loggingService.logSimpleFieldChange(ShipRegionMasterEntity.class, docId, key, "deleted", "N", "Y", "ShipRegionMaster soft deleted");
        loggingService.logSimpleFieldChange(ShipRegionMasterEntity.class, docId, key, "active", "Y", "N", "ShipRegionMaster soft deleted");

        log.info("Successfully deleted region master with id: {}", regionPoid);
    }
}

