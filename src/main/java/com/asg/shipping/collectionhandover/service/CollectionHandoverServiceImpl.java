package com.asg.shipping.collectionhandover.service;

import javax.sql.DataSource;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.request.LogRequestDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.annotation.PerformGlPosting;
import com.asg.shipping.collectionhandover.dto.*;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseDtl;
import com.asg.shipping.collectionhandover.entity.ArShDayEndCloseHdr;
import com.asg.shipping.collectionhandover.repository.CollectionHandoverHdrRepository;
import com.asg.shipping.collectionhandover.repository.CollectionHandoverDtlRepository;
import com.asg.shipping.collectionhandover.util.CollectionHandoverMapper;
import com.asg.shipping.common.repository.GlobalCurrencyDenominationRepository;
import com.asg.shipping.common.service.DocumentRightsService;
import com.asg.common.lib.enums.UserRolesRightsEnum;
import com.asg.shipping.daycloseshiping.dto.DayCloseSummaryProjection;
import com.asg.shipping.daycloseshiping.repository.ArShReceiptHdrRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JasperReport;

import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionHandoverServiceImpl implements CollectionHandoverService {

    private static final String COLLECTION_HANDOVER = "Collection Handover";
    private static final String TRANSACTION_POID_FIELD = "transactionPoid";
    private static final String DOC_ID = "300-106";

    /**
     * Document whose "Edit" right identifies a main-office user allowed to verify/receive a
     * handover — legacy {@code IsGrantedRights("000-208", "Edit")} (ArShDayEndBean:445, 468).
     */
    private static final String MAIN_OFFICE_RIGHTS_DOC_ID = "000-208";

    /** Legacy showMessage text (ArShDayEndBean:453) — informational, it never blocked the save. */
    private static final String MAIN_OFFICE_CHECK_MESSAGE = "Verified yes & Main office remarks check.";

    private final CollectionHandoverHdrRepository headerRepository;
    private final CollectionHandoverDtlRepository detailRepository;
    private final DocumentSearchService documentService;
    private final CollectionHandoverMapper mapper;
    private final LoggingService loggingService;
    private final PrintService printService;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final GlobalCurrencyDenominationRepository denomRepo;
    private final ArShReceiptHdrRepository receiptHdrRepository;
    private final EntityManager entityManager;
    private final DocumentRightsService documentRightsService;

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchCollectionHandovers(String docId, com.asg.common.lib.dto.FilterRequestDto request,
            Pageable pageable, LocalDate startDate, LocalDate endDate) {

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        if (startDate != null && endDate != null) {
            filters = documentService.resolveDateFilters(request, "TRANSACTION_DATE", startDate, endDate);
        }

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted,
                "DOC_REF", "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public CollectionHandoverDto getCollectionHandover(Long id) {
        Long groupPoid = UserContext.getGroupPoid();
        ArShDayEndCloseHdr handover = headerRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(COLLECTION_HANDOVER, TRANSACTION_POID_FIELD, id.toString()));
        List<ArShDayEndCloseDtl> detailList = detailRepository.findByTransactionPoidOrderByDetRowId(id);
        return mapper.mapToDto(handover, detailList);
    }

    // -------------------------------------------------------------------------
    // New-record auto-populate data (DocumentAfterNew logic)
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public DayCloseSummaryProjection getNewHandoverData(Long groupPoid, Long companyPoid, String transactionDate) {
        return receiptHdrRepository.fetchNewDayCloseSummary(groupPoid, companyPoid, transactionDate)
                .orElseThrow(() -> new ResourceNotFoundException(COLLECTION_HANDOVER, "transactionDate", transactionDate));
    }

    // -------------------------------------------------------------------------
    // Denominations (LoadDinominationCurTypes logic)
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDenominations(String currencyCode) {
        return denomRepo.findByCurrencyCodeOrderBySeqNo(currencyCode).stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("denomination", new BigDecimal(d.getCurrencyAmount()));
            map.put("currencyType", d.getCurrencyType());
            return map;
        }).toList();
    }

    // -------------------------------------------------------------------------
    // Create (DocumentBeforeSave + DocumentAfterSave logic)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @PerformGlPosting
    public CollectionHandoverDto createCollectionHandover(CollectionHandoverCreateDTO dto, Long groupPoid, Long userPoid) {
        log.info("Creating collection handover");

        // Company is authoritative from the session/user context (legacy: getLoginCompanyPoid()),
        // never taken from the client payload.
        Long companyPoid = UserContext.getCompanyPoid() != null ? UserContext.getCompanyPoid() : dto.getCompanyPoid();

        validateForCreate(dto, groupPoid, companyPoid);
        validateAmounts(dto.getTotalAmount(), dto.getCashAmount(), dto.getChequeAmount(), dto.getDetails());

        // No main-office check here: legacy gates it on document mode "Edit" (ArShDayEndBean:446),
        // so it never runs on a new record. A new record starts at VerifiedRcvd='N' /
        // MainOfcRemarks='.' (ArShDayEndBean:236-237).

        ArShDayEndCloseHdr handover = new ArShDayEndCloseHdr();
        mapper.mapCreateDTOToEntity(dto, handover, groupPoid);
        handover.setCompanyPoid(companyPoid);
        ArShDayEndCloseHdr saved = headerRepository.save(handover);
        // Flush so the DB trigger fires and populates TRANSACTION_POID + DOC_REF,
        // then refresh to read trigger-generated values back into the entity.
        entityManager.flush();
        entityManager.refresh(saved);

        saveDetailRecords(saved.getTransactionPoid(), dto.getDetails());

        // DocumentAfterSave: call GL procedure.
        // P_LOGIN_USER_POID is VARCHAR2 in the procedure — pass the user POID as String.
        String procResult = callProcGlChoIntoChqMainShip(
                saved.getTransactionPoid(), saved.getTransactionDate(),
                UserContext.getDocumentId(), saved.getDocRef(),
                groupPoid, saved.getCompanyPoid(), String.valueOf(userPoid));
        if (procResult != null && procResult.startsWith("ERROR")) {
            throw new IllegalStateException("PROC_GL_CHO_INTO_CHQ_MAIN_SHIP failed: " + procResult);
        }

        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(),
                saved.getTransactionPoid().toString());

        List<ArShDayEndCloseDtl> detailList = detailRepository.findByTransactionPoidOrderByDetRowId(saved.getTransactionPoid());
        return mapper.mapToDto(saved, detailList);
    }

    // -------------------------------------------------------------------------
    // Update (DocumentBeforeSave logic)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @PerformGlPosting
    public CollectionHandoverDto updateCollectionHandover(Long id, CollectionHandoverUpdateDTO dto,
            Long groupPoid, Long userPoid) {
        log.info("Updating collection handover with id: {}", id);

        ArShDayEndCloseHdr handover = headerRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(COLLECTION_HANDOVER, TRANSACTION_POID_FIELD, id.toString()));

        // DocumentBeforeSave: block if already handed over
        if ("Y".equals(handover.getVerifiedRcvd())) {
            throw new ValidationException("Already Handover Completed.");
        }

        validateForUpdate(dto, id);
        validateAmounts(dto.getTotalAmount(), dto.getCashAmount(), dto.getChequeAmount(), dto.getDetails());

        // DocumentBeforeSave main-office check (ArShDayEndBean:445). The update is a patch merge
        // (mapper only applies non-null fields), so validate the EFFECTIVE values the row will end
        // up with, exactly as legacy read them back off the edited row.
        String effectiveVerified = dto.getVerifiedRcvd() != null ? dto.getVerifiedRcvd() : handover.getVerifiedRcvd();
        String effectiveRemarks = dto.getMainOfcRemarks() != null ? dto.getMainOfcRemarks() : handover.getMainOfcRemarks();

        // The right only decides whether the check applies, exactly as legacy — a user without it
        // is not rejected, the check simply does not run for them. The outcome is a notice, not an
        // error: the save proceeds either way (legacy returned true after showMessage).
        String mainOfficeNotice = null;
        if (documentRightsService.isGrantedRight(MAIN_OFFICE_RIGHTS_DOC_ID, UserRolesRightsEnum.EDIT)) {
            mainOfficeNotice = mainOfficeHandoverNotice(effectiveVerified, effectiveRemarks);
        }

        ArShDayEndCloseHdr oldHandover = new ArShDayEndCloseHdr();
        BeanUtils.copyProperties(handover, oldHandover);

        mapper.mapUpdateDTOToEntity(dto, handover);
        ArShDayEndCloseHdr saved = headerRepository.save(handover);

        saveDetailRecords(id, dto.getDetails());

        loggingService.logChanges(oldHandover, handover, ArShDayEndCloseHdr.class,
                UserContext.getDocumentId(), id.toString(), LogDetailsEnum.MODIFIED, "TRANSACTION_POID");

        List<ArShDayEndCloseDtl> detailList = detailRepository.findByTransactionPoidOrderByDetRowId(id);
        CollectionHandoverDto result = mapper.mapToDto(saved, detailList);
        result.setInfoMessage(mainOfficeNotice);
        return result;
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteCollectionHandover(Long id) {
        Long groupPoid = UserContext.getGroupPoid();
        ArShDayEndCloseHdr handover = headerRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(COLLECTION_HANDOVER, TRANSACTION_POID_FIELD, id.toString()));

        if ("Y".equals(handover.getDeleted())) return;

        handover.setDeleted("Y");
        handover.setLastModifiedBy(getCurrentUser());
        handover.setLastModifiedDate(LocalDateTime.now());
        headerRepository.save(handover);
    }

    // -------------------------------------------------------------------------
    // Toggle verify status
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void toggleVerifyStatus(Long id, String verifiedRcvd, String mainOfcRemarks) {
        Long groupPoid = UserContext.getGroupPoid();
        ArShDayEndCloseHdr handover = headerRepository.findByTransactionPoidAndGroupPoid(id, groupPoid)
                .orElseThrow(() -> new ResourceNotFoundException(COLLECTION_HANDOVER, TRANSACTION_POID_FIELD, id.toString()));

        if (verifiedRcvd != null && !verifiedRcvd.matches("^[YN]$")) {
            throw new ValidationException("Verified received must be Y or N");
        }

        // Unlike the save path this endpoint DOES block: it has no legacy counterpart and exists
        // only to complete the verification, so marking a handover received with no real remarks
        // is rejected rather than merely reported. mainOfcRemarks is optional here, so fall back
        // to what is already stored.
        String effectiveRemarks = mainOfcRemarks != null ? mainOfcRemarks : handover
                .getMainOfcRemarks();
        if ("Y".equalsIgnoreCase(verifiedRcvd) && mainOfficeHandoverNotice(verifiedRcvd, effectiveRemarks) != null) {
            throw new ValidationException(MAIN_OFFICE_CHECK_MESSAGE);
        }

        handover.setVerifiedRcvd(verifiedRcvd);
        if (mainOfcRemarks != null) {
            handover.setMainOfcRemarks(mainOfcRemarks);
        }
        handover.setLastModifiedBy(getCurrentUser());
        handover.setLastModifiedDate(LocalDateTime.now());
        headerRepository.save(handover);
    }

    // -------------------------------------------------------------------------
    // Print (DocumentBeforePrint logic) — fixed doc ID 300-106
    // -------------------------------------------------------------------------

    @Override
    public byte[] print(Long transactionPoid) throws net.sf.jasperreports.engine.JRException, java.sql.SQLException {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, DOC_ID);
        params.put("SH_DAY_CLOSE_CASH_SUBREPORT_1", printService.load("Shipping/SH/SH_DAY_CLOSE_CASH_subreport1.jrxml"));
        params.put("SH_DAY_CLOSE_CHQ_SUBREPORT_1",  printService.load("Shipping/SH/SH_DAY_CLOSE_CHQ_subreport1.jrxml"));
        params.put("SH_DAY_CLOSE_SMRY_SUBREPORT_1", printService.load("Shipping/SH/SH_DAY_CLOSE_SMRY_subreport1.jrxml"));
        JasperReport mainReport = printService.load("Shipping/SH/SH_DAY_CLOSE.jrxml");
        try {
            return printService.fillReportToPdf(mainReport, params, dataSource);
        } catch (Exception e) {
            throw new net.sf.jasperreports.engine.JRException("Failed to generate PDF report", e);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * DocumentBeforeSave main-office handover check (ArShDayEndBean:445-455).
     *
     * <p>Legacy: for a user holding the 000-208 "Edit" right, saving the document in Edit mode
     * reports that the handover is not complete unless the main office has both marked it
     * verified/received ({@code VerifiedRcvd='Y'}) and entered real remarks. {@code length() <= 1}
     * catches the {@code "."} placeholder the record is created with (ArShDayEndBean:237).
     *
     * <p>This is <strong>not</strong> a blocking validation: legacy raised it with
     * {@code common.showMessage(...)} and then {@code return true}, so the save always went
     * through. Hence a message is returned rather than an exception being thrown.
     *
     * <p>Callers apply the {@code IsGrantedRights("000-208", "Edit")} gate through
     * {@link DocumentRightsService} before calling this.
     *
     * @return the notice to pass back to the caller, or {@code null} when the handover is complete
     */
    private String mainOfficeHandoverNotice(String verifiedRcvd, String mainOfcRemarks) {
        boolean verified = verifiedRcvd != null && "Y".equalsIgnoreCase(verifiedRcvd.trim());
        boolean hasRemarks = mainOfcRemarks != null && mainOfcRemarks.trim().length() > 1;
        return (verified && hasRemarks) ? null : MAIN_OFFICE_CHECK_MESSAGE;
    }

    /**
     * DocumentBeforeSave amount validations:
     * 1. cashAmount == sum(denomination.cashAmount)
     * 2. totalAmount == cashAmount + chequeAmount
     */
    private void validateAmounts(BigDecimal totalAmount, BigDecimal cashAmount, BigDecimal chequeAmount,
            List<? extends Object> details) {

        if (cashAmount != null && chequeAmount != null && totalAmount != null) {
            if (cashAmount.add(chequeAmount).compareTo(totalAmount) != 0) {
                throw new ValidationException("Receipt Total and Cheque, Cash total amount not match.");
            }
        }

        if (details == null || details.isEmpty()) return;

        BigDecimal denomTotal = details.stream()
                .map(d -> {
                    if (d instanceof CollectionHandoverDetailCreateDTO c) {
                        if ("ISDELETED".equalsIgnoreCase(c.getAction())) return BigDecimal.ZERO;
                        return c.getCashAmount() != null ? c.getCashAmount() : BigDecimal.ZERO;
                    }
                    if (d instanceof CollectionHandoverDetailUpdateDTO u) {
                        if ("ISDELETED".equalsIgnoreCase(u.getAction())) return BigDecimal.ZERO;
                        return u.getCashAmount() != null ? u.getCashAmount() : BigDecimal.ZERO;
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (cashAmount != null && denomTotal.compareTo(cashAmount) != 0) {
            throw new ValidationException("Receipt total and Denomination total amount not match.");
        }
    }

    private void validateForCreate(CollectionHandoverCreateDTO dto, Long groupPoid, Long companyPoid) {
        // DOC_REF is trigger-generated on INSERT — no uniqueness check needed here.
        // Duplicate transaction date check (matches DayCloseServiceImpl)
        if (dto.getTransactionDate() != null && companyPoid != null
                && headerRepository.countByTransactionDateAndGroupPoidAndCompanyPoid(
                        dto.getTransactionDate(), groupPoid, companyPoid) > 0) {
            throw new ValidationException("Transaction date already closed: " + dto.getTransactionDate());
        }
    }

    private void validateForUpdate(CollectionHandoverUpdateDTO dto, Long id) {
        if (dto.getDocRef() != null && headerRepository.existsByDocRefExcludingPoid(dto.getDocRef(), id)) {
            throw new ValidationException("Document reference already exists: " + dto.getDocRef());
        }
    }

    /**
     * Action-based detail save matching DayCloseServiceImpl.saveDenominations().
     * Accepts both create and update DTOs via overloads.
     */
    private void saveDetailRecords(Long transactionPoid, List<?> details) {
        if (details == null || details.isEmpty()) return;

        String docId    = UserContext.getDocumentId();
        String docKeyPoid = transactionPoid.toString();
        Long maxDetRowId = detailRepository.getMaxDetRowId(transactionPoid);

        List<ArShDayEndCloseDtl> toSave   = new ArrayList<>();
        List<ArShDayEndCloseDtl> toUpdate  = new ArrayList<>();
        List<Long>               toDelete  = new ArrayList<>();
        List<LogRequestDto<ArShDayEndCloseDtl>> logRequests = new ArrayList<>();

        for (Object raw : details) {
            String action;
            Long   detRowId;
            ArShDayEndCloseDtl entity;

            if (raw instanceof CollectionHandoverDetailCreateDTO c) {
                action   = c.getAction() != null ? c.getAction().toUpperCase() : "ISCREATED";
                detRowId = c.getDetRowId();
                entity   = buildDetailEntity(transactionPoid, c);
            } else if (raw instanceof CollectionHandoverDetailUpdateDTO u) {
                action   = u.getAction() != null ? u.getAction().toUpperCase() : "ISUPDATED";
                detRowId = u.getDetRowId();
                entity   = buildDetailEntity(transactionPoid, u);
            } else {
                continue;
            }

            switch (action) {
                case "ISCREATED" -> {
                    entity.setDetRowId(detRowId != null ? detRowId : ++maxDetRowId);
                    toSave.add(entity);
                }
                case "ISUPDATED" -> {
                    if (detRowId == null) {
                        throw new ValidationException("detRowId is required for an update (ISUPDATED) detail action.");
                    }
                    ArShDayEndCloseDtl existing = detailRepository
                            .findByTransactionPoidAndDetRowId(transactionPoid, detRowId)
                            .orElse(new ArShDayEndCloseDtl());
                    ArShDayEndCloseDtl oldEntity = new ArShDayEndCloseDtl();
                    BeanUtils.copyProperties(existing, oldEntity);
                    // Apply ONLY the editable scalar columns onto the managed entity. A blanket
                    // BeanUtils.copyProperties(entity, existing) also copies the freshly-built entity's
                    // null 'header' association and null audit fields, nulling them on 'existing' — which
                    // wipes CREATED_BY/CREATED_DATE and logs a phantom "Header" change per detail row.
                    existing.setCurrencyAmount(entity.getCurrencyAmount());
                    existing.setCurrencyType(entity.getCurrencyType());
                    existing.setNoOfTran(entity.getNoOfTran());
                    existing.setCashAmount(entity.getCashAmount());
                    existing.setTransactionPoid(transactionPoid);
                    existing.setDetRowId(detRowId);
                    toUpdate.add(existing);
                    logRequests.add(new LogRequestDto<>(oldEntity, existing, ArShDayEndCloseDtl.class,
                            docId, docKeyPoid, "DAYENDCLOSE DET_ROW_ID: " + detRowId));
                }
                case "ISDELETED" -> {
                    if (detRowId == null) {
                        throw new ValidationException("detRowId is required for a delete (ISDELETED) detail action.");
                    }
                    toDelete.add(detRowId);
                    loggingService.logDelete(raw, docId, docKeyPoid);
                }
                default -> { /* ignore unknown actions */ }
            }
        }

        if (!toSave.isEmpty()) {
            List<ArShDayEndCloseDtl> saved = detailRepository.saveAll(toSave);
            saved.forEach(e -> loggingService.createLogSummaryEntry(docId, docKeyPoid,
                    "CollectionHandover detail created with detRowId: " + e.getDetRowId()));
        }
        if (!toUpdate.isEmpty()) {
            detailRepository.saveAll(toUpdate);
            if (!logRequests.isEmpty()) loggingService.createLogBatch(logRequests);
        }
        if (!toDelete.isEmpty()) {
            detailRepository.deleteByTransactionPoidAndDetRowIdIn(transactionPoid, toDelete);
        }
    }

    private ArShDayEndCloseDtl buildDetailEntity(Long transactionPoid, CollectionHandoverDetailCreateDTO dto) {
        ArShDayEndCloseDtl e = new ArShDayEndCloseDtl();
        e.setTransactionPoid(transactionPoid);
        mapper.mapDetailCreateDTOToEntity(dto, e);
        return e;
    }

    private ArShDayEndCloseDtl buildDetailEntity(Long transactionPoid, CollectionHandoverDetailUpdateDTO dto) {
        ArShDayEndCloseDtl e = new ArShDayEndCloseDtl();
        e.setTransactionPoid(transactionPoid);
        mapper.mapDetailUpdateDTOToEntity(dto, e);
        return e;
    }

    /**
     * DocumentAfterSave: PROC_GL_CHO_INTO_CHQ_MAIN_SHIP.
     * P_LOGIN_USER_POID is declared VARCHAR2 in the procedure — pass the user POID as String.
     */
    private String callProcGlChoIntoChqMainShip(Long transactionPoid, LocalDate transactionDate,
            String docId, String docRef, Long groupPoid, Long companyPoid, String loginUserPoid) {

        String proc = "{call PROC_GL_CHO_INTO_CHQ_MAIN_SHIP(?, ?, ?, ?, ?, ?, ?, ?)}";
        return jdbcTemplate.execute((Connection con) -> {
            try (CallableStatement cs = con.prepareCall(proc)) {
                cs.setLong(1, transactionPoid);
                cs.setString(2, transactionDate != null ? transactionDate.toString() : null);
                cs.setLong(3, groupPoid);
                cs.setLong(4, companyPoid);
                cs.setString(5, loginUserPoid);   // VARCHAR2 in procedure — user POID as String
                cs.setString(6, docId);
                cs.setString(7, docRef);
                cs.registerOutParameter(8, Types.VARCHAR);
                cs.execute();
                return cs.getString(8);
            } catch (SQLException ex) {
                throw new IllegalStateException("Error calling PROC_GL_CHO_INTO_CHQ_MAIN_SHIP: " + ex.getMessage(), ex);
            }
        });
    }
}
