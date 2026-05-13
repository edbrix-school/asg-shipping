package com.asg.shipping.shippingmanifestcorrector.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.shippingmanifestcorrector.dto.*;
import com.asg.shipping.shippingmanifestcorrector.entity.*;
import com.asg.shipping.shippingmanifestcorrector.event.ManifestCorrectorSaveEvent;
import com.asg.shipping.shippingmanifestcorrector.repository.*;
import com.asg.shipping.shippingmanifestcorrector.util.ManifestCorrectorMapper;
import com.asg.shipping.shippingmanifestcorrector.dto.ManifestCorrectorChargeDtlDto;
import com.asg.shipping.shippingmanifestcorrector.dto.ValidateRefundAmountRequest;
import com.asg.shipping.shippingmanifestcorrector.dto.ValidateRefundAmountResponse;
import com.asg.shipping.shippingmanifestcorrector.entity.ShipBlReprintHdr;
import com.asg.shipping.shippingmanifestcorrector.repository.ShipBlReprintChargeDtlRepository;
import com.asg.shipping.shippingmanifestcorrector.repository.ShipBlReprintHdrRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.asg.common.lib.security.util.UserContext.*;

/**
 * Service implementation for Shipping Manifest Corrector operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManifestCorrectorServiceImpl implements ManifestCorrectorService {

    private final ShipBlReprintHdrRepository hdrRepository;
    private final ShipBlReprintChargeDtlRepository chargeDtlRepository;
    private final ShipBlReprintContainerDtlRepository containerDtlRepository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final JdbcTemplate jdbcTemplate;
    private final ManifestCorrectorMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchManifestCorrector(String docId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        log.info("Searching Shipping Manifest Corrector records with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentSearchService.resolveOperator(filters);
        String isDeleted = documentSearchService.resolveIsDeleted(filters);
        List<FilterDto> filterList = documentSearchService.resolveDateFilters(filters, "TRANSACTION_DATE", startDate,
                endDate);
        RawSearchResult raw = documentSearchService.search(
                docId,
                filterList,
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
    public ManifestCorrectorDto getManifestCorrectorById(Long transactionPoid) {
        log.info("Getting Shipping Manifest Corrector with id: {}", transactionPoid);

        ShipBlReprintHdr entity = hdrRepository.findActiveByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping Manifest Corrector", "transactionPoid", transactionPoid.toString()));

        ManifestCorrectorDto dto = mapper.mapToDto(entity);

        List<ShipBlReprintChargeDtl> charges = chargeDtlRepository.findByTransactionPoid(transactionPoid);
        List<ShipBlReprintContainerDtl> containers = containerDtlRepository.findByTransactionPoid(transactionPoid);

        dto.setChargesDetails(mapper.mapChargeDtlListToDto(charges));
        dto.setContainerDetails(mapper.mapContainerDtlListToDto(containers));

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString());

        log.info("Successfully retrieved Shipping Manifest Corrector with id: {}", transactionPoid);
        return dto;
    }

    @Override
    @Transactional
    public ManifestCorrectorDto createManifestCorrector(ManifestCorrectorCreateDTO createDTO) {
        log.info("Creating Shipping Manifest Corrector record");

        Long companyPoid = getCompanyPoid();

        validateCreateDTO(createDTO);

        ShipBlReprintHdr entity = new ShipBlReprintHdr();
        mapper.mapCreateDTOToEntity(createDTO, entity, companyPoid);

        ShipBlReprintHdr saved = hdrRepository.saveAndFlush(entity);

        handleReprintFlags(saved);

        saveDetailTables(createDTO, saved.getTransactionPoid());

        eventPublisher.publishEvent(new ManifestCorrectorSaveEvent(saved, Long.parseLong(saved.getBlNumber())));

        ManifestCorrectorDto result = mapper.mapToDto(saved);
        loadDetailTables(result, saved.getTransactionPoid());
//        enrichLovData(result);

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, com.asg.common.lib.security.util.UserContext.getDocumentId(), saved.getTransactionPoid().toString());

        log.info("Successfully created Shipping Manifest Corrector with id: {}", saved.getTransactionPoid());
        return result;
    }

    @Override
    @Transactional
    public ManifestCorrectorDto updateManifestCorrector(Long transactionPoid, ManifestCorrectorUpdateDTO updateDTO) {
        log.info("Updating Shipping Manifest Corrector with id: {}", transactionPoid);

        ShipBlReprintHdr entity = hdrRepository.findActiveByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping Manifest Corrector", "transactionPoid", transactionPoid.toString()));

        validateUpdateDTO(updateDTO);

        boolean flagsChanged = checkFlagsChanged(entity, updateDTO);

        mapper.mapUpdateDTOToEntity(updateDTO, entity);

        ShipBlReprintHdr saved = hdrRepository.saveAndFlush(entity);

        if (flagsChanged) {
            handleReprintFlags(saved);
        }

        updateDetailTables(updateDTO, saved.getTransactionPoid());

        callProcShipBlReprintAftSave(saved.getTransactionPoid(), Long.parseLong(saved.getBlNumber()));

        ManifestCorrectorDto result = mapper.mapToDto(saved);
        loadDetailTables(result, saved.getTransactionPoid());
//        enrichLovData(result);

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString());

        log.info("Successfully updated Shipping Manifest Corrector with id: {}", transactionPoid);
        return result;
    }

    @Override
    @Transactional
    public void deleteManifestCorrector(Long transactionPoid, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting Shipping Manifest Corrector with id: {}", transactionPoid);

        ShipBlReprintHdr entity = hdrRepository.findActiveByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping Manifest Corrector", "transactionPoid", transactionPoid.toString()));

        documentDeleteService.deleteDocument(
                transactionPoid,
                "SHIP_BL_REPRINT_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                java.time.LocalDate.now()
        );

        entity.setDeleted("Y");
        hdrRepository.saveAndFlush(entity);

        chargeDtlRepository.deleteByTransactionPoid(transactionPoid);
        containerDtlRepository.deleteByTransactionPoid(transactionPoid);

        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString());

        log.info("Successfully deleted Shipping Manifest Corrector with id: {}", transactionPoid);
    }

//    Demurrage Refund functionality has been dropped in the latest development

//    @Override
//    @Transactional(readOnly = true)
//    public List<ManifestCorrectorChargeDtlDto> loadDemurrageRefundCharges(Long transactionPoid, Long blPoid) {
//        log.info("Loading demurrage refund charges for BL POID: {}", blPoid);
//
//        List<ManifestCorrectorChargeDtlDto> result = new ArrayList<>();
//
//        try {
//            String sql = "{call PROC_SHIP_BL_REPRINT_DEM_LOAD(?,?)}";
//            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
//                cs.setLong(1, blPoid);
//                cs.registerOutParameter(2, Types.REF_CURSOR);
//                cs.execute();
//
//                try (ResultSet rs = (ResultSet) cs.getObject(2)) {
//                    if (rs != null) {
//                        while (rs.next()) {
//                            ManifestCorrectorChargeDtlDto dto = ManifestCorrectorChargeDtlDto.builder()
//                                    .chargePoid(getLongOrNull(rs, "CHARGE_POID"))
//                                    .chargeType(rs.getString("CHARGE_TYPE"))
//                                    .containerNumber(rs.getString("CONTAINER_NO"))
//                                    .equipmentIsoType(rs.getString("EQUIPMENT_ISO_TYPE"))
//                                    .buyPercharge(getBigDecimalOrNull(rs, "BUY_PERCHARGE"))
//                                    .perQuantityAmount(getBigDecimalOrNull(rs, "PER_QUANTITY_AMOUNT"))
//                                    .freightType(rs.getString("FREIGHT_TYPE"))
//                                    .revPayable(getBigDecimalOrNull(rs, "REV_PAYABLE"))
//                                    .revIncome(getBigDecimalOrNull(rs, "REV_INCOME"))
//                                    .currencyExchange(BigDecimal.ONE)
//                                    .quantity(BigDecimal.ONE)
//                                    .currencyCode("BHD")
//                                    .chargeBasisOn(rs.getString("EQUIPMENT_ISO_TYPE"))
//                                    .build();
//                            result.add(dto);
//                        }
//                    }
//                }
//                return null;
//            });
//        } catch (Exception e) {
//            log.error("Error calling PROC_SHIP_BL_REPRINT_DEM_LOAD", e);
//            throw new ValidationException("Error loading demurrage refund charges: " + e.getMessage());
//        }
//
//        if (result.isEmpty()) {
//            log.warn("No demurrage refund charges found for BL POID: {}", blPoid);
//        }
//
//        return result;
//    }

    @Override
    @Transactional(readOnly = true)
    public ValidateRefundAmountResponse validateRefundAmounts(ValidateRefundAmountRequest request) {
        log.info("Validating refund amounts for BL POID: {}, Container: {}", request.getBlPoid(), request.getContainerNumber());

        ValidateRefundAmountResponse response = ValidateRefundAmountResponse.builder()
                .valid(false)
                .build();

        try {
            String sql = "{call PROC_SHIP_BL_REPRINT_AMT_VAL(?,?,?,?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, request.getBlPoid());
                cs.setString(2, request.getContainerNumber());
                cs.registerOutParameter(3, Types.NUMERIC);
                cs.registerOutParameter(4, Types.NUMERIC);
                cs.execute();

                BigDecimal oldPayableAmt = getBigDecimalOrNull(cs, 3);
                BigDecimal oldIncomeAmt = getBigDecimalOrNull(cs, 4);

                response.setOldPayableAmt(oldPayableAmt != null ? oldPayableAmt : BigDecimal.ZERO);
                response.setOldIncomeAmt(oldIncomeAmt != null ? oldIncomeAmt : BigDecimal.ZERO);

                BigDecimal revPayable = request.getRevPayable() != null ? request.getRevPayable().abs() : BigDecimal.ZERO;
                BigDecimal revIncome = request.getRevIncome() != null ? request.getRevIncome().abs() : BigDecimal.ZERO;
                BigDecimal perQuantityAmount = request.getPerQuantityAmount() != null ? request.getPerQuantityAmount().abs() : BigDecimal.ZERO;

                BigDecimal totalRefund = revPayable.add(revIncome);

                if (totalRefund.compareTo(perQuantityAmount) <= 0) {
                    response.setValid(true);
                    BigDecimal calculatedAmount = totalRefund.negate();
                    response.setCalculatedBuyPercharge(calculatedAmount);
                    response.setCalculatedPerQuantityAmount(calculatedAmount);
                    response.setMessage("Validation successful");
                } else {
                    response.setValid(false);
                    response.setMessage("Sum of reverse payable and income (" + totalRefund + ") should be less than or equal to collected amount (" + perQuantityAmount + ")");
                }

                return null;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_BL_REPRINT_AMT_VAL", e);
            response.setValid(false);
            response.setMessage("Error validating refund amounts: " + e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManifestCorrectorChargeDtlDto> autoFillDoReprint(String blNumber) {
        log.info("Auto-filling DO reprint charges for BL: {}", blNumber);
        
        if (blNumber == null || blNumber.trim().isEmpty()) {
            throw new ValidationException("BL number is required");
        }
        
        Long blPoid = validateAndGetBlPoid(blNumber);
        return getChargesForDoReprint(blPoid);
    }

    @Override
    @Transactional(readOnly = true)
    public ContainerReprintResponse autoFillContainerReprint(String blNumber) {
        log.info("Auto-filling container reprint for BL: {}", blNumber);
        
        if (blNumber == null || blNumber.trim().isEmpty()) {
            throw new ValidationException("BL number is required");
        }
        
        Long blPoid = validateAndGetBlPoid(blNumber);
        
        List<ManifestCorrectorContainerDtlDto> containers = getContainersForReprint(blPoid);
        List<ManifestCorrectorChargeDtlDto> charges = getChargesForContainerReprint(blPoid);
        
        return ContainerReprintResponse.builder()
                .blNumber(blNumber)
                .containers(containers)
                .charges(charges)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManifestCorrectorChargeDtlDto> autoFillBlReprint(String blNumber) {
        log.info("Auto-filling BL reprint charges for BL: {}", blNumber);
        
        if (blNumber == null || blNumber.trim().isEmpty()) {
            throw new ValidationException("BL number is required");
        }
        
        Long blPoid = validateAndGetBlPoid(blNumber);
        return getChargesForBlReprint(blPoid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManifestCorrectorChargeDtlDto> autoFillDemRefund(String blNumber) {
        log.info("Auto-filling DEM refund charges for BL: {}", blNumber);
        
        if (blNumber == null || blNumber.trim().isEmpty()) {
            throw new ValidationException("BL number is required");
        }
        
        Long blPoid = validateAndGetBlPoid(blNumber);
        return getDemRefundCharges(blPoid);
    }

    @Override
    @Transactional(readOnly = true)
    public ManifestCorrectorBlAutoPopulateDto autoPopulateFromBlBrowse(
            String blNumber,
            ManifestCorrectorBlAutoPopulateRequest request) {
        log.info("Auto-populating Shipping Manifest Corrector fields for BL: {}", blNumber);

        if (blNumber == null || blNumber.trim().isEmpty()) {
            throw new ValidationException("BL number is required");
        }

        Long blPoid = validateAndGetBlPoid(blNumber);
        Long transactionPoid = request != null && request.getTransactionPoid() != null
                ? request.getTransactionPoid()
                : 0L;

        ManifestCorrectorBlAutoPopulateDto response = ManifestCorrectorBlAutoPopulateDto.builder()
                .blPoid(blPoid)
                .transactionPoid(transactionPoid)
                .build();

        try {
            String sql = "{call PRODUCTION.PROC_LOV_AFTER_BRWS_100_143(?,?,?,?,?,?,?,?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, getGroupPoid());
                cs.setLong(2, getCompanyPoid());
                cs.setLong(3, getUserPoid());
                cs.setString(4, getDocumentId());
                cs.setLong(5, transactionPoid);
                cs.setString(6, "SHIP_BL_REPRINT");
                cs.setString(7, blPoid.toString());
                cs.registerOutParameter(8, Types.REF_CURSOR);
                cs.execute();

                try (ResultSet rs = (ResultSet) cs.getObject(8)) {
                    if (rs == null || !rs.next()) {
                        throw new ValidationException("No auto-population data found for BL number: " + blNumber);
                    }

                    response.setConsigneePoid(getLongOrNull(rs, "CONSIGNEE_POID"));
                    response.setIssueType(rs.getString("ISSUE_TYPE"));
                    response.setNotifyPoid(getLongOrNull(rs, "NOTIFY_POID"));
                    response.setShipperEdiName(rs.getString("SHIPPER_EDI_NAME"));
                    response.setBlType(rs.getString("BL_TYPE"));
                    response.setHoldCanDo(rs.getString("HOLD_CAN_DO"));
                    response.setHoldReason(rs.getString("HOLD_REASON"));
                    response.setPayableGlPoid(getLongOrNull(rs, "PAYABLE_GL_POID"));
                    response.setIncomeGlPoid(getLongOrNull(rs, "INCOME_GL_POID"));
                    response.setPlaceOfDeliveryPoid(getLongOrNull(rs, "PLACE_OF_DELIEVERY_POID"));
                    response.setPlaceOfReceiptPoid(getLongOrNull(rs, "PLACE_OF_RECIEPT_POID"));
                    response.setBlPlaceReceipt(rs.getString("BL_PLACE_RECEIPT"));
                    response.setBlPlaceLoad(rs.getString("BL_PLACE_LOAD"));
                    response.setBlFinalDestination(rs.getString("BL_FINAL_DESTINATION"));
                    response.setBlPlaceDischargeDesc(rs.getString("BL_PLACE_DISCHARE_DESC"));
                    response.setPortOfLoadingPoid(getLongOrNull(rs, "PORT_OF_LOADING_POID"));
                    response.setPortOfDischargePoid(getLongOrNull(rs, "PORT_OF_DISCHARGE_POID"));
                    response.setVoyageTransactionPoid(getLongOrNull(rs, "VOYAGE_TRANSACTION_POID"));
                }
                return null;
            });
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling PROC_LOV_AFTER_BRWS_100_143 for BL: {}", blNumber, e);
            throw new ValidationException("Error auto-populating BL details: " + e.getMessage());
        }

        return response;
    }

    // ==================== Private Helper Methods ====================

    /**
     * Validate BL number and return BL POID
     */
    private Long validateAndGetBlPoid(String blNumber) {
        String checkBlSql = "SELECT COUNT(*) FROM SHIP_BL_MANIFEST_HDR WHERE TRANSACTION_POID = ? AND (DELETED = 'N' OR DELETED IS NULL)";
        Long blPoid = Long.parseLong(blNumber);
        Integer count = jdbcTemplate.queryForObject(checkBlSql, Integer.class, blPoid);
        if (count == null || count == 0) {
            throw new ValidationException("BL number not found: " + blNumber);
        }
        return blPoid;
    }

    /**
     * Get charges for DO reprint (PERBL basis, REPRINTIMP)
     */
    private List<ManifestCorrectorChargeDtlDto> getChargesForDoReprint(Long blPoid) {
        String sql = "SELECT MCDTL.CHARGE_CODE_POID, MCDTL.AMOUNT_OTHER, " +
                "CM.CHARGE_CODE, CM.CHARGE_NAME " +
                "FROM SHIP_PORT_CHARGES_HDR MCHDR " +
                "INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL ON MCHDR.TRANSACTION_POID = MCDTL.TRANSACTION_POID " +
                "LEFT JOIN SHIP_CHARGE_MASTER CM ON MCDTL.CHARGE_CODE_POID = CM.CHARGE_POID " +
                "WHERE (SELECT TO_DATE(NVL(VHDR.ARRIVAL_DATE, VHDR.EXPECTED_DATE)) " +
                "       FROM SHIP_VOYAGE_HDR VHDR " +
                "       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID " +
                "       WHERE BLHDR.TRANSACTION_POID = ?) BETWEEN MCHDR.PERIOD_FROM AND MCHDR.PERIOD_TO " +
                "AND NVL(MCHDR.CHARGE_LINE_POID, 0) = 0 " +
                "AND MCDTL.CHARGE_TYPE_APPLICABLE = 'REPRINTIMP' " +
                "AND MCDTL.CHARGE_APPLICABLE = 'PERBL' " +
                "AND NVL(MCHDR.DELETED, 'N') = 'N' " +
                "ORDER BY MCDTL.CHARGE_TYPE_APPLICABLE, MCDTL.CHARGE_APPLICABLE";

        List<Map<String, Object>> charges = jdbcTemplate.queryForList(sql, blPoid);
        List<ManifestCorrectorChargeDtlDto> result = new ArrayList<>();
        
        Long detRowId = 1L;
        for (Map<String, Object> charge : charges) {
            ManifestCorrectorChargeDtlDto dto = ManifestCorrectorChargeDtlDto.builder()
                    .detRowId(detRowId++)
                    .chargePoid(getLongFromMap(charge, "CHARGE_CODE_POID"))
                    .currencyExchange(BigDecimal.ONE)
                    .quantity(BigDecimal.ONE)
                    .perQuantityAmount(getBigDecimalFromMap(charge, "AMOUNT_OTHER"))
                    .chargeType("LOCAL")
                    .printGroup("Local Charge")
                    .currencyCode("BHD")
                    .chargeBasisOn("NOBASIS")
                    .freightType("C")
                    .build();
            result.add(dto);
        }
        
        return result;
    }

    /**
     * Get charges for container reprint (PERQUENTITY basis, REPRINTIMP)
     */
    private List<ManifestCorrectorChargeDtlDto> getChargesForContainerReprint(Long blPoid) {
        // First get container sizes - using a simpler approach
        String containerSizeSql = "SELECT EQUIPMENT_ISO_TYPE " +
                "FROM SHIP_BL_MANIFEST_CONTAINER_DTL " +
                "WHERE TRANSACTION_POID = ?";
        List<String> containerTypes = jdbcTemplate.queryForList(containerSizeSql, String.class, blPoid);

        // Count containers by size (assuming first 2 characters indicate size)
        long count20 = containerTypes.stream().filter(type -> type != null && type.startsWith("2")).count();
        long count40 = containerTypes.stream().filter(type -> type != null && type.startsWith("4")).count();

        // Load charges
        String sql = "SELECT MCDTL.CHARGE_CODE_POID, MCDTL.AMOUNT_20, MCDTL.AMOUNT_40, " +
                "CM.CHARGE_CODE, CM.CHARGE_NAME " +
                "FROM SHIP_PORT_CHARGES_HDR MCHDR " +
                "INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL ON MCHDR.TRANSACTION_POID = MCDTL.TRANSACTION_POID " +
                "LEFT JOIN SHIP_CHARGE_MASTER CM ON MCDTL.CHARGE_CODE_POID = CM.CHARGE_POID " +
                "WHERE (SELECT TO_DATE(NVL(VHDR.ARRIVAL_DATE, VHDR.EXPECTED_DATE)) " +
                "       FROM SHIP_VOYAGE_HDR VHDR " +
                "       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID " +
                "       WHERE BLHDR.TRANSACTION_POID = ?) BETWEEN MCHDR.PERIOD_FROM AND MCHDR.PERIOD_TO " +
                "AND NVL(MCHDR.CHARGE_LINE_POID, 0) = 0 " +
                "AND MCDTL.CHARGE_TYPE_APPLICABLE = 'REPRINTIMP' " +
                "AND MCDTL.CHARGE_APPLICABLE = 'PERQUENTITY' " +
                "AND NVL(MCHDR.DELETED, 'N') = 'N' " +
                "ORDER BY MCDTL.CHARGE_TYPE_APPLICABLE, MCDTL.CHARGE_APPLICABLE";

        List<Map<String, Object>> charges = jdbcTemplate.queryForList(sql, blPoid);
        List<ManifestCorrectorChargeDtlDto> result = new ArrayList<>();
        
        Long detRowId = 1L;
        for (Map<String, Object> charge : charges) {
            // Create charge for 20ft containers
            if (count20 > 0) {
                ManifestCorrectorChargeDtlDto dto20 = ManifestCorrectorChargeDtlDto.builder()
                        .detRowId(detRowId++)
                        .chargePoid(getLongFromMap(charge, "CHARGE_CODE_POID"))
                        .currencyExchange(BigDecimal.ONE)
                        .quantity(BigDecimal.valueOf(count20))
                        .perQuantityAmount(getBigDecimalFromMap(charge, "AMOUNT_20"))
                        .chargeType("LOCAL")
                        .printGroup("Local Charge")
                        .currencyCode("BHD")
                        .chargeBasisOn("NOBASIS")
                        .freightType("C")
                        .build();
                result.add(dto20);
            }

            // Create charge for 40ft containers
            if (count40 > 0) {
                ManifestCorrectorChargeDtlDto dto40 = ManifestCorrectorChargeDtlDto.builder()
                        .detRowId(detRowId++)
                        .chargePoid(getLongFromMap(charge, "CHARGE_CODE_POID"))
                        .currencyExchange(BigDecimal.ONE)
                        .quantity(BigDecimal.valueOf(count40))
                        .perQuantityAmount(getBigDecimalFromMap(charge, "AMOUNT_40"))
                        .chargeType("LOCAL")
                        .printGroup("Local Charge")
                        .currencyCode("BHD")
                        .chargeBasisOn("NOBASIS")
                        .freightType("C")
                        .build();
                result.add(dto40);
            }
        }
        
        return result;
    }

    /**
     * Get charges for BL reprint (PERBL basis, REPRINTEXP, PREPAID)
     */
    private List<ManifestCorrectorChargeDtlDto> getChargesForBlReprint(Long blPoid) {
        String sql = "SELECT MCDTL.CHARGE_CODE_POID, MCDTL.AMOUNT_OTHER, " +
                "CM.CHARGE_CODE, CM.CHARGE_NAME " +
                "FROM SHIP_PORT_CHARGES_HDR MCHDR " +
                "INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL ON MCHDR.TRANSACTION_POID = MCDTL.TRANSACTION_POID " +
                "LEFT JOIN SHIP_CHARGE_MASTER CM ON MCDTL.CHARGE_CODE_POID = CM.CHARGE_POID " +
                "WHERE (SELECT TO_DATE(NVL(VHDR.ARRIVAL_DATE, VHDR.EXPECTED_DATE)) " +
                "       FROM SHIP_VOYAGE_HDR VHDR " +
                "       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID " +
                "       WHERE BLHDR.TRANSACTION_POID = ?) BETWEEN MCHDR.PERIOD_FROM AND MCHDR.PERIOD_TO " +
                "AND NVL(MCHDR.CHARGE_LINE_POID, 0) = 0 " +
                "AND MCDTL.CHARGE_TYPE_APPLICABLE = 'REPRINTEXP' " +
                "AND MCDTL.CHARGE_APPLICABLE = 'PERBL' " +
                "AND NVL(MCHDR.DELETED, 'N') = 'N' " +
                "ORDER BY MCDTL.CHARGE_TYPE_APPLICABLE, MCDTL.CHARGE_APPLICABLE";

        List<Map<String, Object>> charges = jdbcTemplate.queryForList(sql, blPoid);
        List<ManifestCorrectorChargeDtlDto> result = new ArrayList<>();
        
        Long detRowId = 1L;
        for (Map<String, Object> charge : charges) {
            ManifestCorrectorChargeDtlDto dto = ManifestCorrectorChargeDtlDto.builder()
                    .detRowId(detRowId++)
                    .chargePoid(getLongFromMap(charge, "CHARGE_CODE_POID"))
                    .currencyExchange(BigDecimal.ONE)
                    .quantity(BigDecimal.ONE)
                    .perQuantityAmount(getBigDecimalFromMap(charge, "AMOUNT_OTHER"))
                    .chargeType("LOCAL")
                    .printGroup("Local Charge")
                    .currencyCode("BHD")
                    .chargeBasisOn("NOBASIS")
                    .freightType("P")
                    .build();
            result.add(dto);
        }
        
        return result;
    }

    /**
     * Get containers for reprint
     */
    private List<ManifestCorrectorContainerDtlDto> getContainersForReprint(Long blPoid) {
        String sql = "SELECT CONTAINER_NO, EQUIPMENT_ISO_TYPE, " +
                "NVL(PRINT_DELIVERY_FORM_DEFAULT, 'N') PRINT_DELIVERY_FORM_DEFAULT " +
                "FROM SHIP_BL_MANIFEST_CONTAINER_DTL " +
                "WHERE TRANSACTION_POID = ?";

        List<Map<String, Object>> containers = jdbcTemplate.queryForList(sql, blPoid);
        List<ManifestCorrectorContainerDtlDto> result = new ArrayList<>();
        
        Long detRowId = 1L;
        for (Map<String, Object> container : containers) {
            ManifestCorrectorContainerDtlDto dto = ManifestCorrectorContainerDtlDto.builder()
                    .detRowId(detRowId++)
                    .containerNumber((String) container.get("CONTAINER_NO"))
                    .containerType((String) container.get("EQUIPMENT_ISO_TYPE"))
                    .equipmentIsoType((String) container.get("EQUIPMENT_ISO_TYPE"))
                    .isSelectedDlv((String) container.get("PRINT_DELIVERY_FORM_DEFAULT"))
                    .isSelectedRtn("N")
                    .build();
            result.add(dto);
        }
        
        return result;
    }

    /**
     * Get DEM refund charges using stored procedure
     */
    private List<ManifestCorrectorChargeDtlDto> getDemRefundCharges(Long blPoid) {
        List<ManifestCorrectorChargeDtlDto> result = new ArrayList<>();

        try {
            String sql = "{call PROC_SHIP_BL_REPRINT_DEM_LOAD(?,?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, blPoid);
                cs.registerOutParameter(2, Types.REF_CURSOR);
                cs.execute();

                try (ResultSet rs = (ResultSet) cs.getObject(2)) {
                    if (rs != null) {
                        Long detRowId = 1L;
                        while (rs.next()) {
                            ManifestCorrectorChargeDtlDto dto = ManifestCorrectorChargeDtlDto.builder()
                                    .detRowId(detRowId++)
                                    .chargePoid(getLongOrNull(rs, "CHARGE_POID"))
                                    .containerNumber(rs.getString("CONTAINER_NO"))
                                    .equipmentIsoType(rs.getString("EQUIPMENT_ISO_TYPE"))
                                    .buyPercharge(getBigDecimalOrNull(rs, "BUY_PERCHARGE"))
                                    .perQuantityAmount(getBigDecimalOrNull(rs, "PER_QUANTITY_AMOUNT"))
                                    .freightType(rs.getString("FREIGHT_TYPE"))
                                    .revPayable(getBigDecimalOrNull(rs, "REV_PAYABLE"))
                                    .revIncome(getBigDecimalOrNull(rs, "REV_INCOME"))
                                    .currencyExchange(BigDecimal.ONE)
                                    .quantity(BigDecimal.ONE)
                                    .currencyCode("BHD")
                                    .chargeBasisOn(rs.getString("EQUIPMENT_ISO_TYPE"))
                                    .chargeType("LOCAL")
                                    .printGroup("Local Charge")
                                    .build();
                            result.add(dto);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_BL_REPRINT_DEM_LOAD", e);
            throw new ValidationException("Error loading demurrage refund charges: " + e.getMessage());
        }

        if (result.isEmpty()) {
            log.warn("No demurrage refund charges found for BL POID: {}", blPoid);
        }

        return result;
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
    private void validateCreateDTO(ManifestCorrectorCreateDTO dto) {
        if (dto.getBlNumber() == null || dto.getBlNumber().trim().isEmpty()) {
            throw new ValidationException("BL number is required");
        }

        String checkBlSql = "SELECT COUNT(*) FROM SHIP_BL_MANIFEST_HDR WHERE TRANSACTION_POID = ? AND (DELETED = 'N' OR DELETED IS NULL)";
        Long blPoid = Long.parseLong(dto.getBlNumber());
        Integer count = jdbcTemplate.queryForObject(checkBlSql, Integer.class, blPoid);
        if (count == null || count == 0) {
            throw new ValidationException("BL number not found: " + dto.getBlNumber());
        }

        validateMutuallyExclusiveFlags(dto.getDoReprint(), dto.getContainerReprint(),
                dto.getReturnReprint(), dto.getBlReprint(), dto.getDemRefund());

    }

    /**
     * Validate update DTO
     */
    private void validateUpdateDTO(ManifestCorrectorUpdateDTO dto) {
        if (dto.getBlNumber() != null && !dto.getBlNumber().trim().isEmpty()) {
            String checkBlSql = "SELECT COUNT(*) FROM SHIP_BL_MANIFEST_HDR WHERE TRANSACTION_POID = ? AND (DELETED = 'N' OR DELETED IS NULL)";
            Long blPoid = Long.parseLong(dto.getBlNumber());
            Integer count = jdbcTemplate.queryForObject(checkBlSql, Integer.class, blPoid);
            if (count == null || count == 0) {
                throw new ValidationException("BL number not found: " + dto.getBlNumber());
            }
        }

        validateMutuallyExclusiveFlags(dto.getDoReprint(), dto.getContainerReprint(),
                dto.getReturnReprint(), dto.getBlReprint(), dto.getDemRefund());

    }

    /**
     * Validate mutually exclusive reprint flags
     */
    private void validateMutuallyExclusiveFlags(String doReprint, String containerReprint, 
                                                String returnReprint, String blReprint, String demRefund) {
        int count = 0;
        if ("Y".equals(doReprint)) count++;
        if ("Y".equals(containerReprint)) count++;
        if ("Y".equals(returnReprint)) count++;
        if ("Y".equals(blReprint)) count++;
        if ("Y".equals(demRefund)) count++;

        if (count > 1) {
            throw new ValidationException("Only one reprint/refund option can be selected at a time");
        }
    }

    /**
     * Check if flags changed in update
     */
    private boolean checkFlagsChanged(ShipBlReprintHdr entity, ManifestCorrectorUpdateDTO dto) {
        return !Objects.equals(entity.getDoReprint(), dto.getDoReprint()) ||
               !Objects.equals(entity.getContainerReprint(), dto.getContainerReprint()) ||
               !Objects.equals(entity.getReturnReprint(), dto.getReturnReprint()) ||
               !Objects.equals(entity.getBlReprint(), dto.getBlReprint()) ||
               !Objects.equals(entity.getDemRefund(), dto.getDemRefund());
    }

    /**
     * Handle reprint flags - load charges/containers based on flags
     */
    private void handleReprintFlags(ShipBlReprintHdr entity) {
        String blNumber = entity.getBlNumber();
        if (blNumber == null || blNumber.trim().isEmpty()) {
            return;
        }

        Long blPoid = Long.parseLong(blNumber);

        // Clear existing details
        chargeDtlRepository.deleteByTransactionPoid(entity.getTransactionPoid());
        containerDtlRepository.deleteByTransactionPoid(entity.getTransactionPoid());

        if ("Y".equals(entity.getDoReprint())) {
            // Load charges for DO reprint
            loadChargesForDoReprint(entity.getTransactionPoid(), blPoid);
        } else if ("Y".equals(entity.getContainerReprint())) {
            // Load containers and charges for container reprint
            loadContainersForReprint(entity.getTransactionPoid(), blPoid);
            loadChargesForContainerReprint(entity.getTransactionPoid(), blPoid);
        } else if ("Y".equals(entity.getBlReprint())) {
            // Load charges for BL reprint (EXPORT)
            loadChargesForBlReprint(entity.getTransactionPoid(), blPoid);
        }

    }

    /**
     * Load charges for DO reprint (PERBL basis)
     */
    private void loadChargesForDoReprint(Long transactionPoid, Long blPoid) {
        String sql = "SELECT CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE, CHARGE_CODE_POID, " +
                "AMOUNT_20, AMOUNT_40, AMOUNT_OTHER " +
                "FROM SHIP_PORT_CHARGES_HDR MCHDR " +
                "INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL ON MCHDR.TRANSACTION_POID = MCDTL.TRANSACTION_POID " +
                "WHERE (SELECT TO_DATE(NVL(VHDR.ARRIVAL_DATE, VHDR.EXPECTED_DATE)) " +
                "       FROM SHIP_VOYAGE_HDR VHDR " +
                "       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID " +
                "       WHERE BLHDR.TRANSACTION_POID = ?) BETWEEN MCHDR.PERIOD_FROM AND MCHDR.PERIOD_TO " +
                "AND NVL(MCHDR.CHARGE_LINE_POID, 0) = 0 " +
                "AND MCDTL.CHARGE_TYPE_APPLICABLE = 'REPRINTIMP' " +
                "AND MCDTL.CHARGE_APPLICABLE = 'PERBL' " +
                "AND MCDTL.CHARGE_TYPE_APPLICABLE IN ('REPRINTIMP', 'REPRINTEXP', 'REPRINTBOTH') " +
                "AND NVL(MCHDR.DELETED, 'N') = 'N' " +
                "ORDER BY MCDTL.CHARGE_TYPE_APPLICABLE, MCDTL.CHARGE_APPLICABLE";

        List<Map<String, Object>> charges = jdbcTemplate.queryForList(sql, blPoid);

        Long detRowId = chargeDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        if (detRowId == null) detRowId = 0L;

        for (Map<String, Object> charge : charges) {
            if ("PERBL".equalsIgnoreCase((String) charge.get("CHARGE_APPLICABLE"))) {
                detRowId++;
                ShipBlReprintChargeDtl chargeDtl = ShipBlReprintChargeDtl.builder()
                        .transactionPoid(transactionPoid)
                        .detRowId(detRowId)
                        .chargePoid(getLongFromMap(charge, "CHARGE_CODE_POID"))
                        .currencyExchange(BigDecimal.ONE)
                        .quantity(BigDecimal.ONE)
                        .perQuantityAmount(getBigDecimalFromMap(charge, "AMOUNT_OTHER"))
                        .chargeType("LOCAL")
                        .printGroup("Local Charge")
                        .currencyCode("BHD")
                        .chargeBasisOn("NOBASIS")
                        .freightType("C")
                        .build();
                chargeDtlRepository.saveAndFlush(chargeDtl);
            }
        }
    }

    /**
     * Load charges for container reprint (PERQUENTITY basis)
     */
    private void loadChargesForContainerReprint(Long transactionPoid, Long blPoid) {
        // First get container sizes - using a simpler approach
        String containerSizeSql = "SELECT EQUIPMENT_ISO_TYPE " +
                "FROM SHIP_BL_MANIFEST_CONTAINER_DTL " +
                "WHERE TRANSACTION_POID = ?";
        List<String> containerTypes = jdbcTemplate.queryForList(containerSizeSql, String.class, blPoid);

        // Count containers by size (assuming first character indicates size)
        long count20 = containerTypes.stream().filter(type -> type != null && type.startsWith("2")).count();
        long count40 = containerTypes.stream().filter(type -> type != null && type.startsWith("4")).count();

        // Load charges
        String sql = "SELECT CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE, CHARGE_CODE_POID, " +
                "AMOUNT_20, AMOUNT_40, AMOUNT_OTHER " +
                "FROM SHIP_PORT_CHARGES_HDR MCHDR " +
                "INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL ON MCHDR.TRANSACTION_POID = MCDTL.TRANSACTION_POID " +
                "WHERE (SELECT TO_DATE(NVL(VHDR.ARRIVAL_DATE, VHDR.EXPECTED_DATE)) " +
                "       FROM SHIP_VOYAGE_HDR VHDR " +
                "       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID " +
                "       WHERE BLHDR.TRANSACTION_POID = ?) BETWEEN MCHDR.PERIOD_FROM AND MCHDR.PERIOD_TO " +
                "AND NVL(MCHDR.CHARGE_LINE_POID, 0) = 0 " +
                "AND MCDTL.CHARGE_TYPE_APPLICABLE = 'REPRINTIMP' " +
                "AND MCDTL.CHARGE_APPLICABLE = 'PERQUENTITY' " +
                "AND MCDTL.CHARGE_TYPE_APPLICABLE IN ('REPRINTIMP', 'REPRINTEXP', 'REPRINTBOTH') " +
                "AND NVL(MCHDR.DELETED, 'N') = 'N' " +
                "ORDER BY MCDTL.CHARGE_TYPE_APPLICABLE, MCDTL.CHARGE_APPLICABLE";

        List<Map<String, Object>> charges = jdbcTemplate.queryForList(sql, blPoid);

        Long detRowId = chargeDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        if (detRowId == null) detRowId = 0L;

        for (Map<String, Object> charge : charges) {
            if ("PERQUENTITY".equalsIgnoreCase((String) charge.get("CHARGE_APPLICABLE"))) {
                // Create charge for 20ft containers
                if (count20 > 0) {
                    detRowId++;
                    ShipBlReprintChargeDtl chargeDtl20 = ShipBlReprintChargeDtl.builder()
                            .transactionPoid(transactionPoid)
                            .detRowId(detRowId)
                            .chargePoid(getLongFromMap(charge, "CHARGE_CODE_POID"))
                            .currencyExchange(BigDecimal.ONE)
                            .quantity(BigDecimal.valueOf(count20))
                            .perQuantityAmount(getBigDecimalFromMap(charge, "AMOUNT_20"))
                            .chargeType("LOCAL")
                            .printGroup("Local Charge")
                            .currencyCode("BHD")
                            .chargeBasisOn("NOBASIS")
                            .freightType("C")
                            .build();
                    chargeDtlRepository.saveAndFlush(chargeDtl20);
                }

                // Create charge for 40ft containers
                if (count40 > 0) {
                    detRowId++;
                    ShipBlReprintChargeDtl chargeDtl40 = ShipBlReprintChargeDtl.builder()
                            .transactionPoid(transactionPoid)
                            .detRowId(detRowId)
                            .chargePoid(getLongFromMap(charge, "CHARGE_CODE_POID"))
                            .currencyExchange(BigDecimal.ONE)
                            .quantity(BigDecimal.valueOf(count40))
                            .perQuantityAmount(getBigDecimalFromMap(charge, "AMOUNT_40"))
                            .chargeType("LOCAL")
                            .printGroup("Local Charge")
                            .currencyCode("BHD")
                            .chargeBasisOn("NOBASIS")
                            .freightType("C")
                            .build();
                    chargeDtlRepository.saveAndFlush(chargeDtl40);
                }
            }
        }
    }

    /**
     * Load charges for BL reprint (EXPORT, PERBL basis, PREPAID)
     */
    private void loadChargesForBlReprint(Long transactionPoid, Long blPoid) {
        String sql = "SELECT CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE, CHARGE_CODE_POID, " +
                "AMOUNT_20, AMOUNT_40, AMOUNT_OTHER " +
                "FROM SHIP_PORT_CHARGES_HDR MCHDR " +
                "INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL ON MCHDR.TRANSACTION_POID = MCDTL.TRANSACTION_POID " +
                "WHERE (SELECT TO_DATE(NVL(VHDR.ARRIVAL_DATE, VHDR.EXPECTED_DATE)) " +
                "       FROM SHIP_VOYAGE_HDR VHDR " +
                "       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID " +
                "       WHERE BLHDR.TRANSACTION_POID = ?) BETWEEN MCHDR.PERIOD_FROM AND MCHDR.PERIOD_TO " +
                "AND NVL(MCHDR.CHARGE_LINE_POID, 0) = 0 " +
                "AND MCDTL.CHARGE_TYPE_APPLICABLE = 'REPRINTEXP' " +
                "AND MCDTL.CHARGE_APPLICABLE = 'PERBL' " +
                "AND MCDTL.CHARGE_TYPE_APPLICABLE IN ('REPRINTIMP', 'REPRINTEXP', 'REPRINTBOTH') " +
                "AND NVL(MCHDR.DELETED, 'N') = 'N' " +
                "ORDER BY MCDTL.CHARGE_TYPE_APPLICABLE, MCDTL.CHARGE_APPLICABLE";

        List<Map<String, Object>> charges = jdbcTemplate.queryForList(sql, blPoid);

        Long detRowId = chargeDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        if (detRowId == null) detRowId = 0L;

        for (Map<String, Object> charge : charges) {
            if ("PERBL".equalsIgnoreCase((String) charge.get("CHARGE_APPLICABLE"))) {
                detRowId++;
                ShipBlReprintChargeDtl chargeDtl = ShipBlReprintChargeDtl.builder()
                        .transactionPoid(transactionPoid)
                        .detRowId(detRowId)
                        .chargePoid(getLongFromMap(charge, "CHARGE_CODE_POID"))
                        .currencyExchange(BigDecimal.ONE)
                        .quantity(BigDecimal.ONE)
                        .perQuantityAmount(getBigDecimalFromMap(charge, "AMOUNT_OTHER"))
                        .chargeType("LOCAL")
                        .printGroup("Local Charge")
                        .currencyCode("BHD")
                        .chargeBasisOn("NOBASIS")
                        .freightType("P")
                        .build();
                chargeDtlRepository.saveAndFlush(chargeDtl);
            }
        }
    }

    /**
     * Load containers from manifest
     */
    private void loadContainersForReprint(Long transactionPoid, Long blPoid) {
        String sql = "SELECT CONTAINER_NO, EQUIPMENT_ISO_TYPE, " +
                "NVL(PRINT_DELIVERY_FORM_DEFAULT, 'N') PRINT_DELIVERY_FORM_DEFAULT, " +
                "'N' PRINT_RETURN_FORM_DEFAULT " +
                "FROM SHIP_BL_MANIFEST_CONTAINER_DTL " +
                "WHERE TRANSACTION_POID = ?";

        List<Map<String, Object>> containers = jdbcTemplate.queryForList(sql, blPoid);

        Long detRowId = containerDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        if (detRowId == null) detRowId = 0L;

        for (Map<String, Object> container : containers) {
            detRowId++;
            ShipBlReprintContainerDtl containerDtl = ShipBlReprintContainerDtl.builder()
                    .transactionPoid(transactionPoid)
                    .detRowId(detRowId)
                    .containerNumber((String) container.get("CONTAINER_NO"))
                    .equipmentIsoType((String) container.get("EQUIPMENT_ISO_TYPE"))
                    .isSelectedDlv((String) container.get("PRINT_DELIVERY_FORM_DEFAULT"))
                    .isSelectedRtn("N")
                    .build();
            containerDtlRepository.saveAndFlush(containerDtl);
        }
    }

    /**
     * Save detail tables from DTO
     */
    private void saveDetailTables(ManifestCorrectorCreateDTO dto, Long transactionPoid) {
        if (dto.getChargesDetails() != null && !dto.getChargesDetails().isEmpty()) {
            Long detRowId = chargeDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
            if (detRowId == null) detRowId = 0L;

            for (ManifestCorrectorChargeDtlDto chargeDto : dto.getChargesDetails()) {
                detRowId++;
                ShipBlReprintChargeDtl chargeDtl = mapper.mapChargeDtlFromDto(chargeDto, transactionPoid, detRowId);
                ShipBlReprintChargeDtl saved = chargeDtlRepository.saveAndFlush(chargeDtl);
                
                // Log child table create
                String logDetail = String.format("Row Created on Manifest Corrector Charge Detail with detRowId: %s", saved.getDetRowId());
                loggingService.createLogSummaryEntry(com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            }
        }

        if (dto.getContainerDetails() != null && !dto.getContainerDetails().isEmpty()) {
            Long detRowId = containerDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
            if (detRowId == null) detRowId = 0L;

            for (ManifestCorrectorContainerDtlDto containerDto : dto.getContainerDetails()) {
                detRowId++;
                ShipBlReprintContainerDtl containerDtl = mapper.mapContainerDtlFromDto(containerDto, transactionPoid, detRowId);
                ShipBlReprintContainerDtl saved = containerDtlRepository.saveAndFlush(containerDtl);
                
                // Log child table create
                String logDetail = String.format("Row Created on Manifest Corrector Container Detail with detRowId: %s", saved.getDetRowId());
                loggingService.createLogSummaryEntry(com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            }
        }
    }

    /**
     * Update detail tables from DTO
     */
    private void updateDetailTables(ManifestCorrectorUpdateDTO dto, Long transactionPoid) {
        // Get existing details for logging deletions
        List<ShipBlReprintChargeDtl> existingCharges = chargeDtlRepository.findByTransactionPoid(transactionPoid);
        List<ShipBlReprintContainerDtl> existingContainers = containerDtlRepository.findByTransactionPoid(transactionPoid);
        
        // Log deletions
        existingCharges.forEach(deleted -> loggingService.logDelete(deleted, com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString()));
        existingContainers.forEach(deleted -> loggingService.logDelete(deleted, com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString()));
        
        // Delete existing details
        chargeDtlRepository.deleteByTransactionPoid(transactionPoid);
        containerDtlRepository.deleteByTransactionPoid(transactionPoid);

        // Save new details
        if (dto.getChargesDetails() != null && !dto.getChargesDetails().isEmpty()) {
            Long detRowId = 0L;
            for (ManifestCorrectorChargeDtlDto chargeDto : dto.getChargesDetails()) {
                detRowId++;
                ShipBlReprintChargeDtl chargeDtl = mapper.mapChargeDtlFromDto(chargeDto, transactionPoid, detRowId);
                ShipBlReprintChargeDtl saved = chargeDtlRepository.saveAndFlush(chargeDtl);
                
                // Log child table create
                String logDetail = String.format("Row Created on Manifest Corrector Charge Detail with detRowId: %s", saved.getDetRowId());
                loggingService.createLogSummaryEntry(com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            }
        }

        if (dto.getContainerDetails() != null && !dto.getContainerDetails().isEmpty()) {
            Long detRowId = 0L;
            for (ManifestCorrectorContainerDtlDto containerDto : dto.getContainerDetails()) {
                detRowId++;
                ShipBlReprintContainerDtl containerDtl = mapper.mapContainerDtlFromDto(containerDto, transactionPoid, detRowId);
                ShipBlReprintContainerDtl saved = containerDtlRepository.saveAndFlush(containerDtl);
                
                // Log child table create
                String logDetail = String.format("Row Created on Manifest Corrector Container Detail with detRowId: %s", saved.getDetRowId());
                loggingService.createLogSummaryEntry(com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            }
        }
    }

    /**
     * Load detail tables into DTO
     */
    private void loadDetailTables(ManifestCorrectorDto dto, Long transactionPoid) {
        List<ShipBlReprintChargeDtl> charges = chargeDtlRepository.findByTransactionPoid(transactionPoid);
        List<ShipBlReprintContainerDtl> containers = containerDtlRepository.findByTransactionPoid(transactionPoid);

        dto.setChargesDetails(mapper.mapChargeDtlListToDto(charges));
        dto.setContainerDetails(mapper.mapContainerDtlListToDto(containers));
    }

    /**
     * Call PROC_SHIP_BL_REPRINT_AFT_SAVE
     */
    public void callProcShipBlReprintAftSave(Long transactionPoid, Long blPoid) {
        try {
            String sql = "{call PROC_SHIP_BL_REPRINT_AFT_SAVE(?,?,?,?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, transactionPoid);
                cs.setLong(2, blPoid);
                cs.setString(3, getUserName());
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.execute();

                String message = cs.getString(4);
                if (message != null && !message.trim().isEmpty()) {
                    log.info("PROC_SHIP_BL_REPRINT_AFT_SAVE message: {}", message);

                    if (message.contains("ERROR")){
                        log.error("Error calling PROC_SHIP_BL_REPRINT_AFT_SAVE");
                        throw new ValidationException("Error in post-save processing!");
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_BL_REPRINT_AFT_SAVE", e);
            throw new ValidationException("Error in post-save processing: " + e.getMessage());
        }
    }

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
            return switch (value) {
                case null -> null;
                case BigDecimal bigDecimal -> bigDecimal;
                case Number number -> BigDecimal.valueOf(number.doubleValue());
                default -> new BigDecimal(value.toString());
            };
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getBigDecimalOrNull(CallableStatement cs, int index) {
        try {
            Object value = cs.getObject(index);
            return switch (value) {
                case null -> null;
                case BigDecimal bigDecimal -> bigDecimal;
                case Number number -> BigDecimal.valueOf(number.doubleValue());
                default -> new BigDecimal(value.toString());
            };
        } catch (Exception e) {
            return null;
        }
    }

    private Long getLongFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private BigDecimal getBigDecimalFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return switch (value) {
            case null -> null;
            case BigDecimal bigDecimal -> bigDecimal;
            case Number number -> BigDecimal.valueOf(number.doubleValue());
            default -> new BigDecimal(value.toString());
        };
    }
}
