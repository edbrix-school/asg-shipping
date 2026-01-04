package com.asg.shipping.vesseltypemaster.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeCreateDTO;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeDto;
import com.asg.shipping.vesseltypemaster.dto.VesselTypeUpdateDTO;
import com.asg.shipping.vesseltypemaster.entity.ShipVesselTypeMaster;
import com.asg.shipping.vesseltypemaster.repository.ShipVesselTypeMasterRepository;
import com.asg.shipping.vesseltypemaster.util.VesselTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Service implementation for Vessel Type operations
 */
@Service

@RequiredArgsConstructor
@Slf4j
public class VesselTypeServiceImpl implements VesselTypeService {

    private final ShipVesselTypeMasterRepository vesselTypeRepository;
    private final DocumentSearchService documentSearchService;
    private final VesselTypeMapper mapper;
    private final LoggingService loggingService;
    private final com.asg.common.lib.service.LovDataService lovService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchVesselTypes(String docId, com.asg.common.lib.dto.FilterRequestDto request, Pageable pageable) {
        log.info("Searching vessel types with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentSearchService.resolveOperator(request);
        String isDeleted = documentSearchService.resolveIsDeleted(request);
        List<FilterDto> filters = documentSearchService.resolveFilters(request);

        RawSearchResult raw = documentSearchService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "VESSEL_TYPE_NAME",
                "VESSEL_TYPE_POID"
        );

        Page<Map<String, Object>> page = new PageImpl<>(
                raw.records(),
                pageable,
                raw.totalRecords()
        );

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public VesselTypeDto getVesselType(Long id) {
        log.info("Getting vessel type with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipVesselTypeMaster vesselType = vesselTypeRepository.findByVesselTypePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Vessel Type", "vesselTypePoid", id.toString()));

        VesselTypeDto dto = mapper.mapToDto(vesselType);
        enrichDtoWithLovData(dto, vesselType, groupPoid);

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), id.toString());

        log.info("Successfully retrieved vessel type with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public VesselTypeDto createVesselType(VesselTypeCreateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Creating vessel type with code: {}, groupId: {}, userPoid: {}", dto.getVesselTypeCode(), groupPoid, userPoid);

        validateVesselTypeCreateDTO(dto);

        ShipVesselTypeMaster vesselType = new ShipVesselTypeMaster();
        mapper.mapCreateDTOToEntity(dto, vesselType, groupPoid, userPoid);

        ShipVesselTypeMaster saved = vesselTypeRepository.save(vesselType);

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getVesselTypePoid().toString());

        VesselTypeDto result = mapper.mapToDto(saved);
        enrichDtoWithLovData(result, saved, groupPoid);

        log.info("Successfully created vessel type with id: {}", saved.getVesselTypePoid());
        return result;
    }

    @Override
    @Transactional
    public VesselTypeDto updateVesselType(Long id, VesselTypeUpdateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Updating vessel type with id: {}, groupId: {}, userPoid: {}", id, groupPoid, userPoid);

        ShipVesselTypeMaster vesselType = vesselTypeRepository.findByVesselTypePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Vessel Type", "vesselTypePoid", id.toString()));

        validateVesselTypeUpdateDTO(dto, id);

        ShipVesselTypeMaster oldVesselType = ShipVesselTypeMaster.builder()
                .vesselTypeName(vesselType.getVesselTypeName())
                .vesselTypeName2(vesselType.getVesselTypeName2())
                .active(vesselType.getActive())
                .seqno(vesselType.getSeqno())
                .build();

        mapper.mapUpdateDTOToEntity(dto, vesselType, groupPoid, userPoid);

        ShipVesselTypeMaster saved = vesselTypeRepository.save(vesselType);

        loggingService.logChanges(oldVesselType, saved, ShipVesselTypeMaster.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, "VESSEL_TYPE_POID");

        VesselTypeDto result = mapper.mapToDto(saved);
        enrichDtoWithLovData(result, saved, groupPoid);

        log.info("Successfully updated vessel type with id: {}", id);
        return result;
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        log.info("Toggling active status for vessel type with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipVesselTypeMaster vesselType = vesselTypeRepository.findByVesselTypePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Vessel Type", "vesselTypePoid", id.toString()));

        final String currentActive = vesselType.getActive();
        vesselType.setActive("Y".equals(currentActive) ? "N" : "Y");
        vesselType.setLastModifiedBy(getCurrentUser());
        vesselType.setLastModifiedDate(LocalDateTime.now());

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), id.toString());
        String logDetail = String.format("KeyId = VESSEL_TYPE_POID:%s", id);
        String tableName = ShipVesselTypeMaster.class.getAnnotation(jakarta.persistence.Table.class).name();
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), id.toString(), "Active", currentActive, vesselType.getActive(), logDetail, tableName);

        vesselTypeRepository.save(vesselType);

        log.info("Successfully toggled active status for vessel type with id: {} to {}", id, vesselType.getActive());
    }

    @Override
    @Transactional
    public void deleteVesselType(Long id) {
        log.info("Deleting vessel type with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipVesselTypeMaster vesselType = vesselTypeRepository.findByVesselTypePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Vessel Type", "vesselTypePoid", id.toString()));

        if ("Y".equals(vesselType.getDeleted())) {
            log.info("Vessel type with id: {} is already deleted", id);
            return;
        }

        vesselType.setDeleted("Y");
        vesselType.setActive("N");
        vesselType.setLastModifiedBy(getCurrentUser());
        vesselType.setLastModifiedDate(LocalDateTime.now());

        vesselTypeRepository.save(vesselType);

        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, UserContext.getDocumentId(), id.toString());
        String logDetail = String.format("KeyId = VESSEL_TYPE_POID:%s", id);
        String tableName = ShipVesselTypeMaster.class.getAnnotation(jakarta.persistence.Table.class).name();
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), id.toString(), "Deleted", "N", "Y", logDetail, tableName);
        loggingService.createLogDetailsEntry(UserContext.getDocumentId(), id.toString(), "Active", "Y", "N", logDetail, tableName);

        log.info("Successfully deleted vessel type with id: {}", id);
    }

    /**
     * Validate VesselTypeCreateDTO
     */
    private void validateVesselTypeCreateDTO(VesselTypeCreateDTO dto) {
        if (vesselTypeRepository.existsByVesselTypeCode(dto.getVesselTypeCode())) {
            throw new ResourceAlreadyExistsException("Vessel type code", dto.getVesselTypeCode());
        }

        if (vesselTypeRepository.existsByVesselTypeName(dto.getVesselTypeName())) {
            throw new ResourceAlreadyExistsException("Vessel type name", dto.getVesselTypeName());
        }

        validateCostCentre(dto.getCostCentrePoid());
    }

    /**
     * Validate VesselTypeUpdateDTO
     */
    private void validateVesselTypeUpdateDTO(VesselTypeUpdateDTO dto, Long excludeVesselTypePoid) {
        if (vesselTypeRepository.existsByVesselTypeNameExcludingPoid(dto.getVesselTypeName(), excludeVesselTypePoid)) {
            throw new ResourceAlreadyExistsException("Vessel type name", dto.getVesselTypeName());
        }

        validateCostCentre(dto.getCostCentrePoid());
    }

    private void validateCostCentre(Long costCentrePoid) {
        if (costCentrePoid != null) {
            com.asg.common.lib.dto.LovGetListDto lovGetListDto = lovService.getDetailsByPoidAndLovName(costCentrePoid, "GL_COST_CENTRE");
            if (lovGetListDto == null || lovGetListDto.getPoid() == null) {
                throw new com.asg.common.lib.exception.ValidationException("Cost Centre is not active");
            }
        }
    }

    private void enrichDtoWithLovData(VesselTypeDto dto, ShipVesselTypeMaster entity, Long groupPoid) {
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        try {
            if (entity.getCostCentrePoid() != null) {
                dto.setCostCentreDet(lovService.getDetailsByPoidAndLovName(entity.getCostCentrePoid(), "GL_COST_CENTRE"));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch LOV data", e);
        }
    }
}
