package com.asg.shipping.vesselvoyagecreation.service.impl;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.DateUtil;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.exceptions.ResourceAlreadyExistsException;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.vesselvoyagecreation.dto.*;
import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageHdrEntity;
import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageTranshipDtlEntity;
import com.asg.shipping.vesselvoyagecreation.entity.VwShipEdiExceptionUploadEntity;
import com.asg.shipping.vesselvoyagecreation.entity.VwShipVoyageCurrencyEntity;
import com.asg.shipping.vesselvoyagecreation.repository.*;
import com.asg.shipping.vesselvoyagecreation.service.VesselVoyageService;
import com.asg.shipping.vesselvoyagecreation.util.DateValidationUtil;
import com.asg.shipping.vesselvoyagecreation.util.VoyageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class VesselVoyageServiceImpl implements VesselVoyageService {

    private static final String MSG_VOYAGE_NOT_FOUND = "Vessel voyage not found: ";
    private static final String FIELD_TRANSACTION_POID = "TRANSACTION_POID";
    private static final String MSG_MISSING_GROUP_POID = "Missing groupPoid";
    private static final String MSG_MISSING_COMPANY_POID = "Missing companyPoid";

    private final ShipVoyageHdrRepository voyageHdrRepository;
    private final VoyageLineMasterRepository voyageLineMasterRepository;
    private final ShipVoyageTranshipDtlRepository transhipDtlRepository;
    private final VwShipEdiExceptionUploadRepository ediExceptionUploadRepository;
    private final VwShipVoyageCurrencyRepository voyageCurrencyRepository;
    private final StoredProcedureRepository storedProcedureRepository;
    private final DocumentSearchService documentSearchService;
    private final VoyageBillsRepository voyageBillsRepository;
    private final LoggingService loggingService;
    private final PrintService printService;
    private final DataSource dataSource;

    @Value("${vvc.edi.upload-dir:./uploads/edi}")
    private String ediUploadDir;

    @Value("${vvc.exports.dir:./exports}")
    private String exportsDir;

    @Override
    public Map<String, Object> listVoyages(FilterRequestDto request, Pageable pageable, String docId, LocalDate startDate, LocalDate endDate) {
        String effectiveDocId = docId != null ? docId : UserContext.getDocumentId();
        if (effectiveDocId == null || effectiveDocId.isBlank()) {
            throw new IllegalArgumentException("X-Document-Id header is required for list endpoint");
        }

        Long userPoid = UserContext.getUserPoid();
        String lineStatus = null;
        if (userPoid != null) {
            lineStatus = storedProcedureRepository.procGlobUserLineListing(userPoid);
        }

        log.info("List voyages | docId={} page={} size={} startDate={} endDate={} lineStatus={}", effectiveDocId, pageable.getPageNumber(), pageable.getPageSize(), startDate, endDate, lineStatus);

        String operator = documentSearchService.resolveOperator(request);
        String isDeleted = documentSearchService.resolveIsDeleted(request);
        var filters = documentSearchService.resolveFilters(request);

        // Add date filters if provided
        if (startDate != null && endDate != null) {
            // Add date range filter for TRANSACTION_DATE field
            filters = documentSearchService.resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);
        }

        RawSearchResult raw = documentSearchService.search(
                effectiveDocId,
                filters,
                operator,
                pageable,
                isDeleted,
                "VOYAGE_NO",
                FIELD_TRANSACTION_POID
        );

        // Apply line access restriction post-fetch (best-effort). If LINE_POID is not in row, we validate using SHIP_VOYAGE_HDR lookup.
        List<Map<String, Object>> filteredRows = applyLineRestriction(raw.records(), lineStatus);
        Page<Map<String, Object>> page = new PageImpl<>(filteredRows, pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private static final String FIELD_LINE_POID = "LINE_POID";

    private List<Map<String, Object>> applyLineRestriction(List<Map<String, Object>> rows, String status) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        if (!shouldRestrictByLine(status)) {
            return rows;
        }

        Set<Long> allowedLines = parseLineList(status);
        if (allowedLines.isEmpty()) {
            return rows;
        }

        if (hasLinePoidInRows(rows)) {
            return filterRowsByLinePoid(rows, allowedLines);
        }

        List<Long> poids = extractTransactionPoids(rows);
        if (poids.isEmpty()) {
            return rows;
        }

        Set<Long> allowedVoyages = resolveAllowedVoyagesByLookup(poids, allowedLines);
        return filterRowsByAllowedVoyages(rows, allowedVoyages);
    }

    private boolean shouldRestrictByLine(String status) {
        return status != null && !status.isBlank() && !status.contains("ALL_LINE_USER");
    }

    private boolean hasLinePoidInRows(List<Map<String, Object>> rows) {
        return !rows.isEmpty() && rows.getFirst().containsKey(FIELD_LINE_POID);
    }

    private List<Map<String, Object>> filterRowsByLinePoid(List<Map<String, Object>> rows, Set<Long> allowedLines) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long linePoid = toLong(row.get(FIELD_LINE_POID));
            if (linePoid == null || allowedLines.contains(linePoid)) {
                out.add(row);
            }
        }
        return out;
    }

    private List<Long> extractTransactionPoids(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(m -> toLong(m.get(FIELD_TRANSACTION_POID)))
                .filter(Objects::nonNull)
                .toList();
    }

    private Set<Long> resolveAllowedVoyagesByLookup(List<Long> poids, Set<Long> allowedLines) {
        Set<Long> allowedVoyages = new HashSet<>();
        List<ShipVoyageHdrEntity> hdrs = voyageHdrRepository.findAllById(poids);
        for (ShipVoyageHdrEntity h : hdrs) {
            Long linePoid = h.getLinePoid();
            if (linePoid == null || allowedLines.contains(linePoid)) {
                allowedVoyages.add(h.getTransactionPoid());
            }
        }
        return allowedVoyages;
    }

    private List<Map<String, Object>> filterRowsByAllowedVoyages(List<Map<String, Object>> rows, Set<Long> allowedVoyages) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long t = toLong(row.get(FIELD_TRANSACTION_POID));
            if (t == null || allowedVoyages.contains(t)) {
                out.add(row);
            }
        }
        return out;
    }

    private Set<Long> parseLineList(String status) {
        // status may look like "(1,2,3)" or " (1,2)" or already with parentheses.
        String s = status.trim();
        if (s.startsWith("(") && s.endsWith(")")) s = s.substring(1, s.length() - 1);
        Set<Long> out = new HashSet<>();
        for (String part : s.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            // Defensive: ignore non-numeric tokens if stored-proc returns unexpected values.
            if (p.matches("\\d+")) {
                out.add(Long.parseLong(p));
            }
        }
        return out;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VoyageResponse getVoyage(Long voyagePoid) {
        Long groupPoid = UserContext.getGroupPoid();
        ShipVoyageHdrEntity e = voyageHdrRepository.findByTransactionPoidAndGroupPoid(voyagePoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_VOYAGE_NOT_FOUND + voyagePoid));

        String lineCode = voyageLineMasterRepository.findLineCodeByLinePoid(e.getLinePoid()).orElse(null);

        loggingService.createLogSummaryEntry(LogDetailsEnum.VIEWED, UserContext.getDocumentId(), voyagePoid.toString());

        return VoyageMapper.toResponse(e, lineCode);
    }

    @Override
    @Transactional
    public VoyageResponse createVoyage(VoyageUpsertRequest request) {
        validateVoyageRequest(request);

        Long groupPoid = require(UserContext.getGroupPoid(), MSG_MISSING_GROUP_POID);
        Long companyPoid = require(UserContext.getCompanyPoid(), MSG_MISSING_COMPANY_POID);
        require(UserContext.getUserId(), "Missing userId in UserContext");

        validateLineCompany(request.getLinePoid(), companyPoid);

        boolean dup = voyageHdrRepository.existsByGroupPoidAndLinePoidAndVesselPoidAndVoyageNo(
                groupPoid, request.getLinePoid(), request.getVesselPoid(), request.getVoyageNo()
        );
        if (dup) throw new ResourceAlreadyExistsException("Duplicate Job, Check Line, Vessel, Voyage...");

        ShipVoyageHdrEntity entity = VoyageMapper.toEntityForCreate(request, groupPoid, companyPoid);
        entity.setTransactionDate(DateUtil.getCurrentDateInUserTimeZone());

        ShipVoyageHdrEntity saved = voyageHdrRepository.save(entity);
        // Reload to get trigger-populated docRef/jobNo if needed
        ShipVoyageHdrEntity fresh = voyageHdrRepository.findById(saved.getTransactionPoid()).orElse(saved);

        // Add logging
        String docId = UserContext.getDocumentId();
        String key = fresh.getTransactionPoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, docId, key);

        String lineCode = voyageLineMasterRepository.findLineCodeByLinePoid(fresh.getLinePoid()).orElse(null);
        return VoyageMapper.toResponse(fresh, lineCode);
    }

    @Override
    @Transactional
    public VoyageResponse updateVoyage(Long voyagePoid, VoyageUpsertRequest request) {
        validateVoyageRequest(request);

        Long groupPoid = require(UserContext.getGroupPoid(), MSG_MISSING_GROUP_POID);
        Long companyPoid = require(UserContext.getCompanyPoid(), MSG_MISSING_COMPANY_POID);
        require(UserContext.getUserId(), "Missing userId in UserContext");

        ShipVoyageHdrEntity entity = voyageHdrRepository.findByTransactionPoidAndGroupPoid(voyagePoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_VOYAGE_NOT_FOUND + voyagePoid));

        validateLineCompany(request.getLinePoid(), companyPoid);

        boolean dup = voyageHdrRepository.existsDuplicateExcludingPoid(
                groupPoid, request.getLinePoid(), request.getVesselPoid(), request.getVoyageNo(), voyagePoid
        );
        if (dup) throw new ResourceAlreadyExistsException("Duplicate Job, Check Line, Vessel, Voyage...");

        // Store old entity for logging
        ShipVoyageHdrEntity oldEntity = new ShipVoyageHdrEntity();
        BeanUtils.copyProperties(entity, oldEntity);

        VoyageMapper.updateEntity(entity, request);
        voyageHdrRepository.save(entity);

        // Add logging
        String docId = UserContext.getDocumentId();
        String key = entity.getTransactionPoid().toString();
        loggingService.logChanges(oldEntity, entity, ShipVoyageHdrEntity.class, docId, key, LogDetailsEnum.MODIFIED, FIELD_TRANSACTION_POID);

        String lineCode = voyageLineMasterRepository.findLineCodeByLinePoid(entity.getLinePoid()).orElse(null);
        return VoyageMapper.toResponse(entity, lineCode);
    }

    private void validateVoyageRequest(VoyageUpsertRequest r) {
        // Key date sequencing enhancements (SRS)
        DateValidationUtil.requireGte(r.getArrivalDate(), r.getExpectedDate(), "Arrival date must be >= Expected date");
        DateValidationUtil.requireGte(r.getSailDate(), r.getArrivalDate(), "Sail date must be >= Arrival date");
        DateValidationUtil.requireGte(r.getCustomRegdate(), r.getArrivalDate(), "Customs registration date must be >= Arrival date");
        DateValidationUtil.requireGte(r.getOperationStartDate(), r.getArrivalDate(), "Ops start date must be >= Arrival date");
        DateValidationUtil.requireGte(r.getOperationEndDate(), r.getOperationStartDate(), "Ops end date must be >= Ops start date");
    }

    private void validateLineCompany(Long linePoid, Long loginCompanyPoid) {
        Long lineCompany = voyageLineMasterRepository.findCompanyPoidByLinePoid(linePoid)
                .orElseThrow(() -> new IllegalArgumentException("Check line company in line master..."));
        if (!Objects.equals(lineCompany, loginCompanyPoid)) {
            throw new IllegalArgumentException("Check line company in line master...");
        }
    }

    private static <T> T require(T v, String msg) {
        if (v == null) throw new IllegalArgumentException(msg);
        return v;
    }

    @Override
    public Page<VoyageBlRow> listBls(Long voyagePoid, VoyageBlTab tab, VoyageBlFilter filter, Pageable pageable) {
        return voyageBillsRepository.listBills(voyagePoid, tab, filter, pageable);
    }

    @Override
    public List<VwShipEdiExceptionUploadEntity> getEdiErrors(Long voyagePoid) {
        return ediExceptionUploadRepository.findByTransactionPoidOrderByPkIdRowAsc(voyagePoid);
    }

    @Override
    public String reprocessEdi(Long voyagePoid) {
        require(voyagePoid, "Missing voyagePoid");
        Long groupPoid = require(UserContext.getGroupPoid(), MSG_MISSING_GROUP_POID);
        Long companyPoid = require(UserContext.getCompanyPoid(), MSG_MISSING_COMPANY_POID);
        String docId = Optional.ofNullable(UserContext.getDocumentId()).orElse("100-101");
        Long userPoid = Optional.ofNullable(UserContext.getUserPoid()).orElse(0L);

        // Validate that voyagePoid exists
        if (!voyageHdrRepository.existsById(voyagePoid)) {
            throw new ResourceNotFoundException("Voyage not found: " + voyagePoid);
        }

        log.info("Reprocess EDI | voyagePoid={} groupPoid={} companyPoid={} docId={} user={}", voyagePoid, groupPoid, companyPoid, docId, userPoid);
        return storedProcedureRepository.procAttachmentsEdiProcNew(
                groupPoid, companyPoid, docId, voyagePoid, voyagePoid, userPoid);

    }

    @Override
    public String uploadAndProcessEdi(Long voyagePoid, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("EDI file is required");

        // Save file into a configured directory (must align with LINE_EDI_READ_TRANSFER backend expectations)
        try {
            Path dir = Path.of(ediUploadDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(Objects.requireNonNull(file.getOriginalFilename()));
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("EDI file stored at {}", target.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to store EDI file: " + e.getMessage());
        }

        return reprocessEdi(voyagePoid);
    }

    @Override
    public List<ShipVoyageTranshipDtlEntity> listTranshipments(Long voyagePoid) {
        return transhipDtlRepository.findByTransactionPoidOrderByDetRowIdAsc(voyagePoid);
    }

    @Override
    @Transactional
    public List<ShipVoyageTranshipDtlEntity> updateTranshipments(Long voyagePoid, TranshipmentUpdateRequest request) {
        List<ShipVoyageTranshipDtlEntity> existing = transhipDtlRepository.findByTransactionPoidOrderByDetRowIdAsc(voyagePoid);
        Map<Long, ShipVoyageTranshipDtlEntity> map = new HashMap<>();
        for (ShipVoyageTranshipDtlEntity e : existing) map.put(e.getDetRowId(), e);

        for (TranshipmentUpdateItem item : request.getItems()) {
            ShipVoyageTranshipDtlEntity e = map.get(item.getDetRowId());
            if (e == null)
                throw new ResourceNotFoundException("Transhipment row not found detRowId=" + item.getDetRowId());
            if (item.getContainerNo() != null) e.setContainerNo(item.getContainerNo());
            e.setContainerType(item.getContainerType());
            e.setIsoCode(item.getIsoCode());
            e.setStatus(item.getStatus());
            e.setOrigin(item.getOrigin());
            e.setPol(item.getPol());
            e.setIsLoaded(item.getIsLoaded());
            e.setLoadTransactionPoid(item.getLoadTransactionPoid());
            e.setIsRefer(item.getIsRefer());
            e.setRefferTemp(item.getRefferTemp());
            e.setImoCode1(item.getImoCode1());
            e.setUnNo1(item.getUnNo1());
            e.setImoCode2(item.getImoCode2());
            e.setUnNo2(item.getUnNo2());
            e.setLoadWeightKg(item.getLoadWeightKg());
            e.setWeightTon(item.getWeightTon());
            e.setOogH(item.getOogH());
            e.setOogL(item.getOogL());
            e.setOogLW(item.getOogLW());
            e.setOogRW(item.getOogRW());
            e.setHsCode(item.getHsCode());
            e.setHsShortname(item.getHsShortname());
            e.setSlot(item.getSlot());
            e.setBlading(item.getBlading());
            e.setOutboundVessel(item.getOutboundVessel());
            e.setLoadOrigin(item.getLoadOrigin());
            e.setLoadFinalDestination(item.getLoadFinalDestination());
        }

        transhipDtlRepository.saveAll(existing);
        return transhipDtlRepository.findByTransactionPoidOrderByDetRowIdAsc(voyagePoid);
    }

    @Override
    @Transactional
    public String transferTranshipments(Long voyagePoid, TranshipmentTransferRequest request) {
        List<ShipVoyageTranshipDtlEntity> rows = transhipDtlRepository.findByTransactionPoidOrderByDetRowIdAsc(voyagePoid);
        Set<Long> ids = new HashSet<>(request.getDetRowIds());
        int updated = 0;
        for (ShipVoyageTranshipDtlEntity e : rows) {
            if (!ids.contains(e.getDetRowId())) continue;
            e.setIsLoaded("Y");
            e.setLoadTransactionPoid(request.getTargetVoyagePoid());
            updated++;
        }
        transhipDtlRepository.saveAll(rows);
        return "Transfer Assignment Completed (" + updated + " rows). Press Save/Refresh.";
    }

    @Override
    public String importHnjnTranshipments(Long voyagePoid) {
        log.info("Import HNJN transhipments | voyagePoid={}", voyagePoid);
        return storedProcedureRepository.procShipVoyageHnjnTranImpV2(voyagePoid);
    }

    @Override
    public List<VwShipVoyageCurrencyEntity> listVoyageCurrencies(Long voyagePoid) {
        return voyageCurrencyRepository.findByTransactionPoidOrderByDetRowIdAsc(voyagePoid);
    }

    @Override
    public String updateCurrencyRates(Long voyagePoid, CurrencyUpdateRequest request) {
        Long groupPoid = require(UserContext.getGroupPoid(), MSG_MISSING_GROUP_POID);
        Long companyPoid = require(UserContext.getCompanyPoid(), MSG_MISSING_COMPANY_POID);
        String userCode = Optional.ofNullable(UserContext.getUserId()).orElse("0");

        StringBuilder sb = new StringBuilder();
        for (var item : request.getItems()) {
            if (item.getNewCurrencyExchange() == null || item.getNewCurrencyExchange() == 0) continue;
            sb.append(",").append(item.getCurrencyCode()).append("=").append(item.getNewCurrencyExchange());
        }
        String payload = sb.toString();
        log.info("Update currency | voyagePoid={} payload={}", voyagePoid, payload);
        storedProcedureRepository.procShipVoyageCurrencyUpd(groupPoid, companyPoid, voyagePoid, userCode, payload);
        return "Currency updated";
    }

    @Override
    public String resendCan(Long voyagePoid, Long blTransactionPoid) {
        log.info("Resend CAN | voyagePoid={} blTransactionPoid={}", voyagePoid, blTransactionPoid);
        storedProcedureRepository.prodResendCan(voyagePoid, blTransactionPoid);
        return "CAN resend queued";
    }

    @Override
    public String createEmptyManifest(Long voyagePoid) {
        String userCode = Optional.ofNullable(UserContext.getUserId()).orElse("0");
        log.info("Create empty manifest | voyagePoid={} userCode={}", voyagePoid, userCode);
        return storedProcedureRepository.procMateRcptEmptyManifest(voyagePoid, userCode);
    }

    @Override
    public String createTdr(Long voyagePoid, boolean createEmptyManifestFirst) {
        Long userPoid = Optional.ofNullable(UserContext.getUserPoid()).orElse(0L);
        if (createEmptyManifestFirst) {
            createEmptyManifest(voyagePoid);
        }
        log.info("Create TDR | voyagePoid={} userPoid={}", voyagePoid, userPoid);
        return storedProcedureRepository.procLoadTdrOwnLine(voyagePoid, userPoid);
    }

    @Override
    public String exportEdiCosco(Long voyagePoid, Long blPoid) {
        Long userPoid = Optional.ofNullable(UserContext.getUserPoid()).orElse(0L);
        Long effectiveBlPoid = blPoid != null ? blPoid : 0L;
        log.info("Export EDI COSCO | voyagePoid={} blPoid={} userPoid={}", voyagePoid, effectiveBlPoid, userPoid);
        storedProcedureRepository.procShipExportEdiCosco(voyagePoid, effectiveBlPoid, userPoid);
        return "COSCO export triggered (file/email handled by DB procedure)";
    }

    @Override
    public String importGeneralCargo(Long voyagePoid) {
        Long groupPoid = require(UserContext.getGroupPoid(), MSG_MISSING_GROUP_POID);
        Long companyPoid = require(UserContext.getCompanyPoid(), MSG_MISSING_COMPANY_POID);
        String userCode = Optional.ofNullable(UserContext.getUserId()).orElse("0");
        log.info("General cargo import | voyagePoid={} groupPoid={} companyPoid={} user={}", voyagePoid, groupPoid, companyPoid, userCode);
        return storedProcedureRepository.procGeneralImportCargo(groupPoid, companyPoid, userCode, voyagePoid);
    }

    @Override
    public String importSelectedXl(Long voyagePoid) {
        log.info("Import selected XL template | voyagePoid={}", voyagePoid);
        return storedProcedureRepository.procLoadEdiXlTemplate(voyagePoid);
    }

    @Override
    public Resource downloadExcelExport(Long voyagePoid, String type) {
        // Functional implementation: serve files from a configured exports folder.
        // Generation is assumed to be handled by DB procedures / report server job, similar to legacy.
        Path dir = Path.of(exportsDir, String.valueOf(voyagePoid));
        String t = type == null ? "" : type.toLowerCase();

        try {
            return switch (t) {
                case "apmt-discharge" -> readSingle(dir, "Discharge_list.xlsx");
                case "transhipment-discharge" -> readSingle(dir, "Transhipment_Discharge_list.xlsx");
                case "apmt-general-vessel-discharge" -> readSingle(dir, "APMTLISTGERN.xlsx");
                case "yml-export-csv" -> readSingle(dir, "OA_Booking_csv_format.csv");
                case "tbl" -> readZip(dir, Map.of(
                        "TBLManifestTemplate.xlsx", "TBLManifestTemplate.xlsx",
                        "TBLLIST.xlsx", "TBLLIST.xlsx"
                ), "TBL_Export_" + voyagePoid + ".zip");
                default -> throw new IllegalArgumentException("Unsupported export type: " + type);
            };
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read export file(s): " + e.getMessage());
        }
    }

    @Override
    public Resource downloadManifestReport(Long voyagePoid, String freightCargo, String importExport) {
        // Functional implementation: serve PDFs from configured folder.
        // Expected naming convention can be aligned with your report server output.
        String fc = freightCargo != null ? freightCargo.toUpperCase() : "FALSE";
        String ie = importExport != null ? importExport.toUpperCase() : "BOTH";
        Path dir = Path.of(exportsDir, String.valueOf(voyagePoid), "reports");
        String fileName = "MANIFEST_" + ie + "_" + (fc.equals("TRUE") ? "FREIGHT" : "CARGO") + ".pdf";
        try {
            return readSingle(dir, fileName);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read report file: " + e.getMessage());
        }
    }

    private Resource readSingle(Path dir, String fileName) throws IOException {
        Path f = dir.resolve(fileName);
        if (!Files.exists(f)) {
            throw new ResourceNotFoundException("Export file not found: " + f.toAbsolutePath());
        }
        return new ByteArrayResource(Files.readAllBytes(f)) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
    }

    private Resource readZip(Path dir, Map<String, String> sourceToEntry, String zipName) throws IOException {
        for (String src : sourceToEntry.keySet()) {
            Path f = dir.resolve(src);
            if (!Files.exists(f)) {
                throw new ResourceNotFoundException("Export file not found: " + f.toAbsolutePath());
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (var e : sourceToEntry.entrySet()) {
                Path f = dir.resolve(e.getKey());
                zos.putNextEntry(new ZipEntry(e.getValue()));
                zos.write(Files.readAllBytes(f));
                zos.closeEntry();
            }
        }
        byte[] zipBytes = baos.toByteArray();
        return new ByteArrayResource(zipBytes) {
            @Override
            public String getFilename() {
                return zipName;
            }
        };
    }

    @Override
    @Transactional
    public void deleteVoyage(Long voyagePoid) {
        log.info("Deleting vessel voyage with id: {}", voyagePoid);

        Long groupPoid = UserContext.getGroupPoid();
        ShipVoyageHdrEntity entity = voyageHdrRepository.findByTransactionPoidAndGroupPoid(voyagePoid, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(MSG_VOYAGE_NOT_FOUND + voyagePoid));

        // Check if already deleted
        if ("Y".equals(entity.getDeleted())) {
            log.info("Vessel Voyage with id: {} is already deleted", voyagePoid);
            return;
        }

        entity.setDeleted("Y");
        voyageHdrRepository.save(entity);

        // Add logging
        String docId = UserContext.getDocumentId();
        String key = entity.getTransactionPoid().toString();
        loggingService.createLogSummaryEntry(LogDetailsEnum.DELETED, docId, key);
        loggingService.logSimpleFieldChange(ShipVoyageHdrEntity.class, docId, key, "deleted", "N", "Y", "VesselVoyage soft deleted");

        log.info("Successfully deleted vessel voyage with id: {}", voyagePoid);
    }


    @Override
    public byte[] print(Long transactionPoid, String freightCargo, String importExport) {
        try {
            Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-101");
            params.put("P_FREIGHTCARGO", freightCargo);
            params.put("P_IMPORT_EXPORT", importExport);
            params.put("SUBREPORT_MARK_INFO", printService.load("Shipping/SH/Cargo/Mark_Info_Subreport1.jrxml"));
            params.put("SUBREPORT_CONTAINER_INFO", printService.load("Shipping/SH/Cargo/Container_Info_Subreport1.jrxml"));
            params.put("SUBREPORT_DESCRIPTION_INFO", printService.load("Shipping/SH/Cargo/Description_Info_Subreport1.jrxml"));
            params.put("SUBREPORT_FREIGHT_DETAIL", printService.load("Shipping/SH/Cargo/Freight_Detail_Subreport1.jrxml"));
            JasperReport mainReport = printService.load("Shipping/SH/Cargo/Manifest_Cargo_WithCharges.jrxml");
            return printService.fillReportToPdf(mainReport, params, dataSource);
        } catch (Exception ex) {
            throw new com.asg.shipping.exceptions.CustomException("Failed to generate Vessel Voyage PDF", ex);
        }
    }

}


