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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
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

    @Override
    @Transactional
    public LinePayableTransferReportingDto createLinePayableTransfer(LinePayableTransferReportingCreateDTO createDTO) {
        log.info("Creating Line Payable Transfer As Per Reporting record");

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();

        // Validate
        validateCreateDTO(createDTO, companyPoid);

        // Create entity
        ShipLineReportTransferHdr entity = new ShipLineReportTransferHdr();
        mapper.mapCreateDTOToEntity(createDTO, entity, groupPoid, companyPoid);

        // Save entity (generates TRANSACTION_POID via IDENTITY)
        ShipLineReportTransferHdr saved = hdrRepository.save(entity);
        hdrRepository.flush();
        // Bypass Hibernate L1 cache to get trigger-generated values (TRANSACTION_POID, DOC_REF)
        entityManager.refresh(saved);

        // If line, BL type, and dates are provided, load data via stored procedure
//        if (saved.getLinePoid() != null && saved.getBlType() != null
//                && saved.getReportStartDate() != null && saved.getReportEndDate() != null) {
//            loadDataByDateRange(saved.getTransactionPoid(),
//                    LoadDataByDateRangeRequest.builder()
//                            .linePoid(saved.getLinePoid())
//                            .blType(saved.getBlType())
//                            .reportStartDate(saved.getReportStartDate())
//                            .reportEndDate(saved.getReportEndDate())
//                            .build());
//        }

        // Log the header before the details, so the header entry precedes its rows
        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), saved.getTransactionPoid().toString(),
                String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), saved.getDocRef()));

        // Save detail tables from DTO (if provided)
        saveDetailTables(createDTO, saved.getTransactionPoid());

        LinePayableTransferReportingDto result = mapper.mapToDto(saved);
        loadDetailTables(result, saved.getTransactionPoid());
        enrichLovData(result);

        log.info("Successfully created Line Payable Transfer As Per Reporting with id: {}", saved.getTransactionPoid());
        return result;
    }

    @Override
    @Transactional
    public LinePayableTransferReportingDto updateLinePayableTransfer(Long transactionPoid, LinePayableTransferReportingUpdateDTO updateDTO) {
        log.info("Updating Line Payable Transfer As Per Reporting with id: {}", transactionPoid);

        Long companyPoid = getCompanyPoid();

        ShipLineReportTransferHdr entity = hdrRepository.findActiveByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Line Payable Transfer As Per Reporting", "transactionPoid", transactionPoid.toString()));

        // Validate
        validateUpdateDTO(updateDTO, companyPoid);

        // Check if data loading parameters changed. Must be evaluated before the entity is mutated
        boolean filterChanged = checkShouldReloadData(entity, updateDTO);

        // Snapshot the header before it is mutated, so the changed fields can be logged
        ShipLineReportTransferHdr oldEntity = new ShipLineReportTransferHdr();
        BeanUtils.copyProperties(entity, oldEntity);

        // Update entity
        mapper.mapUpdateDTOToEntity(updateDTO, entity);

        ShipLineReportTransferHdr saved = hdrRepository.save(entity);

        // If parameters changed, reload data
//        if (filterChanged && saved.getLinePoid() != null && saved.getBlType() != null
//                && saved.getReportStartDate() != null && saved.getReportEndDate() != null) {
//            loadDataByDateRange(saved.getTransactionPoid(),
//                    LoadDataByDateRangeRequest.builder()
//                            .linePoid(saved.getLinePoid())
//                            .blType(saved.getBlType())
//                            .reportStartDate(saved.getReportStartDate())
//                            .reportEndDate(saved.getReportEndDate())
//                            .build());
//        }

        // Log the header before the details, so the header entries precede their rows
        loggingService.createLogSummaryEntry(com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString(),
                String.format("%s %s", LogDetailsEnum.MODIFIED.getDescription(), saved.getDocRef()));

        // Log the changed header fields as detail entries
        loggingService.logDetails(oldEntity, saved, ShipLineReportTransferHdr.class,
                com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString(), COL_TRANSACTION_POID);

        // Update detail tables
        updateDetailTables(updateDTO, saved.getTransactionPoid(), filterChanged);

        LinePayableTransferReportingDto result = mapper.mapToDto(saved);
        loadDetailTables(result, saved.getTransactionPoid());
        enrichLovData(result);

        log.info("Successfully updated Line Payable Transfer As Per Reporting with id: {}", transactionPoid);
        return result;
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

        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString());

        log.info("Successfully deleted Line Payable Transfer As Per Reporting with id: {}", transactionPoid);
    }

    @Override
    @Transactional
    public List<LinePayableTransferReportingDtlDto> loadDataByDateRange(Long transactionPoid, LoadDataByDateRangeRequest request) {
        log.info("Loading data by date range for transaction: {}, line: {}, BL type: {}, dates: {} to {}",
                transactionPoid, request.getLinePoid(), request.getBlType(),
                request.getReportStartDate(), request.getReportEndDate());

        // Validate
        if (request.getReportStartDate().isAfter(request.getReportEndDate())) {
            throw new ValidationException("Report start date must be less than or equal to end date");
        }

        // Validate line exists
        String checkLineSql = "SELECT COUNT(*) FROM SHIP_LINE_MASTER WHERE LINE_POID = ? AND NVL(DELETED,'N') = 'N'";
        Integer lineCount = jdbcTemplate.queryForObject(checkLineSql, Integer.class, request.getLinePoid());
        if (lineCount == null || lineCount == 0) {
            throw new ValidationException("Line not found: " + request.getLinePoid());
        }

        // Clear existing details
        dtlRepository.deleteByTransactionPoid(transactionPoid);

        List<LinePayableTransferReportingDtlDto> result = new ArrayList<>();

        try {
            Long groupPoid = getGroupPoid();
            Long companyPoid = getCompanyPoid();

            String sql = "{call PROC_SHIP_REPORT_LINE_DATEWISE(?,?,?,?,?,?,?,?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, request.getLinePoid());
                cs.setString(4, request.getBlType());
                cs.setDate(5, Date.valueOf(request.getReportStartDate()));
                cs.setDate(6, Date.valueOf(request.getReportEndDate()));
                cs.setString(7, resolveChargeFilter(request.getChargeFilter()));
                cs.registerOutParameter(8, Types.REF_CURSOR);
                cs.execute();

                try (ResultSet rs = (ResultSet) cs.getObject(8)) {
                    if (rs != null) {
                        Long detRowId = dtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
                        if (detRowId == null) detRowId = 0L;

                        while (rs.next()) {
                            detRowId++;

                            LinePayableTransferReportingDtlDto dto = LinePayableTransferReportingDtlDto.builder()
                                    .mainfestTransactionPoid(getLongOrNull(rs, "TRANSACTION_POID"))
                                    .blNumber(rs.getString("BL_NUMBER"))
                                    .acutalAmount(toThreeDecimals(getBigDecimalOrNull(rs, "ACTUAL_AMOUNT")))
                                    .totalAmountTransfer(toThreeDecimals(getBigDecimalOrNull(rs, "ACTUAL_AMOUNT"))) // Default to actual amount
                                    .isSelect("Y") // Default to not selected
                                    .chargePoid(getLongOrNull(rs, "CHARGE_POID"))
                                    .freightType(rs.getString("FREIGHT_TYPE"))
                                    .currencyCode(rs.getString("CURRENCY_CODE"))
                                    .currencyExchange(toThreeDecimals(getBigDecimalOrNull(rs, "CURRENCY_EXCHANGE")))
                                    .currencyAmount(toThreeDecimals(getBigDecimalOrNull(rs, "CURRENCY_AMOUNT")))
                                    .build();

                            result.add(dto);

                            // Save to database
                            ShipLineReportTransferDtl dtl = mapper.mapDtlFromDto(dto, transactionPoid, detRowId);
                            dtlRepository.save(dtl);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_REPORT_LINE_DATEWISE", e);
            throw new ValidationException("Error loading data by date range: " + e.getMessage());
        }

        if (result.isEmpty()) {
            log.warn("No data found for line: {}, BL type: {}, dates: {} to {}",
                    request.getLinePoid(), request.getBlType(),
                    request.getReportStartDate(), request.getReportEndDate());
        } else {
            log.info("Loaded {} records for transaction: {}", result.size(), transactionPoid);
        }

        return result;
    }

    @Override
    @Transactional
    public List<LinePayableTransferReportingDtlDto> processWeeklyBlReport(Long transactionPoid, LoadDataByDateRangeRequest request) {
        log.info("Processing weekly BL report for transaction: {}, line: {}, dates: {} to {}",
                transactionPoid, request.getLinePoid(), request.getReportStartDate(), request.getReportEndDate());

        if (request.getReportStartDate().isAfter(request.getReportEndDate())) {
            throw new ValidationException("Report start date must be less than or equal to end date");
        }

        dtlRepository.deleteByTransactionPoid(transactionPoid);
        List<LinePayableTransferReportingDtlDto> result = new ArrayList<>();

        try {
            Long groupPoid = getGroupPoid();
            Long companyPoid = getCompanyPoid();
            log.info("Calling PROC_weekly_bl_report with groupPoid={}, companyPoid={}, linePoid={}, dates={} to {}",
                    groupPoid, companyPoid, request.getLinePoid(), request.getReportStartDate(), request.getReportEndDate());

            String sql = "{call PROC_weekly_bl_report(?,?,?,?,?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setDate(1, Date.valueOf(request.getReportStartDate()));
                cs.setDate(2, Date.valueOf(request.getReportEndDate()));
                cs.setString(3, DOC_ID);
                cs.setLong(4, request.getLinePoid());
                cs.registerOutParameter(5, Types.REF_CURSOR);
                cs.execute();

                try (ResultSet rs = (ResultSet) cs.getObject(5)) {
                    if (rs != null) {
                        Long detRowId = 0L;
                        while (rs.next()) {
                            detRowId++;
                            LinePayableTransferReportingDtlDto dto = LinePayableTransferReportingDtlDto.builder()
                                    .detRowId(detRowId)
                                    .mainfestTransactionPoid(getLongOrNull(rs, "BL_POID"))
                                    .blNumber(rs.getString("BL_NUMBER"))
                                    .acutalAmount(getBigDecimalOrNull(rs, "manifest_amount"))
                                    .totalAmountTransfer(getBigDecimalOrNull(rs, "chargeamount_local"))
                                    .isSelect("N")
                                    .chargePoid(getLongOrNull(rs, "WKYRPT_INCLUDE_POID"))
                                    .freightType(rs.getString("BL_TYPE"))
                                    .currencyCode(rs.getString("CURRENCY_CODE"))
                                    .currencyExchange(getBigDecimalOrNull(rs, "CURRENCY_EXCHANGE"))
                                    .currencyAmount(getBigDecimalOrNull(rs, "THC_AMOUNT"))
                                    .build();
                            result.add(dto);
                            ShipLineReportTransferDtl dtl = mapper.mapDtlFromDto(dto, transactionPoid, detRowId);
                            dtlRepository.save(dtl);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_weekly_bl_report", e);
            throw new ValidationException("Error processing weekly BL report: " + e.getMessage());
        }

        log.info("Processed {} records for transaction: {}", result.size(), transactionPoid);
        return result;
    }

    @Override
    public List<LinePayableTransferReportingDtlDto> loadDataBeforeCreate(LoadDataByDateRangeRequest request) {
        log.info("Loading data before create for line: {}, BL type: {}, dates: {} to {}",
                request.getLinePoid(), request.getBlType(), request.getReportStartDate(), request.getReportEndDate());

        if (request.getReportStartDate().isAfter(request.getReportEndDate())) {
            throw new ValidationException("Report start date must be less than or equal to end date");
        }

        List<LinePayableTransferReportingDtlDto> result = new ArrayList<>();

        try {
            Long groupPoid = getGroupPoid();
            Long companyPoid = getCompanyPoid();
            log.info("Calling PROC_SHIP_REPORT_LINE_DATEWISE with groupPoid={}, companyPoid={}, linePoid={}, blType={}, dates={} to {}",
                    groupPoid, companyPoid, request.getLinePoid(), request.getBlType(), 
                    request.getReportStartDate(), request.getReportEndDate());

            String sql = "{call PROC_SHIP_REPORT_LINE_DATEWISE(?,?,?,?,?,?,?,?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, request.getLinePoid());
                cs.setString(4, request.getBlType());
                cs.setDate(5, Date.valueOf(request.getReportStartDate()));
                cs.setDate(6, Date.valueOf(request.getReportEndDate()));
                cs.setString(7, resolveChargeFilter(request.getChargeFilter()));
                cs.registerOutParameter(8, Types.REF_CURSOR);
                cs.execute();

                try (ResultSet rs = (ResultSet) cs.getObject(8)) {
                    if (rs != null) {
                        while (rs.next()) {
                            result.add(LinePayableTransferReportingDtlDto.builder()
                                    .detRowId((long) (result.size() + 1))
                                    .mainfestTransactionPoid(getLongOrNull(rs, "TRANSACTION_POID"))
                                    .blNumber(rs.getString("BL_NUMBER"))
                                    .acutalAmount(toThreeDecimals(getBigDecimalOrNull(rs, "ACTUAL_AMOUNT")))
                                    .totalAmountTransfer(toThreeDecimals(getBigDecimalOrNull(rs, "ACTUAL_AMOUNT")))
                                    .isSelect("Y")
                                    .chargePoid(getLongOrNull(rs, "CHARGE_POID"))
                                    .freightType(rs.getString("FREIGHT_TYPE"))
                                    .currencyCode(rs.getString("CURRENCY_CODE"))
                                    .currencyExchange(toThreeDecimals(getBigDecimalOrNull(rs, "CURRENCY_EXCHANGE")))
                                    .currencyAmount(toThreeDecimals(getBigDecimalOrNull(rs, "CURRENCY_AMOUNT")))
                                    .build());
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_REPORT_LINE_DATEWISE", e);
            throw new ValidationException("Error loading data: " + e.getMessage());
        }

        enrichDetailListWithLov(result);
        log.info("Loaded {} records before create", result.size());
        return result;
    }

    @Override
    public List<LinePayableTransferReportingDtlDto> processWeeklyBeforeCreate(LoadDataByDateRangeRequest request) {
        log.info("Processing weekly BL report before create for line: {}, dates: {} to {}",
                request.getLinePoid(), request.getReportStartDate(), request.getReportEndDate());

        if (request.getReportStartDate().isAfter(request.getReportEndDate())) {
            throw new ValidationException("Report start date must be less than or equal to end date");
        }

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
                    if (rs != null) {
                        Long detRowId = 0L;
                        while (rs.next()) {
                            detRowId++;
                            result.add(LinePayableTransferReportingDtlDto.builder()
                                    .detRowId(detRowId)
                                    .mainfestTransactionPoid(getLongOrNull(rs, "BL_POID"))
                                    .blNumber(rs.getString("BL_NUMBER"))
                                    .acutalAmount(getBigDecimalOrNull(rs, "manifest_amount"))
                                    .totalAmountTransfer(getBigDecimalOrNull(rs, "chargeamount_local"))
                                    .isSelect("N")
                                    .chargePoid(getLongOrNull(rs, "WKYRPT_INCLUDE_POID"))
                                    .freightType(rs.getString("BL_TYPE"))
                                    .currencyCode(rs.getString("CURRENCY_CODE"))
                                    .currencyExchange(getBigDecimalOrNull(rs, "CURRENCY_EXCHANGE"))
                                    .currencyAmount(getBigDecimalOrNull(rs, "THC_AMOUNT"))
                                    .build());
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_weekly_bl_report", e);
            throw new ValidationException("Error processing weekly report: " + e.getMessage());
        }

        enrichDetailListWithLov(result);
        log.info("Processed {} weekly records before create", result.size());
        return result;
    }

    // ==================== Private Helper Methods ====================

    private String resolveChargeFilter(String chargeFilter) {
        if (chargeFilter == null || chargeFilter.isBlank()) {
            return "ALL";
        }
        return chargeFilter.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Generate DOC_REF using company code and sequence
     */
    private String generateDocRef(Long companyPoid) {
        try {
            String companyCode = jdbcTemplate.queryForObject(
                    "SELECT GET_COMPANY_CODE(?) FROM DUAL",
                    String.class,
                    companyPoid
            );

            Long sequenceNo = jdbcTemplate.queryForObject(
                    "SELECT RTN_GLOBAL_SEQ_NO(?) FROM DUAL",
                    Long.class,
                    companyPoid
            );

            return companyCode + "-" + String.format("%05d", sequenceNo);
        } catch (Exception e) {
            log.error("Error generating DOC_REF", e);
            throw new ValidationException("Error generating document reference: " + e.getMessage());
        }
    }

    /**
     * Validate create DTO
     */
    private void validateCreateDTO(LinePayableTransferReportingCreateDTO dto, Long companyPoid) {
        if (dto.getLinePoid() == null) {
            throw new ValidationException("Line POID is required");
        }

        // Validate line exists (without GROUP_POID/COMPANY_POID filter)
        String checkLineSql = "SELECT COUNT(*) FROM SHIP_LINE_MASTER WHERE LINE_POID = ? AND NVL(DELETED,'N') = 'N'";
        Integer count = jdbcTemplate.queryForObject(checkLineSql, Integer.class, dto.getLinePoid());
        if (count == null || count == 0) {
            throw new ValidationException("Line not found: " + dto.getLinePoid());
        }

        if (dto.getBlType() == null || dto.getBlType().trim().isEmpty()) {
            throw new ValidationException("BL type is required");
        }

        if (!"IMPORT".equalsIgnoreCase(dto.getBlType()) && !"EXPORT".equalsIgnoreCase(dto.getBlType())) {
            throw new ValidationException("BL type must be IMPORT or EXPORT");
        }

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
    private void validateUpdateDTO(LinePayableTransferReportingUpdateDTO dto, Long companyPoid) {
        if (dto.getLinePoid() != null) {
            String checkLineSql = "SELECT COUNT(*) FROM SHIP_LINE_MASTER WHERE LINE_POID = ? AND NVL(DELETED,'N') = 'N'";
            Integer count = jdbcTemplate.queryForObject(checkLineSql, Integer.class, dto.getLinePoid());
            if (count == null || count == 0) {
                throw new ValidationException("Line not found: " + dto.getLinePoid());
            }
        }

        if (dto.getBlType() != null && !dto.getBlType().trim().isEmpty()) {
            if (!"IMPORT".equalsIgnoreCase(dto.getBlType()) && !"EXPORT".equalsIgnoreCase(dto.getBlType())) {
                throw new ValidationException("BL type must be IMPORT or EXPORT");
            }
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
        loggingService.createLogSummaryEntry(DOC_ID, transactionPoid.toString(), logDetail);
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
        existingDetails.forEach(deleted -> loggingService.logDelete(deleted, DOC_ID, transactionPoid.toString()));
        
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
            loggingService.createLog(oldRow, saved, ShipLineReportTransferDtl.class, DOC_ID,
                    transactionPoid.toString(), String.format(LOG_ROW_KEY, COL_DET_ROW_ID, detRowId));
        }

        // Whatever the payload no longer carries has been removed
        storedRows.values().forEach(removed -> {
            loggingService.logDelete(removed, DOC_ID, transactionPoid.toString());
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

        if (dto.getDetails() == null || dto.getDetails().isEmpty()) return;

        // --- Gather distinct IDs/codes from all detail rows ---
        List<Long> mainfestPoids = dto.getDetails().stream()
                .map(LinePayableTransferReportingDtlDto::getMainfestTransactionPoid)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        List<Long> chargePoids = dto.getDetails().stream()
                .map(LinePayableTransferReportingDtlDto::getChargePoid)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        List<String> currencyCodes = dto.getDetails().stream()
                .map(LinePayableTransferReportingDtlDto::getCurrencyCode)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());

        // --- Single batch call per LOV type ---
        Map<Long, LovGetListDto> mainfestMap = lovDataService.getDetailsByPoidsAndLovName(mainfestPoids, LOV_ALL_BL_NUMBER);
        Map<Long, LovGetListDto> chargeMap = lovDataService.getDetailsByPoidsAndLovName(chargePoids, LOV_CHARGE_MASTER);
        Map<String, LovGetListDto> currencyMap = lovDataService.getDetailsByCodesAndLovName(currencyCodes, LOV_CURRENCY);

        // --- Apply to each detail ---
        for (LinePayableTransferReportingDtlDto detail : dto.getDetails()) {
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

        CompletableFuture<Map<Long, LovGetListDto>> mainfestFuture = CompletableFuture.supplyAsync(
                () -> lovDataService.getDetailsByPoidsAndLovName(mainfestPoids, LOV_ALL_BL_NUMBER));
        CompletableFuture<Map<Long, LovGetListDto>> chargeFuture = CompletableFuture.supplyAsync(
                () -> lovDataService.getDetailsByPoidsAndLovName(chargePoids, LOV_CHARGE_MASTER));
        CompletableFuture<Map<String, LovGetListDto>> currencyFuture = CompletableFuture.supplyAsync(
                () -> lovDataService.getDetailsByCodesAndLovName(currencyCodes, LOV_CURRENCY));

        CompletableFuture.allOf(mainfestFuture, chargeFuture, currencyFuture).join();

        Map<Long, LovGetListDto> mainfestMap = mainfestFuture.join();
        Map<Long, LovGetListDto> chargeMap = chargeFuture.join();
        Map<String, LovGetListDto> currencyMap = currencyFuture.join();

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
