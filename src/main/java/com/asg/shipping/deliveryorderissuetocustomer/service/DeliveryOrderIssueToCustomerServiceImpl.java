package com.asg.shipping.deliveryorderissuetocustomer.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.deliveryorderissuetocustomer.dto.DeliveryOrderIssueToCustomerDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.IssueDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.UpdateDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.ValidateDocumentDto;
import com.asg.shipping.deliveryorderissuetocustomer.entity.DoShPrintingDtl;
import com.asg.shipping.deliveryorderissuetocustomer.entity.ShipBlManifestHDR;
import com.asg.shipping.deliveryorderissuetocustomer.enums.ButtonType;
import com.asg.shipping.deliveryorderissuetocustomer.repository.DeliveryOrderIssueToCustomerRepository;
import com.asg.shipping.deliveryorderissuetocustomer.repository.DoShPrintingDtlRepository;
import com.asg.shipping.deliveryorderissuetocustomer.repository.ShipBlManifestHDRRepository;
import com.asg.shipping.receipts.repository.ReceiptHdrRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static com.asg.common.lib.security.util.UserContext.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DeliveryOrderIssueToCustomerServiceImpl implements DeliveryOrderIssueToCustomerService {

    private final DeliveryOrderIssueToCustomerRepository viewRepository;
    private final ShipBlManifestHDRRepository blManifestRepository;
    private final DoShPrintingDtlRepository doShPrintingDtlRepository;
    private final LovDataService lovService;
    private final JdbcTemplate jdbcTemplate;
    private final PrintService printService;
    private final DataSource dataSource;
    private final LoggingService loggingService;
    private final DocumentSearchService documentSearchService;
    private final ReceiptHdrRepository receiptHdrRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String ARSHRCPTPRINTUPDATE = "ARSHRCPTPRINTUPDATE";
    private static final String TRANSACTIONPOID = "transactionPoid";
    private static final String DELIVERYORDER = "Delivery Order";

    @Override
    @Transactional(readOnly = true)
    public DeliveryOrderIssueToCustomerDto getDeliveryOrderIssueToCustomer(Long transactionPoid) {
        log.info("Getting delivery order with transactionPoid: {}, company poid: {}", transactionPoid, getCompanyPoid());

        DeliveryOrderIssueToCustomerDto dto = viewRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(DELIVERYORDER, TRANSACTIONPOID, transactionPoid.toString()));

        viewRepository.findRemarksByTransactionPoid(transactionPoid).ifPresent(dto::setRemarks);
        enrichWithLovData(dto);
        dto.setIsEnableAutoSend(viewRepository.getGlobalParameterValue("START_DO_CNT_DIRECT_CUST", "START_DO_CNT_CUST", "1", "N"));
        receiptHdrRepository.findByBlPoid(transactionPoid).ifPresent(receipt -> {
            dto.setReceiptsDocRef(receipt.getDocRef());
            dto.setReceiptsPoid(receipt.getTransactionPoid());
        });
        return dto;
    }

    @Override
    @Transactional
    public void issueDeliveryOrder(Long transactionPoid, IssueDeliveryOrderRequestDto request) {
        log.info("Issuing delivery order for BL transaction: {}", transactionPoid);

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        String username = getUserName();

        // Fetch the delivery order DTO to get blReleaseTypeOffice and principalDoRequired for validation
        DeliveryOrderIssueToCustomerDto dto = viewRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException(DELIVERYORDER, TRANSACTIONPOID, transactionPoid.toString()));

        validateAllFields(transactionPoid, dto, request);

        ShipBlManifestHDR blManifest = blManifestRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("BL Manifest", TRANSACTIONPOID, transactionPoid.toString()));

        if ("Y".equals(blManifest.getDeleted())) {
            throw new ResourceNotFoundException("BL Manifest", TRANSACTIONPOID, transactionPoid.toString());
        }

        callProcShipDoCntPrintAfter(groupPoid, companyPoid, transactionPoid, null,
                ARSHRCPTPRINTUPDATE, username, request.getDoReleasedIdPerson(),
                request.getDoReleasedToPerson(), request.getDoReleasedAddressPerson(),
                request.getOriginalBlReleaseCr(), request.getDoPriority(), request.getDoCntToConsignee(),
                request.getDoCntToNotify(), request.getDoCntToOthers(), request.getDoCntToOthersMails(),
                request.getEmailsDo(), request.getDeliverySentTo(), request.getPrincipalDoNumber(), null);
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), transactionPoid.toString());
    }

    @Override
    @Transactional
    public ValidateDocumentDto updateDeliveryOrder(Long transactionPoid, UpdateDeliveryOrderRequestDto request) {
        return updateDeliveryOrderViaProc(transactionPoid, request);
    }

    // update via PROC_SHIP_DO_CNT_PRINT_AFTER — P_DO_PRIORITY = 'C' or 'N' triggers the main update block
    private ValidateDocumentDto updateDeliveryOrderViaProc(Long transactionPoid, UpdateDeliveryOrderRequestDto request) {
        log.info("Updating delivery order via procedure for BL transaction: {}", transactionPoid);

        // UI sends LOV value (poid: "1","2","3","4") — DB/procedure expects LOV code ("C","N","B","O")
        String doPriorityCode = mapDoPriorityToCode(request.getDoPriority());
        String deliverySentToCode = mapDeliverySentToCode(request.getDeliverySentTo());

        log.info("[updateDeliveryOrder] doPriority: {} -> {}, deliverySentTo: {} -> {}",
                request.getDoPriority(), doPriorityCode, request.getDeliverySentTo(), deliverySentToCode);

        // snapshot BEFORE procedure runs — for logDetails diff
        ShipBlManifestHDR oldBlManifest = blManifestRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("BL Manifest", TRANSACTIONPOID, transactionPoid.toString()));
        ShipBlManifestHDR oldBlManifestSnapshot = new ShipBlManifestHDR();
        BeanUtils.copyProperties(oldBlManifest, oldBlManifestSnapshot);

        log.info("[updateDeliveryOrder] BEFORE — doPriority={}, deliverySentTo={}, doCntToConsignee={}, doCntToNotify={}, doCntToOthers={}, doCntToOthersMails={}, principalDoNumber={}, doIssueAuth={}",
                oldBlManifest.getDoPriority(), oldBlManifest.getDeliverySentTo(),
                oldBlManifest.getDoCntToConsignee(), oldBlManifest.getDoCntToNotify(),
                oldBlManifest.getDoCntToOthers(), oldBlManifest.getDoCntToOthersMails(),
                oldBlManifest.getPrincipalDoNumber(), oldBlManifest.getDoIssueAuth());

        DoShPrintingDtl oldDoShPrintingDtl = doShPrintingDtlRepository.findByTransactionPoid(transactionPoid)
                .orElse(null);
        DoShPrintingDtl oldDoShPrintingDtlSnapshot = null;
        if (oldDoShPrintingDtl != null) {
            oldDoShPrintingDtlSnapshot = new DoShPrintingDtl();
            BeanUtils.copyProperties(oldDoShPrintingDtl, oldDoShPrintingDtlSnapshot);
            log.info("[updateDeliveryOrder] BEFORE — doReleasedIdPerson={}, doReleasedToPerson={}, doReleasedAddrsPerson={}, orignalBlReleaseCr={}, reprintBy={}",
                    oldDoShPrintingDtl.getDoReleasedIdPerson(), oldDoShPrintingDtl.getDoReleasedToPerson(),
                    oldDoShPrintingDtl.getDoReleasedAddrsPerson(), oldDoShPrintingDtl.getOrignalBlReleaseCr(),
                    oldDoShPrintingDtl.getReprintBy());
        } else {
            log.info("[updateDeliveryOrder] BEFORE — DoShPrintingDtl row does not exist yet for transactionPoid={}", transactionPoid);
        }

        callProcShipDoCntPrintAfter(
                getGroupPoid(),
                getCompanyPoid(),
                transactionPoid,
                null,
                doPriorityCode,                    // P_UPDATE_TYPE: 'C' or 'N' triggers main update block
                getUserName(),
                request.getDoReleasedIdPerson(),
                request.getDoReleasedToPerson(),
                request.getDoReleasedAddressPerson(),
                request.getOriginalBlReleaseCr(),
                doPriorityCode,                    // P_DO_PRIORITY: code value for get_address_DO_NAME()
                StringUtils.defaultIfBlank(request.getDoCntToConsignee(), "N"),
                StringUtils.defaultIfBlank(request.getDoCntToNotify(), "N"),
                request.getDoCntToOthers(),
                request.getDoCntToOthersMails(),
                null,                              // P_DO_CNT_TO_REGS_MAILS: computed internally by procedure
                deliverySentToCode,
                request.getPrincipalDoNumber(),
                request.getRemarks()
        );

        // refresh AFTER procedure runs — evict from JPA cache first so we get fresh DB values
        // procedure uses AUTONOMOUS_TRANSACTION so its COMMIT is already done
        entityManager.refresh(oldBlManifest);
        ShipBlManifestHDR newBlManifest = oldBlManifest;

        DoShPrintingDtl newDoShPrintingDtl = null;
        if (oldDoShPrintingDtl != null) {
            entityManager.refresh(oldDoShPrintingDtl);
            newDoShPrintingDtl = oldDoShPrintingDtl;
        } else {
            // row may have been inserted by procedure — try loading it now
            newDoShPrintingDtl = doShPrintingDtlRepository.findByTransactionPoid(transactionPoid).orElse(null);
        }

        log.info("[updateDeliveryOrder] AFTER — doPriority={}, deliverySentTo={}, doCntToConsignee={}, doCntToNotify={}, doCntToOthers={}, doCntToOthersMails={}, principalDoNumber={}, doIssueAuth={}",
                newBlManifest.getDoPriority(), newBlManifest.getDeliverySentTo(),
                newBlManifest.getDoCntToConsignee(), newBlManifest.getDoCntToNotify(),
                newBlManifest.getDoCntToOthers(), newBlManifest.getDoCntToOthersMails(),
                newBlManifest.getPrincipalDoNumber(), newBlManifest.getDoIssueAuth());

        if (newDoShPrintingDtl != null) {
            log.info("[updateDeliveryOrder] AFTER — doReleasedIdPerson={}, doReleasedToPerson={}, doReleasedAddrsPerson={}, orignalBlReleaseCr={}, reprintBy={}",
                    newDoShPrintingDtl.getDoReleasedIdPerson(), newDoShPrintingDtl.getDoReleasedToPerson(),
                    newDoShPrintingDtl.getDoReleasedAddrsPerson(), newDoShPrintingDtl.getOrignalBlReleaseCr(),
                    newDoShPrintingDtl.getReprintBy());
        } else {
            log.info("[updateDeliveryOrder] AFTER — DoShPrintingDtl row still does not exist for transactionPoid={}", transactionPoid);
        }

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), transactionPoid.toString());
        loggingService.logDetails(oldBlManifestSnapshot, newBlManifest, ShipBlManifestHDR.class, UserContext.getDocumentId(), transactionPoid.toString(), "TRANSACTION_POID");
        if (oldDoShPrintingDtlSnapshot != null && newDoShPrintingDtl != null) {
            loggingService.logDetails(oldDoShPrintingDtlSnapshot, newDoShPrintingDtl, DoShPrintingDtl.class, UserContext.getDocumentId(), transactionPoid.toString(), "TRANSACTION_POID");
        }

        String canSendEmail = viewRepository.getGlobalParameterValue("START_DO_CNT_DIRECT_CUST", "START_DO_CNT_CUST", "1", "N");
        log.info("[updateDeliveryOrder] canSendEmail={}", canSendEmail);
        if ("Y".equalsIgnoreCase(canSendEmail)) {
            return new ValidateDocumentDto(false, "Verification Completed");
        }
        return new ValidateDocumentDto(true, null);
    }

    // DO_PRIORITY_SH: poid 1=C(Consignee), 2=N(Notify)
    private String mapDoPriorityToCode(String value) {
        return switch (StringUtils.defaultIfBlank(value, "1")) {
            case "1" -> "C";
            case "2" -> "N";
            default  -> value; // already a code (C/N), pass as-is
        };
    }

    // DELIVERY_SENT_TO: poid 1=C(Consignee), 2=N(Notify), 3=B(Both), 4=O(Only Additional Emails)
    private String mapDeliverySentToCode(String value) {
        return switch (StringUtils.defaultIfBlank(value, "1")) {
            case "1" -> "C";
            case "2" -> "N";
            case "3" -> "B";
            case "4" -> "O";
            default  -> value; // already a code (C/N/B/O), pass as-is
        };
    }

    // OLD: update via JPA (kept for reference)
    /*
    private Long updateDeliveryOrderViaJpa(Long transactionPoid, UpdateDeliveryOrderRequestDto request) {
        log.info("Updating delivery order for BL transaction: {}", transactionPoid);

        ShipBlManifestHDR blManifest = blManifestRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("BL Manifest", TRANSACTIONPOID, transactionPoid.toString()));

        ShipBlManifestHDR oldBlManifest = new ShipBlManifestHDR();
        BeanUtils.copyProperties(blManifest, oldBlManifest);

        if (StringUtils.isNotBlank(request.getDoPriority())) {
            blManifest.setDoPriority(request.getDoPriority());
        }
        if (StringUtils.isNotBlank(request.getDeliverySentTo())) {
            blManifest.setDeliverySentTo(request.getDeliverySentTo());
        }
        if (StringUtils.isNotBlank(request.getPrincipalDoNumber())) {
            blManifest.setPrincipalDoNumber(request.getPrincipalDoNumber());
        }
        if (StringUtils.isNotBlank(request.getDoCntToOthers())) {
            blManifest.setDoCntToOthers(request.getDoCntToOthers());
        }
        if (StringUtils.isNotBlank(request.getDoCntToOthersMails())) {
            blManifest.setDoCntToOthersMails(request.getDoCntToOthersMails());
        }
        if (StringUtils.isNotBlank(request.getRemarks())) {
            blManifest.setRemarks(request.getRemarks());
        }

        blManifestRepository.save(blManifest);

        DoShPrintingDtl doShPrintingDtl = doShPrintingDtlRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery order ship printing detail", TRANSACTIONPOID, transactionPoid.toString()));

        DoShPrintingDtl oldDoShPrintingDtl = new DoShPrintingDtl();
        BeanUtils.copyProperties(doShPrintingDtl, oldDoShPrintingDtl);

        if (StringUtils.isNotBlank(request.getOriginalBlReleaseCr())) {
            doShPrintingDtl.setOrignalBlReleaseCr(request.getOriginalBlReleaseCr());
        }
        if (StringUtils.isNotBlank(request.getDoReleasedIdPerson())) {
            doShPrintingDtl.setDoReleasedIdPerson(request.getDoReleasedIdPerson());
        }
        if (StringUtils.isNotBlank(request.getDoReleasedToPerson())) {
            doShPrintingDtl.setDoReleasedToPerson(request.getDoReleasedToPerson());
        }
        if (StringUtils.isNotBlank(request.getDoReleasedAddressPerson())) {
            doShPrintingDtl.setDoReleasedAddrsPerson(request.getDoReleasedAddressPerson());
        }

        doShPrintingDtlRepository.save(doShPrintingDtl);

        loggingService.createLogSummaryEntry(LogDetailsEnum.MODIFIED, UserContext.getDocumentId(), transactionPoid.toString());
        loggingService.logDetails(oldBlManifest, blManifest, ShipBlManifestHDR.class, UserContext.getDocumentId(), transactionPoid.toString(), "TRANSACTION_POID");
        loggingService.logDetails(oldDoShPrintingDtl, doShPrintingDtl, DoShPrintingDtl.class, UserContext.getDocumentId(), transactionPoid.toString(), "TRANSACTION_POID");
        return transactionPoid;
    }
    */

    @Override
    public byte[] print(Long transactionPoid, IssueDeliveryOrderRequestDto requestDto, ButtonType buttonType) throws Exception {

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        String username = getUserName();

        log.info("[PRINT] Starting print for transactionPoid={}, buttonType={}, groupPoid={}, companyPoid={}, username={}",
                transactionPoid, buttonType, groupPoid, companyPoid, username);

        DoShPrintingDtl dtl = doShPrintingDtlRepository.findByTransactionPoid(transactionPoid)
                .orElseThrow(() -> new ValidationException("Document must be validated before printing"));
        log.info("[PRINT] DoShPrintingDtl found: idPerson={}, toPerson={}, addrsPerson={}, blReleaseCr={}",
                dtl.getDoReleasedIdPerson(), dtl.getDoReleasedToPerson(), dtl.getDoReleasedAddrsPerson(), dtl.getOrignalBlReleaseCr());

        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-414");
        params.put("P_TRAN_NO", transactionPoid.toString()); // JRXML declares P_TRAN_NO as java.lang.String
        log.info("[PRINT] Params built, keys={}", params.keySet());

        byte[] result = generatePrintByButtonType(transactionPoid, buttonType, params);
        log.info("[PRINT] generatePrintByButtonType completed, resultSize={} bytes", result != null ? result.length : 0);

        if (result != null && result.length > 0) {
            callProcShipDoCntPrintAfter(groupPoid, companyPoid, transactionPoid, null, buttonType.name(),
                    username, dtl.getDoReleasedIdPerson(), dtl.getDoReleasedToPerson(),
                    dtl.getDoReleasedAddrsPerson(), dtl.getOrignalBlReleaseCr(),
                    null, null, null, null, null, null, null, null, null);
            return result;
        }

        return new byte[0]; // unreachable — generate methods now throw instead of returning null
    }

    // validateDocument is no longer used — edit API handles save + canPrint check
    /*
    @Override
    public ValidateDocumentDto validateDocument(Long id, IssueDeliveryOrderRequestDto requestDto) {

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        String username = getUserName();

        validateAllFields(id, null, requestDto);

        callProcShipDoCntPrintAfter(
                groupPoid, companyPoid, id, null,
                ARSHRCPTPRINTUPDATE, username,
                requestDto.getDoReleasedIdPerson(),
                requestDto.getDoReleasedToPerson(),
                requestDto.getDoReleasedAddressPerson(),
                requestDto.getOriginalBlReleaseCr(),
                requestDto.getDoPriority(),
                requestDto.getDoCntToConsignee(),
                requestDto.getDoCntToNotify(),
                requestDto.getDoCntToOthers(),
                requestDto.getDoCntToOthersMails(),
                requestDto.getEmailsDo(),
                requestDto.getDeliverySentTo(),
                requestDto.getPrincipalDoNumber()
        );
        String canSendEmail = viewRepository.getGlobalParameterValue("START_DO_CNT_DIRECT_CUST", "START_DO_CNT_CUST", "1", "N");
        log.info("canSendEmail :{} ", canSendEmail);

        if ("Y".equalsIgnoreCase(canSendEmail)) {
            return new ValidateDocumentDto(false, "Verification Completed");
        }
        return new ValidateDocumentDto(true, null);
    }
    */

    @Override
    public ValidateDocumentDto validateDocument(Long id, IssueDeliveryOrderRequestDto requestDto) {
        throw new UnsupportedOperationException("validateDocument is no longer used. Use the edit API instead.");
    }

    private byte[] generatePrintByButtonType(Long transactionPoid, ButtonType buttonType, Map<String, Object> params) throws Exception {
        return switch (buttonType) {
            case DELIVERYORDERPRINT -> generateDeliveryOrderPrint(transactionPoid, params);
            case CONTAINERFORMPRINT -> generateContainerFormPrint(transactionPoid, params);
            case RETURNFORMPRINT -> generateReturnFormPrint(transactionPoid, params);
        };
    }

    private byte[] generateDeliveryOrderPrint(Long transactionPoid, Map<String, Object> params) throws Exception {
        log.info("[DELIVERYORDERPRINT] Starting for transactionPoid={}", transactionPoid);
        validatePrintDocument(transactionPoid, "DO");
        String templatePath = "Shipping/SH/DO_SH.jrxml";
        log.info("[DELIVERYORDERPRINT] Loading template={}", templatePath);
        JasperReport mainReport = printService.load(templatePath);
        log.info("[DELIVERYORDERPRINT] Template loaded, calling fillReportToPdf with P_TRAN_NO={}, paramKeys={}",
                params.get("P_TRAN_NO"), params.keySet());
        byte[] pdf = printService.fillReportToPdf(mainReport, params, dataSource);
        log.info("[DELIVERYORDERPRINT] fillReportToPdf completed, pdfSize={} bytes", pdf != null ? pdf.length : 0);
        if (pdf != null && pdf.length <= 1024) {
            log.warn("[DELIVERYORDERPRINT] PDF is suspiciously small ({} bytes) — likely empty report (no rows returned by query) for transactionPoid={}",
                    pdf.length, transactionPoid);
        }
        return pdf;
    }

    private byte[] generateContainerFormPrint(Long transactionPoid, Map<String, Object> params) throws Exception {
        log.info("[CONTAINERFORMPRINT] Starting for transactionPoid={}", transactionPoid);
        validatePrintDocument(transactionPoid, "DELIVERYFORM");

        String pLineCode = viewRepository.getPlineCode(transactionPoid);
        log.info("[CONTAINERFORMPRINT] pLineCode={}", pLineCode);

        String templatePath = "HANJN".equalsIgnoreCase(pLineCode) ?
                "Shipping/SH/Container_Delivery_ValidityHJS_Currently_not.jrxml" :
                "Shipping/SH/Container_Delivery_Validity.jrxml";
        log.info("[CONTAINERFORMPRINT] Loading template={}", templatePath);

        JasperReport mainReport = printService.load(templatePath);
        log.info("[CONTAINERFORMPRINT] Template loaded successfully");

        String fslStamp = "FSL_STAMP";
        try {
            InputStream stampStream = getClass().getClassLoader().getResourceAsStream("jasper/Shipping/jpg/FSL_STAMP.jpg");
            if (stampStream == null) {
                log.warn("[CONTAINERFORMPRINT] FSL_STAMP.jpg not found in classpath");
                params.put(fslStamp, null);
            } else {
                log.info("[CONTAINERFORMPRINT] FSL_STAMP.jpg loaded successfully");
                byte[] stampBytes = stampStream.readAllBytes();
                stampStream.close();
                params.put(fslStamp, new java.io.ByteArrayInputStream(stampBytes));
            }
        } catch (Exception e) {
            log.error("[CONTAINERFORMPRINT] Error loading FSL_STAMP.jpg", e);
            params.put(fslStamp, null);
        }

        if ("HANJN".equalsIgnoreCase(pLineCode)) {
            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("jasper/Shipping/jpg/hidd_map4.jpg");
            if (imageStream != null) {
                log.info("[CONTAINERFORMPRINT] hidd_map4.jpg loaded successfully");
                params.put("IMAGE_MAP", imageStream);
            } else {
                log.warn("[CONTAINERFORMPRINT] hidd_map4.jpg not found in classpath");
            }
        }

        log.info("[CONTAINERFORMPRINT] Calling fillReportToPdf with P_TRAN_NO={}, paramKeys={}",
                params.get("P_TRAN_NO"), params.keySet());
        byte[] pdf = printService.fillReportToPdf(mainReport, params, dataSource);
        log.info("[CONTAINERFORMPRINT] fillReportToPdf completed, pdfSize={} bytes", pdf != null ? pdf.length : 0);
        if (pdf != null && pdf.length <= 1024) {
            log.warn("[CONTAINERFORMPRINT] PDF is suspiciously small ({} bytes) — likely empty report (no rows returned by query) for transactionPoid={}",
                    pdf.length, transactionPoid);
            diagnoseCntFormQuery(transactionPoid);
        }
        return pdf;
    }

    private byte[] generateReturnFormPrint(Long transactionPoid, Map<String, Object> params) throws Exception {
        log.info("[RETURNFORMPRINT] Starting for transactionPoid={}", transactionPoid);
        validatePrintDocument(transactionPoid, "RETURNFORM");

        String templatePath = "Shipping/SH/Container_Return_Validity.jrxml";
        log.info("[RETURNFORMPRINT] Loading template={}", templatePath);
        JasperReport mainReport = printService.load(templatePath);
        log.info("[RETURNFORMPRINT] Template loaded successfully");

        log.info("[RETURNFORMPRINT] Calling fillReportToPdf with P_TRAN_NO={}, paramKeys={}",
                params.get("P_TRAN_NO"), params.keySet());
        byte[] pdf = printService.fillReportToPdf(mainReport, params, dataSource);
        log.info("[RETURNFORMPRINT] fillReportToPdf completed, pdfSize={} bytes", pdf != null ? pdf.length : 0);
        if (pdf != null && pdf.length <= 1024) {
            log.warn("[RETURNFORMPRINT] PDF is suspiciously small ({} bytes) — likely empty report (no rows returned by query) for transactionPoid={}",
                    pdf.length, transactionPoid);
        }
        return pdf;
    }

    private void validatePrintDocument(Long transactionPoid, String printDocument) {
        log.info("[validatePrintDocument] START transactionPoid={}, printDocument={}", transactionPoid, printDocument);

        String result = viewRepository.callFuncPrintDoCntRtnForm(
                getGroupPoid(), getCompanyPoid(), getUserPoid(),
                UserContext.getDocumentId(), transactionPoid, printDocument);

        log.info("[validatePrintDocument] FUNC_PRINT_DO_CNT_RTN_FORM result={}, transactionPoid={}, printDocument={}",
                result, transactionPoid, printDocument);

        if (!"N".equalsIgnoreCase(result)) {
            throw new ValidationException(result);
        }
    }

    private void validateAllFields(Long transactionPoid, DeliveryOrderIssueToCustomerDto dto, IssueDeliveryOrderRequestDto request) {
        if (dto == null) {
            dto = viewRepository.findByTransactionPoid(transactionPoid)
                    .orElseThrow(() -> new ResourceNotFoundException(DELIVERYORDER, TRANSACTIONPOID, transactionPoid.toString()));
        }
        if ("Y".equalsIgnoreCase(dto.getPrincipalDoRequired())) {
            if (request.getPrincipalDoNumber() == null || request.getPrincipalDoNumber().trim().length() <= 3) {
                throw new ValidationException("Principal Do number can not be blank");
            }
        }
        if (StringUtils.isBlank(request.getDoReleasedAddressPerson())) {
            throw new ValidationException("Address can not be blank");
        }
        if (StringUtils.isBlank(request.getDoReleasedIdPerson())) {
            throw new ValidationException("ID/CPR can not be blank");
        }
        if (StringUtils.isBlank(request.getDoReleasedToPerson())) {
            throw new ValidationException("Name can not be blank");
        }
        if (StringUtils.isBlank(request.getDoPriority())) {
            throw new ValidationException("Do Issue TO, can not be blank");
        }
        if (StringUtils.isBlank(request.getOriginalBlReleaseCr())) {
            throw new ValidationException("Bl issue type can not be blank");
        }
        if (StringUtils.isBlank(dto.getBlReleaseTypeOffice())) {
            throw new ValidationException("Office Bl issue type can not be blank");
        }
        if (!dto.getBlReleaseTypeOffice().equalsIgnoreCase(request.getOriginalBlReleaseCr())) {
            throw new ValidationException("Check Bl issue type");
        }
        if (StringUtils.isBlank(request.getDeliverySentTo())) {
            throw new ValidationException("Select delivery send to from dropdown list");
        }
        validateEmailConfiguration(request);
    }

    private void validateEmailConfiguration(IssueDeliveryOrderRequestDto request) {
        if (StringUtils.isNotBlank(request.getEmailsAdditional()) &&
                (request.getDoCntToOthers() == null || !"Y".equals(request.getDoCntToOthers()))) {
            throw new ValidationException("Select additional emails check box when providing additional emails");
        }

        if (StringUtils.isNotBlank(request.getEmailsAdditional())) {
            if (request.getEmailsAdditional().trim().length() <= 5) {
                throw new ValidationException("Check additional emails value - must be longer than 5 characters");
            }
            if (!request.getEmailsAdditional().contains("@")) {
                throw new ValidationException("Check additional emails value - must contain @ symbol");
            }
        }

        if (StringUtils.isBlank(request.getEmailsDo())) {
            throw new ValidationException("Delivery emails not added for customer");
        }

        if ("Y".equals(request.getDoCntToOthers()) && StringUtils.isNotBlank(request.getEmailsDo()) && StringUtils.isBlank(request.getEmailsAdditional())) {
            throw new ValidationException("Additional emails need to be added when others is selected");
        }
    }

    private void callProcShipDoCntPrintAfter(Long groupPoid, Long companyPoid, Long blPoid, Long splitBookingNo,
                                             String actionType, String user, String doReleasedIdPerson, String doReleasedToPerson,
                                             String doReleasedAddressPerson, String originalBlReleaseCr, String doPriority,
                                             String doCntToConsignee, String doCntToNotify, String doCntToOthers, String doCntToOthersMails,
                                             String doCntToRegsMails, String deliverySentTo, String principalDoNumber, String remarks) {

        try {
            String sql = "{call PROC_SHIP_DO_CNT_PRINT_AFTER(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";

            jdbcTemplate.execute((Connection connection) -> {
                try (CallableStatement cs = connection.prepareCall(sql)) {

                    cs.setLong(1, groupPoid);
                    cs.setLong(2, companyPoid);
                    cs.setLong(3, blPoid);
                    cs.setObject(4, splitBookingNo);

                    cs.setString(5, StringUtils.defaultIfBlank(actionType, ARSHRCPTPRINTUPDATE));
                    cs.setString(6, user);

                    cs.setString(7, StringUtils.defaultIfBlank(doReleasedIdPerson, null));
                    cs.setString(8, StringUtils.defaultIfBlank(doReleasedToPerson, null));
                    cs.setString(9, StringUtils.defaultIfBlank(doReleasedAddressPerson, null));
                    cs.setString(10, StringUtils.defaultIfBlank(originalBlReleaseCr, "0"));
//                    cs.setString(11, StringUtils.defaultIfBlank(doPriority, "C"));
                    cs.setString(11, (doPriority));

                    cs.setString(12, StringUtils.defaultIfBlank(doCntToConsignee, "N"));
                    cs.setString(13, StringUtils.defaultIfBlank(doCntToNotify, "N"));
                    cs.setString(14, StringUtils.defaultIfBlank(doCntToOthers, "N"));

                    cs.setString(15, StringUtils.defaultIfBlank(doCntToOthersMails, null));
                    cs.setString(16, StringUtils.defaultIfBlank(doCntToRegsMails, null));

                    cs.setString(17, StringUtils.defaultIfBlank(deliverySentTo, "C"));

                    cs.setString(18, StringUtils.defaultIfBlank(principalDoNumber, null));

                    cs.setString(19, StringUtils.defaultIfBlank(remarks, null));

                    cs.execute();
                }
                return null;
            });

            log.debug("Successfully called PROC_SHIP_DO_CNT_PRINT_AFTER for BL transaction: {}", blPoid);

        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_DO_CNT_PRINT_AFTER for BL transaction: {}", blPoid, e);
            throw new ValidationException("Error processing delivery order: " + e.getMessage());
        }
    }

    private void callProcShipDoCntPrintAfterNotUpdate(Long groupPoid, Long companyPoid, Long blPoid, Long splitBookingNo,
                                                      String actionType, String user, String doReleasedIdPerson, String doReleasedToPerson,
                                                      String doReleasedAddrsPerson, String originalBlReleaseCr) {

        try {
            String sql = "{call PROC_SHIP_DO_CNT_PRINT_AFTER(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";

            jdbcTemplate.execute((Connection connection) -> {
                try (CallableStatement cs = connection.prepareCall(sql)) {

                    cs.setLong(1, groupPoid);
                    cs.setLong(2, companyPoid);
                    cs.setLong(3, blPoid);
                    cs.setObject(4, splitBookingNo);

                    cs.setString(5, StringUtils.defaultIfBlank(actionType, ARSHRCPTPRINTUPDATE));
                    cs.setString(6, user);

                    cs.setString(7, StringUtils.defaultIfBlank(doReleasedIdPerson, null));
                    cs.setString(8, StringUtils.defaultIfBlank(doReleasedToPerson, null));
                    cs.setString(9, StringUtils.defaultIfBlank(doReleasedAddrsPerson, null));
                    cs.setString(10, StringUtils.defaultIfBlank(originalBlReleaseCr, null));

                    cs.setString(11, "NOT_UPDATE");

                    cs.execute();
                }
                return null;
            });

            log.debug("Successfully called PROC_SHIP_DO_CNT_PRINT_AFTER (NOT_UPDATE) for BL transaction: {}", blPoid);

        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_DO_CNT_PRINT_AFTER (NOT_UPDATE) for BL transaction: {}", blPoid, e);
            throw new ValidationException("Error processing delivery order: " + e.getMessage());
        }
    }

    private void enrichWithLovData(DeliveryOrderIssueToCustomerDto dto) {
        dto.setDoPriorityDet(lovService.getDetailsByCodeAndLovName(dto.getDoPriority(), "DO_PRIORITY_SH"));
        dto.setDoIssueAuthPoidDet(lovService.getDetailsByPoidAndLovName(dto.getDoIssueAuthPoid(), "USER_MASTER"));
        dto.setDeliverySentToDet(lovService.getDetailsByCodeAndLovName(dto.getDeliverySentTo(), "DELIVERY_SENT_TO"));
    }

    public String checkPrintDocumentData(Long transactionPoid, String printType) {
        log.info("[checkPrintDocumentData] START transactionPoid={}, printType={}", transactionPoid, printType);
        try {
            List<Object[]> results = viewRepository.fetchShipLineDetails(transactionPoid);

            if (results.isEmpty()) {
                log.warn("[checkPrintDocumentData] No SHIP_LINE_MASTER row found for transactionPoid={} — returning N", transactionPoid);
                return "N";
            }

            Object[] row = results.get(0);
            String containerFormVhent = convertToString(row[0]);
            String containerFormRtn = convertToString(row[1]);
            String doPrintLine = convertToString(row[2]);
            String lineCode = convertToString(row[3]);
            String rcptPrintLine = convertToString(row[4]);

            log.info("[checkPrintDocumentData] SHIP_LINE_MASTER values: transactionPoid={}, printType={}, " +
                            "CONTAINER_FORM_VHENT={}, CONTAINER_FORM_RTN={}, DO_PRINT_LINE={}, LINE_CODE={}, RCPT_PRINT_LINE={}",
                    transactionPoid, printType, containerFormVhent, containerFormRtn, doPrintLine, lineCode, rcptPrintLine);

            String result = switch (printType.toUpperCase()) {
                case "DO"     -> doPrintLine != null ? doPrintLine : "N";
                case "RTNCNT" -> containerFormRtn != null ? containerFormRtn : "N";
                case "DLVCNT" -> containerFormVhent != null ? containerFormVhent : "N";
                case "RCPCNT" -> rcptPrintLine != null ? rcptPrintLine : "N";
                default       -> "N";
            };

            log.info("[checkPrintDocumentData] Result for printType={}: enabled={}, transactionPoid={}",
                    printType, result, transactionPoid);
            return result;

        } catch (Exception e) {
            log.error("[checkPrintDocumentData] Error fetching SHIP_LINE_MASTER for transactionPoid={}, printType={}",
                    transactionPoid, printType, e);
            return "N";
        }
    }

    /**
     * Runs each WHERE-clause filter of Container_Delivery_Validity.jrxml independently
     * and logs the row count so we can pinpoint which condition eliminates the data.
     * Called only when the PDF comes back suspiciously small (empty report).
     */
    private void diagnoseCntFormQuery(Long transactionPoid) {
        log.warn("[DIAGNOSE-CNTFORM] Starting query filter diagnosis for transactionPoid={}", transactionPoid);

        // Base join — no filters yet
        String base =
            "SELECT COUNT(*) FROM SHIP_BL_MANIFEST_CONTAINER_DTL D " +
            "JOIN SHIP_BL_MANIFEST_HDR H ON D.TRANSACTION_POID = H.TRANSACTION_POID " +
            "JOIN SHIP_VOYAGE_HDR VY ON VY.TRANSACTION_POID = H.VOYAGE_TRANSACTION_POID " +
            "JOIN SHIP_LINE_MASTER SLN ON SLN.LINE_POID = VY.LINE_POID " +
            "JOIN SHIP_CONTAINER_TYPE_MASTER CONTTYP ON CONTTYP.CONTAINER_TYPE_CODE = D.EQUIPMENT_ISO_TYPE " +
            "JOIN SHIP_LINE_TARIFF_HDR shlinetHD ON shlinetHD.LINE_POID = VY.LINE_POID " +
            "JOIN SHIP_LINE_TARIFF_IMP_DTL shlinetrf ON shlinetHD.TRANSACTION_POID = shlinetrf.TRANSACTION_POID " +
            "  AND shlinetrf.CONTAINER_TYPE_POID = GET_CONTAINER_CODE_POID(D.EQUIPMENT_ISO_TYPE) " +
            "WHERE H.TRANSACTION_POID = ?";

        logCount("[DIAGNOSE-CNTFORM] 1. Base joins only (no filters)", base, transactionPoid);

        logCount("[DIAGNOSE-CNTFORM] 2. + RETURN_FROM_CONSIGNEE IS NULL",
            base + " AND D.RETURN_FROM_CONSIGNEE IS NULL", transactionPoid);

        logCount("[DIAGNOSE-CNTFORM] 3. + HOLD_REASON filter (IS NULL OR NOT IN '1','3')",
            base + " AND D.RETURN_FROM_CONSIGNEE IS NULL" +
            " AND (D.HOLD_REASON IS NULL OR D.HOLD_REASON NOT IN ('1','3'))", transactionPoid);

        logCount("[DIAGNOSE-CNTFORM] 4. + CONTAINER_TYPE_CATEGORY NOT IN ('SPL','REF')",
            base + " AND D.RETURN_FROM_CONSIGNEE IS NULL" +
            " AND (D.HOLD_REASON IS NULL OR D.HOLD_REASON NOT IN ('1','3'))" +
            " AND CONTTYP.CONTAINER_TYPE_CATEGORY NOT IN ('SPL','REF')", transactionPoid);

        logCount("[DIAGNOSE-CNTFORM] 5. + NVL(SLN.CONTAINER_FORM_RTN,'N')='Y'",
            base + " AND D.RETURN_FROM_CONSIGNEE IS NULL" +
            " AND (D.HOLD_REASON IS NULL OR D.HOLD_REASON NOT IN ('1','3'))" +
            " AND CONTTYP.CONTAINER_TYPE_CATEGORY NOT IN ('SPL','REF')" +
            " AND NVL(SLN.CONTAINER_FORM_RTN,'N') = 'Y'", transactionPoid);

        logCount("[DIAGNOSE-CNTFORM] 6. + LINE_POID NOT IN (COS/BSL 20ft exclusion)",
            base + " AND D.RETURN_FROM_CONSIGNEE IS NULL" +
            " AND (D.HOLD_REASON IS NULL OR D.HOLD_REASON NOT IN ('1','3'))" +
            " AND CONTTYP.CONTAINER_TYPE_CATEGORY NOT IN ('SPL','REF')" +
            " AND NVL(SLN.CONTAINER_FORM_RTN,'N') = 'Y'" +
            " AND VY.LINE_POID NOT IN (SELECT LINE_POID FROM SHIP_LINE_MASTER WHERE LINE_CODE IN ('COS','BSL') AND CONTAINER_TYPE_SIZE='20')",
            transactionPoid);

        logCount("[DIAGNOSE-CNTFORM] 7. + ARRIVAL_DATE BETWEEN PERIOD_FROM AND PERIOD_TO (tariff date range)",
            base + " AND D.RETURN_FROM_CONSIGNEE IS NULL" +
            " AND (D.HOLD_REASON IS NULL OR D.HOLD_REASON NOT IN ('1','3'))" +
            " AND CONTTYP.CONTAINER_TYPE_CATEGORY NOT IN ('SPL','REF')" +
            " AND NVL(SLN.CONTAINER_FORM_RTN,'N') = 'Y'" +
            " AND VY.LINE_POID NOT IN (SELECT LINE_POID FROM SHIP_LINE_MASTER WHERE LINE_CODE IN ('COS','BSL') AND CONTAINER_TYPE_SIZE='20')" +
            " AND TO_DATE(NVL(VY.ARRIVAL_DATE, VY.EXPECTED_DATE)) BETWEEN TO_DATE(shlinetrf.PERIOD_FROM) AND TO_DATE(shlinetrf.PERIOD_TO)",
            transactionPoid);

        logCount("[DIAGNOSE-CNTFORM] 8. + NVL(print_return_form_DEFAULT,'N') IN ('Y','MANUALLYPRINT') — FULL query",
            base + " AND D.RETURN_FROM_CONSIGNEE IS NULL" +
            " AND (D.HOLD_REASON IS NULL OR D.HOLD_REASON NOT IN ('1','3'))" +
            " AND CONTTYP.CONTAINER_TYPE_CATEGORY NOT IN ('SPL','REF')" +
            " AND NVL(SLN.CONTAINER_FORM_RTN,'N') = 'Y'" +
            " AND VY.LINE_POID NOT IN (SELECT LINE_POID FROM SHIP_LINE_MASTER WHERE LINE_CODE IN ('COS','BSL') AND CONTAINER_TYPE_SIZE='20')" +
            " AND TO_DATE(NVL(VY.ARRIVAL_DATE, VY.EXPECTED_DATE)) BETWEEN TO_DATE(shlinetrf.PERIOD_FROM) AND TO_DATE(shlinetrf.PERIOD_TO)" +
            " AND NVL(shlinetrf.PRINT_RETURN_FORM_DEFAULT,'N') IN ('Y','MANUALLYPRINT')",
            transactionPoid);

        log.warn("[DIAGNOSE-CNTFORM] Diagnosis complete for transactionPoid={}", transactionPoid);
    }

    private void logCount(String label, String sql, Long transactionPoid) {
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, transactionPoid);
            log.warn("{} => rowCount={}", label, count);
        } catch (Exception e) {
            log.error("{} => ERROR: {}", label, e.getMessage());
        }
    }

    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str;
        }
        if (value instanceof Character ch) {
            return String.valueOf(ch);
        }
        return value.toString();
    }


    @Override
    public Map<String, Object> searchDeliveryOrders(String documentId, com.asg.common.lib.dto.FilterRequestDto filters, Pageable pageable) {
        try {
            String operator = documentSearchService.resolveOperator(filters);
            String isDeleted = documentSearchService.resolveIsDeleted(filters);
            List<FilterDto> filterList = documentSearchService.resolveFilters(filters);

            RawSearchResult raw = documentSearchService.search(
                    documentId,
                    filterList,
                    operator,
                    pageable,
                    isDeleted,
                    "BL_NUMBER",
                    "TRANSACTION_POID");

            Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());
            return PaginationUtil.wrapPage(page, raw.displayFields());
        } catch (Exception e) {
            log.error("Error searching delivery orders", e);
            throw e;
        }
    }
}
