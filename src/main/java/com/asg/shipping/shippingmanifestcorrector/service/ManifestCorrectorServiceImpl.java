package com.asg.shipping.shippingmanifestcorrector.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.model.CustomAuthDetails;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static com.asg.common.lib.security.util.UserContext.*;
import com.asg.common.lib.security.util.UserContext;

/**
 * Service implementation for Shipping Manifest Corrector operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManifestCorrectorServiceImpl implements ManifestCorrectorService {
    private static final String ACTION_ISCREATED = "ISCREATED";
    private static final String ACTION_ISUPDATED = "ISUPDATED";
    private static final String ACTION_ISDELETED = "ISDELETED";
    private static final String ACTION_NOCHANGES = "NOCHANGES";

    private final ShipBlReprintHdrRepository hdrRepository;
    private final ShipBlReprintChargeDtlRepository chargeDtlRepository;
    private final ShipBlReprintContainerDtlRepository containerDtlRepository;
    private final DocumentSearchService documentSearchService;
    private final DocumentDeleteService documentDeleteService;
    private final LoggingService loggingService;
    private final JdbcTemplate jdbcTemplate;
    private final ManifestCorrectorMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    @Qualifier("lovLookupExecutor")
    private final Executor lovLookupExecutor;

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

        ShipBlReprintHdr entity = hdrRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping Manifest Corrector", "transactionPoid", transactionPoid.toString()));

        ManifestCorrectorDto dto = mapper.mapToDto(entity);

        List<ShipBlReprintChargeDtl> charges = chargeDtlRepository.findByTransactionPoid(transactionPoid);
        List<ShipBlReprintContainerDtl> containers = containerDtlRepository.findByTransactionPoid(transactionPoid);

        dto.setChargesDetails(mapper.mapChargeDtlListToDto(charges));
        dto.setContainerDetails(mapper.mapContainerDtlListToDto(containers));
        enrichLovData(dto);

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

        saveDetailTables(createDTO, saved);

        eventPublisher.publishEvent(new ManifestCorrectorSaveEvent(saved, Long.parseLong(saved.getBlNumber())));

        ManifestCorrectorDto result = mapper.mapToDto(saved);
        loadDetailTables(result, saved.getTransactionPoid());
        enrichLovData(result);

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

        ShipBlReprintHdr oldEntity = new ShipBlReprintHdr();
        BeanUtils.copyProperties(entity, oldEntity);

        validateUpdateDTO(updateDTO);

        mapper.mapUpdateDTOToEntity(updateDTO, entity);

        ShipBlReprintHdr saved = hdrRepository.saveAndFlush(entity);

        updateDetailTables(updateDTO, saved);

        // Defer the proc call until after commit so the autonomous proc can read the saved rows.
        eventPublisher.publishEvent(
                new ManifestCorrectorSaveEvent(saved, Long.parseLong(saved.getBlNumber())));

        ManifestCorrectorDto result = mapper.mapToDto(saved);
        loadDetailTables(result, saved.getTransactionPoid());
        enrichLovData(result);

        loggingService.logChanges(oldEntity, saved, ShipBlReprintHdr.class,
                com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString(),
                LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

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

        //entity.setDeleted("Y");
        hdrRepository.saveAndFlush(entity);

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

        Long transactionPoid = request != null && request.getTransactionPoid() != null
                ? request.getTransactionPoid()
                : 0L;

        List<Long> poids = request != null ? request.resolvedBlPoids() : List.of();

        if (poids.isEmpty()) {
            throw new ValidationException("At least one BL poid is required");
        }

        String findPoidSql = "SELECT TRANSACTION_POID FROM SHIP_BL_MANIFEST_HDR " +
                "WHERE TRANSACTION_POID IN (" + inClausePlaceholders(poids.size()) + ") AND (DELETED = 'N' OR DELETED IS NULL) " +
                "AND ROWNUM = 1";
        List<Long> found = jdbcTemplate.queryForList(findPoidSql, Long.class, poids.toArray());
        if (found.isEmpty()) {
            log.info("No valid BL poid found for BL: {}", blNumber);
            return null;
        }

        Long blPoid = found.get(0);
        ManifestCorrectorBlAutoPopulateDto result = callAutoPopulateProc(blNumber, blPoid, transactionPoid);
        if (result == null) {
            log.info("No BL auto-population data returned for BL: {}", blNumber);
            return null;
        }
        enrichLovData(result);
        return result;
    }

    private ManifestCorrectorBlAutoPopulateDto callAutoPopulateProc(String blNumber, Long blPoid, Long transactionPoid) {
        ManifestCorrectorBlAutoPopulateDto response = ManifestCorrectorBlAutoPopulateDto.builder()
                .blPoid(blPoid)
                .transactionPoid(transactionPoid)
                .build();
        final boolean[] dataFound = {false};

        try {
            String sql = "{call PROC_LOV_AFTER_BRWS_100_143(?,?,?,?,?,?,?,?)}";
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
                        return null;
                    }
                    dataFound[0] = true;

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
        } catch (Exception e) {
            if (isNoDataFoundException(e)) {
                log.info("No data for blPoid: {}, trying next", blPoid);
                return null;
            }
            log.error("Error calling PROC_LOV_AFTER_BRWS_100_143 for BL: {}, poid: {}", blNumber, blPoid, e);
            throw new ValidationException("Error auto-populating BL details: " + e.getMessage());
        }

        return dataFound[0] ? response : null;
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

    private boolean isNoDataFoundException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("ORA-01403") || message.contains("no data found"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void enrichLovData(ManifestCorrectorDto dto) {
        if (dto == null) return;

        CustomAuthDetails caller = UserContext.getCurrentUser();

        List<ManifestCorrectorChargeDtlDto> charges = dto.getChargesDetails() != null ? dto.getChargesDetails() : List.of();

        // Collect all poids per LOV name
        List<Long> portPoids = filterNonNull(
                dto.getPlaceOfDeliveryPoid(), dto.getPlaceOfReceiptPoid(),
                dto.getPortOfLoadingPoid(), dto.getPortOfDischargePoid());
        charges.forEach(c -> { if (c.getPaidAtPortPoid() != null) portPoids.add(c.getPaidAtPortPoid()); });

        List<Long> chargePoids = charges.stream().map(ManifestCorrectorChargeDtlDto::getChargePoid).filter(java.util.Objects::nonNull).toList();
        List<Long> blPoids = filterNonNull(parseLongSafely(dto.getBlNumber()));
        List<Long> addressPoids = filterNonNull(dto.getConsigneePoid(), dto.getNotifyPoid());
        List<Long> voyagePoids = filterNonNull(dto.getVoyageTransactionPoid());

        List<String> currencyCodes   = charges.stream().map(ManifestCorrectorChargeDtlDto::getCurrencyCode).filter(c -> c != null && !c.isBlank()).distinct().toList();
        List<String> chargeTypeCodes = charges.stream().map(ManifestCorrectorChargeDtlDto::getChargeType).filter(c -> c != null && !c.isBlank()).distinct().toList();
        List<String> freightCodes    = charges.stream().map(ManifestCorrectorChargeDtlDto::getFreightType).filter(c -> c != null && !c.isBlank()).distinct().toList();
        List<String> basisCodes      = charges.stream().map(ManifestCorrectorChargeDtlDto::getChargeBasisOn).filter(c -> c != null && !c.isBlank()).distinct().toList();

        // Each of these is an independent DB round trip — fan them out concurrently instead of paying for them one at a time.
        CompletableFuture<Map<Long, LovGetListDto>> blFuture       = supplyLovAsync(caller, () -> fetchBlReprintMap(blPoids));
        CompletableFuture<Map<Long, LovGetListDto>> addressFuture  = supplyLovAsync(caller, () -> fetchAddressMasterMap(addressPoids));
        CompletableFuture<Map<Long, LovGetListDto>> portFuture     = supplyLovAsync(caller, () -> fetchPortMasterMap(portPoids));
        CompletableFuture<Map<Long, LovGetListDto>> voyageFuture   = supplyLovAsync(caller, () -> fetchVoyageMap(voyagePoids));
        CompletableFuture<Map<Long, LovGetListDto>> chargeFuture   = supplyLovAsync(caller, () -> fetchChargeMasterMap(chargePoids));
        CompletableFuture<Map<String, LovGetListDto>> currencyFuture = supplyLovAsync(caller, () -> fetchCurrencyMap(currencyCodes));
        CompletableFuture<Map<String, LovGetListDto>> basisFuture    = supplyLovAsync(caller, () -> fetchContainerBasisMap(basisCodes));

        joinAllLovFutures(blFuture, addressFuture, portFuture, voyageFuture, chargeFuture, currencyFuture, basisFuture);

        Map<Long, LovGetListDto> blMap       = blFuture.join();
        Map<Long, LovGetListDto> addressMap  = addressFuture.join();
        Map<Long, LovGetListDto> portMap     = portFuture.join();
        Map<Long, LovGetListDto> voyageMap   = voyageFuture.join();
        Map<Long, LovGetListDto> chargeMap   = chargeFuture.join();
        Map<String, LovGetListDto> currencyMap = currencyFuture.join();
        Map<String, LovGetListDto> basisMap    = basisFuture.join();

        Map<String, LovGetListDto> issueTypeMap   = mapByCode(BL_ISSUE_TYPE_LOV, filterNonNullStr(dto.getIssueType()));
        Map<String, LovGetListDto> holdReasonMap  = mapByCode(SHIP_DO_ANOTICE_HOLD_LOV, filterNonNullStr(dto.getHoldReason()));
        Map<String, LovGetListDto> chargeTypeMap  = mapByCode(CHARGE_TYPE_LOV, chargeTypeCodes);
        Map<String, LovGetListDto> freightMap     = mapByCode(SHIP_FREIGHT_TYPE_LOV, freightCodes);

        // Resolve header fields
        dto.setBlNumberDet(blMap.get(parseLongSafely(dto.getBlNumber())));
        dto.setIssueTypeDet(issueTypeMap.get(dto.getIssueType()));
        dto.setConsigneeDet(addressMap.get(dto.getConsigneePoid()));
        dto.setNotifyDet(addressMap.get(dto.getNotifyPoid()));
        dto.setHoldReasonDet(holdReasonMap.get(dto.getHoldReason()));
        dto.setPlaceOfDeliveryDet(portMap.get(dto.getPlaceOfDeliveryPoid()));
        dto.setPlaceOfReceiptDet(portMap.get(dto.getPlaceOfReceiptPoid()));
        dto.setPortOfLoadingDet(portMap.get(dto.getPortOfLoadingPoid()));
        dto.setPortOfDischargeDet(portMap.get(dto.getPortOfDischargePoid()));
        dto.setVoyageTransactionDet(voyageMap.get(dto.getVoyageTransactionPoid()));

        // Resolve charge detail fields
        for (ManifestCorrectorChargeDtlDto charge : charges) {
            charge.setChargeDet(chargeMap.get(charge.getChargePoid()));
            charge.setPaidAtPortDet(portMap.get(charge.getPaidAtPortPoid()));
            charge.setCurrencyDet(currencyMap.get(charge.getCurrencyCode()));
            charge.setChargeTypeDet(chargeTypeMap.get(charge.getChargeType()));
            charge.setFreightTypeDet(freightMap.get(charge.getFreightType()));
            charge.setChargeBasisOnDet(basisMap.get(charge.getChargeBasisOn()));
        }
    }

    private void enrichLovData(ManifestCorrectorBlAutoPopulateDto dto) {
        if (dto == null) return;

        CustomAuthDetails caller = UserContext.getCurrentUser();

        Long notifyPoid = dto.getNotifyPoid() != null && dto.getNotifyPoid() > 1 ? dto.getNotifyPoid() : null;
        List<Long> blPoids = filterNonNull(dto.getBlPoid());
        List<Long> addressPoids = filterNonNull(dto.getConsigneePoid(), notifyPoid);
        List<Long> glPoids = filterNonNull(dto.getPayableGlPoid(), dto.getIncomeGlPoid());
        List<Long> portPoids = filterNonNull(dto.getPlaceOfDeliveryPoid(), dto.getPlaceOfReceiptPoid(),
                dto.getPortOfLoadingPoid(), dto.getPortOfDischargePoid());
        List<Long> voyagePoids = filterNonNull(dto.getVoyageTransactionPoid());

        // Each of these is an independent DB round trip — fan them out concurrently instead of paying for them one at a time.
        CompletableFuture<Map<Long, LovGetListDto>> blFuture      = supplyLovAsync(caller, () -> fetchBlReprintMap(blPoids));
        CompletableFuture<Map<Long, LovGetListDto>> addressFuture = supplyLovAsync(caller, () -> fetchAddressMasterMap(addressPoids));
        CompletableFuture<Map<Long, LovGetListDto>> glFuture      = supplyLovAsync(caller, () -> fetchGlMasterLedgersMap(glPoids));
        CompletableFuture<Map<Long, LovGetListDto>> portFuture    = supplyLovAsync(caller, () -> fetchPortMasterMap(portPoids));
        CompletableFuture<Map<Long, LovGetListDto>> voyageFuture  = supplyLovAsync(caller, () -> fetchVoyageMap(voyagePoids));

        joinAllLovFutures(blFuture, addressFuture, glFuture, portFuture, voyageFuture);

        Map<Long, LovGetListDto> blMap      = blFuture.join();
        Map<Long, LovGetListDto> addressMap = addressFuture.join();
        Map<Long, LovGetListDto> glMap      = glFuture.join();
        Map<Long, LovGetListDto> portMap    = portFuture.join();
        Map<Long, LovGetListDto> voyageMap  = voyageFuture.join();

        Long issueTypePoid = parseLongSafely(dto.getIssueType());
        Map<Long, LovGetListDto> issueTypeMap = mapByPoid(BL_ISSUE_TYPE_LOV, filterNonNull(issueTypePoid));
        Map<String, LovGetListDto> blTypeMap = mapByCode(BL_TYPE_LOV, filterNonNullStr(dto.getBlType()));
        Map<String, LovGetListDto> holdReasonMap = mapByCode(SHIP_DO_ANOTICE_HOLD_LOV, filterNonNullStr(dto.getHoldReason()));

        dto.setBlDet(blMap.get(dto.getBlPoid()));
        dto.setConsigneeDet(addressMap.get(dto.getConsigneePoid()));
        dto.setNotifyDet(addressMap.get(notifyPoid));
        dto.setPayableGlDet(glMap.get(dto.getPayableGlPoid()));
        dto.setIncomeGlDet(glMap.get(dto.getIncomeGlPoid()));
        dto.setPlaceOfDeliveryDet(portMap.get(dto.getPlaceOfDeliveryPoid()));
        dto.setPlaceOfReceiptDet(portMap.get(dto.getPlaceOfReceiptPoid()));
        dto.setPortOfLoadingDet(portMap.get(dto.getPortOfLoadingPoid()));
        dto.setPortOfDischargeDet(portMap.get(dto.getPortOfDischargePoid()));
        dto.setVoyageTransactionDet(voyageMap.get(dto.getVoyageTransactionPoid()));
        dto.setIssueTypeDet(issueTypeMap.get(issueTypePoid));
        dto.setBlTypeDet(blTypeMap.get(dto.getBlType()));
        dto.setHoldReasonDet(holdReasonMap.get(dto.getHoldReason()));
    }

    /**
     * Each fetch*Map lookup below pulls its own pooled Connection per call via {@code jdbcTemplate}
     * and doesn't rely on the request-bound EntityManager/Session, so it's safe to fan out across
     * worker threads. UserContext is a plain ThreadLocal a pooled thread can't see, so the caller's
     * auth details are carried onto the worker and cleared again once the lookup is done — same
     * convention as ImportManifestServiceImpl#supplyLovAsync.
     */
    private <T> CompletableFuture<T> supplyLovAsync(CustomAuthDetails caller, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            UserContext.setCurrentUser(caller);
            try {
                return supplier.get();
            } finally {
                UserContext.clear();
            }
        }, lovLookupExecutor);
    }

    private void joinAllLovFutures(CompletableFuture<?>... futures) {
        try {
            CompletableFuture.allOf(futures).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }

    private static String inClausePlaceholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }

    // Static LOVs — hardcoded SELECT ... FROM DUAL UNION lists in PROC_LOV_GETLIST; no DB round trip needed.
    private static final List<LovGetListDto> BL_ISSUE_TYPE_LOV = List.of(
            staticLov(1L, "OBL", "ORIGINAL BL REQUIRED"),
            staticLov(2L, "EXPRESS", "EXPRESS RELEASED"),
            staticLov(3L, "SEAWAY", "SEAWAY BILL"),
            staticLov(4L, "OTHER", "OTHER"));

    private static final List<LovGetListDto> SHIP_DO_ANOTICE_HOLD_LOV = List.of(
            staticLov(1L, "1", "HOLD DO"),
            staticLov(4L, "4", "VERIFIED"),
            staticLov(5L, "5", "NOTVERIFIED"),
            staticLov(6L, "6", "VERIFIED-ELSEWHERE"));

    private static final List<LovGetListDto> CHARGE_TYPE_LOV = List.of(
            staticLov(1L, "MANIFEST", "MANIFESTED PRINCIPAL PAYABLE"),
            staticLov(2L, "LOCAL", "LOCAL CHARGE"),
            staticLov(3L, "BOTH", "BOTH"));

    private static final List<LovGetListDto> SHIP_FREIGHT_TYPE_LOV = List.of(
            staticLov(1L, "P", "PREPAID"),
            staticLov(2L, "C", "COLLECT"),
            staticLov(3L, "E", "ELSEWHERE"));

    private static final List<LovGetListDto> CONTAINER_BASIS_STATIC_LOV = List.of(
            staticLov(273L, "AUTOBASIS", "AUTOBASIS"),
            staticLov(253L, "NOBASIS", "NOBASIS"));

    private static final List<LovGetListDto> BL_TYPE_LOV = List.of(
            staticLov(1L, "IMPORT", "IMPORT"),
            staticLov(2L, "EXPORT", "EXPORT"),
            staticLov(3L, "SWITCH", "SWITCH"),
            staticLov(4L, "CROSSTRADE", "CROSSTRADE"));

    private static LovGetListDto staticLov(Long poid, String code, String description) {
        LovGetListDto dto = new LovGetListDto();
        dto.setPoid(poid);
        dto.setCode(code);
        dto.setDescription(description);
        dto.setLabel(description);
        dto.setValue(poid);
        return dto;
    }

    private Map<String, LovGetListDto> mapByCode(List<LovGetListDto> lov, List<String> codes) {
        if (codes.isEmpty()) return Collections.emptyMap();
        return lov.stream()
                .filter(d -> codes.stream().anyMatch(c -> c.equalsIgnoreCase(d.getCode())))
                .collect(java.util.stream.Collectors.toMap(LovGetListDto::getCode, d -> d));
    }

    private Map<Long, LovGetListDto> mapByPoid(List<LovGetListDto> lov, List<Long> poids) {
        if (poids.isEmpty()) return Collections.emptyMap();
        return lov.stream()
                .filter(d -> poids.contains(d.getPoid()))
                .collect(java.util.stream.Collectors.toMap(LovGetListDto::getPoid, d -> d));
    }

    private Map<Long, LovGetListDto> fetchChargeMasterMap(List<Long> poids) {
        if (poids.isEmpty()) return Collections.emptyMap();
        String sql = "SELECT charge_poid AS POID, charge_code AS CODE, charge_name AS DESCRIPTION " +
                "FROM ship_charge_master " +
                "WHERE NVL(active, 'N') = 'Y' AND division_code IN ('SH', 'ALL') AND charge_poid IN (" + inClausePlaceholders(poids.size()) + ")";
        return mapLovRowsByPoid(sql, poids.toArray());
    }

    private Map<String, LovGetListDto> fetchCurrencyMap(List<String> codes) {
        if (codes.isEmpty()) return Collections.emptyMap();
        String sql = "SELECT currency_poid AS POID, currency_code AS CODE, currency_name AS DESCRIPTION " +
                "FROM global_currency_master " +
                "WHERE NVL(active, 'Y') = 'Y' AND NVL(deleted, 'N') = 'N' AND currency_code IN (" + inClausePlaceholders(codes.size()) + ")";
        return mapLovRowsByCode(sql, codes.toArray());
    }

    private Map<String, LovGetListDto> fetchContainerBasisMap(List<String> codes) {
        if (codes.isEmpty()) return Collections.emptyMap();
        Map<String, LovGetListDto> result = new HashMap<>();
        CONTAINER_BASIS_STATIC_LOV.stream()
                .filter(d -> codes.stream().anyMatch(c -> c.equalsIgnoreCase(d.getCode())))
                .forEach(d -> result.put(d.getCode(), d));

        List<String> remaining = codes.stream()
                .filter(c -> result.keySet().stream().noneMatch(k -> k.equalsIgnoreCase(c)))
                .toList();
        if (!remaining.isEmpty()) {
            String sql = "SELECT container_type_poid AS POID, container_type_code AS CODE, " +
                    "container_type_code || '-' || container_type_name AS DESCRIPTION " +
                    "FROM ship_container_type_master " +
                    "WHERE active = 'Y' AND container_type_code IN (" + inClausePlaceholders(remaining.size()) + ")";
            result.putAll(mapLovRowsByCode(sql, remaining.toArray()));
        }
        return result;
    }

    private Map<String, LovGetListDto> mapLovRowsByCode(String sql, Object... args) {
        List<LovGetListDto> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long poid = rs.getLong("POID");
            String code = rs.getString("CODE");
            String description = rs.getString("DESCRIPTION");
            LovGetListDto dto = new LovGetListDto();
            dto.setPoid(poid);
            dto.setCode(code);
            dto.setDescription(description);
            dto.setLabel(description);
            dto.setValue(poid);
            return dto;
        }, args);
        Map<String, LovGetListDto> result = new HashMap<>();
        rows.forEach(d -> result.put(d.getCode(), d));
        return result;
    }

    private Map<Long, LovGetListDto> fetchGlMasterLedgersMap(List<Long> poids) {
        if (poids.isEmpty()) return Collections.emptyMap();
        String sql = "SELECT gl_poid AS POID, gl_code AS CODE, gl_description || '(' || gl_ac_type || ')' AS DESCRIPTION " +
                "FROM gl_master " +
                "WHERE gl_type = 'LEDGER' AND NVL(deleted, 'N') = 'N' AND NVL(active, 'Y') = 'Y' " +
                "AND gl_poid IN (" + inClausePlaceholders(poids.size()) + ")";
        return mapLovRowsByPoid(sql, poids.toArray());
    }

    private Map<Long, LovGetListDto> fetchBlReprintMap(List<Long> poids) {
        if (poids.isEmpty()) return Collections.emptyMap();
        String sql = "SELECT hdr.transaction_poid AS POID, hdr.bl_number AS CODE, hdr.bl_number AS DESCRIPTION " +
                "FROM ship_bl_manifest_hdr hdr " +
                "WHERE NVL(hdr.deleted, 'N') = 'N' AND hdr.transaction_poid IN (" + inClausePlaceholders(poids.size()) + ")";
        return mapLovRowsByPoid(sql, poids.toArray());
    }

    private Map<Long, LovGetListDto> fetchAddressMasterMap(List<Long> poids) {
        if (poids.isEmpty()) return Collections.emptyMap();
        String sql = "SELECT am.address_master_poid AS POID, am.address_master_poid AS CODE, am.address_name AS DESCRIPTION " +
                "FROM global_address_master am " +
                "WHERE am.address_master_poid IN (" + inClausePlaceholders(poids.size()) + ") " +
                "AND NVL(am.active, 'Y') = 'Y' AND NVL(am.deleted, 'N') = 'N'";
        return mapLovRowsByPoid(sql, poids.toArray());
    }

    private Map<Long, LovGetListDto> fetchPortMasterMap(List<Long> poids) {
        if (poids.isEmpty()) return Collections.emptyMap();
        String sql = "SELECT port_poid AS POID, port_code AS CODE, port_name AS DESCRIPTION " +
                "FROM ship_port_master " +
                "WHERE active = 'Y' AND port_poid IN (" + inClausePlaceholders(poids.size()) + ")";
        return mapLovRowsByPoid(sql, poids.toArray());
    }

    private Map<Long, LovGetListDto> fetchVoyageMap(List<Long> poids) {
        if (poids.isEmpty()) return Collections.emptyMap();
        String sql = "SELECT voyage.transaction_poid AS POID, voyage.job_no AS CODE, " +
                "'VESSEL/VOYAGE/LINE :' || vessel.vessel_name || '/' || voyage.voyage_no || '/' || mline.line_code AS DESCRIPTION " +
                "FROM ship_voyage_hdr voyage " +
                "INNER JOIN ship_line_master mline ON voyage.line_poid = mline.line_poid " +
                "INNER JOIN ship_vessel_master vessel ON vessel.vessel_poid = voyage.vessel_poid " +
                "WHERE voyage.company_poid = ? AND voyage.transaction_poid IN (" + inClausePlaceholders(poids.size()) + ")";
        Object[] args = new Object[poids.size() + 1];
        args[0] = getCompanyPoid();
        for (int i = 0; i < poids.size(); i++) args[i + 1] = poids.get(i);
        return mapLovRowsByPoid(sql, args);
    }

    private Map<Long, LovGetListDto> mapLovRowsByPoid(String sql, Object... args) {
        List<LovGetListDto> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Long poid = rs.getLong("POID");
            String description = rs.getString("DESCRIPTION");
            LovGetListDto dto = new LovGetListDto();
            dto.setPoid(poid);
            dto.setCode(rs.getString("CODE"));
            dto.setDescription(description);
            dto.setLabel(description);
            dto.setValue(poid);
            return dto;
        }, args);
        Map<Long, LovGetListDto> result = new HashMap<>();
        rows.forEach(d -> result.put(d.getPoid(), d));
        return result;
    }

    private List<Long> filterNonNull(Long... poids) {
        List<Long> result = new ArrayList<>();
        for (Long p : poids) {
            if (p != null) result.add(p);
        }
        return result;
    }

    private List<String> filterNonNullStr(String... codes) {
        List<String> result = new ArrayList<>();
        for (String c : codes) {
            if (c != null && !c.isBlank()) result.add(c);
        }
        return result;
    }

    private Long parseLongSafely(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse numeric value: {}", value);
            return null;
        }
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
    private void saveDetailTables(ManifestCorrectorCreateDTO dto, ShipBlReprintHdr entity) {
        populateDetailTables(
                entity,
                dto.getChargesDetails(),
                dto.getContainerDetails(),
                false
        );
    }

    /**
     * Update detail tables from DTO
     */
    private void updateDetailTables(ManifestCorrectorUpdateDTO dto, ShipBlReprintHdr entity) {
        Long transactionPoid = entity.getTransactionPoid();
        if (dto.getChargesDetails() != null) {
            updateChargeDetails(transactionPoid, dto.getChargesDetails());
        }

        if (dto.getContainerDetails() != null) {
            updateContainerDetails(transactionPoid, dto.getContainerDetails());
        }
    }

    private void populateDetailTables(ShipBlReprintHdr entity,
                                      List<ManifestCorrectorChargeDtlDto> chargesDetails,
                                      List<ManifestCorrectorContainerDtlDto> containerDetails,
                                      boolean logCreates) {
        Long transactionPoid = entity.getTransactionPoid();
        Long blPoid = parseLongSafely(entity.getBlNumber());

        if (chargesDetails != null && !chargesDetails.isEmpty()) {
            saveProvidedChargeDetails(transactionPoid, chargesDetails, logCreates);
        } else if (blPoid != null) {
            if ("Y".equals(entity.getDoReprint())) {
                loadChargesForDoReprint(transactionPoid, blPoid);
            } else if ("Y".equals(entity.getContainerReprint())) {
                loadChargesForContainerReprint(transactionPoid, blPoid);
            } else if ("Y".equals(entity.getBlReprint())) {
                loadChargesForBlReprint(transactionPoid, blPoid);
            }
        }

        if (containerDetails != null && !containerDetails.isEmpty()) {
            saveProvidedContainerDetails(transactionPoid, containerDetails, logCreates);
        } else if (blPoid != null && "Y".equals(entity.getContainerReprint())) {
            loadContainersForReprint(transactionPoid, blPoid);
        }
    }

    private void saveProvidedChargeDetails(Long transactionPoid,
                                           List<ManifestCorrectorChargeDtlDto> chargesDetails,
                                           boolean logCreates) {
        Long detRowId = chargeDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        if (detRowId == null) detRowId = 0L;

        for (ManifestCorrectorChargeDtlDto chargeDto : chargesDetails) {
            detRowId++;
            ShipBlReprintChargeDtl chargeDtl = mapper.mapChargeDtlFromDto(chargeDto, transactionPoid, detRowId);
            ShipBlReprintChargeDtl saved = chargeDtlRepository.saveAndFlush(chargeDtl);

            if (logCreates) {
                String logDetail = String.format("Row Created on Manifest Corrector Charge Detail with detRowId: %s", saved.getDetRowId());
                loggingService.createLogSummaryEntry(com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            }
        }
    }

    private void saveProvidedContainerDetails(Long transactionPoid,
                                              List<ManifestCorrectorContainerDtlDto> containerDetails,
                                              boolean logCreates) {
        Long detRowId = containerDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
        if (detRowId == null) detRowId = 0L;

        for (ManifestCorrectorContainerDtlDto containerDto : containerDetails) {
            detRowId++;
            ShipBlReprintContainerDtl containerDtl = mapper.mapContainerDtlFromDto(containerDto, transactionPoid, detRowId);
            ShipBlReprintContainerDtl saved = containerDtlRepository.saveAndFlush(containerDtl);

            if (logCreates) {
                String logDetail = String.format("Row Created on Manifest Corrector Container Detail with detRowId: %s", saved.getDetRowId());
                loggingService.createLogSummaryEntry(com.asg.common.lib.security.util.UserContext.getDocumentId(), transactionPoid.toString(), logDetail);
            }
        }
    }

    private void updateChargeDetails(Long transactionPoid, List<ManifestCorrectorChargeDtlDto> chargesDetails) {
        List<ShipBlReprintChargeDtl> toUpdate = new ArrayList<>();
        List<LogRequestDto<ShipBlReprintChargeDtl>> logRequests = new ArrayList<>();

        for (ManifestCorrectorChargeDtlDto chargeDto : chargesDetails) {
            String action = resolveActionType(chargeDto.getActionType(), chargeDto.getDetRowId());
            switch (action) {
                case ACTION_NOCHANGES -> {
                }
                case ACTION_ISDELETED -> {
                    Long detRowId = requireDetRowId(chargeDto.getDetRowId(), "Charge Detail");
                    chargeDtlRepository.deleteById(new ShipBlReprintChargeDtlId(transactionPoid, detRowId));
                    String logDetail = String.format("Row Deleted on Manifest Corrector Charge Detail with detRowId: %s", detRowId);
                    loggingService.createLogSummaryEntry(getDocumentId(), transactionPoid.toString(), logDetail);
                }
                case ACTION_ISCREATED -> {
                    Long maxDetRowId = chargeDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
                    long detRowId = normalizeDetRowId(chargeDto.getDetRowId()) != null
                            ? chargeDto.getDetRowId()
                            : (maxDetRowId != null ? maxDetRowId + 1 : 1L);
                    ShipBlReprintChargeDtl entity = mapper.mapChargeDtlFromDto(chargeDto, transactionPoid, detRowId);
                    chargeDtlRepository.saveAndFlush(entity);
                    String logDetail = String.format("Row Created on Manifest Corrector Charge Detail with detRowId: %s", detRowId);
                    loggingService.createLogSummaryEntry(getDocumentId(), transactionPoid.toString(), logDetail);
                }
                case ACTION_ISUPDATED -> {
                    Long detRowId = requireDetRowId(chargeDto.getDetRowId(), "Charge Detail");
                    ShipBlReprintChargeDtl entity = chargeDtlRepository.findById(new ShipBlReprintChargeDtlId(transactionPoid, detRowId))
                            .orElseThrow(() -> new ResourceNotFoundException("Charge Detail", "detRowId", detRowId.toString()));
                    ShipBlReprintChargeDtl oldEntity = new ShipBlReprintChargeDtl();
                    BeanUtils.copyProperties(entity, oldEntity);
                    mapper.updateChargeDtlEntity(chargeDto, entity);
                    toUpdate.add(entity);
                    String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detRowId);
                    logRequests.add(new LogRequestDto<>(oldEntity, entity, ShipBlReprintChargeDtl.class, getDocumentId(), transactionPoid.toString(), logDetail));
                }
            }
        }

        if (!toUpdate.isEmpty()) {
            chargeDtlRepository.saveAllAndFlush(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
    }

    private void updateContainerDetails(Long transactionPoid, List<ManifestCorrectorContainerDtlDto> containerDetails) {
        List<ShipBlReprintContainerDtl> toUpdate = new ArrayList<>();
        List<LogRequestDto<ShipBlReprintContainerDtl>> logRequests = new ArrayList<>();

        for (ManifestCorrectorContainerDtlDto containerDto : containerDetails) {
            String action = resolveActionType(containerDto.getActionType(), containerDto.getDetRowId());
            switch (action) {
                case ACTION_NOCHANGES -> {
                }
                case ACTION_ISDELETED -> {
                    Long detRowId = requireDetRowId(containerDto.getDetRowId(), "Container Detail");
                    containerDtlRepository.deleteById(new ShipBlReprintContainerDtlId(transactionPoid, detRowId));
                    String logDetail = String.format("Row Deleted on Manifest Corrector Container Detail with detRowId: %s", detRowId);
                    loggingService.createLogSummaryEntry(getDocumentId(), transactionPoid.toString(), logDetail);
                }
                case ACTION_ISCREATED -> {
                    Long maxDetRowId = containerDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
                    long detRowId = normalizeDetRowId(containerDto.getDetRowId()) != null
                            ? containerDto.getDetRowId()
                            : (maxDetRowId != null ? maxDetRowId + 1 : 1L);
                    ShipBlReprintContainerDtl entity = mapper.mapContainerDtlFromDto(containerDto, transactionPoid, detRowId);
                    containerDtlRepository.saveAndFlush(entity);
                    String logDetail = String.format("Row Created on Manifest Corrector Container Detail with detRowId: %s", detRowId);
                    loggingService.createLogSummaryEntry(getDocumentId(), transactionPoid.toString(), logDetail);
                }
                case ACTION_ISUPDATED -> {
                    Long detRowId = requireDetRowId(containerDto.getDetRowId(), "Container Detail");
                    ShipBlReprintContainerDtl entity = containerDtlRepository.findById(new ShipBlReprintContainerDtlId(transactionPoid, detRowId))
                            .orElseThrow(() -> new ResourceNotFoundException("Container Detail", "detRowId", detRowId.toString()));
                    ShipBlReprintContainerDtl oldEntity = new ShipBlReprintContainerDtl();
                    BeanUtils.copyProperties(entity, oldEntity);
                    mapper.updateContainerDtlEntity(containerDto, entity);
                    toUpdate.add(entity);
                    String logDetail = String.format("KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s", transactionPoid, detRowId);
                    logRequests.add(new LogRequestDto<>(oldEntity, entity, ShipBlReprintContainerDtl.class, getDocumentId(), transactionPoid.toString(), logDetail));
                }
            }
        }

        if (!toUpdate.isEmpty()) {
            containerDtlRepository.saveAllAndFlush(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
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

    private String resolveActionType(String actionType, Long detRowId) {
        if (actionType == null || actionType.trim().isEmpty()) {
            return normalizeDetRowId(detRowId) == null ? ACTION_ISCREATED : ACTION_ISUPDATED;
        }

        String normalized = actionType.trim().replace("_", "").replace(" ", "").toUpperCase();
        return switch (normalized) {
            case "CREATE", "CREATED", ACTION_ISCREATED -> ACTION_ISCREATED;
            case "UPDATE", "UPDATED", ACTION_ISUPDATED -> ACTION_ISUPDATED;
            case "DELETE", "DELETED", ACTION_ISDELETED -> ACTION_ISDELETED;
            case "NOCHANGE", "NOCHANGES", "UNCHANGED" -> ACTION_NOCHANGES;
            default -> ACTION_NOCHANGES;
        };
    }

    private Long normalizeDetRowId(Long detRowId) {
        if (detRowId == null || detRowId == 0L) {
            return null;
        }
        return detRowId;
    }

    private Long requireDetRowId(Long detRowId, String detailName) {
        Long normalizedDetRowId = normalizeDetRowId(detRowId);
        if (normalizedDetRowId == null) {
            throw new ValidationException(detailName + " detRowId is required for this action");
        }
        return normalizedDetRowId;
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
