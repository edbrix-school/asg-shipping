package com.asg.shipping.daycloseshiping.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.common.repository.GlobalCurrencyDenominationRepository;
import com.asg.shipping.daycloseshiping.dto.DayCloseDenominationDto;
import com.asg.shipping.daycloseshiping.dto.DayCloseDto;
import com.asg.shipping.daycloseshiping.dto.DayCloseHdrDto;
import com.asg.shipping.daycloseshiping.dto.DayCloseSummaryProjection;
import com.asg.shipping.daycloseshiping.entity.ArShDayEndCloseDtl;
import com.asg.shipping.daycloseshiping.entity.ArShDayEndCloseHdr;
import com.asg.shipping.daycloseshiping.repository.ArShDayEndCloseDtlRepository;
import com.asg.shipping.daycloseshiping.repository.ArShDayEndCloseHdrRepository;
import com.asg.shipping.daycloseshiping.repository.ArShReceiptHdrRepository;
import com.asg.shipping.daycloseshiping.util.DayCloseMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DayCloseServiceImpl implements DayCloseService {

    private static final String RECEIPT_CHEQUE_CASH_MISMATCH =
            "Receipt Total and Cheque, Cash total amount not match, Call for support.....";
    private static final String RECEIPT_DENOMINATION_MISMATCH =
            "Receipt total and Denomination total amount not match.....";

    private final ArShDayEndCloseHdrRepository hdrRepo;
    private final ArShDayEndCloseDtlRepository dtlRepo;
    private final GlobalCurrencyDenominationRepository denomRepo;
    private final ArShReceiptHdrRepository receiptHdrRepository;
    private final JdbcTemplate jdbcTemplate;
    private final DocumentSearchService documentService;
    private final DocumentDeleteService documentDeleteService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;

    @PersistenceContext
    private final EntityManager entityManager;

    private static final String TRANSACTION_POID="TRANSACTION_POID";
    private static final String TRANSACTIONPOID="transactionPoid";

    @Override
    public DayCloseDto getDayClose(Long transactionPoid, Long groupPoid, Long companyPoid) {

        ArShDayEndCloseHdr hdr = hdrRepo.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Day Close", TRANSACTIONPOID, transactionPoid.toString()));

        List<ArShDayEndCloseDtl> details = dtlRepo.findByTransactionPoid(transactionPoid);

        DayCloseDto dto = DayCloseMapper.mapToDto(hdr);
        dto.setDenominations(DayCloseMapper.mapDtlListToDto(details));

        return dto;
    }

    @Override
    public DayCloseDto createDayClose(DayCloseDto dto, Long groupPoid, Long companyPoid, Long userPoid) {

        DayCloseHdrDto header = dto.getHeader();
        validateDuplicateTransactionDate(header.getTransactionDate(), groupPoid, companyPoid, null);

        validateAmounts(dto);

        ArShDayEndCloseHdr hdr = new ArShDayEndCloseHdr();
        DayCloseMapper.mapCreateDTOToEntity(header, hdr, groupPoid, companyPoid);

        hdr = hdrRepo.saveAndFlush(hdr);
        entityManager.refresh(hdr);

        saveDenominations(hdr.getTransactionPoid(), dto.getDenominations());

        String status = callProcGlChoIntoChqMainShip(hdr.getTransactionPoid(), hdr.getTransactionDate(), UserContext.getDocumentId(),
                hdr.getDocRef(), groupPoid, companyPoid, userPoid);
        if (status != null && !status.startsWith("SUCCESS")) throw new IllegalStateException(status);
        loggingService.createLogSummaryEntry(UserContext.getDocumentId(),hdr.getTransactionPoid().toString(), String.format("%s %s", LogDetailsEnum.CREATED.getDescription(), hdr.getDocRef()));
        return getDayClose(hdr.getTransactionPoid(), groupPoid, companyPoid);
    }

    @Override
    public DayCloseSummaryProjection getNewDayCloseData(Long groupPoid, Long companyPoid, String transactionDate) {

        return receiptHdrRepository.fetchNewDayCloseSummary(groupPoid, companyPoid, transactionDate)
                .orElseThrow(() -> new ResourceNotFoundException("Day Close", "transactionDate", transactionDate));
    }

    @Override
    public List<Map<String, Object>> getDenominations(String currencyCode) {

        return denomRepo.findByCurrencyCodeOrderBySeqNo(currencyCode).stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("denomination", new BigDecimal(d.getCurrencyAmount()));
            map.put("currencyType", d.getCurrencyType());
            return map;
        }).toList();
    }

    @Override
    public DayCloseDto updateDayClose(DayCloseDto request, Long transactionPoid, Long groupPoid, Long companyPoid,
                                      Long userPoid) {

        validateDuplicateTransactionDate(request.getHeader().getTransactionDate(), groupPoid, companyPoid,
                transactionPoid);
        validateAmounts(request);

        ArShDayEndCloseHdr existingData = hdrRepo.findById(transactionPoid).orElseThrow(() -> new ResourceNotFoundException("Day close Shipping", TRANSACTIONPOID, transactionPoid));

        if("Y".equals(existingData.getVerifiedRcvd())){
            throw new ValidationException("Already Handover Completed.");
        }

        ArShDayEndCloseHdr oldData = new ArShDayEndCloseHdr();
        BeanUtils.copyProperties(existingData, oldData);

        DayCloseMapper.mapCreateDTOToEntity(request.getHeader(), existingData, groupPoid, companyPoid);
        hdrRepo.save(existingData);

        saveDenominations(transactionPoid, request.getDenominations());
        String docId = UserContext.getDocumentId();
        loggingService.logChanges(oldData, existingData, ArShDayEndCloseHdr.class, docId, transactionPoid.toString(), LogDetailsEnum.MODIFIED, TRANSACTION_POID);

        return getDayClose(transactionPoid, groupPoid, companyPoid);
    }

    @Override
    public void deleteDayClose(Long id, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting DayClose Shipping with id: {}", id);


        ArShDayEndCloseHdr existingData=hdrRepo.findByTransactionPoidDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dayclose Shipping", TRANSACTIONPOID, id));

        // Use DocumentDeleteService for deletion (handles logging internally)
        documentDeleteService.deleteDocument(
                id,
                "AR_SH_DAY_END_CLOSE_HDR",
                TRANSACTION_POID,
                deleteReasonDto,
                existingData.getTransactionDate()
        );

        log.info("Successfully deleted dayclose shipping with id: {}", id);
    }

    @Override
    public Map<String, Object> searchDayClose(String docId, FilterRequestDto request, Pageable pageable, LocalDate startDate, LocalDate endDate) {

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        if (startDate != null && endDate != null) {
            // Add date range filter for TRANSACTION_DATE field
            filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);
        }

        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "LOCATION_CODE",
                TRANSACTION_POID
        );

        Page<Map<String, Object>> page = new PageImpl<>(
                raw.records(),
                pageable,
                raw.totalRecords()
        );
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    private void saveDenominations(Long transactionPoid, List<DayCloseDenominationDto> details) {

        if (details == null || details.isEmpty()) {
            return;
        }

        String docId = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();

        Long maxDetRowId = dtlRepo.getMaxDetRowId(transactionPoid);

        List<ArShDayEndCloseDtl> toSave = new ArrayList<>();
        List<ArShDayEndCloseDtl> toUpdate = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();
        List<LogRequestDto<ArShDayEndCloseDtl>> logRequests = new ArrayList<>();

        for (DayCloseDenominationDto dto : details) {

            String action = dto.getAction().toUpperCase();

            switch (action) {

                case "ISCREATED":
                    ArShDayEndCloseDtl entity = DayCloseMapper.mapDtlFromDto(dto, transactionPoid, null);
                    entity.setDetRowId(dto.getDetRowId() != null ? dto.getDetRowId() : ++maxDetRowId);
                    toSave.add(entity);
                    break;

                case "ISUPDATED":
                    ArShDayEndCloseDtl existingData = dtlRepo
                            .findByTransactionPoidAndDetRowId(transactionPoid, dto.getDetRowId())
                            .orElseThrow(() -> new com.asg.shipping.exceptions.ValidationException(
                                    "Container detail not found for detRowId: " + dto.getDetRowId()));
                    ArShDayEndCloseDtl oldEntity = new ArShDayEndCloseDtl();
                    BeanUtils.copyProperties(existingData, oldEntity);

                    mapDayCloseDtlFromDto(dto, existingData, transactionPoid);
                    toUpdate.add(existingData);
                    logRequests.add(new LogRequestDto<>(oldEntity, existingData, ArShDayEndCloseDtl.class, docId,
                            docKeyPoid, "DAYENDCLOSE DET_ROW_ID: " + dto.getDetRowId()));
                    break;

                case "ISDELETED":
                    toDelete.add(dto.getDetRowId());
                    loggingService.logDelete(dto, docId, docKeyPoid);
                    break;

                default:
                    break;
            }
        }

        if (!toSave.isEmpty()) {
            List<ArShDayEndCloseDtl> saved = dtlRepo.saveAll(toSave);
            saved.forEach(e -> loggingService.createLogSummaryEntry(docId, docKeyPoid,
                    "DayEndClose detail created with detRowId: " + e.getDetRowId()));
        }
        if (!toUpdate.isEmpty()) {
            dtlRepo.saveAll(toUpdate);
            if (!logRequests.isEmpty()) {
                loggingService.createLogBatch(logRequests);
            }
        }

        if (!toDelete.isEmpty()) {
            dtlRepo.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
    }

    private String callProcGlChoIntoChqMainShip(Long transactionPoid, LocalDate transactionDate, String docId,
                                                String docRef, Long groupPoid, Long companyPoid, Long userPoid) {
        String proc = "{call PROC_GL_CHO_INTO_CHQ_MAIN_SHIP(?, ?, ?, ?, ?, ?, ?, ?)}";

        return jdbcTemplate.execute((Connection con) -> {
            try (CallableStatement cs = con.prepareCall(proc)) {

                cs.setLong(1, transactionPoid);
                cs.setString(2, transactionDate.toString());
                cs.setLong(3, groupPoid);
                cs.setLong(4, companyPoid);
                cs.setLong(5, userPoid);
                cs.setString(6, docId);
                cs.setString(7, docRef);
                cs.registerOutParameter(8, Types.VARCHAR);

                cs.execute();

                return cs.getString(8);

            } catch (SQLException ex) {
                throw new IllegalStateException(
                        "Error calling PROC_GL_CHO_INTO_CHQ_MAIN_SHIP: " + ex.getMessage(), ex);
            }
        });

    }

    private void validateAmounts(DayCloseDto request) {

        DayCloseHdrDto header = request.getHeader();
        List<DayCloseDenominationDto> details = request.getDenominations();

        if (header == null) {
            throw new ValidationException("Day close header is required.");
        }

        BigDecimal cash = header.getCashAmount();
        BigDecimal cheque = header.getChequeAmount();
        BigDecimal total = header.getTotalAmount();

        if (cash != null && cheque != null && total != null && cash.add(cheque).compareTo(total) != 0) {
            throw new ValidationException(RECEIPT_CHEQUE_CASH_MISMATCH);
        }

        if (details == null || details.isEmpty()) {
            return;
        }

        BigDecimal denomTotal = details.stream().map(this::calculateDenominationAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);

        if (cash != null && denomTotal.compareTo(cash) != 0) {
            throw new ValidationException(RECEIPT_DENOMINATION_MISMATCH);
        }
    }

    private BigDecimal calculateDenominationAmount(DayCloseDenominationDto dto) {

        if (dto == null || (dto.getAction() != null && dto.getAction().equalsIgnoreCase("ISDELETED"))) {
            return BigDecimal.ZERO;
        }

        if (dto.getCashAmount() != null) {
            return dto.getCashAmount();
        }

        if (dto.getDenomination() != null && dto.getNoOfTran() != null) {
            return dto.getDenomination().multiply(BigDecimal.valueOf(dto.getNoOfTran()));
        }

        return BigDecimal.ZERO;
    }

    private void mapDayCloseDtlFromDto(DayCloseDenominationDto dto, ArShDayEndCloseDtl entity, Long transactionPoid) {
        entity.setTransactionPoid(transactionPoid);
        entity.setCurrencyAmount(dto.getDenomination());
        entity.setDetRowId(dto.getDetRowId());
        entity.setCurrencyType(dto.getCurrencyType());
        entity.setNoOfTran(dto.getNoOfTran());
        entity.setCashAmount(dto.getCashAmount());
    }

    private void validateDuplicateTransactionDate(LocalDate transactionDate, Long groupPoid, Long companyPoid,
                                                  Long excludeTransactionPoid) {
        if (transactionDate == null) {
            return;
        }

        Long count = (excludeTransactionPoid == null)
                ? hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoid(transactionDate, groupPoid, companyPoid)
                : hdrRepo.countByTransactionDateAndGroupPoidAndCompanyPoidExcludingTransactionPoid(
                        transactionDate, groupPoid, companyPoid, excludeTransactionPoid);

        if (count != null && count > 0) {
            throw new ValidationException("Transaction date already closed: " + transactionDate);
        }
    }

    @Override
    public byte[] printDayClose(Long transactionPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "300-106");
        params.put("SH_DAY_CLOSE_CASH_SUBREPORT_1", printService.load("Shipping/SH/SH_DAY_CLOSE_CASH_subreport1.jrxml"));
        params.put("SH_DAY_CLOSE_CHQ_SUBREPORT_1", printService.load("Shipping/SH/SH_DAY_CLOSE_CHQ_subreport1.jrxml"));
        params.put("SH_DAY_CLOSE_SMRY_SUBREPORT_1", printService.load("Shipping/SH/SH_DAY_CLOSE_SMRY_subreport1.jrxml"));
        return printService.fillReportToPdf(printService.load("Shipping/SH/SH_DAY_CLOSE.jrxml"), params, dataSource);
    }

    @Override
    public byte[] printDetails(Long transactionPoid) throws Exception {
        Map<String, Object> params = buildDateParams(transactionPoid);
        params.put("DAY_CLOSED_REPORT_SUBREPORT_1", printService.load("Shipping/SH/Day_Closed_Report_subreport1.jrxml"));
        params.put("DAY_CLOSED_REPORT_SUBREPORT_2", printService.load("Shipping/SH/Day_Closed_Report_subreport2.jrxml"));
        return printService.fillReportToPdf(printService.load("Shipping/SH/Day_Closed_Report.jrxml"), params, dataSource);
    }

    @Override
    public byte[] printSplitReceipt(Long transactionPoid) throws Exception {
        Map<String, Object> params = buildDateParams(transactionPoid);
        return printService.fillReportToPdf(printService.load("Shipping/SH/Day_Closed_Report_Split_receipt.jrxml"), params, dataSource);
    }

    @Override
    public byte[] printSummary(Long transactionPoid) throws Exception {
        Map<String, Object> params = buildDateParams(transactionPoid);
        return printService.fillReportToPdf(printService.load("Shipping/SH/Day_Closed_Report_Summary.jrxml"), params, dataSource);
    }

    private Map<String, Object> buildDateParams(Long transactionPoid) throws Exception {
        ArShDayEndCloseHdr hdr = hdrRepo.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Day Close", TRANSACTIONPOID, transactionPoid.toString()));
        String txDate = hdr.getTransactionDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "300-106");
        params.put("P_START_DATE", txDate);
        params.put("P_END_DATE", txDate);
        return params;
    }
}
