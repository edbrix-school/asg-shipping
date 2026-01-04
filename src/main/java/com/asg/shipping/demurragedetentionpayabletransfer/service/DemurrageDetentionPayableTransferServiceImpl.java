package com.asg.shipping.demurragedetentionpayabletransfer.service;


import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.*;
import com.asg.shipping.demurragedetentionpayabletransfer.entity.*;
import com.asg.shipping.demurragedetentionpayabletransfer.repository.*;
import com.asg.shipping.demurragedetentionpayabletransfer.util.DemurrageDetentionPayableTransferMapper;
import com.asg.shipping.exceptions.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

/**
 * Service implementation for Demurrage/Detention Payable Transfer operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemurrageDetentionPayableTransferServiceImpl implements DemurrageDetentionPayableTransferService {

    private final ShipDemDetnTransferHdrRepository headerRepository;
    private final ShipDemDetnTransferDtlRepository transferDtlRepository;
    private final ShipDemDtnTransferBillDtlRepository billDtlRepository;
    private final DocumentSearchService documentService;
    private final LovDataService lovService;
    private final DemurrageDetentionPayableTransferMapper mapper;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchDemurrageDetentionPayableTransfer(String docId, com.asg.common.lib.dto.FilterRequestDto request, Pageable pageable) {
        log.info("Searching demurrage/detention payable transfer records with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);
        
        // Filter out any filters with null searchField to prevent NullPointerException
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
    public DemurrageDetentionPayableTransferDto getDemurrageDetentionPayableTransfer(Long id) {
        log.info("Getting demurrage/detention payable transfer with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        ShipDemDetnTransferHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString()));

        if ("Y".equals(entity.getDeleted())) {
            throw new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString());
        }

        // Load detail tables
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
    public DemurrageDetentionPayableTransferDto createDemurrageDetentionPayableTransfer(
            DemurrageDetentionPayableTransferCreateDTO dto, Long companyPoid, Long groupPoid) {
        log.info("Creating demurrage/detention payable transfer");

        // Validate
        validateCreateDTO(dto, companyPoid, groupPoid);

        // Auto-populate GL accounts if LINE_POID or BL_TYPE provided
        Long payableGlPoid = dto.getPayableGlPoid();
        Long incomeGlPoid = dto.getIncomeGlPoid();

        if (dto.getLinePoid() != null && dto.getBlType() != null) {
            // Call PROC_DEM_DEN_SET_DEFAULT to get default payable GL
            String defaultPayableGl = callProcDemDenSetDefault(dto.getLinePoid(), dto.getBlType());
            if (defaultPayableGl != null && !defaultPayableGl.equals("NO_DATA")) {
                payableGlPoid = Long.parseLong(defaultPayableGl);
            }

            // Get income GL from global parameter
            if (incomeGlPoid == null) {
                incomeGlPoid = getIncomeGlPoidFromParameter(groupPoid);
            }
        }

        // Validate required fields
        if (payableGlPoid == null) {
            throw new ValidationException("Payable GL POID is required");
        }
        if (incomeGlPoid == null) {
            throw new ValidationException("Income GL POID is required");
        }

        // Create header entity
        ShipDemDetnTransferHdr entity = new ShipDemDetnTransferHdr();
        mapper.mapCreateDTOToEntity(dto, entity, groupPoid, companyPoid);
        entity.setPayableGlPoid(payableGlPoid);
        entity.setIncomeGlPoid(incomeGlPoid);

        // Generate DOC_REF
        String docRef = generateDocRef(companyPoid);
        entity.setDocRef(docRef);

        // Validate date range
        if (entity.getEmptyFromDate() != null && entity.getEmptyToDate() != null) {
            if (entity.getEmptyToDate().isBefore(entity.getEmptyFromDate())) {
                throw new ValidationException("Empty To Date must be greater than or equal to Empty From Date");
            }
        }

        // Save header (generates TRANSACTION_POID via IDENTITY)
        ShipDemDetnTransferHdr saved = headerRepository.save(entity);

        // Create detail records
        createDetailRecords(saved.getTransactionPoid(), dto.getTransferDetails(), dto.getBillDetails());

        // Fetch all detail records for response
        List<ShipDemDetnTransferDtl> transferDetails = transferDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());
        List<ShipDemDtnTransferBillDtl> billDetails = billDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());

        DemurrageDetentionPayableTransferDto result = mapper.mapToDto(saved);
        result.setTransferDetails(mapper.mapTransferDtlListToDto(transferDetails));
        result.setBillDetails(mapper.mapBillDtlListToDto(billDetails));
        enrichLovData(result);

        log.info("Successfully created demurrage/detention payable transfer with id: {}", saved.getTransactionPoid());
        return result;
    }

    @Override
    @Transactional
    public DemurrageDetentionPayableTransferDto updateDemurrageDetentionPayableTransfer(
            Long id, DemurrageDetentionPayableTransferUpdateDTO dto, Long companyPoid, Long groupPoid) {
        log.info("Updating demurrage/detention payable transfer with id: {}", id);

        // Find existing entity
        ShipDemDetnTransferHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString()));

        if ("Y".equals(entity.getDeleted())) {
            throw new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString());
        }

        // Validate
        validateUpdateDTO(dto, id, companyPoid, groupPoid);

        // Check if LINE_POID or BL_TYPE changed - auto-populate GL accounts
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

        // Update header entity
        mapper.mapUpdateDTOToEntity(dto, entity);

        // Validate date range
        if (entity.getEmptyFromDate() != null && entity.getEmptyToDate() != null) {
            if (entity.getEmptyToDate().isBefore(entity.getEmptyFromDate())) {
                throw new ValidationException("Empty To Date must be greater than or equal to Empty From Date");
            }
        }

        ShipDemDetnTransferHdr saved = headerRepository.save(entity);

        // Update detail records
        updateDetailRecords(id, dto.getTransferDetails(), dto.getBillDetails());

        // Fetch all detail records for response
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
    public void deleteDemurrageDetentionPayableTransfer(Long id, Long companyPoid, Long groupPoid) {
        log.info("Deleting demurrage/detention payable transfer with id: {}", id);

        ShipDemDetnTransferHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString()));

        if ("Y".equals(entity.getDeleted())) {
            log.info("Demurrage/Detention Payable Transfer with id: {} is already deleted", id);
            return;
        }

        entity.setDeleted("Y");
        entity.setLastModifiedBy(getCurrentUser());
        entity.setLastModifiedDate(LocalDateTime.now());

        headerRepository.save(entity);

        log.info("Successfully deleted demurrage/detention payable transfer with id: {}", id);
    }

    @Override
    @Transactional
    public DemurrageDetentionPayableTransferDto processData(Long id, ProcessDataRequestDTO request) {
        log.info("Processing data for demurrage/detention payable transfer with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        ShipDemDetnTransferHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString()));

        // Use request criteria or header criteria
        Long linePoid = request.getLinePoid() != null ? request.getLinePoid() : entity.getLinePoid();
        String blType = request.getBlType() != null ? request.getBlType() : entity.getBlType();
        LocalDate emptyFromDate = request.getEmptyFromDate() != null ? request.getEmptyFromDate() : entity.getEmptyFromDate();
        LocalDate emptyToDate = request.getEmptyToDate() != null ? request.getEmptyToDate() : entity.getEmptyToDate();

        if (linePoid == null || blType == null) {
            throw new ValidationException("Line POID and BL Type must be set in header or request");
        }

        // Delete existing transfer details
        transferDtlRepository.deleteByTransactionPoid(id);

        // Query available containers (similar to legacy VwShipDemDtnTransferView)
        // This would typically query a view or use a stored procedure
        // For now, we'll use a native query to get available containers
        String sql = "SELECT * FROM VW_SHIP_DEM_DTN_TRANSFER " +
                "WHERE (MAINFEST_TRANSACTION_POID, CONTAINER_NO) NOT IN (" +
                "  SELECT MAINFEST_TRANSACTION_POID, CONTAINER_NO " +
                "  FROM SHIP_DEM_DETN_TRANSFER_HDR SDDHDR " +
                "  INNER JOIN SHIP_DEM_DETN_TRANSFER_DTL SDTRNF ON SDTRNF.TRANSACTION_POID = SDDHDR.TRANSACTION_POID " +
                "  WHERE NVL(DELETED, 'N') = 'N' AND NVL(IS_SELECT, 'N') = 'Y'" +
                ") " +
                "AND BL_TYPE = ? AND LINE_POID = ? AND COMPANY_POID = ? " +
                "ORDER BY BL_NUMBER";

        // Note: This is a simplified version. In production, you would need to:
        // 1. Create a proper entity or DTO for the view result
        // 2. Map the results to transfer detail entities
        // 3. Handle date range filtering if provided

        log.warn("Process data functionality requires implementation of VW_SHIP_DEM_DTN_TRANSFER view query");

        // For now, return the current state
        List<ShipDemDetnTransferDtl> transferDetails = transferDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipDemDtnTransferBillDtl> billDetails = billDtlRepository.findByTransactionPoidOrderByDetRowId(id);

        DemurrageDetentionPayableTransferDto result = mapper.mapToDto(entity);
        result.setTransferDetails(mapper.mapTransferDtlListToDto(transferDetails));
        result.setBillDetails(mapper.mapBillDtlListToDto(billDetails));
        enrichLovData(result);

        return result;
    }

    @Override
    @Transactional
    public void updateFreeDays(Long id, UpdateFreeDaysRequestDTO request) {
        log.info("Updating free days for demurrage/detention payable transfer with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();

        ShipDemDetnTransferHdr entity = headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Demurrage/Detention Payable Transfer", "transactionPoid", id.toString()));

        // Check if there are any bill details for this transaction
        List<ShipDemDtnTransferBillDtl> existingBillDetails = billDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        if (existingBillDetails.isEmpty()) {
            throw new ValidationException("No bill details found for transaction: " + id + ". Please create bill details first.");
        }

        // Update each container's free days and call stored procedure
        for (UpdateFreeDaysRequestDTO.ContainerFreeDaysUpdate update : request.getContainerUpdates()) {
            ShipDemDtnTransferBillDtl billDetail = billDtlRepository
                    .findByTransactionPoidAndDetRowId(id, update.getDetRowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bill Detail", "detRowId", update.getDetRowId().toString()));

            // Validate container matches
            if (!billDetail.getContainerNo().equals(update.getContainerNo())) {
                throw new ValidationException("Container details mismatch for detRowId: " + update.getDetRowId());
            }

            // Call PROC_SHIP_CNT_PPFREDAYS_UPDATE
            callProcShipCntPpfredaysUpdate(
                    update.getMainfestTransactionPoid(),
                    update.getExtraFreeDaysPrnpls(),
                    update.getContainerNo()
            );
        }

        log.info("Successfully updated free days for demurrage/detention payable transfer with id: {}", id);
    }

    // Private helper methods

    private void validateCreateDTO(DemurrageDetentionPayableTransferCreateDTO dto, Long companyPoid, Long groupPoid) {
        if (dto.getLinePoid() == null) {
            throw new ValidationException("Line POID is required");
        }
        if (dto.getBlType() == null || (!dto.getBlType().equals("IMPORT") && !dto.getBlType().equals("EXPORT"))) {
            throw new ValidationException("BL Type must be IMPORT or EXPORT");
        }

        // Validate container uniqueness for transfer details
        if (dto.getTransferDetails() != null) {
            for (DemurrageDetentionTransferDetailDto detail : dto.getTransferDetails()) {
                if (detail.getMainfestTransactionPoid() != null && detail.getContainerNo() != null) {
                    if (transferDtlRepository.existsByManifestAndContainerAndSelected(
                            detail.getMainfestTransactionPoid(), detail.getContainerNo())) {
                        throw new ValidationException(
                                "Container " + detail.getContainerNo() + " is already selected in another transfer record");
                    }
                }
            }
        }
    }

    private void validateUpdateDTO(DemurrageDetentionPayableTransferUpdateDTO dto, Long id, Long companyPoid, Long groupPoid) {
        if (dto.getBlType() != null && !dto.getBlType().equals("IMPORT") && !dto.getBlType().equals("EXPORT")) {
            throw new ValidationException("BL Type must be IMPORT or EXPORT");
        }

        // Validate container uniqueness for updated/new transfer details
        if (dto.getTransferDetails() != null) {
            for (DemurrageDetentionTransferDetailDto detail : dto.getTransferDetails()) {
                if (detail.getMainfestTransactionPoid() != null && detail.getContainerNo() != null) {
                    // Check if this container is already selected in another record (excluding current record)
                    // This would require a custom query to check across all records except current
                    // For now, we'll do a simple check
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
                }
            }
        }
    }

    private void createDetailRecords(Long transactionPoid,
                                     List<DemurrageDetentionTransferDetailDto> transferDetails,
                                     List<DemurrageDetentionTransferBillDetailDto> billDetails) {
        // Create transfer details
        if (transferDetails != null && !transferDetails.isEmpty()) {
            Long maxDetRowId = transferDtlRepository.getMaxDetRowId(transactionPoid);
            long currentDetRowId = maxDetRowId != null ? maxDetRowId : 0;

            for (DemurrageDetentionTransferDetailDto detailDto : transferDetails) {
                currentDetRowId++;
                ShipDemDetnTransferDtl detail = mapper.mapTransferDtlFromDto(detailDto, transactionPoid);
                detail.setDetRowId(currentDetRowId);
                detail.setTransactionPoid(transactionPoid);
                transferDtlRepository.save(detail);
            }
        }

        // Create bill details
        if (billDetails != null && !billDetails.isEmpty()) {
            Long maxDetRowId = billDtlRepository.getMaxDetRowId(transactionPoid);
            long currentDetRowId = maxDetRowId != null ? maxDetRowId : 0;

            for (DemurrageDetentionTransferBillDetailDto detailDto : billDetails) {
                currentDetRowId++;
                ShipDemDtnTransferBillDtl detail = mapper.mapBillDtlFromDto(detailDto, transactionPoid);
                detail.setDetRowId(currentDetRowId);
                detail.setTransactionPoid(transactionPoid);
                billDtlRepository.save(detail);
            }
        }
    }

    private void updateDetailRecords(Long transactionPoid,
                                     List<DemurrageDetentionTransferDetailDto> transferDetails,
                                     List<DemurrageDetentionTransferBillDetailDto> billDetails) {
        // Delete existing details
        transferDtlRepository.deleteByTransactionPoid(transactionPoid);
        billDtlRepository.deleteByTransactionPoid(transactionPoid);

        // Create new details
        createDetailRecords(transactionPoid, transferDetails, billDetails);
    }

    /**
     * Call PROC_SHIP_CNT_PPFREDAYS_UPDATE stored procedure
     * Parameters: p_transaction_poid (varchar2), p_EXTRA_FREE_DAYS_PRNPLS (varchar2), p_container_no (varchar2)
     */
    private void callProcShipCntPpfredaysUpdate(Long mainfestTransactionPoid, java.math.BigDecimal extraFreeDaysPrnpls, String containerNo) {
        try {
            String sql = "{call PROC_SHIP_CNT_PPFREDAYS_UPDATE(?, ?, ?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setString(1, String.valueOf(mainfestTransactionPoid)); // Convert Long to String as SP expects varchar2
                cs.setString(2, String.valueOf(extraFreeDaysPrnpls)); // Convert BigDecimal to String as SP expects varchar2
                cs.setString(3, containerNo);
                cs.execute();
                return null;
            });
            log.debug("Successfully called PROC_SHIP_CNT_PPFREDAYS_UPDATE for manifest: {}, container: {}", mainfestTransactionPoid, containerNo);
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_CNT_PPFREDAYS_UPDATE", e);
            throw new ValidationException("Failed to update container free days: " + e.getMessage());
        }
    }

    /**
     * Call PROC_DEM_DEN_SET_DEFAULT stored procedure
     * Parameters: P_LINE_POID (NUMBER), P_BL_TYPE (VARCHAR2), P_AC_PAYABLE (OUT NUMBER)
     * Returns: Payable GL POID as String or "NO_DATA"
     */
    private String callProcDemDenSetDefault(Long linePoid, String blType) {
        try {
            return jdbcTemplate.execute(
                    "{call PROC_DEM_DEN_SET_DEFAULT(?, ?, ?)}",
                    (CallableStatement cs) -> {
                        cs.setLong(1, linePoid); // P_LINE_POID is NUMBER
                        cs.setString(2, blType); // P_BL_TYPE is VARCHAR2
                        cs.registerOutParameter(3, Types.NUMERIC); // P_AC_PAYABLE is OUT NUMBER
                        cs.execute();
                        
                        // Handle the case where SP sets P_AC_PAYABLE to 'NO_DATA' (which is invalid for NUMBER)
                        // The SP has a bug - it tries to assign string to NUMBER parameter
                        try {
                            Long result = cs.getLong(3);
                            return result != null ? String.valueOf(result) : "NO_DATA";
                        } catch (Exception e) {
                            // If SP fails due to invalid assignment, return NO_DATA
                            log.warn("SP returned invalid value for NUMBER parameter: {}", e.getMessage());
                            return "NO_DATA";
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Error calling PROC_DEM_DEN_SET_DEFAULT", e);
            return "NO_DATA";
        }
    }

    /**
     * Get income GL POID from global parameter DEM_DET_ACCOUNT_INCOME
     */
    private Long getIncomeGlPoidFromParameter(Long groupPoid) {
        try {
            // Query global parameters table
            String sql = "SELECT PARAMETER_VALUE FROM GLOBAL_PARAMETERS " +
                    "WHERE PARAMETER_KEYID_TYPE = 'DEM_DET_ACCOUNT_INCOME' " +
                    "AND GROUP_POID = ?";
            String value = jdbcTemplate.queryForObject(sql, String.class, groupPoid);
            if (value != null && !value.isEmpty()) {
                return Long.parseLong(value);
            }
        } catch (Exception e) {
            log.warn("Failed to get DEM_DET_ACCOUNT_INCOME parameter value", e);
        }
        return null;
    }

    private String generateDocRef(Long companyPoid) {
        // TODO: Implement DOC_REF generation similar to legacy RTN_GLOBAL_SEQ_NO('DEM_DET_TRANSFER', P_PREFIX, 5)
        // For now, use a simple pattern
        return "DEM-DET-" + String.format("%08d", System.currentTimeMillis() % 100000000);
    }

    private void enrichLovData(DemurrageDetentionPayableTransferDto dto) {
        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();
        Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();
        Long userPoid = com.asg.common.lib.security.util.UserContext.getUserPoid();

        // Enrich header LOVs
        enrichLov(dto.getLinePoid(), dto::setLinePoidDet, "LINE", groupPoid, companyPoid, userPoid);
        enrichLov(dto.getPayableGlPoid(), dto::setPayableGlPoidDet, "GL_ACCOUNT", groupPoid, companyPoid, userPoid);
        enrichLov(dto.getIncomeGlPoid(), dto::setIncomeGlPoidDet, "GL_ACCOUNT", groupPoid, companyPoid, userPoid);

        // Enrich transfer detail LOVs
        if (dto.getTransferDetails() != null) {
            for (DemurrageDetentionTransferDetailDto detail : dto.getTransferDetails()) {
                enrichLov(detail.getMainfestTransactionPoid(), detail::setMainfestTransactionPoidDet, "MANIFEST", groupPoid, companyPoid, userPoid);
                enrichLov(detail.getLinePoid(), detail::setLinePoidDet, "LINE", groupPoid, companyPoid, userPoid);
                if (detail.getEquipmentIsoType() != null && !detail.getEquipmentIsoType().isEmpty()) {
                    enrichLov(detail.getEquipmentIsoType(), detail::setEquipmentIsoTypeDet, "CONTAINER_TYPE", groupPoid, companyPoid, userPoid);
                }
            }
        }

        // Enrich bill detail LOVs
        if (dto.getBillDetails() != null) {
            for (DemurrageDetentionTransferBillDetailDto detail : dto.getBillDetails()) {
                enrichLov(detail.getGlPoid(), detail::setGlPoidDet, "GL_ACCOUNT", groupPoid, companyPoid, userPoid);
            }
        }
    }

    private void enrichLov(Long poid, java.util.function.Consumer<LovGetListDto> setter, String lovType,
                           Long groupPoid, Long companyPoid, Long userPoid) {
        if (poid != null) {
            try {
                LovGetListDto lovItem = lovService.getDetailsByPoidAndLovName(poid, lovType);
                setter.accept(lovItem);
            } catch (Exception e) {
                log.warn("Failed to fetch {} LOV for poid: {}", lovType, poid, e);
            }
        }
    }

    private void enrichLov(String code, java.util.function.Consumer<LovGetListDto> setter, String lovType,
                           Long groupPoid, Long companyPoid, Long userPoid) {
        if (code != null && !code.isEmpty()) {
            try {
                LovGetListDto lovItem = lovService.getDetailsByCodeAndLovName(code, lovType);
                setter.accept(lovItem);
            } catch (Exception e) {
                log.warn("Failed to fetch {} LOV for code: {}", lovType, code, e);
            }
        }
    }
}