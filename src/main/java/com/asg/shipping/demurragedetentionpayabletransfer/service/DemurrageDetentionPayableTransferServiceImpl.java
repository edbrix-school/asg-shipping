package com.asg.shipping.demurragedetentionpayabletransfer.service;


import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.annotation.PerformGlPosting;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.*;
import com.asg.shipping.demurragedetentionpayabletransfer.entity.*;
import com.asg.shipping.demurragedetentionpayabletransfer.repository.*;
import com.asg.shipping.demurragedetentionpayabletransfer.util.DemurrageDetentionPayableTransferMapper;
import com.asg.shipping.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Service implementation for Demurrage/Detention Payable Transfer operations.
 *
 * Legacy reference: DemurrageDettnTransferPayablePageBean.java
 * Legacy FE reference: DemurrageDettnTransferPayablePage.jsff
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemurrageDetentionPayableTransferServiceImpl implements DemurrageDetentionPayableTransferService {

    private final ShipDemDetnTransferHdrRepository headerRepository;
    private final ShipDemDetnTransferDtlRepository transferDtlRepository;
    private final ShipDemDtnTransferBillDtlRepository billDtlRepository;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final LovDataService lovService;
    private final DemurrageDetentionPayableTransferMapper mapper;
    private final LoggingService loggingService;
    private final JdbcTemplate jdbcTemplate;

    // Legacy DocId (DemurrageDettnTransferPayablePage.jsff line 204)
    private static final String DOC_ID = "100-151";

    private static final String ACTION_NO_CHANGE = "noChange";
    private static final String ACTION_IS_CREATED = "isCreated";
    private static final String ACTION_IS_UPDATED = "isUpdated";
    private static final String ACTION_IS_DELETED = "isDeleted";

    // GL codes used when querying VW_SHIP_BILLWISE_ACCOUNT_TRN (legacy bean lines 285-289)
    private static final String GL_CODE_IMPORT = "LINE_DEM";
    private static final String GL_CODE_EXPORT = "LINE_DET";

    // Fallback income GL POID (legacy: common.GetParameterValue("DEM_DET_ACCOUNT_INCOME","Group","1","13653"))
    private static final long DEFAULT_INCOME_GL_POID = 13653L;

    private static final String LOG_KEY_ID = "KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s";

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchDemurrageDetentionPayableTransfer(String docId, com.asg.common.lib.dto.FilterRequestDto request, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Searching demurrage/detention payable transfer records with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);

        if (filters != null) {
            filters = filters.stream()
                    .filter(filter -> filter != null && filter.searchField() != null)
                    .collect(Collectors.toList());
        }

        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "TRANSACTION_POID",
                "DOC_REF"
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
    public DemurrageDetentionPayableTransferDto getDemurrageDetentionPayableTransfer(Long id) {
        log.info("Getting demurrage/detention payable transfer with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        ShipDemDetnTransferHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString()));

        if ("Y".equals(entity.getDeleted())) {
            throw new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString());
        }

        List<ShipDemDetnTransferDtl> transferDetails = transferDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipDemDtnTransferBillDtl> billDetails = billDtlRepository.findByTransactionPoidOrderByDetRowId(id);

        DemurrageDetentionPayableTransferDto dto = mapper.mapToDto(entity);
        dto.setTransferDetails(mapper.mapTransferDtlListToDto(transferDetails));
        dto.setBillDetails(mapper.mapBillDtlListToDto(billDetails));

        enrichLovData(dto);

        log.info("Successfully retrieved demurrage/detention payable transfer with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    @PerformGlPosting
    public DemurrageDetentionPayableTransferDto createDemurrageDetentionPayableTransfer(
            DemurrageDetentionPayableTransferCreateDTO dto, Long companyPoid, Long groupPoid) {
        log.info("Creating demurrage/detention payable transfer");

        // GAP-5: pre-save validation — billwise rows must exist (legacy DocumentBeforeSave lines 540-556)
        validateCreateDTO(dto, companyPoid, groupPoid);

        // Auto-populate GL accounts when LINE_POID and BL_TYPE are provided
        Long payableGlPoid = dto.getPayableGlPoid();
        Long incomeGlPoid = dto.getIncomeGlPoid();

        if (dto.getLinePoid() != null && dto.getBlType() != null) {
            String defaultPayableGl = callProcDemDenSetDefault(dto.getLinePoid(), dto.getBlType());
            if (defaultPayableGl != null && !defaultPayableGl.equals("NO_DATA")) {
                payableGlPoid = Long.parseLong(defaultPayableGl);
            }
            if (incomeGlPoid == null) {
                incomeGlPoid = getIncomeGlPoidFromParameter(groupPoid);
            }
        }

        if (payableGlPoid == null) {
            throw new ValidationException("Payable GL POID is required");
        }
        if (incomeGlPoid == null) {
            throw new ValidationException("Income GL POID is required");
        }

        ShipDemDetnTransferHdr entity = new ShipDemDetnTransferHdr();
        mapper.mapCreateDTOToEntity(dto, entity, groupPoid, companyPoid);
        entity.setPayableGlPoid(payableGlPoid);
        entity.setIncomeGlPoid(incomeGlPoid);

        // GAP-6: generate DOC_REF via RTN_GLOBAL_SEQ_NO (legacy uses framework auto-call)
        String docRef = generateDocRef(companyPoid);
        entity.setDocRef(docRef);

        if (entity.getEmptyFromDate() != null && entity.getEmptyToDate() != null) {
            if (entity.getEmptyToDate().isBefore(entity.getEmptyFromDate())) {
                throw new ValidationException("Empty To Date must be greater than or equal to Empty From Date");
            }
        }

        ShipDemDetnTransferHdr saved = headerRepository.save(entity);

        createDetailRecords(saved.getTransactionPoid(), dto.getTransferDetails(), dto.getBillDetails());

        List<ShipDemDetnTransferDtl> transferDetails = transferDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());
        List<ShipDemDtnTransferBillDtl> billDetails = billDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());

        DemurrageDetentionPayableTransferDto result = mapper.mapToDto(saved);
        result.setTransferDetails(mapper.mapTransferDtlListToDto(transferDetails));
        result.setBillDetails(mapper.mapBillDtlListToDto(billDetails));
        enrichLovData(result);


        loggingService.createLogSummaryEntry("400-110", saved.getTransactionPoid().toString(), String.format("%s %s", LogDetailsEnum.CREATED, saved.getDocRef()));

        log.info("Successfully created demurrage/detention payable transfer with id: {}", saved.getTransactionPoid());
        return result;
    }

    @Override
    @Transactional
    @PerformGlPosting
    public DemurrageDetentionPayableTransferDto updateDemurrageDetentionPayableTransfer(
            Long id, DemurrageDetentionPayableTransferUpdateDTO dto, Long companyPoid, Long groupPoid) {
        log.info("Updating demurrage/detention payable transfer with id: {}", id);

        ShipDemDetnTransferHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString()));

        if ("Y".equals(entity.getDeleted())) {
            throw new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString());
        }

        // GAP-5: pre-save validation for update
        validateUpdateDTO(dto, id, companyPoid, groupPoid);

        boolean lineOrBlTypeChanged = (dto.getLinePoid() != null && !dto.getLinePoid().equals(entity.getLinePoid())) ||
                (dto.getBlType() != null && !dto.getBlType().equals(entity.getBlType()));

        if (lineOrBlTypeChanged) {
            Long linePoid = dto.getLinePoid() != null ? dto.getLinePoid() : entity.getLinePoid();
            String blType = dto.getBlType() != null ? dto.getBlType() : entity.getBlType();

            if (linePoid != null && blType != null) {
                String defaultPayableGl = callProcDemDenSetDefault(linePoid, blType);
                if (defaultPayableGl != null && !defaultPayableGl.equals("NO_DATA")) {
                    dto.setPayableGlPoid(Long.parseLong(defaultPayableGl));
                }
            }
        }

        ShipDemDetnTransferHdr oldEntity = ShipDemDetnTransferHdr.builder()
                .transactionDate(entity.getTransactionDate())
                .docRef(entity.getDocRef())
                .linePoid(entity.getLinePoid())
                .blType(entity.getBlType())
                .emptyFromDate(entity.getEmptyFromDate())
                .emptyToDate(entity.getEmptyToDate())
                .payableGlPoid(entity.getPayableGlPoid())
                .incomeGlPoid(entity.getIncomeGlPoid())
                .build();

        mapper.mapUpdateDTOToEntity(dto, entity);

        if (entity.getEmptyFromDate() != null && entity.getEmptyToDate() != null) {
            if (entity.getEmptyToDate().isBefore(entity.getEmptyFromDate())) {
                throw new ValidationException("Empty To Date must be greater than or equal to Empty From Date");
            }
        }

        ShipDemDetnTransferHdr saved = headerRepository.save(entity);

        loggingService.logChanges(oldEntity, saved, ShipDemDetnTransferHdr.class, com.asg.common.lib.security.util.UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        updateDetailRecords(id, dto.getTransferDetails(), dto.getBillDetails());

        List<ShipDemDetnTransferDtl> transferDetails = transferDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());
        List<ShipDemDtnTransferBillDtl> billDetails = billDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());

        DemurrageDetentionPayableTransferDto result = mapper.mapToDto(saved);
        result.setTransferDetails(mapper.mapTransferDtlListToDto(transferDetails));
        result.setBillDetails(mapper.mapBillDtlListToDto(billDetails));
        enrichLovData(result);

        log.info("Successfully updated demurrage/detention payable transfer with id: {}", id);
        return result;
    }

    @Override
    @Transactional
    public void deleteDemurrageDetentionPayableTransfer(Long id, Long companyPoid, Long groupPoid, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting demurrage/detention payable transfer with id: {}", id);

        ShipDemDetnTransferHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString()));

        if ("Y".equals(entity.getDeleted())) {
            log.info("Demurrage/Detention Payable Transfer with id: {} is already deleted", id);
            return;
        }

        documentDeleteService.deleteDocument(
                id,
                "SHIP_DEM_DETN_TRANSFER_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                LocalDate.now()
        );

        entity.setDeleted("Y");
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        headerRepository.save(entity);

        loggingService.createLogSummaryEntry("400-110", id.toString(),
                String.format("%s %s", LogDetailsEnum.DELETED, entity.getDocRef()));

        log.info("Successfully deleted demurrage/detention payable transfer with id: {}", id);
    }

    /**
     * Process For Filtered Data — post-create variant (legacy bean lines 117-225: ProcessForDataAction).
     * Wipes existing transfer details and re-populates from VW_SHIP_DEM_DTN_TRANSFER using
     * BL_TYPE, LINE_POID, COMPANY_POID filters with NOT-EXISTS exclusion for already-selected containers.
     * Date range filter is intentionally omitted (legacy EmptyFromDate/EmptyToDate are rendered=false).
     */
    @Override
    @Transactional
    public DemurrageDetentionPayableTransferDto processData(Long id, ProcessDataRequestDTO request) {
        log.info("Processing data for demurrage/detention payable transfer with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        ShipDemDetnTransferHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString()));

        Long linePoid = request.getLinePoid() != null ? request.getLinePoid() : entity.getLinePoid();
        String blType = request.getBlType() != null ? request.getBlType() : entity.getBlType();

        if (linePoid == null || blType == null) {
            throw new ValidationException("Line POID and BL Type must be set in header or request");
        }

        // Wipe existing transfer details (legacy bean line 161-165)
        transferDtlRepository.deleteByTransactionPoid(id);

        // Query available containers (legacy bean lines 137-145, with GAP-10 IS NOT NULL guards)
        String sql = buildContainerQuerySql();
        List<Map<String, Object>> containers;
        try {
            containers = jdbcTemplate.queryForList(sql, blType, linePoid, companyPoid);
        } catch (Exception e) {
            log.error("Error querying VW_SHIP_DEM_DTN_TRANSFER", e);
            throw new ValidationException("Failed to load available containers: " + e.getMessage());
        }

        // Insert new rows with IsSelect='N' (legacy bean line 181)
        long detRowId = 0;
        for (Map<String, Object> row : containers) {
            detRowId++;
            transferDtlRepository.save(buildTransferDetailFromViewRow(row, id, detRowId));
        }

        log.info("Inserted {} transfer detail rows for transaction: {}", detRowId, id);

        List<ShipDemDetnTransferDtl> transferDetails = transferDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipDemDtnTransferBillDtl> billDetails = billDtlRepository.findByTransactionPoidOrderByDetRowId(id);

        DemurrageDetentionPayableTransferDto result = mapper.mapToDto(entity);
        result.setTransferDetails(mapper.mapTransferDtlListToDto(transferDetails));
        result.setBillDetails(mapper.mapBillDtlListToDto(billDetails));
        enrichLovData(result);

        return result;
    }

    /**
     * Process For Filtered Data — pre-create variant.
     * Returns the available container list without persisting. The FE includes the selected rows
     * as transferDetails in the subsequent create request.
     * Date range filter is omitted to match legacy behaviour (fields are hidden in legacy FE).
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> processDataBeforeCreate(ProcessDataRequestDTO request) {
        log.info("Processing data before create for line: {}, blType: {}", request.getLinePoid(), request.getBlType());

        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        if (request.getLinePoid() == null || request.getBlType() == null) {
            throw new ValidationException("Line POID and BL Type are required");
        }

        String sql = buildContainerQuerySql();
        try {
            List<Map<String, Object>> containers = jdbcTemplate.queryForList(sql, request.getBlType(), request.getLinePoid(), companyPoid);
            log.info("Found {} available containers", containers.size());
            return Map.of("containers", containers, "totalCount", containers.size());
        } catch (Exception e) {
            log.error("Error querying VW_SHIP_DEM_DTN_TRANSFER", e);
            throw new ValidationException("Failed to load available containers: " + e.getMessage());
        }
    }

    /**
     * Load Selected Billwise — post-create variant (legacy bean lines 244-388: loadSelectedBilliwiseActionListiner).
     * Only processes transfer detail rows where IS_SELECT='Y'.
     * Single-row branch uses VW_SHIP_BILLWISE_ACCOUNT_TRN directly.
     * Multi-row branch uses VW_AR_SH_CONTAINER_DEMG_DTTN with GET_BL_NUMBER grouping.
     */
    @Override
    @Transactional
    public DemurrageDetentionPayableTransferDto loadBillwiseData(Long id, LoadBillwiseRequestDTO request) {
        log.info("Loading bill-wise settlement data for demurrage/detention payable transfer with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        ShipDemDetnTransferHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString()));

        if ("Y".equals(entity.getDeleted())) {
            throw new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString());
        }

        // GL_CODE determined by BL_TYPE (legacy bean lines 285-289)
        if (entity.getBlType() == null) {
            throw new ValidationException("BL Type is required for loading bill-wise data. Please set BL Type in the header first.");
        }
        String glCode = resolveGlCode(entity.getBlType());

        // Only iterate rows where IsSelect='Y' (legacy bean line 277)
        List<ShipDemDetnTransferDtl> selectedDetails = transferDtlRepository
                .findByTransactionPoidOrderByDetRowId(id)
                .stream()
                .filter(d -> "Y".equalsIgnoreCase(d.getIsSelect()))
                .collect(Collectors.toList());

        // Wipe existing bill details (legacy bean lines 248-258)
        billDtlRepository.deleteByTransactionPoid(id);

        long currentDetRowId = 0;
        for (ShipDemDetnTransferDtl container : selectedDetails) {
            currentDetRowId = buildAndPersistBillRows(id, container, glCode, currentDetRowId);
        }

        List<ShipDemDetnTransferDtl> transferDetails = transferDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipDemDtnTransferBillDtl> billDetails = billDtlRepository.findByTransactionPoidOrderByDetRowId(id);

        DemurrageDetentionPayableTransferDto result = mapper.mapToDto(entity);
        result.setTransferDetails(mapper.mapTransferDtlListToDto(transferDetails));
        result.setBillDetails(mapper.mapBillDtlListToDto(billDetails));
        enrichLovData(result);

        log.info("Successfully loaded bill-wise settlement data for id: {}", id);
        return result;
    }

    /**
     * Load Selected Billwise — pre-create variant.
     * Computes bill rows from the selected containers in the request and returns them
     * without persisting. The FE includes these as billDetails in the create request.
     * Requires blType at request level and totalPayableAmount/totalIncomeAmount per container.
     */
    public Map<String, Object> loadBillwiseDataBeforeCreate(LoadBillwiseRequestDTO request) {
        int containerCount = request.getSelectedContainers() == null ? 0 : request.getSelectedContainers().size();
        log.info("Loading bill-wise data before create for {} containers with BL Type: {}",
                containerCount, request.getBlType());

        if (request.getBlType() == null || request.getBlType().trim().isEmpty()) {
            throw new ValidationException("BL Type is required for bill-wise data loading");
        }
        if (!"IMPORT".equals(request.getBlType()) && !"EXPORT".equals(request.getBlType())) {
            throw new ValidationException("BL Type must be IMPORT or EXPORT");
        }

        List<Map<String, Object>> allBillDetails = new java.util.ArrayList<>();
        String glCode = resolveGlCode(request.getBlType());
        long lastRowNumber = 0;

        for (LoadBillwiseRequestDTO.SelectedContainer container : request.getSelectedContainers()) {
            if (container.getIsSelect() != null && !"Y".equalsIgnoreCase(container.getIsSelect())) {
                log.debug("Skipping container {} — IsSelect != Y", container.getContainerNo());
                continue;
            }

            // Mirror legacy VwShipBillwiseAccountTrnView1: filter by GL_CODE and REMARKS only (no COMPANY_POID)
            List<Map<String, Object>> billwiseAccounts = queryBillwiseAccountView(glCode, container.getBlNumber());
            int billwiseCount = billwiseAccounts.size();
            log.debug("Found {} billwise account records for container: {}", billwiseCount, container.getContainerNo());

            if (billwiseCount == 0) {
                // Legacy produces no bill rows when no billwise accounts exist — skip this container
                log.warn("No billwise accounts found for container: {}, blNumber: {} — skipping (legacy behaviour)",
                        container.getContainerNo(), container.getBlNumber());
                continue;
            }

            Map<String, Object> firstBillwiseAccount = billwiseAccounts.get(0);

            if (billwiseCount == 1) {
                // Single-row branch (legacy bean lines 299-321)
                lastRowNumber++;
                Map<String, Object> billDetail = new java.util.HashMap<>();
                billDetail.put("detRowId", lastRowNumber);
                billDetail.put("checkall", "Y");
                billDetail.put("description", firstBillwiseAccount.get("REMARKS"));
                billDetail.put("billRefType", "AGAINST");
                billDetail.put("billRefno", firstBillwiseAccount.get("BILL_REF"));
                billDetail.put("containerNo", container.getContainerNo());
                billDetail.put("billwiseBalance", firstBillwiseAccount.get("BALANCE"));
                BigDecimal drAmt = container.getTotalPayableAmount() != null ?
                        container.getTotalPayableAmount().abs() : BigDecimal.ZERO;
                billDetail.put("drAmt", drAmt);
                billDetail.put("crAmt", container.getTotalIncomeAmount() != null ?
                        container.getTotalIncomeAmount() : BigDecimal.ZERO);
                billDetail.put("glPoid", firstBillwiseAccount.get("GL_POID"));
                allBillDetails.add(billDetail);

            } else {
                // Multi-row branch: use VW_AR_SH_CONTAINER_DEMG_DTTN (legacy bean lines 323-374)
                List<Map<String, Object>> dynRows = queryDemDetBreakdown(container.getContainerNo(), container.getBlNumber());
                int totalDynCount = dynRows.size();
                int currentDynCount = totalDynCount;
                for (Map<String, Object> dynRow : dynRows) {
                    lastRowNumber++;
                    Map<String, Object> billDetail = new java.util.HashMap<>();
                    billDetail.put("detRowId", lastRowNumber);
                    billDetail.put("checkall", "Y");
                    billDetail.put("description", firstBillwiseAccount.get("REMARKS"));
                    billDetail.put("billRefType", "AGAINST");
                    billDetail.put("billRefno", dynRow.get("DOC_REF"));
                    billDetail.put("containerNo", container.getContainerNo());
                    billDetail.put("billwiseBalance", "0");
                    BigDecimal dmAmt = dynRow.get("DM_CHARGE_AMT") != null ?
                            new BigDecimal(dynRow.get("DM_CHARGE_AMT").toString()).abs() : BigDecimal.ZERO;
                    if (totalDynCount == currentDynCount) {
                        BigDecimal incomeAmt = container.getTotalIncomeAmount() != null ?
                                container.getTotalIncomeAmount() : BigDecimal.ZERO;
                        billDetail.put("drAmt", dmAmt.subtract(incomeAmt));
                        billDetail.put("crAmt", incomeAmt);
                    } else {
                        billDetail.put("drAmt", dmAmt);
                        billDetail.put("crAmt", BigDecimal.ZERO);
                    }
                    billDetail.put("glPoid", firstBillwiseAccount.get("GL_POID"));
                    allBillDetails.add(billDetail);
                    currentDynCount--;
                }
            }
        }

        log.info("Loaded {} bill-wise records for BL Type: {}", allBillDetails.size(), request.getBlType());
        return Map.of(
                "billDetails", allBillDetails,
                "totalCount", allBillDetails.size(),
                "blType", request.getBlType(),
                "glCode", glCode
        );
    }

    /**
     * Update Principal Days — pre-create variant.
     * Calls PROC_SHIP_CNT_PPFREDAYS_UPDATE for each container in the request.
     */
    @Override
    @Transactional
    public void updatePrincipalDays(UpdateFreeDaysRequestDTO request) {
        log.info("Updating principal extra days for {} containers", request.getContainerUpdates().size());

        String transactionPoidStr = request.getContainerUpdates().isEmpty() ? null
                : String.valueOf(request.getContainerUpdates().get(0).getMainfestTransactionPoid());

        if (transactionPoidStr != null) {
            loggingService.createLogSummaryEntry(DOC_ID, transactionPoidStr,
                    String.format("%s - Principal Days Updated", LogDetailsEnum.MODIFIED));
        }

        for (UpdateFreeDaysRequestDTO.ContainerFreeDaysUpdate update : request.getContainerUpdates()) {
            // GAP-9: validate nulls before calling SP
            validateFreeDaysUpdate(update);

            // Fetch old value before update for detail log
            BigDecimal oldValue = null;
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT EXTRA_FREE_DAYS_PRNPLS FROM SHIP_DEM_DETN_TRANSFER_DTL " +
                        "WHERE MAINFEST_TRANSACTION_POID = ? AND CONTAINER_NO = ?",
                        update.getMainfestTransactionPoid(), update.getContainerNo());
                if (!rows.isEmpty() && rows.get(0).get("EXTRA_FREE_DAYS_PRNPLS") != null) {
                    oldValue = asBigDecimal(rows.get(0).get("EXTRA_FREE_DAYS_PRNPLS"));
                }
            } catch (Exception e) {
                log.warn("Could not fetch old EXTRA_FREE_DAYS_PRNPLS for container: {}", update.getContainerNo());
            }

            callProcShipCntPpfredaysUpdate(
                    update.getMainfestTransactionPoid(),
                    update.getExtraFreeDaysPrnpls(),
                    update.getContainerNo()
            );

            // Update the transfer detail record to reflect the new principal days
            updateTransferDetailPrincipalDays(
                    update.getMainfestTransactionPoid(),
                    update.getExtraFreeDaysPrnpls(),
                    update.getContainerNo()
            );

            String logDetail = String.format(LOG_KEY_ID, update.getTransactionPoid(), update.getDetRowId());
            loggingService.createLogDetailsEntry(
                    DOC_ID,
                    String.valueOf(update.getTransactionPoid()),
                    "ExtraFreeDaysPrnpls",
                    oldValue != null ? oldValue.toPlainString() : "",
                    update.getExtraFreeDaysPrnpls() != null ? update.getExtraFreeDaysPrnpls().toPlainString() : "",
                    logDetail,
                    "SHIP_DEM_DETN_TRANSFER_DTL"
            );
        }

        // Force flush and clear to ensure changes are committed and cache is cleared
        try {
            jdbcTemplate.execute("COMMIT");
            log.debug("Explicitly committed transaction after updating principal days");
        } catch (Exception e) {
            log.warn("Failed to explicitly commit transaction: {}", e.getMessage());
        }

        log.info("Successfully updated principal extra days");
    }

    /**
     * Update transfer detail record to reflect updated principal days from manifest
     */
    private void updateTransferDetailPrincipalDays(Long manifestTransactionPoid, BigDecimal extraFreeDaysPrnpls, String containerNo) {
        try {
            String sql = "UPDATE SHIP_DEM_DETN_TRANSFER_DTL " +
                    "SET EXTRA_FREE_DAYS_PRNPLS = ? " +
                    "WHERE MAINFEST_TRANSACTION_POID = ? " +
                    "AND CONTAINER_NO = ?";

            int updatedRows = jdbcTemplate.update(sql, extraFreeDaysPrnpls, manifestTransactionPoid, containerNo);
            log.debug("Updated {} transfer detail rows for manifest: {}, container: {}",
                    updatedRows, manifestTransactionPoid, containerNo);
        } catch (Exception e) {
            log.error("Error updating transfer detail principal days for manifest: {}, container: {}",
                    manifestTransactionPoid, containerNo, e);
            // Don't throw exception as the manifest is already updated
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAutoPopulatedGlAccounts(Long linePoid, String blType, Long groupPoid) {
        log.info("Getting auto-populated GL accounts for line: {}, blType: {}", linePoid, blType);

        Map<String, Object> result = new HashMap<>();
        result.put("payableGlPoid", null);
        result.put("payableGlDet", null);
        result.put("incomeGlPoid", null);

        if (linePoid != null && blType != null) {
            String defaultPayableGl = callProcDemDenSetDefault(linePoid, blType);
            if (defaultPayableGl != null && !defaultPayableGl.equals("NO_DATA")) {
                Long payableGlPoid = Long.parseLong(defaultPayableGl);
                result.put("payableGlPoid", payableGlPoid);
                try {
                    result.put("payableGlDet", lovService.getDetailsByPoidAndLovName(payableGlPoid, "GL_MASTER_LEDGERS"));
                } catch (Exception e) {
                    log.warn("Failed to fetch GL_MASTER_LEDGERS LOV for payable GL: {}", payableGlPoid, e);
                }
            }
            Long incomeGlPoid = getIncomeGlPoidFromParameter(groupPoid);
            if (incomeGlPoid != null) {
                result.put("incomeGlPoid", incomeGlPoid);
            }
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void validateCreateDTO(DemurrageDetentionPayableTransferCreateDTO dto, Long companyPoid, Long groupPoid) {
        if (dto.getLinePoid() == null) {
            throw new ValidationException("Line POID is required");
        }
        if (dto.getBlType() == null || (!dto.getBlType().equals("IMPORT") && !dto.getBlType().equals("EXPORT"))) {
            throw new ValidationException("BL Type must be IMPORT or EXPORT");
        }
        // GAP-5: Require at least one billwise row before save (legacy DocumentBeforeSave lines 540-556)
        if (dto.getBillDetails() == null || dto.getBillDetails().isEmpty()) {
            throw new ValidationException("Check billwise settle tab: at least one billwise settlement row is required before saving");
        }
        // Container uniqueness: no container already selected in another active transfer.
        // Wrapped in try-catch so that a missing table in the target schema (ORA-00942 during
        // initial deployment) is treated as a warning, not a hard block.
        if (dto.getTransferDetails() != null) {
            for (DemurrageDetentionTransferDetailDto detail : dto.getTransferDetails()) {
                if (detail.getMainfestTransactionPoid() != null && detail.getContainerNo() != null) {
                    try {
                        if (transferDtlRepository.existsByManifestAndContainerAndSelected(
                                detail.getMainfestTransactionPoid(), detail.getContainerNo())) {
                            throw new ValidationException(
                                    "Container " + detail.getContainerNo() + " is already selected in another transfer record");
                        }
                    } catch (ValidationException ve) {
                        throw ve;
                    } catch (Exception e) {
                        log.warn("Container uniqueness check failed (table may not exist yet): {}", e.getMessage());
                    }
                }
            }
        }
    }

    private void validateUpdateDTO(DemurrageDetentionPayableTransferUpdateDTO dto, Long id, Long companyPoid, Long groupPoid) {
        if (dto.getBlType() != null && !dto.getBlType().equals("IMPORT") && !dto.getBlType().equals("EXPORT")) {
            throw new ValidationException("BL Type must be IMPORT or EXPORT");
        }
        // GAP-5: If billDetails are explicitly passed in the update, they cannot be empty
        if (dto.getBillDetails() != null && dto.getBillDetails().isEmpty()) {
            throw new ValidationException("Check billwise settle tab: at least one billwise settlement row is required before saving");
        }
        if (dto.getTransferDetails() != null) {
            for (DemurrageDetentionTransferDetailDto detail : dto.getTransferDetails()) {
                if (detail.getMainfestTransactionPoid() != null && detail.getContainerNo() != null) {
                    try {
                        List<ShipDemDetnTransferDtl> existing = transferDtlRepository.findByTransactionPoidOrderByDetRowId(id);
                        boolean isCurrentRecord = existing.stream()
                                .anyMatch(e -> e.getDetRowId().equals(detail.getDetRowId()) &&
                                        e.getMainfestTransactionPoid().equals(detail.getMainfestTransactionPoid()) &&
                                        e.getContainerNo().equals(detail.getContainerNo()));

                        if (!isCurrentRecord && transferDtlRepository.existsByManifestAndContainerAndSelected(
                                detail.getMainfestTransactionPoid(), detail.getContainerNo())) {
                            throw new ValidationException(
                                    "Container " + detail.getContainerNo() + " is already selected in another transfer record");
                        }
                    } catch (ValidationException ve) {
                        throw ve;
                    } catch (Exception e) {
                        log.warn("Container uniqueness check failed (table may not exist yet): {}", e.getMessage());
                    }
                }
            }
        }
    }

    private void validateFreeDaysUpdate(UpdateFreeDaysRequestDTO.ContainerFreeDaysUpdate update) {
        if (update.getMainfestTransactionPoid() == null) {
            throw new ValidationException("Manifest transaction POID is required for detRowId: " + update.getDetRowId());
        }
        if (update.getExtraFreeDaysPrnpls() == null) {
            throw new ValidationException("Extra free days principal is required for detRowId: " + update.getDetRowId());
        }
        if (update.getContainerNo() == null || update.getContainerNo().isBlank()) {
            throw new ValidationException("Container number is required for detRowId: " + update.getDetRowId());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detail record persistence
    // ─────────────────────────────────────────────────────────────────────────

    private void createDetailRecords(Long transactionPoid,
                                     List<DemurrageDetentionTransferDetailDto> transferDetails,
                                     List<DemurrageDetentionTransferBillDetailDto> billDetails) {
        if (transferDetails != null && !transferDetails.isEmpty()) {
            Long maxDetRowId = transferDtlRepository.getMaxDetRowId(transactionPoid);
            long currentDetRowId = maxDetRowId != null ? maxDetRowId : 0;

            for (DemurrageDetentionTransferDetailDto detailDto : transferDetails) {
                currentDetRowId++;
                ShipDemDetnTransferDtl detail = mapper.mapTransferDtlFromDto(detailDto, transactionPoid);
                detail.setDetRowId(currentDetRowId);
                detail.setTransactionPoid(transactionPoid);
                ShipDemDetnTransferDtl saved = transferDtlRepository.save(detail);
                loggingService.createLogSummaryEntry(DOC_ID, transactionPoid.toString(),
                        String.format("Row Created on Demurrage Detention Transfer Detail with detRowId: %s", saved.getDetRowId()));
            }
        }

        if (billDetails != null && !billDetails.isEmpty()) {
            Long maxDetRowId = billDtlRepository.getMaxDetRowId(transactionPoid);
            long currentDetRowId = maxDetRowId != null ? maxDetRowId : 0;

            for (DemurrageDetentionTransferBillDetailDto detailDto : billDetails) {
                currentDetRowId++;
                ShipDemDtnTransferBillDtl detail = mapper.mapBillDtlFromDto(detailDto, transactionPoid);
                detail.setDetRowId(currentDetRowId);
                detail.setTransactionPoid(transactionPoid);
                ShipDemDtnTransferBillDtl saved = billDtlRepository.save(detail);
                loggingService.createLogSummaryEntry(DOC_ID, transactionPoid.toString(),
                        String.format("Row Created on Demurrage Detention Bill Detail with detRowId: %s", saved.getDetRowId()));
            }
        }
    }

    private void updateDetailRecords(Long transactionPoid,
                                     List<DemurrageDetentionTransferDetailDto> transferDetails,
                                     List<DemurrageDetentionTransferBillDetailDto> billDetails) {
        String docKeyPoid = transactionPoid.toString();

        // ── Transfer detail rows ──────────────────────────────────────────────
        if (transferDetails != null) {
            List<ShipDemDetnTransferDtl> existing = transferDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
            Map<Long, ShipDemDetnTransferDtl> existingByDetRow = existing.stream()
                    .filter(e -> e.getDetRowId() != null)
                    .collect(Collectors.toMap(ShipDemDetnTransferDtl::getDetRowId, e -> e));
            long maxDetRowId = existing.stream().map(ShipDemDetnTransferDtl::getDetRowId)
                    .filter(java.util.Objects::nonNull).mapToLong(Long::longValue).max().orElse(0L);

            for (DemurrageDetentionTransferDetailDto dto : transferDetails) {
                if (dto == null) continue;
                String action = resolveActionType(dto.getActionType(), dto.getDetRowId());
                switch (action) {
                    case ACTION_NO_CHANGE -> { /* nothing to do */ }
                    case ACTION_IS_CREATED -> {
                        long newId = dto.getDetRowId() == null ? ++maxDetRowId : dto.getDetRowId();
                        maxDetRowId = Math.max(maxDetRowId, newId);
                        ShipDemDetnTransferDtl entity = mapper.mapTransferDtlFromDto(dto, transactionPoid);
                        entity.setDetRowId(newId);
                        ShipDemDetnTransferDtl saved = transferDtlRepository.save(entity);
                        loggingService.createLogSummaryEntry(DOC_ID, docKeyPoid,
                                String.format("Row Created on Demurrage Detention Transfer Detail with detRowId: %s", saved.getDetRowId()));
                    }
                    case ACTION_IS_UPDATED -> {
                        ShipDemDetnTransferDtl entity = existingByDetRow.get(dto.getDetRowId());
                        if (entity == null) throw new com.asg.common.lib.exception.ResourceNotFoundException("TransferDetail", "detRowId", dto.getDetRowId());
                        ShipDemDetnTransferDtl oldEntity = new ShipDemDetnTransferDtl();
                        org.springframework.beans.BeanUtils.copyProperties(entity, oldEntity);
                        ShipDemDetnTransferDtl updated = mapper.mapTransferDtlFromDto(dto, transactionPoid);
                        updated.setDetRowId(entity.getDetRowId());
                        transferDtlRepository.save(updated);
                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, dto.getDetRowId());
                        loggingService.createLog(oldEntity, updated, ShipDemDetnTransferDtl.class, DOC_ID, docKeyPoid, logDetail);
                    }
                    case ACTION_IS_DELETED -> {
                        ShipDemDetnTransferDtl entity = existingByDetRow.get(dto.getDetRowId());
                        if (entity != null) {
                            transferDtlRepository.delete(entity);
                            loggingService.createLogSummaryEntry(DOC_ID, docKeyPoid,
                                    String.format("Row Deleted on Demurrage Detention Transfer Detail with detRowId: %s, containerNo: %s",
                                            entity.getDetRowId(), entity.getContainerNo()));
                        }
                    }
                }
            }
        }

        // ── Bill detail rows ──────────────────────────────────────────────────
        if (billDetails != null) {
            List<ShipDemDtnTransferBillDtl> existingBill = billDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
            Map<Long, ShipDemDtnTransferBillDtl> existingBillByDetRow = existingBill.stream()
                    .filter(e -> e.getDetRowId() != null)
                    .collect(Collectors.toMap(ShipDemDtnTransferBillDtl::getDetRowId, e -> e));
            long maxBillDetRowId = existingBill.stream().map(ShipDemDtnTransferBillDtl::getDetRowId)
                    .filter(java.util.Objects::nonNull).mapToLong(Long::longValue).max().orElse(0L);

            for (DemurrageDetentionTransferBillDetailDto dto : billDetails) {
                if (dto == null) continue;
                String action = resolveActionType(dto.getActionType(), dto.getDetRowId());
                switch (action) {
                    case ACTION_NO_CHANGE -> { /* nothing to do */ }
                    case ACTION_IS_CREATED -> {
                        long newId = dto.getDetRowId() == null ? ++maxBillDetRowId : dto.getDetRowId();
                        maxBillDetRowId = Math.max(maxBillDetRowId, newId);
                        ShipDemDtnTransferBillDtl entity = mapper.mapBillDtlFromDto(dto, transactionPoid);
                        entity.setDetRowId(newId);
                        ShipDemDtnTransferBillDtl saved = billDtlRepository.save(entity);
                        loggingService.createLogSummaryEntry(DOC_ID, docKeyPoid,
                                String.format("Row Created on Demurrage Detention Bill Detail with detRowId: %s", saved.getDetRowId()));
                    }
                    case ACTION_IS_UPDATED -> {
                        ShipDemDtnTransferBillDtl entity = existingBillByDetRow.get(dto.getDetRowId());
                        if (entity == null) throw new com.asg.common.lib.exception.ResourceNotFoundException("BillDetail", "detRowId", dto.getDetRowId());
                        ShipDemDtnTransferBillDtl oldEntity = new ShipDemDtnTransferBillDtl();
                        org.springframework.beans.BeanUtils.copyProperties(entity, oldEntity);
                        ShipDemDtnTransferBillDtl updated = mapper.mapBillDtlFromDto(dto, transactionPoid);
                        updated.setDetRowId(entity.getDetRowId());
                        billDtlRepository.save(updated);
                        String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, dto.getDetRowId());
                        loggingService.createLog(oldEntity, updated, ShipDemDtnTransferBillDtl.class, DOC_ID, docKeyPoid, logDetail);
                    }
                    case ACTION_IS_DELETED -> {
                        ShipDemDtnTransferBillDtl entity = existingBillByDetRow.get(dto.getDetRowId());
                        if (entity != null) {
                            billDtlRepository.delete(entity);
                            loggingService.createLogSummaryEntry(DOC_ID, docKeyPoid,
                                    String.format("Row Deleted on Demurrage Detention Bill Detail with detRowId: %s, containerNo: %s",
                                            entity.getDetRowId(), entity.getContainerNo()));
                        }
                    }
                }
            }
        }
    }

    private String resolveActionType(String actionType, Long detRowId) {
        if (actionType != null && !actionType.isBlank()) return actionType.trim();
        return detRowId == null ? ACTION_IS_CREATED : ACTION_IS_UPDATED;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Billwise load helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Core billwise load logic shared by loadBillwiseData (post-create).
     * Returns the updated currentDetRowId after inserting rows for this container.
     */
    private long buildAndPersistBillRows(Long transactionPoid, ShipDemDetnTransferDtl container,
                                         String glCode, long currentDetRowId) {
        // Query VW_SHIP_BILLWISE_ACCOUNT_TRN (legacy bean lines 280-295)
        List<Map<String, Object>> billwiseRows = queryBillwiseAccountView(glCode, container.getBlNumber());

        if (billwiseRows.isEmpty()) {
            log.warn("No billwise rows found for container: {}, blNumber: {}", container.getContainerNo(), container.getBlNumber());
            return currentDetRowId;
        }

        Map<String, Object> firstBillwiseRow = billwiseRows.get(0);
        BigDecimal totalPayable = container.getTotalPayableAmount() != null ? container.getTotalPayableAmount() : BigDecimal.ZERO;
        BigDecimal totalIncome = container.getTotalIncomeAmount() != null ? container.getTotalIncomeAmount() : BigDecimal.ZERO;

        if (billwiseRows.size() == 1) {
            // Single-row branch (legacy bean lines 299-321)
            currentDetRowId++;
            ShipDemDtnTransferBillDtl billDetail = ShipDemDtnTransferBillDtl.builder()
                    .transactionPoid(transactionPoid)
                    .detRowId(currentDetRowId)
                    .checkall("Y")
                    .description(asString(firstBillwiseRow.get("REMARKS")))
                    .billRefType("AGAINST")
                    .billRefno(asString(firstBillwiseRow.get("BILL_REF")))
                    .containerNo(container.getContainerNo())
                    .billwiseBalance(asBigDecimal(firstBillwiseRow.get("BALANCE")))
                    .drAmt(totalPayable.abs())
                    .crAmt(totalIncome)
                    .glPoid(asLong(firstBillwiseRow.get("GL_POID")))
                    .glCompanyPoid(asLong(firstBillwiseRow.get("GL_COMPANY_POID")))
                    .build();
            billDtlRepository.save(billDetail);
        } else {
            // Multi-row branch: use VW_AR_SH_CONTAINER_DEMG_DTTN (legacy bean lines 323-374)
            List<Map<String, Object>> dynRows = queryDemDetBreakdown(container.getContainerNo(), container.getBlNumber());
            boolean isFirst = true;
            for (Map<String, Object> dynRow : dynRows) {
                currentDetRowId++;
                BigDecimal dmAmt = asBigDecimal(dynRow.get("DM_CHARGE_AMT"));
                if (dmAmt == null) dmAmt = BigDecimal.ZERO;
                BigDecimal drAmt = isFirst ? dmAmt.abs().subtract(totalIncome) : dmAmt.abs();
                BigDecimal crAmt = isFirst ? totalIncome : BigDecimal.ZERO;
                isFirst = false;

                ShipDemDtnTransferBillDtl billDetail = ShipDemDtnTransferBillDtl.builder()
                        .transactionPoid(transactionPoid)
                        .detRowId(currentDetRowId)
                        .checkall("Y")
                        .description(asString(firstBillwiseRow.get("REMARKS")))  // from first billwise row
                        .billRefType("AGAINST")
                        .billRefno(asString(dynRow.get("DOC_REF")))
                        .containerNo(container.getContainerNo())
                        .billwiseBalance(BigDecimal.ZERO)  // legacy forces 0 in multi-row branch
                        .drAmt(drAmt)
                        .crAmt(crAmt)
                        .glPoid(asLong(firstBillwiseRow.get("GL_POID")))  // from first billwise row
                        .glCompanyPoid(asLong(firstBillwiseRow.get("GL_COMPANY_POID")))
                        .build();
                billDtlRepository.save(billDetail);
            }
        }
        return currentDetRowId;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Database query helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Container availability query for Process For Filtered Data.
     * Uses NOT IN to exactly mirror legacy query behavior (NOT EXISTS was causing data filtering issues).
     * No date filter — legacy EmptyFromDate/EmptyToDate are rendered=false.
     * No DEMURRAGE_ACUTAL filter — legacy doesn't have this condition.
     */
    private String buildContainerQuerySql() {
        return "SELECT * FROM VW_SHIP_DEM_DTN_TRANSFER_TEMP V " +
                "WHERE (V.MAINFEST_TRANSACTION_POID, V.CONTAINER_NO) NOT IN (" +
                "  SELECT D.MAINFEST_TRANSACTION_POID, D.CONTAINER_NO " +
                "  FROM SHIP_DEM_DETN_TRANSFER_HDR H " +
                "  INNER JOIN SHIP_DEM_DETN_TRANSFER_DTL D ON D.TRANSACTION_POID = H.TRANSACTION_POID " +
                "  WHERE NVL(H.DELETED, 'N') = 'N' " +
                "  AND NVL(D.IS_SELECT, 'N') = 'Y'" +
                ") " +
                "AND V.BL_TYPE = ? AND V.LINE_POID = ? AND V.COMPANY_POID = ? " +
                "ORDER BY V.BL_NUMBER";
    }

    /**
     * Query VW_SHIP_BILLWISE_ACCOUNT_TRN (legacy VwShipBillwiseAccountTrnView1).
     * Confirmed DB view name from VwShipBillwiseAccountTrn.xml: DBObjectName="VW_SHIP_BILLWISE_ACCOUNT_TRN".
     */
    private List<Map<String, Object>> queryBillwiseAccountView(String glCode, String blNumber) {
        String sql = "SELECT GL_POID, GL_COMPANY_POID, BILL_REF, REMARKS, BALANCE " +
                "FROM VW_SHIP_BILLWISE_ACCOUNT_TRN " +
                "WHERE GL_CODE = ? AND REMARKS LIKE ?";
        try {
            return jdbcTemplate.queryForList(sql, glCode, "%" + blNumber + "%");
        } catch (Exception e) {
            log.error("Error querying VW_SHIP_BILLWISE_ACCOUNT_TRN for glCode: {}, blNumber: {}", glCode, blNumber, e);
            return Collections.emptyList();
        }
    }

    /**
     * Multi-row billwise breakdown query on VW_AR_SH_CONTAINER_DEMG_DTTN (legacy bean lines 323-335).
     */
    private List<Map<String, Object>> queryDemDetBreakdown(String containerNo, String blNumber) {
        String sql = "SELECT DOC_REF, SUM(DM_CHARGE_AMT) DM_CHARGE_AMT " +
                "FROM VW_AR_SH_CONTAINER_DEMG_DTTN " +
                "WHERE NVL(DM_CHARGE_AMT, 0) <> 0 " +
                "AND CONTAINER_NO = ? " +
                "AND GET_BL_NUMBER(BL_POID) = ? " +
                "GROUP BY DOC_REF";
        try {
            return jdbcTemplate.queryForList(sql, containerNo, blNumber);
        } catch (Exception e) {
            log.error("Error querying VW_AR_SH_CONTAINER_DEMG_DTTN for container: {}, blNumber: {}", containerNo, blNumber, e);
            return Collections.emptyList();
        }
    }

    /**
     * Build a ShipDemDetnTransferDtl entity from a VW_SHIP_DEM_DTN_TRANSFER row.
     * Column name mapping follows legacy VO attribute → DB column convention.
     */
    private ShipDemDetnTransferDtl buildTransferDetailFromViewRow(Map<String, Object> row, Long transactionPoid, long detRowId) {
        return ShipDemDetnTransferDtl.builder()
                .transactionPoid(transactionPoid)
                .detRowId(detRowId)
                .isSelect("N")  // GAP-7: always 'N' after process (legacy bean line 181)
                .blNumber(asString(row.get("BL_NUMBER")))
                .containerNo(asString(row.get("CONTAINER_NO")))
                .equipmentIsoType(asString(row.get("EQUIPMENT_ISO_TYPE")))
                .sailDate(asLocalDate(row.get("SAIL_DATE")))
                .arrivalDate(asLocalDate(row.get("ARRIVAL_DATE")))
                .emptyIn(asLocalDate(row.get("EMPTY_IN")))
                .demurrageAcutal(asBigDecimal(row.get("DEMURRAGE_ACUTAL")))
                .extraFreeDays(asBigDecimal(row.get("EXTRA_FREE_DAYS")))
                .extraFreeDaysPrnpls(asBigDecimal(row.get("EXTRA_FREE_DAYS_PRNPLS")))
                .startDate(asLocalDate(row.get("START_DATE")))
                .endDate(asLocalDate(row.get("END_DATE")))
                .totalCollectedDays(asBigDecimal(row.get("TOTAL_COLLECTED_DAYS")))
                .totalCollectedAmt(asBigDecimal(row.get("TOTAL_COLLECTED_AMT")))
                .totalShortExcessAmount(asBigDecimal(row.get("SHORT_ACCESS")))   // view col = SHORT_ACCESS (legacy attr = ShortAccess)
                .totalPayableAmount(asBigDecimal(row.get("PAYABLE_AMT")))        // view col = PAYABLE_AMT (legacy attr = PayableAmt)
                .totalIncomeAmount(asBigDecimal(row.get("INCOME_AMT")))          // view col = INCOME_AMT  (legacy attr = IncomeAmt)
                .jobNo(asString(row.get("JOB_NO")))
                .consignee(asString(row.get("CONSIGNEE")))
                .notify(asString(row.get("NOTIFY1")))
                .linePoid(asLong(row.get("LINE_POID")))
                .netIncomeAmt(asBigDecimal(row.get("NET_INCOME_AMT")))
                .mainfestTransactionPoid(asLong(row.get("MAINFEST_TRANSACTION_POID")))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stored procedure callers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * PROC_SHIP_CNT_PPFREDAYS_UPDATE(P_MANIFEST_POID VARCHAR2, P_EXTRA_FREE_DAYS_PRNPLS VARCHAR2, P_CONTAINER_NO VARCHAR2).
     * Legacy bean lines 433-440.
     */
    private void callProcShipCntPpfredaysUpdate(Long mainfestTransactionPoid, BigDecimal extraFreeDaysPrnpls, String containerNo) {
        try {
            String sql = "{call PROC_SHIP_CNT_PPFREDAYS_UPDATE(?, ?, ?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setString(1, String.valueOf(mainfestTransactionPoid));
                cs.setString(2, String.valueOf(extraFreeDaysPrnpls));
                cs.setString(3, containerNo);
                cs.execute();
                return null;
            });
            log.debug("PROC_SHIP_CNT_PPFREDAYS_UPDATE called for manifest: {}, container: {}", mainfestTransactionPoid, containerNo);
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_CNT_PPFREDAYS_UPDATE", e);
            throw new ValidationException("Failed to update container free days: " + e.getMessage());
        }
    }

    /**
     * PROC_DEM_DEN_SET_DEFAULT(P_LINE_POID NUMBER, P_BL_TYPE VARCHAR2, P_AC_PAYABLE OUT VARCHAR2).
     * Legacy bean lines 495-512.
     * GAP-4 fix: OUT parameter registered as Types.VARCHAR (legacy OracleTypes.VARCHAR), not NUMERIC.
     */
    private String callProcDemDenSetDefault(Long linePoid, String blType) {
        try {
            return jdbcTemplate.execute(
                    "{call PROC_DEM_DEN_SET_DEFAULT(?, ?, ?)}",
                    (CallableStatement cs) -> {
                        cs.setLong(1, linePoid);
                        cs.setString(2, blType);
                        cs.registerOutParameter(3, Types.VARCHAR);  // GAP-4: must be VARCHAR, not NUMERIC
                        cs.execute();
                        String result = cs.getString(3);
                        return (result == null || result.trim().isEmpty()) ? "NO_DATA" : result.trim();
                    }
            );
        } catch (Exception e) {
            log.error("Error calling PROC_DEM_DEN_SET_DEFAULT for linePoid: {}, blType: {}", linePoid, blType, e);
            return "NO_DATA";
        }
    }

    /**
     * GAP-6: Generate DOC_REF via RTN_GLOBAL_SEQ_NO function.
     * Pattern: GET_COMPANY_CODE(companyPoid) → RTN_GLOBAL_SEQ_NO('DEM_DET_TRANSFER', companyCode, 5).
     * Reference: ShipCommissionTransferServiceImpl.generateDocRef (lines 603-626).
     */
    private String generateDocRef(Long companyPoid) {
        try {
            String companyCode = jdbcTemplate.queryForObject(
                    "SELECT GET_COMPANY_CODE(?) FROM DUAL", String.class, companyPoid);
            String seqNo = jdbcTemplate.queryForObject(
                    "SELECT RTN_GLOBAL_SEQ_NO('DEM_DET_TRANSFER', ?, 5) FROM DUAL",
                    String.class,
                    companyCode != null ? companyCode : "");
            if (seqNo != null && !seqNo.isBlank()) return seqNo;
        } catch (Exception e) {
            log.error("Error generating DOC_REF via RTN_GLOBAL_SEQ_NO for companyPoid: {}", companyPoid, e);
            throw new ValidationException("Failed to generate document reference number: " + e.getMessage());
        }
        throw new ValidationException("RTN_GLOBAL_SEQ_NO returned empty DOC_REF for companyPoid: " + companyPoid);
    }

    /**
     * GAP-8: Get income GL POID from global parameter DEM_DET_ACCOUNT_INCOME.
     * Legacy: common.GetParameterValue("DEM_DET_ACCOUNT_INCOME", "Group", "1", "13653") — default fallback is 13653.
     * Uses ROWNUM=1 to prevent IncorrectResultSizeDataAccessException on multiple rows.
     */
    private Long getIncomeGlPoidFromParameter(Long groupPoid) {
        try {
            List<String> values = jdbcTemplate.queryForList(
                    "SELECT PARAMETER_VALUE FROM GLOBAL_PARAMETERS " +
                            "WHERE PARAMETER_KEYID_TYPE = 'DEM_DET_ACCOUNT_INCOME' " +
                            "AND GROUP_POID = ? AND ROWNUM = 1",
                    String.class, groupPoid);
            if (!values.isEmpty() && values.get(0) != null && !values.get(0).isBlank()) {
                return Long.parseLong(values.get(0).trim());
            }
        } catch (Exception e) {
            log.warn("Failed to get DEM_DET_ACCOUNT_INCOME parameter value, using default {}", DEFAULT_INCOME_GL_POID, e);
        }
        return DEFAULT_INCOME_GL_POID;  // GAP-8: default fallback per legacy
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOV enrichment
    // ─────────────────────────────────────────────────────────────────────────

    private void enrichLovData(DemurrageDetentionPayableTransferDto dto) {
        // Header LOVs (jsff lines 103-144)
        enrichLovByPoid(dto.getLinePoid(), dto::setLinePoidDet, "LINE_MASTER");
        enrichLovByPoid(dto.getPayableGlPoid(), dto::setPayableGlPoidDet, "GL_MASTER_LEDGERS");
        enrichLovByPoid(dto.getIncomeGlPoid(), dto::setIncomeGlPoidDet, "GL_MASTER_LEDGERS");
        // GAP-LOV: blType is CODE-based (matchingField="CODE" in jsff line 108)
        enrichLovByCode(dto.getBlType(), dto::setBlTypeDet, "BL_TYPE");

        // Transfer detail LOVs
        if (dto.getTransferDetails() != null) {
            transferDetailLOVs(dto);
        }

        // Bill detail LOVs
        if (dto.getBillDetails() != null) {
            billDetailLOVs(dto);
        }
    }

    void transferDetailLOVs(DemurrageDetentionPayableTransferDto dto) {
        Set<Long> manifestPoids = new HashSet<>();
        Set<Long> linePoids = new HashSet<>();
        Set<String> containerTypes = new HashSet<>();

        // Collect all ids
        for (DemurrageDetentionTransferDetailDto detail : dto.getTransferDetails()) {
            if (detail.getMainfestTransactionPoid() != null) {
                manifestPoids.add(detail.getMainfestTransactionPoid());
            }

            if (detail.getLinePoid() != null) {
                linePoids.add(detail.getLinePoid());
            }

            if (detail.getEquipmentIsoType() != null) {
                containerTypes.add(detail.getEquipmentIsoType());
            }
        }

        Map<Long, LovGetListDto> manifestMap = manifestPoids.isEmpty()
                ? Collections.emptyMap()
                : lovService.getDetailsByPoidsAndLovName(new ArrayList<>(manifestPoids), "MANIFEST");

        Map<Long, LovGetListDto> lineMap = linePoids.isEmpty()
                ? Collections.emptyMap()
                : lovService.getDetailsByPoidsAndLovName(new ArrayList<>(linePoids), "LINE_MASTER");

        Map<String, LovGetListDto> containerMap = containerTypes.isEmpty()
                ? Collections.emptyMap()
                : lovService.getDetailsByCodesAndLovName(new ArrayList<>(containerTypes), "CONTAINER_TYPE_MASTER");

        // Set values
        for (DemurrageDetentionTransferDetailDto detail : dto.getTransferDetails()) {

            detail.setMainfestTransactionPoidDet(
                    manifestMap.get(detail.getMainfestTransactionPoid()));

            detail.setLinePoidDet(
                    lineMap.get(detail.getLinePoid()));

            detail.setEquipmentIsoTypeDet(
                    containerMap.get(detail.getEquipmentIsoType()));
        }
    }

    void billDetailLOVs(DemurrageDetentionPayableTransferDto dto) {
        Set<Long> glPoids = new HashSet<>();

        for (DemurrageDetentionTransferBillDetailDto detail : dto.getBillDetails()) {
            if (detail.getGlPoid() != null) {
                glPoids.add(detail.getGlPoid());
            }
        }

        Map<Long, LovGetListDto> lovGetListDtoMap = glPoids.isEmpty()
                ? Collections.emptyMap()
                : lovService.getDetailsByPoidsAndLovName(new ArrayList<>(glPoids), "GL_MASTER_LEDGERS");

        // Set Values
        for (DemurrageDetentionTransferBillDetailDto detail : dto.getBillDetails()) {
            detail.setGlPoidDet(lovGetListDtoMap.get(detail.getGlPoid()));
        }
    }

    private void enrichLovByPoid(Long poid, Consumer<LovGetListDto> setter, String lovType) {
        if (poid != null) {
            try {
                setter.accept(lovService.getDetailsByPoidAndLovName(poid, lovType));
            } catch (Exception e) {
                log.warn("Failed to fetch {} LOV for poid: {}", lovType, poid, e);
            }
        }
    }

    private void enrichLovByCode(String code, Consumer<LovGetListDto> setter, String lovType) {
        if (code != null && !code.isBlank()) {
            try {
                setter.accept(lovService.getDetailsByCodeAndLovName(code, lovType));
            } catch (Exception e) {
                log.warn("Failed to fetch {} LOV for code: {}", lovType, code, e);
            }
        }
    }

    /**
     * Resolve GL Code based on BL Type following legacy logic
     */
    private String resolveGlCode(String blType) {
        if ("IMPORT".equalsIgnoreCase(blType)) {
            return "LINE_DEM"; // Demurrage for Import
        } else if ("EXPORT".equalsIgnoreCase(blType)) {
            return "LINE_DET"; // Detention for Export
        }
        throw new ValidationException("Invalid BL Type: " + blType + ". Must be IMPORT or EXPORT");
    }

    public Map<String, Object> getGlAccountsDirectFromSp(Long linePoid, String blType) {
        log.info("Getting GL accounts directly from SP for line: {}, blType: {}", linePoid, blType);
        Map<String, Object> result = new java.util.HashMap<>();

        if (linePoid != null && blType != null) {
            String payableGl = callProcDemDenSetDefault(linePoid, blType);
            result.put("payableGlPoid", payableGl != null && !payableGl.equals("NO_DATA") ? payableGl : null);
        }
        return result;
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString().trim()); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try { return new BigDecimal(value.toString().trim()); } catch (NumberFormatException e) { return null; }
    }

    private LocalDate asLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        if (value instanceof java.sql.Timestamp) return ((java.sql.Timestamp) value).toLocalDateTime().toLocalDate();
        if (value instanceof LocalDate) return (LocalDate) value;
        return null;
    }

}
