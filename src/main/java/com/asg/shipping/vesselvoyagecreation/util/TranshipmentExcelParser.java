package com.asg.shipping.vesselvoyagecreation.util;

import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageTranshipDtlEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class TranshipmentExcelParser {

    private static final Pattern CONTAINER_PATTERN = Pattern.compile("\\b([A-Z]{4}\\d{7})\\b");

    private TranshipmentExcelParser() {}

    public static List<ShipVoyageTranshipDtlEntity> parseTranshipmentExcel(InputStream in, Long voyagePoid, long startDetRowId) {
        List<ShipVoyageTranshipDtlEntity> result = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return result;

            int headerRowNum = -1;
            Map<String, Integer> colMap = new HashMap<>();

            // 1. Locate header row by searching for known column names
            for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 30); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, Integer> tempMap = new HashMap<>();
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null) continue;
                    String val = getCellValueAsString(cell).trim().toLowerCase();
                    if (val.isEmpty()) continue;

                    if (val.contains("container") || val.contains("cntr") || val.contains("eqp")) tempMap.put("containerNo", c);
                    else if (val.contains("iso") || val.contains("size") || val.contains("type")) tempMap.put("isoCode", c);
                    else if (val.contains("category") || val.contains("status")) tempMap.put("status", c);
                    else if (val.contains("origin")) tempMap.put("origin", c);
                    else if (val.contains("pol")) tempMap.put("pol", c);
                    else if (val.contains("seal 01") || val.contains("seal 1") || val.contains("seal")) tempMap.put("sealNo", c);
                    else if (val.contains("reefer") || val.contains("refer")) tempMap.put("isRefer", c);
                    else if (val.contains("tmp") || val.contains("temp")) tempMap.put("refferTemp", c);
                    else if (val.contains("imo code 01") || val.contains("imo 01") || val.contains("imo 1") || val.contains("imo")) tempMap.put("imoCode1", c);
                    else if (val.contains("un no 01") || val.contains("un 01") || val.contains("un 1") || val.contains("un no")) tempMap.put("unNo1", c);
                    else if (val.contains("weight")) tempMap.put("weight", c);
                    else if (val.contains("line operator") || val.contains("operator")) tempMap.put("lineOperator", c);
                }

                if (tempMap.containsKey("containerNo") || tempMap.containsKey("status") || tempMap.containsKey("pol") || tempMap.size() >= 3) {
                    headerRowNum = r;
                    colMap = tempMap;
                    log.info("Found Excel header at row {}: {}", headerRowNum, colMap);
                    break;
                }
            }

            if (headerRowNum == -1) {
                log.warn("Header row not detected in Excel file for voyagePoid={}", voyagePoid);
                headerRowNum = 11; // Fallback to row 12 (0-indexed 11)
            }

            long currentDetRowId = startDetRowId;

            // 2. Process data rows below header
            for (int r = headerRowNum + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String containerNo = getCell(row, colMap.get("containerNo"));
                String lineOperator = getCell(row, colMap.get("lineOperator"));
                String statusStr = getCell(row, colMap.get("status"));
                String polStr = getCell(row, colMap.get("pol"));
                String originStr = getCell(row, colMap.get("origin"));

                // If container number cell doesn't match a standard 11-char container, check entire row for container code
                if (containerNo == null || !CONTAINER_PATTERN.matcher(containerNo).find()) {
                    containerNo = findContainerInRow(row);
                }

                // Skip completely empty rows
                if ((containerNo == null || containerNo.isBlank()) && (statusStr == null || statusStr.isBlank()) && (polStr == null || polStr.isBlank())) {
                    continue;
                }

                currentDetRowId++;
                ShipVoyageTranshipDtlEntity entity = ShipVoyageTranshipDtlEntity.builder()
                        .transactionPoid(voyagePoid)
                        .detRowId(currentDetRowId)
                        .containerNo(containerNo != null && !containerNo.isBlank() ? containerNo : "CONTAINER-" + currentDetRowId)
                        .isoCode(getCell(row, colMap.get("isoCode")))
                        .status(statusStr != null && !statusStr.isBlank() ? statusStr : "FCL")
                        .origin(originStr)
                        .pol(polStr)
                        .sealNo(getCell(row, colMap.get("sealNo")))
                        .isRefer(getCell(row, colMap.get("isRefer")))
                        .refferTemp(getCell(row, colMap.get("refferTemp")))
                        .imoCode1(getCell(row, colMap.get("imoCode1")))
                        .unNo1(getCell(row, colMap.get("unNo1")))
                        .weightKg(getCell(row, colMap.get("weight")))
                        .isLoaded("Y")
                        .loadTransactionPoid(voyagePoid)
                        .build();

                result.add(entity);
            }

            log.info("Parsed {} transhipment rows from Excel file for voyagePoid={}", result.size(), voyagePoid);
            return result;
        } catch (Exception e) {
            log.error("Failed to parse transhipment Excel file for voyagePoid={}: {}", voyagePoid, e.getMessage(), e);
            return result;
        }
    }

    private static String findContainerInRow(Row row) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell == null) continue;
            String val = getCellValueAsString(cell).trim();
            Matcher matcher = CONTAINER_PATTERN.matcher(val);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static String getCell(Row row, Integer colIdx) {
        if (colIdx == null || colIdx < 0 || colIdx >= row.getLastCellNum()) return null;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        String val = getCellValueAsString(cell).trim();
        return val.isEmpty() ? null : val;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue().toString() : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
