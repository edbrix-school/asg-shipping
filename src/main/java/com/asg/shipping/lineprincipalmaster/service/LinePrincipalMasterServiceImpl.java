package com.asg.shipping.lineprincipalmaster.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.common.dto.LovItem;
 import com.asg.shipping.common.service.LovService;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.exceptions.ValidationException;
import com.asg.shipping.lineprincipalmaster.dto.*;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMaster;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterChargeDtl;
import com.asg.shipping.lineprincipalmaster.repository.ShipLineMasterChargeDtlRepository;
import com.asg.shipping.lineprincipalmaster.repository.ShipLineMasterRepository;
import com.asg.shipping.lineprincipalmaster.util.LinePrincipalMasterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;
import static com.asg.common.lib.security.util.UserContext.getUserPoid;
import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;

/**
 * Service implementation for Line Principal Master operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LinePrincipalMasterServiceImpl implements LinePrincipalMasterService {

    private static final String DOC_ID = "100-007";

    private final ShipLineMasterRepository lineRepository;
    private final ShipLineMasterChargeDtlRepository chargeDtlRepository;
    private final DocumentSearchService documentSearchService;
    private final LovService lovService;
    private final LinePrincipalMasterMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchLines(com.asg.common.lib.dto.FilterRequestDto request, Pageable pageable) {
        log.info("Searching lines with page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentSearchService.resolveOperator(request);
        String isDeleted = documentSearchService.resolveIsDeleted(request);
        List<FilterDto> filters = documentSearchService.resolveFilters(request);

        RawSearchResult raw = documentSearchService.search(DOC_ID, filters, operator, pageable, isDeleted,
                "LINE_NAME", // label field
                "LINE_POID"); // value field

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public LinePrincipalMasterDto getLine(Long id) {
        log.info("Getting line with id: {}", id);

        Long groupPoid = getGroupPoid();
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line", "linePoid", id.toString()));

        LinePrincipalMasterDto dto = mapper.mapToDto(line);

        // Fetch charge details
        List<ShipLineMasterChargeDtl> charges = chargeDtlRepository.findByLinePoidOrderByDetRowId(id);
        dto.setCharges(mapper.mapChargeDetailsToDto(charges));

        // Enrich with LOV data
        enrichDtoWithLovData(dto, line, groupPoid);

        log.info("Successfully retrieved line with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public LinePrincipalMasterDto createLine(LinePrincipalMasterCreateDTO dto) {
        log.info("Creating line with code: {}, name: {}", dto.getLineCode(), dto.getLineName());

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();
        Long companyPoid = getCompanyPoid();

        // Validate
        validateLineCreateDTO(dto, groupPoid);

        // Create main entity
        ShipLineMaster line = new ShipLineMaster();
        mapper.mapCreateDTOToEntity(dto, line, groupPoid, userPoid, companyPoid);

        // Save main entity
        ShipLineMaster saved = lineRepository.save(line);

        // Create charge details
        if (dto.getCharges() != null && !dto.getCharges().isEmpty()) {
            createChargeDetails(saved.getLinePoid(), dto.getCharges(), userPoid);
        }

        // Call stored procedure
        callAfterSaveProcedure(groupPoid, companyPoid, userPoid, saved.getLinePoid());

        // Fetch and return with LOV data
        LinePrincipalMasterDto result = mapper.mapToDto(saved);
        List<ShipLineMasterChargeDtl> charges = chargeDtlRepository.findByLinePoidOrderByDetRowId(saved.getLinePoid());
        result.setCharges(mapper.mapChargeDetailsToDto(charges));
        enrichDtoWithLovData(result, saved, groupPoid);

        log.info("Successfully created line with id: {}", saved.getLinePoid());
        return result;
    }

    @Override
    @Transactional
    public LinePrincipalMasterDto updateLine(Long id, LinePrincipalMasterUpdateDTO dto) {
        log.info("Updating line with id: {}", id);

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();
        Long companyPoid = getCompanyPoid();

        // Find existing line
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line", "linePoid", id.toString()));

        // Validate
        validateLineUpdateDTO(dto, groupPoid, id);

        // Update main entity
        mapper.mapUpdateDTOToEntity(dto, line, groupPoid, userPoid, companyPoid);
        ShipLineMaster saved = lineRepository.save(line);

        // Handle charge details
        updateChargeDetails(id, dto.getCharges(), userPoid);

        // Call stored procedure
        callAfterSaveProcedure(groupPoid, companyPoid, userPoid, saved.getLinePoid());

        // Fetch and return with LOV data
        LinePrincipalMasterDto result = mapper.mapToDto(saved);
        List<ShipLineMasterChargeDtl> charges = chargeDtlRepository.findByLinePoidOrderByDetRowId(saved.getLinePoid());
        result.setCharges(mapper.mapChargeDetailsToDto(charges));
        enrichDtoWithLovData(result, saved, groupPoid);

        log.info("Successfully updated line with id: {}", id);
        return result;
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        log.info("Toggling active status for line with id: {}", id);

        Long groupPoid = getGroupPoid();
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line", "linePoid", id.toString()));

        String currentActive = line.getActive();
        line.setActive("Y".equals(currentActive) ? "N" : "Y");
        line.setLastModifiedBy(getCurrentUser());
        line.setLastModifiedDate(LocalDateTime.now());

        lineRepository.save(line);
        log.info("Successfully toggled active status for line with id: {} to {}", id, line.getActive());
    }

    @Override
    @Transactional
    public void deleteLine(Long id) {
        log.info("Deleting line with id: {}", id);

        Long groupPoid = getGroupPoid();
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line", "linePoid", id.toString()));

        if ("Y".equals(line.getDeleted())) {
            log.info("Line with id: {} is already deleted", id);
            return;
        }

        line.setDeleted("Y");
        line.setActive("N");
        line.setLastModifiedBy(getCurrentUser());
        line.setLastModifiedDate(LocalDateTime.now());

        lineRepository.save(line);
        log.info("Successfully deleted line with id: {}", id);
    }

    @Override
    @Transactional
    public Integer copyCharges(Long id, CopyChargesRequestDto request) {
        log.info("Copying charges from line {} to line {}", request.getSourceLinePoid(), id);

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();

        // Validate target line
        ShipLineMaster targetLine = lineRepository.findByLinePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Target Line", "linePoid", id.toString()));

        // Validate source line
        ShipLineMaster sourceLine = lineRepository.findByLinePoidAndGroupPoid(request.getSourceLinePoid(), groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Source Line", "linePoid", request.getSourceLinePoid().toString()));

        // Get source charges
        List<ShipLineMasterChargeDtl> sourceCharges = chargeDtlRepository.findByLinePoidOrderByDetRowId(request.getSourceLinePoid());

        if (sourceCharges.isEmpty()) {
            log.info("No charges found in source line {}", request.getSourceLinePoid());
            return 0;
        }

        // Get existing target charges to avoid duplicates
        Set<Long> existingChargePoids = chargeDtlRepository.findByLinePoidOrderByDetRowId(id).stream()
                .map(ShipLineMasterChargeDtl::getChargePoid)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        int copiedCount = 0;
        for (ShipLineMasterChargeDtl sourceCharge : sourceCharges) {
            // Skip if charge already exists in target
            if (sourceCharge.getChargePoid() != null && existingChargePoids.contains(sourceCharge.getChargePoid())) {
                continue;
            }

            // Create new charge detail for target line
            ShipLineMasterChargeDtl newCharge = ShipLineMasterChargeDtl.builder()
                    .linePoid(id)
                    .chargePoid(sourceCharge.getChargePoid())
                    .lineChargeCode(sourceCharge.getLineChargeCode())
                    .lineChargeDescription(sourceCharge.getLineChargeDescription())
                    .validUntil(sourceCharge.getValidUntil())
                    .remunCommissionCharge(sourceCharge.getRemunCommissionCharge())
                    .excludedFromEdi(sourceCharge.getExcludedFromEdi())
                    .defaultPrintGroupEdi(sourceCharge.getDefaultPrintGroupEdi())
                    .wkyrptIncludeAs(sourceCharge.getWkyrptIncludeAs())
                    .build();

            chargeDtlRepository.save(newCharge);
            copiedCount++;
        }

        log.info("Successfully copied {} charges from line {} to line {}", copiedCount, request.getSourceLinePoid(), id);
        return copiedCount;
    }

    @Override
    @Transactional
    public void createGlMaster(Long id) {
        log.info("Creating GL master for line with id: {}", id);

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        String userName = getCurrentUser();

        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line", "linePoid", id.toString()));

        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_LINE_GL_MASTER_CREATION(?,?,?,?,?,?,?,?)}")) {

            stmt.setLong(1, id);
            stmt.setLong(2, groupPoid);
            stmt.setLong(3, companyPoid);
            stmt.setString(4, userName);
            stmt.setString(5, DOC_ID);
            stmt.setString(6, line.getLineCode());
            stmt.setString(7, line.getLineName());
            stmt.registerOutParameter(8, Types.VARCHAR);

            stmt.execute();

            String status = stmt.getString(8);
            if (status != null && status.contains("ERROR")) {
                throw new com.asg.common.lib.exception.ValidationException("Error creating GL master: " + status);
            }
            if (status != null && status.contains("WARNING")) {
                log.warn("Warning while creating GL master: {}", status);
            }

            log.info("Successfully created GL master for line with id: {}", id);
        } catch (Exception e) {
            log.error("Error creating GL master for line with id: {}", id, e);
            throw new ValidationException("Error creating GL master: " + e.getMessage());
        }
    }

    /**
     * Create charge details for a line
     */
    private void createChargeDetails(Long linePoid, List<ChargeDetailDto> chargeDtos, Long userPoid) {
        String currentUser = getCurrentUser();
        Set<Long> chargePoids = new java.util.HashSet<>();

        for (ChargeDetailDto chargeDto : chargeDtos) {
            if (chargeDto.getChargePoid() != null) {
                // Check for duplicate charge POID within the request
                if (!chargePoids.add(chargeDto.getChargePoid())) {
                    throw new ValidationException("Duplicate charge POID: " + chargeDto.getChargePoid());
                }

                // Check if charge POID already exists for this line
                if (chargeDtlRepository.existsByLinePoidAndChargePoid(linePoid, chargeDto.getChargePoid())) {
                    throw new ValidationException("Charge POID " + chargeDto.getChargePoid() + " already exists for this line");
                }
            }

            ShipLineMasterChargeDtl charge = mapper.mapChargeDetailDtoToEntity(chargeDto, linePoid, currentUser);
            chargeDtlRepository.save(charge);
        }
    }

    /**
     * Update charge details for a line
     */
    private void updateChargeDetails(Long linePoid, List<ChargeDetailDto> chargeDtos, Long userPoid) {
        if (chargeDtos == null) {
            return;
        }

        String currentUser = getCurrentUser();
        List<ShipLineMasterChargeDtl> existingCharges = chargeDtlRepository.findByLinePoidOrderByDetRowId(linePoid);
        Set<Long> existingDetRowIds = existingCharges.stream()
                .map(ShipLineMasterChargeDtl::getDetRowId)
                .collect(Collectors.toSet());

        Set<Long> requestDetRowIds = chargeDtos.stream()
                .map(ChargeDetailDto::getDetRowId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // Delete charges not in request
        List<Long> toDelete = existingDetRowIds.stream()
                .filter(id -> !requestDetRowIds.contains(id))
                .collect(Collectors.toList());
        for (Long detRowId : toDelete) {
            chargeDtlRepository.deleteById(new com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterChargeDtlId(linePoid, detRowId));
        }

        // Update or create charges
        Set<Long> chargePoids = new java.util.HashSet<>();
        for (ChargeDetailDto chargeDto : chargeDtos) {
            if (chargeDto.getChargePoid() != null) {
                if (!chargePoids.add(chargeDto.getChargePoid())) {
                    throw new ValidationException("Duplicate charge POID: " + chargeDto.getChargePoid());
                }

                // Check uniqueness excluding current detail row
                if (chargeDto.getDetRowId() != null) {
                    if (chargeDtlRepository.existsByLinePoidAndChargePoidExcluding(linePoid, chargeDto.getChargePoid(), chargeDto.getDetRowId())) {
                        throw new ValidationException("Charge POID " + chargeDto.getChargePoid() + " already exists for this line");
                    }
                } else {
                    if (chargeDtlRepository.existsByLinePoidAndChargePoid(linePoid, chargeDto.getChargePoid())) {
                        throw new com.asg.common.lib.exception.ValidationException("Charge POID " + chargeDto.getChargePoid() + " already exists for this line");
                    }
                }
            }

            if (chargeDto.getDetRowId() != null) {
                // Update existing
                ShipLineMasterChargeDtl existing = chargeDtlRepository.findById(
                        new com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterChargeDtlId(linePoid, chargeDto.getDetRowId()))
                        .orElseThrow(() -> new ResourceNotFoundException("Charge Detail", "detRowId", chargeDto.getDetRowId().toString()));

                mapper.updateChargeDetailFromDto(chargeDto, existing, currentUser);
                chargeDtlRepository.save(existing);
            } else {
                // Create new
                ShipLineMasterChargeDtl newCharge = mapper.mapChargeDetailDtoToEntity(chargeDto, linePoid, currentUser);
                chargeDtlRepository.save(newCharge);
            }
        }
    }

    /**
     * Call PROC_LINE_MASTER_AFTER_SAVE stored procedure
     */
    private void callAfterSaveProcedure(Long groupPoid, Long companyPoid, Long userPoid, Long linePoid) {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             CallableStatement stmt = conn.prepareCall("{call PROC_LINE_MASTER_AFTER_SAVE(?,?,?,?,?,?)}")) {

            stmt.setLong(1, groupPoid);
            stmt.setLong(2, companyPoid);
            stmt.setLong(3, userPoid);
            stmt.setString(4, DOC_ID);
            stmt.setLong(5, linePoid);
            stmt.registerOutParameter(6, Types.VARCHAR);

            stmt.execute();

            String status = stmt.getString(6);
            if (status != null && status.contains("ERROR")) {
                log.error("Error in PROC_LINE_MASTER_AFTER_SAVE: {}", status);
                throw new ValidationException("Error in after save procedure: " + status);
            }
            if (status != null && status.contains("WARNING")) {
                log.warn("Warning in PROC_LINE_MASTER_AFTER_SAVE: {}", status);
            }
        } catch (Exception e) {
            log.error("Error calling PROC_LINE_MASTER_AFTER_SAVE for line: {}", linePoid, e);
            // Don't throw exception, just log it (as per legacy behavior)
        }
    }

    /**
     * Enrich DTO with LOV data
     */
    private void enrichDtoWithLovData(LinePrincipalMasterDto dto, ShipLineMaster line, Long groupPoid) {
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();

        try {
            if (line.getCountryPoid() != null) {
                dto.setCountryDet(lovService.getLovItemByPoid(line.getCountryPoid(), "COUNTRY", groupPoid, companyPoid, userPoid));
            }
            if (line.getCurrencyPoid() != null) {
                dto.setCurrencyDet(lovService.getLovItemByPoid(line.getCurrencyPoid(), "CURRENCY", groupPoid, companyPoid, userPoid));
            }
            if (line.getPrincipalPoid() != null) {
                dto.setPrincipalDet(lovService.getLovItemByPoid(line.getPrincipalPoid(), "PRINCIPAL_MASTER_FOR_PDA", groupPoid, companyPoid, userPoid));
            }
            if (line.getCompanyPoid() != null) {
                dto.setCompanyDet(lovService.getLovItemByPoid(line.getCompanyPoid(), "COMPANY", groupPoid, companyPoid, userPoid));
            }
            if (line.getBankGuaranteeBankPoid() != null) {
                dto.setBankDet(lovService.getLovItemByPoid(line.getBankGuaranteeBankPoid(), "BANK_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (line.getLinePortRefno() != null) {
                // For tradelane, we need to get by code, not POID
                // This is a simplified version - actual implementation may need to query by code
            }
            if (line.getBillTo() != null) {
                // For billTo, we need to get by code, not POID
                // This is a simplified version - actual implementation may need to query by code
            }
        } catch (Exception e) {
            log.warn("Failed to fetch some LOV data", e);
        }

        // Enrich charge details with LOV data
        if (dto.getCharges() != null) {
            for (ChargeDetailDto charge : dto.getCharges()) {
                try {
                    if (charge.getChargePoid() != null) {
                        charge.setChargeDet(lovService.getLovItemByPoid(charge.getChargePoid(), "CHARGE_MASTER", groupPoid, companyPoid, userPoid));
                    }
                    if (charge.getWkyrptIncludeAs() != null) {
                        charge.setWkyrptIncludeAsDet(lovService.getLovItemByPoid(charge.getWkyrptIncludeAs(), "CHARGE_MASTER", groupPoid, companyPoid, userPoid));
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch LOV data for charge", e);
                }
            }
        }
    }

    /**
     * Validate LineCreateDTO
     */
    private void validateLineCreateDTO(LinePrincipalMasterCreateDTO dto, Long groupPoid) {
        if (lineRepository.existsByLineCodeAndGroupPoid(dto.getLineCode(), groupPoid)) {
            throw new ValidationException("Line code already exists for this group");
        }
        if (lineRepository.existsByLineNameAndGroupPoid(dto.getLineName(), groupPoid)) {
            throw new ValidationException("Line name already exists for this group");
        }
    }

    /**
     * Validate LineUpdateDTO
     */
    private void validateLineUpdateDTO(LinePrincipalMasterUpdateDTO dto, Long groupPoid, Long excludeLinePoid) {
        if (lineRepository.existsByLineCodeAndGroupPoidExcluding(dto.getLineCode(), groupPoid, excludeLinePoid)) {
            throw new ValidationException("Line code already exists for this group");
        }
        if (lineRepository.existsByLineNameAndGroupPoidExcluding(dto.getLineName(), groupPoid, excludeLinePoid)) {
            throw new ValidationException("Line name already exists for this group");
        }
    }
}

