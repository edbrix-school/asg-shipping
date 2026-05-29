package com.asg.shipping.bookingFormSH.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.*;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.bookingFormSH.dto.*;
import com.asg.shipping.bookingFormSH.entity.*;
import com.asg.shipping.bookingFormSH.repository.*;
import com.asg.shipping.bookingFormSH.util.BookingFormMapper;
import com.asg.shipping.bookingFormSH.util.TriConsumer;
import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.common.dto.ValidationError;
import com.asg.shipping.common.entity.GlobalAddressDetails;
import com.asg.shipping.common.repository.GlobalAddressDetailsRepository;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.exceptions.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.CallableStatement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingFormServiceImpl implements BookingFormService {

    private final ShipMateHdrRepository headerRepository;
    private final ShipMateCargoDtlRepository cargoDtlRepository;
    private final ShipMateChargesDtlRepository chargesDtlRepository;
    private final ShipMateContainerDtlRepository containerDtlRepository;
    private final ShipMateStuffingDtlRepository stuffingDtlRepository;
    private final GlobalAddressDetailsRepository globalAddressDetailsRepository;
    private final BookingFormLovService lovService;
    private final DocumentDeleteService documentDeleteService;
    private final DocumentSearchService documentService;
    private final JdbcTemplate jdbcTemplate;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;
    private final LovDataService lovDataService;

    @PersistenceContext
    private final EntityManager entityManager;

    private static final String ISCREATED = "ISCREATED";
    private static final String ISUPDATED = "ISUPDATED";
    private static final String ISDELETED = "ISDELETED";
    private static final String NOCHANGES = "NOCHANGES";
    private static final String ALLOCATESPLITBOOKING = "ALLOCATESPLITBOOKING";
    private static final String TRANSACTIONPOID = "transactionPoid";
    private static final String BOOKINGFORM = "Booking Form";


    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchBookingForm(String docId, FilterRequestDto request, Pageable pageable, LocalDate startDate, LocalDate endDate) {
        log.info("startdate", startDate, endDate);
        log.info("Searching booking form records with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(),
                pageable.getPageSize());

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate,
                endDate);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted, "DOC_REF",
                "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)

    public Map<String, Object> searchContainerInventory(String docId, String containerNo, String equipmentIsoType, Long linePoid, Pageable pageable) {
        log.info("Searching container inventory, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (StringUtil.isNotBlank(containerNo)) {
            conditions.add("UPPER(CONTAINER_NO) LIKE UPPER(?)");
            params.add("%" + containerNo.trim() + "%");
        }

        if (StringUtil.isNotBlank(equipmentIsoType)) {
            conditions.add("UPPER(EQUIPMENT_ISO_TYPE) LIKE UPPER(?)");
            params.add("%" + equipmentIsoType.trim() + "%");
        }

        if (linePoid != null) {
            conditions.add("LINE_POID = ?");
            params.add(linePoid);
        }

        conditions.add("LOAD_FULL IS NULL");

        if (!conditions.isEmpty()) {
            where.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        long offset = (long) pageable.getPageNumber() * pageable.getPageSize();
        long limit = offset + pageable.getPageSize();

        String dataQuery = "SELECT * FROM (SELECT a.*, ROWNUM rn FROM (" +
                "SELECT * FROM VW_CONTAINER_INVENTORY_EMPTYIN" + where +
                ") a WHERE ROWNUM <= ?) WHERE rn > ?";
        String countQuery = "SELECT COUNT(*) FROM VW_CONTAINER_INVENTORY_EMPTYIN" + where;

        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(limit);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(dataQuery, dataParams.toArray());
        Long total = jdbcTemplate.queryForObject(countQuery, params.toArray(), Long.class);
        records.forEach(row -> row.remove("RN"));

        long totalCount = total != null ? total : 0L;

        Page<Map<String, Object>> page2 = new PageImpl<>(records, pageable, totalCount);
        return PaginationUtil.wrapPage(page2, null);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingFormDto getBookingForm(Long id) {
        log.info("Getting booking form with id: {}", id);

        ShipMateHdr entity = headerRepository
                .findByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKINGFORM, TRANSACTIONPOID, id.toString()));

        // Load detail tables
        List<ShipMateCargoDtl> cargoDetails = cargoDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipMateChargesDtl> chargesDetails = chargesDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipMateContainerDtl> containerDetails = containerDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        List<ShipMateStuffingDtl> stuffingDetails = stuffingDtlRepository.findByTransactionPoidOrderByDetRowId(id);
        BookingFormDto dto = BookingFormMapper.mapToDto(entity);

        // Map detail tables
        dto.setCargoDetails(BookingFormMapper.mapCargoDtlListToDto(cargoDetails));
        dto.setChargesDetails(BookingFormMapper.mapChargesDtlListToDto(chargesDetails));
        dto.setContainerDetails(BookingFormMapper.mapContainerDtlListToDto(containerDetails));
        dto.setStuffingDetails(BookingFormMapper.mapStuffingDtlListToDto(stuffingDetails));
        // Enrich with LOV data
        enrichLovDetails(dto);
        return dto;
    }

    @Override
    @Transactional
    public BookingFormDto createBookingForm(BookingFormCreateDTO createDTO) {
        log.info("Creating new booking form");

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();

        validateLineMate(createDTO.getLinePoid(), null, groupPoid, companyPoid);

        ShipMateHdr entity = new ShipMateHdr();
        BookingFormMapper.mapCreateDTOToEntity(createDTO, entity, groupPoid, companyPoid);

        entity = headerRepository.saveAndFlush(entity);
        entityManager.refresh(entity);

        if (createDTO.getContainerDetails() != null) {
            for (BookingFormContainerDetailDto containerDto : createDTO.getContainerDetails()) {
                if (containerDto.getContainerNo() != null && containerDto.getContainerNo().length() >= 3) {
                    validateContainerLoad(containerDto.getContainerNo(), entity.getLinePoid(),
                            entity.getTransactionPoid(), groupPoid, companyPoid);
                }
            }
        }

        // Save detail tables
        saveDetailTables(entity.getTransactionPoid(), createDTO.getCargoDetails(), createDTO.getChargesDetails(),
                createDTO.getContainerDetails(), createDTO.getStuffingDetails());

        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), entity.getTransactionPoid().toString(), String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), entity.getDocRef()));
        // Reload and return
        return getBookingForm(entity.getTransactionPoid());
    }

    @Override
    @Transactional
    public void updateBookingForm(Long id, BookingFormUpdateDTO updateDTO) {
        log.info("Updating booking form with id: {}", id);

        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();

        ShipMateHdr existingData = headerRepository
                .findByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKINGFORM, TRANSACTIONPOID, id.toString()));

        if ("Y".equals(existingData.getDeleted())) {
            throw new ResourceNotFoundException(BOOKINGFORM, TRANSACTIONPOID, id.toString());
        }

        // Validate LINE_POID and MATE relationship before save
        if (updateDTO.getLinePoid() != null) {
            validateLineMate(updateDTO.getLinePoid(), id, groupPoid, companyPoid);
        }

        ShipMateHdr oldSnapshot = new ShipMateHdr();
        BeanUtils.copyProperties(existingData, oldSnapshot);

        ShipMateHdr entity = new ShipMateHdr();
        BeanUtils.copyProperties(existingData, entity);

        // Update header
        BookingFormMapper.mapUpdateDTOToEntity(updateDTO, entity);

        // Validate BOOKING_ISSUE_NO uniqueness if changed
        if (updateDTO.getBookingIssueNo() != null
                && !updateDTO.getBookingIssueNo().equals(entity.getBookingIssueNo())) {
            if (headerRepository.existsByBookingIssueNoAndNotDeletedExcludingPoid(updateDTO.getBookingIssueNo(), id)) {
                throw new ValidationException("Booking Issue Number already exists: " + updateDTO.getBookingIssueNo());
            }
            entity.setBookingIssueNo(updateDTO.getBookingIssueNo());
        }

        // Validate containers before saving details
        if (updateDTO.getContainerDetails() != null) {
            Long linePoid = updateDTO.getLinePoid() != null ? updateDTO.getLinePoid() : entity.getLinePoid();
            for (BookingFormContainerDetailDto containerDto : updateDTO.getContainerDetails()) {
                if (containerDto.getContainerNo() != null && containerDto.getContainerNo().length() >= 3) {
                    validateContainerLoad(containerDto.getContainerNo(), linePoid, id, groupPoid, companyPoid);
                }
            }
        }

        // Save updated detail tables
        saveDetailTables(id, updateDTO.getCargoDetails(), updateDTO.getChargesDetails(),
                updateDTO.getContainerDetails(), updateDTO.getStuffingDetails());
        headerRepository.save(entity);

        // Call PROC_SHIP_BL_PAGE_SAVE_AFTER after save
        Long userPoid = UserContext.getUserPoid();
