package com.asg.shipping.lineprincipalmaster.service;

import com.asg.common.lib.dto.AddressDetailsDTO;
import com.asg.common.lib.dto.AddressTypeMapDTO;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.common.entity.GlobalAddressDetails;
import com.asg.shipping.common.entity.GlobalAddressMaster;
import com.asg.shipping.common.entity.ShipLineMasterType;
import com.asg.shipping.common.entity.ShipLineMasterTypeId;
import com.asg.shipping.common.repository.GlobalAddressDetailsRepository;
import com.asg.shipping.common.repository.GlobalAddressMasterRepository;
import com.asg.shipping.common.repository.ShipLineMasterTypeRepository;
import com.asg.shipping.common.service.LovService;
import com.asg.shipping.exceptions.CustomException;
import com.asg.shipping.exceptions.ResourceAlreadyExistsException;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.exceptions.ValidationException;
import com.asg.shipping.lineprincipalmaster.dto.*;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMaster;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterChargeDtl;
import com.asg.shipping.lineprincipalmaster.repository.ShipLineMasterChargeDtlRepository;
import com.asg.shipping.lineprincipalmaster.repository.ShipLineMasterRepository;
import com.asg.shipping.lineprincipalmaster.util.LinePrincipalMasterMapper;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterUserRoleDtl;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterUserRoleDtlId;
import com.asg.shipping.lineprincipalmaster.repository.ShipLineMasterUserRoleDtlRepository;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterPicDtl;
import com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterPicDtlId;
import com.asg.shipping.lineprincipalmaster.repository.ShipLineMasterPicDtlRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private static final String LINE_POID_COLUMN = "LINE_POID";
    private static final String SHIP_LINE_MASTER_TABLE = "SHIP_LINE_MASTER";

    private final ShipLineMasterRepository lineRepository;
    private final ShipLineMasterChargeDtlRepository chargeDtlRepository;
    private final ShipLineMasterTypeRepository containerTypeRepository;
    private final ShipLineMasterUserRoleDtlRepository userRoleDtlRepository;
    private final ShipLineMasterPicDtlRepository picDtlRepository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LovService lovService;
    private final LinePrincipalMasterMapper mapper;
    private final LoggingService loggingService;
    private final JdbcTemplate jdbcTemplate;
    private final GlobalAddressMasterRepository addressMasterRepository;
    private final GlobalAddressDetailsRepository addressDetailsRepository;
    private final EntityManager entityManager;

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

        // Fetch container type details
        List<ShipLineMasterType> containerTypes = containerTypeRepository.findByLinePoidOrderByDetRowId(id);
        dto.setContainerTypes(mapper.mapContainerTypeDetailsToDto(containerTypes));

        // Fetch user role details
        List<ShipLineMasterUserRoleDtl> userRoles = userRoleDtlRepository.findByLinePoidOrderByDetRowId(id);
        dto.setUserRoles(mapper.mapUserRoleDetailsToDto(userRoles));

        // Fetch PIC details
        List<ShipLineMasterPicDtl> picDetails = picDtlRepository.findByLinePoidOrderByDetRowId(id);
        dto.setPicDetails(mapper.mapPicDetailsToDto(picDetails));

        // Fetch address type map
        if (line.getAddressPoid() != null) {
            List<GlobalAddressDetails> addressDetails = addressDetailsRepository.findByAddressMasterPoid(line.getAddressPoid());
            if (!addressDetails.isEmpty()) {
                Map<String, List<AddressDetailsDTO>> byType = addressDetails.stream()
                        .filter(a -> a.getAddressType() != null)
                        .collect(Collectors.groupingBy(
                                GlobalAddressDetails::getAddressType,
                                Collectors.mapping(mapper::mapToAddressDetailsDto, Collectors.toList())
                        ));
                AddressTypeMapDTO addressTypeMap = new AddressTypeMapDTO();
                addressTypeMap.setMain(byType.get("MAIN"));
                addressTypeMap.setFinance(byType.get("FINANCE"));
                addressTypeMap.setSales(byType.get("SALES"));
                addressTypeMap.setOperation(byType.get("OPERATION"));
                addressTypeMap.setInvoiceAddress(byType.get("INVOICE_ADDRESS"));
                addressTypeMap.setDeliveryOrder(byType.get("DELIVERY_ORDER"));
                addressTypeMap.setShipChandling(byType.get("SHIP_CHANDLING"));
                addressTypeMap.setClaimUac(byType.get("CLAIM_UAC"));
                addressTypeMap.setCan(byType.get("CAN"));
                dto.setAddressTypeMap(addressTypeMap);
            }
        }

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

        GlobalAddressMaster addressMaster = resolveAddressMasterForCreate(
                dto.getAddressPoid(), dto.getLineName(), dto.getSeqno(), groupPoid);
        dto.setAddressPoid(addressMaster.getAddressMasterPoid());

        // Create main entity
        ShipLineMaster line = new ShipLineMaster();
        mapper.mapCreateDTOToEntity(dto, line, groupPoid, userPoid, companyPoid);

        // Save main entity
        // Force parent insert before inserting child rows (Oracle FK checks are immediate)
        ShipLineMaster saved = lineRepository.saveAndFlush(line);

        // Some environments assign/override LINE_POID in DB (e.g., trigger). Re-resolve to the DB value
        // and use the reloaded entity for all downstream operations + response.
        ShipLineMaster resolvedLine = lineRepository.findByLineCodeAndGroupPoid(saved.getLineCode(), groupPoid)
                .orElseThrow(() -> new ValidationException("Failed to resolve LINE_POID for newly created line."));
        Long resolvedLinePoid = resolvedLine.getLinePoid();

        if (dto.getAddressTypeMap() != null) {
            saveAllAddressDetails(dto.getAddressTypeMap(), addressMaster, getCurrentUser(), resolvedLinePoid.toString());
            refreshPersistenceContext();
        }

        // Create charge details
        if (dto.getCharges() != null && !dto.getCharges().isEmpty()) {
            createChargeDetails(resolvedLinePoid, dto.getCharges(), userPoid);
        }

        // Create container type details
        if (dto.getContainerTypes() != null && !dto.getContainerTypes().isEmpty()) {
            createContainerTypeDetails(resolvedLinePoid, dto.getContainerTypes(), userPoid);
        }

        // Create user role details
        if (dto.getUserRoles() != null && !dto.getUserRoles().isEmpty()) {
            createUserRoleDetails(resolvedLinePoid, dto.getUserRoles(), userPoid);
        }

        // Create PIC details
        if (dto.getPicDetails() != null && !dto.getPicDetails().isEmpty()) {
            createPicDetails(resolvedLinePoid, dto.getPicDetails(), userPoid);
        }

        // Call stored procedure
        callAfterSaveProcedure(groupPoid, companyPoid, userPoid, resolvedLinePoid);
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, currentDocumentId(), resolvedLinePoid.toString());

        // Fetch and return with LOV data
        LinePrincipalMasterDto result = mapper.mapToDto(resolvedLine);
        List<ShipLineMasterChargeDtl> charges = chargeDtlRepository.findByLinePoidOrderByDetRowId(resolvedLinePoid);
        result.setCharges(mapper.mapChargeDetailsToDto(charges));
        List<ShipLineMasterType> containerTypes = containerTypeRepository.findByLinePoidOrderByDetRowId(resolvedLinePoid);
        result.setContainerTypes(mapper.mapContainerTypeDetailsToDto(containerTypes));
        List<ShipLineMasterUserRoleDtl> savedUserRoles = userRoleDtlRepository.findByLinePoidOrderByDetRowId(resolvedLinePoid);
        result.setUserRoles(mapper.mapUserRoleDetailsToDto(savedUserRoles));
        List<ShipLineMasterPicDtl> savedPicDetails = picDtlRepository.findByLinePoidOrderByDetRowId(resolvedLinePoid);
        result.setPicDetails(mapper.mapPicDetailsToDto(savedPicDetails));
        enrichDtoWithLovData(result, resolvedLine, groupPoid);

        // Populate addressTypeMap
        if (resolvedLine.getAddressPoid() != null) {
            List<GlobalAddressDetails> addressDetails = addressDetailsRepository.findByAddressMasterPoid(resolvedLine.getAddressPoid());
            if (!addressDetails.isEmpty()) {
                Map<String, List<com.asg.common.lib.dto.AddressDetailsDTO>> byType = addressDetails.stream()
                        .filter(a -> a.getAddressType() != null)
                        .collect(Collectors.groupingBy(
                                GlobalAddressDetails::getAddressType,
                                Collectors.mapping(mapper::mapToAddressDetailsDto, Collectors.toList())
                        ));
                com.asg.common.lib.dto.AddressTypeMapDTO addressTypeMap = new com.asg.common.lib.dto.AddressTypeMapDTO();
                addressTypeMap.setMain(byType.get("MAIN"));
                addressTypeMap.setFinance(byType.get("FINANCE"));
                addressTypeMap.setSales(byType.get("SALES"));
                addressTypeMap.setOperation(byType.get("OPERATION"));
                addressTypeMap.setInvoiceAddress(byType.get("INVOICE_ADDRESS"));
                addressTypeMap.setDeliveryOrder(byType.get("DELIVERY_ORDER"));
                addressTypeMap.setShipChandling(byType.get("SHIP_CHANDLING"));
                addressTypeMap.setClaimUac(byType.get("CLAIM_UAC"));
                addressTypeMap.setCan(byType.get("CAN"));
                result.setAddressTypeMap(addressTypeMap);
            }
        }

        log.info("Successfully created line with id: {}", resolvedLinePoid);
        return result;
    }

    @Override
    @Transactional(timeout = 180)
    public LinePrincipalMasterDto updateLine(Long id, LinePrincipalMasterUpdateDTO dto) {
        log.info("Updating line with id: {}", id);

        Long groupPoid = getGroupPoid();
        Long userPoid = getUserPoid();
        Long companyPoid = getCompanyPoid();

        // Find existing line
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line", "linePoid", id.toString()));
        ShipLineMaster oldLine = new ShipLineMaster();
        BeanUtils.copyProperties(line, oldLine);

        // Validate
        validateLineUpdateDTO(dto, groupPoid, id);

        GlobalAddressMaster addressMaster = resolveAddressMasterForUpdate(
                dto.getAddressPoid(), dto.getLineName(), dto.getSeqno(), groupPoid);
        dto.setAddressPoid(addressMaster.getAddressMasterPoid());

        // Update main entity
        mapper.mapUpdateDTOToEntity(dto, line, groupPoid, userPoid, companyPoid);
        ShipLineMaster saved = lineRepository.save(line);

        if (dto.getAddressTypeMap() != null) {
            saveAllAddressDetails(dto.getAddressTypeMap(), addressMaster, getCurrentUser(), id.toString());
            refreshPersistenceContext();
        }

        // Handle charge details
        updateChargeDetails(id, dto.getCharges(), userPoid);

        // Handle container type details
        updateContainerTypeDetails(id, dto.getContainerTypes(), userPoid);

        // Handle user role details
        updateUserRoleDetails(id, dto.getUserRoles(), userPoid);

        // Handle PIC details
        updatePicDetails(id, dto.getPicDetails(), userPoid);

        // Call stored procedure
        callAfterSaveProcedure(groupPoid, companyPoid, userPoid, saved.getLinePoid());
    loggingService.logChanges(oldLine, saved, ShipLineMaster.class, currentDocumentId(), id.toString(),
        LogDetailsEnum.MODIFIED, LINE_POID_COLUMN);

        // Fetch and return with LOV data
        LinePrincipalMasterDto result = mapper.mapToDto(saved);
        List<ShipLineMasterChargeDtl> charges = chargeDtlRepository.findByLinePoidOrderByDetRowId(saved.getLinePoid());
        result.setCharges(mapper.mapChargeDetailsToDto(charges));
        List<ShipLineMasterType> containerTypes = containerTypeRepository.findByLinePoidOrderByDetRowId(saved.getLinePoid());
        result.setContainerTypes(mapper.mapContainerTypeDetailsToDto(containerTypes));
        List<ShipLineMasterUserRoleDtl> updatedUserRoles = userRoleDtlRepository.findByLinePoidOrderByDetRowId(saved.getLinePoid());
        result.setUserRoles(mapper.mapUserRoleDetailsToDto(updatedUserRoles));
        List<ShipLineMasterPicDtl> updatedPicDetails = picDtlRepository.findByLinePoidOrderByDetRowId(saved.getLinePoid());
        result.setPicDetails(mapper.mapPicDetailsToDto(updatedPicDetails));
        //enrichDtoWithLovData(result, saved, groupPoid);

        // Populate addressTypeMap
        if (saved.getAddressPoid() != null) {
            List<GlobalAddressDetails> addressDetails = addressDetailsRepository.findByAddressMasterPoid(saved.getAddressPoid());
            if (!addressDetails.isEmpty()) {
                Map<String, List<com.asg.common.lib.dto.AddressDetailsDTO>> byType = addressDetails.stream()
                        .filter(a -> a.getAddressType() != null)
                        .collect(Collectors.groupingBy(
                                GlobalAddressDetails::getAddressType,
                                Collectors.mapping(mapper::mapToAddressDetailsDto, Collectors.toList())
                        ));
                com.asg.common.lib.dto.AddressTypeMapDTO addressTypeMap = new com.asg.common.lib.dto.AddressTypeMapDTO();
                addressTypeMap.setMain(byType.get("MAIN"));
                addressTypeMap.setFinance(byType.get("FINANCE"));
                addressTypeMap.setSales(byType.get("SALES"));
                addressTypeMap.setOperation(byType.get("OPERATION"));
                addressTypeMap.setInvoiceAddress(byType.get("INVOICE_ADDRESS"));
                addressTypeMap.setDeliveryOrder(byType.get("DELIVERY_ORDER"));
                addressTypeMap.setShipChandling(byType.get("SHIP_CHANDLING"));
                addressTypeMap.setClaimUac(byType.get("CLAIM_UAC"));
                addressTypeMap.setCan(byType.get("CAN"));
                result.setAddressTypeMap(addressTypeMap);
            }
        }

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
        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, currentDocumentId(), id.toString());
        String logDetail = String.format("KeyId = %s:%s", LINE_POID_COLUMN, id);
        loggingService.createLogDetailsEntry(currentDocumentId(), id.toString(), "Active", currentActive,
                line.getActive(), logDetail, SHIP_LINE_MASTER_TABLE);
        log.info("Successfully toggled active status for line with id: {} to {}", id, line.getActive());
    }

    @Override
    @Transactional
    public void deleteLine(Long id, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting line with id: {}", id);

        Long groupPoid = getGroupPoid();
        ShipLineMaster line = lineRepository.findByLinePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line", "linePoid", id.toString()));

        if ("Y".equals(line.getDeleted())) {
            log.info("Line with id: {} is already deleted", id);
            return;
        }

        documentDeleteService.deleteDocument(id, SHIP_LINE_MASTER_TABLE, LINE_POID_COLUMN, deleteReasonDto, null);

        line.setDeleted("Y");
        line.setActive("N");
        line.setLastModifiedBy(getCurrentUser());
        line.setLastModifiedDate(LocalDateTime.now());

        lineRepository.save(line);
        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, currentDocumentId(), id.toString());
        String logDetail = String.format("KeyId = %s:%s", LINE_POID_COLUMN, id);
        loggingService.createLogDetailsEntry(currentDocumentId(), id.toString(), "Deleted", "N", "Y", logDetail,
            SHIP_LINE_MASTER_TABLE);
        loggingService.createLogDetailsEntry(currentDocumentId(), id.toString(), "Active", "Y", "N", logDetail,
            SHIP_LINE_MASTER_TABLE);
        log.info("Successfully deleted line with id: {}", id);
    }

    @Override
    @Transactional
    public Integer copyCharges(Long id, CopyChargesRequestDto request) {
        log.info("Copying charges from line {} to line {}", request.getSourceLinePoid(), id);

        Long groupPoid = getGroupPoid();

        // Validate target line
        lineRepository.findByLinePoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Target Line", "linePoid", id.toString()));

        // Validate source line
        lineRepository.findByLinePoidAndGroupPoid(request.getSourceLinePoid(), groupPoid)
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
        Long maxDetRowId = chargeDtlRepository.findMaxDetRowIdByLinePoid(id);
        long nextDetRowId = (maxDetRowId != null ? maxDetRowId : 0L) + 1L;
        for (ShipLineMasterChargeDtl sourceCharge : sourceCharges) {
            // Skip if charge already exists in target
            if (sourceCharge.getChargePoid() != null && existingChargePoids.contains(sourceCharge.getChargePoid())) {
                continue;
            }

            // Create new charge detail for target line
            ShipLineMasterChargeDtl newCharge = ShipLineMasterChargeDtl.builder()
                    .linePoid(id)
                    .detRowId(nextDetRowId++)
                    .chargePoid(sourceCharge.getChargePoid())
                    .lineChargeCode(sourceCharge.getLineChargeCode())
                    .lineChargeDescription(sourceCharge.getLineChargeDescription())
                    .validUntil(sourceCharge.getValidUntil())
                    .remunCommissionCharge(sourceCharge.getRemunCommissionCharge())
                    .excludedFromEdi(sourceCharge.getExcludedFromEdi())
                    .defaultPrintGroupEdi(sourceCharge.getDefaultPrintGroupEdi())
                    .wkyrptIncludeAs(sourceCharge.getWkyrptIncludeAs())
                    .build();
                    newCharge.setCreatedBy(getCurrentUser());
                    newCharge.setCreatedDate(LocalDateTime.now());

            chargeDtlRepository.save(newCharge);
                    logChildCreated(id, "Charge Details", newCharge.getDetRowId());
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
        Long maxDetRowId = chargeDtlRepository.findMaxDetRowIdByLinePoid(linePoid);
        long nextDetRowId = (maxDetRowId != null ? maxDetRowId : 0L) + 1L;

        for (ChargeDetailDto chargeDto : chargeDtos) {
            String action = resolveActionType(chargeDto.getActionType(), chargeDto.getDetRowId());
            if (isDeleteAction(action)) {
                continue;
            }

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
            charge.setDetRowId(nextDetRowId++);
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
        Map<Long, ShipLineMasterChargeDtl> existingByDetRow = chargeDtlRepository.findByLinePoidOrderByDetRowId(linePoid).stream()
                .collect(Collectors.toMap(ShipLineMasterChargeDtl::getDetRowId, detail -> detail));

        // Update or create charges
        Set<Long> chargePoids = new java.util.HashSet<>();
        Long maxDetRowId = chargeDtlRepository.findMaxDetRowIdByLinePoid(linePoid);
        long nextDetRowId = (maxDetRowId != null ? maxDetRowId : 0L) + 1L;
        for (ChargeDetailDto chargeDto : chargeDtos) {
            if (chargeDto == null) {
                continue;
            }
            String action = resolveActionType(chargeDto.getActionType(), chargeDto.getDetRowId());
            
            if (isNoChangeAction(action)) {
                continue;
            } else if (isDeleteAction(action)) {
                Long detRowId = normalizeDetRowId(chargeDto.getDetRowId());
                if (detRowId != null) {
                    ShipLineMasterChargeDtl existing = existingByDetRow.get(detRowId);
                    if (existing != null) {
                        chargeDtlRepository.deleteById(new com.asg.shipping.lineprincipalmaster.entity.ShipLineMasterChargeDtlId(linePoid, detRowId));
                        logChildDeleted(linePoid, existing);
                    }
                }
            } else {
                // Validation for non-deleted items
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

                if (isUpdateAction(action)) {
                    Long detRowId = normalizeDetRowId(chargeDto.getDetRowId());
                    if (detRowId == null) {
                        throw new ValidationException("detRowId is required for updating Charge Details");
                    }
                    ShipLineMasterChargeDtl existing = existingByDetRow.get(detRowId);
                    if (existing == null) {
                        throw new ResourceNotFoundException("Charge Detail", "detRowId", detRowId.toString());
                    }
                    ShipLineMasterChargeDtl oldCharge = new ShipLineMasterChargeDtl();
                    BeanUtils.copyProperties(existing, oldCharge);
                    mapper.updateChargeDetailFromDto(chargeDto, existing, currentUser);
                    chargeDtlRepository.save(existing);
                    logChildUpdated(linePoid, detRowId, oldCharge, existing, ShipLineMasterChargeDtl.class);
                } else if (isCreateAction(action)) {
                    ShipLineMasterChargeDtl newCharge = mapper.mapChargeDetailDtoToEntity(chargeDto, linePoid, currentUser);
                    newCharge.setDetRowId(nextDetRowId++);
                    chargeDtlRepository.save(newCharge);
                    logChildCreated(linePoid, "Charge Details", newCharge.getDetRowId());
                }
            }
        }
    }

    /**
     * Create container type details for a line
     */
    private void createContainerTypeDetails(Long linePoid, List<ContainerTypeDetailDto> containerTypeDtos, Long userPoid) {
        String currentUser = getCurrentUser();
        Set<Long> containerTypePoids = new java.util.HashSet<>();
        Long maxDetRowId = containerTypeRepository.findMaxDetRowIdByLinePoid(linePoid);
        long nextDetRowId = (maxDetRowId != null ? maxDetRowId : 0L) + 1L;

        for (ContainerTypeDetailDto containerTypeDto : containerTypeDtos) {
            String action = resolveActionType(containerTypeDto.getActionType(), containerTypeDto.getDetRowId());
            if (isDeleteAction(action)) {
                continue;
            }

            if (containerTypeDto.getContainerTypePoid() != null) {
                // Check for duplicate container type POID within the request
                if (!containerTypePoids.add(containerTypeDto.getContainerTypePoid())) {
                    throw new ValidationException("Duplicate container type POID: " + containerTypeDto.getContainerTypePoid());
                }

                // Check if container type POID already exists for this line
                if (containerTypeRepository.existsByLinePoidAndContainerTypePoid(linePoid, containerTypeDto.getContainerTypePoid())) {
                    throw new ValidationException("Container type POID " + containerTypeDto.getContainerTypePoid() + " already exists for this line");
                }
            }

            ShipLineMasterType containerType = mapper.mapContainerTypeDetailDtoToEntity(containerTypeDto, linePoid, currentUser);
            containerType.setDetRowId(nextDetRowId++);
            containerTypeRepository.save(containerType);
        }
    }

    /**
     * Update container type details for a line
     */
    private void updateContainerTypeDetails(Long linePoid, List<ContainerTypeDetailDto> containerTypeDtos, Long userPoid) {
        if (containerTypeDtos == null) {
            return;
        }

        String currentUser = getCurrentUser();
        Map<Long, ShipLineMasterType> existingByDetRow = containerTypeRepository.findByLinePoidOrderByDetRowId(linePoid).stream()
                .collect(Collectors.toMap(ShipLineMasterType::getDetRowId, detail -> detail));

        // Update or create container types
        Set<Long> containerTypePoids = new java.util.HashSet<>();
        Long maxDetRowId = containerTypeRepository.findMaxDetRowIdByLinePoid(linePoid);
        long nextDetRowId = (maxDetRowId != null ? maxDetRowId : 0L) + 1L;
        for (ContainerTypeDetailDto containerTypeDto : containerTypeDtos) {
            if (containerTypeDto == null) {
                continue;
            }
            String action = resolveActionType(containerTypeDto.getActionType(), containerTypeDto.getDetRowId());
            
            if (isNoChangeAction(action)) {
                continue;
            } else if (isDeleteAction(action)) {
                Long detRowId = normalizeDetRowId(containerTypeDto.getDetRowId());
                if (detRowId != null) {
                    ShipLineMasterType existing = existingByDetRow.get(detRowId);
                    if (existing != null) {
                        containerTypeRepository.deleteById(new ShipLineMasterTypeId(linePoid, detRowId));
                        logChildDeleted(linePoid, existing);
                    }
                }
            } else {
                // Validation for non-deleted items
                if (containerTypeDto.getContainerTypePoid() != null) {
                    if (!containerTypePoids.add(containerTypeDto.getContainerTypePoid())) {
                        throw new ValidationException("Duplicate container type POID: " + containerTypeDto.getContainerTypePoid());
                    }

                    // Check uniqueness excluding current detail row
                    if (containerTypeDto.getDetRowId() != null) {
                        if (containerTypeRepository.existsByLinePoidAndContainerTypePoidExcluding(linePoid, containerTypeDto.getContainerTypePoid(), containerTypeDto.getDetRowId())) {
                            throw new ValidationException("Container type POID " + containerTypeDto.getContainerTypePoid() + " already exists for this line");
                        }
                    } else {
                        if (containerTypeRepository.existsByLinePoidAndContainerTypePoid(linePoid, containerTypeDto.getContainerTypePoid())) {
                            throw new ValidationException("Container type POID " + containerTypeDto.getContainerTypePoid() + " already exists for this line");
                        }
                    }
                }

                if (isUpdateAction(action)) {
                    Long detRowId = normalizeDetRowId(containerTypeDto.getDetRowId());
                    if (detRowId == null) {
                        throw new ValidationException("detRowId is required for updating Container Type Details");
                    }
                    ShipLineMasterType existing = existingByDetRow.get(detRowId);
                    if (existing == null) {
                        throw new ResourceNotFoundException("Container Type Detail", "detRowId", detRowId.toString());
                    }
                    ShipLineMasterType oldContainerType = new ShipLineMasterType();
                    BeanUtils.copyProperties(existing, oldContainerType);
                    mapper.updateContainerTypeDetailFromDto(containerTypeDto, existing, currentUser);
                    containerTypeRepository.save(existing);
                    logChildUpdated(linePoid, detRowId, oldContainerType, existing, ShipLineMasterType.class);
                } else if (isCreateAction(action)) {
                    ShipLineMasterType newContainerType = mapper.mapContainerTypeDetailDtoToEntity(containerTypeDto, linePoid, currentUser);
                    newContainerType.setDetRowId(nextDetRowId++);
                    containerTypeRepository.save(newContainerType);
                    logChildCreated(linePoid, "Container Type Details", newContainerType.getDetRowId());
                }
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
            if (line.getLinePortRefno() != null || (dto.getLinePortRefnos() != null && !dto.getLinePortRefnos().isEmpty())) {
                if ((dto.getLinePortRefnos() == null || dto.getLinePortRefnos().isEmpty()) && line.getLinePortRefno() != null) {
                    dto.setLinePortRefnos(splitCodesService(line.getLinePortRefno()));
                }

                List<com.asg.shipping.common.dto.LovItem> tradelaneDets = new java.util.ArrayList<>();
                if (dto.getLinePortRefnos() != null) {
                    for (String code : dto.getLinePortRefnos()) {
                        try {
                            com.asg.shipping.common.dto.LovItem item = lovService.getLovItemByCode(code.trim(), "TRADELANE_MASTER", groupPoid, companyPoid, userPoid);
                            if (item != null) {
                                tradelaneDets.add(item);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to fetch TRADELANE LOV item for code: {}", code, e);
                        }
                    }
                }
                dto.setTradelaneDets(tradelaneDets);
                dto.setTradelaneDet(tradelaneDets);
            }
            if (line.getBillTo() != null) {
                dto.setBillToDet(lovService.getLovItemByCode(line.getBillTo(), "PRINCIPAL_MASTER_FOR_PDA", groupPoid,
                        companyPoid, userPoid));
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

        // Enrich container type details with LOV data
        if (dto.getContainerTypes() != null) {
            for (ContainerTypeDetailDto containerType : dto.getContainerTypes()) {
                try {
                    if (containerType.getContainerTypePoid() != null) {
                        containerType.setContainerTypeDet(lovService.getLovItemByPoid(containerType.getContainerTypePoid(), "LINE_CONTAINER_TYPE_MASTER", groupPoid, companyPoid, userPoid));
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch LOV data for container type", e);
                }
            }
        }

        // Enrich user role details with LOV data
        if (dto.getUserRoles() != null) {
            for (UserRoleDetailDto userRole : dto.getUserRoles()) {
                try {
                    if (userRole.getUserRolePoid() != null) {
                        userRole.setUserRoleDet(lovService.getLovItemByPoid(userRole.getUserRolePoid(), "USER_ROLES", groupPoid, companyPoid, userPoid));
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch LOV data for user role", e);
                }
            }
        }

        // Enrich PIC details with LOV data
        if (dto.getPicDetails() != null) {
            for (PicDetailDto pic : dto.getPicDetails()) {
                try {
                    if (pic.getDepartmentPoid() != null) {
                        pic.setDepartmentDet(lovService.getLovItemByPoid(pic.getDepartmentPoid(), "LINE_PIC_DEPARTMENT", groupPoid, companyPoid, userPoid));
                    }
                    if (pic.getHandledUserPoid() != null) {
                        pic.setHandledUserDet(lovService.getLovItemByPoid(pic.getHandledUserPoid(), "LINE_PIC_USER", groupPoid, companyPoid, userPoid));
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch LOV data for PIC detail", e);
                }
            }
        }
    }

    private void createUserRoleDetails(Long linePoid, List<UserRoleDetailDto> userRoleDtos, Long userPoid) {
        String currentUser = getCurrentUser();
        Set<Long> userRolePoids = new java.util.HashSet<>();
        Long maxDetRowId = userRoleDtlRepository.findMaxDetRowIdByLinePoid(linePoid);
        long nextDetRowId = (maxDetRowId != null ? maxDetRowId : 0L) + 1L;

        for (UserRoleDetailDto dto : userRoleDtos) {
            String action = resolveActionType(dto.getActionType(), dto.getDetRowId());
            if (isDeleteAction(action)) {
                continue;
            }

            if (dto.getUserRolePoid() != null) {
                if (!userRolePoids.add(dto.getUserRolePoid())) {
                    throw new ValidationException("Duplicate user role POID: " + dto.getUserRolePoid());
                }
                if (userRoleDtlRepository.existsByLinePoidAndUserRolePoid(linePoid, dto.getUserRolePoid())) {
                    throw new ValidationException("User role POID " + dto.getUserRolePoid() + " already exists for this line");
                }
            }
            ShipLineMasterUserRoleDtl entity = mapper.mapUserRoleDetailDtoToEntity(dto, linePoid, currentUser);
            entity.setDetRowId(nextDetRowId++);
            userRoleDtlRepository.save(entity);
        }
    }

    private void updateUserRoleDetails(Long linePoid, List<UserRoleDetailDto> userRoleDtos, Long userPoid) {
        if (userRoleDtos == null) return;
        String currentUser = getCurrentUser();
        Map<Long, ShipLineMasterUserRoleDtl> existingByDetRow = userRoleDtlRepository.findByLinePoidOrderByDetRowId(linePoid).stream()
                .collect(Collectors.toMap(ShipLineMasterUserRoleDtl::getDetRowId, detail -> detail));

        // Update or create user roles
        Set<Long> userRolePoids = new java.util.HashSet<>();
        Long maxDetRowId = userRoleDtlRepository.findMaxDetRowIdByLinePoid(linePoid);
        long nextDetRowId = (maxDetRowId != null ? maxDetRowId : 0L) + 1L;
        for (UserRoleDetailDto dto : userRoleDtos) {
            if (dto == null) {
                continue;
            }
            String action = resolveActionType(dto.getActionType(), dto.getDetRowId());
            
            if (isNoChangeAction(action)) {
                continue;
            } else if (isDeleteAction(action)) {
                Long detRowId = normalizeDetRowId(dto.getDetRowId());
                if (detRowId != null) {
                    ShipLineMasterUserRoleDtl existing = existingByDetRow.get(detRowId);
                    if (existing != null) {
                        userRoleDtlRepository.deleteById(new ShipLineMasterUserRoleDtlId(linePoid, detRowId));
                        logChildDeleted(linePoid, existing);
                    }
                }
            } else {
                // Validation for non-deleted items
                if (dto.getUserRolePoid() != null) {
                    if (!userRolePoids.add(dto.getUserRolePoid())) {
                        throw new ValidationException("Duplicate user role POID: " + dto.getUserRolePoid());
                    }
                    if (dto.getDetRowId() != null) {
                        if (userRoleDtlRepository.existsByLinePoidAndUserRolePoidExcluding(linePoid, dto.getUserRolePoid(), dto.getDetRowId())) {
                            throw new ValidationException("User role POID " + dto.getUserRolePoid() + " already exists for this line");
                        }
                    } else {
                        if (userRoleDtlRepository.existsByLinePoidAndUserRolePoid(linePoid, dto.getUserRolePoid())) {
                            throw new ValidationException("User role POID " + dto.getUserRolePoid() + " already exists for this line");
                        }
                    }
                }

                if (isUpdateAction(action)) {
                    Long detRowId = normalizeDetRowId(dto.getDetRowId());
                    if (detRowId == null) {
                        throw new ValidationException("detRowId is required for updating User Role Details");
                    }
                    ShipLineMasterUserRoleDtl entity = existingByDetRow.get(detRowId);
                    if (entity == null) {
                        throw new ResourceNotFoundException("User Role Detail", "detRowId", detRowId.toString());
                    }
                    ShipLineMasterUserRoleDtl oldEntity = new ShipLineMasterUserRoleDtl();
                    BeanUtils.copyProperties(entity, oldEntity);
                    mapper.updateUserRoleDetailFromDto(dto, entity, currentUser);
                    userRoleDtlRepository.save(entity);
                    logChildUpdated(linePoid, detRowId, oldEntity, entity, ShipLineMasterUserRoleDtl.class);
                } else if (isCreateAction(action)) {
                    ShipLineMasterUserRoleDtl entity = mapper.mapUserRoleDetailDtoToEntity(dto, linePoid, currentUser);
                    entity.setDetRowId(nextDetRowId++);
                    userRoleDtlRepository.save(entity);
                    logChildCreated(linePoid, "User Role Details", entity.getDetRowId());
                }
            }
        }
    }

    private void createPicDetails(Long linePoid, List<PicDetailDto> picDtos, Long userPoid) {
        String currentUser = getCurrentUser();
        Long maxDetRowId = picDtlRepository.findMaxDetRowIdByLinePoid(linePoid);
        long nextDetRowId = (maxDetRowId != null ? maxDetRowId : 0L) + 1L;

        for (PicDetailDto dto : picDtos) {
            ShipLineMasterPicDtl entity = mapper.mapPicDetailDtoToEntity(dto, linePoid, currentUser);
            entity.setDetRowId(nextDetRowId++);
            picDtlRepository.save(entity);
        }
    }

    private void updatePicDetails(Long linePoid, List<PicDetailDto> picDtos, Long userPoid) {
        if (picDtos == null) return;
        String currentUser = getCurrentUser();
        Map<Long, ShipLineMasterPicDtl> existingByDetRow = picDtlRepository.findByLinePoidOrderByDetRowId(linePoid).stream()
                .collect(Collectors.toMap(ShipLineMasterPicDtl::getDetRowId, detail -> detail));

        Long maxDetRowId = picDtlRepository.findMaxDetRowIdByLinePoid(linePoid);
        long nextDetRowId = (maxDetRowId != null ? maxDetRowId : 0L) + 1L;
        for (PicDetailDto dto : picDtos) {
            if (dto == null) {
                continue;
            }
            String action = resolveActionType(dto.getActionType(), dto.getDetRowId());
            if (isNoChangeAction(action)) {
                continue;
            }

            if (isDeleteAction(action)) {
                Long detRowId = normalizeDetRowId(dto.getDetRowId());
                if (detRowId == null) {
                    throw new ValidationException("detRowId is required for deleting PIC Details");
                }
                ShipLineMasterPicDtl existing = existingByDetRow.get(detRowId);
                if (existing == null) {
                    throw new ResourceNotFoundException("PIC Detail", "detRowId", detRowId.toString());
                }
                picDtlRepository.deleteById(new ShipLineMasterPicDtlId(linePoid, detRowId));
                logChildDeleted(linePoid, existing);
            } else if (isUpdateAction(action)) {
                Long detRowId = normalizeDetRowId(dto.getDetRowId());
                if (detRowId == null) {
                    throw new ValidationException("detRowId is required for updating PIC Details");
                }
                ShipLineMasterPicDtl entity = existingByDetRow.get(detRowId);
                if (entity == null) {
                    throw new ResourceNotFoundException("PIC Detail", "detRowId", detRowId.toString());
                }
                ShipLineMasterPicDtl oldEntity = new ShipLineMasterPicDtl();
                BeanUtils.copyProperties(entity, oldEntity);
                mapper.updatePicDetailFromDto(dto, entity, currentUser);
                picDtlRepository.save(entity);
                logChildUpdated(linePoid, detRowId, oldEntity, entity, ShipLineMasterPicDtl.class);
            } else {
                ShipLineMasterPicDtl entity = mapper.mapPicDetailDtoToEntity(dto, linePoid, currentUser);
                entity.setDetRowId(nextDetRowId++);
                picDtlRepository.save(entity);
                logChildCreated(linePoid, "PIC Details", entity.getDetRowId());
            }
        }
    }

    private GlobalAddressMaster resolveAddressMasterForCreate(Long addressPoid, String lineName, Integer seqno, Long groupPoid) {
        if (addressPoid == null) {
            return createAddressMaster(lineName, seqno, groupPoid);
        }

        return addressMasterRepository.findByAddressMasterPoid(addressPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressPoid", addressPoid.toString()));
    }

    private GlobalAddressMaster resolveAddressMasterForUpdate(Long addressPoid, String lineName, Integer seqno, Long groupPoid) {
        if (addressPoid == null) {
            return createAddressMaster(lineName, seqno, groupPoid);
        }

        return addressMasterRepository.findByAddressMasterPoid(addressPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressPoid", addressPoid.toString()));
    }

    private GlobalAddressMaster createAddressMaster(String lineName, Integer seqno, Long groupPoid) {
        if (StringUtils.isBlank(lineName)) {
            throw new CustomException("Address Name is required for creating new address", 400);
        }
        if (addressMasterRepository.existsByAddressNameIgnoreCaseAndGroupPoid(lineName, groupPoid)) {
            throw new ResourceAlreadyExistsException("Address Name", lineName);
        }

        GlobalAddressMaster addressMaster = new GlobalAddressMaster();
        addressMaster.setAddressName(lineName);
        addressMaster.setGroupPoid(groupPoid);
        addressMaster.setSeqno(seqno);
        addressMaster.setCreatedBy(getCurrentUser());
        addressMaster.setDeleted("N");
        addressMaster.setActive("Y");
        return addressMasterRepository.save(addressMaster);
    }

    private void saveAllAddressDetails(AddressTypeMapDTO typeMap, GlobalAddressMaster master, String currentUser, String parentPoid) {
        if (typeMap == null) {
            return;
        }

        String entityId = StringUtils.isNotBlank(parentPoid) ? parentPoid : String.valueOf(master.getAddressMasterPoid());
        List<GlobalAddressDetails> existingDetails = addressDetailsRepository.findByAddressMasterPoid(master.getAddressMasterPoid());
        Map<String, GlobalAddressDetails> existingMap = existingDetails.stream()
                .collect(Collectors.toMap(detail -> String.valueOf(detail.getAddressPoid()), detail -> detail));

        List<GlobalAddressDetails> createdDetails = new ArrayList<>();
        List<String> toDelete = new ArrayList<>();

        Map<String, List<AddressDetailsDTO>> typedLists = Map.of(
                "MAIN", Optional.ofNullable(typeMap.getMain()).orElse(List.of()),
                "FINANCE", Optional.ofNullable(typeMap.getFinance()).orElse(List.of()),
                "SALES", Optional.ofNullable(typeMap.getSales()).orElse(List.of()),
                "OPERATION", Optional.ofNullable(typeMap.getOperation()).orElse(List.of()),
                "INVOICE_ADDRESS", Optional.ofNullable(typeMap.getInvoiceAddress()).orElse(List.of()),
                "DELIVERY_ORDER", Optional.ofNullable(typeMap.getDeliveryOrder()).orElse(List.of()),
                "SHIP_CHANDLING", Optional.ofNullable(typeMap.getShipChandling()).orElse(List.of()),
                "CLAIM_UAC", Optional.ofNullable(typeMap.getClaimUac()).orElse(List.of()),
                "CAN", Optional.ofNullable(typeMap.getCan()).orElse(List.of())
        );

        int counter = existingDetails.size() + 1;

        for (Map.Entry<String, List<AddressDetailsDTO>> entry : typedLists.entrySet()) {
            String type = entry.getKey();
            for (AddressDetailsDTO dto : entry.getValue()) {
                String actionType = StringUtils.defaultIfBlank(dto.getActionType(), "isCreated");

                if ("isDeleted".equalsIgnoreCase(actionType)) {
                    if (StringUtils.isNotBlank(dto.getAddressPoid())) {
                        GlobalAddressDetails detail = resolveExistingAddressDetail(existingMap, dto.getAddressPoid());
                        toDelete.add(dto.getAddressPoid());
                        if (detail != null) {
                            String logDetail = String.format("Row Deleted on Address Detail with addressPoid: %s", detail.getAddressPoid());
                            loggingService.createLogSummaryEntry(UserContext.getDocumentId(), entityId, logDetail);
                        }
                    }
                } else if ("isUpdated".equalsIgnoreCase(actionType)) {
                    if (StringUtils.isNotBlank(dto.getAddressPoid())) {
                        GlobalAddressDetails detail = resolveExistingAddressDetail(existingMap, dto.getAddressPoid());
                        GlobalAddressDetails oldDetail = null;
                        GlobalAddressDetails updatedDetail = null;
                        if (detail != null) {
                            oldDetail = new GlobalAddressDetails();
                            BeanUtils.copyProperties(detail, oldDetail);
                            updatedDetail = new GlobalAddressDetails();
                            BeanUtils.copyProperties(detail, updatedDetail);
                            updateAddressDetail(updatedDetail, dto, currentUser);
                        }
                        updateAddressDetailByPoid(dto, master, type, currentUser);
                        if (detail != null) {
                            String logDetail = String.format("KeyId = ADDRESS_MASTER_POID %s: ADDRESS_POID %s",
                                    master.getAddressMasterPoid(), detail.getAddressPoid());
                            loggingService.createLog(oldDetail, updatedDetail, GlobalAddressDetails.class,
                                    UserContext.getDocumentId(), entityId, logDetail);
                        }
                    }
                } else if ("isCreated".equalsIgnoreCase(actionType)) {
                    GlobalAddressDetails detail = buildAddressDetail(dto, master, type, counter++, currentUser);
                    createdDetails.add(detail);
                }
            }
        }

        if (!createdDetails.isEmpty()) {
            insertCreatedAddressDetails(createdDetails);
            createdDetails.forEach(detail -> {
                String logDetail = String.format("Row Created on Address Detail with addressPoid: %s", detail.getAddressPoid());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), entityId, logDetail);
            });
        }
        if (!toDelete.isEmpty()) {
            deleteAddressDetails(master.getAddressMasterPoid(), toDelete);
        }
    }

    private GlobalAddressDetails buildAddressDetail(AddressDetailsDTO dto, GlobalAddressMaster master, String type, int counter, String currentUser) {
        GlobalAddressDetails detail = new GlobalAddressDetails();
        detail.setAddressPoid(resolveAddressDetailPoid(dto.getAddressPoid(), counter));
        detail.setAddressMasterPoid(master.getAddressMasterPoid());
        detail.setAddressType(type);
        detail.setCreatedBy(currentUser);
        applyAddressDetailFields(detail, dto);
        return detail;
    }

    private void refreshPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    private void insertCreatedAddressDetails(List<GlobalAddressDetails> details) {
        String sql = """
                INSERT INTO GLOBAL_ADDRESS_DETAILS (
                    ADDRESS_POID, ADDRESS_MASTER_POID, ADDRESS_TYPE, OFF_TEL1, OFF_TEL2,
                    CONTACT_PERSON, DESIGNATION, MOBILE, FAX, EMAIL1, EMAIL2, WEBSITE,
                    PO_BOX, OFF_NO, BLDG, ROAD, AREA_CITY, STATE, COUNTRY_POID,
                    LAND_MARK, CREATED_BY, CREATED_DATE, VERIFIED, VERIFIED_BY,
                    VERIFIED_DATE, CITY, WHATSAPP_NO, LINKEDIN, INSTAGRAM, FACEBOOK
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, SYSDATE, ?, ?,
                    ?, ?, ?, ?, ?, ?
                )
                """;

        jdbcTemplate.batchUpdate(sql, details, details.size(), (ps, detail) -> {
            ps.setObject(1, detail.getAddressPoid());
            ps.setObject(2, detail.getAddressMasterPoid());
            ps.setString(3, detail.getAddressType());
            ps.setString(4, detail.getOffTel1());
            ps.setString(5, detail.getOffTel2());
            ps.setString(6, detail.getContactPerson());
            ps.setString(7, detail.getDesignation());
            ps.setString(8, detail.getMobile());
            ps.setString(9, detail.getFax());
            ps.setString(10, detail.getEmail1());
            ps.setString(11, detail.getEmail2());
            ps.setString(12, detail.getWebsite());
            ps.setString(13, detail.getPoBox());
            ps.setString(14, detail.getOffNo());
            ps.setString(15, detail.getBldg());
            ps.setString(16, detail.getRoad());
            ps.setString(17, detail.getAreaCity());
            ps.setString(18, detail.getState());
            ps.setObject(19, detail.getCountryPoid());
            ps.setString(20, detail.getLandMark());
            ps.setString(21, detail.getCreatedBy());
            ps.setString(22, detail.getVerified());
            ps.setString(23, detail.getVerifiedBy());
            ps.setTimestamp(24, detail.getVerifiedDate());
            ps.setString(25, detail.getCity());
            ps.setString(26, detail.getWhatsappNo());
            ps.setString(27, detail.getLinkedin());
            ps.setString(28, detail.getInstagram());
            ps.setString(29, detail.getFacebook());
        });
    }

    private void updateAddressDetailByPoid(AddressDetailsDTO dto, GlobalAddressMaster master, String type, String currentUser) {
        GlobalAddressDetails detail = buildAddressDetailForSql(dto, master, type, currentUser);
        String sql = """
                UPDATE GLOBAL_ADDRESS_DETAILS
                   SET ADDRESS_TYPE = ?,
                       OFF_TEL1 = ?,
                       OFF_TEL2 = ?,
                       CONTACT_PERSON = ?,
                       DESIGNATION = ?,
                       MOBILE = ?,
                       FAX = ?,
                       EMAIL1 = ?,
                       EMAIL2 = ?,
                       WEBSITE = ?,
                       PO_BOX = ?,
                       OFF_NO = ?,
                       BLDG = ?,
                       ROAD = ?,
                       AREA_CITY = ?,
                       STATE = ?,
                       COUNTRY_POID = ?,
                       LAND_MARK = ?,
                       LASTMODIFIED_BY = ?,
                       LASTMODIFIED_DATE = SYSDATE,
                       VERIFIED = ?,
                       VERIFIED_BY = ?,
                       VERIFIED_DATE = ?,
                       CITY = ?,
                       WHATSAPP_NO = ?,
                       LINKEDIN = ?,
                       INSTAGRAM = ?,
                       FACEBOOK = ?
                 WHERE ADDRESS_MASTER_POID = ?
                   AND ADDRESS_POID = ?
                """;

        Object[] args = {
                detail.getAddressType(),
                detail.getOffTel1(),
                detail.getOffTel2(),
                detail.getContactPerson(),
                detail.getDesignation(),
                detail.getMobile(),
                detail.getFax(),
                detail.getEmail1(),
                detail.getEmail2(),
                detail.getWebsite(),
                detail.getPoBox(),
                detail.getOffNo(),
                detail.getBldg(),
                detail.getRoad(),
                detail.getAreaCity(),
                detail.getState(),
                detail.getCountryPoid(),
                detail.getLandMark(),
                currentUser,
                detail.getVerified(),
                detail.getVerifiedBy(),
                detail.getVerifiedDate(),
                detail.getCity(),
                detail.getWhatsappNo(),
                detail.getLinkedin(),
                detail.getInstagram(),
                detail.getFacebook(),
                master.getAddressMasterPoid(),
                toAddressPoidNumber(dto.getAddressPoid())
        };

        int updatedRows = jdbcTemplate.update(sql, args);
        if (updatedRows == 0) {
            args[args.length - 1] = toNormalizedAddressPoidNumber(dto.getAddressPoid());
            jdbcTemplate.update(sql, args);
        }
    }

    private GlobalAddressDetails buildAddressDetailForSql(AddressDetailsDTO dto, GlobalAddressMaster master, String type, String currentUser) {
        GlobalAddressDetails detail = new GlobalAddressDetails();
        detail.setAddressMasterPoid(master.getAddressMasterPoid());
        detail.setAddressType(type);
        detail.setLastmodifiedBy(currentUser);
        applyAddressDetailFields(detail, dto);
        return detail;
    }

    private void deleteAddressDetails(Long addressMasterPoid, List<String> addressPoids) {
        String sql = "DELETE FROM GLOBAL_ADDRESS_DETAILS WHERE ADDRESS_MASTER_POID = ? AND ADDRESS_POID = ?";
        jdbcTemplate.batchUpdate(sql, addressPoids, addressPoids.size(), (ps, addressPoid) -> {
            ps.setObject(1, addressMasterPoid);
            ps.setObject(2, toAddressPoidNumber(addressPoid));
        });
        jdbcTemplate.batchUpdate(sql, addressPoids, addressPoids.size(), (ps, addressPoid) -> {
            ps.setObject(1, addressMasterPoid);
            ps.setObject(2, toNormalizedAddressPoidNumber(addressPoid));
        });
    }

    private GlobalAddressDetails resolveExistingAddressDetail(Map<String, GlobalAddressDetails> existingMap, String addressPoid) {
        GlobalAddressDetails exact = existingMap.get(addressPoid);
        if (exact != null) {
            return exact;
        }
        return existingMap.get(normalizeAddressPoidKey(addressPoid));
    }

    private String normalizeAddressPoidKey(String addressPoid) {
        if (StringUtils.isBlank(addressPoid)) {
            return addressPoid;
        }
        BigDecimal numericPoid = toAddressPoidNumber(addressPoid);
        try {
            return numericPoid.toBigIntegerExact().toString();
        } catch (ArithmeticException ignored) {
            return numericPoid.toBigInteger().toString();
        }
    }

    private BigDecimal toAddressPoidNumber(String addressPoid) {
        return new BigDecimal(addressPoid.trim());
    }

    private BigDecimal toNormalizedAddressPoidNumber(String addressPoid) {
        return new BigDecimal(normalizeAddressPoidKey(addressPoid));
    }

    private Long resolveAddressDetailPoid(String addressPoid, int counter) {
        if (StringUtils.isNotBlank(addressPoid)) {
            return toAddressPoidNumber(addressPoid).longValue();
        }
        return System.currentTimeMillis() + counter;
    }

    private void updateAddressDetail(GlobalAddressDetails entity, AddressDetailsDTO dto, String currentUser) {
        entity.setLastmodifiedBy(currentUser);
        applyAddressDetailFields(entity, dto);
    }

    private void applyAddressDetailFields(GlobalAddressDetails entity, AddressDetailsDTO dto) {
        entity.setContactPerson(dto.getContactPerson());
        entity.setDesignation(dto.getDesignation());
        entity.setOffTel1(dto.getOffTel1());
        entity.setOffTel2(dto.getOffTel2());
        entity.setMobile(dto.getMobile());
        entity.setFax(dto.getFax());

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            entity.setEmail1(dto.getEmail().get(0));
            entity.setEmail2(dto.getEmail().size() > 1 ? dto.getEmail().get(1) : null);
        } else {
            entity.setEmail1(null);
            entity.setEmail2(null);
        }

        entity.setWebsite(dto.getWebsite());
        entity.setPoBox(dto.getPoBox());
        entity.setOffNo(dto.getOffNo());
        entity.setBldg(dto.getBldg());
        entity.setRoad(dto.getRoad());

        String area = dto.getArea();
        String city = dto.getCity();
        if ((city == null || city.isBlank()) && area != null && area.contains(",")) {
            String[] parts = area.split(",", 2);
            area = parts[0].trim();
            city = parts.length > 1 ? parts[1].trim() : null;
        }
        entity.setAreaCity(area);
        entity.setCity(city);

        entity.setState(dto.getState() != null && !dto.getState().isEmpty() ? String.join(",", dto.getState()) : null);
        entity.setLandMark(dto.getLandMark());
        entity.setVerified(dto.getVerified());
        entity.setVerifiedBy(dto.getVerifiedBy());
        entity.setVerifiedDate(dto.getVerifiedDate() != null ? Timestamp.valueOf(dto.getVerifiedDate().atStartOfDay()) : null);
        entity.setWhatsappNo(dto.getWhatsappNo());
        entity.setLinkedin(dto.getLinkedIn());
        entity.setInstagram(dto.getInstagram());
        entity.setFacebook(dto.getFacebook());
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

    private List<String> splitCodesService(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    private String currentDocumentId() {
        return UserContext.getDocumentId() != null ? UserContext.getDocumentId() : DOC_ID;
    }

    private String resolveActionType(String actionType, Long detRowId) {
        if (actionType == null || actionType.trim().isEmpty()) {
            return normalizeDetRowId(detRowId) == null ? "ISCREATED" : "ISUPDATED";
        }
        return actionType.trim().replace("_", "").replace(" ", "").toUpperCase();
    }

    private Long normalizeDetRowId(Long detRowId) {
        if (detRowId == null || detRowId == 0L) {
            return null;
        }
        return detRowId;
    }

    private boolean isUpdateAction(String action) {
        return "UPDATE".equals(action) || "ISUPDATED".equals(action);
    }

    private boolean isDeleteAction(String action) {
        return "DELETE".equals(action) || "ISDELETED".equals(action);
    }

    private boolean isCreateAction(String action) {
        return "CREATE".equals(action) || "ISCREATED".equals(action);
    }

    private boolean isNoChangeAction(String action) {
        return "NOCHANGE".equals(action) || "NOCHANGES".equals(action) || "UNCHANGED".equals(action);
    }

    private void logChildCreated(Long linePoid, String sectionName, Long detRowId) {
        loggingService.createLogSummaryEntry(currentDocumentId(), linePoid.toString(),
                String.format("Row Created on %s with detRowId: %s", sectionName, detRowId));
    }

    private <T> void logChildUpdated(Long linePoid, Long detRowId, T oldEntity, T newEntity, Class<T> entityClass) {
        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, currentDocumentId(), linePoid.toString());
        String logDetail = String.format("KeyId = %s:%s DET_ROW_ID:%s", LINE_POID_COLUMN, linePoid, detRowId);
        loggingService.createLog(oldEntity, newEntity, entityClass, currentDocumentId(), linePoid.toString(), logDetail);
    }

    private <T> void logChildDeleted(Long linePoid, T entity) {
        loggingService.logDelete(entity, currentDocumentId(), linePoid.toString());
    }
}

