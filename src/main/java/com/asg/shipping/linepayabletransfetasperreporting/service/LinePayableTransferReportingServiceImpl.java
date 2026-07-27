package com.asg.shipping.linepayabletransfetasperreporting.service;



import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.exceptions.ValidationException;
import com.asg.shipping.linepayabletransfetasperreporting.dto.*;
import com.asg.shipping.linepayabletransfetasperreporting.entity.ShipLineReportTransferDtl;
import com.asg.shipping.linepayabletransfetasperreporting.entity.ShipLineReportTransferHdr;
import com.asg.shipping.linepayabletransfetasperreporting.repository.ShipLineReportTransferDtlRepository;
import com.asg.shipping.linepayabletransfetasperreporting.repository.ShipLineReportTransferHdrRepository;
import com.asg.shipping.linepayabletransfetasperreporting.util.LinePayableTransferReportingMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;

/**
 * Service implementation for Line Payable Transfer As Per Reporting operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LinePayableTransferReportingServiceImpl implements LinePayableTransferReportingService {

    private final ShipLineReportTransferHdrRepository hdrRepository;
    private final ShipLineReportTransferDtlRepository dtlRepository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final JdbcTemplate jdbcTemplate;
    private final LinePayableTransferReportingMapper mapper;
    private final LovDataService lovDataService;
    private final PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String DOC_ID = "100-432";

    private static final String LOG_ROW_CREATED = "Row Created on Line Payable Transfer Detail with detRowId: %s";
    private static final String LOG_ROW_KEY = "KeyId = %s:%s";
    private static final String COL_DET_ROW_ID = "DET_ROW_ID";
    private static final String COL_TRANSACTION_POID = "TRANSACTION_POID";

    private static final String LOV_LINE_MASTER = "LINE_MASTER";
    private static final String LOV_ALL_BL_NUMBER = "ALLBLNUMBER";
    private static final String LOV_CHARGE_MASTER = "CHARGE_MASTER";
    private static final String LOV_CURRENCY = "CURRENCY";
    private static final String LOV_BL_TYPE = "REPORT_CNT_BL_TYPE";

    /** UPDATE_TYPE legacy passes to PROC_SHIP_BL_PAGE_SAVE_AFTER from DocumentAfterSave */
    private static final String AFTER_SAVE_UPDATE_TYPE = "SHIPLNRPTAFTERSAVE";

    private static final String CHARGE_FILTER_ALL = "ALL";
    private static final String CHARGE_FILTER_FRTTHC = "FRTTHC";
    private static final String CHARGE_FILTER_OTHERS = "OTHERS";
    private static final Set<String> CHARGE_FILTERS =
            Set.of(CHARGE_FILTER_ALL, CHARGE_FILTER_FRTTHC, CHARGE_FILTER_OTHERS);

    // Messages carried over verbatim from the legacy page
    private static final String MSG_NO_DATA_FOUND = "No Data Found...";
    private static final String MSG_LOAD_FAILED = "Some error occured while loading data, please check the log...";

    private static final String SQL_CHECK_LINE =
            "SELECT COUNT(*) FROM SHIP_LINE_MASTER WHERE LINE_POID = ? AND NVL(DELETED,'N') = 'N'";

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchLinePayableTransfer(String docId, FilterRequestDto filterRequest, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        log.info("Searching Line Payable Transfer As Per Reporting records");

        String operator = documentSearchService.resolveOperator(filterRequest);
        String isDeleted = documentSearchService.resolveIsDeleted(filterRequest);
        List<FilterDto> filters = documentSearchService.resolveDateFilters(filterRequest, "TRANSACTION_DATE", fromDate, toDate);

        RawSearchResult raw = documentSearchService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "DOC_REF",
                "TRANSACTION_POID"
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
    public LinePayableTransferReportingDto getLinePayableTransferById(Long transactionPoid) {
        log.info("Getting Line Payable Transfer As Per Reporting with id: {}", transactionPoid);

        ShipLineReportTransferHdr entity = hdrRepository.findActiveByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line Payable Transfer As Per Reporting", "transactionPoid", transactionPoid.toString()));

        LinePayableTransferReportingDto dto = mapper.mapToDto(entity);

        // Load detail records
        List<ShipLineReportTransferDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        dto.setDetails(mapper.mapDtlListToDto(details));

        // Enrich with LOV data
        enrichLovData(dto);

        log.info("Successfully retrieved Line Payable Transfer As Per Reporting with id: {}", transactionPoid);
        return dto;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Deliberately not annotated {@code @Transactional}: the save is committed by
     * {@link #persistCreate} before PROC_SHIP_BL_PAGE_SAVE_AFTER runs. See
     * {@link #callProcShipBlPageSaveAfter} for why the procedure cannot run inside that
     * transaction.
     */
    @Override
    public LinePayableTransferReportingDto createLinePayableTransfer(LinePayableTransferReportingCreateDTO createDTO) {
        log.info("Creating Line Payable Transfer As Per Reporting record");

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();

        // Validate
        validateCreateDTO(createDTO);

        Long transactionPoid = inNewTransaction(() -> persistCreate(createDTO, groupPoid, companyPoid));

        // Legacy DocumentAfterSave post-processing, run once the rows above are committed
        callProcShipBlPageSaveAfter(groupPoid, companyPoid, transactionPoid);

        // Read back after the procedure, which drops the rows left unselected. Legacy refreshes
        // its grid at the same point for the same reason.
        LinePayableTransferReportingDto result = reloadAfterSave(transactionPoid);

        log.info("Successfully created Line Payable Transfer As Per Reporting with id: {}", transactionPoid);
        return result;
    }

    /**
     * Persist the header and its details as one committed unit, returning the new TRANSACTION_POID
     */
    private Long persistCreate(LinePayableTransferReportingCreateDTO createDTO, Long groupPoid, Long companyPoid) {
        // Create entity
        ShipLineReportTransferHdr entity = new ShipLineReportTransferHdr();
        mapper.mapCreateDTOToEntity(createDTO, entity, groupPoid, companyPoid);

        // Save entity (generates TRANSACTION_POID via IDENTITY)
        ShipLineReportTransferHdr saved = hdrRepository.save(entity);
        hdrRepository.flush();
        // Bypass Hibernate L1 cache to get trigger-generated values (TRANSACTION_POID, DOC_REF)
        entityManager.refresh(saved);

        // Log the header before the details, so the header entry precedes its rows
        loggingService.createLogSummaryEntry(resolveDocumentId(), saved.getTransactionPoid().toString(),
                String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), saved.getDocRef()));

        // Save detail tables from DTO (if provided)
        saveDetailTables(createDTO, saved.getTransactionPoid());

        return saved.getTransactionPoid();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Not annotated {@code @Transactional} for the same reason as
     * {@link #createLinePayableTransfer}.
     */
    @Override
    public LinePayableTransferReportingDto updateLinePayableTransfer(Long transactionPoid, LinePayableTransferReportingUpdateDTO updateDTO) {
        log.info("Updating Line Payable Transfer As Per Reporting with id: {}", transactionPoid);

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();

        // Validate
        validateUpdateDTO(updateDTO);

        inNewTransaction(() -> {
            persistUpdate(transactionPoid, updateDTO);
            return null;
        });

        // Legacy DocumentAfterSave post-processing, run once the rows above are committed
        callProcShipBlPageSaveAfter(groupPoid, companyPoid, transactionPoid);

        LinePayableTransferReportingDto result = reloadAfterSave(transactionPoid);

        log.info("Successfully updated Line Payable Transfer As Per Reporting with id: {}", transactionPoid);
        return result;
    }

    /**
     * Apply the header and detail changes as one committed unit
     */
    private void persistUpdate(Long transactionPoid, LinePayableTransferReportingUpdateDTO updateDTO) {
        ShipLineReportTransferHdr entity = hdrRepository.findActiveByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line Payable Transfer As Per Reporting", "transactionPoid", transactionPoid.toString()));

        // Check if data loading parameters changed. Must be evaluated before the entity is mutated
        boolean filterChanged = checkShouldReloadData(entity, updateDTO);

        // Snapshot the header before it is mutated, so the changed fields can be logged
        ShipLineReportTransferHdr oldEntity = new ShipLineReportTransferHdr();
        BeanUtils.copyProperties(entity, oldEntity);

        // Update entity
        mapper.mapUpdateDTOToEntity(updateDTO, entity);

        ShipLineReportTransferHdr saved = hdrRepository.save(entity);

        // Log the header before the details, so the header entries precede their rows
        loggingService.createLogSummaryEntry(resolveDocumentId(), transactionPoid.toString(),
                String.format("%s %s", LogDetailsEnum.MODIFIED.getDescription(), saved.getDocRef()));

        // Log the changed header fields as detail entries
        loggingService.logDetails(oldEntity, saved, ShipLineReportTransferHdr.class,
                resolveDocumentId(), transactionPoid.toString(), COL_TRANSACTION_POID);

        // Update detail tables
        updateDetailTables(updateDTO, saved.getTransactionPoid(), filterChanged);
    }

    @Override
    @Transactional
    public void deleteLinePayableTransfer(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting Line Payable Transfer As Per Reporting with id: {}", transactionPoid);

        ShipLineReportTransferHdr entity = hdrRepository.findActiveByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line Payable Transfer As Per Reporting", "transactionPoid", transactionPoid.toString()));

        documentDeleteService.deleteDocument(
                transactionPoid,
                "SHIP_LINE_REPORT_TRANSFER_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                entity.getTransactionDate()
        );

        entity.setDeleted("Y");
        hdrRepository.save(entity);

        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, resolveDocumentId(), transactionPoid.toString());

        log.info("Successfully deleted Line Payable Transfer As Per Reporting with id: {}", transactionPoid);
    }

    @Override
    @Transactional
    public List<LinePayableTransferReportingDtlDto> loadDataByDateRange(Long transactionPoid, LoadDataByDateRangeRequest request) {
        log.info("Loading data by date range for transaction: {}, line: {}, BL type: {}, dates: {} to {}",
                transactionPoid, request.getLinePoid(), request.getBlType(),
                request.getReportStartDate(), request.getReportEndDate());

        validateDateRange(request);
        validateLineExists(request.getLinePoid());

        // Clear existing details, exactly as legacy empties the array table before reloading it
        dtlRepository.deleteByTransactionPoid(transactionPoid);

        List<LinePayableTransferReportingDtlDto> result = callReportLineDatewise(request);

        // The rows are persisted after the ref cursor is closed, so no JPA flush runs while the
        // cursor is still open on the transaction's connection
        long detRowId = 0L;
        for (LinePayableTransferReportingDtlDto dto : result) {
            detRowId++;
            dto.setDetRowId(detRowId);
            dtlRepository.save(mapper.mapDtlFromDto(dto, transactionPoid, detRowId));
        }

        enrichDetailListWithLov(result);
        log.info("Loaded {} records for transaction: {}", result.size(), transactionPoid);
        return result;
    }

    @Override
    @Transactional
    public List<LinePayableTransferReportingDtlDto> processWeeklyBlReport(Long transactionPoid, LoadDataByDateRangeRequest request) {
        log.info("Processing weekly BL report for transaction: {}, line: {}, dates: {} to {}",
                transactionPoid, request.getLinePoid(), request.getReportStartDate(), request.getReportEndDate());

        validateDateRange(request);
        validateLineExists(request.getLinePoid());

        dtlRepository.deleteByTransactionPoid(transactionPoid);

        List<LinePayableTransferReportingDtlDto> result = callWeeklyBlReport(request);

        // Persisted after the ref cursor is closed, so no JPA flush runs while it is still open
        long detRowId = 0L;
        for (LinePayableTransferReportingDtlDto dto : result) {
            detRowId++;
            dto.setDetRowId(detRowId);
            dtlRepository.save(mapper.mapDtlFromDto(dto, transactionPoid, detRowId));
        }

        enrichDetailListWithLov(result);
        log.info("Processed {} records for transaction: {}", result.size(), transactionPoid);
        return result;
    }

    @Override
    public List<LinePayableTransferReportingDtlDto> loadDataBeforeCreate(LoadDataByDateRangeRequest request) {
        log.info("Loading data before create for line: {}, BL type: {}, dates: {} to {}",
                request.getLinePoid(), request.getBlType(), request.getReportStartDate(), request.getReportEndDate());

        validateDateRange(request);
        validateLineExists(request.getLinePoid());

        List<LinePayableTransferReportingDtlDto> result = callReportLineDatewise(request);

        long detRowId = 0L;
        for (LinePayableTransferReportingDtlDto dto : result) {
            dto.setDetRowId(++detRowId);
        }

        enrichDetailListWithLov(result);
        log.info("Loaded {} records before create", result.size());
        return result;
    }

    @Override
    public List<LinePayableTransferReportingDtlDto> processWeeklyBeforeCreate(LoadDataByDateRangeRequest request) {
        log.info("Processing weekly BL report before create for line: {}, dates: {} to {}",
                request.getLinePoid(), request.getReportStartDate(), request.getReportEndDate());

        validateDateRange(request);
        validateLineExists(request.getLinePoid());

        List<LinePayableTransferReportingDtlDto> result = callWeeklyBlReport(request);

        long detRowId = 0L;
        for (LinePayableTransferReportingDtlDto dto : result) {
            dto.setDetRowId(++detRowId);
        }

        enrichDetailListWithLov(result);
        log.info("Processed {} weekly records before create", result.size());
        return result;
    }

    @Override
    public List<LinePayableTransferReportingDtlDto> applyExchangeRate(ApplyExchangeRateRequest request) {
        log.info("Applying exchange rate {} to currency {}", request.getCurrencyExchange(), request.getCurrencyCode());

        List<LinePayableTransferReportingDtlDto> details =
                request.getDetails() != null ? request.getDetails() : Collections.emptyList();

        BigDecimal rate = request.getCurrencyExchange();
        String currencyCode = request.getCurrencyCode().trim();

        for (LinePayableTransferReportingDtlDto detail : details) {
            // Legacy compares the row currency case insensitively and leaves other rows untouched
            if (detail.getCurrencyCode() == null || !detail.getCurrencyCode().equalsIgnoreCase(currencyCode)) {
                continue;
            }

            detail.setCurrencyExchange(toThreeDecimals(rate));

            // Legacy recomputes both amounts from CurrencyAmount * new rate
            if (detail.getCurrencyAmount() != null) {
                BigDecimal converted = toThreeDecimals(detail.getCurrencyAmount().multiply(rate));
                detail.setAcutalAmount(converted);
                detail.setTotalAmountTransfer(converted);
            }
        }

        // Returned in the same shape as the load endpoints, so the grid can be replaced wholesale
        enrichDetailListWithLov(details);
        return details;
    }

    // ==================== Private Helper Methods ====================

    /**
     * Run PROC_SHIP_REPORT_LINE_DATEWISE and materialise its ref cursor.
     *
     * <p>Nothing is written to the database here, so the caller decides whether the rows are
     * persisted (edit of a saved document) or only returned (new document).
     */
    private List<LinePayableTransferReportingDtlDto> callReportLineDatewise(LoadDataByDateRangeRequest request) {
        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        String chargeFilter = resolveChargeFilter(request.getChargeFilter());

        log.info("Calling PROC_SHIP_REPORT_LINE_DATEWISE with groupPoid={}, companyPoid={}, linePoid={}, blType={}, chargeFilter={}, dates={} to {}",
                groupPoid, companyPoid, request.getLinePoid(), request.getBlType(), chargeFilter,
                request.getReportStartDate(), request.getReportEndDate());

        List<LinePayableTransferReportingDtlDto> result = new ArrayList<>();
        try {
            String sql = "{call PROC_SHIP_REPORT_LINE_DATEWISE(?,?,?,?,?,?,?,?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, request.getLinePoid());
                cs.setString(4, request.getBlType());
                cs.setDate(5, Date.valueOf(request.getReportStartDate()));
                cs.setDate(6, Date.valueOf(request.getReportEndDate()));
                cs.setString(7, chargeFilter);
                cs.registerOutParameter(8, Types.REF_CURSOR);
                cs.execute();

                try (ResultSet rs = (ResultSet) cs.getObject(8)) {
                    if (rs == null) {
                        // Legacy stops here and tells the user nothing came back
                        throw new ValidationException(MSG_NO_DATA_FOUND);
                    }
                    while (rs.next()) {
                        result.add(LinePayableTransferReportingDtlDto.builder()
                                .mainfestTransactionPoid(getLongOrNull(rs, COL_TRANSACTION_POID))
                                .blNumber(rs.getString("BL_NUMBER"))
                                .acutalAmount(toThreeDecimals(getBigDecimalOrNull(rs, "ACTUAL_AMOUNT")))
                                .totalAmountTransfer(toThreeDecimals(getBigDecimalOrNull(rs, "ACTUAL_AMOUNT")))
                                .isSelect("Y") // Legacy marks every loaded row as selected
                                .chargePoid(getLongOrNull(rs, "CHARGE_POID"))
                                .freightType(rs.getString("FREIGHT_TYPE"))
                                .currencyCode(rs.getString("CURRENCY_CODE"))
                                .currencyExchange(toThreeDecimals(getBigDecimalOrNull(rs, "CURRENCY_EXCHANGE")))
                                .currencyAmount(toThreeDecimals(getBigDecimalOrNull(rs, "CURRENCY_AMOUNT")))
                                .build());
                    }
                }
                return null;
            });
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_REPORT_LINE_DATEWISE", e);
            throw new ValidationException(MSG_LOAD_FAILED);
        }
        return result;
    }

    /**
     * Run PROC_weekly_bl_report and materialise its ref cursor, without writing anything
     */
    private List<LinePayableTransferReportingDtlDto> callWeeklyBlReport(LoadDataByDateRangeRequest request) {
        log.info("Calling PROC_weekly_bl_report with linePoid={}, dates={} to {}",
                request.getLinePoid(), request.getReportStartDate(), request.getReportEndDate());

        List<LinePayableTransferReportingDtlDto> result = new ArrayList<>();
        try {
            String sql = "{call PROC_weekly_bl_report(?,?,?,?,?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setDate(1, Date.valueOf(request.getReportStartDate()));
                cs.setDate(2, Date.valueOf(request.getReportEndDate()));
                cs.setString(3, DOC_ID);
                cs.setLong(4, request.getLinePoid());
                cs.registerOutParameter(5, Types.REF_CURSOR);
                cs.execute();

                try (ResultSet rs = (ResultSet) cs.getObject(5)) {
                    if (rs == null) {
                        throw new ValidationException(MSG_NO_DATA_FOUND);
                    }
                    while (rs.next()) {
                        result.add(LinePayableTransferReportingDtlDto.builder()
                                .mainfestTransactionPoid(getLongOrNull(rs, "BL_POID"))
                                .blNumber(rs.getString("BL_NUMBER"))
                                .acutalAmount(toThreeDecimals(getBigDecimalOrNull(rs, "MANIFEST_AMOUNT")))
                                .totalAmountTransfer(toThreeDecimals(getBigDecimalOrNull(rs, "CHARGEAMOUNT_LOCAL")))
                                .isSelect("N")
                                .chargePoid(getLongOrNull(rs, "WKYRPT_INCLUDE_POID"))
                                .freightType(rs.getString("BL_TYPE"))
                                .currencyCode(rs.getString("CURRENCY_CODE"))
                                .currencyExchange(toThreeDecimals(getBigDecimalOrNull(rs, "CURRENCY_EXCHANGE")))
                                .currencyAmount(toThreeDecimals(getBigDecimalOrNull(rs, "THC_AMOUNT")))
                                .build());
                    }
                }
                return null;
            });
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling PROC_weekly_bl_report", e);
            throw new ValidationException(MSG_LOAD_FAILED);
        }
        return result;
    }

    /**
     * Run {@code work} in its own transaction, which is committed before this method returns
     */
    private <T> T inNewTransaction(Supplier<T> work) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> work.get());
    }

    /**
     * Post save hook legacy runs from DocumentAfterSave.
     *
     * <p>PROC_SHIP_BL_PAGE_SAVE_AFTER is declared PRAGMA AUTONOMOUS_TRANSACTION, so it runs in a
     * transaction of its own. For SHIPLNRPTAFTERSAVE it deletes the SHIP_LINE_REPORT_TRANSFER_DTL
     * rows left with IS_SELECT = 'N' and commits. Called from inside the saving transaction it
     * would see none of the rows just written, and its DELETE would block on rows this session
     * still holds locked, so the save must be committed first. Legacy does the same: it runs the
     * procedure on a separate connection from DocumentAfterSave, once the document is committed.
     *
     * <p>A failure therefore leaves the document saved, which is also how legacy behaves.
     */
    private void callProcShipBlPageSaveAfter(Long groupPoid, Long companyPoid, Long transactionPoid) {
        try {
            String sql = "{call PROC_SHIP_BL_PAGE_SAVE_AFTER(?,?,?,?,?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, transactionPoid);
                cs.setNull(4, Types.NUMERIC);
                cs.setString(5, AFTER_SAVE_UPDATE_TYPE);
                cs.execute();
                return null;
            });
            log.debug("Successfully called PROC_SHIP_BL_PAGE_SAVE_AFTER for transaction: {}", transactionPoid);
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_BL_PAGE_SAVE_AFTER for transaction: {}", transactionPoid, e);
            throw new ValidationException("Error on after save processing: " + e.getMessage());
        }
    }

    /**
     * Resolve the charge filter passed to PROC_SHIP_REPORT_LINE_DATEWISE.
     *
     * <p>An absent filter means ALL, which is what legacy sends when both page checkboxes are
     * ticked, their state on a new document.
     */
    private String resolveChargeFilter(String chargeFilter) {
        if (chargeFilter == null || chargeFilter.isBlank()) {
            return CHARGE_FILTER_ALL;
        }
        String normalized = chargeFilter.trim().toUpperCase(Locale.ROOT);
        if (!CHARGE_FILTERS.contains(normalized)) {
            throw new ValidationException("Charge filter must be one of: ALL, FRTTHC, OTHERS");
        }
        return normalized;
    }

    /**
     * The document id the log entries are keyed on, falling back to the page's own id when the
     * request carries none
     */
    private String resolveDocumentId() {
        String documentId = UserContext.getDocumentId();
        return documentId != null && !documentId.isBlank() ? documentId : DOC_ID;
    }

    private void validateDateRange(LoadDataByDateRangeRequest request) {
        if (request.getReportStartDate().isAfter(request.getReportEndDate())) {
            throw new ValidationException("Report start date must be less than or equal to end date");
        }
    }

    private void validateLineExists(Long linePoid) {
        Integer count = jdbcTemplate.queryForObject(SQL_CHECK_LINE, Integer.class, linePoid);
        if (count == null || count == 0) {
            throw new ValidationException("Line not found: " + linePoid);
        }
    }

    /**
     * Validate the BL type against the REPORT_CNT_BL_TYPE list of values the legacy dropdown is
     * bound to. The check is skipped when the LOV yields nothing, so an unavailable LOV cannot
     * block a save.
     */
    private void validateBlType(String blType) {
        List<String> codes = getBlTypeCodes();
        if (codes.isEmpty()) {
            log.warn("LOV {} returned no entries, skipping BL type validation", LOV_BL_TYPE);
            return;
        }
        boolean known = codes.stream().anyMatch(code -> code.equalsIgnoreCase(blType.trim()));
        if (!known) {
            throw new ValidationException("BL type must be one of: " + String.join(", ", codes));
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getBlTypeCodes() {
        try {
            Map<String, Object> lov = lovDataService.getLovList("", getGroupPoid(), getCompanyPoid(),
                    UserContext.getUserPoid(), LOV_BL_TYPE, 0, 0, "", "");
            if (lov == null) {
                return Collections.emptyList();
            }
            List<LovGetListDto> data = (List<LovGetListDto>) lov.get("data");
            if (data == null) {
                return Collections.emptyList();
            }
            return data.stream()
                    .map(LovGetListDto::getCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not read LOV {} for BL type validation", LOV_BL_TYPE, e);
            return Collections.emptyList();
        }
    }

    /**
     * Validate create DTO
     */
    private void validateCreateDTO(LinePayableTransferReportingCreateDTO dto) {
        if (dto.getLinePoid() == null) {
            throw new ValidationException("Line POID is required");
        }

        // Validate line exists (without GROUP_POID/COMPANY_POID filter)
        validateLineExists(dto.getLinePoid());

        if (dto.getBlType() == null || dto.getBlType().trim().isEmpty()) {
            throw new ValidationException("BL type is required");
        }

        validateBlType(dto.getBlType());

        if (dto.getReportStartDate() == null) {
            throw new ValidationException("Report start date is required");
        }

        if (dto.getReportEndDate() == null) {
            throw new ValidationException("Report end date is required");
        }

        if (dto.getReportStartDate().isAfter(dto.getReportEndDate())) {
            throw new ValidationException("Report start date must be less than or equal to end date");
        }
    }

    /**
     * Validate update DTO
     */
    private void validateUpdateDTO(LinePayableTransferReportingUpdateDTO dto) {
        if (dto.getLinePoid() != null) {
            validateLineExists(dto.getLinePoid());
        }

        if (dto.getBlType() != null && !dto.getBlType().trim().isEmpty()) {
            validateBlType(dto.getBlType());
        }

        if (dto.getReportStartDate() != null && dto.getReportEndDate() != null) {
            if (dto.getReportStartDate().isAfter(dto.getReportEndDate())) {
                throw new ValidationException("Report start date must be less than or equal to end date");
            }
        }
    }

    /**
     * Check if data should be reloaded
     */
    private boolean checkShouldReloadData(ShipLineReportTransferHdr entity, LinePayableTransferReportingUpdateDTO dto) {
        // A field the payload omits is left untouched by the mapper, so it is not a change
        return isFilterFieldChanged(entity.getLinePoid(), dto.getLinePoid()) ||
                isFilterFieldChanged(entity.getBlType(), dto.getBlType()) ||
                isFilterFieldChanged(entity.getReportStartDate(), dto.getReportStartDate()) ||
                isFilterFieldChanged(entity.getReportEndDate(), dto.getReportEndDate());
    }

    private boolean isFilterFieldChanged(Object storedValue, Object incomingValue) {
        return incomingValue != null && !Objects.equals(storedValue, incomingValue);
    }

    /**
     * Save detail tables from DTO
     */
    private void saveDetailTables(LinePayableTransferReportingCreateDTO dto, Long transactionPoid) {
        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            Long detRowId = dtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
            if (detRowId == null) detRowId = 0L;

            for (LinePayableTransferReportingDtlDto dtlDto : dto.getDetails()) {
                detRowId++;
                ShipLineReportTransferDtl dtl = mapper.mapDtlFromDto(dtlDto, transactionPoid, detRowId);
                dtlRepository.save(dtl);
                logDetailRowCreated(dtl, transactionPoid);
            }
        }
    }

    /**
     * Log a single created detail row
     */
    private void logDetailRowCreated(ShipLineReportTransferDtl dtl, Long transactionPoid) {
        String logDetail = String.format(LOG_ROW_CREATED, dtl.getDetRowId());
        loggingService.createLogSummaryEntry(resolveDocumentId(), transactionPoid.toString(), logDetail);
    }

    /**
     * Update detail tables from DTO.
     *
     * <p>When the filter (line / BL type / report dates) changed, the stored rows were loaded for a
     * different filter and no longer apply, so they are replaced wholesale by the payload rows.
     * When the filter is unchanged the payload is an edit of the same rows, so each row is matched
     * against the stored one by DET_ROW_ID and only the actual field changes are logged.
     */
    private void updateDetailTables(LinePayableTransferReportingUpdateDTO dto, Long transactionPoid, boolean filterChanged) {
        if (filterChanged) {
            replaceDetailTables(dto, transactionPoid);
        } else {
            syncDetailTables(dto, transactionPoid);
        }
    }

    /**
     * Delete every stored detail row and insert the rows coming in the payload
     */
    private void replaceDetailTables(LinePayableTransferReportingUpdateDTO dto, Long transactionPoid) {
        // Get existing details for logging deletions
        List<ShipLineReportTransferDtl> existingDetails = dtlRepository.findByTransactionPoid(transactionPoid);
        
        // Log deletions
        existingDetails.forEach(deleted -> loggingService.logDelete(deleted, resolveDocumentId(), transactionPoid.toString()));
        
        // Delete existing details
        dtlRepository.deleteByTransactionPoid(transactionPoid);

        // The bulk delete bypasses the persistence context, so the rows just loaded are still
        // held there as managed entities. Detach them, otherwise saving a new detail that reuses
        // a DET_ROW_ID is merged onto the stale instance and flushed as an UPDATE of a deleted row.
        existingDetails.forEach(entityManager::detach);

        // Save new details
        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            Long detRowId = 0L;
            for (LinePayableTransferReportingDtlDto dtlDto : dto.getDetails()) {
                detRowId++;
                ShipLineReportTransferDtl dtl = mapper.mapDtlFromDto(dtlDto, transactionPoid, detRowId);
                dtlRepository.save(dtl);
                logDetailRowCreated(dtl, transactionPoid);
            }
        }
    }

    /**
     * Keep the stored detail rows and apply the payload onto them, logging the changed fields of
     * each row. Rows the payload no longer carries are deleted, extra rows are created.
     */
    private void syncDetailTables(LinePayableTransferReportingUpdateDTO dto, Long transactionPoid) {
        Map<Long, ShipLineReportTransferDtl> storedRows = dtlRepository.findByTransactionPoid(transactionPoid).stream()
                .collect(Collectors.toMap(ShipLineReportTransferDtl::getDetRowId, dtl -> dtl,
                        (first, second) -> first, LinkedHashMap::new));

        List<LinePayableTransferReportingDtlDto> incomingRows =
                dto.getDetails() != null ? dto.getDetails() : Collections.emptyList();

        // The payload carries no DET_ROW_ID, so rows are matched by their position, which is how
        // they were numbered when they were stored
        Long detRowId = 0L;
        for (LinePayableTransferReportingDtlDto dtlDto : incomingRows) {
            detRowId++;
            ShipLineReportTransferDtl incoming = mapper.mapDtlFromDto(dtlDto, transactionPoid, detRowId);
            ShipLineReportTransferDtl stored = storedRows.remove(detRowId);

            if (stored == null) {
                dtlRepository.save(incoming);
                logDetailRowCreated(incoming, transactionPoid);
                continue;
            }

            // Snapshot before the row is mutated, so the changed fields can be logged
            ShipLineReportTransferDtl oldRow = new ShipLineReportTransferDtl();
            BeanUtils.copyProperties(stored, oldRow);

            applyDetailChanges(stored, incoming);
            ShipLineReportTransferDtl saved = dtlRepository.save(stored);

            // Only the changed fields are captured, as detail log entries keyed on the row
            loggingService.createLog(oldRow, saved, ShipLineReportTransferDtl.class, resolveDocumentId(),
                    transactionPoid.toString(), String.format(LOG_ROW_KEY, COL_DET_ROW_ID, detRowId));
        }

        // Whatever the payload no longer carries has been removed
        storedRows.values().forEach(removed -> {
            loggingService.logDelete(removed, resolveDocumentId(), transactionPoid.toString());
            dtlRepository.delete(removed);
        });
    }

    /**
     * Copy the payload driven columns of a detail row onto the stored row
     */
    private void applyDetailChanges(ShipLineReportTransferDtl stored, ShipLineReportTransferDtl incoming) {
        stored.setMainfestTransactionPoid(incoming.getMainfestTransactionPoid());
        stored.setBlNumber(incoming.getBlNumber());
        stored.setAcutalAmount(incoming.getAcutalAmount());
        stored.setTotalAmountTransfer(incoming.getTotalAmountTransfer());
        stored.setIsSelect(incoming.getIsSelect());
        stored.setChargePoid(incoming.getChargePoid());
        stored.setFreightType(incoming.getFreightType());
        stored.setCurrencyCode(incoming.getCurrencyCode());
        stored.setCurrencyExchange(incoming.getCurrencyExchange());
        stored.setCurrencyAmount(incoming.getCurrencyAmount());
    }

    /**
     * Build the response after a save, reading the header and details back so whatever
     * PROC_SHIP_BL_PAGE_SAVE_AFTER changed is reflected in what the caller gets
     */
    private LinePayableTransferReportingDto reloadAfterSave(Long transactionPoid) {
        ShipLineReportTransferHdr reloaded = hdrRepository.findActiveByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line Payable Transfer As Per Reporting",
                        "transactionPoid", transactionPoid.toString()));

        LinePayableTransferReportingDto dto = mapper.mapToDto(reloaded);
        loadDetailTables(dto, transactionPoid);
        enrichLovData(dto);
        return dto;
    }

    /**
     * Load detail tables into DTO
     */
    private void loadDetailTables(LinePayableTransferReportingDto dto, Long transactionPoid) {
        List<ShipLineReportTransferDtl> details = dtlRepository.findByTransactionPoid(transactionPoid);
        dto.setDetails(mapper.mapDtlListToDto(details));
    }

    private void enrichLovData(LinePayableTransferReportingDto dto) {
        // --- Header ---
        if (dto.getLinePoid() != null) {
            Map<Long, LovGetListDto> lineMap = lovDataService.getDetailsByPoidsAndLovName(
                    List.of(dto.getLinePoid()), LOV_LINE_MASTER);
            LovGetListDto lineLov = lineMap.get(dto.getLinePoid());
            if (lineLov != null) {
                dto.setLineDet(lineLov);
                dto.setLineName(lineLov.getDescription());
                dto.setLineCode(lineLov.getCode());
            }
        }

        enrichDetailListWithLov(dto.getDetails());
    }

    /**
     * Fill in the LOV descriptions of a detail list, one batch call per LOV type.
     *
     * <p>The lookups run on the calling thread on purpose: LovDataService reads the group, company
     * and user from the request scoped {@code UserContext}, which is a plain ThreadLocal and is not
     * visible from a pooled worker thread.
     */
    private void enrichDetailListWithLov(List<LinePayableTransferReportingDtlDto> details) {
        if (details == null || details.isEmpty()) return;

        List<Long> mainfestPoids = details.stream()
                .map(LinePayableTransferReportingDtlDto::getMainfestTransactionPoid)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        List<Long> chargePoids = details.stream()
                .map(LinePayableTransferReportingDtlDto::getChargePoid)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        List<String> currencyCodes = details.stream()
                .map(LinePayableTransferReportingDtlDto::getCurrencyCode)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<Long, LovGetListDto> mainfestMap = lovDataService.getDetailsByPoidsAndLovName(mainfestPoids, LOV_ALL_BL_NUMBER);
        Map<Long, LovGetListDto> chargeMap = lovDataService.getDetailsByPoidsAndLovName(chargePoids, LOV_CHARGE_MASTER);
        Map<String, LovGetListDto> currencyMap = lovDataService.getDetailsByCodesAndLovName(currencyCodes, LOV_CURRENCY);

        for (LinePayableTransferReportingDtlDto detail : details) {
            if (detail.getMainfestTransactionPoid() != null) {
                LovGetListDto lov = mainfestMap.get(detail.getMainfestTransactionPoid());
                if (lov != null) {
                    detail.setMainfestDet(lov);
                    detail.setBlNumber(lov.getCode());
                }
            }
            if (detail.getChargePoid() != null) {
                LovGetListDto lov = chargeMap.get(detail.getChargePoid());
                if (lov != null) {
                    detail.setChargeDet(lov);
                    detail.setChargeDescription(lov.getDescription());
                    detail.setChargeCode(lov.getCode());
                }
            }
            if (detail.getCurrencyCode() != null) {
                LovGetListDto lov = currencyMap.get(detail.getCurrencyCode());
                if (lov != null) {
                    detail.setCurrencyDet(lov);
                    detail.setCurrencyName(lov.getDescription());
                }
            }
        }
    }

    // Utility methods for safe value extraction
    private Long getLongOrNull(ResultSet rs, String columnName) {
        try {
            Object value = rs.getObject(columnName);
            if (value == null) return null;
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getBigDecimalOrNull(ResultSet rs, String columnName) {
        try {
            Object value = rs.getObject(columnName);
            if (value == null) return null;
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            }
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toThreeDecimals(BigDecimal value) {
        return value == null ? null : value.setScale(3, RoundingMode.HALF_UP);
    }
}