//        callProcShipBlPageSaveAfter(groupPoid, companyPoid, id, null, ALLOCATESPLITBOOKING, userPoid);
        String key = id.toString();
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldSnapshot, entity, ShipMateHdr.class, docId, key, LogDetailsEnum.MODIFIED,
                "TRANSACTION_POID");
    }

    @Override
    @Transactional
    public void deleteBookingForm(Long id, DeleteReasonDto deleteReason) {
        log.info("Deleting booking form with id: {}", id);

        ShipMateHdr entity = headerRepository
                .findByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKINGFORM, TRANSACTIONPOID, id.toString()));

        documentDeleteService.deleteDocument(id, "SHIP_MATE_HDR",
                "TRANSACTION_POID", deleteReason, entity.getTransactionDate());
    }

    @Override
    @Transactional
    public String generateCoprarBooking(Long transactionPoid) {
        log.info("Generating COPRAR booking file for transaction: {}", transactionPoid);

        Long userPoid = UserContext.getUserPoid();

        try {
            String sql = "{call proc_coprar_BOOKING_V2(?, ?, ?)}";
            String res = jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
                CallableStatement cs = connection.prepareCall(sql);
                cs.setLong(1, transactionPoid);
                cs.setLong(2, userPoid);
                cs.registerOutParameter(3, Types.VARCHAR);
                cs.execute();
                String status = cs.getString(3);
                cs.close();
                return status;
            });
            if (res != null && !res.toLowerCase().startsWith("error"))
                return generateCoprarFile(transactionPoid, userPoid);
            return res;
        } catch (Exception e) {
            log.error("Error generating COPRAR booking file for transaction: {}", transactionPoid, e);
            throw new ValidationException("Error generating COPRAR booking file: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getEmptyShipper(Long companyPoid) {
        log.info("Getting empty shipper for company: {}", companyPoid);

        try {
            String sql = "{? = call FUNC_MATE_EMPTY_SHIPPER(?)}";
            return jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
                CallableStatement cs = connection.prepareCall(sql);
                cs.registerOutParameter(1, Types.VARCHAR);
                cs.setLong(2, companyPoid);
                cs.execute();
                String result = cs.getString(1);
                cs.close();
                return result;
            });
        } catch (Exception e) {
            log.error("Error getting empty shipper for company: {}", companyPoid, e);
            throw new ValidationException("Error getting empty shipper: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String processEmptyContainerLoad(Long transactionPoid) {
        log.info("Processing empty container load for transaction: {}", transactionPoid);

        Long groupPoid = UserContext.getGroupPoid();
        Long userPoid = UserContext.getUserPoid();
        Long companyPoid = UserContext.getCompanyPoid();

        try {
            String sql = "{call PROC_SHIP_EMPTY_CONTAINER_LOAD(?, ?, ?, ?, ?)}";
            return jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
                CallableStatement cs = connection.prepareCall(sql);
                cs.setLong(1, groupPoid);
                cs.setLong(2, userPoid);
                cs.setLong(3, companyPoid);
                cs.setLong(4, transactionPoid);
                cs.registerOutParameter(5, Types.VARCHAR);
                cs.execute();
                String status = cs.getString(5);
                cs.close();
                return status;
            });
        } catch (Exception e) {
            log.error("Error processing empty container load for transaction: {}", transactionPoid, e);
            throw new ValidationException("Error processing empty container load: " + e.getMessage());
        }
    }

    private void validateLineMate(Long linePoid, Long transactionPoid, Long groupPoid, Long companyPoid) {
        try {
            String sql = "{call PROC_SHIP_MATE_VLD_LINE_MATE(?, ?, ?, ?, ?)}";
            jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
                CallableStatement cs = connection.prepareCall(sql);
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, linePoid);
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.setObject(5, transactionPoid);
                cs.execute();
                String status = cs.getString(4);
                cs.close();
                if (status == null || status.contains("ERROR")) {
                    throw new ValidationException("Line and Mate validation failed: " + status);
                }
                return null;
            });
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating line and mate for linePoid: {}, transactionPoid: {}", linePoid, transactionPoid,
                    e);
            throw new ValidationException("Error validating line and mate: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void transferBookingWithContainers(Long oldTransactionPoid) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();
        ShipMateHdr entity = headerRepository
                .findByTransactionPoid(oldTransactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKINGFORM, TRANSACTIONPOID, oldTransactionPoid));

        callProcShipBlPageSaveAfter(groupPoid, companyPoid, oldTransactionPoid, entity.getSplitBookingNo(), ALLOCATESPLITBOOKING, userPoid);
    }

    @Override
    public byte[] exportStuffingAdviceExcel(Long id) {
        ShipMateHdr entity = headerRepository
                .findByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException(BOOKINGFORM, TRANSACTIONPOID, id.toString()));
        List<ShipMateStuffingDtl> stuffingDetails =
                stuffingDtlRepository.findByTransactionPoidOrderByDetRowId(id);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Stuffing Advice");
        XSSFColor yellow = new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 0}, null);
        XSSFColor blue = new XSSFColor(new byte[]{(byte) 0, (byte) 255, (byte) 255}, null);
        XSSFColor gray = new XSSFColor(new byte[]{(byte) 166, (byte) 166, (byte) 166}, null);

        DataFormat dataFormat = workbook.createDataFormat();
        XSSFFont boldFont = workbook.createFont();
        boldFont.setBold(true);

        XSSFFont titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);

        XSSFCellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        XSSFCellStyle yellowBoldLeft = workbook.createCellStyle();
        yellowBoldLeft.setFont(boldFont);
        yellowBoldLeft.setFillForegroundColor(yellow);
        yellowBoldLeft.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        yellowBoldLeft.setAlignment(HorizontalAlignment.LEFT);
        yellowBoldLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThickBorders(yellowBoldLeft);

        XSSFCellStyle yellowBoldCenter = workbook.createCellStyle();
        yellowBoldCenter.setFont(boldFont);
        yellowBoldCenter.setFillForegroundColor(yellow);
        yellowBoldCenter.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        yellowBoldCenter.setAlignment(HorizontalAlignment.CENTER);
        yellowBoldCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThickBorders(yellowBoldCenter);

        XSSFCellStyle yellowTotalDecStyle = workbook.createCellStyle();
        yellowTotalDecStyle.setFont(boldFont);
        yellowTotalDecStyle.setFillForegroundColor(yellow);
        yellowTotalDecStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        yellowTotalDecStyle.setDataFormat(dataFormat.getFormat("0.000"));
        yellowTotalDecStyle.setAlignment(HorizontalAlignment.CENTER);
        yellowTotalDecStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThickBorders(yellowTotalDecStyle);

        XSSFCellStyle yellowHeaderStyle = workbook.createCellStyle();
        yellowHeaderStyle.setFont(boldFont);
        yellowHeaderStyle.setFillForegroundColor(yellow);
        yellowHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        yellowHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        yellowHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        yellowHeaderStyle.setWrapText(true);
        applyThickBorders(yellowHeaderStyle);

        XSSFCellStyle blueHeaderStyle = workbook.createCellStyle();
        blueHeaderStyle.setFont(boldFont);
        blueHeaderStyle.setFillForegroundColor(blue);
        blueHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        blueHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        blueHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        blueHeaderStyle.setWrapText(true);
        applyThickBorders(blueHeaderStyle);

        XSSFCellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(dataStyle);

        XSSFCellStyle decimalStyle = workbook.createCellStyle();
        decimalStyle.setDataFormat(dataFormat.getFormat("0.000"));
        decimalStyle.setAlignment(HorizontalAlignment.CENTER);
        decimalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(decimalStyle);

        XSSFCellStyle grayStyle = workbook.createCellStyle();
        grayStyle.setFillForegroundColor(gray);
        grayStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        grayStyle.setAlignment(HorizontalAlignment.LEFT);
        grayStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorders(grayStyle);

        XSSFCellStyle plainBoldStyle = workbook.createCellStyle();
        plainBoldStyle.setFont(boldFont);
        plainBoldStyle.setAlignment(HorizontalAlignment.LEFT);
        plainBoldStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        XSSFFont redBoldFont = workbook.createFont();
        redBoldFont.setBold(true);
        redBoldFont.setColor(IndexedColors.RED.getIndex());

        XSSFCellStyle noteStyle = workbook.createCellStyle();
        noteStyle.setFont(redBoldFont);
        noteStyle.setFillForegroundColor(yellow);
        noteStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        noteStyle.setAlignment(HorizontalAlignment.LEFT);
        noteStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThickBorders(noteStyle);

        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 12 * 256);
        sheet.setColumnWidth(3, 12 * 256);
        sheet.setColumnWidth(4, 16 * 256);
        sheet.setColumnWidth(5, 14 * 256);
        sheet.setColumnWidth(6, 12 * 256);
        sheet.setColumnWidth(7, 13 * 256);

        int r = 0;
        Row row0 = sheet.createRow(r);
        row0.setHeightInPoints(28);
        Cell titleCell = row0.createCell(0);
        titleCell.setCellValue("STUFFING ADVICE");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 7));
        r++;

        Row row1 = sheet.createRow(r);
        row1.setHeightInPoints(16);
        String dateStr = "DATE: " + new java.text.SimpleDateFormat("d/M/yyyy").format(new java.util.Date());
        Cell dateCell = row1.createCell(4);
        dateCell.setCellValue(dateStr);
        dateCell.setCellStyle(yellowBoldCenter);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 4, 7));
        r++;

        Row row2 = sheet.createRow(r);
        row2.setHeightInPoints(16);

        Cell agencyCell = row2.createCell(0);
        agencyCell.setCellValue("AGENCY: NATIONAL SHIPPING AGENCY");
        agencyCell.setCellStyle(yellowBoldLeft);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 2));

        Cell bookingCell = row2.createCell(3);
        bookingCell.setCellValue("BOOKING NO :  " + nvl(entity.getBookingIssueNo()));
        bookingCell.setCellStyle(yellowBoldLeft);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 3, 7));
        r++;

        sheet.createRow(r).setHeightInPoints(8);
        r++;

        Row row4 = sheet.createRow(r);
        row4.setHeightInPoints(16);

        Cell vesselCell = row4.createCell(0);
        vesselCell.setCellValue("VESSEL NAME: " + nvl(entity.getVessalAgentName()));
        vesselCell.setCellStyle(yellowBoldLeft);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 2));

        Cell voyCell = row4.createCell(3);
        voyCell.setCellValue("VOY NBR: " + nvl(entity.getVoyageNo()));
        voyCell.setCellStyle(yellowBoldLeft);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 3, 7));
        r++;
        sheet.createRow(r).setHeightInPoints(8);
        r++;

        Row row6 = sheet.createRow(r);
        row6.setHeightInPoints(16);

        Cell etaCell = row6.createCell(0);
        etaCell.setCellValue("ETA: " + nvl(entity.getVesselEtaDate()));
        etaCell.setCellStyle(yellowBoldLeft);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 2));
        String lineName = "";
        if (entity.getLinePoid() != null) {
            List<LovItem> portList = lovService.getPortMasterLov(entity.getLinePoid());
            if (!portList.isEmpty()) {
                lineName = nvl(portList.get(0).getDescription());
            }
        }
        Cell lineCell = row6.createCell(3);
        lineCell.setCellValue("LINE: " + nvl(lineName));
        lineCell.setCellStyle(yellowBoldLeft);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 3, 7));
        r++;
        sheet.createRow(r).setHeightInPoints(8);
        r++;

        Row row8 = sheet.createRow(r);
        row8.setHeightInPoints(16);

        Cell shipperCell = row8.createCell(0);
        shipperCell.setCellValue("SHIPPER: " + nvl(entity.getShipperDetailsManually()));
        shipperCell.setCellStyle(yellowBoldLeft);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 2));
        String portName = "";
        if (entity.getPortOfDischargePoid() != null) {
            List<LovItem> portList = lovService.getPortMasterLov(entity.getPortOfDischargePoid());
            if (!portList.isEmpty()) {
                portName = nvl(portList.get(0).getDescription());
            }
        }
        Cell portCell = row8.createCell(3);
        portCell.setCellValue("PORT OF DISCHARGE :" + nvl(portName));
        portCell.setCellStyle(yellowBoldLeft);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 3, 7));
        r++;

        sheet.createRow(r).setHeightInPoints(8);
        r++;

        String[] columns = {
                "S/N",
                "CONTAINER NBR",
                "SEAL NBR",
                "CONT SIZE",
                "MARKS\n(ALBA order no)",
                "COLOUR CODE",
                "QTY OF\nBUNDLES",
                "WEIGHT\nTONNES"
        };

        Row headerRow = sheet.createRow(r);
        headerRow.setHeightInPoints(40);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            if (i == 0 || i == 1 || i == 2 || i == 7) {
                cell.setCellStyle(blueHeaderStyle);
            } else {
                cell.setCellStyle(yellowHeaderStyle);
            }
        }
        r++;

        double totalBundles = 0;
        double totalWeight = 0;
        int serialNo = 1;
        String marks = "";

        for (ShipMateStuffingDtl dtl : stuffingDetails) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(16);

            Cell c0 = row.createCell(0);
            c0.setCellValue(serialNo++);
            c0.setCellStyle(dataStyle);

            Cell c1 = row.createCell(1);
            c1.setCellValue(nvl(dtl.getContainerNo()));
            c1.setCellStyle(dataStyle);

            Cell c2 = row.createCell(2);
            c2.setCellValue(nvl(dtl.getEquipmentSealNo()));
            c2.setCellStyle(dataStyle);

            Cell c3 = row.createCell(3);
            c3.setCellValue(nvl(dtl.getEquipmentIsoType()));
            c3.setCellStyle(dataStyle);

            Cell c4 = row.createCell(4);
            c4.setCellValue(nvl(dtl.getMarks()));
            marks = dtl.getMarks();
            c4.setCellStyle(dataStyle);

            Cell c5 = row.createCell(5);
            c5.setCellValue(nvl(dtl.getColourCode()));
            c5.setCellStyle(dataStyle);

            double bundles = Double.parseDouble(nvl(dtl.getQtyOfBundles()));
            Cell c6 = row.createCell(6);
            c6.setCellValue(bundles);
            c6.setCellStyle(dataStyle);
            totalBundles += bundles;

            double weight = Double.parseDouble(nvl(dtl.getWeightTonnes()));
            Cell c7 = row.createCell(7);
            c7.setCellValue(weight);
            c7.setCellStyle(decimalStyle);
            totalWeight += weight;
        }

        Row totalRow = sheet.createRow(r++);
        totalRow.setHeightInPoints(16);

        totalRow.createCell(0).setCellStyle(dataStyle);
        totalRow.createCell(1).setCellStyle(dataStyle);
        totalRow.createCell(2).setCellStyle(dataStyle);
        totalRow.createCell(3).setCellStyle(dataStyle);
        totalRow.createCell(4).setCellStyle(dataStyle);

        Cell totalLabel = totalRow.createCell(5);
        totalLabel.setCellValue("TOTAL");
        totalLabel.setCellStyle(yellowBoldCenter);

        Cell totalB = totalRow.createCell(6);
        totalB.setCellValue(totalBundles);
        totalB.setCellStyle(yellowBoldCenter);

        Cell totalW = totalRow.createCell(7);
        totalW.setCellValue(totalWeight);
        totalW.setCellStyle(yellowTotalDecStyle);

        sheet.createRow(r++).setHeightInPoints(8);
        Row remarksLabelRow = sheet.createRow(r++);
        remarksLabelRow.setHeightInPoints(16);
        Cell remarksLabel = remarksLabelRow.createCell(0);
        remarksLabel.setCellValue("REMARKS:");
        remarksLabel.setCellStyle(plainBoldStyle);

        String[] remarksLines = {
                marks,
                portName,
                "MADE IN BAHRAIN"
        };
        for (String line : remarksLines) {
            if (line.isBlank()) continue;
            Row rv = sheet.createRow(r);
            rv.setHeightInPoints(16);
            Cell vc = rv.createCell(0);
            vc.setCellValue(line);
            vc.setCellStyle(grayStyle);
            sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 3));
            r++;
        }
        sheet.createRow(r++).setHeightInPoints(8);
        Row noteRow = sheet.createRow(r);
        noteRow.setHeightInPoints(16);
        Cell noteCell = noteRow.createCell(0);
        noteCell.setCellValue("NOTE : Pls allocate HEAVY DUTY containers for above booking");
        noteCell.setCellStyle(noteStyle); // yellow bg + red bold text
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 7));
        r++;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            workbook.write(out);
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    private String nvl(Object val) {
        return val != null ? val.toString() : "";
    }

    private void applyThickBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THICK);
        style.setBorderBottom(BorderStyle.THICK);
        style.setBorderLeft(BorderStyle.THICK);
        style.setBorderRight(BorderStyle.THICK);
    }

    private void applyThinBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    public String generateCoprarFile(Long bookingTransactionPoid, Long loginUserPoid) {
        try {

            String timestamp = new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date());

            String fileName = "COPRANBAH_" + timestamp + "_" + bookingTransactionPoid + ".TXT";

            String directory = "/win3mount/EMAILS_FROM_ORACLE";

            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path filePath = Paths.get(directory, fileName);

            List<String> ediLines = jdbcTemplate.query("""
                    SELECT TAB_TEXT
                    FROM EDI_COPRAR_CREATED
                    WHERE BOOKING_TRANSACTION_POID = ?
                    ORDER BY BOOKING_TRANSACTION_POID, SEQNO
                    """, (rs, rowNum) -> rs.getString("TAB_TEXT"), bookingTransactionPoid);

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {

                for (String line : ediLines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            queueMail(loginUserPoid, fileName);

            return "Copran generated, Sent Mail...";

        } catch (Exception e) {
            return "ERROR : " + e.getMessage();
        }
    }

    private void queueMail(Long userPoid, String fileName) {
        jdbcTemplate.update("""
                INSERT INTO GLOBAL_MAIL_SENDING_QUEUE
                (RECEVER_EMAIL_ID, MESSAGE_SUBJECT, MESSGE_BODY,
                 SENT_STATUS, QUEUE_DATE, SENDING_POID,
                 ATTACHED_FILE_NAME, LINK_TRANSACTION_POID)
                SELECT NVL(USER_EMAIL,'OPS@SHIPPINGBAHRAIN.COM'),
                       'COPRAN EXPORT BOOKING EDI ' || TO_CHAR(SYSDATE,'DD-MON-RRRR'),
                       'COPRAN MATE BOOKING ' || TO_CHAR(SYSDATE,'DD-MON-RRRR'),
                       'N', SYSDATE, ?, ?, NULL
                FROM GLOBAL_USERS
                WHERE USER_POID = ?
                """, userPoid, fileName, userPoid);
    }

    /**
     * Validate container load using PROC_SHIP_VLD_CONTAINER_LOAD
     */
    private void validateContainerLoad(String containerNo, Long linePoid, Long transactionPoid, Long groupPoid,
                                       Long companyPoid) {
        try {
            String sql = "{call PROC_SHIP_VLD_CONTAINER_LOAD(?, ?, ?, ?, ?, ?)}";
            jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
                CallableStatement cs = connection.prepareCall(sql);
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setString(3, containerNo);
                cs.setLong(4, linePoid);
                cs.registerOutParameter(5, Types.VARCHAR);
                cs.setLong(6, transactionPoid);
                cs.execute();
                String status = cs.getString(5);
                cs.close();
                if (status == null || status.contains("ERROR")) {
                    throw new ValidationException(
                            "Container load validation failed for container " + containerNo + ": " + status);
                }
                return null;
            });
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating container load for containerNo: {}, linePoid: {}, transactionPoid: {}",
                    containerNo, linePoid, transactionPoid, e);
            throw new ValidationException("Error validating container load: " + e.getMessage());
        }
    }

    /**
     * Call PROC_SHIP_BL_PAGE_SAVE_AFTER stored procedure
     */
    private void callProcShipBlPageSaveAfter(Long groupPoid, Long companyPoid, Long transactionPoid,
                                             Long splitBookingNo, String actionType, Long userPoid) {
        try {
            String sql = "{call PROC_SHIP_BL_PAGE_SAVE_AFTER(?, ?, ?, ?, ?, ?)}";
            jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
                CallableStatement cs = connection.prepareCall(sql);
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, splitBookingNo != null ? splitBookingNo : 0L);
                cs.setLong(4, transactionPoid);
                cs.setString(5, actionType != null ? actionType : ALLOCATESPLITBOOKING);
                cs.setString(6, String.valueOf(userPoid));
                cs.execute();
                cs.close();
                return null;
            });
            log.debug("Successfully called PROC_SHIP_BL_PAGE_SAVE_AFTER for transaction: {}", transactionPoid);
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_BL_PAGE_SAVE_AFTER for transaction: {}", transactionPoid, e);
        }
    }

    /**
     * Save detail tables
     */

    private <D, E> void processDetails(
            Long transactionPoid,
            List<D> dtos,
            Long maxDetRowId,
            Function<D, String> actionExtractor,
            Function<D, Long> rowIdExtractor,
            BiFunction<D, Long, E> createMapper,
            TriConsumer<D, E, Long> updateMapper,
            BiFunction<Long, Long, Optional<E>> fetchExisting,
            Function<List<E>, List<E>> saveAll,
            BiConsumer<Long, List<Long>> deleteAll,
            Function<E, Long> entityRowId,
            String logPrefix
    ) {

        if (dtos == null || dtos.isEmpty()) return;

        String docId = UserContext.getDocumentId();
        String docKey = transactionPoid.toString();

        List<E> toSave = new ArrayList<>();
        List<E> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<E>> logRequests = new ArrayList<>();

        for (D dto : dtos) {

            String action = actionExtractor.apply(dto) != null ? actionExtractor.apply(dto).toUpperCase() : ISCREATED;
            Long rowId = rowIdExtractor.apply(dto);

            switch (action) {

                case ISCREATED -> {
                    E entity = createMapper.apply(dto, transactionPoid);
                    setRowId(entity, rowId != null ? rowId : ++maxDetRowId);
                    toSave.add(entity);
                }

                case ISUPDATED -> {
                    E existingData = fetchExisting.apply(transactionPoid, rowId)
                            .orElseThrow(() -> new ValidationException(
                                    logPrefix + " detail not found for detRowId: " + rowId));

                    E oldEntity = cloneEntity(existingData);
                    E updated = cloneEntity(existingData);

                    updateMapper.accept(dto, updated, transactionPoid);
                    toUpdate.add(updated);

                    logRequests.add(new LogRequestDto<>(
                            oldEntity, updated, (Class<E>) updated.getClass(),
                            docId, docKey,
                            logPrefix + " DET_ROW_ID: " + rowId
                    ));
                }

                case ISDELETED -> {
                    toDelete.add(rowId);
                    loggingService.logDelete(dto, docId, docKey);
                }

                case NOCHANGES -> {
                }

                default -> throw new ValidationException("Invalid action: " + action);
            }
        }

        if (!toSave.isEmpty()) {
            List<E> saved = saveAll.apply(toSave);
            saved.forEach(e -> loggingService.createLogSummaryEntry(
                    docId, docKey,
                    logPrefix + " detail created with detRowId: " + entityRowId.apply(e)
            ));
        }

        if (!toUpdate.isEmpty()) {
            saveAll.apply(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }

        if (!toDelete.isEmpty()) {
            deleteAll.accept(transactionPoid, toDelete);
        }
    }

    private void saveDetailTables(
            Long transactionPoid,
            List<BookingFormCargoDetailDtoRequest> cargoDetails,
            List<BookingFormChargesDetailDtoRequest> chargesDetails,
            List<BookingFormContainerDetailDtoRequest> containerDetails,
            List<BookingFormStuffingLoadDetailDtoRequest> stuffingDetails) {

        /* -------------------- CARGO -------------------- */
        processDetails(
                transactionPoid,
                cargoDetails,
                cargoDtlRepository.getMaxDetRowId(transactionPoid),
                BookingFormCargoDetailDtoRequest::getActionType,
                BookingFormCargoDetailDtoRequest::getDetRowId,
                BookingFormMapper::mapCargoDtlFromDto,
                this::mapCargoDtlFromDto,
                cargoDtlRepository::findByTransactionPoidAndDetRowId,
                cargoDtlRepository::saveAll,
                cargoDtlRepository::deleteByTransactionPoidAndDetRowIdIn,
                ShipMateCargoDtl::getDetRowId,
                "Cargo"
        );

        /* -------------------- CHARGES -------------------- */
        processDetails(
                transactionPoid,
                chargesDetails,
                chargesDtlRepository.getMaxDetRowId(transactionPoid),
                BookingFormChargesDetailDtoRequest::getActionType,
                BookingFormChargesDetailDtoRequest::getDetRowId,
                BookingFormMapper::mapChargesDtlFromDto,
                this::mapChargesDtlFromDto,
                chargesDtlRepository::findByTransactionPoidAndDetRowId,
                chargesDtlRepository::saveAll,
                chargesDtlRepository::deleteByTransactionPoidAndDetRowIdIn,
                ShipMateChargesDtl::getDetRowId,
                "charges"
        );

        /* -------------------- CONTAINER -------------------- */
        processDetails(
                transactionPoid,
                containerDetails,
                containerDtlRepository.getMaxDetRowId(transactionPoid),
                BookingFormContainerDetailDtoRequest::getActionType,
                BookingFormContainerDetailDtoRequest::getDetRowId,
                BookingFormMapper::mapContainerDtlFromDto,
                this::mapContainerDtlFromDto,
                containerDtlRepository::findByTransactionPoidAndDetRowId,
                containerDtlRepository::saveAll,
                containerDtlRepository::deleteByTransactionPoidAndDetRowIdIn,
                ShipMateContainerDtl::getDetRowId,
                "container"
        );

        /* -------------------- STUFFING  -------------------- */

        processDetails(
                transactionPoid,
                stuffingDetails,
                stuffingDtlRepository.getMaxDetRowId(transactionPoid),
                BookingFormStuffingLoadDetailDtoRequest::getActionType,
                BookingFormStuffingLoadDetailDtoRequest::getDetRowId,
                BookingFormMapper::mapStuffingDtlFromDto,
                this::mapStuffingDtlFromDto,
                stuffingDtlRepository::findByTransactionPoidAndDetRowId,
                stuffingDtlRepository::saveAll,
                stuffingDtlRepository::deleteByTransactionPoidAndDetRowIdIn,
                ShipMateStuffingDtl::getDetRowId,
                "stuffing"
        );

    }

    private <E> E cloneEntity(E source) {
        try {
            E target = (E) source.getClass().getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new IllegalArgumentException("Clone failed", e);
        }
    }

    private <E> void setRowId(E entity, Long rowId) {
        try {
            entity.getClass().getMethod("setDetRowId", Long.class).invoke(entity, rowId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set detRowId", e);
        }
    }

    private ShipMateCargoDtl mapCargoDtlFromDto(
            BookingFormCargoDetailDto dto,
            ShipMateCargoDtl entity,
            Long transactionPoid) {

        if (entity == null) {
            entity = new ShipMateCargoDtl();
        }

        entity.setTransactionPoid(transactionPoid);

        entity.setDetRowId(dto.getDetRowId());
        entity.setCargoDescription(dto.getCargoDescription());
        entity.setEquipmentType(dto.getEquipmentType());
        entity.setEquipmentSize(dto.getEquipmentSize());
        entity.setQuantity(dto.getQuantity());
        entity.setVolume(dto.getVolume());
        entity.setWeight(dto.getWeight());
        entity.setEquipmentIsoType(dto.getEquipmentIsoType());

        entity.setIsImco(dto.getIsImco());
        entity.setImo(dto.getImo());

        entity.setIsOog(dto.getIsOog());
        entity.setOogL(dto.getOogL());
        entity.setOogB(dto.getOogB());
        entity.setOogH(dto.getOogH());
        entity.setOogLW(dto.getOogLW());
        entity.setOogRW(dto.getOogRW());
        entity.setOogF(dto.getOogF());
        entity.setOogA(dto.getOogA());

        entity.setIsRefer(dto.getIsRefer());
        entity.setReferType(dto.getReferType());
        entity.setRefferTemp(dto.getRefferTemp());
        entity.setRefferHum(dto.getRefferHum());
        entity.setRefferVent(dto.getRefferVent());

        return entity;
    }


    private ShipMateChargesDtl mapChargesDtlFromDto(
            BookingFormChargesDetailDto dto,
            ShipMateChargesDtl entity,
            Long transactionPoid) {

        if (entity == null) {
            entity = new ShipMateChargesDtl();
        }

        entity.setTransactionPoid(transactionPoid);

        entity.setDetRowId(dto.getDetRowId());
        entity.setChargePoid(dto.getChargePoid());
        entity.setCurrencyExchange(dto.getCurrencyExchange());
        entity.setQuantity(dto.getQuantity());
        entity.setPerQuantityAmount(dto.getPerQuantityAmount());
        entity.setPaidAtPortPoid(dto.getPaidAtPortPoid());
        entity.setBuyPercharge(dto.getBuyPercharge());
        entity.setCurrencyCode(dto.getCurrencyCode());

        return entity;
    }


    private ShipMateContainerDtl mapContainerDtlFromDto(
            BookingFormContainerDetailDto dto,
            ShipMateContainerDtl entity,
            Long transactionPoid) {

        if (entity == null) {
            entity = new ShipMateContainerDtl();
        }

        entity.setTransactionPoid(transactionPoid);

        entity.setDetRowId(dto.getDetRowId());
        entity.setContainerNo(dto.getContainerNo());
        entity.setEquipmentSealNo(dto.getEquipmentSealNo());
        entity.setEquipmentIsoType(dto.getEquipmentIsoType());
        entity.setEquipmentType(dto.getEquipmentType());
        entity.setEquipmentSize(dto.getEquipmentSize());

        entity.setQuantity(dto.getQuantity());
        entity.setGrsVolume(dto.getGrsVolume());
        entity.setGrsWeight(dto.getGrsWeight());
        entity.setNetVolume(dto.getNetVolume());
        entity.setNetWeight(dto.getNetWeight());
        entity.setNoOfPacks(dto.getNoOfPacks());

        entity.setPackUnit(dto.getPackUnit());
        entity.setComodityPoid(dto.getComodityPoid());
        entity.setDestinationPortPoid(dto.getDestinationPortPoid());

        entity.setImo(dto.getImo());
        entity.setCargoDescription(dto.getCargoDescription());

        entity.setOogL(dto.getOogL());
        entity.setOogB(dto.getOogB());
        entity.setOogH(dto.getOogH());
        entity.setOogLW(dto.getOogLW());
        entity.setOogRW(dto.getOogRW());
        entity.setOogF(dto.getOogF());
        entity.setOogA(dto.getOogA());

        entity.setIsImco(dto.getIsImco());
        entity.setIsOog(dto.getIsOog());
        entity.setIsRefer(dto.getIsRefer());
        entity.setReferType(dto.getReferType());

        entity.setEquipmentShipperOwn(dto.getEquipmentShipperOwn());
        entity.setIssueToShipper(dto.getIssueToShipper());
        entity.setReturnFromShipper(dto.getReturnFromShipper());
        entity.setReleaseAllocation(dto.getReleaseAllocation());

        entity.setIsSplit(dto.getIsSplit());
        entity.setImcoClassType(dto.getImcoClassType());
        entity.setOogType(dto.getOogType());

        entity.setVgmWeight(dto.getVgmWeight());
        entity.setVgmDocId(dto.getVgmDocId());
        entity.setVgmDate(dto.getVgmDate());
        entity.setVgmEdi(dto.getVgmEdi());

        entity.setRefferHum(dto.getRefferHum());
        entity.setRefferTemp(dto.getRefferTemp());
        entity.setRefferVent(dto.getRefferVent());

        return entity;
    }

    private ShipMateStuffingDtl mapStuffingDtlFromDto(
            BookingFormStuffingLoadDetailDto dto,
            ShipMateStuffingDtl entity,
            Long transactionPoid) {

        if (entity == null) {
            entity = new ShipMateStuffingDtl();
        }

        entity.setTransactionPoid(transactionPoid);

        entity.setDetRowId(dto.getDetRowId());
        entity.setContainerNo(dto.getContainerNo());
        entity.setEquipmentSealNo(dto.getEquipmentSealNo());
        entity.setEquipmentIsoType(dto.getEquipmentIsoType());
        entity.setMarks(dto.getMarks());
        entity.setColourCode(dto.getColourCode());
        entity.setWeightTonnes(dto.getWeightTonnes());
        entity.setQtyOfBundles(dto.getQtyOfBundles());

        return entity;
    }

    private void enrichLovDetails(BookingFormDto dto) {
        if (dto.getQuotationTransactionPoid() != null) {
            dto.setQuotationTransactionPoidDet(
                    lovDataService.getDetailsByPoidAndLovNameFast(dto.getQuotationTransactionPoid(), "SHIP_QUOTATION_EXPORT")
            );
        }
        setLov(dto.getVesselPoid(), lovService::getVesselMasterLov, dto::setVesselPoidDet);
        setLov(dto.getLinePoid(), lovService::getLineMasterLov, dto::setLinePoidDet);
        setLov(dto.getSalesmanPoid(), lovService::getSalesmanLov, dto::setSalesmanPoidDet);
        setLov(dto.getComodityPoid(), lovService::getCommodityMasterLov, dto::setComodityPoidDet);

        setLov(dto.getPlaceOfRecieptPoid(), lovService::getPortMasterLov, dto::setPlaceOfRecieptPoidDet);
        setLov(dto.getPlaceOfDelieveryPoid(), lovService::getPortMasterLov, dto::setPlaceOfDelieveryPoidDet);
        setLov(dto.getPortOfLoadingPoid(), lovService::getPortMasterLov, dto::setPortOfLoadingPoidDet);
        setLov(dto.getPortOfDischargePoid(), lovService::getPortMasterLov, dto::setPortOfDischargePoidDet);

        setLov(dto.getMateLoadVoyagePoid(), lovService::getVoyageMasterLov, dto::setMateLoadVoyagePoidDet);

        enrichCargoDetails(dto);
        enrichChargeDetails(dto);
        enrichContainerDetails(dto);
    }

    private <T> void setLov(
            Long poid,
            Function<Long, List<T>> lovFetcher,
            Consumer<T> setter) {

        if (poid != null) {
            lovFetcher.apply(poid)
                    .stream()
                    .findFirst()
                    .ifPresent(setter);
        }
    }

    private void enrichChargeDetails(BookingFormDto dto) {
        if (dto.getChargesDetails() == null) return;

        dto.getChargesDetails().forEach(charge -> {
            setLov(charge.getChargePoid(), lovService::getChargeMasterLov, charge::setChargePoidDet);
            setLov(charge.getPaidAtPortPoid(), lovService::getPortMasterLov, charge::setPaidAtPortPoidDet);
        });
    }

    private void enrichCargoDetails(BookingFormDto dto) {
        if (dto.getChargesDetails() == null) return;

        dto.getCargoDetails().forEach(cargo -> {
            if (cargo.getEquipmentIsoType() != null) {
                lovService.getEquipmentIsoTypeLov(cargo.getEquipmentIsoType()).stream().findFirst().ifPresent(cargo::setEquipmentIsoTypeDet);
            }
        });
    }

    private void enrichContainerDetails(BookingFormDto dto) {
        if (dto.getContainerDetails() == null) return;

        dto.getContainerDetails().forEach(container -> {
            setLov(container.getComodityPoid(), lovService::getCommodityMasterLov, container::setComodityPoidDet);
            setLov(container.getDestinationPortPoid(), lovService::getPortMasterLov, container::setDestinationPortPoidDet);
            if (container.getEquipmentIsoType() != null && !container.getEquipmentIsoType().isBlank())
                lovService.getEquipmentIsoTypeLov(container.getEquipmentIsoType()).stream().findFirst().ifPresent(container::setEquipmentIsoTypeDet);
            if (container.getImcoClassType() != null && !container.getImcoClassType().isBlank()) {
                lovService.getImcoClassTypeLov(container.getImcoClassType())
                        .stream()
                        .findFirst()
                        .ifPresent(container::setImcoClassTypeDet);
            }
            if (container.getOogType() != null && !container.getOogType().isBlank()) {
                lovService.getOogTypeLov(container.getOogType())
                        .stream()
                        .findFirst()
                        .ifPresent(container::setOogTypeDet);
            }
        });
    }

    @Override
    public byte[] mateBookingPrintForm(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-140");
        JasperReport mainReport = printService.load("Shipping/SH/Container_mate_receipts.jrxml");
        params.put("SUBREPORT_CONTAINER_MATE_RECEIPTS_1",
                printService.load("Shipping/SH/Container_mate_receipts_subreport1.jrxml"));
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public byte[] cntEmptyBookingPrintForm(Long transactionPoid) throws Exception {
        String docId = UserContext.getDocumentId();
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, docId);
        JasperReport mainReport = printService.load("Shipping/SH/Container_Release.jrxml");
        params.put("SUBREPORT_CONTAINER_RELEASE_1",
                printService.load("Shipping/SH/Container_Release_subreport1.jrxml"));
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public byte[] cntReturnBookingPrintFormAll(Long transactionPoid, String printStamp) throws Exception {
        String docId = UserContext.getDocumentId();
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, docId);
        JasperReport mainReport = printService.load("Shipping/SH/Container_Return_ALL.jrxml");
        params.put("SUBREPORT_CONTAINER_RETURN_ALL_1", printService.load("Shipping/SH/Container_Return_subreport1_ALL.jrxml"));
        params.put("PRINT_STAMP", printStamp);
        params.put("ASG_STAMP", getClass().getClassLoader().getResource("jasper/Shipping/jpg/ASG_STAMP.jpg"));
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public byte[] cntReturnBookingPrintForm(Long transactionPoid, String printStamp, String containerNo) throws Exception {
        String docId = UserContext.getDocumentId();
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, docId);
        JasperReport mainReport = printService.load("Shipping/SH/Container_Return.jrxml");
        params.put("CONTAINER_RETURN_SUBREPORT_1", printService.load("Shipping/SH/Container_Return_subreport1.jrxml"));
        params.put("PRINT_STAMP", printStamp);
        params.put("P_CONTAINERNO", containerNo);
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    @Override
    public BookingFormAddressMasterDto getCustomerAddress(Long addressMasterPoid, String addressType) {
        GlobalAddressDetails entity = globalAddressDetailsRepository
                .findByAddressMasterPoidAndAddressType(addressMasterPoid, addressType)
                .orElseThrow(() -> new RuntimeException(
                        "Address not found for poid: " + addressMasterPoid + " and type: " + addressType
                ));
        return BookingFormMapper.mapAddressList(entity);
    }

    @Override
    @Transactional(timeout = 600)
    public String importFileWithTransaction(org.springframework.web.multipart.MultipartFile file, Long transactionPoid, Long groupPoid, Long companyPoid, Long userPoid) {
        if (file.isEmpty()) {
            throw new ValidationException(
                    "Ship Mate container file is empty",
                    List.of(new ValidationError("file", "Please select a valid Excel file"))
            );
        }

        String result = uploadDetailsFromExcel(transactionPoid, groupPoid, companyPoid, userPoid, file, false);

        // Log TDR file import action
        String logDetail = String.format("Ship Mate container file imported: %s", file.getOriginalFilename());
        loggingService.createLogSummaryEntry(UserContext.getDocumentId(), transactionPoid.toString(), logDetail);

        return result;
    }

    // Inner class for Excel configuration
    private static class ExcelConfig {
        int startRowNumber;
        int startColNumber;
        int endColNumber;
        String tempTableName;
        String excelSheetName;
    }

    @Transactional(timeout = 600)
    public String uploadDetailsFromExcel(Long transactionPoid, Long groupPoid, Long companyPoid, Long userPoid, org.springframework.web.multipart.MultipartFile file, boolean callStoredProcedure) {
        if (file.isEmpty()) {
            throw new ValidationException(
                    "File is empty",
                    List.of(new ValidationError("file", "Please select a valid Excel file"))
            );
        }

        String docId = "110-160_2";
        ExcelConfig config = getExcelConfig(docId);
        log.info("Excel config - startRowNumber: {}, startColNumber: {}, endColNumber: {}, tempTable: {}",
                config.startRowNumber, config.startColNumber, config.endColNumber, config.tempTableName);

        jdbcTemplate.update("DELETE FROM " + config.tempTableName);

        List<List<Object>> rowsCollection = new ArrayList<>();

        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
            if (workbook == null) {
                throw new ValidationException(
                        "Excel Workbook not able to open...",
                        List.of(new ValidationError("file", "Excel Workbook not able to open..."))
                );
            }

            org.apache.poi.ss.usermodel.Sheet sheet = config.excelSheetName != null
                    ? workbook.getSheet(config.excelSheetName)
                    : workbook.getSheetAt(0);

            if (sheet == null) {
                String sheetName = config.excelSheetName != null ? config.excelSheetName : "at index 0";
                throw new ValidationException(
                        "Excel sheet " + sheetName + " not able to open...",
                        List.of(new ValidationError("file", "Excel sheet " + sheetName + " not able to open..."))
                );
            }

            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                List<Object> colCollection = new ArrayList<>();
                for (int cn = config.startColNumber - 1; cn <= config.endColNumber - 1; cn++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(cn, org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    switch (cell.getCellType()) {
                        case NUMERIC -> colCollection.add(cell.getNumericCellValue());
                        case STRING -> colCollection.add(cell.getStringCellValue());
                        case BOOLEAN -> colCollection.add(cell.getBooleanCellValue());
                        default -> colCollection.add("");
                    }
                }
                rowsCollection.add(colCollection);
            }
        } catch (Exception e) {
            throw new ValidationException(
                    "Error processing Excel file",
                    List.of(new ValidationError("file", "Failed to read Excel file: " + e.getMessage()))
            );
        }

        log.info("Total rows read from Excel: {}, Rows to be inserted (after startRowNumber {}): {}",
                rowsCollection.size(), config.startRowNumber, Math.max(0, rowsCollection.size() - config.startRowNumber + 1));

        saveImportedDataAsync(config.startRowNumber, rowsCollection, config.tempTableName);

        // Verify data was inserted
        Integer insertedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + config.tempTableName, Integer.class);
        log.info("Rows inserted into temp table {}: {}", config.tempTableName, insertedCount);

        if (callStoredProcedure) {
            String result = callImportTdrDetail(groupPoid, userPoid, companyPoid, transactionPoid);
            if (result != null && result.startsWith("ERROR")) {
                throw new ValidationException(
                        "Failed to upload TDR details from Excel",
                        List.of(new ValidationError("file", result))
                );
            }
            return result != null ? result : "TDR details uploaded successfully from Excel";
        } else {
            return "Records Loaded...";
        }
    }

    private ExcelConfig getExcelConfig(String docId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("PROC_GLOB_EXCEL_IMPORT_SHEETS")
                .declareParameters(
                        new SqlParameter("P_COMPANY_POID", Types.NUMERIC),
                        new SqlParameter("P_DOC_ID", Types.VARCHAR),
                        new SqlOutParameter("OUTDATA", oracle.jdbc.internal.OracleTypes.CURSOR),
                        new SqlOutParameter("P_STATUS", Types.VARCHAR)
                );

        Map<String, Object> result = jdbcCall.execute(
                Map.of(
                        "P_COMPANY_POID", UserContext.getCompanyPoid(),
                        "P_DOC_ID", docId
                )
        );

        List<Map<String, Object>> configs = (List<Map<String, Object>>) result.get("OUTDATA");
        if (configs == null || configs.isEmpty()) {
            throw new ValidationException(
                    "Excel configuration not found",
                    List.of(new ValidationError("file", "No Excel configuration found for DOC_ID: " + docId))
            );
        }

        Map<String, Object> configRow = configs.get(0);
        ExcelConfig config = new ExcelConfig();
        config.startRowNumber = ((Number) configRow.get("START_ROW_NUMBER")).intValue();
        config.startColNumber = ((Number) configRow.get("START_COL_NUMBER")).intValue();
        config.endColNumber = ((Number) configRow.get("END_COL_NUMBER")).intValue();
        config.tempTableName = (String) configRow.get("TEMP_TABLE_NAME");
        config.excelSheetName = (String) configRow.get("EXCEL_SHEET_NAME");
        return config;
    }

    protected void saveImportedDataAsync(int startRowNumber, List<List<Object>> rowsCollection, String tempTableName) {
        List<String> batchQueries = new ArrayList<>();
        int rowNum = 0;

        for (List<Object> cols : rowsCollection) {
            rowNum++;
            if (startRowNumber <= rowNum) {
                StringBuilder insertQuery = new StringBuilder("INSERT INTO " + tempTableName + " VALUES (");
                for (Object col : cols) {
                    if (col == null) {
                        insertQuery.append("NULL,");
                    } else {
                        insertQuery.append("'").append(col.toString().replace("'", "''")).append("',");
                    }
                }
                insertQuery.setLength(insertQuery.length() - 1);
                insertQuery.append(")");

                jdbcTemplate.update(insertQuery.toString());
            }
        }

//        // Execute in batches of 50
//        int batchSize = 50;
//        for (int i = 0; i < batchQueries.size(); i += batchSize) {
//            int endIndex = Math.min(i + batchSize, batchQueries.size());
//            List<String> batch = batchQueries.subList(i, endIndex);
//            jdbcTemplate.batchUpdate(batch.toArray(new String[0]));
//        }
    }

    public String callImportTdrDetail(Long groupPoid, Long userPoid, Long companyPoid, Long transactionPoid) {
        try {
            log.info("[SP-3] PROC_PDA_IMPORT_TDR_DETAIL2 - transactionPoid: {}", transactionPoid);

            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("PROC_PDA_IMPORT_TDR_DETAIL2")
                    .withoutProcedureColumnMetaDataAccess()
                    .declareParameters(
                            new SqlParameter("P_LOGIN_GROUP_POID", Types.NUMERIC),
                            new SqlParameter("P_LOGIN_USER_POID", Types.NUMERIC),
                            new SqlParameter("P_LOGIN_COMPANY_POID", Types.NUMERIC),
                            new SqlParameter("P_TRANSACTION_POID", Types.NUMERIC),
                            new SqlOutParameter("P_STATUS", Types.VARCHAR)
                    );

            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("P_LOGIN_GROUP_POID", groupPoid);
            inputMap.put("P_LOGIN_USER_POID", userPoid);
            inputMap.put("P_LOGIN_COMPANY_POID", companyPoid);
            inputMap.put("P_TRANSACTION_POID", transactionPoid);

            Map<String, Object> result = jdbcCall.execute(inputMap);

            String status = (String) result.get("P_STATUS");

            log.info("[SP-3] PROC_PDA_IMPORT_TDR_DETAIL2 - Completed. Status: {}", status);
            return status != null ? status : "Success";

        } catch (Exception e) {
            log.error("[SP-3] PROC_PDA_IMPORT_TDR_DETAIL2 - Error: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

}
