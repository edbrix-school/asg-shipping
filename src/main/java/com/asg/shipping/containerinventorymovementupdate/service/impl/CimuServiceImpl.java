package com.asg.shipping.containerinventorymovementupdate.service.impl;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.containerinventorymovementupdate.dto.ContainerHistoryRowDto;
import com.asg.shipping.containerinventorymovementupdate.dto.ContainerInfoDto;
import com.asg.shipping.containerinventorymovementupdate.dto.DemurrageCalculateRequest;
import com.asg.shipping.containerinventorymovementupdate.dto.DemurrageCalculateResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.ExcelInspectionRow;
import com.asg.shipping.containerinventorymovementupdate.dto.InspectionLoadResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.InspectionUploadResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.QueryCimuRequest;
import com.asg.shipping.containerinventorymovementupdate.dto.QueryCimuResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.SocUpdateRequest;
import com.asg.shipping.containerinventorymovementupdate.dto.SocUpdateResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.SuggestContainerResponse;
import com.asg.shipping.containerinventorymovementupdate.dto.UpdateCimuRequest;
import com.asg.shipping.containerinventorymovementupdate.dto.UpdateCimuResponse;
import com.asg.shipping.containerinventorymovementupdate.repository.jdbc.CimuDemurrageRepository;
import com.asg.shipping.containerinventorymovementupdate.repository.jdbc.CimuLovSuggestionRepository;
import com.asg.shipping.containerinventorymovementupdate.repository.jdbc.CimuQueryRepository;
import com.asg.shipping.containerinventorymovementupdate.repository.jdbc.CimuRightsRepository;
import com.asg.shipping.containerinventorymovementupdate.repository.jdbc.CimuUpdateRepository;
import com.asg.shipping.containerinventorymovementupdate.service.CimuService;
import com.asg.shipping.containerinventorymovementupdate.util.ExcelInspectionParser;
import com.asg.shipping.containerinventorymovementupdate.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CimuServiceImpl implements CimuService {
    private static final DateTimeFormatter DEMURRAGE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final CimuLovSuggestionRepository suggestionRepository;
    private final CimuQueryRepository queryRepository;
    private final CimuUpdateRepository updateRepository;
    private final CimuDemurrageRepository demurrageRepository;
    private final CimuRightsRepository rightsRepository;


    @Override
    public SuggestContainerResponse suggestContainers(String query) {
        if (query == null || query.trim().length() < 6) {
            throw new ValidationException("query must have at least 6 characters");
        }

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        List<String> values = suggestionRepository.suggestContainerNos(groupPoid, companyPoid, userPoid, query.trim());
        List<SuggestContainerResponse.Item> items = values.stream()
                .map(v -> new SuggestContainerResponse.Item(v))
                .collect(Collectors.toList());
        return new SuggestContainerResponse(items);
    }

    @Override
    @Transactional(readOnly = true)
    public QueryCimuResponse queryScreenData(QueryCimuRequest request) {
        String containerNo = request != null ? normalize(request.getContainerNo()) : null;
        String blNumber = request != null ? normalize(request.getBlNumber()) : null;

        if ((containerNo == null || containerNo.isBlank()) && (blNumber == null || blNumber.isBlank())) {
            throw new ValidationException("Either containerNo or blNumber is required");
        }

        boolean canEditActualDischargeDate = safeRight("000-279");

        List<ContainerInfoDto> info = queryRepository.fetchContainerInfo(containerNo, blNumber);
        List<ContainerHistoryRowDto> history = (containerNo != null && containerNo.length() >= 4)
                ? queryRepository.fetchHistoryByContainerNo(containerNo)
                : List.of();


        return QueryCimuResponse.builder()
                .queryEcho(QueryCimuResponse.QueryEcho.builder().containerNo(containerNo).blNumber(blNumber).build())
                .permissions(QueryCimuResponse.Permissions.builder()
                        .canEditActualDischargeDate(canEditActualDischargeDate)
                        .build())
                .containerInfoList(info)
                .containerHistoryList(history)
                .build();
    }

    @Override
    public UpdateCimuResponse updateContainerData(UpdateCimuRequest request) {
        if (request == null) throw new ValidationException("Request body is required");

        Long transactionPoid = request.getTransactionPoid();
        String containerNo = normalize(request.getContainerNo());
        boolean applyAll = request.getApplyToAllContainers() != null && request.getApplyToAllContainers();
        boolean holdReturnForm = request.getHoldReturnForm() != null && request.getHoldReturnForm();

        if (!applyAll && (containerNo == null || containerNo.isBlank())) {
            throw new ValidationException("containerNo is required when applyToAllContainers=false");
        }

        if (request.getBlNumber() != null && !request.getBlNumber().isBlank()) {
            String blNumber = request.getBlNumber().trim();
            if (!queryRepository.blNumberMatchesTransaction(transactionPoid, blNumber)) {
                throw new ValidationException("blNumber does not match transactionPoid; please re-query and try again");
            }
        }

        if (!applyAll) {
            if (!queryRepository.containerExistsInBl(transactionPoid, containerNo)) {
                throw new ValidationException("containerNo does not belong to this BL (transactionPoid); please re-query");
            }
        }

        Long userPoid = UserContext.getUserPoid();
        if (userPoid == null) {
            throw new ValidationException("Missing user context (X-User-Id / X-User-Poid headers)");
        }

        // Rights enforcement (server-side)
        if (holdReturnForm && !rightsRepository.hasDocRight("000-248", userPoid)) {
            throw new ValidationException("User have no right to hold Return Form (000-248)");
        }

        // Build legacy-style dynamic SQL for milestone updates (optional; procedure executes it if present)
        // We only apply milestone updates for a specific container, not for ALL.
        String runStatement = buildRunStatement(
                transactionPoid,
                containerNo,
                request.getWithConsigneeFull(),
                request.getEmptyIn(),
                request.getActualDischargeDate(),
                UserContext.getUserId() != null ? UserContext.getUserId() : String.valueOf(userPoid)
        );

        String containerNoOrAll = applyAll ? "ALL" : containerNo;
        String cntRtnHold = holdReturnForm ? "MANUALLYPRINTNO" : "N";

        String status = updateRepository.callProcShipCntInvtUpdate(
                runStatement,
                transactionPoid,
                request.getExtraFreeDays(),
                request.getExtraFreeDaysPrnpls(),
                normalize(request.getBlIssueType()),
                containerNoOrAll,
                userPoid,
                cntRtnHold
        );

        // Default to "TRUE" to match legacy behavior (legacy treats null as success).
        if (status == null || status.isBlank()) {
            log.warn("PROC_SHIP_CNT_INVT_UPDATE returned null/blank status; defaulting to TRUE. transactionPoid={}", transactionPoid);
            status = "TRUE";
        }
        return UpdateCimuResponse.builder().status(status).build();
    }

    @Override
    public SocUpdateResponse socUpdate(SocUpdateRequest request) {
        if (request == null || request.getBlNumber() == null || request.getBlNumber().isBlank()) {
            throw new ValidationException("blNumber is required");
        }
        Long userPoid = UserContext.getUserPoid();
        if (userPoid == null) {
            throw new ValidationException("Missing user context (X-User-Id / X-User-Poid headers)");
        }
        String status = updateRepository.callProcShipCntSocUpdate(request.getBlNumber().trim(), userPoid);
        if (status == null || status.isBlank()) status = "TRUE";
        return SocUpdateResponse.builder().status(status).build();
    }

    @Override
    public DemurrageCalculateResponse calculateDemurrage(DemurrageCalculateRequest request) {
        if (request == null) throw new ValidationException("Request body is required");

        String containerNo = normalize(request.getContainerNo());

        LocalDate demDt = request.getDemDt();
        if (demDt == null) {
            throw new ValidationException("demDt is required");
        }

        String demDtStr = demDt.format(DEMURRAGE_DATE_FORMAT);

        // Legacy: FUNC_SHIP_CNT_dem_Rtn(transactionPoid, containerNo, demDt)
        BigDecimal dem = demurrageRepository.calculateDemurrage(request.getTransactionPoid(), containerNo, demDtStr);

        // Legacy: FUNC_SHIP_CNT_IMPORT_TOTAL(transactionPoid, containerNo, demDt)
        String importTotalMsg = demurrageRepository.getImportTotalMessage(
                request.getTransactionPoid(), containerNo, demDtStr);

        // FUNC_SHIP_CNT_PORT_DAYS(transactionPoid, containerNo, emptyIn)
        BigDecimal portDays = demurrageRepository.getPortDays(
                request.getTransactionPoid(), containerNo, request.getEmptyIn());

        BigDecimal resolvedPortDays = portDays != null ? portDays : BigDecimal.ZERO;
        BigDecimal resolvedDem = dem != null ? dem : BigDecimal.ZERO;
        String portDaysMsg = "Total days lying in the Port: " + resolvedPortDays.stripTrailingZeros().toPlainString();
        String demMsg = "Total Demurrage: " + resolvedDem.stripTrailingZeros().toPlainString();
        String fullMessage = (importTotalMsg != null ? importTotalMsg + ", " : "") + portDaysMsg + ", " + demMsg;

        return DemurrageCalculateResponse.builder()
                .demurrageAmount(resolvedDem)
                .collectedSummaryMessage(fullMessage)
                .portDays(resolvedPortDays)
                .build();
    }

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean safeRight(String rightId) {
        try {
            Long userPoid = UserContext.getUserPoid();
            if (userPoid == null) return false;
            return rightsRepository.hasDocRight(rightId, userPoid);
        } catch (Exception e) {
            log.warn("Rights check failed for {}: {}", rightId, e.getMessage());
            return false;
        }
    }

    /**
     * Mimics legacy dynamic SQL update used as P_RUN_STATEMENT inside PROC_SHIP_CNT_INVT_UPDATE.
     * Only includes fields provided.
     *
     * Note: We keep table/column names unchanged (per instruction).
     */
    private String buildRunStatement(
            Long transactionPoid,
            String containerNo,
            String withConsigneeFull,
            String emptyIn,
            String actualDischargeDate,
            String userIdForAudit
    ) {
        if (transactionPoid == null || containerNo == null || containerNo.isBlank()) return "";

        boolean hasAny = (withConsigneeFull != null && !withConsigneeFull.isBlank())
                || (emptyIn != null && !emptyIn.isBlank())
                || (actualDischargeDate != null && !actualDischargeDate.isBlank());

        if (!hasAny) return "";

        StringBuilder q = new StringBuilder();
        q.append("BEGIN UPDATE SHIP_BL_MANIFEST_CONTAINER_DTL SET LASTMODIFIED_DATE=SYSDATE, LASTMODIFIED_BY='")
                .append(userIdForAudit != null ? userIdForAudit.replace("'", "''") : "SYSTEM")
                .append("'");

        if (withConsigneeFull != null && !withConsigneeFull.isBlank()) {
            String ts = DateTimeUtil.parseFlexibleDateTimeToOracle(withConsigneeFull);
            q.append(", ISSUE_TO_CONSIGNEE=TO_DATE(SUBSTR('").append(ts).append("',1,19),'RRRR-MM-DD HH24:MI:SS')");
        }
        if (emptyIn != null && !emptyIn.isBlank()) {
            String ts = DateTimeUtil.parseFlexibleDateTimeToOracle(emptyIn);
            q.append(", RETURN_FROM_CONSIGNEE=TO_DATE(SUBSTR('").append(ts).append("',1,19),'RRRR-MM-DD HH24:MI:SS')");
        }
        if (actualDischargeDate != null && !actualDischargeDate.isBlank()) {
            String ts = DateTimeUtil.parseFlexibleDateTimeToOracle(actualDischargeDate);
            q.append(", ACTUAL_DISCHARGE_DATE=TO_DATE(SUBSTR('").append(ts).append("',1,19),'RRRR-MM-DD HH24:MI:SS')");
        }
        // Note: amountPerDayAfterFree removed from update - not in SRS document

        // Keep legacy safety:
        q.append(", PRINT_RETURN_FORM_DEFAULT = DECODE(PRINT_RETURN_FORM_DEFAULT,'N','MANUALLYPRINTNO',PRINT_RETURN_FORM_DEFAULT)");

        q.append(" WHERE TRANSACTION_POID=").append(transactionPoid);
        q.append(" AND CONTAINER_NO='").append(containerNo.replace("'", "''")).append("'");
        q.append("; END;");
        return q.toString();
    }


    // -------------------- SRS enhancement: Container Inspection XL Upload (merged) --------------------

    @Transactional
    public InspectionUploadResponse importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("file is required");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.xlsx";
        if (!fileName.toLowerCase().endsWith(".xlsx")) {
            throw new ValidationException("Only .xlsx files are supported");
        }

        try {
            // Parse Excel file
            List<ExcelInspectionRow> excelRows = ExcelInspectionParser.parseFullExcelRows(file.getInputStream());
            if (excelRows.isEmpty()) {
                throw new ValidationException("No container data found in the uploaded file");
            }

            // Clear temp table first (as per procedure requirement)
            queryRepository.clearInspectionTempTable();

            // Insert parsed data into SH_CONTAINER_INSPECT_IMP_TBL
            // Based on PROC_SH_CNT_INSPECT_XL_UPLOAD (DDL lines 2469-2472) and table DDL (line 2547-2555):
            // Temp table columns: SL_NO (VARCHAR2), CONTAINER_NO, MOVES_DATE, LINE_NAME, SIZE_TYPE, LOCATION_STATUS, CONTAINER_STATUS, REMARKS
            //
            // Excel column mapping (flexible matching):
            // - SL_NO: Auto-generated sequence number (stored as VARCHAR2 in temp table, converted to NUMBER by procedure if valid)
            // - CONTAINER_NO: Required, from containerNo field
            // - MOVES_DATE: Maps from Excel columns like "Date", "MOVES_DATE", "MOVES DATE", "Inspection Date"
            // - LINE_NAME: Maps from "Line", "LINE_NAME", "LINE NAME", "Line Name"
            // - SIZE_TYPE: Maps from "Size", "Type", "SIZE/TYPE", "SIZE_TYPE", "Size/Type"
            // - LOCATION_STATUS: Maps from "Location", "LOCATION_STATUS", "Location Status"
            // - CONTAINER_STATUS: Maps from "Sound", "Damage", "CONTAINER_STATUS", "Status", "Sound / Damage"
            // - REMARKS: Maps from "Remark", "REMARKS", "Remarks", "Note", "Notes"
            int slNoCounter = 1;
            for (ExcelInspectionRow excelRow : excelRows) {
                String containerNo = excelRow.getContainerNo();
                if (containerNo == null || containerNo.trim().isEmpty()) {
                    continue; // Skip rows without container number
                }

                // Map Excel columns to temp table columns (procedure expects these exact field names)
                Map<String, String> cols = excelRow.getOtherColumns();

                // Check if Excel has SL_NO column, otherwise use auto-generated sequence
                String slNo = findColumnValue(cols, "SL_NO", "SL NO", "S.No", "S.NO", "Serial No", "SERIAL NO", "Serial Number");
                if (slNo == null || slNo.trim().isEmpty()) {
                    slNo = String.valueOf(slNoCounter++);  // Auto-generate if not in Excel
                }

                String movesDate = findColumnValue(cols,
                        "MOVES_DATE", "MOVES DATE", "Date", "DATE", "Inspection Date", "INSPECTION DATE");
                String lineName = findColumnValue(cols,
                        "LINE_NAME", "LINE NAME", "Line", "LINE", "Line Name");
                String sizeType = findColumnValue(cols,
                        "SIZE_TYPE", "SIZE/TYPE", "Size/Type", "Size", "SIZE", "Type", "TYPE");
                String locationStatus = findColumnValue(cols,
                        "LOCATION_STATUS", "Location Status", "Location", "LOCATION");
                String containerStatus = findColumnValue(cols,
                        "CONTAINER_STATUS", "Container Status", "Status", "STATUS",
                        "Sound / Damage", "SOUND / DAMAGE", "Sound", "SOUND", "Damage", "DAMAGE");
                String remarks = findColumnValue(cols,
                        "REMARKS", "Remarks", "REMARK", "Remark", "Note", "NOTE", "Notes", "NOTES");

                queryRepository.insertInspectionTempTable(
                        slNo,                                      // SL_NO (VARCHAR2 in temp table - DDL line 2548)
                        containerNo,                              // CONTAINER_NO (required)
                        movesDate,                                // MOVES_DATE (optional)
                        lineName,                                 // LINE_NAME (optional)
                        sizeType,                                 // SIZE_TYPE (optional)
                        locationStatus,                           // LOCATION_STATUS (optional)
                        containerStatus,                          // CONTAINER_STATUS (optional)
                        remarks                                   // REMARKS (optional)
                );
            }

            log.info("Imported {} rows into SH_CONTAINER_INSPECT_IMP_TBL", excelRows.size());

            // Generate uploadId (for tracking - not stored in memory)
            String uploadId = "upload_" + System.currentTimeMillis();

            // Preview: first 20 container numbers
            List<String> preview = excelRows.stream()
                    .map(ExcelInspectionRow::getContainerNo)
                    .limit(20)
                    .toList();

            return InspectionUploadResponse.builder()
                    .uploadId(uploadId)
                    .fileName(fileName)
                    .rowCount((long) excelRows.size())
                    .parsedPreview(preview)
                    .build();

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error importing file", e);
            throw new RuntimeException("Unable to import file: " + e.getMessage(), e);
        }
    }

    /**
     * Step 2: Load Container Details
     *
     * Flow:
     * 1. Calls PROC_SH_CNT_INSPECT_XL_UPLOAD procedure
     * 2. Procedure reads from SH_CONTAINER_INSPECT_IMP_TBL (temp table)
     * 3. Procedure inserts into SH_CONTAINER_INSPECT_DTL_TBL (main table)
     * 4. Procedure deletes data from temp table
     *
     * Column Mapping (DDL lines 2462-2476):
     * Temp Table → Main Table:
     * - SL_NO → SL_NO (converted to number if valid)
     * - CONTAINER_NO → CONTAINER_NO (trimmed)
     * - MOVES_DATE → MOVES_DATE_CHAR (renamed, kept as string)
     * - LINE_NAME → LINE_NAME (trimmed)
     * - SIZE_TYPE → SIZE_TYPE (trimmed)
     * - LOCATION_STATUS → LOCATION_STATUS (trimmed)
     * - CONTAINER_STATUS → CONTAINER_STATUS (trimmed)
     * - REMARKS → REMARKS (trimmed)
     *
     * Additional columns added by procedure:
     * - TRANSACTION_POID (generated: max + 1)
     * - DET_ROW_ID (generated: row_number())
     * - CREATED_BY, CREATED_DATE, LASTMODIFIED_BY, LASTMODIFIED_DATE (audit fields)
     *
     * This is the ONLY step that uses the procedure.
     *
     * @return Response with transactionPoid and status
     */
    @Transactional
    public InspectionLoadResponse loadContainerDetails() {
        Long groupPoid = UserContext.getGroupPoid();
        Long userPoid = UserContext.getUserPoid();
        Long companyPoid = UserContext.getCompanyPoid();

        if (userPoid == null) {
            throw new ValidationException("Missing user context (X-User-Poid header)");
        }

        // Call PROC_SH_CNT_INSPECT_XL_UPLOAD
        // Procedure reads from SH_CONTAINER_INSPECT_IMP_TBL and inserts into SH_CONTAINER_INSPECT_DTL_TBL
        Map<String, String> result = updateRepository.callProcShCntInspectXlUpload(
                groupPoid != null ? groupPoid : 1L,
                userPoid,
                companyPoid != null ? companyPoid : 1L
        );

        String transactionPoid = result.get("transactionPoid");
        String status = result.get("status");

        boolean success = status != null &&
                (status.contains("Success") || status.contains("SUCCESS") ||
                        status.toLowerCase().contains("loaded"));

        return InspectionLoadResponse.builder()
                .transactionPoid(transactionPoid)
                .status(status)
                .success(success)
                .build();
    }

    /**
     * Helper method to find column value from Excel otherColumns map.
     * Searches for column header names (case-insensitive, flexible matching).
     *
     * @param columns Map of column header -> value from Excel
     * @param searchTerms Multiple possible column header names to search for
     * @return First matching column value, or null if not found
     */
    private String findColumnValue(Map<String, String> columns, String... searchTerms) {
        if (columns == null || columns.isEmpty()) return null;

        for (String term : searchTerms) {
            String termUpper = term.toUpperCase();
            for (Map.Entry<String, String> entry : columns.entrySet()) {
                String headerUpper = entry.getKey().toUpperCase();
                if (headerUpper.contains(termUpper) || headerUpper.equals(termUpper)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
}


