package com.asg.shipping.exportManifestBl.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.ApprovalService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.exportManifestBl.dto.*;
import com.asg.shipping.exportManifestBl.entity.*;
import com.asg.shipping.exportManifestBl.repository.*;
import com.asg.shipping.exportManifestBl.repository.ShipBlToFfRepository;
import com.asg.shipping.exportManifestBl.util.ExportManifestBlMapper;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestCargoDtl;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestChargesDtl;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestContainerDtl;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestGeneralDtl;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestHdr;
import com.asg.shipping.common.service.LovService;
import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.importmanifestupdate.dto.CargoDescriptionRequestDto;
import com.asg.shipping.importmanifestupdate.dto.ChargeRequestDto;
import com.asg.shipping.importmanifestupdate.dto.ContainerRequestDto;
import com.asg.shipping.importmanifestupdate.dto.GeneralCargoRequestDto;
import com.asg.shipping.exportManifestUpdate.dto.GenerateBlPrintRequest;
import com.asg.shipping.exportManifestUpdate.dto.GenerateManifestRequest;
import com.asg.shipping.bookingFormSH.entity.ShipMateContainerDtl;
import com.asg.shipping.bookingFormSH.entity.ShipMateHdr;
import com.asg.shipping.bookingFormSH.repository.ShipMateContainerDtlRepository;
import com.asg.shipping.bookingFormSH.repository.ShipMateHdrRepository;
import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageHdrEntity;
import com.asg.shipping.vesselvoyagecreation.repository.ShipVoyageHdrRepository;
import com.asg.shipping.importmanifestbl.dto.ChargeDefaultsRequestDto;
import com.asg.shipping.importmanifestbl.dto.ChargeDefaultsResponseDto;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestDtlId;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestCargoDtlId;
import com.asg.shipping.importmanifestupdate.service.BlManifestValidationService;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.asg.common.lib.security.util.UserContext.getCompanyPoid;
import static com.asg.common.lib.security.util.UserContext.getGroupPoid;
import static com.asg.common.lib.security.util.UserContext.getUserPoid;
import static org.springframework.util.StringUtils.hasText;

