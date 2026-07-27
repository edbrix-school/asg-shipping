package com.asg.shipping.exportManifestUpdate.service;

import javax.sql.DataSource;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
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
import com.asg.shipping.exportManifestUpdate.util.ExportManifestAddressTypeAudit;
import com.asg.shipping.importmanifestupdate.service.BlManifestValidationService;
import com.asg.shipping.address.entity.AddressDetails;
import com.asg.shipping.address.entity.AddressDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import com.asg.common.lib.security.model.CustomAuthDetails;

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
    private static final String HDR_KEY_ID_LABEL = "TRANSACTION_POID";
    private static final String LOG_KEY_ID_FORMAT = "KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s";
    private static final String LOG_CARGO_KEY_ID_FORMAT = "KeyId = TRANSACTION_POID: %s DESCRIPTION_TYPE: %s";
    private static final String LOG_ROW_CREATED_FORMAT = "Row Created on %s with DetRowId: %s";

    private final ExportShipBlManifestHdrRepository hdrRepository;
    private final ExportShipBlManifestGeneralDtlRepository generalDtlRepository;
    private final ExportShipBlManifestContainerDtlRepository containerDtlRepository;
    private final ExportShipBlManifestCargoDtlRepository cargoDtlRepository;
    private final ExportShipBlManifestChargesDtlRepository chargesDtlRepository;
    private final ExportManifestBlCustomRepository customBLRepository;
    private final ExportManifestUpdateMapper mapper;
    private final BlManifestValidationService blManifestValidationService;
    private final DocumentSearchService documentSearchService;
    private final LovService lovService;
	private final PrintService printService;
	private final DataSource dataSource;
    private final AddressDetailsRepository addressDetailsRepository;
    private final LoggingService loggingService;

    // ========== Header Operations ==========

    @Override
    @Transactional(readOnly = true)
    public ExportManifestBlResponse getExportBlById(Long transactionPoid) {
        log.info("Getting Export BL by ID: {}", transactionPoid);
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        
        // Query filters: DELETED = 'N' (NULL excluded), BL_TYPE = 'EXPORT', GROUP_POID and COMPANY_POID match
        ExportShipBlManifestHdr entity = hdrRepository
                .findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new RuntimeException("Export BL not found with ID: " + transactionPoid));
        
        ExportManifestBlResponse response = mapper.mapToResponse(entity);

        // Enrich with LOV data
        enrichHeaderWithLovData(response, entity);

        // Fetch status once — used for both statusDetails and displayTopInfoExportBLS
        Long userPoid = UserContext.getUserPoid();
        String status = customBLRepository.getBlStatus(groupPoid, companyPoid, userPoid, transactionPoid);
        response.setStatusDetails(parseBlStatus(status));
        response.setDisplayTopInfoExportBLS(resolveDisplayTopInfoFromStatus(status));

        // Retrieve general cargo details (header already validated above)
        response.setGeneralCargoDetails(fetchGeneralCargoDetails(transactionPoid));

        // Simple single-string view: all DESC / MARK rows joined via native query on string column
        response.setSimpleCargoDescription(joinDescriptionStrings(
                cargoDtlRepository.findDescriptionStringsByType(transactionPoid, "CARGO")));
        response.setSimpleCargoMarks(joinDescriptionStrings(
                cargoDtlRepository.findDescriptionStringsByType(transactionPoid, "MARK")));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchExportBls(FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Searching Export BLs with filters: {}", filters);

        // Resolve filter components from FilterRequestDto
        String operator = documentSearchService.resolveOperator(filters);
        String isDeleted = documentSearchService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentSearchService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate, endDate);
        
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
            String status = customBLRepository.validateBlNumberDuplicate(
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

        loggingService.createLogSummaryEntry(
                LogDetailsEnum.CREATED, resolveDocId(), String.valueOf(saved.getTransactionPoid()));

        log.info("Successfully created Export BL with ID: {}", saved.getTransactionPoid());
        return mapper.mapToResponse(saved);
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

        Long shipperAddressPoidBefore = entity.getShipperAddressPoid();

        // Snapshot before the mapper mutates the managed entity — used for the field-level audit diff
        ExportShipBlManifestHdr oldEntity = new ExportShipBlManifestHdr();
        BeanUtils.copyProperties(entity, oldEntity);

        // Validate BL number uniqueness if changed
        if (request.getBlNumber() != null && !request.getBlNumber().trim().equals(entity.getBlNumber())) {
            String status = customBLRepository.validateBlNumberDuplicate(
                    request.getBlNumber().trim(), entity.getBlNumber(), "UPDATING");
            if (status == null || !status.startsWith("SUCCESS")) {
                throw new RuntimeException("Export BL number already exists: " + request.getBlNumber());
            }
        }
        
        // Store old cargo type for comparison
        String oldCargoType = entity.getCargoType();

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

        String docId = resolveDocId();
        loggingService.logChanges(oldEntity, saved, ExportShipBlManifestHdr.class, docId,
                transactionPoid.toString(), LogDetailsEnum.MODIFIED, HDR_KEY_ID_LABEL);

        ExportManifestAddressTypeAudit.logShipperAddressTypeChange(
                loggingService,
                addressDetailsRepository,
                docId,
                transactionPoid.toString(),
                shipperAddressPoidBefore,
                request.getShipperAddressType());

        log.info("Successfully updated Export BL with ID: {}", saved.getTransactionPoid());
        return mapper.mapToResponse(saved);
    }

    private ExportManifestBlResponse loadMinimalHeader(Long transactionPoid) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        ExportShipBlManifestHdr entity = hdrRepository
                .findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new RuntimeException("Export BL not found with ID: " + transactionPoid));
        return mapper.mapToResponse(entity);
    }

    private void validateHeaderExists(Long transactionPoid) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        if (hdrRepository.findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid).isEmpty()) {
            throw new RuntimeException("Export BL not found with ID: " + transactionPoid);
        }
    }

    private ActionType resolveAction(ActionType actionType) {
        return actionType != null ? actionType : ActionType.NOCHANGES;
    }

    // ========== Logging Helpers ==========

    /** Document id from the request context, falling back to this document's own id. */
    private String resolveDocId() {
        String docId = UserContext.getDocumentId();
        return (docId != null && !docId.isBlank()) ? docId : DOC_ID;
    }

    private void logSummaryEntries(List<String> logEntries, String docId, String docKeyPoid) {
        logEntries.forEach(entry -> loggingService.createLogSummaryEntry(docId, docKeyPoid, entry));
    }

    /** Writes the field-level diffs collected for updated detail rows. */
    private <T> void logDetailChanges(List<LogRequestDto<T>> logRequests) {
        if (!logRequests.isEmpty()) {
            loggingService.createLogBatch(logRequests);
        }
    }


    @Override
    public ExportManifestUpdateResponse updateExportBlCombined(Long transactionPoid, ExportManifestUpdateRequest request) {
        log.info("Updating Export BL combined for ID: {}", transactionPoid);

        // Validate once — all sub-operations share this check
        validateHeaderExists(transactionPoid);
        String userId = getCurrentUser();

        // --- Simple string saves run FIRST so subsequent list fetches reflect the new data ---
        String simpleCargoDescResponse = null;
        if (request.getSimpleCargoDescription() != null) {
            simpleCargoDescResponse = saveSimpleCargoDescription(transactionPoid, request.getSimpleCargoDescription());
        }

        String simpleCargoMarksResponse = null;
        if (request.getSimpleCargoMarks() != null) {
            simpleCargoMarksResponse = saveSimpleCargoMarks(transactionPoid, request.getSimpleCargoMarks());
        }

        // --- Header ---
        // With @JsonUnwrapped, header is always non-null but fields may all be null if FE sent nothing.
        // Voyage is the usual presence signal; also accept comodityPoid so commodity-only header edits persist.
        ExportManifestBlRequest headerReq = request.getHeader();
        boolean hasHeaderUpdate = headerReq != null
                && (headerReq.getVoyageTransactionPoid() != null || headerReq.getComodityPoid() != null);
        ExportManifestBlResponse headerResponse = hasHeaderUpdate
                ? updateExportBl(transactionPoid, headerReq)
                : loadMinimalHeader(transactionPoid);

        // Capture UserContext (ThreadLocal) so child threads can inherit it
        CustomAuthDetails authDetails = UserContext.getCurrentUser();

        // Fire all 5 detail writes in parallel — no fetch, no return value
        CompletableFuture<Void> generalFuture = request.getGeneralCargoDetails() != null
                ? CompletableFuture.runAsync(() -> {
                    UserContext.setCurrentUser(authDetails);
                    try { doUpdateGeneralCargoDetails(transactionPoid, request.getGeneralCargoDetails(), userId); }
                    finally { UserContext.clear(); }
                }) : CompletableFuture.completedFuture(null);

        CompletableFuture<Void> containerFuture = request.getContainerDetails() != null
                ? CompletableFuture.runAsync(() -> {
                    UserContext.setCurrentUser(authDetails);
                    try { doUpdateContainerDetails(transactionPoid, request.getContainerDetails(), userId); }
                    finally { UserContext.clear(); }
                }) : CompletableFuture.completedFuture(null);

        CompletableFuture<Void> cargoDescFuture = request.getCargoDescription() != null
                ? CompletableFuture.runAsync(() -> {
                    UserContext.setCurrentUser(authDetails);
                    try { doUpdateCargoDescription(transactionPoid, request.getCargoDescription(), userId); }
                    finally { UserContext.clear(); }
                }) : CompletableFuture.completedFuture(null);

        CompletableFuture<Void> cargoMarksFuture = request.getCargoMarks() != null
                ? CompletableFuture.runAsync(() -> {
                    UserContext.setCurrentUser(authDetails);
                    try { doUpdateCargoMarks(transactionPoid, request.getCargoMarks(), userId); }
                    finally { UserContext.clear(); }
                }) : CompletableFuture.completedFuture(null);

        CompletableFuture<Void> chargeFuture = request.getChargeDetails() != null
                ? CompletableFuture.runAsync(() -> {
                    UserContext.setCurrentUser(authDetails);
                    try { doUpdateChargeDetails(transactionPoid, request.getChargeDetails(), userId); }
                    finally { UserContext.clear(); }
                }) : CompletableFuture.completedFuture(null);

        // Wait for all writes; rethrow the first failure
        try {
            CompletableFuture.allOf(generalFuture, containerFuture, cargoDescFuture, cargoMarksFuture, chargeFuture).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            throw (cause instanceof RuntimeException) ? (RuntimeException) cause : new RuntimeException(cause);
        }

        // Recalculate header totals once after all parallel writes complete
        boolean anyUpdate = request.getGeneralCargoDetails() != null
                || request.getContainerDetails() != null
                || request.getChargeDetails() != null;
        if (anyUpdate) {
            recalculateHeaderTotals(transactionPoid);
        }

        ExportManifestUpdateResponse response = new ExportManifestUpdateResponse();
        response.setHeader(headerResponse);
        response.setSimpleCargoDescription(simpleCargoDescResponse);
        response.setSimpleCargoMarks(simpleCargoMarksResponse);

        return response;
    }

    // ========== Simple single-string join helpers ==========

    /** Joins raw description strings fetched via native query — most reliable approach. */
    private String joinDescriptionStrings(List<String> rows) {
        if (rows == null || rows.isEmpty()) return null;
        return rows.stream()
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private String joinCargoDescriptions(List<CargoDescriptionDto> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream()
                .map(CargoDescriptionDto::getCargoDescription)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private String joinCargoMarks(List<CargoMarksDto> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream()
                .map(CargoMarksDto::getCargoDescription)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    // ========== Simple single-string save helpers ==========

    private String saveSimpleCargoDescription(Long transactionPoid, String text) {
        log.info("Saving simple cargo description for Export BL: {}", transactionPoid);
        // Rows are replaced wholesale, so capture the previous text before it is gone
        String previousText = joinDescriptionStrings(
                cargoDtlRepository.findDescriptionStringsByType(transactionPoid, "CARGO"));
        cargoDtlRepository.deleteAllByTransactionPoidAndDescriptionType(transactionPoid, "CARGO");
        if (text != null && !text.trim().isEmpty()) {
            ExportShipBlManifestCargoDtl entity = new ExportShipBlManifestCargoDtl();
            entity.setTransactionPoid(transactionPoid);
            entity.setDetRowId(1L);
            entity.setDescriptionType("CARGO");
            entity.setCargoDescription(text);
            cargoDtlRepository.save(entity);
        }
        logSimpleCargoTextChange(transactionPoid, "CARGO", "simpleCargoDescription", previousText, text);
        return text;
    }

    private String saveSimpleCargoMarks(Long transactionPoid, String text) {
        log.info("Saving simple cargo marks for Export BL: {}", transactionPoid);
        // Rows are replaced wholesale, so capture the previous text before it is gone
        String previousText = joinDescriptionStrings(
                cargoDtlRepository.findDescriptionStringsByType(transactionPoid, "MARK"));
        cargoDtlRepository.deleteAllByTransactionPoidAndDescriptionType(transactionPoid, "MARK");
        if (text != null && !text.trim().isEmpty()) {
            ExportShipBlManifestCargoDtl entity = new ExportShipBlManifestCargoDtl();
            entity.setTransactionPoid(transactionPoid);
            entity.setDetRowId(1L);
            entity.setDescriptionType("MARK");
            entity.setCargoDescription(text);
            cargoDtlRepository.save(entity);
        }
        logSimpleCargoTextChange(transactionPoid, "MARK", "simpleCargoMarks", previousText, text);
        return text;
    }

    /** Logs the before/after text of a wholesale-replaced cargo description block. */
    private void logSimpleCargoTextChange(Long transactionPoid, String descriptionType, String fieldName,
                                          String oldText, String newText) {
        String oldValue = oldText == null ? "" : oldText;
        String newValue = newText == null ? "" : newText;
        if (oldValue.equals(newValue)) {
            return;
        }
        loggingService.logSimpleFieldChange(
                ExportShipBlManifestCargoDtl.class,
                resolveDocId(),
                transactionPoid.toString(),
                fieldName,
                oldValue,
                newValue,
                String.format(LOG_CARGO_KEY_ID_FORMAT, transactionPoid, descriptionType));
    }

    @Override
    public void deleteExportBl(Long transactionPoid) {
        log.info("Deleting Export BL with ID: {}", transactionPoid);
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        
        ExportShipBlManifestHdr entity = hdrRepository
                .findExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new RuntimeException("Export BL not found with ID: " + transactionPoid));
        
        entity.setDeleted("Y");
        hdrRepository.save(entity);

        // Deletion is audited by the database procedure — nothing to log here

        log.info("Successfully deleted Export BL with ID: {}", transactionPoid);
    }

    // ========== General Cargo Details Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<GeneralCargoDetailDto> getGeneralCargoDetails(Long transactionPoid) {
        log.info("Getting general cargo details for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        return fetchGeneralCargoDetails(transactionPoid);
    }

    private List<GeneralCargoDetailDto> fetchGeneralCargoDetails(Long transactionPoid) {
        List<ExportShipBlManifestGeneralDtl> entities = generalDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<GeneralCargoDetailDto> dtos = mapper.mapGeneralCargoListToDto(entities);
        enrichGeneralCargoDetailsWithLovData(dtos);
        return dtos;
    }

    @Override
    public List<GeneralCargoDetailDto> updateGeneralCargoDetails(Long transactionPoid, List<GeneralCargoDetailDto> request) {
        log.info("Updating general cargo details for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        doUpdateGeneralCargoDetails(transactionPoid, request, getCurrentUser());
        recalculateHeaderTotals(transactionPoid);
        return fetchGeneralCargoDetails(transactionPoid);
    }

    private void doUpdateGeneralCargoDetails(Long transactionPoid, List<GeneralCargoDetailDto> request, String userId) {
        if (request != null) {
            String docId = resolveDocId();
            String docKeyPoid = transactionPoid.toString();
            List<Long> deleteIds = new ArrayList<>();
            List<String> summaryEntries = new ArrayList<>();
            List<LogRequestDto<ExportShipBlManifestGeneralDtl>> logRequests = new ArrayList<>();
            for (GeneralCargoDetailDto dto : request) {
                ActionType action = resolveAction(dto.getActionType());
                Long detRowId = dto.getDetRowId();

                switch (action) {
                    case ISDELETED:
                        if (detRowId != null) deleteIds.add(detRowId);
                        break;
                    case ISCREATED:
                        if (dto.getComodityPoid() == null && (dto.getCargoDescription() == null || dto.getCargoDescription().isBlank())) break;
                        detRowId = generalDtlRepository.getNextDetRowId(transactionPoid);
                        ExportShipBlManifestGeneralDtl newEntity = mapper.mapGeneralCargoToEntity(dto, transactionPoid, detRowId, userId);
                        generalDtlRepository.save(newEntity);
                        summaryEntries.add(String.format(LOG_ROW_CREATED_FORMAT, "General Cargo Detail", detRowId));
                        break;
                    case ISUPDATED:
                        if (detRowId != null) {
                            ExportShipBlManifestGeneralDtl entity = generalDtlRepository.findById(new ExportShipBlManifestGeneralDtlId(transactionPoid, detRowId))
                                .orElseThrow(() -> new RuntimeException("General cargo detail not found"));
                            ExportShipBlManifestGeneralDtl oldEntity = new ExportShipBlManifestGeneralDtl();
                            BeanUtils.copyProperties(entity, oldEntity);
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
                            logRequests.add(new LogRequestDto<>(oldEntity, entity, ExportShipBlManifestGeneralDtl.class,
                                    docId, docKeyPoid, String.format(LOG_KEY_ID_FORMAT, transactionPoid, detRowId)));
                        }
                        break;
                }
            }
            if (!deleteIds.isEmpty()) {
                generalDtlRepository.deleteByTransactionPoidAndDetRowIds(transactionPoid, deleteIds);
            }
            logSummaryEntries(summaryEntries, docId, docKeyPoid);
            logDetailChanges(logRequests);
        }
    }

    // ========== Container Details Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<ContainerDetailDto> getContainerDetails(Long transactionPoid) {
        log.info("Getting container details for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        return fetchContainerDetails(transactionPoid);
    }

    private List<ContainerDetailDto> fetchContainerDetails(Long transactionPoid) {
        List<ExportShipBlManifestContainerDtl> entities = containerDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<ContainerDetailDto> dtos = mapper.mapContainerListToDto(entities);
        enrichContainerDetailsWithLovData(dtos);
        return dtos;
    }

    @Override
    public List<ContainerDetailDto> updateContainerDetails(Long transactionPoid, List<ContainerDetailDto> request) {
        log.info("Updating container details for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        doUpdateContainerDetails(transactionPoid, request, getCurrentUser());
        recalculateHeaderTotals(transactionPoid);
        return fetchContainerDetails(transactionPoid);
    }

    private void doUpdateContainerDetails(Long transactionPoid, List<ContainerDetailDto> request, String userId) {
        if (request != null) {
            String docId = resolveDocId();
            String docKeyPoid = transactionPoid.toString();
            List<Long> deleteIds = new ArrayList<>();
            List<String> summaryEntries = new ArrayList<>();
            List<LogRequestDto<ExportShipBlManifestContainerDtl>> logRequests = new ArrayList<>();
            for (ContainerDetailDto dto : request) {
                ActionType action = resolveAction(dto.getActionType());
                Long detRowId = dto.getDetRowId();

                switch (action) {
                    case ISDELETED:
                        if (detRowId != null) deleteIds.add(detRowId);
                        break;
                    case ISCREATED:
                        detRowId = containerDtlRepository.getNextDetRowId(transactionPoid);
                        ExportShipBlManifestContainerDtl newEntity = mapper.mapContainerToEntity(dto, transactionPoid, detRowId, userId);
                        containerDtlRepository.save(newEntity);
                        summaryEntries.add(String.format(LOG_ROW_CREATED_FORMAT, "Container Detail", detRowId));
                        break;
                    case ISUPDATED:
                        if (detRowId != null) {
                            ExportShipBlManifestContainerDtl entity = containerDtlRepository.findById(new ExportShipBlManifestContainerDtlId(transactionPoid, detRowId))
                                .orElseThrow(() -> new RuntimeException("Container detail not found"));
                            ExportShipBlManifestContainerDtl oldEntity = new ExportShipBlManifestContainerDtl();
                            BeanUtils.copyProperties(entity, oldEntity);
                            mapper.mapContainerUpdatesToEntity(dto, entity);
                            containerDtlRepository.save(entity);
                            logRequests.add(new LogRequestDto<>(oldEntity, entity, ExportShipBlManifestContainerDtl.class,
                                    docId, docKeyPoid, String.format(LOG_KEY_ID_FORMAT, transactionPoid, detRowId)));
                        }
                        break;
                }
            }
            if (!deleteIds.isEmpty()) {
                containerDtlRepository.deleteByTransactionPoidAndDetRowIds(transactionPoid, deleteIds);
            }
            logSummaryEntries(summaryEntries, docId, docKeyPoid);
            logDetailChanges(logRequests);
        }
    }

    // ========== Cargo Description and Marks Operations ==========

    @Override
    @Transactional(readOnly = true)
    public List<CargoDescriptionDto> getCargoDescription(Long transactionPoid) {
        log.info("Getting cargo description for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        return fetchCargoDescription(transactionPoid);
    }

    private List<CargoDescriptionDto> fetchCargoDescription(Long transactionPoid) {
        List<ExportShipBlManifestCargoDtl> entities = cargoDtlRepository.findCargoRowsByType(transactionPoid, "CARGO");
        return mapper.mapCargoDescriptionListToDto(entities);
    }

    @Override
    public List<CargoDescriptionDto> updateCargoDescription(Long transactionPoid, List<CargoDescriptionDto> request) {
        log.info("Updating cargo description for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        doUpdateCargoDescription(transactionPoid, request, getCurrentUser());
        return fetchCargoDescription(transactionPoid);
    }

    private void doUpdateCargoDescription(Long transactionPoid, List<CargoDescriptionDto> request, String userId) {
        if (request != null) {
            String docId = resolveDocId();
            String docKeyPoid = transactionPoid.toString();
            List<Long> deleteIds = new ArrayList<>();
            List<String> summaryEntries = new ArrayList<>();
            List<LogRequestDto<ExportShipBlManifestCargoDtl>> logRequests = new ArrayList<>();
            for (CargoDescriptionDto dto : request) {
                ActionType action = resolveAction(dto.getActionType());
                Long detRowId = dto.getDetRowId();

                switch (action) {
                    case ISDELETED:
                        if (detRowId != null) deleteIds.add(detRowId);
                        break;
                    case ISCREATED:
                        detRowId = cargoDtlRepository.getNextDetRowId(transactionPoid, "CARGO");
                        ExportShipBlManifestCargoDtl newEntity = mapper.mapCargoDescriptionToEntity(dto, transactionPoid, detRowId, userId);
                        newEntity.setDescriptionType("CARGO");
                        cargoDtlRepository.save(newEntity);
                        summaryEntries.add(String.format(LOG_ROW_CREATED_FORMAT, "Cargo Description Detail", detRowId));
                        break;
                    case ISUPDATED:
                        if (detRowId != null) {
                            ExportShipBlManifestCargoDtl entity = cargoDtlRepository.findById(new ExportShipBlManifestCargoDtlId(transactionPoid, detRowId, "CARGO"))
                                .orElseThrow(() -> new RuntimeException("Cargo description detail not found"));
                            ExportShipBlManifestCargoDtl oldEntity = new ExportShipBlManifestCargoDtl();
                            BeanUtils.copyProperties(entity, oldEntity);
                            if (dto.getCargoDescription() != null) entity.setCargoDescription(dto.getCargoDescription());
                            cargoDtlRepository.save(entity);
                            logRequests.add(new LogRequestDto<>(oldEntity, entity, ExportShipBlManifestCargoDtl.class,
                                    docId, docKeyPoid, String.format(LOG_KEY_ID_FORMAT, transactionPoid, detRowId)));
                        }
                        break;
                }
            }
            if (!deleteIds.isEmpty()) {
                cargoDtlRepository.deleteByTransactionPoidAndDescriptionTypeAndDetRowIds(transactionPoid, "CARGO", deleteIds);
            }
            logSummaryEntries(summaryEntries, docId, docKeyPoid);
            logDetailChanges(logRequests);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExportManifestCargoContainerResponse getCargoContainerDetails(Long transactionPoid) {
        log.info("Getting cargo and container details for Export BL: {}", transactionPoid);

        validateHeaderExists(transactionPoid);

        ExportManifestCargoContainerResponse response = new ExportManifestCargoContainerResponse();
        response.setCargoDescription(fetchCargoDescription(transactionPoid));
        response.setCargoMarks(fetchCargoMarks(transactionPoid));
        response.setContainerDetails(fetchContainerDetails(transactionPoid));
        response.setSimpleCargoDescription(joinDescriptionStrings(
                cargoDtlRepository.findDescriptionStringsByType(transactionPoid, "CARGO")));
        response.setSimpleCargoMarks(joinDescriptionStrings(
                cargoDtlRepository.findDescriptionStringsByType(transactionPoid, "MARK")));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CargoMarksDto> getCargoMarks(Long transactionPoid) {
        log.info("Getting cargo marks for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        return fetchCargoMarks(transactionPoid);
    }

    private List<CargoMarksDto> fetchCargoMarks(Long transactionPoid) {
        List<ExportShipBlManifestCargoDtl> entities = cargoDtlRepository.findCargoRowsByType(transactionPoid, "MARK");
        return mapper.mapCargoMarksListToDto(entities);
    }

    @Override
    public List<CargoMarksDto> updateCargoMarks(Long transactionPoid, List<CargoMarksDto> request) {
        log.info("Updating cargo marks for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        doUpdateCargoMarks(transactionPoid, request, getCurrentUser());
        return fetchCargoMarks(transactionPoid);
    }

    private void doUpdateCargoMarks(Long transactionPoid, List<CargoMarksDto> request, String userId) {
        if (request != null) {
            String docId = resolveDocId();
            String docKeyPoid = transactionPoid.toString();
            List<Long> deleteIds = new ArrayList<>();
            List<String> summaryEntries = new ArrayList<>();
            List<LogRequestDto<ExportShipBlManifestCargoDtl>> logRequests = new ArrayList<>();
            for (CargoMarksDto dto : request) {
                ActionType action = resolveAction(dto.getActionType());
                Long detRowId = dto.getDetRowId();

                switch (action) {
                    case ISDELETED:
                        if (detRowId != null) deleteIds.add(detRowId);
                        break;
                    case ISCREATED:
                        detRowId = cargoDtlRepository.getNextDetRowId(transactionPoid, "MARK");
                        ExportShipBlManifestCargoDtl newEntity = mapper.mapCargoMarksToEntity(dto, transactionPoid, detRowId, userId);
                        newEntity.setDescriptionType("MARK");
                        cargoDtlRepository.save(newEntity);
                        summaryEntries.add(String.format(LOG_ROW_CREATED_FORMAT, "Cargo Marks Detail", detRowId));
                        break;
                    case ISUPDATED:
                        if (detRowId != null) {
                            ExportShipBlManifestCargoDtl entity = cargoDtlRepository.findById(new ExportShipBlManifestCargoDtlId(transactionPoid, detRowId, "MARK"))
                                .orElseThrow(() -> new RuntimeException("Cargo marks detail not found"));
                            ExportShipBlManifestCargoDtl oldEntity = new ExportShipBlManifestCargoDtl();
                            BeanUtils.copyProperties(entity, oldEntity);
                            if (dto.getCargoDescription() != null) entity.setCargoDescription(dto.getCargoDescription());
                            cargoDtlRepository.save(entity);
                            logRequests.add(new LogRequestDto<>(oldEntity, entity, ExportShipBlManifestCargoDtl.class,
                                    docId, docKeyPoid, String.format(LOG_KEY_ID_FORMAT, transactionPoid, detRowId)));
                        }
                        break;
                }
            }
            if (!deleteIds.isEmpty()) {
                cargoDtlRepository.deleteByTransactionPoidAndDescriptionTypeAndDetRowIds(transactionPoid, "MARK", deleteIds);
            }
            logSummaryEntries(summaryEntries, docId, docKeyPoid);
            logDetailChanges(logRequests);
        }
    }

    // ========== Charge Details Operations ==========

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getChargeDetails(Long transactionPoid) {
        log.info("Getting charge details for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        return fetchChargeDetails(transactionPoid);
    }

    private Map<String, Object> fetchChargeDetails(Long transactionPoid) {
        List<ExportShipBlManifestChargesDtl> entities = chargesDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<ChargeDetailDto> dtos = mapper.mapChargeListToDto(entities);
        enrichChargeDetailsWithLovData(dtos);

        BigDecimal totalBuyAmount = BigDecimal.ZERO;
        BigDecimal totalSaleAmount = BigDecimal.ZERO;
        for (ChargeDetailDto dto : dtos) {
            if (dto.getBuyAmount() != null) totalBuyAmount = totalBuyAmount.add(dto.getBuyAmount());
            if (dto.getSaleAmount() != null) totalSaleAmount = totalSaleAmount.add(dto.getSaleAmount());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("chargeDetails", dtos);
        response.put("totalBuyAmount", totalBuyAmount);
        response.put("totalSaleAmount", totalSaleAmount);
        return response;
    }

    @Override
    public Map<String, Object> updateChargeDetails(Long transactionPoid, List<ChargeDetailDto> request) {
        log.info("Updating charge details for Export BL: {}", transactionPoid);
        validateHeaderExists(transactionPoid);
        doUpdateChargeDetails(transactionPoid, request, getCurrentUser());
        recalculateHeaderTotals(transactionPoid);
        return fetchChargeDetails(transactionPoid);
    }

    private void doUpdateChargeDetails(Long transactionPoid, List<ChargeDetailDto> request, String userId) {
        if (request != null) {
            String docId = resolveDocId();
            String docKeyPoid = transactionPoid.toString();
            List<Long> deleteIds = new ArrayList<>();
            List<String> summaryEntries = new ArrayList<>();
            List<LogRequestDto<ExportShipBlManifestChargesDtl>> logRequests = new ArrayList<>();
            for (ChargeDetailDto dto : request) {
                ActionType action = resolveAction(dto.getActionType());
                Long detRowId = dto.getDetRowId();

                switch (action) {
                    case ISDELETED:
                        if (detRowId != null) deleteIds.add(detRowId);
                        break;
                    case ISCREATED:
                        if (dto.getChargePoid() == null) break;
                        blManifestValidationService.validateChargeTypeMandatory(dto.getChargeType());
                        blManifestValidationService.validateFreightTypeMandatory(dto.getFreightType());
                        detRowId = chargesDtlRepository.getNextDetRowId(transactionPoid);
                        ExportShipBlManifestChargesDtl newEntity = mapper.mapChargeToEntity(dto, transactionPoid, detRowId, userId);
                        chargesDtlRepository.save(newEntity);
                        summaryEntries.add(String.format(LOG_ROW_CREATED_FORMAT, "Charge Detail", detRowId));
                        break;
                    case ISUPDATED:
                        if (detRowId != null) {
                            ExportShipBlManifestChargesDtl entity = chargesDtlRepository.findById(new ExportShipBlManifestChargesDtlId(transactionPoid, detRowId))
                                .orElseThrow(() -> new RuntimeException("Charge detail not found"));
                            ExportShipBlManifestChargesDtl oldEntity = new ExportShipBlManifestChargesDtl();
                            BeanUtils.copyProperties(entity, oldEntity);
                            mapper.mapChargeUpdatesToEntity(dto, entity);
                            blManifestValidationService.validateChargeTypeMandatory(entity.getChargeType());
                            blManifestValidationService.validateFreightTypeMandatory(entity.getFreightType());
                            chargesDtlRepository.save(entity);
                            logRequests.add(new LogRequestDto<>(oldEntity, entity, ExportShipBlManifestChargesDtl.class,
                                    docId, docKeyPoid, String.format(LOG_KEY_ID_FORMAT, transactionPoid, detRowId)));
                        }
                        break;
                }
            }
            if (!deleteIds.isEmpty()) {
                chargesDtlRepository.deleteByTransactionPoidAndDetRowIds(transactionPoid, deleteIds);
            }
            logSummaryEntries(summaryEntries, docId, docKeyPoid);
            logDetailChanges(logRequests);
        }
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

    // ========== Special Operations (to be continued in next part) ==========

    @Override
    public Map<String, Object> loadBooking(Long transactionPoid, LoadBookingRequest request) {
        // TODO: Implement load booking from MATE
        throw new UnsupportedOperationException("Load booking not yet implemented");
    }

    @Override
    public byte[] generateBlPrint(Long transactionPoid, GenerateBlPrintRequest request,String docId) throws Exception {
    	
    	log.info("Bl print : {}", transactionPoid);
    	
    	Long groupPoid = UserContext.getGroupPoid();
    	Long companyPoid = UserContext.getCompanyPoid();
    	ExportShipBlManifestHdr entity = hdrRepository
                .findActiveExportBlByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ValidationException("Export BL not found with ID: " + transactionPoid));
    	if(entity.getBlOrginalPrint()!=null && entity.getBlOrginalPrint().equalsIgnoreCase("Y")) {
    		throw new ValidationException("BL already printed");
    	}
    	
    	String jrxmlFile = customBLRepository.getBlPrintReport(groupPoid, companyPoid, docId, transactionPoid, "BL_PRINT");
    	if (jrxmlFile == null || jrxmlFile.isBlank()) {
    		throw new ValidationException("BL print report template not configured for this BL");
    	}
    	if (!jrxmlFile.toLowerCase().contains(".jrxml")) {
    		throw new ValidationException(jrxmlFile.trim());
    	}
    	Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-140");
		params.put("DOC_KEY_POID", String.valueOf(transactionPoid));
		JasperReport mainReport = printService.load("Shipping/" + jrxmlFile);
		params.put("DRAFT_ORIGINAL", request.getDraftOriginalParam());
		return printService.fillReportToPdf(mainReport, params, dataSource);
    	
    }

    @Override
    public byte[] generateManifest(Long transactionPoid, GenerateManifestRequest request, String docId) throws Exception {
    	Map<String, Object> params = printService.buildBaseParams(transactionPoid, docId);
		params.put("P_FREIGHTCARGO", request.getFreightCargoParam());
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
        
        customBLRepository.exportEdi(transactionPoid, userPoid);
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
            String status = customBLRepository.validateBlNumberDuplicate(
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
        
        customBLRepository.processAfterSave(groupPoid, companyPoid, transactionPoid, null, "AUTOSUMWEIGHTPEXPORT", userPoid);
    }

    @Override
    public BlStatusResponse getBlStatus(Long transactionPoid) {
        log.info("Getting BL status for Export BL: {}", transactionPoid);
        
        validateHeaderExists(transactionPoid);
        
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        
        String status = customBLRepository.getBlStatus(groupPoid, companyPoid, userPoid, transactionPoid);

        BlStatusResponse response = new BlStatusResponse();
        response.setStatus(status);
        response.setDisplayInfo(status);
        response.setStatusDetails(parseBlStatus(status));

        return response;
    }

    @Override
    public Map<String, Object> quotationAfterBrowse(Long transactionPoid, QuotationAfterBrowseRequest request) {
        log.info("Processing quotation after browse for Export BL: {}", transactionPoid);
        
        Long actualTransactionPoid = transactionPoid != null ? transactionPoid : 0L; // Use 0 for new records
        
        customBLRepository.processQuotationAfterBrowse(
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

    private String resolveDisplayTopInfoExportBLS(Long transactionPoid) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        try {
            return resolveDisplayTopInfoFromStatus(
                    customBLRepository.getBlStatus(groupPoid, companyPoid, userPoid, transactionPoid));
        } catch (Exception e) {
            log.warn("Failed to fetch display top info for Export BL: {}", transactionPoid, e);
            return "NEW";
        }
    }

    private String resolveDisplayTopInfoFromStatus(String status) {
        if (status == null || status.isBlank() || "FALSE".equalsIgnoreCase(status)) return "NEW";
        return status;
    }

    private BlStatusResponse.StatusDetails parseBlStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank() || "FALSE".equalsIgnoreCase(rawStatus)) {
            return BlStatusResponse.StatusDetails.builder().build();
        }

        BlStatusResponse.StatusDetails.StatusDetailsBuilder builder = BlStatusResponse.StatusDetails.builder();
        String[] tokens = rawStatus.split(",");

        for (String token : tokens) {
            String value = token == null ? "" : token.trim();
            if (value.isEmpty()) {
                continue;
            }

            value = value.replaceFirst("^-+\\s*", "");

            if (value.startsWith("JobNo:")) {
                builder.jobNo(extractStatusValue(value, "JobNo:"));
            } else if (value.startsWith("Line:")) {
                builder.line(extractStatusValue(value, "Line:"));
            } else if (value.startsWith("Vessel:")) {
                builder.vessel(extractStatusValue(value, "Vessel:"));
            } else if (value.startsWith("VoyageNo:")) {
                builder.voyageNo(extractStatusValue(value, "VoyageNo:"));
            } else if (value.startsWith("ArrivalDt:")) {
                builder.arrivalDt(extractStatusValue(value, "ArrivalDt:"));
            } else if (value.startsWith("SailDt:")) {
                builder.sailDt(extractStatusValue(value, "SailDt:"));
            } else if (value.startsWith("BLNO:")) {
                builder.blNo(extractStatusValue(value, "BLNO:"));
            } else if (!value.contains(":")) {
                builder.jobStatus(value);
            }
        }

        return builder.build();
    }

    private String extractStatusValue(String text, String prefix) {
        return text.substring(prefix.length()).trim();
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
                        entity.getShipperPoid().longValue(), "CUSTOMER_MASTER", groupPoid, companyPoid, userPoid));
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
     * Helper to fetch LOV item using an in-request cache map to avoid duplicate calls.
     */
    private LovItem getLovItemWithCache(Map<String, Map<Long, LovItem>> cache, Long poid, String masterType, Long groupPoid, Long companyPoid, Long userPoid) {
        if (poid == null) {
            return null;
        }
        Map<Long, LovItem> innerCache = cache.computeIfAbsent(masterType, k -> new HashMap<>());
        if (!innerCache.containsKey(poid)) {
            try {
                LovItem item = lovService.getLovItemByPoid(poid, masterType, groupPoid, companyPoid, userPoid);
                innerCache.put(poid, item);
            } catch (Exception e) {
                log.warn("Failed to fetch LOV for masterType: {} and poid: {}", masterType, poid, e);
                innerCache.put(poid, null);
            }
        }
        return innerCache.get(poid);
    }

    /**
     * Helper to fetch LOV item by code using an in-request cache map to avoid duplicate calls.
     */
    private LovItem getLovItemByCodeWithCache(Map<String, Map<String, LovItem>> cache, String code, String lovName, Long groupPoid, Long companyPoid, Long userPoid) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        Map<String, LovItem> innerCache = cache.computeIfAbsent(lovName, k -> new HashMap<>());
        if (!innerCache.containsKey(code)) {
            try {
                LovItem item = lovService.getLovItemByCode(code, lovName, groupPoid, companyPoid, userPoid);
                innerCache.put(code, item);
            } catch (Exception e) {
                log.warn("Failed to fetch LOV for lovName: {} and code: {}", lovName, code, e);
                innerCache.put(code, null);
            }
        }
        return innerCache.get(code);
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
        Map<String, Map<Long, LovItem>> cache = new HashMap<>();

        for (GeneralCargoDetailDto dto : dtos) {
            try {
                if (dto.getComodityPoid() != null) {
                    dto.setComodityDet(getLovItemWithCache(cache, dto.getComodityPoid(), "COMODITY", groupPoid, companyPoid, userPoid));
                }
                if (dto.getDestinationPortPoid() != null) {
                    dto.setDestinationPortDet(getLovItemWithCache(cache, dto.getDestinationPortPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
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
        Map<String, Map<Long, LovItem>> poidCache = new HashMap<>();
        Map<String, Map<String, LovItem>> codeCache = new HashMap<>();

        for (ContainerDetailDto dto : dtos) {
            try {
                // POID-based LOVs
                if (dto.getComodityPoid() != null) {
                    dto.setComodityDet(getLovItemWithCache(poidCache, dto.getComodityPoid(), "COMODITY", groupPoid, companyPoid, userPoid));
                }
                if (dto.getDestinationPortPoid() != null) {
                    dto.setDestinationPortDet(getLovItemWithCache(poidCache, dto.getDestinationPortPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
                }
                // CODE-based LOVs
                if (dto.getEquipmentIsoType() != null) {
                    dto.setEquipmentIsoTypeDet(getLovItemByCodeWithCache(codeCache, dto.getEquipmentIsoType(), "CONTAINER_TYPE_MASTER", groupPoid, companyPoid, userPoid));
                }
                if (dto.getImcoClassType() != null) {
                    dto.setImcoClassTypeDet(getLovItemByCodeWithCache(codeCache, dto.getImcoClassType(), "IMCO_CLASS", groupPoid, companyPoid, userPoid));
                }
                if (dto.getOogType() != null) {
                    dto.setOogTypeDet(getLovItemByCodeWithCache(codeCache, dto.getOogType(), "OOG_TYPE", groupPoid, companyPoid, userPoid));
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
        Map<String, Map<Long, LovItem>> cache = new HashMap<>();
        Map<String, Map<String, LovItem>> codeCache = new HashMap<>();

        for (ChargeDetailDto dto : dtos) {
            try {
                if (dto.getChargePoid() != null) {
                    dto.setChargeDet(getLovItemWithCache(cache, dto.getChargePoid(), "CHARGE_MASTER", groupPoid, companyPoid, userPoid));
                }
                if (dto.getPaidAtPortPoid() != null) {
                    dto.setPaidAtPortDet(getLovItemWithCache(cache, dto.getPaidAtPortPoid(), "PORT_MASTER", groupPoid, companyPoid, userPoid));
                }
                if (dto.getReceiptInvoicePoid() != null) {
                    dto.setReceiptInvoiceDet(getLovItemWithCache(cache, dto.getReceiptInvoicePoid(), "MANIFEST_RECEIPT_INVOICE", groupPoid, companyPoid, userPoid));
                }
                if (dto.getTaxPoid() != null) {
                    dto.setTaxDet(getLovItemWithCache(cache, dto.getTaxPoid(), "TAX_MASTER", groupPoid, companyPoid, userPoid));
                }
                if (dto.getChargeType() != null) {
                    dto.setChargeTypeDet(getLovItemByCodeWithCache(codeCache, dto.getChargeType(), "CHARGE_TYPE", groupPoid, companyPoid, userPoid));
                }
                if (dto.getFreightType() != null) {
                    dto.setFreightTypeDet(getLovItemByCodeWithCache(codeCache, dto.getFreightType(), "SHIP_FREIGHT_TYPE", groupPoid, companyPoid, userPoid));
                }
                if (dto.getChargeBasisOn() != null) {
                    dto.setChargeBasisOnDet(getLovItemByCodeWithCache(codeCache, dto.getChargeBasisOn(), "CONTAINER_TYPE_MASTER", groupPoid, companyPoid, userPoid));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch LOV data for charge detail with detRowId: {}", dto.getDetRowId(), e);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExportManifestAddressDto getAddressDetails(Long addressMasterPoid, String addressType) {
        log.info("Fetching address details for addressMasterPoid: {} and addressType: {}", addressMasterPoid, addressType);
        
        List<AddressDetails> addressList = addressDetailsRepository.findByAddressMasterPoidAndAddressType(addressMasterPoid, addressType);
        if (addressList == null || addressList.isEmpty()) {
            if (addressType != null && !addressType.equalsIgnoreCase("MAIN")) {
                log.info("Address of type {} not found. Falling back to MAIN.", addressType);
                addressList = addressDetailsRepository.findByAddressMasterPoidAndAddressType(addressMasterPoid, "MAIN");
            }
        }
        
        if (addressList == null || addressList.isEmpty()) {
            log.info("Address of type MAIN not found. Fetching any address for master poid.");
            addressList = addressDetailsRepository.findByAddressMasterPoid(addressMasterPoid);
        }
        
        if (addressList == null || addressList.isEmpty()) {
            throw new RuntimeException("Address details not found for master poid: " + addressMasterPoid);
        }
        
        return mapper.mapAddressToDto(addressList.getFirst());
    }
}
