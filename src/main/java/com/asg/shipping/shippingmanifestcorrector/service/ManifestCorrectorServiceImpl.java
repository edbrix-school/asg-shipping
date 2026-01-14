package com.asg.shipping.shippingmanifestcorrector.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentSearchService;
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
    private final JdbcTemplate jdbcTemplate;
    private final ManifestCorrectorMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
//    private final LovDataService lovService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchManifestCorrector(String docId, FilterRequestDto request, Pageable pageable) {
        log.info("Searching Shipping Manifest Corrector records with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentSearchService.resolveOperator(request);
        String isDeleted = documentSearchService.resolveIsDeleted(request);
        List<FilterDto> filters = documentSearchService.resolveFilters(request);

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
    public ManifestCorrectorDto getManifestCorrectorById(Long transactionPoid) {
        log.info("Getting Shipping Manifest Corrector with id: {}", transactionPoid);

        ShipBlReprintHdr entity = hdrRepository.findActiveByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping Manifest Corrector", "transactionPoid", transactionPoid.toString()));

        ManifestCorrectorDto dto = mapper.mapToDto(entity);

        List<ShipBlReprintChargeDtl> charges = chargeDtlRepository.findByTransactionPoid(transactionPoid);
        List<ShipBlReprintContainerDtl> containers = containerDtlRepository.findByTransactionPoid(transactionPoid);

        dto.setChargesDetails(mapper.mapChargeDtlListToDto(charges));
        dto.setContainerDetails(mapper.mapContainerDtlListToDto(containers));
//        enrichLovData(dto);

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

        log.info("Successfully updated Shipping Manifest Corrector with id: {}", transactionPoid);
        return result;
    }

    @Override
    @Transactional
    public void deleteManifestCorrector(Long transactionPoid) {
        log.info("Deleting Shipping Manifest Corrector with id: {}", transactionPoid);

        ShipBlReprintHdr entity = hdrRepository.findActiveByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping Manifest Corrector", "transactionPoid", transactionPoid.toString()));

        entity.setDeleted("Y");
        hdrRepository.saveAndFlush(entity);

        chargeDtlRepository.deleteByTransactionPoid(transactionPoid);
        containerDtlRepository.deleteByTransactionPoid(transactionPoid);

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

    // ==================== Private Helper Methods ====================

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

//      Demurrage Refund functionality has been dropped in the latest development
//        if ("Y".equals(dto.getDemRefund())) {
//            if (dto.getDemPayType() == null || dto.getDemPayType().trim().isEmpty()) {
//                throw new ValidationException("Demurrage payment type is required when demurrage refund is selected");
//            }
//            if ((dto.getDemPayType().contains("CUSTOMER") || dto.getDemPayType().contains("BANK_PAYMENT"))
//                    && dto.getDemCustomerPoid() == null) {
//                throw new ValidationException("Customer/Bank is required for demurrage refund");
//            }
//        }
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

//      Demurrage Refund functionality has been dropped in the latest development
//        if ("Y".equals(dto.getDemRefund())) {
//            if (dto.getDemPayType() == null || dto.getDemPayType().trim().isEmpty()) {
//                throw new ValidationException("Demurrage payment type is required when demurrage refund is selected");
//            }
//            if ((dto.getDemPayType().contains("CUSTOMER") || dto.getDemPayType().contains("BANK_PAYMENT"))
//                    && dto.getDemCustomerPoid() == null) {
//                throw new ValidationException("Customer/Bank is required for demurrage refund");
//            }
//        }
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
//      Demurrage Refund functionality has been dropped in the latest development
//        else if ("Y".equals(entity.getDemRefund())) {
//            // Demurrage refund charges are loaded via separate API
//            // Do nothing here, charges will be loaded when user calls loadDemurrageRefundCharges
//        }
    }

    /**
     * Load charges for DO reprint (PERBL basis)
     */
    private void loadChargesForDoReprint(Long transactionPoid, Long blPoid) {
        String sql = "SELECT CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE, CHARGE_CODE_POID, " +
                "AMOUNT_20, AMOUNT_40, AMOUNT_OTHER " +
                "FROM SHIP_PORT_CHARGES_HDR MCHDR " +
                "INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL ON MCHDR.TRANSACTION_POID = MCDTL.TRANSACTION_POID " +
                "WHERE (SELECT TO_DATE(NVL(ARRIVAL_DATE, EXPECTED_DATE)) " +
                "       FROM SHIP_VOYAGE_HDR VHDR " +
                "       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID " +
                "       WHERE BLHDR.TRANSACTION_POID = ?) BETWEEN PERIOD_FROM AND PERIOD_TO " +
                "AND NVL(CHARGE_LINE_POID, '0') = '0' " +
                "AND CHARGE_TYPE_APPLICABLE = 'REPRINTIMP' " +
                "AND CHARGE_APPLICABLE = 'PERBL' " +
                "AND CHARGE_TYPE_APPLICABLE IN ('REPRINTIMP', 'REPRINTEXP', 'REPRINTBOTH') " +
                "AND NVL(DELETED, 'N') = 'N' " +
                "ORDER BY CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE";

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
        // First get container sizes
        String containerSizeSql = "SELECT GET_CONTAINER_TYPE(EQUIPMENT_ISO_TYPE, 'SIZE') CNTSIZE " +
                "FROM SHIP_BL_MANIFEST_CONTAINER_DTL " +
                "WHERE TRANSACTION_POID = ?";
        List<String> containerSizes = jdbcTemplate.queryForList(containerSizeSql, String.class, blPoid);

        long count20 = containerSizes.stream().filter("20"::equals).count();
        long count40 = containerSizes.stream().filter(s -> !"20".equals(s)).count();

        // Load charges
        String sql = "SELECT CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE, CHARGE_CODE_POID, " +
                "AMOUNT_20, AMOUNT_40, AMOUNT_OTHER " +
                "FROM SHIP_PORT_CHARGES_HDR MCHDR " +
                "INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL ON MCHDR.TRANSACTION_POID = MCDTL.TRANSACTION_POID " +
                "WHERE (SELECT TO_DATE(NVL(ARRIVAL_DATE, EXPECTED_DATE)) " +
                "       FROM SHIP_VOYAGE_HDR VHDR " +
                "       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID " +
                "       WHERE BLHDR.TRANSACTION_POID = ?) BETWEEN PERIOD_FROM AND PERIOD_TO " +
                "AND NVL(CHARGE_LINE_POID, '0') = '0' " +
                "AND CHARGE_TYPE_APPLICABLE = 'REPRINTIMP' " +
                "AND CHARGE_APPLICABLE = 'PERQUENTITY' " +
                "AND CHARGE_TYPE_APPLICABLE IN ('REPRINTIMP', 'REPRINTEXP', 'REPRINTBOTH') " +
                "AND NVL(DELETED, 'N') = 'N' " +
                "ORDER BY CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE";

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
                "WHERE (SELECT TO_DATE(NVL(ARRIVAL_DATE, EXPECTED_DATE)) " +
                "       FROM SHIP_VOYAGE_HDR VHDR " +
                "       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID = BLHDR.VOYAGE_TRANSACTION_POID " +
                "       WHERE BLHDR.TRANSACTION_POID = ?) BETWEEN PERIOD_FROM AND PERIOD_TO " +
                "AND NVL(CHARGE_LINE_POID, '0') = '0' " +
                "AND CHARGE_TYPE_APPLICABLE = 'REPRINTEXP' " +
                "AND CHARGE_APPLICABLE = 'PERBL' " +
                "AND CHARGE_TYPE_APPLICABLE IN ('REPRINTIMP', 'REPRINTEXP', 'REPRINTBOTH') " +
                "AND NVL(DELETED, 'N') = 'N' " +
                "ORDER BY CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE";

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
                chargeDtlRepository.saveAndFlush(chargeDtl);
            }
        }

        if (dto.getContainerDetails() != null && !dto.getContainerDetails().isEmpty()) {
            Long detRowId = containerDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
            if (detRowId == null) detRowId = 0L;

            for (ManifestCorrectorContainerDtlDto containerDto : dto.getContainerDetails()) {
                detRowId++;
                ShipBlReprintContainerDtl containerDtl = mapper.mapContainerDtlFromDto(containerDto, transactionPoid, detRowId);
                containerDtlRepository.saveAndFlush(containerDtl);
            }
        }
    }

    /**
     * Update detail tables from DTO
     */
    private void updateDetailTables(ManifestCorrectorUpdateDTO dto, Long transactionPoid) {
        // Delete existing details
        chargeDtlRepository.deleteByTransactionPoid(transactionPoid);
        containerDtlRepository.deleteByTransactionPoid(transactionPoid);

        // Save new details
        if (dto.getChargesDetails() != null && !dto.getChargesDetails().isEmpty()) {
            Long detRowId = 0L;
            for (ManifestCorrectorChargeDtlDto chargeDto : dto.getChargesDetails()) {
                detRowId++;
                ShipBlReprintChargeDtl chargeDtl = mapper.mapChargeDtlFromDto(chargeDto, transactionPoid, detRowId);
                chargeDtlRepository.saveAndFlush(chargeDtl);
            }
        }

        if (dto.getContainerDetails() != null && !dto.getContainerDetails().isEmpty()) {
            Long detRowId = 0L;
            for (ManifestCorrectorContainerDtlDto containerDto : dto.getContainerDetails()) {
                detRowId++;
                ShipBlReprintContainerDtl containerDtl = mapper.mapContainerDtlFromDto(containerDto, transactionPoid, detRowId);
                containerDtlRepository.saveAndFlush(containerDtl);
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

    /**
     * Enrich DTO with LOV data
     */
//    private void enrichLovData(ManifestCorrectorDto dto) {
//
//        try {
//
//            if (dto.getVoyageTransactionPoid() != null) {
//                String voyageSql = "SELECT VOYAGE_NO FROM SHIP_VOYAGE_HDR WHERE TRANSACTION_POID = ?";
//                try {
//                    String voyageNo = jdbcTemplate.queryForObject(voyageSql, String.class, dto.getVoyageTransactionPoid());
//                    dto.setVoyageNumber(voyageNo);
//                } catch (Exception e) {
//                    log.warn("Could not get voyage number for POID: {}", dto.getVoyageTransactionPoid());
//                }
//                dto.setVoyageTransactionDet(lovService.getDetailsByPoidAndLovName(dto.getVoyageTransactionPoid(), "VESSAL_VOYAGE"));
//            }
//            if (dto.getBlNumber() != null) {
//                dto.setBlNumberDet(lovService.getDetailsByCodeAndLovName(dto.getBlNumber(), "SHIP_BL_REPRINT"));
//            }
//            if (dto.getIssueType() != null) {
//                dto.setIssueTypeDet(lovService.getDetailsByCodeAndLovName(dto.getIssueType(), "BL_ISSUE_TYPE"));
//            }
//            if (dto.getConsigneePoid() != null) {
//                dto.setConsigneeDet(lovService.getDetailsByPoidAndLovName(dto.getConsigneePoid(), "ADDRESS_MASTER"));
//            }
//            if (dto.getNotifyPoid() != null) {
//                dto.setNotifyDet(lovService.getDetailsByPoidAndLovName(dto.getNotifyPoid(), "ADDRESS_MASTER"));
//            }
//            if (dto.getCompanyPoid() != null) {
//                dto.setCompanyDet(lovService.getDetailsByPoidAndLovName(dto.getCompanyPoid(), "COMPANY"));
//            }
//            if (dto.getHoldReason() != null) {
//                dto.setHoldReasonDet(lovService.getDetailsByCodeAndLovName(dto.getHoldReason(), "SHIP_DO_ANOTICE_HOLD"));
//            }
//            if (dto.getPortOfLoadingPoid() != null) {
//                dto.setPortOfLoadingDet(lovService.getDetailsByPoidAndLovName(dto.getPortOfLoadingPoid(), "PORT_MASTER"));
//            }
//            if (dto.getPortOfDischargePoid() != null) {
//                dto.setPortOfDischargeDet(lovService.getDetailsByPoidAndLovName(dto.getPortOfDischargePoid(), "PORT_MASTER"));
//            }
//            if (dto.getPlaceOfDeliveryPoid() != null) {
//                dto.setPlaceOfDeliveryDet(lovService.getDetailsByPoidAndLovName(dto.getPlaceOfDeliveryPoid(), "PORT_MASTER"));
//            }
//            if (dto.getPlaceOfReceiptPoid() != null) {
//                dto.setPlaceOfReceiptDet(lovService.getDetailsByPoidAndLovName(dto.getPlaceOfReceiptPoid(), "PORT_MASTER"));
//            }
//
//            if (dto.getChargesDetails() != null) {
//                enrichChargeLovData(dto.getChargesDetails());
//            }
//        } catch (Exception e) {
//            log.error("Exception while Lov Enrichment for manifest corrector with id {} !", dto.getTransactionPoid());
//        }
//    }
//
//    private void enrichChargeLovData(List<ManifestCorrectorChargeDtlDto> manifestChargesDtlDto){
//        try {
//            manifestChargesDtlDto.forEach(dto -> {
//
//                if (dto.getChargePoid() != null) {
//                    dto.setChargeDet(lovService.getDetailsByPoidAndLovName(dto.getChargePoid(), "CHARGE_MASTER"));
//                }
//                if (dto.getPaidAtPortPoid() != null) {
//                    dto.setPaidAtPortDet(lovService.getDetailsByPoidAndLovName(dto.getPaidAtPortPoid(), "PORT_MASTER"));
//                }
//                if (dto.getChargeType() != null) {
//                    dto.setChargeDet(lovService.getDetailsByCodeAndLovName(dto.getChargeType(), "CHARGE_TYPE"));
//                }
//                if (dto.getCurrencyCode() != null) {
//                    dto.setCurrencyDet(lovService.getDetailsByCodeAndLovName(dto.getCurrencyCode(), "CURRENCY"));
//                }
//                if (dto.getFreightType() != null) {
//                    dto.setFreightTypeDet(lovService.getDetailsByCodeAndLovName(dto.getFreightType(), "SHIP_FREIGHT_TYPE"));
//                }
//                if (dto.getChargeBasisOn() != null) {
//                    dto.setChargeBasisOnDet(lovService.getDetailsByCodeAndLovName(dto.getChargeBasisOn(), "CONTAINER_TYPE_MASTER"));
//                }
//            });
//        }
//        catch (Exception e){
//            log.error("Exception while Charges Lov Enrichment!");
//        }
//    }

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

