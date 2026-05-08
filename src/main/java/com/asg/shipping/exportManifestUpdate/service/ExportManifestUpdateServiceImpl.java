package com.asg.shipping.exportManifestUpdate.service;

import javax.sql.DataSource;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.enums.LogDetailsEnum;
import org.springframework.beans.BeanUtils;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.common.service.LovService;
import com.asg.shipping.exportManifestUpdate.dto.*;
import com.asg.shipping.exportManifestUpdate.entity.*;
import com.asg.shipping.exportManifestUpdate.mapper.ExportManifestUpdateMapper;
import com.asg.shipping.exportManifestUpdate.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;
import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;
import static com.asg.common.lib.security.util.UserContext.getUserPoid;

/**
 * Service implementation for Export Manifest BL operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExportManifestUpdateServiceImpl implements ExportManifestBlService {

    private static final String DOC_ID = "100-352";

    private final ExportShipBlManifestHdrRepository hdrRepository;
    private final ExportShipBlManifestGeneralDtlRepository generalDtlRepository;
    private final ExportShipBlManifestContainerDtlRepository containerDtlRepository;
    private final ExportShipBlManifestCargoDtlRepository cargoDtlRepository;
    private final ExportShipBlManifestChargesDtlRepository chargesDtlRepository;
    private final ExportManifestBlCustomRepository customRepository;
    private final ExportManifestUpdateMapper mapper;
    private final DocumentSearchService documentSearchService;
    private final LovService lovService;
    private final LoggingService loggingService;
    private final PrintService printService;
    private final DataSource dataSource;

    // ========== Header Operations ==========

    @Override
    @Transactional(readOnly = true)
    public ExportManifestBlResponse getExportBlById(Long transactionPoid) {
        log.info("Getting Export BL by ID: {}", transactionPoid);
        // Controller layer also logs VIEW, but ensure service-level audit exists
        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, DOC_ID, transactionPoid.toString());
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        
        // Query filters: DELETED = 'N' (NULL excluded), BL_TYPE = 'EXPORT', GROUP_POID and COMPANY_POID match
        ExportShipBlManifestHdr entity = hdrRepository
                .findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new RuntimeException("Export BL not found with ID: " + transactionPoid));
        
        ExportManifestBlResponse response = mapper.mapToResponse(entity);
        
        // Enrich with LOV data
        enrichHeaderWithLovData(response, entity);
        
        // Populate nested detail collections
        response.setGeneralCargoDetails(getGeneralCargoDetails(transactionPoid));
        response.setContainerDetails(getContainerDetails(transactionPoid));
        response.setCargoDescription(getCargoDescription(transactionPoid));
        response.setCargoMarks(getCargoMarks(transactionPoid));
        
        // Get charge details with totals
        Map<String, Object> chargeData = getChargeDetails(transactionPoid);
        @SuppressWarnings("unchecked")
        List<ChargeDetailDto> chargeDetails = (List<ChargeDetailDto>) chargeData.get("data");
        response.setChargeDetails(chargeDetails);
        response.setChargeTotals((ChargeTotalsDto) chargeData.get("totals"));
        
        log.info("Returning Export BL with {} general cargo, {} container, {} charge details", 
                response.getGeneralCargoDetails() != null ? response.getGeneralCargoDetails().size() : 0,
                response.getContainerDetails() != null ? response.getContainerDetails().size() : 0,
                response.getChargeDetails() != null ? response.getChargeDetails().size() : 0);
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchExportBls(FilterRequestDto filters, Pageable pageable) {
        log.info("Searching Export BLs with filters: {}", filters);
        
        // Resolve filter components from FilterRequestDto
        String operator = documentSearchService.resolveOperator(filters);
        String isDeleted = documentSearchService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentSearchService.resolveFilters(filters);
        
        // Call DocumentSearchService.search with docId "100-352"
        // This will use the document configuration from the database (SQL query, display fields, etc.)
        RawSearchResult raw = documentSearchService.search(
                DOC_ID,
                filterList,
                operator,
                pageable,
                isDeleted,
                "BL_NUMBER",           // label field for display
                "TRANSACTION_POID"      // value field (primary key)
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
    public ExportManifestBlResponse createExportBl(ExportManifestBlRequest request) {
        log.info("Creating Export BL with BL Number: {}", request.getBlNumber());
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        String userId = getCurrentUser();
        
        // Validate required fields
        if (request.getVoyageTransactionPoid() == null) {
            throw new RuntimeException("Voyage Transaction POID is required");
        }
        if (request.getSalesmanPoid() == null) {
            throw new RuntimeException("Salesman POID is required");
        }
        
        // Validate BL number uniqueness
        if (request.getBlNumber() != null && !request.getBlNumber().trim().isEmpty()) {
            String status = customRepository.validateBlNumberDuplicate(
                    request.getBlNumber().trim(), "NEWRECORD", "INSERTING");
            if (status == null || !status.startsWith("SUCCESS")) {
                throw new RuntimeException("Export BL number already exists: " + request.getBlNumber());
            }
        }
        
        // Create entity
        ExportShipBlManifestHdr entity = ExportShipBlManifestHdr.builder()
                .groupPoid(groupPoid)
                .companyPoid(companyPoid)
                .transactionDate(LocalDate.now())
                .blType("EXPORT")
                .blStatus("OPEN")
                .blOrginalPrint("N")
                .releasedStatus("NONE")
                .holdCanDo("NONE")
                .holdReason("5")
                .canSentQueue("N")
                .deleted("N")
                .blIssueType(request.getBlIssueType() != null ? request.getBlIssueType() : "1")
                .bookedByPp(request.getBookedByPp() != null ? request.getBookedByPp() : "N")
                .allInOneFreight(request.getAllInOneFreight() != null ? request.getAllInOneFreight() : "Y")
                .build();
        
        // Map request fields to entity
        mapper.mapRequestToEntity(request, entity, groupPoid, companyPoid, userId);
        
        // Generate docRef if not provided
        if (entity.getDocRef() == null || entity.getDocRef().trim().isEmpty()) {
            entity.setDocRef(entity.getBlNumber() != null ? entity.getBlNumber() : "BL-" + System.currentTimeMillis());
        }
        
        // Generate uniqueBlno
        if (entity.getBlNumber() != null) {
            entity.setUniqueBlno(entity.getBlNumber());
        }
        
        // Save entity
        ExportShipBlManifestHdr saved = hdrRepository.save(entity);
        
        // Persist nested details if provided
        if (request.getGeneralCargoDetails() != null && !request.getGeneralCargoDetails().isEmpty()) {
            for (GeneralCargoDetailDto dto : request.getGeneralCargoDetails()) {
                Long detRowId = generalDtlRepository.getNextDetRowId(saved.getTransactionPoid());
                ExportShipBlManifestGeneralDtl detailEntity = mapper.mapGeneralCargoToEntity(dto, saved.getTransactionPoid(), detRowId, userId);
                generalDtlRepository.save(detailEntity);
            }
        }
        
        if (request.getContainerDetails() != null && !request.getContainerDetails().isEmpty()) {
            for (ContainerDetailDto dto : request.getContainerDetails()) {
                Long detRowId = containerDtlRepository.getNextDetRowId(saved.getTransactionPoid());
                ExportShipBlManifestContainerDtl detailEntity = mapper.mapContainerToEntity(dto, saved.getTransactionPoid(), detRowId, userId);
                containerDtlRepository.save(detailEntity);
            }
        }
        
        if (request.getCargoDescription() != null && !request.getCargoDescription().isEmpty()) {
            for (CargoDescriptionDto dto : request.getCargoDescription()) {
                Long detRowId = cargoDtlRepository.getNextDetRowId(saved.getTransactionPoid(), "CARGO");
                ExportShipBlManifestCargoDtl detailEntity = mapper.mapCargoDescriptionToEntity(dto, saved.getTransactionPoid(), detRowId, userId);
                cargoDtlRepository.save(detailEntity);
            }
        }
        
        if (request.getCargoMarks() != null && !request.getCargoMarks().isEmpty()) {
            for (CargoMarksDto dto : request.getCargoMarks()) {
                Long detRowId = cargoDtlRepository.getNextDetRowId(saved.getTransactionPoid(), "MARKS");
                ExportShipBlManifestCargoDtl detailEntity = mapper.mapCargoMarksToEntity(dto, saved.getTransactionPoid(), detRowId, userId);
                cargoDtlRepository.save(detailEntity);
            }
        }
        
        if (request.getChargeDetails() != null && !request.getChargeDetails().isEmpty()) {
            for (ChargeDetailDto dto : request.getChargeDetails()) {
                Long detRowId = chargesDtlRepository.getNextDetRowId(saved.getTransactionPoid());
                ExportShipBlManifestChargesDtl detailEntity = mapper.mapChargeToEntity(dto, saved.getTransactionPoid(), detRowId, userId);
                chargesDtlRepository.save(detailEntity);
            }
        }
        
        // Recalculate header totals
        recalculateHeaderTotals(saved.getTransactionPoid());
        
        // Log header creation (summary)
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, DOC_ID, saved.getTransactionPoid().toString());
        
        log.info("Successfully created Export BL with ID: {}", saved.getTransactionPoid());
        ExportManifestBlResponse response = mapper.mapToResponse(saved);
        
        // Enrich with LOV data
        enrichHeaderWithLovData(response, saved);
        
        return response;
    }

    @Override
    public ExportManifestBlResponse updateExportBl(Long transactionPoid, ExportManifestBlRequest request) {
        log.info("Updating Export BL with ID: {}", transactionPoid);
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        String userId = getCurrentUser();
        
        // Get existing entity (DELETED = 'N', BL_TYPE = 'EXPORT')
        ExportShipBlManifestHdr entity = hdrRepository
                .findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new RuntimeException("Export BL not found with ID: " + transactionPoid));

        // Keep a snapshot for change logging
        ExportShipBlManifestHdr oldEntity = new ExportShipBlManifestHdr();
        BeanUtils.copyProperties(entity, oldEntity);
        
        // Store old values for change logging
        String oldBlNumber = entity.getBlNumber();
        String oldCargoType = entity.getCargoType();
        
        // Validate BL number uniqueness if changed
        if (request.getBlNumber() != null && !request.getBlNumber().trim().equals(entity.getBlNumber())) {
            String status = customRepository.validateBlNumberDuplicate(
                    request.getBlNumber().trim(), entity.getBlNumber(), "UPDATING");
            if (status == null || !status.startsWith("SUCCESS")) {
                throw new RuntimeException("Export BL number already exists: " + request.getBlNumber());
            }
        }
        
        // Map request fields to entity
        mapper.mapRequestToEntity(request, entity, groupPoid, companyPoid, userId);
        
        // Update cargo type in container inventory if changed
        if (request.getCargoType() != null && !request.getCargoType().equals(oldCargoType)) {
            // This would typically be handled by the trigger, but we can also do it here
            // The trigger SHIP_BL_MANIFEST_HDR_TRG handles this
        }
        
        // Clean EDI fields if BL type is not EXPORT
        if (entity.getBlType() != null && !entity.getBlType().equals("EXPORT")) {
            // Trigger handles this, but we can also do it here
            if (entity.getShipperEdiName() != null) {
                entity.setShipperEdiName(entity.getShipperEdiName().replace("\n", "").replace("\r", "").trim());
            }
            // Similar for other EDI fields
        }
        
        // Save entity
        ExportShipBlManifestHdr saved = hdrRepository.save(entity);
        
        // Persist nested details if provided
        if (request.getGeneralCargoDetails() != null && !request.getGeneralCargoDetails().isEmpty()) {
            processBulkSaveGeneralCargoDetails(saved.getTransactionPoid(), request.getGeneralCargoDetails(), userId);
        }
        
        if (request.getContainerDetails() != null && !request.getContainerDetails().isEmpty()) {
            processBulkSaveContainerDetails(saved.getTransactionPoid(), request.getContainerDetails(), userId);
        }
        
        if (request.getCargoDescription() != null && !request.getCargoDescription().isEmpty()) {
            processBulkSaveCargoDescription(saved.getTransactionPoid(), request.getCargoDescription(), userId);
        }
        
        if (request.getCargoMarks() != null && !request.getCargoMarks().isEmpty()) {
            processBulkSaveCargoMarks(saved.getTransactionPoid(), request.getCargoMarks(), userId);
        }
        
        if (request.getChargeDetails() != null && !request.getChargeDetails().isEmpty()) {
            processBulkSaveChargeDetails(saved.getTransactionPoid(), request.getChargeDetails(), userId);
        }
        
        // Recalculate header totals
        recalculateHeaderTotals(saved.getTransactionPoid());
        
        // Log header update (summary + detailed field changes)
        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, DOC_ID, saved.getTransactionPoid().toString());
        loggingService.logChanges(
            oldEntity,
            saved,
            ExportShipBlManifestHdr.class,
            DOC_ID,
            saved.getTransactionPoid().toString(),
            LogDetailsEnum.MODIFIED,
            "TRANSACTION_POID"
        );
        
        log.info("Successfully updated Export BL with ID: {}", saved.getTransactionPoid());
        ExportManifestBlResponse response = mapper.mapToResponse(saved);
        
        // Enrich with LOV data
        enrichHeaderWithLovData(response, saved);
        
        return response;
    }

    @Override
    public void deleteExportBl(Long transactionPoid) {
        log.info("Deleting Export BL with ID: {}", transactionPoid);
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        String userId = getCurrentUser();
        
        ExportShipBlManifestHdr entity = hdrRepository
                .findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new RuntimeException("Export BL not found with ID: " + transactionPoid));
        
        // Store BL number for logging
        String blNumber = entity.getBlNumber();
        
        // Soft delete
        entity.setDeleted("Y");

        hdrRepository.save(entity);
        
        // Log header deletion (summary)
        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, DOC_ID, transactionPoid.toString());
        
        log.info("Successfully deleted Export BL with ID: {}", transactionPoid);
    }

    // ========== General Cargo Details Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<GeneralCargoDetailDto> getGeneralCargoDetails(Long transactionPoid) {
        log.info("Getting general cargo details for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        
        List<ExportShipBlManifestGeneralDtl> entities = generalDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<GeneralCargoDetailDto> dtos = mapper.mapGeneralCargoListToDto(entities);
        
        // Enrich with LOV data
        enrichGeneralCargoDetailsWithLovData(dtos);
        
        return dtos;
    }

    @Override
    public List<GeneralCargoDetailDto> bulkSaveGeneralCargoDetails(Long transactionPoid, BulkSaveRequest<GeneralCargoDetailDto> request) {
        log.info("Bulk saving general cargo details for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        String userId = getCurrentUser();
        
        // Handle backward-compatible deleteIds (legacy behavior)
        if (request.getDeleteIds() != null && !request.getDeleteIds().isEmpty()) {
            generalDtlRepository.deleteByTransactionPoidAndDetRowIds(transactionPoid, request.getDeleteIds());
            log.debug("Deleted {} general cargo details via deleteIds", request.getDeleteIds().size());
        }
        
        // Process details with actionType support
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            processBulkSaveGeneralCargoDetails(transactionPoid, request.getDetails(), userId);
            log.debug("Processed {} general cargo details with actionType support", request.getDetails().size());
        }
        
        // Recalculate header totals
        recalculateHeaderTotals(transactionPoid);
        
        // Return all details
        return getGeneralCargoDetails(transactionPoid);
    }

    // ========== Container Details Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<ContainerDetailDto> getContainerDetails(Long transactionPoid) {
        log.info("Getting container details for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        
        List<ExportShipBlManifestContainerDtl> entities = containerDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<ContainerDetailDto> dtos = mapper.mapContainerListToDto(entities);
        
        // Enrich with LOV data
        enrichContainerDetailsWithLovData(dtos);
        
        return dtos;
    }

    @Override
    public List<ContainerDetailDto> bulkSaveContainerDetails(Long transactionPoid, BulkSaveRequest<ContainerDetailDto> request) {
        log.info("Bulk saving container details for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        String userId = getCurrentUser();
        
        // Handle backward-compatible deleteIds (legacy behavior)
        if (request.getDeleteIds() != null && !request.getDeleteIds().isEmpty()) {
            containerDtlRepository.deleteByTransactionPoidAndDetRowIds(transactionPoid, request.getDeleteIds());
            log.debug("Deleted {} container details via deleteIds", request.getDeleteIds().size());
        }
        
        // Process details with actionType support
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            processBulkSaveContainerDetails(transactionPoid, request.getDetails(), userId);
            log.debug("Processed {} container details with actionType support", request.getDetails().size());
        }
        
        // Recalculate header totals
        recalculateHeaderTotals(transactionPoid);
        
        // Return all details
        return getContainerDetails(transactionPoid);
    }

    // ========== Cargo Description and Marks Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<CargoDescriptionDto> getCargoDescription(Long transactionPoid) {
        log.info("Getting cargo description for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        
        List<ExportShipBlManifestCargoDtl> entities = cargoDtlRepository.findByTransactionPoidAndDescriptionTypeOrderByDetRowId(transactionPoid, "CARGO");
        return entities.stream().map(mapper::mapCargoDescriptionToDto).collect(Collectors.toList());
    }

    @Override
    public List<CargoDescriptionDto> bulkSaveCargoDescription(Long transactionPoid, BulkSaveRequest<CargoDescriptionDto> request) {
        log.info("Bulk saving cargo description for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        String userId = getCurrentUser();
        
        // Handle backward-compatible deleteIds (legacy behavior)
        if (request.getDeleteIds() != null && !request.getDeleteIds().isEmpty()) {
            cargoDtlRepository.deleteByTransactionPoidAndDescriptionTypeAndDetRowIds(transactionPoid, "CARGO", request.getDeleteIds());
            log.debug("Deleted {} cargo description details via deleteIds", request.getDeleteIds().size());
        }
        
        // Process details with actionType support
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            processBulkSaveCargoDescription(transactionPoid, request.getDetails(), userId);
            log.debug("Processed {} cargo description details with actionType support", request.getDetails().size());
        }
        
        return getCargoDescription(transactionPoid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CargoMarksDto> getCargoMarks(Long transactionPoid) {
        log.info("Getting cargo marks for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        
        List<ExportShipBlManifestCargoDtl> entities = cargoDtlRepository.findByTransactionPoidAndDescriptionTypeOrderByDetRowId(transactionPoid, "MARKS");
        return entities.stream().map(mapper::mapCargoMarksToDto).collect(Collectors.toList());
    }

    @Override
    public List<CargoMarksDto> bulkSaveCargoMarks(Long transactionPoid, BulkSaveRequest<CargoMarksDto> request) {
        log.info("Bulk saving cargo marks for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        String userId = getCurrentUser();
        
        // Handle backward-compatible deleteIds (legacy behavior)
        if (request.getDeleteIds() != null && !request.getDeleteIds().isEmpty()) {
            cargoDtlRepository.deleteByTransactionPoidAndDescriptionTypeAndDetRowIds(transactionPoid, "MARKS", request.getDeleteIds());
            log.debug("Deleted {} cargo marks details via deleteIds", request.getDeleteIds().size());
        }
        
        // Process details with actionType support
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            processBulkSaveCargoMarks(transactionPoid, request.getDetails(), userId);
            log.debug("Processed {} cargo marks details with actionType support", request.getDetails().size());
        }
        
        return getCargoMarks(transactionPoid);
    }

    // ========== Charge Details Operations ==========

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getChargeDetails(Long transactionPoid) {
        log.info("Getting charge details for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        
        List<ExportShipBlManifestChargesDtl> entities = chargesDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<ChargeDetailDto> details = mapper.mapChargeListToDto(entities);
        
        // Enrich with LOV data
        enrichChargeDetailsWithLovData(details);
        
        // Calculate totals
        Object[] totals = chargesDtlRepository.calculateTotals(transactionPoid);
        ChargeTotalsDto totalsDto = new ChargeTotalsDto();
        if (totals != null && totals.length >= 4) {
            totalsDto.setTotalDtlBuyAmt((BigDecimal) totals[0]);
            totalsDto.setTotalDtlSellAmt((BigDecimal) totals[1]);
            totalsDto.setTotalDtlGainAmt((BigDecimal) totals[2]);
            totalsDto.setTotalDtlVatAmt((BigDecimal) totals[3]);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", details);
        result.put("totals", totalsDto);
        
        return result;
    }

    @Override
    public Map<String, Object> bulkSaveChargeDetails(Long transactionPoid, BulkSaveRequest<ChargeDetailDto> request) {
        log.info("Bulk saving charge details for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        String userId = getCurrentUser();
        
        // Handle backward-compatible deleteIds (legacy behavior)
        if (request.getDeleteIds() != null && !request.getDeleteIds().isEmpty()) {
            chargesDtlRepository.deleteByTransactionPoidAndDetRowIds(transactionPoid, request.getDeleteIds());
            log.debug("Deleted {} charge details via deleteIds", request.getDeleteIds().size());
        }
        
        // Process details with actionType support
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            processBulkSaveChargeDetails(transactionPoid, request.getDetails(), userId);
            log.debug("Processed {} charge details with actionType support", request.getDetails().size());
        }
        
        return getChargeDetails(transactionPoid);
    }

    // ========== Helper Methods ==========

    private void validateHeaderExists(Long transactionPoid) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        
        hdrRepository.findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new RuntimeException("Export BL not found with ID: " + transactionPoid));
    }

    private void recalculateHeaderTotals(Long transactionPoid) {
        ExportShipBlManifestHdr header = hdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new RuntimeException("Header not found"));
        
        // Calculate totals from general cargo details
        Object[] generalTotals = generalDtlRepository.calculateTotals(transactionPoid);
        if (generalTotals != null && generalTotals.length >= 5) {
            header.setTotalVolume((BigDecimal) generalTotals[0]);
            header.setTotalNetVolume((BigDecimal) generalTotals[1]);
            header.setTotalWeight((BigDecimal) generalTotals[2]);
            header.setTotalNetWeight((BigDecimal) generalTotals[3]);
            header.setTotalNoOfPacks((BigDecimal) generalTotals[4]);
        }
        
        // Also consider container totals if needed
        Object[] containerTotals = containerDtlRepository.calculateTotals(transactionPoid);
        if (containerTotals != null && containerTotals.length >= 5) {
            // Merge with general totals or use container totals
        }
        
        hdrRepository.save(header);
    }

    /**
     * Normalize actionType value to standardized form
     * Accepts: CREATED, ISCREATED, UPDATED, ISUPDATED, DELETED, ISDELETED, NOCHANGES, or null/empty
     */
    private String normalizeActionType(String actionType) {
        if (actionType == null || actionType.trim().isEmpty()) {
            return null; // No action
        }
        
        String upper = actionType.toUpperCase().trim();
        if (upper.equals("ISCREATED")) {
            return "CREATED";
        } else if (upper.equals("ISUPDATED")) {
            return "UPDATED";
        } else if (upper.equals("ISDELETED")) {
            return "DELETED";
        } else if (upper.equals("NOCHANGES")) {
            return null; // No action
        }
        
        return upper; // Return as-is if already CREATED, UPDATED, or DELETED
    }

    /**
     * Process bulk save for general cargo details with actionType support
     */
    private void processBulkSaveGeneralCargoDetails(Long transactionPoid, List<GeneralCargoDetailDto> details, String userId) {
        if (details == null || details.isEmpty()) {
            return;
        }
        
        for (GeneralCargoDetailDto dto : details) {
            String actionType = normalizeActionType(dto.getActionType());
            Long detRowId = dto.getDetRowId();
            
            if ("DELETED".equals(actionType)) {
                // Delete record
                if (detRowId != null) {
                    generalDtlRepository.deleteById(new ExportShipBlManifestGeneralDtlId(transactionPoid, detRowId));
                }
            } else if ("CREATED".equals(actionType) || detRowId == null) {
                // Create new record
                Long newDetRowId = generalDtlRepository.getNextDetRowId(transactionPoid);
                ExportShipBlManifestGeneralDtl entity = mapper.mapGeneralCargoToEntity(dto, transactionPoid, newDetRowId, userId);
                generalDtlRepository.save(entity);
            } else if ("UPDATED".equals(actionType) && detRowId != null) {
                // Update existing record
                ExportShipBlManifestGeneralDtl entity = generalDtlRepository.findById(
                        new ExportShipBlManifestGeneralDtlId(transactionPoid, detRowId))
                        .orElseThrow(() -> new RuntimeException("General cargo detail not found"));
                
                if (dto.getComodityPoid() != null) entity.setComodityPoid(dto.getComodityPoid());
                if (dto.getCargoDescription() != null) entity.setCargoDescription(dto.getCargoDescription());
                if (dto.getQuantity() != null) entity.setQuantity(dto.getQuantity());
                if (dto.getGrsVolume() != null) entity.setGrsVolume(dto.getGrsVolume());
                if (dto.getGrsWeight() != null) entity.setGrsWeight(dto.getGrsWeight());
                if (dto.getNetVolume() != null) entity.setNetVolume(dto.getNetVolume());
                if (dto.getNetWeight() != null) entity.setNetWeight(dto.getNetWeight());
                if (dto.getTareWeight() != null) entity.setTareWeight(dto.getTareWeight());
                if (dto.getNoOfPacks() != null) entity.setNoOfPacks(dto.getNoOfPacks());
                if (dto.getPackUnit() != null) entity.setPackUnit(dto.getPackUnit());
                if (dto.getDestinationPortPoid() != null) entity.setDestinationPortPoid(dto.getDestinationPortPoid());
                
                generalDtlRepository.save(entity);
            }
            // If actionType is null (no changes), skip
        }
    }

    /**
     * Process bulk save for container details with actionType support
     */
    private void processBulkSaveContainerDetails(Long transactionPoid, List<ContainerDetailDto> details, String userId) {
        if (details == null || details.isEmpty()) {
            return;
        }
        
        for (ContainerDetailDto dto : details) {
            String actionType = normalizeActionType(dto.getActionType());
            Long detRowId = dto.getDetRowId();
            
            if ("DELETED".equals(actionType)) {
                if (detRowId != null) {
                    containerDtlRepository.deleteById(new ExportShipBlManifestContainerDtlId(transactionPoid, detRowId));
                }
            } else if ("CREATED".equals(actionType) || detRowId == null) {
                Long newDetRowId = containerDtlRepository.getNextDetRowId(transactionPoid);
                ExportShipBlManifestContainerDtl entity = mapper.mapContainerToEntity(dto, transactionPoid, newDetRowId, userId);
                containerDtlRepository.save(entity);
            } else if ("UPDATED".equals(actionType) && detRowId != null) {
                ExportShipBlManifestContainerDtl entity = containerDtlRepository.findById(
                        new ExportShipBlManifestContainerDtlId(transactionPoid, detRowId))
                        .orElseThrow(() -> new RuntimeException("Container detail not found"));
                
                // Update container-specific fields (selective update of provided fields)
                if (dto.getContainerNo() != null) entity.setContainerNo(dto.getContainerNo());
                if (dto.getEquipmentIsoType() != null) entity.setEquipmentIsoType(dto.getEquipmentIsoType());
                if (dto.getEquipmentType() != null) entity.setEquipmentType(dto.getEquipmentType());
                if (dto.getQuantity() != null) entity.setQuantity(dto.getQuantity());
                if (dto.getGrsWeight() != null) entity.setGrsWeight(dto.getGrsWeight());
                if (dto.getNetWeight() != null) entity.setNetWeight(dto.getNetWeight());
                if (dto.getComodityPoid() != null) entity.setComodityPoid(dto.getComodityPoid());
                
                containerDtlRepository.save(entity);
            }
        }
    }

    /**
     * Process bulk save for cargo description with actionType support
     */
    private void processBulkSaveCargoDescription(Long transactionPoid, List<CargoDescriptionDto> details, String userId) {
        if (details == null || details.isEmpty()) {
            return;
        }
        
        for (CargoDescriptionDto dto : details) {
            String actionType = normalizeActionType(dto.getActionType());
            Long detRowId = dto.getDetRowId();
            
            if ("DELETED".equals(actionType)) {
                if (detRowId != null) {
                    cargoDtlRepository.deleteById(new ExportShipBlManifestCargoDtlId(transactionPoid, detRowId, "CARGO"));
                }
            } else if ("CREATED".equals(actionType) || detRowId == null) {
                Long newDetRowId = cargoDtlRepository.getNextDetRowId(transactionPoid, "CARGO");
                ExportShipBlManifestCargoDtl entity = mapper.mapCargoDescriptionToEntity(dto, transactionPoid, newDetRowId, userId);
                cargoDtlRepository.save(entity);
            } else if ("UPDATED".equals(actionType) && detRowId != null) {
                ExportShipBlManifestCargoDtl entity = cargoDtlRepository.findById(
                        new ExportShipBlManifestCargoDtlId(transactionPoid, detRowId, "CARGO"))
                        .orElseThrow(() -> new RuntimeException("Cargo description not found"));
                
                if (dto.getCargoDescription() != null) entity.setCargoDescription(dto.getCargoDescription());
                if (dto.getRecordOrder() != null) entity.setRecordOrder(dto.getRecordOrder());
                
                cargoDtlRepository.save(entity);
            }
        }
    }

    /**
     * Process bulk save for cargo marks with actionType support
     */
    private void processBulkSaveCargoMarks(Long transactionPoid, List<CargoMarksDto> details, String userId) {
        if (details == null || details.isEmpty()) {
            return;
        }
        
        for (CargoMarksDto dto : details) {
            String actionType = normalizeActionType(dto.getActionType());
            Long detRowId = dto.getDetRowId();
            
            if ("DELETED".equals(actionType)) {
                if (detRowId != null) {
                    cargoDtlRepository.deleteById(new ExportShipBlManifestCargoDtlId(transactionPoid, detRowId, "MARKS"));
                }
            } else if ("CREATED".equals(actionType) || detRowId == null) {
                Long newDetRowId = cargoDtlRepository.getNextDetRowId(transactionPoid, "MARKS");
                ExportShipBlManifestCargoDtl entity = mapper.mapCargoMarksToEntity(dto, transactionPoid, newDetRowId, userId);
                cargoDtlRepository.save(entity);
            } else if ("UPDATED".equals(actionType) && detRowId != null) {
                ExportShipBlManifestCargoDtl entity = cargoDtlRepository.findById(
                        new ExportShipBlManifestCargoDtlId(transactionPoid, detRowId, "MARKS"))
                        .orElseThrow(() -> new RuntimeException("Cargo marks not found"));
                
                if (dto.getCargoDescription() != null) entity.setCargoDescription(dto.getCargoDescription());
                if (dto.getRecordOrder() != null) entity.setRecordOrder(dto.getRecordOrder());
                
                cargoDtlRepository.save(entity);
            }
        }
    }

    /**
     * Process bulk save for charge details with actionType support
     */
    private void processBulkSaveChargeDetails(Long transactionPoid, List<ChargeDetailDto> details, String userId) {
        if (details == null || details.isEmpty()) {
            return;
        }
        
        for (ChargeDetailDto dto : details) {
            String actionType = normalizeActionType(dto.getActionType());
            Long detRowId = dto.getDetRowId();
            
            if ("DELETED".equals(actionType)) {
                if (detRowId != null) {
                    chargesDtlRepository.deleteById(new ExportShipBlManifestChargesDtlId(transactionPoid, detRowId));
                }
            } else if ("CREATED".equals(actionType) || detRowId == null) {
                Long newDetRowId = chargesDtlRepository.getNextDetRowId(transactionPoid);
                ExportShipBlManifestChargesDtl entity = mapper.mapChargeToEntity(dto, transactionPoid, newDetRowId, userId);
                chargesDtlRepository.save(entity);
            } else if ("UPDATED".equals(actionType) && detRowId != null) {
                ExportShipBlManifestChargesDtl entity = chargesDtlRepository.findById(
                        new ExportShipBlManifestChargesDtlId(transactionPoid, detRowId))
                        .orElseThrow(() -> new RuntimeException("Charge detail not found"));
                
                if (dto.getChargePoid() != null) entity.setChargePoid(dto.getChargePoid());
                if (dto.getQuantity() != null) entity.setQuantity(dto.getQuantity());
                if (dto.getBuyPercharge() != null) entity.setBuyPercharge(dto.getBuyPercharge());
                if (dto.getPerQuantityAmount() != null) entity.setPerQuantityAmount(dto.getPerQuantityAmount());
                if (dto.getCurrencyCode() != null) entity.setCurrencyCode(dto.getCurrencyCode());
                
                chargesDtlRepository.save(entity);
            }
        }
    }

    // ========== Special Operations (to be continued in next part) ==========

    @Override
    public Map<String, Object> loadBooking(Long transactionPoid, LoadBookingRequest request) {
        // TODO: Implement load booking from MATE
        throw new UnsupportedOperationException("Load booking not yet implemented");
    }

    @Override
    public byte[] generateBlPrint(Long transactionPoid, GenerateBlPrintRequest request,String docId) throws Exception {
    	
    	log.info("Bl print : {}", transactionPoid);
    	
    	Long groupPoid=UserContext.getGroupPoid();
    	Long companyPoid=UserContext.getCompanyPoid();
    	ExportShipBlManifestHdr entity = hdrRepository
                .findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new RuntimeException("Export BL not found with ID: " + transactionPoid));
    	if(entity.getBlOrginalPrint()!=null && entity.getBlOrginalPrint().equalsIgnoreCase("Y")) {
    		throw new RuntimeException("BL already printed");
    	}
    	
    	String jrxmlFile=customRepository.getBlPrintReport(groupPoid, companyPoid, docId, transactionPoid, "BL_PRINT");
    	Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-140");
		JasperReport mainReport = printService.load("Shipping/"+jrxmlFile);
		params.put("DRAFT_ORIGINAL", request.getDraftOriginal());
		return printService.fillReportToPdf(mainReport, params, dataSource);
    	
    }

    @Override
    public byte[] generateManifest(Long transactionPoid, GenerateManifestRequest request, String docId) throws Exception {
    	Map<String, Object> params = printService.buildBaseParams(transactionPoid, docId);
		params.put("P_FREIGHTCARGO", request.getFreightCargo().toString().toUpperCase());
        params.put("SUBREPORT_MARK_INFO", printService.load("Shipping/SH/Cargo/Mark_Info_Subreport1.jrxml"));
	    params.put("SUBREPORT_CONTAINER_INFO", printService.load("Shipping/SH/Cargo/Container_Info_Subreport1.jrxml"));
	    params.put("SUBREPORT_DESCRIPTION_INFO", printService.load("Shipping/SH/Cargo/Description_Info_Subreport1.jrxml"));
	    params.put("SUBREPORT_FREIGHT_DETAIL", printService.load("Shipping/SH/Cargo/Freight_Detail_Subreport1.jrxml"));
	    JasperReport mainReport = printService.load("Shipping/SH/Cargo/Manifest_Cargo_WithCharges.jrxml");
	    return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public byte[] generateDetentionStorage(Long transactionPoid,String docId) throws Exception {
    	Map<String, Object> params = printService.buildBaseParams(transactionPoid, docId);
		JasperReport mainReport = printService.load("Shipping/SH/Container_Detention_details.jrxml");
		return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public void exportEdi(Long transactionPoid) {
        log.info("Exporting EDI for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        Long userPoid = UserContext.getUserPoid();
        
        customRepository.exportEdi(transactionPoid, userPoid);
    }

    @Override
    public ValidationResponse validate(Long transactionPoid, ExportManifestBlRequest request) {
        ValidationResponse response = new ValidationResponse();
        
        // Validate required fields
        if (request.getVoyageTransactionPoid() == null) {
            response.addError("Voyage Transaction POID is required");
        }
        if (request.getSalesmanPoid() == null) {
            response.addError("Salesman POID is required");
        }
        
        // Validate BL number uniqueness if provided
        if (request.getBlNumber() != null && !request.getBlNumber().trim().isEmpty()) {
            String oldBlNumber = transactionPoid != null ? 
                    hdrRepository.findById(transactionPoid).map(ExportShipBlManifestHdr::getBlNumber).orElse(null) : null;
            String status = customRepository.validateBlNumberDuplicate(
                    request.getBlNumber().trim(), oldBlNumber != null ? oldBlNumber : "NEWRECORD", 
                    transactionPoid != null ? "UPDATING" : "INSERTING");
            if (status == null || !status.startsWith("SUCCESS")) {
                response.addError("BL number already exists: " + request.getBlNumber());
            }
        }
        
        return response;
    }

    @Override
    public void afterSave(Long transactionPoid) {
        log.info("Processing after save for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        
        customRepository.processAfterSave(groupPoid, companyPoid, transactionPoid, null, "AUTOSUMWEIGHTPEXPORT", userPoid);
    }

    @Override
    public BlStatusResponse getBlStatus(Long transactionPoid) {
        log.info("Getting BL status for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        
        String status = customRepository.getBlStatus(groupPoid, companyPoid, userPoid, transactionPoid);
        
        BlStatusResponse response = new BlStatusResponse();
        response.setStatus(status);
        response.setDisplayInfo(status); // Can be enhanced with more detailed status info
        
        return response;
    }

    @Override
    public Map<String, Object> quotationAfterBrowse(Long transactionPoid, QuotationAfterBrowseRequest request) {
        log.info("Processing quotation after browse for Export BL: {}", transactionPoid);
        
        Long actualTransactionPoid = transactionPoid != null ? transactionPoid : 0L; // Use 0 for new records
        
        customRepository.processQuotationAfterBrowse(
                getGroupPoid(),
                getCompanyPoid(),
                getUserPoid(),
                DOC_ID,
                actualTransactionPoid,
                "SHIP_QUOTATION_EXPORT",
                request.getQuotationTransactionPoid()
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("fieldsUpdated", Arrays.asList("salesmanPoid", "portOfLoadingPoid", "portOfDischargePoid"));
        
        return result;
    }

    // ========== LOV Enrichment Methods ==========

    /**
     * Enrich header DTO with LOV data for all POID fields
     */
    private void enrichHeaderWithLovData(ExportManifestBlResponse dto, ExportShipBlManifestHdr entity) {
        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();

        try {
            // Voyage
            if (entity.getVoyageTransactionPoid() != null) {
                dto.setVoyageTransactionDet(lovService.getLovItemByPoid(
                        entity.getVoyageTransactionPoid(), "VESSAL_VOYAGE", groupPoid, companyPoid, userPoid));
            }

            // Quotation
            if (entity.getQuotationTransactionPoid() != null) {
                dto.setQuotationTransactionDet(lovService.getLovItemByPoid(
                        entity.getQuotationTransactionPoid(), "SHIP_QUOTATION_EXPORT", groupPoid, companyPoid, userPoid));
            }

            // Salesman
            if (entity.getSalesmanPoid() != null) {
                dto.setSalesmanDet(lovService.getLovItemByPoid(
                        entity.getSalesmanPoid(), "SALESMAN", groupPoid, companyPoid, userPoid));
            }

            // Commodity
            if (entity.getComodityPoid() != null) {
                dto.setComodityDet(lovService.getLovItemByPoid(
                        entity.getComodityPoid(), "COMODITY", groupPoid, companyPoid, userPoid));
            }

            // Ports
            if (entity.getPlaceOfIssuePoid() != null) {
                dto.setPlaceOfIssueDet(lovService.getLovItemByPoid(
                        entity.getPlaceOfIssuePoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getPlaceOfRecieptPoid() != null) {
                dto.setPlaceOfRecieptDet(lovService.getLovItemByPoid(
                        entity.getPlaceOfRecieptPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getPlaceOfDelieveryPoid() != null) {
                dto.setPlaceOfDelieveryDet(lovService.getLovItemByPoid(
                        entity.getPlaceOfDelieveryPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getPortOfLoadingPoid() != null) {
                dto.setPortOfLoadingDet(lovService.getLovItemByPoid(
                        entity.getPortOfLoadingPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getPortOfDischargePoid() != null) {
                dto.setPortOfDischargeDet(lovService.getLovItemByPoid(
                        entity.getPortOfDischargePoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
            }

            // Customers (Shipper, Consignee, Notify, Booking Party)
            if (entity.getShipperPoid() != null) {
                dto.setShipperDet(lovService.getLovItemByPoid(
                        entity.getShipperPoid(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getShipperAddressPoid() != null) {
                dto.setShipperAddressDet(lovService.getLovItemByPoid(
                        entity.getShipperAddressPoid(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getConsigneePoid() != null) {
                dto.setConsigneeDet(lovService.getLovItemByPoid(
                        entity.getConsigneePoid(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getConsigneeAddressPoid() != null) {
                dto.setConsigneeAddressDet(lovService.getLovItemByPoid(
                        entity.getConsigneeAddressPoid(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getNotifyPoid1() != null) {
                dto.setNotify1Det(lovService.getLovItemByPoid(
                        entity.getNotifyPoid1(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getNotifyAddressPoid1() != null) {
                dto.setNotify1AddressDet(lovService.getLovItemByPoid(
                        entity.getNotifyAddressPoid1(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getNotifyPoid2() != null) {
                dto.setNotify2Det(lovService.getLovItemByPoid(
                        entity.getNotifyPoid2(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getNotifyAddressPoid2() != null) {
                dto.setNotify2AddressDet(lovService.getLovItemByPoid(
                        entity.getNotifyAddressPoid2(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getNotifyPoid3() != null) {
                dto.setNotify3Det(lovService.getLovItemByPoid(
                        entity.getNotifyPoid3(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getNotifyAddressPoid3() != null) {
                dto.setNotify3AddressDet(lovService.getLovItemByPoid(
                        entity.getNotifyAddressPoid3(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getBookingPartyPoid() != null) {
                dto.setBookingPartyDet(lovService.getLovItemByPoid(
                        entity.getBookingPartyPoid(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }
            if (entity.getCanNotifyCustomerPoid() != null) {
                dto.setCanNotifyCustomerDet(lovService.getLovItemByPoid(
                        entity.getCanNotifyCustomerPoid(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
            }

            // Company and Division
            if (entity.getDocumentCompanyPoid() != null) {
                dto.setDocumentCompanyDet(lovService.getLovItemByPoid(
                        entity.getDocumentCompanyPoid(), "COMPANY", groupPoid, companyPoid, userPoid));
            }
            if (entity.getDocumentCompanyDivisionPoid() != null) {
                dto.setDocumentCompanyDivisionDet(lovService.getLovItemByPoid(
                        entity.getDocumentCompanyDivisionPoid(), "SHIP_DIVISION_PRINT", groupPoid, companyPoid, userPoid));
            }

            // Agent
            if (entity.getAgentPoid() != null) {
                try {
                    dto.setAgentDet(lovService.getLovItemByPoid(
                            entity.getAgentPoid(), "AGENT_MASTER", groupPoid, companyPoid, userPoid));
                } catch (Exception e) {
                    log.warn("Failed to fetch AGENT_MASTER LOV for agentPoid: {}", entity.getAgentPoid(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch some LOV data for header", e);
        }
    }

    /**
     * Enrich general cargo details DTOs with LOV data
     */
    private void enrichGeneralCargoDetailsWithLovData(List<GeneralCargoDetailDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();

        for (GeneralCargoDetailDto dto : dtos) {
            try {
                if (dto.getComodityPoid() != null) {
                    dto.setComodityDet(lovService.getLovItemByPoid(
                            dto.getComodityPoid(), "COMODITY", groupPoid, companyPoid, userPoid));
                }
                if (dto.getDestinationPortPoid() != null) {
                    dto.setDestinationPortDet(lovService.getLovItemByPoid(
                            dto.getDestinationPortPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch LOV data for general cargo detail with detRowId: {}", dto.getDetRowId(), e);
            }
        }
    }

    /**
     * Enrich container details DTOs with LOV data
     */
    private void enrichContainerDetailsWithLovData(List<ContainerDetailDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();

        for (ContainerDetailDto dto : dtos) {
            try {
                if (dto.getComodityPoid() != null) {
                    dto.setComodityDet(lovService.getLovItemByPoid(
                            dto.getComodityPoid(), "COMODITY", groupPoid, companyPoid, userPoid));
                }
                if (dto.getDestinationPortPoid() != null) {
                    dto.setDestinationPortDet(lovService.getLovItemByPoid(
                            dto.getDestinationPortPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
                }
                if (dto.getMateTransactionPoid() != null) {
                    try {
                        dto.setMateTransactionDet(lovService.getLovItemByPoid(
                                dto.getMateTransactionPoid(), "SHIP_MATE_HDR", groupPoid, companyPoid, userPoid));
                    } catch (Exception e) {
                        log.warn("Failed to fetch SHIP_MATE_HDR LOV for mateTransactionPoid: {}", dto.getMateTransactionPoid(), e);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch LOV data for container detail with detRowId: {}", dto.getDetRowId(), e);
            }
        }
    }

    /**
     * Enrich charge details DTOs with LOV data
     */
    private void enrichChargeDetailsWithLovData(List<ChargeDetailDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();

        for (ChargeDetailDto dto : dtos) {
            try {
                if (dto.getChargePoid() != null) {
                    dto.setChargeDet(lovService.getLovItemByPoid(
                            dto.getChargePoid(), "CHARGE_MASTER", groupPoid, companyPoid, userPoid));
                }
                if (dto.getPaidAtPortPoid() != null) {
                    dto.setPaidAtPortDet(lovService.getLovItemByPoid(
                            dto.getPaidAtPortPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
                }
                if (dto.getReceiptInvoicePoid() != null) {
                    try {
                        dto.setReceiptInvoiceDet(lovService.getLovItemByPoid(
                                dto.getReceiptInvoicePoid(), "MANIFEST_RECEIPT_INVOICE", groupPoid, companyPoid, userPoid));
                    } catch (Exception e) {
                        log.warn("Failed to fetch MANIFEST_RECEIPT_INVOICE LOV for receiptInvoicePoid: {}", dto.getReceiptInvoicePoid(), e);
                    }
                }
                if (dto.getTaxPoid() != null) {
                    try {
                        dto.setTaxDet(lovService.getLovItemByPoid(
                                dto.getTaxPoid(), "TAX_MASTER", groupPoid, companyPoid, userPoid));
                    } catch (Exception e) {
                        log.warn("Failed to fetch TAX_MASTER LOV for taxPoid: {}", dto.getTaxPoid(), e);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch LOV data for charge detail with detRowId: {}", dto.getDetRowId(), e);
            }
        }
    }
}

