package com.asg.shipping.linetariffs.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.DiffUtil;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.linetariffs.dto.*;
import com.asg.shipping.linetariffs.entity.*;
import com.asg.shipping.common.repository.ShipLineMasterTypeRepository;
import com.asg.shipping.containertypes.entity.ShipContainerTypeMaster;
import com.asg.shipping.containertypes.repository.ShipContainerTypeMasterRepository;
import com.asg.shipping.linetariffs.repository.*;
import com.asg.shipping.linetariffs.util.LineTariffMapper;
import com.asg.shipping.linetariffs.util.LineTariffSlabValidator;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

import javax.sql.DataSource;

/**
 * Service implementation for Line Tariffs operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineTariffsServiceImpl implements LineTariffsService {

    private static final String LINE_TARIFF = "Line Tariff";
    private static final String TRANSACTION_POID = "transactionPoid";
    private static final String TRANSACTION_POID_COL = "TRANSACTION_POID";
    private static final String TARIFF_DETAIL = "Tariff Detail";
    private static final String DET_ROW_ID = "detRowId";
    private static final String DELETED_FIELD = "DELETED";
    private static final String COMPANY_POID_COL = "COMPANY_POID";
    private static final String DOC_ID = "100-050";
    private static final Map<String, String> LIST_DISPLAY_FIELDS = Map.of(
            TRANSACTION_POID_COL, "text",
            "DESCRIPTION", "text",
            "PERIOD_FROM", "text",
            "PERIOD_TO", "text"
    );
    private static final String ACTION_ISCREATED = "iscreated";
    private static final String ACTION_ISUPDATED = "isupdated";
    private static final String ACTION_ISDELETED = "isdeleted";
    private static final String LOG_KEY_ID_FORMAT = "KeyId = TRANSACTION_POID: %s DET_ROW_ID: %s";
    private static final String IMP_COLLECTABLE_DETAIL = "Import Demurrage Collectable";
    private static final String IMP_PAYABLE_DETAIL = "Import Demurrage Payable";
    private static final String EXP_COLLECTABLE_DETAIL = "Export Detention Collectable";
    private static final String EXP_PAYABLE_DETAIL = "Export Detention Payable";

    private final ShipLineTariffHdrRepository tariffHdrRepository;
    private final LineTariffListRepository lineTariffListRepository;
    private final ShipLineTariffImpDtlRepository impDtlRepository;
    private final ShipLineTariffImpPayDtlRepository impPayDtlRepository;
    private final ShipLineTariffExpDtlRepository expDtlRepository;
    private final ShipLineTariffExpPayDtlRepository expPayDtlRepository;
    private final DocumentSearchService documentService;
    private final LineTariffMapper mapper;
    private final LoggingService loggingService;
    private final DocumentDeleteService documentDeleteService;
    private final ShipContainerTypeMasterRepository containerTypeRepository;
    private final ShipLineMasterTypeRepository lineMasterTypeRepository;
    private final EntityManager entityManager;
    private final PrintService printService;
    private final DataSource dataSource;

    @Override
    @Transactional(readOnly = true)
    public byte[] print(Long transactionPoid) throws Exception {
        log.info("Generating Notice to Trade PDF for line tariff id: {}", transactionPoid);

        Long groupPoid = UserContext.getGroupPoid();
        tariffHdrRepository.findByTransactionPoidAndGroupPoid(transactionPoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, transactionPoid.toString()));

        List<ShipLineTariffImpDtl> impDetails = impDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        if (impDetails.isEmpty()) {
            throw new ValidationException(
                    "No import demurrage collectable slab data found. Load container types (IMP) and save tariff details before printing.");
        }

        Map<String, Object> params = printService.buildBaseParams(transactionPoid, DOC_ID);
        params.put("DOC_KEY_POID", String.valueOf(transactionPoid));
        params.put("NOTICE2TRADE_SUBREPORT2", printService.load("Shipping/SH/NOTICE2TRADE_subreport2.jrxml"));
        JasperReport mainReport = printService.load("Shipping/SH/NOTICE2TRADE.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchLineTariffs(String docId, FilterRequestDto request,
                                                 Pageable pageable, LocalDate startDate, LocalDate endDate) {
        log.info("get LOR LineTariffs started for docId={} startDate={} endDate={}", docId, startDate, endDate);

        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> rawFilters = request != null
                ? new ArrayList<>(documentService.resolveFilters(request))
                : new ArrayList<>();
        List<FilterDto> filtersList = resolveListFilters(rawFilters);

        Long companyPoid = UserContext.getCompanyPoid();
        Long groupPoid = UserContext.getGroupPoid();

        LocalDate effectiveStartDate = startDate;
        LocalDate effectiveEndDate = endDate;
        PeriodRange listPeriod = extractListPeriodFromBody(rawFilters);
        if (listPeriod != null) {
            effectiveStartDate = listPeriod.start();
            effectiveEndDate = listPeriod.end();
        }
        if (hasTextSearchFilters(filtersList)) {
            effectiveStartDate = null;
            effectiveEndDate = null;
        }

        LineTariffListRepository.ListSearchResult result = lineTariffListRepository.search(
                companyPoid,
                groupPoid,
                effectiveStartDate,
                effectiveEndDate,
                isDeleted,
                filtersList,
                pageable
        );

        Page<Map<String, Object>> page = new PageImpl<>(result.records(), pageable, result.totalRecords());
        log.info("get LOR LineTariffs completed for docId={} count={}", docId, result.totalRecords());
        return PaginationUtil.wrapPage(page, LIST_DISPLAY_FIELDS);
    }

    /**
     * Body filters only — list period resolved separately; company/group from UserContext.
     */
    private List<FilterDto> resolveListFilters(List<FilterDto> filters) {
        List<FilterDto> resolved = new ArrayList<>(filters);
        resolved.removeIf(f -> DELETED_FIELD.equalsIgnoreCase(f.searchField()));
        resolved.removeIf(f -> COMPANY_POID_COL.equalsIgnoreCase(f.searchField()));
        resolved.removeIf(f -> "GROUP_POID".equalsIgnoreCase(f.searchField()));
        resolved.removeIf(f -> "PERIOD_FROM".equalsIgnoreCase(f.searchField())
                || "PERIOD_TO".equalsIgnoreCase(f.searchField()));
        return resolved;
    }

    /**
     * LOR period window from request body (UI Period From / Period To).
     */
    private PeriodRange extractListPeriodFromBody(List<FilterDto> rawFilters) {
        LocalDate windowFrom = null;
        LocalDate windowTo = null;
        for (FilterDto filter : rawFilters) {
            if (filter == null || filter.searchField() == null || filter.searchValue() == null) {
                continue;
            }
            String field = filter.searchField().trim().toUpperCase();
            String value = filter.searchValue().trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                if ("PERIOD_FROM".equals(field)) {
                    if (value.startsWith("<=") || value.startsWith("<")) {
                        windowTo = parseFilterDate(value);
                    } else {
                        windowFrom = parseFilterDate(value);
                    }
                } else if ("PERIOD_TO".equals(field)) {
                    if (value.startsWith(">=") || value.startsWith(">")) {
                        windowFrom = parseFilterDate(value);
                    } else {
                        windowTo = parseFilterDate(value);
                    }
                }
            } catch (DateTimeParseException ignored) {
                // skip invalid period filter value
            }
        }
        if (windowFrom != null && windowTo != null) {
            return new PeriodRange(windowFrom, windowTo);
        }
        return null;
    }

    private static LocalDate parseFilterDate(String value) {
        return LocalDate.parse(value.replaceFirst("^[<>=]+", "").trim());
    }

    private record PeriodRange(LocalDate start, LocalDate end) {}

    private boolean hasTextSearchFilters(List<FilterDto> filters) {
        if (filters == null) {
            return false;
        }
        return filters.stream().anyMatch(f -> {
            if (f == null || f.searchField() == null || f.searchValue() == null) {
                return false;
            }
            String field = f.searchField().trim().toUpperCase();
            String value = f.searchValue().trim();
            return !value.isEmpty()
                    && ("GLOBALSEARCH".equals(field) || "DESCRIPTION".equals(field));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public LineTariffDto getLineTariff(Long id) {
        log.info("Getting line tariff with id: {}", id);

        Long groupPoid = com.asg.common.lib.security.util.UserContext.getGroupPoid();

        ShipLineTariffHdr tariff = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, id.toString()));

        // Fetch all detail records
        List<ShipLineTariffImpDtl> impDtlList = impDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffImpPayDtl> impPayDtlList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffExpDtl> expDtlList = expDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffExpPayDtl> expPayDtlList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);

        LineTariffDto dto = mapper.mapToDto(tariff, impDtlList, impPayDtlList, expDtlList, expPayDtlList, buildContainerTypeMap(impDtlList, impPayDtlList, expDtlList, expPayDtlList));

        // Legacy DocumentAfterView: disable period-from when only 1 tariff exists for this line
        long tariffCountForLine = tariffHdrRepository.findLatestByLinePoidAndGroupPoid(tariff.getLinePoid(), groupPoid).size();
        dto.setPeriodFromEditable(tariffCountForLine != 1);

        log.info("Successfully retrieved line tariff with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public LineTariffDto createLineTariff(LineTariffCreateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Creating line tariff for line: {}, period: {} to {}", dto.getLinePoid(), dto.getPeriodFrom(), dto.getPeriodTo());

        // Validate
        validateTariffCreateDTO(dto, groupPoid);

        // Create header entity
        ShipLineTariffHdr tariff = new ShipLineTariffHdr();

        // Auto-generate docRef if not provided (matching legacy RTN_GLOBAL_SEQ_NO behaviour)
        if (dto.getDocRef() == null || dto.getDocRef().trim().isEmpty()) {
            Long companyPoid = UserContext.getCompanyPoid();
            String companyCode = tariffHdrRepository.findCompanyCodeByPoid(companyPoid);
            String generatedDocRef = tariffHdrRepository.generateDocRef(companyCode);
            dto.setDocRef(generatedDocRef);
        }

        mapper.mapCreateDTOToEntity(dto, tariff, groupPoid);
        tariff.setCompanyPoid(UserContext.getCompanyPoid());

        // Additional validation just before save to prevent race conditions
        if (dto.getDocRef() != null && !dto.getDocRef().trim().isEmpty() && tariffHdrRepository.existsByDocRef(dto.getDocRef().trim())) {
            throw new ValidationException("Document Reference " + dto.getDocRef().trim() + " already exists. Please use a different reference.");
        }

        // Save header (generates TRANSACTION_POID)
        ShipLineTariffHdr saved;
        try {
            saved = tariffHdrRepository.save(tariff);
            tariffHdrRepository.flush(); // Force flush to catch constraint violations immediately
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (e.getMessage().contains("UK_DOCREFFSHIP_LINE_TARIFF_HDR")) {
                throw new ValidationException("Document Reference " + dto.getDocRef() + " already exists. Please use a different reference.");
            }
            if (e.getMessage().contains("SHIP_TF_HDR_LINPERION_UK")) {
                throw new ValidationException("A tariff with the same line and period already exists. Please use a different period.");
            }
            throw e;
        }

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), saved.getTransactionPoid().toString());

        // Create detail records
        createDetailRecords(saved.getTransactionPoid(), dto);

        // Fetch all detail records for response
        List<ShipLineTariffImpDtl> impDtlList = impDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());
        List<ShipLineTariffImpPayDtl> impPayDtlList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());
        List<ShipLineTariffExpDtl> expDtlList = expDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());
        List<ShipLineTariffExpPayDtl> expPayDtlList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());

        LineTariffDto result = mapper.mapToDto(saved, impDtlList, impPayDtlList, expDtlList, expPayDtlList, buildContainerTypeMap(impDtlList, impPayDtlList, expDtlList, expPayDtlList));
        log.info("Successfully created line tariff with id: {}", saved.getTransactionPoid());
        return result;
    }

    @Override
    @Transactional(timeout = 120)
    public LineTariffDto updateLineTariff(Long id, LineTariffUpdateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Updating line tariff with id: {}", id);

        // Find existing tariff
        ShipLineTariffHdr tariff = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, id.toString()));

        // Validate
        validateTariffUpdateDTO(dto, id, groupPoid);

        ShipLineTariffHdr oldTariff = new ShipLineTariffHdr();
        BeanUtils.copyProperties(tariff, oldTariff);

        // Update header entity
        mapper.mapUpdateDTOToEntity(dto, tariff);
        tariff.setCompanyPoid(UserContext.getCompanyPoid());
        ShipLineTariffHdr saved;
        try {
            saved = tariffHdrRepository.save(tariff);
            tariffHdrRepository.flush(); // Force flush to catch constraint violations immediately
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (e.getMessage().contains("UK_DOCREFFSHIP_LINE_TARIFF_HDR")) {
                throw new ValidationException("Document Reference " + dto.getDocRef() + " already exists. Please use a different reference.");
            }
            if (e.getMessage().contains("SHIP_TF_HDR_LINPERION_UK")) {
                throw new ValidationException("A tariff with the same line and period already exists. Please use a different period.");
            }
            throw e;
        }

        if (hasEntityChanges(oldTariff, saved, ShipLineTariffHdr.class)) {
            loggingService.logChanges(oldTariff, saved, ShipLineTariffHdr.class, UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, TRANSACTION_POID_COL);
        }

        // Update detail records
        updateDetailRecords(id, dto);

        // Fetch all detail records for response
        List<ShipLineTariffImpDtl> impDtlList = impDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffImpPayDtl> impPayDtlList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffExpDtl> expDtlList = expDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipLineTariffExpPayDtl> expPayDtlList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(id);

        LineTariffDto result = mapper.mapToDto(saved, impDtlList, impPayDtlList, expDtlList, expPayDtlList, buildContainerTypeMap(impDtlList, impPayDtlList, expDtlList, expPayDtlList));
        log.info("Successfully updated line tariff with id: {}", id);
        return result;
    }

    @Override
    @Transactional
    public void deleteLineTariff(Long id, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting line tariff with id: {}", id);

        Long groupPoid = UserContext.getGroupPoid();

        tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, id.toString()));

        documentDeleteService.deleteDocument(
                id,
                "SHIP_LINE_TARIFF_HDR",
                TRANSACTION_POID_COL,
                deleteReasonDto,
                null
        );

        log.info("Successfully deleted line tariff with id: {}", id);
    }

    @Override
    @Transactional
    public LineTariffDto copyLineTariff(Long id, CopyTariffRequestDTO request, Long groupPoid, Long userPoid) {
        log.info("Copying line tariff with id: {} to new period: {} to {}", id, request.getPeriodFrom(), request.getPeriodTo());

        ShipLineTariffHdr source = tariffHdrRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, id.toString()));

        validatePeriodRange(request.getPeriodFrom(), request.getPeriodTo());

        ShipLineTariffHdr newTariff = tryCopyViaProcedure(id, source, groupPoid);
        if (newTariff == null) {
            log.info("Using Java copy for line tariff id: {}", id);
            newTariff = copyViaJava(source, id, request, groupPoid);
        } else {
            log.info("COPY_LINE_TARIFF created new tariff id: {} for source id: {}", newTariff.getTransactionPoid(), id);
            applyCopyOverrides(newTariff, request, groupPoid);
            newTariff = tariffHdrRepository.save(newTariff);
        }

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), newTariff.getTransactionPoid().toString());

        List<ShipLineTariffImpDtl> impDtlList = impDtlRepository.findByTransactionPoidOrderByDetRowId(newTariff.getTransactionPoid());
        List<ShipLineTariffImpPayDtl> impPayDtlList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(newTariff.getTransactionPoid());
        List<ShipLineTariffExpDtl> expDtlList = expDtlRepository.findByTransactionPoidOrderByDetRowId(newTariff.getTransactionPoid());
        List<ShipLineTariffExpPayDtl> expPayDtlList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(newTariff.getTransactionPoid());

        LineTariffDto result = mapper.mapToDto(newTariff, impDtlList, impPayDtlList, expDtlList, expPayDtlList,
                buildContainerTypeMap(impDtlList, impPayDtlList, expDtlList, expPayDtlList));
        log.info("Successfully copied line tariff with id: {} to new tariff with id: {}", id, newTariff.getTransactionPoid());
        return result;
    }

    /**
     * Attempt COPY_LINE_TARIFF stored procedure. Returns the new header when proc succeeds, null to fall back to Java copy.
     */
    private ShipLineTariffHdr tryCopyViaProcedure(Long sourceId, ShipLineTariffHdr source, Long groupPoid) {
        try {
            tariffHdrRepository.callCopyLineTariff(sourceId);
            entityManager.flush();
            entityManager.clear();
        } catch (Exception e) {
            log.warn("COPY_LINE_TARIFF failed for transactionPoid={}, falling back to Java copy: {}",
                    sourceId, e.getMessage());
            return null;
        }

        return tariffHdrRepository.findNewerByLinePoidAndGroupPoid(sourceId, source.getLinePoid(), groupPoid)
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    log.warn("COPY_LINE_TARIFF ran for transactionPoid={} but no newer tariff was found; using Java copy",
                            sourceId);
                    return null;
                });
    }

    private ShipLineTariffHdr copyViaJava(ShipLineTariffHdr source, Long sourceId, CopyTariffRequestDTO request, Long groupPoid) {
        LocalDate truncatedSourceEnd = request.getPeriodFrom().minusDays(1);
        if (truncatedSourceEnd.isBefore(source.getPeriodFrom())) {
            throw new ValidationException("New period from date must be after the source tariff period from date");
        }

        Long companyPoid = UserContext.getCompanyPoid();
        if (tariffHdrRepository.existsOverlappingPeriod(
                source.getLinePoid(), groupPoid, companyPoid,
                request.getPeriodFrom(), request.getPeriodTo(), sourceId)) {
            throw new ValidationException("Period overlaps with an existing tariff for the same line");
        }

        ShipLineTariffHdr oldSource = new ShipLineTariffHdr();
        BeanUtils.copyProperties(source, oldSource);
        source.setPeriodTo(truncatedSourceEnd);
        ShipLineTariffHdr savedSource = tariffHdrRepository.save(source);
        loggingService.logChanges(oldSource, savedSource, ShipLineTariffHdr.class,
                UserContext.getDocumentId(), sourceId.toString(), LogDetailsEnum.MODIFIED, TRANSACTION_POID_COL);

        ShipLineTariffHdr newTariff = buildCopyHeader(source, request, groupPoid, companyPoid);
        ShipLineTariffHdr saved = tariffHdrRepository.save(newTariff);
        copyDetailRecords(sourceId, saved.getTransactionPoid());
        return saved;
    }

    private void applyCopyOverrides(ShipLineTariffHdr newTariff, CopyTariffRequestDTO request, Long groupPoid) {
        if (request.getDescription() != null) {
            newTariff.setDescription(request.getDescription());
        }
        if (request.getPeriodFrom() != null || request.getPeriodTo() != null) {
            LocalDate newFrom = request.getPeriodFrom() != null ? request.getPeriodFrom() : newTariff.getPeriodFrom();
            LocalDate newTo = request.getPeriodTo() != null ? request.getPeriodTo() : newTariff.getPeriodTo();
            if (newFrom != null && newTo != null) {
                validatePeriodRange(newFrom, newTo);
                Long companyPoid = UserContext.getCompanyPoid();
                if (tariffHdrRepository.existsOverlappingPeriod(
                        newTariff.getLinePoid(), groupPoid, companyPoid,
                        newFrom, newTo, newTariff.getTransactionPoid())) {
                    throw new ValidationException("Period overlaps with an existing tariff for the same line");
                }
            }
            newTariff.setPeriodFrom(newFrom);
            newTariff.setPeriodTo(newTo);
        }
    }

    private ShipLineTariffHdr buildCopyHeader(ShipLineTariffHdr source, CopyTariffRequestDTO request,
                                              Long groupPoid, Long companyPoid) {
        ShipLineTariffHdr copy = new ShipLineTariffHdr();
        BeanUtils.copyProperties(source, copy,
                "transactionPoid", "docRef", "transactionDate", "periodFrom", "periodTo", "description",
                "createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate");
        copy.setPeriodFrom(request.getPeriodFrom());
        copy.setPeriodTo(request.getPeriodTo());
        if (request.getDescription() != null) {
            copy.setDescription(request.getDescription());
        }
        copy.setGroupPoid(groupPoid);
        copy.setCompanyPoid(companyPoid);
        copy.setDeleted("N");
        String companyCode = tariffHdrRepository.findCompanyCodeByPoid(companyPoid);
        copy.setDocRef(tariffHdrRepository.generateDocRef(companyCode));
        return copy;
    }

    /**
     * Create detail records for all four detail tables
     */
    private void createDetailRecords(Long transactionPoid, LineTariffCreateDTO dto) {
        // Import Demurrage Collectable
        if (dto.getImportDemurrageCollectable() != null) {
            Long maxDetRowId = impDtlRepository.getMaxDetRowId(transactionPoid);
            for (TariffDetailCreateDTO detailDto : dto.getImportDemurrageCollectable()) {
                LineTariffSlabValidator.validate(detailDto);
                validateContainerTypeExists(detailDto.getContainerTypePoid());
                maxDetRowId++;
                ShipLineTariffImpDtl detail = mapper.mapImpDtlCreateDTOToEntity(detailDto, transactionPoid, maxDetRowId);
                impDtlRepository.save(detail);
            }
        }

        // Import Demurrage Payable
        if (dto.getImportDemurragePayable() != null) {
            Long maxDetRowId = impPayDtlRepository.getMaxDetRowId(transactionPoid);
            for (TariffDetailCreateDTO detailDto : dto.getImportDemurragePayable()) {
                LineTariffSlabValidator.validate(detailDto);
                validateContainerTypeExists(detailDto.getContainerTypePoid());
                maxDetRowId++;
                ShipLineTariffImpPayDtl detail = mapper.mapImpPayDtlCreateDTOToEntity(detailDto, transactionPoid, maxDetRowId);
                impPayDtlRepository.save(detail);
            }
        }

        // Export Detention Collectable
        if (dto.getExportDetentionCollectable() != null) {
            Long maxDetRowId = expDtlRepository.getMaxDetRowId(transactionPoid);
            for (TariffDetailCreateDTO detailDto : dto.getExportDetentionCollectable()) {
                LineTariffSlabValidator.validate(detailDto);
                validateContainerTypeExists(detailDto.getContainerTypePoid());
                maxDetRowId++;
                ShipLineTariffExpDtl detail = mapper.mapExpDtlCreateDTOToEntity(detailDto, transactionPoid, maxDetRowId);
                expDtlRepository.save(detail);
            }
        }

        // Export Detention Payable
        if (dto.getExportDetentionPayable() != null) {
            Long maxDetRowId = expPayDtlRepository.getMaxDetRowId(transactionPoid);
            for (TariffDetailCreateDTO detailDto : dto.getExportDetentionPayable()) {
                LineTariffSlabValidator.validate(detailDto);
                validateContainerTypeExists(detailDto.getContainerTypePoid());
                maxDetRowId++;
                ShipLineTariffExpPayDtl detail = mapper.mapExpPayDtlCreateDTOToEntity(detailDto, transactionPoid, maxDetRowId);
                expPayDtlRepository.save(detail);
            }
        }
    }

    /**
     * Update detail records for all four detail tables
     */
    private void updateDetailRecords(Long transactionPoid, LineTariffUpdateDTO dto) {
        // Collect deleted detRowIds from collectable tables to cascade to payable
        Set<Long> deletedImpDetRowIds = dto.getImportDemurrageCollectable() == null ? Set.of() :
                dto.getImportDemurrageCollectable().stream()
                        .filter(d -> "isDeleted".equalsIgnoreCase(d.getActionType()) && d.getDetRowId() != null)
                        .map(TariffDetailUpdateDTO::getDetRowId)
                        .collect(Collectors.toSet());

        Set<Long> deletedExpDetRowIds = dto.getExportDetentionCollectable() == null ? Set.of() :
                dto.getExportDetentionCollectable().stream()
                        .filter(d -> "isDeleted".equalsIgnoreCase(d.getActionType()) && d.getDetRowId() != null)
                        .map(TariffDetailUpdateDTO::getDetRowId)
                        .collect(Collectors.toSet());

        // Cascade deletes to payable lists
        if (!deletedImpDetRowIds.isEmpty() && dto.getImportDemurragePayable() != null) {
            dto.getImportDemurragePayable().stream()
                    .filter(d -> deletedImpDetRowIds.contains(d.getDetRowId()))
                    .forEach(d -> d.setActionType("isDeleted"));
        }
        if (!deletedExpDetRowIds.isEmpty() && dto.getExportDetentionPayable() != null) {
            dto.getExportDetentionPayable().stream()
                    .filter(d -> deletedExpDetRowIds.contains(d.getDetRowId()))
                    .forEach(d -> d.setActionType("isDeleted"));
        }

        updateDetailRecordsImpDtl(transactionPoid, dto.getImportDemurrageCollectable());
        updateDetailRecordsImpPayDtl(transactionPoid, dto.getImportDemurragePayable());
        updateDetailRecordsExpDtl(transactionPoid, dto.getExportDetentionCollectable());
        updateDetailRecordsExpPayDtl(transactionPoid, dto.getExportDetentionPayable());
    }

    /**
     * Update Import Demurrage Collectable detail records
     */
    private void updateDetailRecordsImpDtl(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos) {
        if (detailDtos == null) {
            return;
        }
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        List<String> logEntries = new ArrayList<>();
        List<ShipLineTariffImpDtl> toUpdate = new ArrayList<>();
        List<ShipLineTariffImpDtl> toDelete = new ArrayList<>();
        List<LogRequestDto<ShipLineTariffImpDtl>> logRequests = new ArrayList<>();
        long maxDetRowId = impDtlRepository.getMaxDetRowId(transactionPoid);

        for (TariffDetailUpdateDTO dto : detailDtos) {
            String action = resolveDetailAction(dto.getActionType(), dto.getDetRowId());
            switch (action) {
                case ACTION_ISCREATED -> {
                    LineTariffSlabValidator.validate(dto);
                    validateContainerTypeExists(dto.getContainerTypePoid());
                    maxDetRowId++;
                    ShipLineTariffImpDtl entity = mapper.mapImpDtlUpdateDTOToEntity(dto, transactionPoid, maxDetRowId);
                    impDtlRepository.save(entity);
                    logEntries.add(String.format("Row Created on %s with DetRowId: %s", IMP_COLLECTABLE_DETAIL, maxDetRowId));
                }
                case ACTION_ISUPDATED -> {
                    if (dto.getDetRowId() == null) {
                        continue;
                    }
                    LineTariffSlabValidator.validate(dto);
                    ShipLineTariffImpDtl existing = impDtlRepository
                            .findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException(TARIFF_DETAIL, DET_ROW_ID, dto.getDetRowId().toString()));
                    ShipLineTariffImpDtl oldEntity = new ShipLineTariffImpDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    mapper.updateImpDtlFromDTO(dto, existing);
                    if (hasEntityChanges(oldEntity, existing, ShipLineTariffImpDtl.class)) {
                        toUpdate.add(existing);
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipLineTariffImpDtl.class, docId, docKeyPoid,
                                String.format(LOG_KEY_ID_FORMAT, transactionPoid, dto.getDetRowId())));
                    }
                }
                case ACTION_ISDELETED -> {
                    if (dto.getDetRowId() != null) {
                        impDtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                                .ifPresent(entity -> {
                                    toDelete.add(entity);
                                    logEntries.add(String.format("Row Deleted on %s with DetRowId: %s",
                                            IMP_COLLECTABLE_DETAIL, dto.getDetRowId()));
                                });
                    }
                }
                default -> { /* no changes */ }
            }
        }

        persistDetailUpdates(impDtlRepository, toUpdate, logRequests);
        persistDetailDeletes(impDtlRepository, toDelete, docId, docKeyPoid);
        logSummaryEntries(logEntries, docId, docKeyPoid);
    }

    /**
     * Update Import Demurrage Payable detail records
     */
    private void updateDetailRecordsImpPayDtl(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos) {
        if (detailDtos == null) {
            return;
        }
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        List<String> logEntries = new ArrayList<>();
        List<ShipLineTariffImpPayDtl> toUpdate = new ArrayList<>();
        List<ShipLineTariffImpPayDtl> toDelete = new ArrayList<>();
        List<LogRequestDto<ShipLineTariffImpPayDtl>> logRequests = new ArrayList<>();
        long maxDetRowId = impPayDtlRepository.getMaxDetRowId(transactionPoid);

        for (TariffDetailUpdateDTO dto : detailDtos) {
            String action = resolveDetailAction(dto.getActionType(), dto.getDetRowId());
            switch (action) {
                case ACTION_ISCREATED -> {
                    LineTariffSlabValidator.validate(dto);
                    validateContainerTypeExists(dto.getContainerTypePoid());
                    maxDetRowId++;
                    ShipLineTariffImpPayDtl entity = mapper.mapImpPayDtlUpdateDTOToEntity(dto, transactionPoid, maxDetRowId);
                    impPayDtlRepository.save(entity);
                    logEntries.add(String.format("Row Created on %s with DetRowId: %s", IMP_PAYABLE_DETAIL, maxDetRowId));
                }
                case ACTION_ISUPDATED -> {
                    if (dto.getDetRowId() == null) {
                        continue;
                    }
                    LineTariffSlabValidator.validate(dto);
                    ShipLineTariffImpPayDtl existing = impPayDtlRepository
                            .findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException(TARIFF_DETAIL, DET_ROW_ID, dto.getDetRowId().toString()));
                    ShipLineTariffImpPayDtl oldEntity = new ShipLineTariffImpPayDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    mapper.updateImpPayDtlFromDTO(dto, existing);
                    if (hasEntityChanges(oldEntity, existing, ShipLineTariffImpPayDtl.class)) {
                        toUpdate.add(existing);
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipLineTariffImpPayDtl.class, docId, docKeyPoid,
                                String.format(LOG_KEY_ID_FORMAT, transactionPoid, dto.getDetRowId())));
                    }
                }
                case ACTION_ISDELETED -> {
                    if (dto.getDetRowId() != null) {
                        impPayDtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                                .ifPresent(entity -> {
                                    toDelete.add(entity);
                                    logEntries.add(String.format("Row Deleted on %s with DetRowId: %s",
                                            IMP_PAYABLE_DETAIL, dto.getDetRowId()));
                                });
                    }
                }
                default -> { /* no changes */ }
            }
        }

        persistDetailUpdates(impPayDtlRepository, toUpdate, logRequests);
        persistDetailDeletes(impPayDtlRepository, toDelete, docId, docKeyPoid);
        logSummaryEntries(logEntries, docId, docKeyPoid);
    }

    /**
     * Update Export Detention Collectable detail records
     */
    private void updateDetailRecordsExpDtl(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos) {
        if (detailDtos == null) {
            return;
        }
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        List<String> logEntries = new ArrayList<>();
        List<ShipLineTariffExpDtl> toUpdate = new ArrayList<>();
        List<ShipLineTariffExpDtl> toDelete = new ArrayList<>();
        List<LogRequestDto<ShipLineTariffExpDtl>> logRequests = new ArrayList<>();
        long maxDetRowId = expDtlRepository.getMaxDetRowId(transactionPoid);

        for (TariffDetailUpdateDTO dto : detailDtos) {
            String action = resolveDetailAction(dto.getActionType(), dto.getDetRowId());
            switch (action) {
                case ACTION_ISCREATED -> {
                    LineTariffSlabValidator.validate(dto);
                    validateContainerTypeExists(dto.getContainerTypePoid());
                    maxDetRowId++;
                    ShipLineTariffExpDtl entity = mapper.mapExpDtlUpdateDTOToEntity(dto, transactionPoid, maxDetRowId);
                    expDtlRepository.save(entity);
                    logEntries.add(String.format("Row Created on %s with DetRowId: %s", EXP_COLLECTABLE_DETAIL, maxDetRowId));
                }
                case ACTION_ISUPDATED -> {
                    if (dto.getDetRowId() == null) {
                        continue;
                    }
                    LineTariffSlabValidator.validate(dto);
                    ShipLineTariffExpDtl existing = expDtlRepository
                            .findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException(TARIFF_DETAIL, DET_ROW_ID, dto.getDetRowId().toString()));
                    ShipLineTariffExpDtl oldEntity = new ShipLineTariffExpDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    mapper.updateExpDtlFromDTO(dto, existing);
                    if (hasEntityChanges(oldEntity, existing, ShipLineTariffExpDtl.class)) {
                        toUpdate.add(existing);
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipLineTariffExpDtl.class, docId, docKeyPoid,
                                String.format(LOG_KEY_ID_FORMAT, transactionPoid, dto.getDetRowId())));
                    }
                }
                case ACTION_ISDELETED -> {
                    if (dto.getDetRowId() != null) {
                        expDtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                                .ifPresent(entity -> {
                                    toDelete.add(entity);
                                    logEntries.add(String.format("Row Deleted on %s with DetRowId: %s",
                                            EXP_COLLECTABLE_DETAIL, dto.getDetRowId()));
                                });
                    }
                }
                default -> { /* no changes */ }
            }
        }

        persistDetailUpdates(expDtlRepository, toUpdate, logRequests);
        persistDetailDeletes(expDtlRepository, toDelete, docId, docKeyPoid);
        logSummaryEntries(logEntries, docId, docKeyPoid);
    }

    /**
     * Update Export Detention Payable detail records
     */
    private void updateDetailRecordsExpPayDtl(Long transactionPoid, List<TariffDetailUpdateDTO> detailDtos) {
        if (detailDtos == null) {
            return;
        }
        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        List<String> logEntries = new ArrayList<>();
        List<ShipLineTariffExpPayDtl> toUpdate = new ArrayList<>();
        List<ShipLineTariffExpPayDtl> toDelete = new ArrayList<>();
        List<LogRequestDto<ShipLineTariffExpPayDtl>> logRequests = new ArrayList<>();
        long maxDetRowId = expPayDtlRepository.getMaxDetRowId(transactionPoid);

        for (TariffDetailUpdateDTO dto : detailDtos) {
            String action = resolveDetailAction(dto.getActionType(), dto.getDetRowId());
            switch (action) {
                case ACTION_ISCREATED -> {
                    LineTariffSlabValidator.validate(dto);
                    validateContainerTypeExists(dto.getContainerTypePoid());
                    maxDetRowId++;
                    ShipLineTariffExpPayDtl entity = mapper.mapExpPayDtlUpdateDTOToEntity(dto, transactionPoid, maxDetRowId);
                    expPayDtlRepository.save(entity);
                    logEntries.add(String.format("Row Created on %s with DetRowId: %s", EXP_PAYABLE_DETAIL, maxDetRowId));
                }
                case ACTION_ISUPDATED -> {
                    if (dto.getDetRowId() == null) {
                        continue;
                    }
                    LineTariffSlabValidator.validate(dto);
                    ShipLineTariffExpPayDtl existing = expPayDtlRepository
                            .findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                            .orElseThrow(() -> new ResourceNotFoundException(TARIFF_DETAIL, DET_ROW_ID, dto.getDetRowId().toString()));
                    ShipLineTariffExpPayDtl oldEntity = new ShipLineTariffExpPayDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    mapper.updateExpPayDtlFromDTO(dto, existing);
                    if (hasEntityChanges(oldEntity, existing, ShipLineTariffExpPayDtl.class)) {
                        toUpdate.add(existing);
                        logRequests.add(new LogRequestDto<>(oldEntity, existing, ShipLineTariffExpPayDtl.class, docId, docKeyPoid,
                                String.format(LOG_KEY_ID_FORMAT, transactionPoid, dto.getDetRowId())));
                    }
                }
                case ACTION_ISDELETED -> {
                    if (dto.getDetRowId() != null) {
                        expPayDtlRepository.findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                                .ifPresent(entity -> {
                                    toDelete.add(entity);
                                    logEntries.add(String.format("Row Deleted on %s with DetRowId: %s",
                                            EXP_PAYABLE_DETAIL, dto.getDetRowId()));
                                });
                    }
                }
                default -> { /* no changes */ }
            }
        }

        persistDetailUpdates(expPayDtlRepository, toUpdate, logRequests);
        persistDetailDeletes(expPayDtlRepository, toDelete, docId, docKeyPoid);
        logSummaryEntries(logEntries, docId, docKeyPoid);
    }

    private String resolveDetailAction(String rawAction, Long detRowId) {
        if (rawAction != null) {
            String action = rawAction.trim().toLowerCase();
            if (ACTION_ISDELETED.equals(action)) {
                return ACTION_ISDELETED;
            }
            if (ACTION_ISCREATED.equals(action)) {
                return ACTION_ISCREATED;
            }
            if (ACTION_ISUPDATED.equals(action)) {
                return ACTION_ISUPDATED;
            }
        }
        return detRowId != null ? ACTION_ISUPDATED : ACTION_ISCREATED;
    }

    private <T> void persistDetailUpdates(org.springframework.data.jpa.repository.JpaRepository<T, ?> repository,
                                          List<T> entities,
                                          List<LogRequestDto<T>> logRequests) {
        if (!entities.isEmpty()) {
            repository.saveAll(entities);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }
    }

    private <T> void persistDetailDeletes(org.springframework.data.jpa.repository.JpaRepository<T, ?> repository,
                                          List<T> entities,
                                          String docId,
                                          String docKeyPoid) {
        if (!entities.isEmpty()) {
            repository.deleteAllInBatch(entities);
            entities.forEach(entity -> loggingService.logDelete(entity, docId, docKeyPoid));
        }
    }

    private void logSummaryEntries(List<String> logEntries, String docId, String docKeyPoid) {
        if (logEntries != null) {
            logEntries.forEach(entry -> loggingService.createLogSummaryEntry(docId, docKeyPoid, entry));
        }
    }

    private <T> boolean hasEntityChanges(T oldEntity, T newEntity, Class<T> entityClass) {
        return !DiffUtil.createDiffList(oldEntity, newEntity, entityClass).isEmpty();
    }

    private void copyDetailRecords(Long sourceTransactionPoid, Long targetTransactionPoid) {
        copyImpDtlRecords(sourceTransactionPoid, targetTransactionPoid);
        copyImpPayDtlRecords(sourceTransactionPoid, targetTransactionPoid);
        copyExpDtlRecords(sourceTransactionPoid, targetTransactionPoid);
        copyExpPayDtlRecords(sourceTransactionPoid, targetTransactionPoid);
    }

    private void copyImpDtlRecords(Long sourceTransactionPoid, Long targetTransactionPoid) {
        List<ShipLineTariffImpDtl> sourceList = impDtlRepository.findByTransactionPoidOrderByDetRowId(sourceTransactionPoid);
        long maxDetRowId = 0L;
        for (ShipLineTariffImpDtl source : sourceList) {
            maxDetRowId++;
            ShipLineTariffImpDtl target = new ShipLineTariffImpDtl();
            target.setTransactionPoid(targetTransactionPoid);
            target.setDetRowId(maxDetRowId);
            target.setContainerTypePoid(source.getContainerTypePoid());
            target.setFreeDays(source.getFreeDays());
            target.setSlab1Tilldays(source.getSlab1Tilldays());
            target.setSlab1Rate(source.getSlab1Rate());
            target.setSlab2Tilldays(source.getSlab2Tilldays());
            target.setSlab2Rate(source.getSlab2Rate());
            target.setSlab3Tilldays(source.getSlab3Tilldays());
            target.setSlab3Rate(source.getSlab3Rate());
            target.setSlab4Tilldays(source.getSlab4Tilldays());
            target.setSlab4Rate(source.getSlab4Rate());
            target.setSlab5Tilldays(source.getSlab5Tilldays());
            target.setSlab5Rate(source.getSlab5Rate());
            target.setSlab6Tilldays(source.getSlab6Tilldays());
            target.setSlab6Rate(source.getSlab6Rate());
            target.setSlab7Tilldays(source.getSlab7Tilldays());
            target.setSlab7Rate(source.getSlab7Rate());
            impDtlRepository.save(target);
        }
    }

    private void copyImpPayDtlRecords(Long sourceTransactionPoid, Long targetTransactionPoid) {
        List<ShipLineTariffImpPayDtl> sourceList = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(sourceTransactionPoid);
        long maxDetRowId = 0L;
        for (ShipLineTariffImpPayDtl source : sourceList) {
            maxDetRowId++;
            ShipLineTariffImpPayDtl target = ShipLineTariffImpPayDtl.builder()
                    .transactionPoid(targetTransactionPoid)
                    .detRowId(maxDetRowId)
                    .containerTypePoid(source.getContainerTypePoid())
                    .freeDays(source.getFreeDays())
                    .slab1Tilldays(source.getSlab1Tilldays())
                    .slab1Rate(source.getSlab1Rate())
                    .slab2Tilldays(source.getSlab2Tilldays())
                    .slab2Rate(source.getSlab2Rate())
                    .slab3Tilldays(source.getSlab3Tilldays())
                    .slab3Rate(source.getSlab3Rate())
                    .slab4Tilldays(source.getSlab4Tilldays())
                    .slab4Rate(source.getSlab4Rate())
                    .slab5Tilldays(source.getSlab5Tilldays())
                    .slab5Rate(source.getSlab5Rate())
                    .slab6Tilldays(source.getSlab6Tilldays())
                    .slab6Rate(source.getSlab6Rate())
                    .slab7Tilldays(source.getSlab7Tilldays())
                    .slab7Rate(source.getSlab7Rate())
                    .build();
            impPayDtlRepository.save(target);
        }
    }

    private void copyExpDtlRecords(Long sourceTransactionPoid, Long targetTransactionPoid) {
        List<ShipLineTariffExpDtl> sourceList = expDtlRepository.findByTransactionPoidOrderByDetRowId(sourceTransactionPoid);
        long maxDetRowId = 0L;
        for (ShipLineTariffExpDtl source : sourceList) {
            maxDetRowId++;
            ShipLineTariffExpDtl target = new ShipLineTariffExpDtl();
            target.setTransactionPoid(targetTransactionPoid);
            target.setDetRowId(maxDetRowId);
            target.setContainerTypePoid(source.getContainerTypePoid());
            target.setFreeDays(source.getFreeDays());
            target.setSlab1Tilldays(source.getSlab1Tilldays());
            target.setSlab1Rate(source.getSlab1Rate());
            target.setSlab2Tilldays(source.getSlab2Tilldays());
            target.setSlab2Rate(source.getSlab2Rate());
            target.setSlab3Tilldays(source.getSlab3Tilldays());
            target.setSlab3Rate(source.getSlab3Rate());
            target.setSlab4Tilldays(source.getSlab4Tilldays());
            target.setSlab4Rate(source.getSlab4Rate());
            target.setSlab5Tilldays(source.getSlab5Tilldays());
            target.setSlab5Rate(source.getSlab5Rate());
            target.setSlab6Tilldays(source.getSlab6Tilldays());
            target.setSlab6Rate(source.getSlab6Rate());
            target.setSlab7Tilldays(source.getSlab7Tilldays());
            target.setSlab7Rate(source.getSlab7Rate());
            expDtlRepository.save(target);
        }
    }

    private void copyExpPayDtlRecords(Long sourceTransactionPoid, Long targetTransactionPoid) {
        List<ShipLineTariffExpPayDtl> sourceList = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(sourceTransactionPoid);
        long maxDetRowId = 0L;
        for (ShipLineTariffExpPayDtl source : sourceList) {
            maxDetRowId++;
            ShipLineTariffExpPayDtl target = new ShipLineTariffExpPayDtl();
            target.setTransactionPoid(targetTransactionPoid);
            target.setDetRowId(maxDetRowId);
            target.setContainerTypePoid(source.getContainerTypePoid());
            target.setFreeDays(source.getFreeDays());
            target.setSlab1Tilldays(source.getSlab1Tilldays());
            target.setSlab1Rate(source.getSlab1Rate());
            target.setSlab2Tilldays(source.getSlab2Tilldays());
            target.setSlab2Rate(source.getSlab2Rate());
            target.setSlab3Tilldays(source.getSlab3Tilldays());
            target.setSlab3Rate(source.getSlab3Rate());
            target.setSlab4Tilldays(source.getSlab4Tilldays());
            target.setSlab4Rate(source.getSlab4Rate());
            target.setSlab5Tilldays(source.getSlab5Tilldays());
            target.setSlab5Rate(source.getSlab5Rate());
            target.setSlab6Tilldays(source.getSlab6Tilldays());
            target.setSlab6Rate(source.getSlab6Rate());
            target.setSlab7Tilldays(source.getSlab7Tilldays());
            target.setSlab7Rate(source.getSlab7Rate());
            expPayDtlRepository.save(target);
        }
    }

    /**
     * Validate TariffCreateDTO
     */
    private void validateTariffCreateDTO(LineTariffCreateDTO dto, Long groupPoid) {
        validatePeriodRange(dto.getPeriodFrom(), dto.getPeriodTo());
        validateOverlapForCreate(dto, groupPoid);
        validateDocRefUniqueness(dto.getDocRef());
        validateMutuallyExclusiveFlags(
                dto.getDmgFromSameday(),
                dto.getDmgFromNextday(),
                dto.getDtnFromSameday(),
                dto.getDtnFromNextday()
        );
    }

    /**
     * Validate TariffUpdateDTO
     */
    private void validateTariffUpdateDTO(LineTariffUpdateDTO dto, Long excludeTransactionPoid, Long groupPoid) {
        validatePeriodRange(dto.getPeriodFrom(), dto.getPeriodTo());

        // Get existing tariff to check linePoid if not provided in update
        ShipLineTariffHdr existing = tariffHdrRepository.findById(excludeTransactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, excludeTransactionPoid.toString()));

        if (!isPeriodOrLineUnchanged(dto, existing)) {
            Long linePoid = dto.getLinePoid() != null ? dto.getLinePoid() : existing.getLinePoid();
            LocalDate periodFrom = dto.getPeriodFrom() != null ? dto.getPeriodFrom() : existing.getPeriodFrom();
            LocalDate periodTo = dto.getPeriodTo() != null ? dto.getPeriodTo() : existing.getPeriodTo();
            validateOverlapForUpdate(linePoid, periodFrom, periodTo, groupPoid, excludeTransactionPoid);
        }

        // Check if document reference already exists (excluding current transaction)
        if (dto.getDocRef() != null
                && !dto.getDocRef().trim().isEmpty()
                && tariffHdrRepository.existsByDocRefExcludingPoid(dto.getDocRef().trim(), excludeTransactionPoid)) {
            throw new ValidationException("Document reference already exists");
        }

        // Validate mutually exclusive flags
        String dmgFromSameday = dto.getDmgFromSameday() != null ? dto.getDmgFromSameday() : existing.getDmgFromSameday();
        String dmgFromNextday = dto.getDmgFromNextday() != null ? dto.getDmgFromNextday() : existing.getDmgFromNextday();
        String dtnFromSameday = dto.getDtnFromSameday() != null ? dto.getDtnFromSameday() : existing.getDtnFromSameday();
        String dtnFromNextday = dto.getDtnFromNextday() != null ? dto.getDtnFromNextday() : existing.getDtnFromNextday();
        validateMutuallyExclusiveFlags(dmgFromSameday, dmgFromNextday, dtnFromSameday, dtnFromNextday);
    }

    private void validatePeriodRange(LocalDate periodFrom, LocalDate periodTo) {
        if (periodFrom != null && periodTo != null && periodFrom.isAfter(periodTo)) {
            throw new ValidationException("Period from date must be less than or equal to period to date");
        }
    }

   
    private boolean isPeriodOrLineUnchanged(LineTariffUpdateDTO dto, ShipLineTariffHdr existing) {
        boolean lineUnchanged = dto.getLinePoid() == null || dto.getLinePoid().equals(existing.getLinePoid());
        boolean periodFromUnchanged = dto.getPeriodFrom() == null || dto.getPeriodFrom().equals(existing.getPeriodFrom());
        boolean periodToUnchanged = dto.getPeriodTo() == null || dto.getPeriodTo().equals(existing.getPeriodTo());
        return lineUnchanged && periodFromUnchanged && periodToUnchanged;
    }

    private void validateOverlapForCreate(LineTariffCreateDTO dto, Long groupPoid) {
        if (dto.getLinePoid() != null && dto.getPeriodFrom() != null && dto.getPeriodTo() != null) {
            Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();
            if (tariffHdrRepository.existsOverlappingPeriod(
                    dto.getLinePoid(),
                    groupPoid,
                    companyPoid,
                    dto.getPeriodFrom(),
                    dto.getPeriodTo(),
                    null)) {
                throw new ValidationException("Period overlaps with an existing tariff for the same line");
            }
        }
    }

    private void validateOverlapForUpdate(
            Long linePoid,
            LocalDate periodFrom,
            LocalDate periodTo,
            Long groupPoid,
            Long excludeTransactionPoid
    ) {
        if (linePoid != null && periodFrom != null && periodTo != null) {
            Long companyPoid = com.asg.common.lib.security.util.UserContext.getCompanyPoid();
            if (tariffHdrRepository.existsOverlappingPeriod(
                    linePoid,
                    groupPoid,
                    companyPoid,
                    periodFrom,
                    periodTo,
                    excludeTransactionPoid)) {
                throw new ValidationException("Period overlaps with an existing tariff for the same line");
            }
        }
    }

    private void validateDocRefUniqueness(String docRef) {
        if (docRef != null && !docRef.trim().isEmpty()) {
            String trimmedDocRef = docRef.trim();
            if (tariffHdrRepository.existsByDocRef(trimmedDocRef)) {
                throw new ValidationException("Document Reference " + trimmedDocRef + " already exists. Please use a different reference.");
            }
        }
    }

    @Override
    @Transactional(timeout = 120)
    public LoadContainerTypesResponseDto loadContainerTypes(Long transactionPoid, String type) {
        log.info("Loading container types for transactionPoid: {}, type: {}", transactionPoid, type);

        ShipLineTariffHdr hdr = tariffHdrRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(LINE_TARIFF, TRANSACTION_POID, transactionPoid.toString()));

        String normalizedType = type != null ? type.trim().toUpperCase() : "";
        Set<Long> existingContainerTypePoids;
        if ("IMP".equals(normalizedType)) {
            existingContainerTypePoids = collectImpContainerTypePoids(transactionPoid);
            impDtlRepository.bulkInsertFromLine(transactionPoid, hdr.getLinePoid());
        } else if ("EXP".equals(normalizedType)) {
            existingContainerTypePoids = collectExpContainerTypePoids(transactionPoid);
            expDtlRepository.bulkInsertFromLine(transactionPoid, hdr.getLinePoid());
        } else {
            throw new ValidationException("Invalid type. Use IMP for Import Demurrage or EXP for Export Detention.");
        }

        List<TariffDetailDto> containerTypes = mapNewlyLoadedContainerTypes(
                transactionPoid, normalizedType, existingContainerTypePoids);

        return LoadContainerTypesResponseDto.builder()
                .transactionPoid(transactionPoid)
                .type(normalizedType)
                .containerTypes(containerTypes)
                .build();
    }

    private Set<Long> collectImpContainerTypePoids(Long transactionPoid) {
        return impDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid).stream()
                .map(ShipLineTariffImpDtl::getContainerTypePoid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<Long> collectExpContainerTypePoids(Long transactionPoid) {
        return expDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid).stream()
                .map(ShipLineTariffExpDtl::getContainerTypePoid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    
    private List<TariffDetailDto> mapNewlyLoadedContainerTypes(
            Long transactionPoid, String type, Set<Long> existingContainerTypePoids) {
        if ("IMP".equals(type)) {
            List<ShipLineTariffImpDtl> newlyLoaded = impDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid).stream()
                    .filter(d -> d.getContainerTypePoid() != null && !existingContainerTypePoids.contains(d.getContainerTypePoid()))
                    .toList();
            if (newlyLoaded.isEmpty()) {
                return List.of();
            }
            Map<Long, ShipContainerTypeMaster> containerTypeMap =
                    buildContainerTypeMap(newlyLoaded, List.of(), List.of(), List.of());
            return newlyLoaded.stream()
                    .map(d -> mapper.mapImpDtlToDto(d, containerTypeMap))
                    .toList();
        }

        List<ShipLineTariffExpDtl> newlyLoaded = expDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid).stream()
                .filter(d -> d.getContainerTypePoid() != null && !existingContainerTypePoids.contains(d.getContainerTypePoid()))
                .toList();
        if (newlyLoaded.isEmpty()) {
            return List.of();
        }
        Map<Long, ShipContainerTypeMaster> containerTypeMap =
                buildContainerTypeMap(List.of(), List.of(), newlyLoaded, List.of());
        return newlyLoaded.stream()
                .map(d -> mapper.mapExpDtlToDto(d, containerTypeMap))
                .toList();
    }

    private Map<Long, ShipContainerTypeMaster> buildContainerTypeMap(
            List<ShipLineTariffImpDtl> impDtlList,
            List<ShipLineTariffImpPayDtl> impPayDtlList,
            List<ShipLineTariffExpDtl> expDtlList,
            List<ShipLineTariffExpPayDtl> expPayDtlList) {

        Set<Long> poids = Stream.of(
                impDtlList.stream().map(ShipLineTariffImpDtl::getContainerTypePoid),
                impPayDtlList.stream().map(ShipLineTariffImpPayDtl::getContainerTypePoid),
                expDtlList.stream().map(ShipLineTariffExpDtl::getContainerTypePoid),
                expPayDtlList.stream().map(ShipLineTariffExpPayDtl::getContainerTypePoid)
        ).flatMap(s -> s).filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        if (poids.isEmpty()) return Map.of();

        return containerTypeRepository.findAllById(poids).stream()
                .collect(Collectors.toMap(ShipContainerTypeMaster::getContainerTypePoid, ct -> ct));
    }

    private void validateContainerTypeExists(Long containerTypePoid) {
        if (containerTypePoid == null || containerTypePoid <= 0) {
            throw new ValidationException("Container type is required");
        }
        if (!containerTypeRepository.existsById(containerTypePoid)) {
            throw new ValidationException("Invalid container type: " + containerTypePoid);
        }
    }

    private void validateMutuallyExclusiveFlags(
            String dmgFromSameday,
            String dmgFromNextday,
            String dtnFromSameday,
            String dtnFromNextday
    ) {
        if ("Y".equals(dmgFromSameday) && "Y".equals(dmgFromNextday)) {
            throw new ValidationException("Same day and Next day both cannot be selected for Demurrage");
        }
        if ("Y".equals(dtnFromSameday) && "Y".equals(dtnFromNextday)) {
            throw new ValidationException("Same day and Next day both cannot be selected for Detention");
        }
    }

    @Override
    @Transactional
    public void copySlabsToPayable(Long id, String type) {
        log.info("Copying slabs to payable for transactionPoid: {}, type: {}", id, type);
        if ("DMG".equalsIgnoreCase(type)) {
            copyImpSlabsToPayable(id, true);
        } else if ("DTN".equalsIgnoreCase(type)) {
            copyExpSlabsToPayable(id, true);
        } else {
            throw new ValidationException("Invalid type. Must be DMG or DTN");
        }
        log.info("Successfully copied slabs to payable for transactionPoid: {}, type: {}", id, type);
    }

    private void copyImpSlabsToPayable(Long transactionPoid, boolean overwriteExisting) {
        List<ShipLineTariffImpDtl> collectables = impDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<ShipLineTariffImpPayDtl> payables = impPayDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Map<Long, ShipLineTariffImpPayDtl> payableByContainerType = payables.stream()
                .filter(p -> p.getContainerTypePoid() != null)
                .collect(Collectors.toMap(ShipLineTariffImpPayDtl::getContainerTypePoid, p -> p, (a, b) -> a));
        long maxDetRowId = payables.stream()
                .map(ShipLineTariffImpPayDtl::getDetRowId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
        for (ShipLineTariffImpDtl col : collectables) {
            if (!isCollectableRowEligibleForPayableCopy(col.getContainerTypePoid(), col.getFreeDays(),
                    col.getSlab1Tilldays(), col.getSlab1Rate(),
                    col.getSlab2Tilldays(), col.getSlab2Rate(),
                    col.getSlab3Tilldays(), col.getSlab3Rate(),
                    col.getSlab4Tilldays(), col.getSlab4Rate(),
                    col.getSlab5Tilldays(), col.getSlab5Rate(),
                    col.getSlab6Tilldays(), col.getSlab6Rate(),
                    col.getSlab7Tilldays(), col.getSlab7Rate())) {
                continue;
            }
            ShipLineTariffImpPayDtl pay = payableByContainerType.get(col.getContainerTypePoid());
            if (pay == null) {
                pay = new ShipLineTariffImpPayDtl();
                pay.setTransactionPoid(transactionPoid);
                pay.setDetRowId(++maxDetRowId);
                pay.setContainerTypePoid(col.getContainerTypePoid());
                payableByContainerType.put(col.getContainerTypePoid(), pay);
            } else if (!overwriteExisting) {
                continue;
            }
            copyImpSlabFields(col, pay);
            impPayDtlRepository.save(pay);
        }
    }

    private void copyExpSlabsToPayable(Long transactionPoid, boolean overwriteExisting) {
        List<ShipLineTariffExpDtl> collectables = expDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        List<ShipLineTariffExpPayDtl> payables = expPayDtlRepository.findByTransactionPoidOrderByDetRowId(transactionPoid);
        Map<Long, ShipLineTariffExpPayDtl> payableByContainerType = payables.stream()
                .filter(p -> p.getContainerTypePoid() != null)
                .collect(Collectors.toMap(ShipLineTariffExpPayDtl::getContainerTypePoid, p -> p, (a, b) -> a));
        long maxDetRowId = payables.stream()
                .map(ShipLineTariffExpPayDtl::getDetRowId)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
        for (ShipLineTariffExpDtl col : collectables) {
            if (!isCollectableRowEligibleForPayableCopy(col.getContainerTypePoid(), col.getFreeDays(),
                    col.getSlab1Tilldays(), col.getSlab1Rate(),
                    col.getSlab2Tilldays(), col.getSlab2Rate(),
                    col.getSlab3Tilldays(), col.getSlab3Rate(),
                    col.getSlab4Tilldays(), col.getSlab4Rate(),
                    col.getSlab5Tilldays(), col.getSlab5Rate(),
                    col.getSlab6Tilldays(), col.getSlab6Rate(),
                    col.getSlab7Tilldays(), col.getSlab7Rate())) {
                continue;
            }
            ShipLineTariffExpPayDtl pay = payableByContainerType.get(col.getContainerTypePoid());
            if (pay == null) {
                pay = new ShipLineTariffExpPayDtl();
                pay.setTransactionPoid(transactionPoid);
                pay.setDetRowId(++maxDetRowId);
                pay.setContainerTypePoid(col.getContainerTypePoid());
                payableByContainerType.put(col.getContainerTypePoid(), pay);
            } else if (!overwriteExisting) {
                continue;
            }
            copyExpSlabFields(col, pay);
            expPayDtlRepository.save(pay);
        }
    }

    private boolean isCollectableRowEligibleForPayableCopy(Long containerTypePoid, Integer freeDays,
                                                           Integer slab1Tilldays, BigDecimal slab1Rate,
                                                           Integer slab2Tilldays, BigDecimal slab2Rate,
                                                           Integer slab3Tilldays, BigDecimal slab3Rate,
                                                           Integer slab4Tilldays, BigDecimal slab4Rate,
                                                           Integer slab5Tilldays, BigDecimal slab5Rate,
                                                           Integer slab6Tilldays, BigDecimal slab6Rate,
                                                           Integer slab7Tilldays, BigDecimal slab7Rate) {
        return containerTypePoid != null && hasTariffDetailData(
                freeDays, slab1Tilldays, slab1Rate, slab2Tilldays, slab2Rate,
                slab3Tilldays, slab3Rate, slab4Tilldays, slab4Rate,
                slab5Tilldays, slab5Rate, slab6Tilldays, slab6Rate,
                slab7Tilldays, slab7Rate);
    }

    private void copyImpSlabFields(ShipLineTariffImpDtl col, ShipLineTariffImpPayDtl pay) {
        pay.setFreeDays(col.getFreeDays());
        pay.setSlab1Tilldays(col.getSlab1Tilldays());
        pay.setSlab1Rate(col.getSlab1Rate());
        pay.setSlab2Tilldays(col.getSlab2Tilldays());
        pay.setSlab2Rate(col.getSlab2Rate());
        pay.setSlab3Tilldays(col.getSlab3Tilldays());
        pay.setSlab3Rate(col.getSlab3Rate());
        pay.setSlab4Tilldays(col.getSlab4Tilldays());
        pay.setSlab4Rate(col.getSlab4Rate());
        pay.setSlab5Tilldays(col.getSlab5Tilldays());
        pay.setSlab5Rate(col.getSlab5Rate());
        pay.setSlab6Tilldays(col.getSlab6Tilldays());
        pay.setSlab6Rate(col.getSlab6Rate());
        pay.setSlab7Tilldays(col.getSlab7Tilldays());
        pay.setSlab7Rate(col.getSlab7Rate());
    }

    private void copyExpSlabFields(ShipLineTariffExpDtl col, ShipLineTariffExpPayDtl pay) {
        pay.setFreeDays(col.getFreeDays());
        pay.setSlab1Tilldays(col.getSlab1Tilldays());
        pay.setSlab1Rate(col.getSlab1Rate());
        pay.setSlab2Tilldays(col.getSlab2Tilldays());
        pay.setSlab2Rate(col.getSlab2Rate());
        pay.setSlab3Tilldays(col.getSlab3Tilldays());
        pay.setSlab3Rate(col.getSlab3Rate());
        pay.setSlab4Tilldays(col.getSlab4Tilldays());
        pay.setSlab4Rate(col.getSlab4Rate());
        pay.setSlab5Tilldays(col.getSlab5Tilldays());
        pay.setSlab5Rate(col.getSlab5Rate());
        pay.setSlab6Tilldays(col.getSlab6Tilldays());
        pay.setSlab6Rate(col.getSlab6Rate());
        pay.setSlab7Tilldays(col.getSlab7Tilldays());
        pay.setSlab7Rate(col.getSlab7Rate());
    }

    private boolean hasTariffDetailData(Integer freeDays, Integer slab1Tilldays, BigDecimal slab1Rate,
                                        Integer slab2Tilldays, BigDecimal slab2Rate,
                                        Integer slab3Tilldays, BigDecimal slab3Rate,
                                        Integer slab4Tilldays, BigDecimal slab4Rate,
                                        Integer slab5Tilldays, BigDecimal slab5Rate,
                                        Integer slab6Tilldays, BigDecimal slab6Rate,
                                        Integer slab7Tilldays, BigDecimal slab7Rate) {
        return freeDays != null
                || slab1Tilldays != null || slab1Rate != null
                || slab2Tilldays != null || slab2Rate != null
                || slab3Tilldays != null || slab3Rate != null
                || slab4Tilldays != null || slab4Rate != null
                || slab5Tilldays != null || slab5Rate != null
                || slab6Tilldays != null || slab6Rate != null
                || slab7Tilldays != null || slab7Rate != null;
    }
}