import net.sf.jasperreports.engine.JasperReport;
import javax.sql.DataSource;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportManifestBlServiceImpl implements ExportManifestBlService {

    private static final String UPDATE_TYPE_AUTO_CUSTOMER_CHARGE = "AUTOCUSTOMERCHARGE";
    /** Matches PROC_SHIP_BL_PAGE_SAVE_AFTER — rolls up CBM/weight/packs and port auto-charges. */
    private static final String UPDATE_TYPE_AUTOSUM_WEIGHT_PACK = "AUTOSUMWEIGHTPACKATE";
    private static final String SALES_INVOICE_DOCUMENT_ID = "300-102";
    /** Matches document master DOC_NAME / ROUTE_NAME for SPA tab navigation. */
    private static final String SALES_INVOICE_DOCUMENT_NAME = "Sales Invoice (Shipping)";
    private static final String EXPORT_MANIFEST_DOCUMENT_ID = "100-104";
    private static final String CARGO_DESCRIPTION_TYPE_DESC = "DESC";
    private static final String LOG_TABLE_HDR = "SHIP_BL_MANIFEST_HDR";
    private static final String LOG_TABLE_CARGO = "SHIP_BL_MANIFEST_CARGO_DTL";

    private final ExportManifestBlHdrRepository repository;
    private final ExportManifestBlGeneralDtlRepository generalDtlRepository;
    private final ExportManifestBlCargoDtlRepository cargoDtlRepository;
    private final ExportManifestBlContainerDtlRepository containerDtlRepository;
    private final ExportManifestBlChargesDtlRepository chargesDtlRepository;
    private final com.asg.shipping.exportManifestUpdate.service.ExportManifestBlService manifestUpdateService;
    private final ExportManifestBlProcRepository procRepository;
    private final DocumentSearchService documentService;
    private final ExportManifestBlMapper mapper;
    private final JdbcTemplate jdbcTemplate;
    private final ShipBlToFfRepository shipBlToFfRepository;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;
    private final LovService lovService;
    private final ExportManifestBlLoadBookingRepository loadBookingRepository;
    private final ShipMateHdrRepository shipMateHdrRepository;
    private final ShipMateContainerDtlRepository shipMateContainerDtlRepository;
    private final ExportManifestBlBookingSelectionRepository bookingSelectionRepository;
    private final ShipVoyageHdrRepository shipVoyageHdrRepository;
    private final ApprovalService approvalService;
    private final BlManifestValidationService blManifestValidationService;

    private String normalizeActionType(String actionType) {
        return actionType == null ? "" : actionType.trim().toLowerCase();
    }

    private String normalizeContainerNo(String containerNo) {
        if (containerNo == null) {
            return null;
        }
        String trimmed = containerNo.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional
    public ExportManifestBlRequestDto createExportManifestBl(ExportManifestBlCreateDto dto) {
        log.info("Creating new Export Manifest BL");

     
        validateMandatoryFields(dto);

        if (dto.getBlNumber() != null && dto.getVoyageTransactionPoid() != null) {
            String trimmedBlNumber = dto.getBlNumber().trim();
            if (repository.existsByVoyageTransactionPoidAndBlNumber(dto.getVoyageTransactionPoid(), trimmedBlNumber)) {
                throw new ValidationException("Export BL number already exists for this voyage");
            }
        }

        ExportManifestBlHdr entity = mapper.mapToEntity(dto);
        entity.setCompanyPoid(UserContext.getCompanyPoid());
        entity.setGroupPoid(UserContext.getGroupPoid());
        
        if (entity.getDocRef() == null || entity.getDocRef().isEmpty()) {
            entity.setDocRef(generateDocRef(entity.getCompanyPoid()));
        }

        formatEdiFields(entity);

        ExportManifestBlHdr saved = repository.saveAndFlush(entity);
        Long transactionPoid = saved.getTransactionPoid();
        log.info("Export Manifest BL header saved with transactionPoid: {}", transactionPoid);

        
        loggingService.createLogSummaryEntry(
                LogDetailsEnum.CREATED,
                UserContext.getDocumentId(),
                transactionPoid.toString()
        );

        saveDetailTables(dto, transactionPoid);

        ExportManifestBlRequestDto result = mapper.mapToDto(saved);
        loadDetailTables(result, transactionPoid);

        log.info("Successfully created Export Manifest BL with id: {}", transactionPoid);
        return result;
    }

    @Override
    @Transactional
    public ExportManifestBlRequestDto updateExportManifestBl(Long id, ExportManifestBlUpdateDto dto, Long companyPoid, Long groupPoid) {
        log.info("Updating Export Manifest BL with id: {}", id);

        ExportManifestBlHdr entity = repository.findActiveExportBlByTransactionPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Export Manifest BL", "transactionPoid", id.toString()));

        ExportManifestBlHdr oldEntity = new ExportManifestBlHdr();
        BeanUtils.copyProperties(entity, oldEntity);

        String docId = UserContext.getDocumentId();
        String docKeyPoid = id.toString();
        String cargoDescriptionBefore = joinCargoTextForAudit(id, "DESC", "DESCRIPTION", "CARGO");
        String cargoMarksBefore = joinCargoTextForAudit(id, "MARK", "MARKS");

        if (dto.getBlNumber() != null && !dto.getBlNumber().trim().equals(entity.getBlNumber())) {
            String trimmedBlNumber = dto.getBlNumber().trim();
            Long voyagePoid = dto.getVoyageTransactionPoid() != null 
                    ? dto.getVoyageTransactionPoid() 
                    : entity.getVoyageTransactionPoid();
            
            if (repository.existsByVoyageTransactionPoidAndBlNumberExcludingPoid(voyagePoid, trimmedBlNumber, id)) {
                throw new ValidationException("Export BL number already exists for this voyage");
            }
        }

        mapper.mapUpdateDTOToEntity(dto, entity);
        
        if (hasAnyEdiChange(dto)) {
            formatEdiFields(entity);
        }

        ExportManifestBlHdr saved = repository.saveAndFlush(entity);

        if (hasDetailUpdates(dto)) {
            applyDetailActions(dto, id);
        }

        logAggregateCargoField(docId, docKeyPoid, "CargoDescription",
                cargoDescriptionBefore, joinCargoTextForAudit(id, "DESC", "DESCRIPTION", "CARGO"));
        logAggregateCargoField(docId, docKeyPoid, "Marks",
                cargoMarksBefore, joinCargoTextForAudit(id, "MARK", "MARKS"));
        if (dto.getBookingMateVoyageNo() != null) {
            logBookingMateVoyageFilter(docId, docKeyPoid, dto.getPreviousBookingMateVoyageNo(), dto.getBookingMateVoyageNo());
        }

        loggingService.logChanges(
                copyHdrForLog(oldEntity),
                copyHdrForLog(saved),
                ShipBlManifestHdr.class,
                docId,
                docKeyPoid,
                LogDetailsEnum.MODIFIED,
                "TRANSACTION_POID"
        );

        ExportManifestBlRequestDto result = mapper.mapToDto(saved);
        loadDetailTables(result, id);

        log.info("Successfully updated Export Manifest BL with id: {}", id);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchExportManifestBl(String docId, FilterRequestDto request, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        log.info("Searching Export Manifest BL with docId: {}", docId);

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", fromDate, toDate);

        // Add BL_TYPE = 'EXPORT' filter
        if (filters == null) {
            filters = new ArrayList<>();
        }
        filters.add(new FilterDto("BL_TYPE", "EXPORT"));

        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "DOC_REF",           // label field for display
                "TRANSACTION_POID"   // value field (primary key)
        );

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    public ExportManifestBlRequestDto getExportManifestBl(Long id) {
        log.info("Getting Export Manifest BL with id: {}", id);

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();

        ExportManifestBlHdr entity = repository.findExportBlByTransactionPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Export Manifest BL", "transactionPoid", id.toString()));

        ExportManifestBlRequestDto dto = mapper.mapToDto(entity);
        loadDetailTables(dto, id);

        
        loggingService.createLogSummaryEntry(
                LogDetailsEnum.VIEWED,
                UserContext.getDocumentId(),
                id.toString()
        );

        log.info("Successfully retrieved Export Manifest BL with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public void deleteExportManifestBl(Long id) {
        log.info("Deleting Export Manifest BL with id: {}", id);

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();

        ExportManifestBlHdr entity = repository.findExportBlByTransactionPoid(id, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Export Manifest BL", "transactionPoid", id.toString()));

        if ("Y".equals(entity.getDeleted())) {
            log.info("Export Manifest BL with id: {} is already deleted", id);
            return;
        }

        entity.setDeleted("Y");
        repository.save(entity);

        log.info("Successfully deleted Export Manifest BL with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ShipBlToFfDto getShipBlToFfByBlNumber(String blNumber) {
        log.info("Getting FF job details for BL number: {}", blNumber);

        if (blNumber == null || blNumber.trim().isEmpty()) {
            throw new ValidationException("BL number is required");
        }

        Long companyPoid = UserContext.getCompanyPoid();
        ShipBlToFfDto result = enrichFfJobRow(shipBlToFfRepository.findByBlNumber(blNumber.trim(), companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FF Job Details", "BL number", blNumber)));

        log.info("Successfully retrieved FF job details for BL number: {}", blNumber);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipBlToFfDto> getShipBlToFfByManifestPoid(Long transactionPoid) {
        log.info("Getting FF job details for manifest transactionPoid: {}", transactionPoid);

        Long companyPoid = UserContext.getCompanyPoid();
        validateActiveExportManifestBl(transactionPoid, companyPoid);

        return shipBlToFfRepository.findAllByManifestPoid(transactionPoid, companyPoid).stream()
                .map(this::enrichFfJobRow)
                .toList();
    }

    @Override
    @Transactional
    public void deleteFfPurchaseJournal(Long transactionPoid, Long rnumid) {
        log.info("Deleting FF purchase journal for manifest transactionPoid: {}, rnumid: {}", transactionPoid, rnumid);

        if (rnumid == null) {
            throw new ValidationException("FF job row id is required");
        }

        Long companyPoid = UserContext.getCompanyPoid();
        Long groupPoid = UserContext.getGroupPoid();
        validateActiveExportManifestBl(transactionPoid, companyPoid);

        ShipBlToFfDto ffRow = shipBlToFfRepository.findByRnumidAndManifestPoid(rnumid, transactionPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("FF Job", "rnumid", rnumid.toString()));

        if (!hasText(ffRow.getFfPj())) {
            throw new ValidationException("No FF Purchase Journal exists to delete for the selected row");
        }
        if (!hasText(ffRow.getMasterBlNo())) {
            throw new ValidationException("Master BL number is missing for the selected FF job row");
        }

        callProcGlReverseShtoffPosting(
                groupPoid,
                companyPoid,
                UserContext.getUserPoid(),
                ffRow.getMasterBlNo().trim(),
                String.valueOf(rnumid));

        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(),
                String.format("FF Jobs row %s reversed via PROC_GL_REVERSE_SHTOFF_POSTING for BL %s, PJ %s",
                        rnumid, ffRow.getMasterBlNo(), ffRow.getFfPj()));

        log.info("Successfully reversed FF purchase journal {} for manifest transactionPoid: {}", ffRow.getFfPj(), transactionPoid);
    }

    private ShipBlToFfDto enrichFfJobRow(ShipBlToFfDto row) {
        row.setDeleteAllowed(hasText(row.getFfPj()));
        return row;
    }

    private void validateActiveExportManifestBl(Long transactionPoid, Long companyPoid) {
        Long groupPoid = UserContext.getGroupPoid();
        repository.findActiveExportBlByTransactionPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Export Manifest BL", "transactionPoid", transactionPoid.toString()));
    }

    private void callProcGlReverseShtoffPosting(Long groupPoid, Long companyPoid, Long userPoid,
                                                 String masterBlNo, String rnumid) {
        try {
            String sql = "{call PROC_GL_REVERSE_SHTOFF_POSTING(?, ?, ?, ?, ?, ?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, userPoid);
                cs.setString(4, masterBlNo);
                cs.setString(5, rnumid);
                cs.registerOutParameter(6, Types.VARCHAR);
                cs.execute();
                String status = cs.getString(6);
                if (status == null || status.isBlank()) {
                    throw new ValidationException("No status returned from PROC_GL_REVERSE_SHTOFF_POSTING");
                }
                if (status.toUpperCase().contains("ERROR")) {
                    throw new ValidationException(status.trim());
                }
                return null;
            });
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error calling PROC_GL_REVERSE_SHTOFF_POSTING for masterBlNo: {}, rnumid: {}",
                    masterBlNo, rnumid, ex);
            throw new ValidationException("Error reversing FF purchase journal: " + ex.getMessage());
        }
    }
    
    @Override
	public byte[] generateBlPrint(Long transactionPoid, GenerateBlPrintRequest request, String docId) throws Exception {
		return manifestUpdateService.generateBlPrint(transactionPoid, request, docId);
	}

	@Override
	public byte[] generateManifest(Long transactionPoid, GenerateManifestRequest request, String docId)
			throws Exception {
		return manifestUpdateService.generateManifest(transactionPoid, request, docId);
	}

	@Override
	public byte[] generateDetentionStorage(Long transactionPoid, String docId) throws Exception {
		return manifestUpdateService.generateDetentionStorage(transactionPoid, docId);
	}
	
	@Override
	public byte[] exportDraftPrint(Long transactionPoid) throws Exception {
		String docId=UserContext.getDocumentId();
		Map<String, Object> params = printService.buildBaseParams(transactionPoid, docId);
	    JasperReport mainReport = printService.load("Shipping/SH/SH_INVOICE_DRAFT_EXP.jrxml");
		params.put("DOC_BL_POID", "0000");
//		params.put("DETAIL_SHOW_FLAG", "Y");
		params.put("DOC_KEY_POID_CNT", transactionPoid.toString());
	    return printService.fillReportToPdf(mainReport, params, dataSource);
	}

    @Override
    @Transactional(readOnly = true)
    public ChargeDefaultsResponseDto getChargeDefaults(ChargeDefaultsRequestDto request) {
        if (request.getChargePoid() == null) {
            throw new ValidationException("Charge POID is required");
        }

        Long companyPoid = UserContext.getCompanyPoid();
        Object[] taxData = procRepository.getTaxRate(
                request.getChargePoid(), companyPoid, request.getTransactionDate());
        Long taxPoid = (Long) taxData[0];
        BigDecimal taxPercentage = (BigDecimal) taxData[1];

        ChargeDefaultsResponseDto.ChargeDefaultsResponseDtoBuilder builder = ChargeDefaultsResponseDto.builder()
                .taxPoid(taxPoid)
                .taxPercentage(taxPercentage != null ? taxPercentage : BigDecimal.ZERO);

        if (taxPoid != null) {
            try {
                builder.taxDet(lovService.getLovItemByPoid(taxPoid, "TAX_MASTER",
                        UserContext.getGroupPoid(), companyPoid, UserContext.getUserPoid()));
            } catch (Exception e) {
                log.warn("Failed to fetch TAX_MASTER LOV for taxPoid: {}", taxPoid, e);
            }
        }

        return builder.build();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> loadLocalCharges(Long transactionPoid) {
        log.info("Loading port local charges for Export Manifest BL: {}", transactionPoid);
        validateActiveExportManifestBl(transactionPoid, UserContext.getCompanyPoid());

        try {
            procRepository.processExportLocalCharge(
                    UserContext.getGroupPoid(),
                    UserContext.getCompanyPoid(),
                    transactionPoid);
        } catch (Exception e) {
            log.error("Error loading local charges for Export Manifest BL: {}", transactionPoid, e);
            throw new ValidationException("Failed to load local charges: " + e.getMessage());
        }

        return fetchChargeDetailsMap(transactionPoid);
    }

    @Override
    public Map<String, Object> loadCustomerLocalCharges(Long transactionPoid) {
        log.info("Loading customer local charges for Export Manifest BL: {}", transactionPoid);
        validateActiveExportManifestBl(transactionPoid, UserContext.getCompanyPoid());

        try {
            procRepository.loadCustomerAutoCharges(
                    UserContext.getGroupPoid(),
                    UserContext.getCompanyPoid(),
                    transactionPoid,
                    0L,
                    UPDATE_TYPE_AUTO_CUSTOMER_CHARGE,
                    UserContext.getUserPoid());
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_BL_CUSTOMER_AUTO for transactionPoid: {}", transactionPoid, e);
            throw new ValidationException("Failed to load customer local charges: " + e.getMessage());
        }

        return fetchChargeDetailsMap(transactionPoid);
    }

    @Override
    @Transactional
    public Map<String, Object> loadDamageClause(Long transactionPoid) {
        log.info("Loading damage clause for Export Manifest BL: {}", transactionPoid);
        validateActiveExportManifestBl(transactionPoid, UserContext.getCompanyPoid());

        List<String> damageClauses;
        try {
            damageClauses = procRepository.loadDamageClauseLines();
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_BL_DAMAGE_LOAD for transactionPoid: {}", transactionPoid, e);
            throw new ValidationException("Some error in PROC_SHIP_BL_DAMAGE_LOAD: " + e.getMessage());
        }

        if (damageClauses.isEmpty()) {
            throw new ValidationException("No Damage Clause Found...");
        }

        String descriptionType = resolveDamageClauseDescriptionType(transactionPoid);
        long nextDetRowId = cargoDtlRepository.findById_TransactionPoid(transactionPoid).stream()
                .map(row -> row.getId().getDetRowId())
                .max(Long::compareTo)
                .orElse(0L);

        for (String clause : damageClauses) {
            nextDetRowId++;
            createCargoDetail(
                    CargoDescriptionRequestDto.builder()
                            .detRowId(nextDetRowId)
                            .descriptionType(descriptionType)
                            .cargoDescription(clause)
                            .recordOrder(nextDetRowId)
                            .build(),
                    transactionPoid);
        }

        List<CargoDescriptionRequestDto> cargoDescriptions = cargoDtlRepository
                .findById_TransactionPoid(transactionPoid)
                .stream()
                .map(entity -> CargoDescriptionRequestDto.builder()
                        .detRowId(entity.getId().getDetRowId())
                        .descriptionType(entity.getId().getDescriptionType())
                        .cargoDescription(entity.getCargoDescription())
                        .recordOrder(entity.getRecordOrder())
                        .build())
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("damageClauses", damageClauses);
        response.put("cargoDescriptions", cargoDescriptions);
        return response;
    }

    private String resolveDamageClauseDescriptionType(Long transactionPoid) {
        return cargoDtlRepository.findById_TransactionPoid(transactionPoid).stream()
                .map(row -> row.getId().getDescriptionType())
                .filter(type -> type != null
                        && !"MARK".equalsIgnoreCase(type)
                        && !"MARKS".equalsIgnoreCase(type))
                .findFirst()
                .orElse(CARGO_DESCRIPTION_TYPE_DESC);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSelectionRowDto> listBookingSelection(
            Long issueVesselVoyagePoid,
            String bookingMateVoyageNo,
            Long linePoid,
            String containerNo,
            String bookingNo) {
        log.info(
                "List pending mate for export BL, issue voyage: {}, mate voyage no: {}",
                issueVesselVoyagePoid,
                bookingMateVoyageNo);

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        if (issueVesselVoyagePoid == null) {
            throw new ValidationException("Issue vessel voyage transaction POID is required");
        }

        ShipVoyageHdrEntity voyage = shipVoyageHdrRepository
                .findByTransactionPoidAndGroupPoid(issueVesselVoyagePoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Voyage", "transactionPoid",
                        issueVesselVoyagePoid.toString()));
        if (!companyPoid.equals(voyage.getCompanyPoid())) {
            throw new ValidationException("Voyage does not belong to current company");
        }

        Long effectiveLinePoid = linePoid;
        if (effectiveLinePoid != null && effectiveLinePoid == 0L) {
            effectiveLinePoid = null;
        }

        List<BookingSelectionRowDto> rows = bookingSelectionRepository.findPendingMateToBl(
                groupPoid,
                companyPoid,
                issueVesselVoyagePoid,
                bookingMateVoyageNo,
                effectiveLinePoid,
                containerNo,
                bookingNo);
        log.info(
                "Booking selection issueVoyage={} returned {} row(s) (mateVoyageNo={}, linePoid={})",
                issueVesselVoyagePoid,
                rows.size(),
                bookingMateVoyageNo,
                effectiveLinePoid);
        return rows;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> loadBooking(Long voyageTransactionPoid, LoadBookingRequest request) {
        log.info("Load booking to export BL, voyage: {}", voyageTransactionPoid);

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();

        Long resolvedVoyagePoid = request.getVoyageTransactionPoid() != null
                ? request.getVoyageTransactionPoid()
                : voyageTransactionPoid;
        if (resolvedVoyagePoid == null) {
            throw new ValidationException("Voyage transaction POID is required");
        }

        List<BookingSelectionItemDto> selections = resolveBookingSelections(request, groupPoid, companyPoid);
        long selectedCount = selections.stream()
                .filter(s -> "Y".equalsIgnoreCase(s.getIsSelected()))
                .count();
        if (selectedCount == 0) {
            throw new ValidationException("At least one booking container must be selected");
        }

        String loginUser = UserContext.getUserId() != null ? UserContext.getUserId() : String.valueOf(userPoid);

        String funcResult = loadBookingRepository.stageAndLoadBookingToBl(
                groupPoid,
                companyPoid,
                selections,
                loginUser,
                resolvedVoyagePoid);
        if (funcResult == null || funcResult.toUpperCase(Locale.ROOT).startsWith("ERROR")) {
            throw new ValidationException(
                    funcResult != null ? funcResult : "FUNC_LOAD_BOOKING_TO_BL failed with no message");
        }

        long newBlPoid;
        try {
            newBlPoid = Long.parseLong(funcResult.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException("FUNC_LOAD_BOOKING_TO_BL returned invalid transaction POID: " + funcResult);
        }
        if (newBlPoid <= 0) {
            throw new ValidationException(
                    "FUNC_LOAD_BOOKING_TO_BL did not create a BL (returned "
                            + funcResult
                            + "). Verify GLOBAL_TEMP_BOOKING_SELECTED rows and QA_DB_USER.FUNC_LOAD_BOOKING_TO_BL.");
        }

        try {
            procRepository.processAfterSave(
                    groupPoid,
                    companyPoid,
                    newBlPoid,
                    0L,
                    UPDATE_TYPE_AUTOSUM_WEIGHT_PACK,
                    userPoid);
        } catch (Exception e) {
            log.error("PROC_SHIP_BL_PAGE_SAVE_AFTER failed after load booking for BL {}", newBlPoid, e);
            throw new ValidationException(
                    "BL created (POID " + newBlPoid + ") but autosum/auto-charges failed: " + e.getMessage());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("voyageTransactionPoid", resolvedVoyagePoid);
        response.put("transactionPoid", newBlPoid);
        response.put("message", "Booking data loaded, export BL created, and totals/auto-charges processed");

        if (request.getBookingMateVoyageNo() != null && !request.getBookingMateVoyageNo().isBlank()) {
            logBookingMateVoyageFilter(
                    EXPORT_MANIFEST_DOCUMENT_ID,
                    String.valueOf(newBlPoid),
                    "",
                    request.getBookingMateVoyageNo().trim());
        }

        return response;
    }

    private List<BookingSelectionItemDto> resolveBookingSelections(
            LoadBookingRequest request, Long groupPoid, Long companyPoid) {
        if (request.getSelections() != null && !request.getSelections().isEmpty()) {
            return request.getSelections();
        }
        if (request.getSelectedBookingIds() == null || request.getSelectedBookingIds().isEmpty()) {
            throw new ValidationException("Either selections or selectedBookingIds is required");
        }

        List<BookingSelectionItemDto> rows = new ArrayList<>();
        for (Long mateTransactionPoid : request.getSelectedBookingIds()) {
            ShipMateHdr hdr = shipMateHdrRepository.findByTransactionPoid(mateTransactionPoid)
                    .orElseThrow(() -> new ValidationException(
                            "Booking not found for transaction POID: " + mateTransactionPoid));
            if (!groupPoid.equals(hdr.getGroupPoid()) || !companyPoid.equals(hdr.getCompanyPoid())) {
                throw new ValidationException("Booking " + mateTransactionPoid + " does not belong to current company");
            }
            if ("Y".equalsIgnoreCase(hdr.getDeleted())) {
                throw new ValidationException("Booking " + mateTransactionPoid + " is deleted");
            }

            List<ShipMateContainerDtl> containers =
                    shipMateContainerDtlRepository.findByTransactionPoidOrderByDetRowId(mateTransactionPoid);
            if (containers.isEmpty()) {
                throw new ValidationException("Booking " + mateTransactionPoid + " has no containers");
            }
            for (ShipMateContainerDtl container : containers) {
                BookingSelectionItemDto item = new BookingSelectionItemDto();
                item.setTransactionPoid(mateTransactionPoid);
                item.setVoyageNo(hdr.getVoyageNo());
                item.setContainerNo(container.getContainerNo());
                item.setBookingIssueNo(hdr.getBookingIssueNo());
                item.setIsSelected("Y");
                rows.add(item);
            }
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public SelectForInvoiceResponseDto selectForInvoice(Long transactionPoid) {
        log.info("Select for invoice (navigation) — export BL: {}", transactionPoid);
        validateActiveExportManifestBl(transactionPoid, UserContext.getCompanyPoid());
        String approvalStatus = resolveExportBlFinalApprovalStatus(transactionPoid);

        Long existingInvoiceTransactionPoid = findExistingInvoiceTransactionPoid(transactionPoid);

        return SelectForInvoiceResponseDto.builder()
                .blPoid(transactionPoid)
                .documentId(SALES_INVOICE_DOCUMENT_ID)
                .documentName(SALES_INVOICE_DOCUMENT_NAME)
                .approvalStatus(approvalStatus)
                .existingInvoiceTransactionPoid(existingInvoiceTransactionPoid)
                .invoiceTransactionPoid(existingInvoiceTransactionPoid)
                .targetApiPath("/v1/sales-invoice-shipping")
                .message(SALES_INVOICE_DOCUMENT_NAME + " is open successfully with the respective BL.")
                .build();
    }

    /**
     * Legacy {@code getApprovalStatus("100-104", …)} must be {@code FINAL_APPROVAL_COMPLETED}; falls back to
     * {@code VOYAGEWISEBILLS_APPROVED} when approval service returns empty.
     */
    private String resolveExportBlFinalApprovalStatus(Long blPoid) {
        try {
            String status = approvalService.getApprovalStatus(EXPORT_MANIFEST_DOCUMENT_ID, blPoid);
            if (hasText(status)) {
                String normalized = status.trim();
                if ("FINAL_APPROVAL_COMPLETED".equalsIgnoreCase(normalized)) {
                    return normalized;
                }
                throw new ValidationException("Approval pending to Proceed...");
            }
        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Approval status lookup failed for export BL {}: {}", blPoid, ex.getMessage());
        }
        validateBlApproved(blPoid);
        return "FINAL_APPROVAL_COMPLETED";
    }

    private void validateBlApproved(Long blPoid) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM VOYAGEWISEBILLS_APPROVED WHERE BL_POID = ?",
                Integer.class,
                blPoid);
        if (count == null || count == 0) {
            throw new ValidationException("BL not Approved");
        }
    }

    private Long findExistingInvoiceTransactionPoid(Long blPoid) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT TRANSACTION_POID FROM AR_SH_SALES_INVOICE_HDR "
                            + "WHERE BL_POID = ? AND (DELETED IS NULL OR DELETED = 'N') "
                            + "ORDER BY TRANSACTION_POID DESC FETCH FIRST 1 ROW ONLY",
                    Long.class,
                    blPoid);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Map<String, Object> fetchChargeDetailsMap(Long transactionPoid) {
        List<ChargeRequestDto> chargeDetails = chargesDtlRepository.findById_TransactionPoid(transactionPoid)
                .stream()
                .map(entity -> ChargeRequestDto.builder()
                        .detRowId(entity.getId().getDetRowId())
                        .chargePoid(entity.getChargePoid())
                        .currencyExchange(entity.getCurrencyExchange())
                        .quantity(entity.getQuantity())
                        .buyPercharge(entity.getBuyPercharge())
                        .perQuantityAmount(entity.getPerQuantityAmount())
                        .paidAtPortPoid(entity.getPaidAtPortPoid())
                        .chargeType(entity.getChargeType())
                        .currencyCode(entity.getCurrencyCode())
                        .freightType(entity.getFreightType())
                        .ediChargeCode(entity.getEdiChargeCode())
                        .arShReceiptTransactionPoid(entity.getArShReceiptTransactionPoid())
                        .chargeBasisOn(entity.getChargeBasisOn())
                        .printGroup(entity.getPrintGroup())
                        .receiptInvoicePoid(entity.getReceiptInvoicePoid())
                        .docRefLinkNo(entity.getDocRefLinkNo())
                        .reprintDetRowId(entity.getReprintDetRowId())
                        .reprintTransactionPoid(entity.getReprintTransactionPoid())
                        .invoiceType(entity.getInvoiceType())
                        .autoCanInvoiceNo(entity.getAutoCanInvoiceNo())
                        .chargeDescription(entity.getChargeDescription())
                        .taxPoid(entity.getTaxPoid())
                        .taxPercentage(entity.getTaxPercentage())
                        .taxAmount(entity.getTaxAmount())
                        .cnRefDocId(entity.getCnRefDocId())
                        .cnRefDocPoid(entity.getCnRefDocPoid())
                        .cnRefDetRowId(entity.getCnRefDetRowId())
                        .cnIssueInvoice(entity.getCnIssueInvoice())
                        .selectRow(entity.getSelectRow())
                        .build())
                .toList();
        enrichChargeLovData(chargeDetails);

        BigDecimal totalBuyAmount = BigDecimal.ZERO;
        BigDecimal totalSaleAmount = BigDecimal.ZERO;
        for (ChargeRequestDto charge : chargeDetails) {
            if (charge.getQuantity() != null && charge.getBuyPercharge() != null) {
                totalBuyAmount = totalBuyAmount.add(charge.getQuantity().multiply(charge.getBuyPercharge()));
            }
            if (charge.getQuantity() != null && charge.getPerQuantityAmount() != null) {
                totalSaleAmount = totalSaleAmount.add(charge.getQuantity().multiply(charge.getPerQuantityAmount()));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("chargeDetails", chargeDetails);
        response.put("totalBuyAmount", totalBuyAmount);
        response.put("totalSaleAmount", totalSaleAmount);
        return response;
    }

    // ==================== Helper Methods ====================

    private void validateMandatoryFields(ExportManifestBlCreateDto dto) {
        if (dto.getVoyageTransactionPoid() == null) {
            throw new ValidationException("Voyage transaction POID is required");
        }
        if (dto.getBlNumber() == null || dto.getBlNumber().trim().isEmpty()) {
            throw new ValidationException("BL number is required");
        }
    }

    private String generateDocRef(Long companyPoid) {
        try {
            String companyCode = jdbcTemplate.queryForObject(
                    "SELECT GET_COMPANY_CODE(?) FROM DUAL",
                    String.class,
                    companyPoid
            );

            String docRef = jdbcTemplate.queryForObject(
                    "SELECT RTN_GLOBAL_SEQ_NO('SHIPPING_MANIFEST', ?, NULL) FROM DUAL",
                    String.class,
                    companyCode
            );

            return docRef;
        } catch (Exception e) {
            log.error("Error generating DOC_REF for companyPoid: {}", companyPoid, e);
            throw new ValidationException("Failed to generate document reference");
        }
    }

    private void formatEdiFields(ExportManifestBlHdr entity) {
        // For EXPORT BL_TYPE, EDI fields should be UPPERCASE
        if (entity.getShipperEdiName() != null) {
            entity.setShipperEdiName(entity.getShipperEdiName().toUpperCase()
                    .replace("\r", "").replace("\n", ""));
        }
        if (entity.getShipperEdiAddress() != null) {
            entity.setShipperEdiAddress(entity.getShipperEdiAddress().toUpperCase()
                    .replace("\r", "").replace("\n", ""));
        }
        if (entity.getConsigneeEdiName() != null) {
            entity.setConsigneeEdiName(entity.getConsigneeEdiName().toUpperCase()
                    .replace("\r", "").replace("\n", ""));
        }
        if (entity.getConsigneeEdiAddress() != null) {
            entity.setConsigneeEdiAddress(entity.getConsigneeEdiAddress().toUpperCase()
                    .replace("\r", "").replace("\n", ""));
        }
        if (entity.getNotify1EdiName() != null) {
            entity.setNotify1EdiName(entity.getNotify1EdiName().toUpperCase()
                    .replace("\r", "").replace("\n", ""));
        }
        if (entity.getNotify1EdiAddress() != null) {
            entity.setNotify1EdiAddress(entity.getNotify1EdiAddress().toUpperCase()
                    .replace("\r", "").replace("\n", ""));
        }
        if (entity.getNotify2EdiName() != null) {
            entity.setNotify2EdiName(entity.getNotify2EdiName().toUpperCase()
                    .replace("\r", "").replace("\n", ""));
        }
        if (entity.getNotify2EdiAddress() != null) {
            entity.setNotify2EdiAddress(entity.getNotify2EdiAddress().toUpperCase()
                    .replace("\r", "").replace("\n", ""));
        }
        if (entity.getNotify3EdiName() != null) {
            entity.setNotify3EdiName(entity.getNotify3EdiName().toUpperCase()
                    .replace("\r", "").replace("\n", ""));
        }
        if (entity.getNotify3EdiAddress() != null) {
            entity.setNotify3EdiAddress(entity.getNotify3EdiAddress().toUpperCase()
                    .replace("\r", "").replace("\n", ""));
        }
    }

    private boolean hasAnyEdiChange(ExportManifestBlUpdateDto dto) {
        return dto.getShipperEdiName() != null || dto.getShipperEdiAddress() != null ||
                dto.getConsigneeEdiName() != null || dto.getConsigneeEdiAddress() != null ||
                dto.getNotify1EdiName() != null || dto.getNotify1EdiAddress() != null ||
                dto.getNotify2EdiName() != null || dto.getNotify2EdiAddress() != null ||
                dto.getNotify3EdiName() != null || dto.getNotify3EdiAddress() != null;
    }

    private boolean hasDetailUpdates(ExportManifestBlUpdateDto dto) {
        return (dto.getGeneralCargoDetails() != null && !dto.getGeneralCargoDetails().isEmpty()) ||
                (dto.getCargoDescriptions() != null && !dto.getCargoDescriptions().isEmpty()) ||
                (dto.getContainers() != null && !dto.getContainers().isEmpty()) ||
                (dto.getChargeDetails() != null && !dto.getChargeDetails().isEmpty());
    }

    private void applyDetailActions(ExportManifestBlUpdateDto dto, Long transactionPoid) {
        if (dto.getGeneralCargoDetails() != null) {
            applyGeneralDetailActions(dto.getGeneralCargoDetails(), transactionPoid);
        }
        if (dto.getCargoDescriptions() != null) {
            applyCargoDetailActions(dto.getCargoDescriptions(), transactionPoid);
        }
        if (dto.getContainers() != null) {
            applyContainerDetailActions(dto.getContainers(), transactionPoid);
        }
        if (dto.getChargeDetails() != null) {
            applyChargeDetailActions(dto.getChargeDetails(), transactionPoid);
        }
    }

    private void applyGeneralDetailActions(List<GeneralCargoRequestDto> details, Long transactionPoid) {
        for (GeneralCargoRequestDto detail : details) {
            String action = normalizeActionType(detail.getActionType());
            switch (action) {
                case "iscreated" -> createGeneralDetail(detail, transactionPoid);
                case "isupdated" -> updateGeneralDetail(detail, transactionPoid);
                case "isdeleted" -> deleteGeneralDetail(detail, transactionPoid);
                case "nochange", "" -> {
                    // no-op
                }
                default -> log.warn("Unknown actionType '{}' for general cargo detail. Skipping.", detail.getActionType());
            }
        }
    }

    private void applyCargoDetailActions(List<CargoDescriptionRequestDto> details, Long transactionPoid) {
        for (CargoDescriptionRequestDto detail : details) {
            String action = normalizeActionType(detail.getActionType());
            switch (action) {
                case "iscreated" -> createCargoDetail(detail, transactionPoid);
                case "isupdated" -> updateCargoDetail(detail, transactionPoid);
                case "isdeleted" -> deleteCargoDetail(detail, transactionPoid);
                case "nochange", "" -> {
                    // no-op
                }
                default -> log.warn("Unknown actionType '{}' for cargo description detail. Skipping.", detail.getActionType());
            }
        }
    }

    private void applyContainerDetailActions(List<ContainerRequestDto> details, Long transactionPoid) {
        for (ContainerRequestDto detail : details) {
            String action = normalizeActionType(detail.getActionType());
            switch (action) {
                case "iscreated" -> createContainerDetail(detail, transactionPoid);
                case "isupdated" -> updateContainerDetail(detail, transactionPoid);
                case "isdeleted" -> deleteContainerDetail(detail, transactionPoid);
                case "nochange", "" -> {
                    // no-op
                }
                default -> log.warn("Unknown actionType '{}' for container detail. Skipping.", detail.getActionType());
            }
        }
    }

    private void applyChargeDetailActions(List<ChargeRequestDto> details, Long transactionPoid) {
        for (ChargeRequestDto detail : details) {
            String action = normalizeActionType(detail.getActionType());
            switch (action) {
                case "iscreated" -> createChargeDetail(detail, transactionPoid);
                case "isupdated" -> updateChargeDetail(detail, transactionPoid);
                case "isdeleted" -> deleteChargeDetail(detail, transactionPoid);
                case "nochange", "" -> {
                    // no-op
                }
                default -> log.warn("Unknown actionType '{}' for charge detail. Skipping.", detail.getActionType());
            }
        }
    }

    private void createGeneralDetail(GeneralCargoRequestDto detail, Long transactionPoid) {
        Long detRowId = detail.getDetRowId();
        ExportManifestBlGeneralDtl entity = ExportManifestBlGeneralDtl.builder()
                .id(new ShipBlManifestDtlId(transactionPoid, detRowId))
                .comodityPoid(detail.getComodityPoid())
                .cargoDescription(detail.getCargoDescription())
                .quantity(detail.getQuantity())
                .grsVolume(detail.getGrsVolume())
                .grsWeight(detail.getGrsWeight())
                .netVolume(detail.getNetVolume())
                .netWeight(detail.getNetWeight())
                .tareWeight(detail.getTareWeight())
                .noOfPacks(detail.getNoOfPacks())
                .packUnit(detail.getPackUnit())
                .destinationPortPoid(detail.getDestinationPortPoid())
                .build();
        generalDtlRepository.save(entity);

        
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        String logDetail = "Row Created on Export Manifest BL General Cargo Detail with detRowId: " + detRowId;
        loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
    }

    private void updateGeneralDetail(GeneralCargoRequestDto detail, Long transactionPoid) {
        Long detRowId = detail.getDetRowId();
        ShipBlManifestDtlId id = new ShipBlManifestDtlId(transactionPoid, detRowId);
        ExportManifestBlGeneralDtl entity = generalDtlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("General Cargo Detail", "detRowId", detRowId.toString()));

        ExportManifestBlGeneralDtl oldEntity = new ExportManifestBlGeneralDtl();
        BeanUtils.copyProperties(entity, oldEntity);

        entity.setComodityPoid(detail.getComodityPoid());
        entity.setCargoDescription(detail.getCargoDescription());
        entity.setQuantity(detail.getQuantity());
        entity.setGrsVolume(detail.getGrsVolume());
        entity.setGrsWeight(detail.getGrsWeight());
        entity.setNetVolume(detail.getNetVolume());
        entity.setNetWeight(detail.getNetWeight());
        entity.setTareWeight(detail.getTareWeight());
        entity.setNoOfPacks(detail.getNoOfPacks());
        entity.setPackUnit(detail.getPackUnit());
        entity.setDestinationPortPoid(detail.getDestinationPortPoid());
        generalDtlRepository.save(entity);

       
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        String logDetail = "KeyId = TRANSACTION_POID:" + transactionPoid + " DET_ROW_ID:" + detRowId;
        loggingService.createLogBatch(
                List.of(new LogRequestDto<>(
                        copyGeneralForLog(oldEntity),
                        copyGeneralForLog(entity),
                        ShipBlManifestGeneralDtl.class,
                        docId,
                        docKeyPoid,
                        logDetail
                ))
        );
    }

    private void deleteGeneralDetail(GeneralCargoRequestDto detail, Long transactionPoid) {
        Long detRowId = detail.getDetRowId();
        generalDtlRepository.deleteById(new ShipBlManifestDtlId(transactionPoid, detRowId));

        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        String logDetail = "Row Deleted on Export Manifest BL General Cargo Detail with detRowId: " + detRowId;
        loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
    }

    private void createCargoDetail(CargoDescriptionRequestDto detail, Long transactionPoid) {
        Long detRowId = detail.getDetRowId();
        String descriptionType = detail.getDescriptionType();
        ExportManifestBlCargoDtl entity = ExportManifestBlCargoDtl.builder()
                .id(new ShipBlManifestCargoDtlId(transactionPoid, detRowId, descriptionType))
                .cargoDescription(detail.getCargoDescription())
                .recordOrder(detail.getRecordOrder())
                .build();
        cargoDtlRepository.save(entity);

        
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        String logDetail = "Row Created on Export Manifest BL Cargo Detail with detRowId: " + detRowId;
        loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
    }

    private void updateCargoDetail(CargoDescriptionRequestDto detail, Long transactionPoid) {
        Long detRowId = detail.getDetRowId();
        String descriptionType = detail.getDescriptionType();
        ShipBlManifestCargoDtlId id = new ShipBlManifestCargoDtlId(transactionPoid, detRowId, descriptionType);
        ExportManifestBlCargoDtl entity = cargoDtlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo Description Detail", "detRowId", detRowId.toString()));

        ExportManifestBlCargoDtl oldEntity = new ExportManifestBlCargoDtl();
        BeanUtils.copyProperties(entity, oldEntity);

        entity.setCargoDescription(detail.getCargoDescription());
        entity.setRecordOrder(detail.getRecordOrder());
        cargoDtlRepository.save(entity);

        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        String logDetail = "KeyId = TRANSACTION_POID:" + transactionPoid + " DET_ROW_ID:" + detRowId;
        loggingService.createLogBatch(
                List.of(new LogRequestDto<>(
                        copyCargoForLog(oldEntity),
                        copyCargoForLog(entity),
                        ShipBlManifestCargoDtl.class,
                        docId,
                        docKeyPoid,
                        logDetail
                ))
        );
    }

    private String joinCargoTextForAudit(Long transactionPoid, String... descriptionTypes) {
        return cargoDtlRepository.findById_TransactionPoid(transactionPoid).stream()
                .filter(row -> {
                    String type = row.getId().getDescriptionType();
                    if (type == null) {
                        return false;
                    }
                    for (String candidate : descriptionTypes) {
                        if (candidate.equalsIgnoreCase(type)) {
                            return true;
                        }
                    }
                    return false;
                })
                .sorted(java.util.Comparator.comparing(row -> row.getId().getDetRowId()))
                .map(ExportManifestBlCargoDtl::getCargoDescription)
                .filter(text -> text != null && !text.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private void deleteCargoDetail(CargoDescriptionRequestDto detail, Long transactionPoid) {
        Long detRowId = detail.getDetRowId();
        String descriptionType = detail.getDescriptionType();
        cargoDtlRepository.deleteById(new ShipBlManifestCargoDtlId(transactionPoid, detRowId, descriptionType));

       
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        String logDetail = "Row Deleted on Export Manifest BL Cargo Detail with detRowId: " + detRowId;
        loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
    }

    private void createContainerDetail(ContainerRequestDto detail, Long transactionPoid) {
        Long detRowId = detail.getDetRowId();
        String containerNo = normalizeContainerNo(detail.getContainerNo());
        if (containerNo != null) {
            containerDtlRepository
                    .findById_TransactionPoidAndContainerNo(transactionPoid, containerNo)
                    .ifPresent(existing -> {
                        Long existingDetRowId = existing.getId() != null ? existing.getId().getDetRowId() : null;
                        if (existingDetRowId == null || !existingDetRowId.equals(detRowId)) {
                            throw new ValidationException("Container number already exists for this BL");
                        }
                    });
        }

        ExportManifestBlContainerDtl entity = ExportManifestBlContainerDtl.builder()
                .id(new ShipBlManifestDtlId(transactionPoid, detRowId))
                .build();
        applyContainerFields(entity, detail);
        containerDtlRepository.save(entity);

       
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        String logDetail = "Row Created on Export Manifest BL Container Detail with detRowId: " + detRowId;
        loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
    }

    private void updateContainerDetail(ContainerRequestDto detail, Long transactionPoid) {
        Long detRowId = detail.getDetRowId();
        ShipBlManifestDtlId id = new ShipBlManifestDtlId(transactionPoid, detRowId);
        ExportManifestBlContainerDtl entity = containerDtlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Container Detail", "detRowId", detRowId.toString()));

        ExportManifestBlContainerDtl oldEntity = new ExportManifestBlContainerDtl();
        BeanUtils.copyProperties(entity, oldEntity);
        String containerNo = normalizeContainerNo(detail.getContainerNo());
        if (containerNo != null) {
            containerDtlRepository
                    .findById_TransactionPoidAndContainerNo(transactionPoid, containerNo)
                    .ifPresent(existing -> {
                        Long existingDetRowId = existing.getId() != null ? existing.getId().getDetRowId() : null;
                        if (existingDetRowId == null || !existingDetRowId.equals(detRowId)) {
                            throw new ValidationException("Container number already exists for this BL");
                        }
                    });
        }
        applyContainerFields(entity, detail);
        containerDtlRepository.save(entity);

       
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        String logDetail = "KeyId = TRANSACTION_POID:" + transactionPoid + " DET_ROW_ID:" + detRowId;
        loggingService.createLogBatch(
                List.of(new LogRequestDto<>(
                        copyContainerForLog(oldEntity),
                        copyContainerForLog(entity),
                        ShipBlManifestContainerDtl.class,
                        docId,
                        docKeyPoid,
                        logDetail
                ))
        );
    }

    private void deleteContainerDetail(ContainerRequestDto detail, Long transactionPoid) {
        Long detRowId = detail.getDetRowId();
        containerDtlRepository.deleteById(new ShipBlManifestDtlId(transactionPoid, detRowId));

        
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        String logDetail = "Row Deleted on Export Manifest BL Container Detail with detRowId: " + detRowId;
        loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
    }

    private void applyContainerFields(ExportManifestBlContainerDtl entity, ContainerRequestDto detail) {
        entity.setMateTransactionPoid(detail.getMateTransactionPoid());
        entity.setContainerNo(normalizeContainerNo(detail.getContainerNo()));
        entity.setEquipmentShipperOwn(detail.getEquipmentShipperOwn());
        entity.setCargoDescription(detail.getCargoDescription());
        entity.setEquipmentSealNo(detail.getEquipmentSealNo());
        entity.setEquipmentIsoType(detail.getEquipmentIsoType());
        entity.setEquipmentType(detail.getEquipmentType());
        entity.setEquipmentSize(detail.getEquipmentSize());
        entity.setQuantity(detail.getQuantity());
        entity.setGrsVolume(detail.getGrsVolume());
        entity.setGrsWeight(detail.getGrsWeight());
        entity.setNetVolume(detail.getNetVolume());
        entity.setNetWeight(detail.getNetWeight());
        entity.setTareWeight(detail.getTareWeight());
        entity.setNoOfPacks(detail.getNoOfPacks());
        entity.setPackUnit(detail.getPackUnit());
        entity.setComodityPoid(detail.getComodityPoid());
        entity.setDestinationPortPoid(detail.getDestinationPortPoid());
        entity.setImo(detail.getImo());
        entity.setOogL(detail.getOogL());
        entity.setOogB(detail.getOogB());
        entity.setOogH(detail.getOogH());
        entity.setRefferTemp(detail.getRefferTemp());
        entity.setRefferHum(detail.getRefferHum());
        entity.setRefferVent(detail.getRefferVent());
        entity.setIssueToConsignee(detail.getIssueToConsignee());
        entity.setReturnFromConsignee(detail.getReturnFromConsignee());
        entity.setIsImco(detail.getIsImco());
        entity.setIsOog(detail.getIsOog());
        entity.setIsRefer(detail.getIsRefer());
        entity.setReferType(detail.getReferType());
        entity.setImcoClassType(detail.getImcoClassType());
        entity.setGuaranteeFlag(detail.getGuaranteeFlag());
        entity.setGuaranteedBy(detail.getGuaranteedBy());
        entity.setExtraFreeDays(detail.getExtraFreeDays());
        entity.setExtraFreeDaysPrnpls(detail.getExtraFreeDaysPrnpls());
        entity.setOogLW(detail.getOogLW());
        entity.setOogRW(detail.getOogRW());
        entity.setOogF(detail.getOogF());
        entity.setOogA(detail.getOogA());
        entity.setOogType(detail.getOogType());
        entity.setImcoClassActual(detail.getImcoClassActual());
        entity.setDisplayCollectedDate(detail.getDisplayCollectedDate());
        entity.setTotalDaysCollected(detail.getTotalDaysCollected());
        entity.setTotalAmountCollected(detail.getTotalAmountCollected());
        entity.setPrintReturnFormDefault(detail.getPrintReturnFormDefault());
        entity.setPrintDeliveryFormDefault(detail.getPrintDeliveryFormDefault());
        entity.setDemDttPbleTrnsfd(detail.getDemDttPbleTrnsfd());
        entity.setHsCode(detail.getHsCode());
        entity.setHsDescription(detail.getHsDescription());
        entity.setAmountPerDayAfterFree(detail.getAmountPerDayAfterFree());
        entity.setActualDischargeDate(detail.getActualDischargeDate());
    }

    private void createChargeDetail(ChargeRequestDto detail, Long transactionPoid) {
        blManifestValidationService.validateChargeTypeMandatory(detail.getChargeType());
        blManifestValidationService.validateFreightTypeMandatory(detail.getFreightType());
        Long detRowId = detail.getDetRowId();
        ExportManifestBlChargesDtl entity = ExportManifestBlChargesDtl.builder()
                .id(new ShipBlManifestDtlId(transactionPoid, detRowId))
                .chargePoid(detail.getChargePoid())
                .currencyExchange(detail.getCurrencyExchange())
                .quantity(detail.getQuantity())
                .buyPercharge(detail.getBuyPercharge())
                .perQuantityAmount(detail.getPerQuantityAmount())
                .paidAtPortPoid(detail.getPaidAtPortPoid())
                .chargeType(detail.getChargeType() != null ? detail.getChargeType() : "MANIFEST")
                .currencyCode(detail.getCurrencyCode())
                .freightType(detail.getFreightType())
                .ediChargeCode(detail.getEdiChargeCode())
                .arShReceiptTransactionPoid(detail.getArShReceiptTransactionPoid())
                .chargeBasisOn(detail.getChargeBasisOn())
                .printGroup(detail.getPrintGroup())
                .receiptInvoicePoid(detail.getReceiptInvoicePoid())
                .docRefLinkNo(detail.getDocRefLinkNo())
                .reprintDetRowId(detail.getReprintDetRowId())
                .reprintTransactionPoid(detail.getReprintTransactionPoid())
                .invoiceType(detail.getInvoiceType())
                .autoCanInvoiceNo(detail.getAutoCanInvoiceNo())
                .chargeDescription(detail.getChargeDescription())
                .taxPoid(detail.getTaxPoid())
                .taxPercentage(detail.getTaxPercentage())
                .taxAmount(detail.getTaxAmount())
                .cnRefDocId(detail.getCnRefDocId())
                .cnRefDocPoid(detail.getCnRefDocPoid())
                .cnRefDetRowId(detail.getCnRefDetRowId())
                .cnIssueInvoice(detail.getCnIssueInvoice())
                .selectRow(detail.getSelectRow())
                .build();
        chargesDtlRepository.save(entity);

        
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        String logDetail = "Row Created on Export Manifest BL Charge Detail with detRowId: " + detRowId;
        loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
    }

    private void updateChargeDetail(ChargeRequestDto detail, Long transactionPoid) {
        Long detRowId = detail.getDetRowId();
        ShipBlManifestDtlId id = new ShipBlManifestDtlId(transactionPoid, detRowId);
        ExportManifestBlChargesDtl entity = chargesDtlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charge Detail", "detRowId", detRowId.toString()));

        ExportManifestBlChargesDtl oldEntity = new ExportManifestBlChargesDtl();
        BeanUtils.copyProperties(entity, oldEntity);
        entity.setChargePoid(detail.getChargePoid());
        entity.setCurrencyExchange(detail.getCurrencyExchange());
        entity.setQuantity(detail.getQuantity());
        entity.setBuyPercharge(detail.getBuyPercharge());
        entity.setPerQuantityAmount(detail.getPerQuantityAmount());
        entity.setPaidAtPortPoid(detail.getPaidAtPortPoid());
        if (detail.getChargeType() != null) {
            entity.setChargeType(detail.getChargeType());
        }
        entity.setCurrencyCode(detail.getCurrencyCode());
        if (detail.getFreightType() != null) {
            entity.setFreightType(detail.getFreightType());
        }
        entity.setEdiChargeCode(detail.getEdiChargeCode());
        entity.setArShReceiptTransactionPoid(detail.getArShReceiptTransactionPoid());
        entity.setChargeBasisOn(detail.getChargeBasisOn());
        entity.setPrintGroup(detail.getPrintGroup());
        entity.setReceiptInvoicePoid(detail.getReceiptInvoicePoid());
        entity.setDocRefLinkNo(detail.getDocRefLinkNo());
        entity.setReprintDetRowId(detail.getReprintDetRowId());
        entity.setReprintTransactionPoid(detail.getReprintTransactionPoid());
        entity.setInvoiceType(detail.getInvoiceType());
        entity.setAutoCanInvoiceNo(detail.getAutoCanInvoiceNo());
        entity.setChargeDescription(detail.getChargeDescription());
        entity.setTaxPoid(detail.getTaxPoid());
        entity.setTaxPercentage(detail.getTaxPercentage());
        entity.setTaxAmount(detail.getTaxAmount());
        entity.setCnRefDocId(detail.getCnRefDocId());
        entity.setCnRefDocPoid(detail.getCnRefDocPoid());
        entity.setCnRefDetRowId(detail.getCnRefDetRowId());
        entity.setCnIssueInvoice(detail.getCnIssueInvoice());
        entity.setSelectRow(detail.getSelectRow());
        blManifestValidationService.validateChargeTypeMandatory(entity.getChargeType());
        blManifestValidationService.validateFreightTypeMandatory(entity.getFreightType());
        chargesDtlRepository.save(entity);

       
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        String logDetail = "KeyId = TRANSACTION_POID:" + transactionPoid + " DET_ROW_ID:" + detRowId;
        loggingService.createLogBatch(
                List.of(new LogRequestDto<>(
                        copyChargeForLog(oldEntity),
                        copyChargeForLog(entity),
                        ShipBlManifestChargesDtl.class,
                        docId,
                        docKeyPoid,
                        logDetail
                ))
        );
    }

    private void deleteChargeDetail(ChargeRequestDto detail, Long transactionPoid) {
        Long detRowId = detail.getDetRowId();
        chargesDtlRepository.deleteById(new ShipBlManifestDtlId(transactionPoid, detRowId));

       
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        String logDetail = "Row Deleted on Export Manifest BL Charge Detail with detRowId: " + detRowId;
        loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
    }

    private void saveDetailTables(ExportManifestBlCreateDto dto, Long transactionPoid) {
        Long detRowId = 1L;
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        // Save General Details
        if (dto.getGeneralCargoDetails() != null) {
            for (GeneralCargoRequestDto detail : dto.getGeneralCargoDetails()) {
            
                ExportManifestBlGeneralDtl entity = ExportManifestBlGeneralDtl.builder()
                        .id(new ShipBlManifestDtlId(transactionPoid, detail.getDetRowId() != null ? detail.getDetRowId() : detRowId++))
                        .comodityPoid(detail.getComodityPoid())
                        .cargoDescription(detail.getCargoDescription())
                        .quantity(detail.getQuantity())
                        .grsVolume(detail.getGrsVolume())
                        .grsWeight(detail.getGrsWeight())
                        .netVolume(detail.getNetVolume())
                        .netWeight(detail.getNetWeight())
                        .tareWeight(detail.getTareWeight())
                        .noOfPacks(detail.getNoOfPacks())
                        .packUnit(detail.getPackUnit())
                        .destinationPortPoid(detail.getDestinationPortPoid())
                        .build();
                generalDtlRepository.save(entity);

                Long rowId = entity.getId().getDetRowId();
                String logDetail = "Row Created on Export Manifest BL General Cargo Detail with detRowId: " + rowId;
                loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
            }
        }

        // Save Cargo Details
        if (dto.getCargoDescriptions() != null) {
            for (CargoDescriptionRequestDto detail : dto.getCargoDescriptions()) {
             
                ExportManifestBlCargoDtl entity = ExportManifestBlCargoDtl.builder()
                        .id(new ShipBlManifestCargoDtlId(
                                transactionPoid,
                                detail.getDetRowId() != null ? detail.getDetRowId() : detRowId++,
                                detail.getDescriptionType()))
                        .cargoDescription(detail.getCargoDescription())
                        .recordOrder(detail.getRecordOrder())
                        .build();
                cargoDtlRepository.save(entity);

                Long rowId = entity.getId().getDetRowId();
                String logDetail = "Row Created on Export Manifest BL Cargo Detail with detRowId: " + rowId;
                loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
            }
        }

        // Save Container Details
        if (dto.getContainers() != null) {
            for (ContainerRequestDto detail : dto.getContainers()) {
             
                ExportManifestBlContainerDtl entity = ExportManifestBlContainerDtl.builder()
                        .id(new ShipBlManifestDtlId(transactionPoid, detail.getDetRowId() != null ? detail.getDetRowId() : detRowId++))
                        .mateTransactionPoid(detail.getMateTransactionPoid())
                        .containerNo(detail.getContainerNo())
                        .equipmentShipperOwn(detail.getEquipmentShipperOwn())
                        .cargoDescription(detail.getCargoDescription())
                        .equipmentSealNo(detail.getEquipmentSealNo())
                        .equipmentIsoType(detail.getEquipmentIsoType())
                        .equipmentType(detail.getEquipmentType())
                        .equipmentSize(detail.getEquipmentSize())
                        .quantity(detail.getQuantity())
                        .grsVolume(detail.getGrsVolume())
                        .grsWeight(detail.getGrsWeight())
                        .netVolume(detail.getNetVolume())
                        .netWeight(detail.getNetWeight())
                        .tareWeight(detail.getTareWeight())
                        .noOfPacks(detail.getNoOfPacks())
                        .packUnit(detail.getPackUnit())
                        .comodityPoid(detail.getComodityPoid())
                        .destinationPortPoid(detail.getDestinationPortPoid())
                        .imo(detail.getImo())
                        .oogL(detail.getOogL())
                        .oogB(detail.getOogB())
                        .oogH(detail.getOogH())
                        .refferTemp(detail.getRefferTemp())
                        .refferHum(detail.getRefferHum())
                        .refferVent(detail.getRefferVent())
                        .issueToConsignee(detail.getIssueToConsignee())
                        .returnFromConsignee(detail.getReturnFromConsignee())
                        .isImco(detail.getIsImco())
                        .isOog(detail.getIsOog())
                        .isRefer(detail.getIsRefer())
                        .referType(detail.getReferType())
                        .imcoClassType(detail.getImcoClassType())
                        .guaranteeFlag(detail.getGuaranteeFlag())
                        .guaranteedBy(detail.getGuaranteedBy())
                        .extraFreeDays(detail.getExtraFreeDays())
                        .extraFreeDaysPrnpls(detail.getExtraFreeDaysPrnpls())
                        .oogLW(detail.getOogLW())
                        .oogRW(detail.getOogRW())
                        .oogF(detail.getOogF())
                        .oogA(detail.getOogA())
                        .oogType(detail.getOogType())
                        .imcoClassActual(detail.getImcoClassActual())
                        .displayCollectedDate(detail.getDisplayCollectedDate())
                        .totalDaysCollected(detail.getTotalDaysCollected())
                        .totalAmountCollected(detail.getTotalAmountCollected())
                        .printReturnFormDefault(detail.getPrintReturnFormDefault())
                        .printDeliveryFormDefault(detail.getPrintDeliveryFormDefault())
                        .demDttPbleTrnsfd(detail.getDemDttPbleTrnsfd())
                        .hsCode(detail.getHsCode())
                        .hsDescription(detail.getHsDescription())
                        .amountPerDayAfterFree(detail.getAmountPerDayAfterFree())
                        .actualDischargeDate(detail.getActualDischargeDate())
                        .build();
                containerDtlRepository.save(entity);

                Long rowId = entity.getId().getDetRowId();
                String logDetail = "Row Created on Export Manifest BL Container Detail with detRowId: " + rowId;
                loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
            }
        }

        // Save Charges Details
        if (dto.getChargeDetails() != null) {
            for (ChargeRequestDto detail : dto.getChargeDetails()) {
                blManifestValidationService.validateChargeTypeMandatory(detail.getChargeType());
                blManifestValidationService.validateFreightTypeMandatory(detail.getFreightType());
                ExportManifestBlChargesDtl entity = ExportManifestBlChargesDtl.builder()
                        .id(new ShipBlManifestDtlId(transactionPoid, detail.getDetRowId() != null ? detail.getDetRowId() : detRowId++))
                        .chargePoid(detail.getChargePoid())
                        .currencyExchange(detail.getCurrencyExchange())
                        .quantity(detail.getQuantity())
                        .buyPercharge(detail.getBuyPercharge())
                        .perQuantityAmount(detail.getPerQuantityAmount())
                        .paidAtPortPoid(detail.getPaidAtPortPoid())
                        .chargeType(detail.getChargeType() != null ? detail.getChargeType() : "MANIFEST")
                        .currencyCode(detail.getCurrencyCode())
                        .freightType(detail.getFreightType())
                        .ediChargeCode(detail.getEdiChargeCode())
                        .arShReceiptTransactionPoid(detail.getArShReceiptTransactionPoid())
                        .chargeBasisOn(detail.getChargeBasisOn())
                        .printGroup(detail.getPrintGroup())
                        .receiptInvoicePoid(detail.getReceiptInvoicePoid())
                        .docRefLinkNo(detail.getDocRefLinkNo())
                        .reprintDetRowId(detail.getReprintDetRowId())
                        .reprintTransactionPoid(detail.getReprintTransactionPoid())
                        .invoiceType(detail.getInvoiceType())
                        .autoCanInvoiceNo(detail.getAutoCanInvoiceNo())
                        .chargeDescription(detail.getChargeDescription())
                        .taxPoid(detail.getTaxPoid())
                        .taxPercentage(detail.getTaxPercentage())
                        .taxAmount(detail.getTaxAmount())
                        .cnRefDocId(detail.getCnRefDocId())
                        .cnRefDocPoid(detail.getCnRefDocPoid())
                        .cnRefDetRowId(detail.getCnRefDetRowId())
                        .cnIssueInvoice(detail.getCnIssueInvoice())
                        .selectRow(detail.getSelectRow())
                        .build();
                chargesDtlRepository.save(entity);

                Long rowId = entity.getId().getDetRowId();
                String logDetail = "Row Created on Export Manifest BL Charge Detail with detRowId: " + rowId;
                loggingService.createLogSummaryEntry(docId, docKeyPoid, logDetail);
            }
        }
    }

    private void loadDetailTables(ExportManifestBlRequestDto dto, Long transactionPoid) {
        // Load General Details
        List<GeneralCargoRequestDto> generalDetails = generalDtlRepository.findById_TransactionPoid(transactionPoid)
                .stream()
                .map(entity -> GeneralCargoRequestDto.builder()
                        .detRowId(entity.getId().getDetRowId())
                        .comodityPoid(entity.getComodityPoid())
                        .cargoDescription(entity.getCargoDescription())
                        .quantity(entity.getQuantity())
                        .grsVolume(entity.getGrsVolume())
                        .grsWeight(entity.getGrsWeight())
                        .netVolume(entity.getNetVolume())
                        .netWeight(entity.getNetWeight())
                        .tareWeight(entity.getTareWeight())
                        .noOfPacks(entity.getNoOfPacks())
                        .packUnit(entity.getPackUnit())
                        .destinationPortPoid(entity.getDestinationPortPoid())
                        .build())
                .toList();
        dto.setGeneralCargoDetails(generalDetails);
        enrichGeneralCargoLovData(generalDetails);

        // Load Cargo Details
        List<CargoDescriptionRequestDto> cargoDetails = cargoDtlRepository.findById_TransactionPoid(transactionPoid)
                .stream()
                .map(entity -> CargoDescriptionRequestDto.builder()
                        .detRowId(entity.getId().getDetRowId())
                        .descriptionType(entity.getId().getDescriptionType())
                        .cargoDescription(entity.getCargoDescription())
                        .recordOrder(entity.getRecordOrder())
                        .build())
                .toList();
        dto.setCargoDescriptions(cargoDetails);

        // Load Container Details
        List<ContainerRequestDto> containerDetails = containerDtlRepository.findById_TransactionPoid(transactionPoid)
                .stream()
                .map(entity -> ContainerRequestDto.builder()
                        .detRowId(entity.getId().getDetRowId())
                        .mateTransactionPoid(entity.getMateTransactionPoid())
                        .containerNo(entity.getContainerNo())
                        .equipmentShipperOwn(entity.getEquipmentShipperOwn())
                        .cargoDescription(entity.getCargoDescription())
                        .equipmentSealNo(entity.getEquipmentSealNo())
                        .equipmentIsoType(entity.getEquipmentIsoType())
                        .equipmentType(entity.getEquipmentType())
                        .equipmentSize(entity.getEquipmentSize())
                        .quantity(entity.getQuantity())
                        .grsVolume(entity.getGrsVolume())
                        .grsWeight(entity.getGrsWeight())
                        .netVolume(entity.getNetVolume())
                        .netWeight(entity.getNetWeight())
                        .tareWeight(entity.getTareWeight())
                        .noOfPacks(entity.getNoOfPacks())
                        .packUnit(entity.getPackUnit())
                        .comodityPoid(entity.getComodityPoid())
                        .destinationPortPoid(entity.getDestinationPortPoid())
                        .imo(entity.getImo())
                        .oogL(entity.getOogL())
                        .oogB(entity.getOogB())
                        .oogH(entity.getOogH())
                        .refferTemp(entity.getRefferTemp())
                        .refferHum(entity.getRefferHum())
                        .refferVent(entity.getRefferVent())
                        .issueToConsignee(entity.getIssueToConsignee())
                        .returnFromConsignee(entity.getReturnFromConsignee())
                        .isImco(entity.getIsImco())
                        .isOog(entity.getIsOog())
                        .isRefer(entity.getIsRefer())
                        .referType(entity.getReferType())
                        .imcoClassType(entity.getImcoClassType())
                        .guaranteeFlag(entity.getGuaranteeFlag())
                        .guaranteedBy(entity.getGuaranteedBy())
                        .extraFreeDays(entity.getExtraFreeDays())
                        .extraFreeDaysPrnpls(entity.getExtraFreeDaysPrnpls())
                        .oogLW(entity.getOogLW())
                        .oogRW(entity.getOogRW())
                        .oogF(entity.getOogF())
                        .oogA(entity.getOogA())
                        .oogType(entity.getOogType())
                        .imcoClassActual(entity.getImcoClassActual())
                        .displayCollectedDate(entity.getDisplayCollectedDate())
                        .totalDaysCollected(entity.getTotalDaysCollected())
                        .totalAmountCollected(entity.getTotalAmountCollected())
                        .printReturnFormDefault(entity.getPrintReturnFormDefault())
                        .printDeliveryFormDefault(entity.getPrintDeliveryFormDefault())
                        .demDttPbleTrnsfd(entity.getDemDttPbleTrnsfd())
                        .hsCode(entity.getHsCode())
                        .hsDescription(entity.getHsDescription())
                        .amountPerDayAfterFree(entity.getAmountPerDayAfterFree())
                        .actualDischargeDate(entity.getActualDischargeDate())
                        .build())
                .toList();
        dto.setContainers(containerDetails);
        enrichContainerLovData(containerDetails);

        // Load Charges Details
        List<ChargeRequestDto> chargeDetails = chargesDtlRepository.findById_TransactionPoid(transactionPoid)
                .stream()
                .map(entity -> ChargeRequestDto.builder()
                        .detRowId(entity.getId().getDetRowId())
                        .chargePoid(entity.getChargePoid())
                        .currencyExchange(entity.getCurrencyExchange())
                        .quantity(entity.getQuantity())
                        .buyPercharge(entity.getBuyPercharge())
                        .perQuantityAmount(entity.getPerQuantityAmount())
                        .paidAtPortPoid(entity.getPaidAtPortPoid())
                        .chargeType(entity.getChargeType())
                        .currencyCode(entity.getCurrencyCode())
                        .freightType(entity.getFreightType())
                        .ediChargeCode(entity.getEdiChargeCode())
                        .arShReceiptTransactionPoid(entity.getArShReceiptTransactionPoid())
                        .chargeBasisOn(entity.getChargeBasisOn())
                        .printGroup(entity.getPrintGroup())
                        .receiptInvoicePoid(entity.getReceiptInvoicePoid())
                        .docRefLinkNo(entity.getDocRefLinkNo())
                        .reprintDetRowId(entity.getReprintDetRowId())
                        .reprintTransactionPoid(entity.getReprintTransactionPoid())
                        .invoiceType(entity.getInvoiceType())
                        .autoCanInvoiceNo(entity.getAutoCanInvoiceNo())
                        .chargeDescription(entity.getChargeDescription())
                        .taxPoid(entity.getTaxPoid())
                        .taxPercentage(entity.getTaxPercentage())
                        .taxAmount(entity.getTaxAmount())
                        .cnRefDocId(entity.getCnRefDocId())
                        .cnRefDocPoid(entity.getCnRefDocPoid())
                        .cnRefDetRowId(entity.getCnRefDetRowId())
                        .cnIssueInvoice(entity.getCnIssueInvoice())
                        .selectRow(entity.getSelectRow())
                        .build())
                .toList();
        dto.setChargeDetails(chargeDetails);
        enrichChargeLovData(chargeDetails);
    }

    private void enrichGeneralCargoLovData(List<GeneralCargoRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        for (GeneralCargoRequestDto dto : dtos) {
            try {
                if (dto.getComodityPoid() != null && dto.getComodityPoid() > 0) {
                    dto.setComodityDet(
                            lovService.getLovItemByPoid(dto.getComodityPoid(), "COMODITY", groupPoid, companyPoid,
                                    userPoid));
                }
                if (dto.getDestinationPortPoid() != null && dto.getDestinationPortPoid() > 0) {
                    dto.setDestinationPortDet(
                            lovService.getLovItemByPoid(dto.getDestinationPortPoid(), "PORT_MASTER", groupPoid,
                                    companyPoid, userPoid));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch LOV data for general cargo detail with detRowId: {}", dto.getDetRowId(), e);
            }
        }
    }

    private void enrichContainerLovData(List<ContainerRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        for (ContainerRequestDto dto : dtos) {
            try {
                if (dto.getComodityPoid() != null && dto.getComodityPoid() > 0) {
                    dto.setComodityDet(
                            lovService.getLovItemByPoid(dto.getComodityPoid(), "COMODITY", groupPoid, companyPoid,
                                    userPoid));
                }
                if (dto.getDestinationPortPoid() != null && dto.getDestinationPortPoid() > 0) {
                    dto.setDestinationPortDet(
                            lovService.getLovItemByPoid(dto.getDestinationPortPoid(), "PORT_MASTER", groupPoid,
                                    companyPoid, userPoid));
                }
                if (dto.getEquipmentIsoType() != null) {
                    dto.setEquipmentIsoTypeDet(
                            lovService.getLovItemByCode(dto.getEquipmentIsoType(), "CONTAINER_TYPE_MASTER",
                                    groupPoid, companyPoid, userPoid));
                }
                if (dto.getImcoClassType() != null) {
                    dto.setImcoClassTypeDet(
                            lovService.getLovItemByCode(dto.getImcoClassType(), "IMCO_CLASS", groupPoid, companyPoid,
                                    userPoid));
                }
                if (dto.getOogType() != null) {
                    dto.setOogTypeDet(
                            lovService.getLovItemByCode(dto.getOogType(), "OOG_TYPE", groupPoid, companyPoid,
                                    userPoid));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch LOV data for container detail with detRowId: {}", dto.getDetRowId(), e);
            }
        }
    }

    private void enrichChargeLovData(List<ChargeRequestDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        Map<String, Map<Long, LovItem>> poidCache = new HashMap<>();
        Map<String, Map<String, LovItem>> codeCache = new HashMap<>();

        for (ChargeRequestDto dto : dtos) {
            if (dto.getChargePoid() != null) {
                dto.setChargeDet(getLovItemWithCache(poidCache, dto.getChargePoid(), "CHARGE_MASTER", groupPoid,
                        companyPoid, userPoid));
            }
            if (dto.getPaidAtPortPoid() != null) {
                dto.setPaidAtPortDet(getLovItemWithCache(poidCache, dto.getPaidAtPortPoid(), "PORT_MASTER", groupPoid,
                        companyPoid, userPoid));
            }
            if (dto.getReceiptInvoicePoid() != null) {
                dto.setReceiptInvoiceDet(getLovItemWithCache(poidCache, dto.getReceiptInvoicePoid(),
                        "MANIFEST_RECEIPT_INVOICE", groupPoid, companyPoid, userPoid));
            }
            if (dto.getTaxPoid() != null) {
                dto.setTaxDet(getLovItemWithCache(poidCache, dto.getTaxPoid(), "TAX_MASTER", groupPoid, companyPoid,
                        userPoid));
            }
            if (dto.getChargeType() != null) {
                dto.setChargeTypeDet(getLovItemByCodeWithCache(codeCache, dto.getChargeType(), "CHARGE_TYPE", groupPoid,
                        companyPoid, userPoid));
            }
            if (dto.getCurrencyCode() != null) {
                dto.setCurrencyCodeDet(getLovItemByCodeWithCache(codeCache, dto.getCurrencyCode(), "CURRENCY",
                        groupPoid, companyPoid, userPoid));
            }
            if (dto.getFreightType() != null) {
                dto.setFreightTypeDet(getLovItemByCodeWithCache(codeCache, dto.getFreightType(), "SHIP_FREIGHT_TYPE",
                        groupPoid, companyPoid, userPoid));
            }
            if (dto.getChargeBasisOn() != null) {
                dto.setBasisDet(resolveChargeBasisDet(dto.getChargeBasisOn(), poidCache, codeCache, groupPoid,
                        companyPoid, userPoid));
            }
        }
    }

    private LovItem resolveChargeBasisDet(String chargeBasisOn,
            Map<String, Map<Long, LovItem>> poidCache,
            Map<String, Map<String, LovItem>> codeCache,
            Long groupPoid, Long companyPoid, Long userPoid) {
        if (chargeBasisOn.chars().allMatch(Character::isDigit)) {
            return getLovItemWithCache(poidCache, Long.parseLong(chargeBasisOn), "CONTAINER_TYPE_MASTER", groupPoid,
                    companyPoid, userPoid);
        }
        return getLovItemByCodeWithCache(codeCache, chargeBasisOn, "CONTAINER_TYPE_MASTER", groupPoid, companyPoid,
                userPoid);
    }

    private LovItem getLovItemWithCache(
            Map<String, Map<Long, LovItem>> cache, Long poid, String lovName,
            Long groupPoid, Long companyPoid, Long userPoid) {
        Map<Long, LovItem> innerCache = cache.computeIfAbsent(lovName, k -> new HashMap<>());
        if (!innerCache.containsKey(poid)) {
            try {
                innerCache.put(poid, lovService.getLovItemByPoid(poid, lovName, groupPoid, companyPoid, userPoid));
            } catch (Exception e) {
                log.warn("Failed to fetch LOV for lovName: {} and poid: {}", lovName, poid, e);
                innerCache.put(poid, null);
            }
        }
        return innerCache.get(poid);
    }

    private LovItem getLovItemByCodeWithCache(
            Map<String, Map<String, LovItem>> cache, String code, String lovName,
            Long groupPoid, Long companyPoid, Long userPoid) {
        Map<String, LovItem> innerCache = cache.computeIfAbsent(lovName, k -> new HashMap<>());
        if (!innerCache.containsKey(code)) {
            try {
                innerCache.put(code, lovService.getLovItemByCode(code, lovName, groupPoid, companyPoid, userPoid));
            } catch (Exception e) {
                log.warn("Failed to fetch LOV for lovName: {} and code: {}", lovName, code, e);
                innerCache.put(code, null);
            }
        }
        return innerCache.get(code);
    }

    // ==================== Document audit (100-104) ====================

    private ShipBlManifestHdr copyHdrForLog(ExportManifestBlHdr source) {
        if (source == null) {
            return null;
        }
        ShipBlManifestHdr target = new ShipBlManifestHdr();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private ShipBlManifestCargoDtl copyCargoForLog(ExportManifestBlCargoDtl source) {
        if (source == null) {
            return null;
        }
        ShipBlManifestCargoDtl target = new ShipBlManifestCargoDtl();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private ShipBlManifestGeneralDtl copyGeneralForLog(ExportManifestBlGeneralDtl source) {
        if (source == null) {
            return null;
        }
        ShipBlManifestGeneralDtl target = new ShipBlManifestGeneralDtl();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private ShipBlManifestContainerDtl copyContainerForLog(ExportManifestBlContainerDtl source) {
        if (source == null) {
            return null;
        }
        ShipBlManifestContainerDtl target = new ShipBlManifestContainerDtl();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private ShipBlManifestChargesDtl copyChargeForLog(ExportManifestBlChargesDtl source) {
        if (source == null) {
            return null;
        }
        ShipBlManifestChargesDtl target = new ShipBlManifestChargesDtl();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private void logAggregateCargoField(
            String documentId, String docKeyPoid, String fieldName, String beforeText, String afterText) {
        if (java.util.Objects.equals(normalizeLogText(beforeText), normalizeLogText(afterText))) {
            return;
        }
        String keyDetail = "KeyId = TRANSACTION_POID:" + docKeyPoid;
        loggingService.createLogDetailsEntry(
                documentId,
                docKeyPoid,
                fieldName,
                normalizeLogText(beforeText),
                normalizeLogText(afterText),
                keyDetail,
                LOG_TABLE_CARGO);
    }

    private void logBookingMateVoyageFilter(String documentId, String docKeyPoid, String before, String after) {
        if (java.util.Objects.equals(normalizeLogText(before), normalizeLogText(after))) {
            return;
        }
        String keyDetail = "KeyId = TRANSACTION_POID:" + docKeyPoid;
        loggingService.createLogDetailsEntry(
                documentId,
                docKeyPoid,
                "BookingMateVoyage",
                normalizeLogText(before),
                normalizeLogText(after),
                keyDetail,
                LOG_TABLE_HDR);
    }

    private static String normalizeLogText(String value) {
        return value == null ? "" : value.trim();
    }
}

