package com.asg.shipping.containerinventorymovementupdate.util;

import com.asg.shipping.containerinventorymovementupdate.dto.ExcelInspectionRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.*;

@Slf4j
public final class ExcelInspectionParser {
    private ExcelInspectionParser() {}


    /**
     * Parses the uploaded Excel and extracts full row data for Container Inspection XL Upload.
     *
     * Based on PROC_SH_CNT_INSPECT_XL_UPLOAD procedure (DDL lines 2469-2472), the procedure expects
     * these columns in SH_CONTAINER_INSPECT_IMP_TBL temp table:
     * - SL_NO (auto-generated sequence number)
     * - CONTAINER_NO (required - from Excel "Container No." column)
     * - MOVES_DATE (optional - from Excel "Date", "MOVES_DATE", "Inspection Date", etc.)
     * - LINE_NAME (optional - from Excel "Line", "LINE_NAME", "Line Name", etc.)
     * - SIZE_TYPE (optional - from Excel "Size/Type", "SIZE_TYPE", "Size", "Type", etc.)
     * - LOCATION_STATUS (optional - from Excel "Location", "LOCATION_STATUS", etc.)
     * - CONTAINER_STATUS (optional - from Excel "Sound / Damage", "CONTAINER_STATUS", "Status", etc.)
     * - REMARKS (optional - from Excel "Remarks", "REMARKS", "Note", etc.)
     *
     * Algorithm:
     * 1. Find header row with "Container No." (case-insensitive, flexible matching)
     * 2. Identify all column indices and headers dynamically
     * 3. Read all row data (container number + all other columns)
     * 4. Store all columns in otherColumns map for flexible mapping
     * 5. Normalize container numbers (trim, uppercase)
     *
     * The service layer will map Excel columns to temp table fields using flexible matching
     * (e.g., "Date" → MOVES_DATE, "Line" → LINE_NAME, "Sound / Damage" → CONTAINER_STATUS).
     *
     * @return List of ExcelInspectionRow containing container number and all column data
     */
    public static List<ExcelInspectionRow> parseFullExcelRows(InputStream in) {
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) return List.of();

            // Step 1: Find header row and identify all columns
            int containerColIndex = -1;
            int headerRowIndex = -1;
            Map<Integer, String> columnHeaders = new LinkedHashMap<>(); // Preserve column order

            // Search first 5 rows for header (usually row 0 or 1)
            for (int r = 0; r < Math.min(5, sheet.getLastRowNum() + 1); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // Check if this row contains "Container No." header
                boolean hasContainerHeader = false;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    String cellValue = cellToString(cell);
                    if (cellValue == null) continue;

                    String normalized = cellValue.trim().toUpperCase();
                    if (normalized.contains("CONTAINER") && normalized.contains("NO")) {
                        containerColIndex = c;
                        headerRowIndex = r;
                        hasContainerHeader = true;
                        break;
                    }
                }

                if (hasContainerHeader) {
                    // Found header row - extract all column headers
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        String headerValue = cellToString(cell);
                        if (headerValue != null && !headerValue.trim().isEmpty()) {
                            columnHeaders.put(c, headerValue.trim());
                        }
                    }
                    log.info("Found header row | row={} containerCol={} totalColumns={}",
                            r, containerColIndex, columnHeaders.size());
                    break;
                }
            }

            if (containerColIndex < 0) {
                log.warn("Container No. column header not found, falling back to column B (index 1)");
                containerColIndex = 1;
                headerRowIndex = 0;
            }

            // Step 2: Extract all row data
            List<ExcelInspectionRow> rows = new ArrayList<>();

            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // Extract container number
                Cell containerCell = row.getCell(containerColIndex);
                String containerNo = cellToString(containerCell);
                if (containerNo == null || containerNo.trim().isEmpty()) continue;
                containerNo = containerNo.trim().toUpperCase();

                if (containerNo.length() < 6 || containerNo.length() > 20) continue;

                // Extract all other columns dynamically (no hardcoded mappings)
                Map<String, String> otherColumns = new HashMap<>();

                for (Map.Entry<Integer, String> entry : columnHeaders.entrySet()) {
                    int colIndex = entry.getKey();
                    String headerName = entry.getValue();

                    if (colIndex == containerColIndex) continue; // Skip container column

                    Cell cell = row.getCell(colIndex);
                    String cellValue = cellToString(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        otherColumns.put(headerName, cellValue.trim());
                    }
                }

                // Build row with all columns stored in otherColumns map
                // Service layer will map these to UpdateCimuRequest fields dynamically
                rows.add(ExcelInspectionRow.builder()
                        .containerNo(containerNo)
                        .otherColumns(otherColumns)
                        .build());
            }

            log.info("Parsed excel rows | count={} headerRow={} containerCol={}",
                    rows.size(), headerRowIndex, containerColIndex);
            return rows;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse Excel file: " + e.getMessage(), e);
        }
    }

    private static String cellToString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception ex) {
                    yield null;
                }
            }
            default -> null;
        };
    }
}


