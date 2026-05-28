package com.asg.shipping.shipcommisiontransfer.service;


import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.shipcommisiontransfer.dto.PdaFdaDtlResponseDTO;
import com.asg.shipping.shipcommisiontransfer.entity.PdaFdaDtl;
import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionDtlId;
import com.asg.shipping.shipcommisiontransfer.repository.PdaFdaDtlRepository;
import com.asg.shipping.shipcommisiontransfer.dto.*;
import com.asg.shipping.shipcommisiontransfer.enums.ActionType;
import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionDtl;
import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionHdr;
import com.asg.shipping.shipcommisiontransfer.repository.ShipBlCommissionDtlRepository;
import com.asg.shipping.shipcommisiontransfer.repository.ShipBlCommissionHdrRepository;
import com.asg.shipping.shipcommisiontransfer.util.ShipCommissionTransferMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Service implementation for Ship Commission Transfer operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShipCommissionTransferServiceImpl implements ShipCommissionTransferService {

    private final ShipBlCommissionHdrRepository headerRepository;
    private final ShipBlCommissionDtlRepository detailRepository;
    private final DocumentSearchService documentService;
    private final LoggingService loggingService;
    private final ShipCommissionTransferMapper mapper;
    private final JdbcTemplate jdbcTemplate;
    private final PdaFdaDtlRepository pdaFdaDtlRepository;
    private final DocumentDeleteService documentDeleteService;

    private static final String TRANSACTION_POID = "transactionPoid";
    private static final String SHIP_COMMISSION_TRANSFER = "Ship Commission Transfer";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchShipCommissionTransfer(String docId, com.asg.common.lib.dto.FilterRequestDto request, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Searching ship commission transfer records with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);

        RawSearchResult raw = documentService.search(
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
    //@Transactional(readOnly = true)
    public ShipCommissionTransferDto getShipCommissionTransfer(Long id) {
        log.info("Getting ship commission transfer with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        ShipBlCommissionHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(SHIP_COMMISSION_TRANSFER,TRANSACTION_POID , id.toString()));

        if ("Y".equals(entity.getDeleted())) {
            throw new ResourceNotFoundException(SHIP_COMMISSION_TRANSFER, TRANSACTION_POID, id.toString());
        }

        // Load detail records
        List<ShipBlCommissionDtl> detailRecords = detailRepository.findByTransactionPoidOrderByDetRowId(id);

        ShipCommissionTransferDto dto = mapper.mapToDto(entity);
        dto.setCommissionDetails(mapper.mapDtlListToDto(detailRecords));

        // Enrich with LOV data
        enrichWithLovData(dto);

        return dto;
    }

    @Override
    @Transactional
    public ShipCommissionTransferDto createShipCommissionTransfer(ShipCommissionTransferCreateDTO createDTO) {
        log.info("Creating new ship commission transfer");

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        // Create header entity
        ShipBlCommissionHdr entity = new ShipBlCommissionHdr();
        mapper.mapCreateDTOToEntity(createDTO, entity, groupPoid, companyPoid);

        // Generate DOC_REF (from trigger logic: RTN_GLOBAL_SEQ_NO('SHIPPING_COMMISSION', companyCode, NULL))
        String docRef = generateDocRef(companyPoid);
        entity.setDocRef(docRef);

        // Save header first
        entity = headerRepository.save(entity);

        // Save detail records
        saveDetailRecords(entity.getTransactionPoid(), createDTO.getCommissionDetails());

        // Call PROC_SHIP_BL_PAGE_SAVE_AFTER for post-save processing
        callProcShipBlPageSaveAfter(groupPoid, companyPoid, entity.getTransactionPoid(), null, "COMMISSION_SAVE",
                com.asg.common.lib.security.util.UserContext.getUserPoid());

        // Log creation
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED,
                com.asg.common.lib.security.util.UserContext.getDocumentId(),
                entity.getTransactionPoid().toString());

        // Reload and return
        return getShipCommissionTransfer(entity.getTransactionPoid());
    }

    @Override
    @Transactional
    public ShipCommissionTransferDto updateShipCommissionTransfer(Long id, ShipCommissionTransferUpdateDTO updateDTO) {
        log.info("Updating ship commission transfer with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        ShipBlCommissionHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(SHIP_COMMISSION_TRANSFER,TRANSACTION_POID, id.toString()));

        ShipBlCommissionHdr oldEntity = new ShipBlCommissionHdr();
        BeanUtils.copyProperties(entity, oldEntity);

        if ("Y".equals(entity.getDeleted())) {
            throw new ResourceNotFoundException(SHIP_COMMISSION_TRANSFER, TRANSACTION_POID, id.toString());
        }

        // Update header
        mapper.mapUpdateDTOToEntity(updateDTO, entity);
        ShipBlCommissionHdr shipBlCommissionHdr = headerRepository.save(entity);

        // Update detail records by actionType
        updateDetailRecords(id, updateDTO.getCommissionDetails());

        // Call PROC_SHIP_BL_PAGE_SAVE_AFTER for post-save processing
        callProcShipBlPageSaveAfter(groupPoid, companyPoid, id, null, "COMMISSION_UPDATE",
                com.asg.common.lib.security.util.UserContext.getUserPoid());

        // Log update
        loggingService.logChanges(oldEntity, shipBlCommissionHdr, ShipBlCommissionHdr.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        // Reload and return
        return getShipCommissionTransfer(id);
    }

    @Override
    @Transactional
    public void deleteShipCommissionTransfer(Long id, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting ship commission transfer with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        ShipBlCommissionHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(SHIP_COMMISSION_TRANSFER, TRANSACTION_POID, id.toString()));

        documentDeleteService.deleteDocument(
                id,
                "SHIP_BL_COMMISSION_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                entity.getTransactionDate()
        );
    }

    @Override
    @Transactional
    public Map<String, Object> calculateCommission(Long transactionPoid, CalculateCommissionRequestDTO request) {
        log.info("Calculating commission for transaction: {}", transactionPoid);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        ShipBlCommissionHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(SHIP_COMMISSION_TRANSFER, TRANSACTION_POID, transactionPoid.toString()));

        if ("Y".equals(entity.getDeleted())) {
            throw new ResourceNotFoundException(SHIP_COMMISSION_TRANSFER, TRANSACTION_POID, transactionPoid.toString());
        }

        // Call PROC_SHIP_COMMISSION_CALCULATE to calculate commission amounts
        callProcShipCommissionCalculate(
                transactionPoid,
                entity.getVoyageTransactionPoid(),
                BigDecimal.ZERO,
                entity.getCurrencyExchange() != null ? entity.getCurrencyExchange() : BigDecimal.ONE,
                "N"
        );

        // Call PROC_SHIP_COMSS_HANDL_CALN to calculate handling charges
        callProcShipComssHandlCaln(
                transactionPoid,
                entity.getVoyageTransactionPoid(),
                BigDecimal.ZERO,
                entity.getCurrencyExchange() != null ? entity.getCurrencyExchange() : BigDecimal.ONE
        );

        // Reload detail records to get calculated amounts
        List<ShipBlCommissionDtl> detailRecords = detailRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);

        // Call PROC_SHIP_BL_PAGE_SAVE_AFTER for post-save processing
        callProcShipBlPageSaveAfter(groupPoid, companyPoid, transactionPoid, null, "COMMISSION_CALCULATE",
                com.asg.common.lib.security.util.UserContext.getUserPoid());

        // Calculate totals
        BigDecimal totalCommission = detailRecords.stream()
                .filter(d -> d.getCommissionAmt() != null)
                .map(ShipBlCommissionDtl::getCommissionAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put(TRANSACTION_POID, transactionPoid);
        result.put("calculatedDetails", detailRecords.size());
        result.put("totalCommission", totalCommission);

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> loadFromVoyage(Long transactionPoid) {
        log.info("Loading commission data from voyage for transaction: {}", transactionPoid);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        ShipBlCommissionHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(SHIP_COMMISSION_TRANSFER, TRANSACTION_POID, transactionPoid.toString()));

        if ("Y".equals(entity.getDeleted())) {
            throw new ResourceNotFoundException(SHIP_COMMISSION_TRANSFER, TRANSACTION_POID, transactionPoid.toString());
        }

        // Validate document is saved first
        if (entity.getDocRef() == null || entity.getDocRef().trim().isEmpty()) {
            throw new ValidationException("Document must be saved before loading data from voyage");
        }

        if (entity.getVoyageTransactionPoid() == null) {
            throw new ValidationException("Voyage Transaction POID is required to load data from voyage");
        }

        // Call PROC_MATE_RCPT_EMPTY_MANIFEST to load data from voyage
        String result = callProcMateRcptEmptyManifest(transactionPoid, getCurrentUser());

        // Reload detail records
        List<ShipBlCommissionDtl> detailRecords = detailRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);

        Map<String, Object> response = new HashMap<>();
        response.put(TRANSACTION_POID, transactionPoid);
        response.put("loadedDetails", detailRecords.size());
        response.put("message", result);

        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> insertPdaCommission(Long transactionPoid) {
        log.info("Inserting commission data into PDA for transaction: {}", transactionPoid);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        ShipBlCommissionHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(SHIP_COMMISSION_TRANSFER, TRANSACTION_POID, transactionPoid.toString()));

        if ("Y".equals(entity.getDeleted())) {
            throw new ResourceNotFoundException(SHIP_COMMISSION_TRANSFER, TRANSACTION_POID, transactionPoid.toString());
        }

        // Validate required data exists
        if (entity.getFdaTransactionPoid() == null) {
            throw new ValidationException("FDA Transaction POID is required to insert PDA commission");
        }

        // Call PROC_INSERT_PDA_COMMISSION to insert data into PDA
        String result = callProcInsertPdaCommission(transactionPoid, entity.getFdaTransactionPoid(), getCurrentUser());

        Map<String, Object> response = new HashMap<>();
        response.put(TRANSACTION_POID, transactionPoid);
        response.put("pdaStatus", result != null && !result.contains("ERROR") ? "SUCCESS" : "FAILED");
        response.put("message", result);

        return response;
    }


    public Map<String, String> getCurrencyExchangeForVoyage(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            Long voyageId
    ) {

        String sql = "{call PROC_LOV_AFTER_BRWS_300_103(?, ?, ?, ?, ?, ?, ?, ?)}";
        Map<String, String> result = new HashMap<>();

        jdbcTemplate.execute(sql, (CallableStatement cs) -> {

            try {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, userPoid);
                cs.setString(4, "100-152");              // DOC_ID
                cs.setLong(5, 0);                        // not used
                cs.setString(6, "VESSAL_VOYAGE");        // LOV_NAME
                cs.setString(7, String.valueOf(voyageId));
                cs.registerOutParameter(8, OracleTypes.CURSOR);

                cs.execute();

                ResultSet rs = (ResultSet) cs.getObject(8);

                if (rs != null && rs.next()) { // 👈 only first row
                    result.put("currencyCode", rs.getString("CURRENCY_CODE"));
                    result.put("exchangeRate", rs.getString("CURRENCY_EXCHANGE"));
                }

            } catch (Exception e) {
                log.error("Error fetching currency exchange: ", e);
            }

            return result;
        });

        return result;
    }

    public List<PdaFdaDtlResponseDTO> getPdaFdaDetails(Long transactionPoid) {

        List<PdaFdaDtl> list = pdaFdaDtlRepository.findByIdTransactionPoid(transactionPoid);

        return list.stream().map(entity -> PdaFdaDtlResponseDTO.builder()
                .detRowId(entity.getId().getDetRowId())
                .charge(entity.getChargePoid())
                .currencyCode(entity.getCurrencyCode())
                .currencyRate(entity.getCurrencyRate())
                .remarks(entity.getRemarks())
                .fdaAmount(entity.getFdaAmount())
                .build()
        ).toList();
    }

    @Override
    @Transactional
    public List<Object[]> getCommissionPending( Long voyageTransactionPoid, CommissionPendingRequestDTO request) {
        log.info("Fetching commission pending for voyageTransactionPoid: {}", voyageTransactionPoid);

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("PROC_SHIP_COMMISSION_RECORD_FETCH");

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_BL_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_VOYAGE_TRANSACTION_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_EXCHANGE", Double.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RECORD_TYPE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_FRT_BUY_ACTUAL", Double.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_SHORT_LEG_SELECTED", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("OUTDATA", void.class, jakarta.persistence.ParameterMode.REF_CURSOR);

        query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        query.setParameter("P_COMPANY_POID", UserContext.getCompanyPoid());
        query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        query.setParameter("P_DOC_ID", UserContext.getDocumentId());
        query.setParameter("P_BL_POID", request.getBlPoid() != null ? request.getBlPoid() : 0L);
        query.setParameter("P_VOYAGE_TRANSACTION_POID", voyageTransactionPoid);
        query.setParameter("P_EXCHANGE", request.getExchangeRate() != null ? request.getExchangeRate() : 1.0);
        query.setParameter("P_RECORD_TYPE", request.getRecordType() != null ? request.getRecordType() : "ALL");
        query.setParameter("P_FRT_BUY_ACTUAL", request.getFrtBuyActual() != null ? request.getFrtBuyActual() : 0.0);
        query.setParameter("P_SHORT_LEG_SELECTED", request.getShortLegSelected() != null ? request.getShortLegSelected() : "N");

        query.execute();

        return query.getResultList();
    }

    @Transactional
    public List<Object[]> getCommissionByVoyage(Long voyageTransactionPoid, Long transactionPoid) {

        return fetchShipCommissionRecords(
                UserContext.getGroupPoid(),        // loginGroupPoid (set default या session से लो)
                UserContext.getCompanyPoid(),        // loginCompanyPoid
                UserContext.getUserPoid(),        // loginUserPoid
                UserContext.getDocumentId(),      // docId
                transactionPoid,      // transactionPoid
                voyageTransactionPoid
                // exchange
                // recordType
                // frtBuyActual
                // shortLegSelected
        );
    }



    @SuppressWarnings("unchecked")
    private  List<Object[]> fetchShipCommissionRecords(
            Long loginGroupPoid,
            Long loginCompanyPoid,
            Long loginUserPoid,
            String docId,
            Long transactionPoid,
            Long voyageTransactionPoid
    ) {

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("QA_DB_USER.PROC_SHIP_COMMISSION_RECORD_FETCH");

        // Register IN params
        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_VOYAGE_TRANSACTION_POID", Long.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_EXCHANGE", Double.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RECORD_TYPE", String.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_FRT_BUY_ACTUAL", Double.class, jakarta.persistence.ParameterMode.IN);
        query.registerStoredProcedureParameter("P_SHORT_LEG_SELECTED", String.class, jakarta.persistence.ParameterMode.IN);

        // Register OUT cursor
        query.registerStoredProcedureParameter("OUTDATA", void.class, jakarta.persistence.ParameterMode.REF_CURSOR);

        // Set values
        query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
        query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
        query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
        query.setParameter("P_DOC_ID", docId);
        query.setParameter("P_TRANSACTION_POID", transactionPoid);
        query.setParameter("P_VOYAGE_TRANSACTION_POID", voyageTransactionPoid);
        query.setParameter("P_EXCHANGE", 1.0);
        query.setParameter("P_RECORD_TYPE", "ALL");
        query.setParameter("P_FRT_BUY_ACTUAL", 0.0);
        query.setParameter("P_SHORT_LEG_SELECTED", "N");

        // Execute
        query.execute();

        return query.getResultList();
    }

    /**
     * Call PROC_SHIP_BL_PAGE_SAVE_AFTER stored procedure
     */
    private void callProcShipBlPageSaveAfter(Long groupPoid, Long companyPoid, Long transactionPoid, Long splitBookingNo, String actionType, Long userPoid) {
        try {
            String sql = "{call PROC_SHIP_BL_PAGE_SAVE_AFTER(?, ?, ?, ?, ?, ?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setObject(3, splitBookingNo != null ? splitBookingNo : transactionPoid);
                cs.setObject(4, splitBookingNo != null ? transactionPoid : null);
                cs.setString(5, actionType != null ? actionType : "COMMISSION_SAVE");
                cs.setLong(6, userPoid);
                cs.execute();
                return null;
            });
            log.debug("Successfully called PROC_SHIP_BL_PAGE_SAVE_AFTER for transaction: {}", transactionPoid);
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_BL_PAGE_SAVE_AFTER for transaction: {}", transactionPoid, e);
            // Don't throw exception - this is a post-save operation
        }
    }

    /**
     * Call PROC_SHIP_COMMISSION_CALCULATE stored procedure
     */
    private void callProcShipCommissionCalculate(Long transactionPoid, Long voyageTransactionPoid, BigDecimal commissionOnAmount, BigDecimal exchange, String shortLegSelected) {
        try {
            String sql = "{? = call PROC_SHIP_COMMISSION_CALCULATE(?, ?, ?, ?, ?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.registerOutParameter(1, Types.NUMERIC);
                cs.setObject(2, voyageTransactionPoid, Types.NUMERIC);
                cs.setObject(3, transactionPoid, Types.NUMERIC);
                cs.setObject(4, commissionOnAmount, Types.NUMERIC);
                cs.setObject(5, exchange, Types.NUMERIC);
                cs.setString(6, shortLegSelected);
                cs.execute();
                return null;
            });
            log.debug("Successfully called PROC_SHIP_COMMISSION_CALCULATE for transaction: {}", transactionPoid);
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_COMMISSION_CALCULATE for transaction: {}", transactionPoid, e);
            throw new ValidationException("Error calculating commission: " + e.getMessage());
        }
    }

    /**
     * Call PROC_SHIP_COMSS_HANDL_CALN stored procedure
     */
    private void callProcShipComssHandlCaln(Long transactionPoid, Long voyageTransactionPoid, BigDecimal commissionOnAmount, BigDecimal exchange) {
        try {
            String sql = "{? = call PROC_SHIP_COMSS_HANDL_CALN(?, ?, ?, ?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.registerOutParameter(1, Types.NUMERIC);
                cs.setObject(2, voyageTransactionPoid, Types.NUMERIC);
                cs.setObject(3, transactionPoid, Types.NUMERIC);
                cs.setObject(4, commissionOnAmount, Types.NUMERIC);
                cs.setObject(5, exchange, Types.NUMERIC);
                cs.execute();
                return null;
            });
            log.debug("Successfully called PROC_SHIP_COMSS_HANDL_CALN for transaction: {}", transactionPoid);
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_COMSS_HANDL_CALN for transaction: {}", transactionPoid, e);
            throw new ValidationException("Error calculating commission handling charges: " + e.getMessage());
        }
    }

    /**
     * Call PROC_MATE_RCPT_EMPTY_MANIFEST stored procedure
     */
    private String callProcMateRcptEmptyManifest(Long transactionPoid, String user) {
        try {
            String sql = "{call PROC_MATE_RCPT_EMPTY_MANIFEST(?, ?, ?)}";
            return jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, transactionPoid);
                cs.setString(2, user);
                cs.registerOutParameter(3, Types.VARCHAR);
                cs.execute();
                return cs.getString(3);
            });
        } catch (Exception e) {
            log.error("Error calling PROC_MATE_RCPT_EMPTY_MANIFEST for transaction: {}", transactionPoid, e);
            throw new ValidationException("Error loading data from voyage: " + e.getMessage());
        }
    }

    /**
     * Call PROC_INSERT_PDA_COMMISSION stored procedure
     */
    private String callProcInsertPdaCommission(Long transactionPoid, Long fdaTransactionPoid, String userCode) {
        try {
            String sql = "{call PROC_INSERT_PDA_COMMISSION(?, ?, ?, ?)}";
            return jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, transactionPoid);
                cs.setString(2, userCode);
                cs.setObject(3, fdaTransactionPoid, Types.NUMERIC);
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.execute();
                return cs.getString(4);
            });
        } catch (Exception e) {
            log.error("Error calling PROC_INSERT_PDA_COMMISSION for transaction: {}", transactionPoid, e);
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("Financial Period")) {
                    throw new ValidationException("Changes allowed only within current Financial Period");
                }
                if (errorMsg.contains("ORA-01403") || errorMsg.contains("no data found")) {
                    throw new ValidationException("Required commission data not found. Please ensure commission details are calculated before inserting to PDA");
                }
            }
            throw new ValidationException("Error inserting commission into PDA: " + errorMsg);
        }
    }

    private void updateDetailRecords(Long transactionPoid, List<ShipCommissionDetailDto> detailDtos) {
        if (detailDtos == null || detailDtos.isEmpty()) return;
        Long maxDetRowId = detailRepository.getMaxDetRowId(transactionPoid);
        for (ShipCommissionDetailDto dto : detailDtos) {
            ActionType action = dto.getActionType();
            if (action == null || action == ActionType.noChange) continue;

            if (action == ActionType.isCreated) {
                ShipBlCommissionDtl entity = mapper.mapDtlFromDto(dto, transactionPoid);
                entity.setDetRowId(++maxDetRowId);
                ShipBlCommissionDtl saved = detailRepository.save(entity);
                String logDetail = String.format("Row Created on  with detRowId: %s", saved.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);

            } else if (action == ActionType.isUpdated) {
                detailRepository.findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId()).ifPresent(existing -> {
                    ShipBlCommissionDtl oldDetail = new ShipBlCommissionDtl();
                    BeanUtils.copyProperties(existing, oldDetail);
                    ShipBlCommissionDtl mapped = mapper.mapDtlFromDto(dto, transactionPoid);
                    BeanUtils.copyProperties(mapped, existing, "transactionPoid", "detRowId");
                    ShipBlCommissionDtl updated = detailRepository.save(existing);
                    String logDetail = String.format("KeyId = TRANSACTION_POID %s: DET_ROW_ID %s", updated.getTransactionPoid(), updated.getDetRowId());
                    loggingService.createLog(oldDetail, updated, ShipBlCommissionDtl.class, UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
                });

            } else if (action == ActionType.isDeleted) {
                ShipBlCommissionDtlId dtlId = new ShipBlCommissionDtlId();
                dtlId.setTransactionPoid(transactionPoid);
                dtlId.setDetRowId(dto.getDetRowId());
                detailRepository.deleteById(dtlId);
                String logDetail = String.format("Row Deleted on  with detRowId: %s", dto.getDetRowId());
                loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            }
        }
    }

    /**
     * Save detail records
     */
    private void saveDetailRecords(Long transactionPoid, List<ShipCommissionDetailDto> detailDtos) {
        if (detailDtos != null && !detailDtos.isEmpty()) {
            Long maxDetRowId = detailRepository.getMaxDetRowId(transactionPoid);
            for (ShipCommissionDetailDto dto : detailDtos) {
                ShipBlCommissionDtl entity = mapper.mapDtlFromDto(dto, transactionPoid);
                if (entity.getDetRowId() == null) {
                    entity.setDetRowId(++maxDetRowId);
                }
                detailRepository.save(entity);
            }
        }
    }

    /**
     * Generate DOC_REF using RTN_GLOBAL_SEQ_NO function
     */
    private String generateDocRef(Long companyPoid) {
        try {
            // Get company code using GET_COMPANY_CODE function
            String companyCode = jdbcTemplate.queryForObject(
                    "SELECT GET_COMPANY_CODE(?) FROM DUAL",
                    String.class,
                    companyPoid
            );

            // Generate sequence number using RTN_GLOBAL_SEQ_NO function
            String seqNo = jdbcTemplate.queryForObject(
                    "SELECT RTN_GLOBAL_SEQ_NO('SHIPPING_COMMISSION', ?, NULL) FROM DUAL",
                    String.class,
                    companyCode != null ? companyCode : "DEFAULT"
            );

            return seqNo != null ? seqNo : String.valueOf(System.currentTimeMillis());
        } catch (Exception e) {
            log.error("Error generating DOC_REF for companyPoid: {}", companyPoid, e);
            // Fallback to timestamp-based generation if function calls fail
            return "COMM-" + System.currentTimeMillis();
        }
    }

    /**
     * Enrich DTO with LOV data
     */
    private void enrichWithLovData(ShipCommissionTransferDto dto) {
        // LOV enrichment can be added here if needed
        // Currently minimal implementation to avoid dependency on LovService
        log.debug("LOV enrichment for Ship Commission Transfer: {}", dto.getTransactionPoid());
    }
}
