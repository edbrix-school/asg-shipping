package com.asg.shipping.vesselmaster.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.exceptions.ValidationException;
import com.asg.shipping.vesselmaster.dto.*;
import com.asg.shipping.vesselmaster.entity.ShipVesselMaster;
import com.asg.shipping.vesselmaster.repository.ShipVesselMasterRepository;
import com.asg.shipping.vesselmaster.util.VesselMasterMapper;
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
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;
import static com.asg.common.lib.security.util.UserContext.getUserPoid;
import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;

/**
 * Service implementation for Vessel Master operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VesselMasterServiceImpl implements VesselMasterService {

    private static final String DOC_ID = "100-008";

    private final ShipVesselMasterRepository vesselRepository;
    private final DocumentSearchService documentSearchService;
    private final VesselMasterLovService vesselMasterLovService;
    private final VesselMasterMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchVessels(com.asg.common.lib.dto.FilterRequestDto request, Pageable pageable) {
        log.info("Searching vessels with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentSearchService.resolveOperator(request);
        String isDeleted = documentSearchService.resolveIsDeleted(request);
        List<FilterDto> filters = documentSearchService.resolveFilters(request);

        RawSearchResult raw = documentSearchService.search(DOC_ID, filters, operator, pageable, isDeleted,
                "VESSEL_NAME", // label field
                "VESSEL_POID"); // value field

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public VesselMasterDto getVessel(Long id) {
        log.info("Getting vessel with id: {}", id);

        Long groupPoid = getGroupPoid();
        ShipVesselMaster vessel = vesselRepository.findByVesselPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Vessel", "vesselPoid", id.toString()));

        VesselMasterDto dto = mapper.mapToDto(vessel);

        // Enrich with LOV data
        enrichDtoWithLovData(dto, vessel, groupPoid);

        log.info("Successfully retrieved vessel with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public VesselMasterDto createVessel(VesselMasterCreateDTO dto) {
        log.info("Creating vessel with code: {}, name: {}", dto.getVesselCode(), dto.getVesselName());

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();
        Long companyPoid = getCompanyPoid();

        // Validate
        validateVesselCreateDTO(dto, groupPoid);

        // Create main entity
        ShipVesselMaster vessel = new ShipVesselMaster();
        mapper.mapCreateDTOToEntity(dto, vessel, groupPoid, userPoid, companyPoid);

        // Save main entity
        ShipVesselMaster saved = vesselRepository.save(vessel);

        // Fetch and return with LOV data
        VesselMasterDto result = mapper.mapToDto(saved);
        enrichDtoWithLovData(result, saved, groupPoid);

        log.info("Successfully created vessel with id: {}", saved.getVesselPoid());
        return result;
    }

    @Override
    @Transactional
    public VesselMasterDto updateVessel(Long id, VesselMasterUpdateDTO dto) {
        log.info("Updating vessel with id: {}", id);

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();
        Long companyPoid = getCompanyPoid();

        // Find existing vessel
        ShipVesselMaster vessel = vesselRepository.findByVesselPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Vessel", "vesselPoid", id.toString()));

        // Validate
        validateVesselUpdateDTO(dto, groupPoid, id);

        // Update main entity
        mapper.mapUpdateDTOToEntity(dto, vessel, groupPoid, userPoid, companyPoid);
        ShipVesselMaster saved = vesselRepository.save(vessel);

        // Fetch and return with LOV data
        VesselMasterDto result = mapper.mapToDto(saved);
        enrichDtoWithLovData(result, saved, groupPoid);

        log.info("Successfully updated vessel with id: {}", id);
        return result;
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        log.info("Toggling active status for vessel with id: {}", id);

        Long groupPoid = getGroupPoid();
        ShipVesselMaster vessel = vesselRepository.findByVesselPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Vessel", "vesselPoid", id.toString()));

        String currentActive = vessel.getActive();
        vessel.setActive("Y".equals(currentActive) ? "N" : "Y");
        vessel.setLastModifiedBy(getCurrentUser());
        vessel.setLastModifiedDate(LocalDateTime.now());

        vesselRepository.save(vessel);
        log.info("Successfully toggled active status for vessel with id: {} to {}", id, vessel.getActive());
    }

    @Override
    @Transactional
    public void deleteVessel(Long id) {
        log.info("Deleting vessel with id: {}", id);

        Long groupPoid = getGroupPoid();
        ShipVesselMaster vessel = vesselRepository.findByVesselPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Vessel", "vesselPoid", id.toString()));

        if ("Y".equals(vessel.getDeleted())) {
            log.info("Vessel with id: {} is already deleted", id);
            return;
        }

        vessel.setDeleted("Y");
        vessel.setActive("N");
        vessel.setLastModifiedBy(getCurrentUser());
        vessel.setLastModifiedDate(LocalDateTime.now());

        vesselRepository.save(vessel);
        log.info("Successfully deleted vessel with id: {}", id);
    }

    /**
     * Enrich DTO with LOV data
     */
    private void enrichDtoWithLovData(VesselMasterDto dto, ShipVesselMaster vessel, Long groupPoid) {
        try {
            if (vessel.getLinePoid() != null) {
                List<LovItem> lineLov = vesselMasterLovService.getLineMasterLov(vessel.getLinePoid());
                if (lineLov != null && !lineLov.isEmpty()) {
                    dto.setLineDet(lineLov.get(0));
                }
            }
            if (vessel.getVesselTypePoid() != null) {
                List<LovItem> vesselTypeLov = vesselMasterLovService.getVesselTypeLov(vessel.getVesselTypePoid());
                if (vesselTypeLov != null && !vesselTypeLov.isEmpty()) {
                    dto.setVesselTypeDet(vesselTypeLov.get(0));
                }
            }
            if (vessel.getAgentPoid() != null) {
                try {
                    List<LovItem> agentLov = vesselMasterLovService.getAgentMasterLov(vessel.getAgentPoid());
                    if (agentLov != null && !agentLov.isEmpty()) {
                        dto.setAgentDet(agentLov.get(0));
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch AGENT_MASTER LOV for agentPoid: {}", vessel.getAgentPoid(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch some LOV data", e);
        }
    }

    /**
     * Validate VesselCreateDTO
     */
    private void validateVesselCreateDTO(VesselMasterCreateDTO dto, Long groupPoid) {
        // Validate IMO number uniqueness if provided
        if (dto.getImoNumber() != null && !dto.getImoNumber().trim().isEmpty()) {
            if (vesselRepository.existsByImoNumberAndGroupPoid(dto.getImoNumber(), groupPoid)) {
                throw new ValidationException("IMO number already exists for this group");
            }
        }
    }

    /**
     * Validate VesselUpdateDTO
     */
    private void validateVesselUpdateDTO(VesselMasterUpdateDTO dto, Long groupPoid, Long excludeVesselPoid) {
        // Validate IMO number uniqueness if provided (excluding current vessel)
        if (dto.getImoNumber() != null && !dto.getImoNumber().trim().isEmpty()) {
            if (vesselRepository.existsByImoNumberAndGroupPoidExcluding(dto.getImoNumber(), groupPoid, excludeVesselPoid)) {
                throw new ValidationException("IMO number already exists for this group");
            }
        }
    }
}

