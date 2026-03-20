package com.asg.shipping.deliveryorderissuetocustomer.service;

import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
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
import com.asg.shipping.remuneration.entity.ShipRemunerationMaster;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    private static final String ARSHRCPTPRINTUPDATE="ARSHRCPTPRINTUPDATE";
    private static final String TRANSACTIONPOID="transactionPoid";
    private static final String DELIVERYORDER="Delivery Order";

    @Override
    @Transactional(readOnly = true)
    public DeliveryOrderIssueToCustomerDto getDeliveryOrderIssueToCustomer(Long transactionPoid) {
        log.info("Getting delivery order with transactionPoid: {}, company poid: {}", transactionPoid,getCompanyPoid());

        DeliveryOrderIssueToCustomerDto dto = viewRepository.findByTransactionPoid(transactionPoid, getCompanyPoid())
                .orElseThrow(() -> new ResourceNotFoundException(DELIVERYORDER, TRANSACTIONPOID, transactionPoid.toString()));

        enrichWithLovData(dto);
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
        DeliveryOrderIssueToCustomerDto dto = viewRepository.findByTransactionPoid(transactionPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(DELIVERYORDER, TRANSACTIONPOID, transactionPoid.toString()));

        // Validate Principal DO Number
        if ("Y".equalsIgnoreCase(dto.getPrincipalDoRequired()) && StringUtils.isBlank(request.getPrincipalDoNumber()) || request.getPrincipalDoNumber().trim().length() <= 3) {
            throw new ValidationException("Principal Do number can not be blank");
        }

        // Validate Address
        if (StringUtils.isBlank(request.getDoReleasedAddressPerson())) {
            throw new ValidationException("Address can not be blank");
        }

        // Validate ID/CPR
        if (StringUtils.isBlank(request.getDoReleasedIdPerson())) {
            throw new ValidationException("ID/CPR can not be blank");
        }

        // Validate Name
        if (StringUtils.isBlank(request.getDoReleasedToPerson())) {
            throw new ValidationException("Name can not be blank");
        }

        // Validate DO Priority
        if (StringUtils.isBlank(request.getDoPriority())) {
            throw new ValidationException("Do Issue TO, can not be blank");
        }

        // Validate Original BL Release CR
        if (StringUtils.isBlank(request.getOriginalBlReleaseCr())) {
            throw new ValidationException("Bl issue type can not be blank");
        }

        // Validate BL Release Type Office matches Original BL Release CR
        if (StringUtils.isBlank(dto.getBlReleaseTypeOffice())) {
            throw new ValidationException("Office Bl issue type can not be blank");
        }
        if (!dto.getBlReleaseTypeOffice().equalsIgnoreCase(request.getOriginalBlReleaseCr())) {
            throw new ValidationException("Check Bl issue type");
        }

        // Validate Delivery Sent To
        if (StringUtils.isBlank(request.getDeliverySentTo())) {
            throw new ValidationException("Select delivery send to from dropdown list");
        }

        // Validate Email Configuration
        validateEmailConfiguration(request);

        ShipBlManifestHDR blManifest = blManifestRepository.findById(transactionPoid)
                .orElseThrow(() -> new ResourceNotFoundException("BL Manifest", TRANSACTIONPOID, transactionPoid.toString()));

        if ("Y".equals(blManifest.getDeleted())) {
            throw new ResourceNotFoundException("BL Manifest", TRANSACTIONPOID, transactionPoid.toString());
        }

        blManifestRepository.save(blManifest);
        loggingService.createLogSummaryEntry(LogDetailsEnum.CREATED, UserContext.getDocumentId(), transactionPoid.toString());

        // Call PROC_SHIP_DO_CNT_PRINT_AFTER with 18 parameters
        // In legacy, this is called before printing. NOT_UPDATE is called AFTER printing all documents.
        // In new architecture, printing is handled separately via print endpoints, so NOT_UPDATE
        // will be called in the print endpoint after each document is printed.
        callProcShipDoCntPrintAfter(groupPoid, companyPoid, transactionPoid, null,
                ARSHRCPTPRINTUPDATE, username, request.getDoReleasedIdPerson(),
                request.getDoReleasedToPerson(), request.getDoReleasedAddressPerson(),
                request.getOriginalBlReleaseCr(), request.getDoPriority(), request.getDoCntToConsignee(),
                request.getDoCntToNotify(), request.getDoCntToOthers(), request.getDoCntToOthersMails(),
                request.getEmailsDo(), request.getDeliverySentTo(), request.getPrincipalDoNumber());
    }

    @Override
    @Transactional
    public Long updateDeliveryOrder(Long transactionPoid, UpdateDeliveryOrderRequestDto request) {

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
        loggingService.logChanges(oldBlManifest,blManifest,ShipBlManifestHDR.class,UserContext.getDocumentId(),blManifest.getTransactionPoid().toString(),LogDetailsEnum.MODIFIED,"TRANSACTION_POID");

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
        loggingService.logChanges(oldDoShPrintingDtl,doShPrintingDtl,DoShPrintingDtl.class,UserContext.getDocumentId(),doShPrintingDtl.getTransactionPoid().toString(),LogDetailsEnum.MODIFIED,"TRANSACTION_POID");
        return transactionPoid;
    }

    @Override
    public byte[] print(Long transactionPoid, IssueDeliveryOrderRequestDto requestDto, ButtonType buttonType) throws Exception {

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        String username = getUserName();

        // In legacy, print methods only check if document can be printed and print it.
        // All field validations are done once in doCntPrint() before printing.
        // Here we only validate print conditions, not field values.
        
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-414");
        params.put("P_TRAN_NO", transactionPoid);

        byte[] result = generatePrintByButtonType(transactionPoid, buttonType, params);
        if (result != null) {
            // Call PROC_SHIP_DO_CNT_PRINT_AFTER with 11 parameters (NOT_UPDATE) after successful print
            // In legacy, this is called once after all 3 documents are printed in sequence.
            // In new architecture, we call it after each successful print since printing is done separately.
            // The stored procedure should handle being called multiple times gracefully.
            callProcShipDoCntPrintAfterNotUpdate(groupPoid, companyPoid, transactionPoid, null, ARSHRCPTPRINTUPDATE,
                    username, requestDto.getDoReleasedIdPerson(), requestDto.getDoReleasedToPerson(),
                    requestDto.getDoReleasedAddressPerson(), requestDto.getOriginalBlReleaseCr()
            );
            return result;
        }
        
        // If printing failed (result is null), don't call NOT_UPDATE
        // This happens when document cannot be printed (print validation failed or already printed)
        return null;
    }

    @Override
    public ValidateDocumentDto validateDocument(Long id, IssueDeliveryOrderRequestDto requestDto) {

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        String username = getUserName();

        // Fetch the delivery order DTO to get blReleaseTypeOffice and principalDoRequired
        DeliveryOrderIssueToCustomerDto dto = viewRepository.findByTransactionPoid(id, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException(DELIVERYORDER, TRANSACTIONPOID, id.toString()));

        // Validate Principal DO Number
        if ("Y".equalsIgnoreCase(dto.getPrincipalDoRequired()) && StringUtils.isBlank(requestDto.getPrincipalDoNumber()) || requestDto.getPrincipalDoNumber().trim().length() <= 3) {
            throw new ValidationException("Principal Do number can not be blank");
        }

        // Validate Address
        if (StringUtils.isBlank(requestDto.getDoReleasedAddressPerson())) {
            throw new ValidationException("Address can not be blank");
        }

        // Validate ID/CPR
        if (StringUtils.isBlank(requestDto.getDoReleasedIdPerson())) {
            throw new ValidationException("ID/CPR can not be blank");
        }

        // Validate Name
        if (StringUtils.isBlank(requestDto.getDoReleasedToPerson())) {
            throw new ValidationException("Name can not be blank");
        }

        // Validate DO Priority
        if (StringUtils.isBlank(requestDto.getDoPriority())) {
            throw new ValidationException("Do Issue TO, can not be blank");
        }

        // Validate Original BL Release CR
        if (StringUtils.isBlank(requestDto.getOriginalBlReleaseCr())) {
            throw new ValidationException("Bl issue type can not be blank");
        }

        // Validate BL Release Type Office matches Original BL Release CR
        if (StringUtils.isBlank(dto.getBlReleaseTypeOffice())) {
            throw new ValidationException("Office Bl issue type can not be blank");
        }
        if (!dto.getBlReleaseTypeOffice().equalsIgnoreCase(requestDto.getOriginalBlReleaseCr())) {
            throw new ValidationException("Check Bl issue type");
        }

        // Validate Delivery Sent To
        if (StringUtils.isBlank(requestDto.getDeliverySentTo())) {
            throw new ValidationException("Select delivery send to from dropdown list");
        }

        // Validate Email Configuration
        validateEmailConfiguration(requestDto);

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

    private byte[] generatePrintByButtonType(Long transactionPoid, ButtonType buttonType, Map<String, Object> params) throws Exception {
        return switch (buttonType) {
            case DELIVERYORDERPRINT -> generateDeliveryOrderPrint(transactionPoid, params);
            case CONTAINERFORMPRINT -> generateContainerFormPrint(transactionPoid, params);
            case RETURNFORMPRINT -> generateReturnFormPrint(transactionPoid, params);
        };
    }

    private byte[] generateDeliveryOrderPrint(Long transactionPoid, Map<String, Object> params) throws Exception {
        if (!validatePrintDocument(transactionPoid, "DO")) {
            return null;
        }
        JasperReport mainReport = printService.load("Shipping/SH/DO_SH.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    private byte[] generateContainerFormPrint(Long transactionPoid, Map<String, Object> params) throws Exception {
        if (!validatePrintDocument(transactionPoid, "DLVCNT")) {
            return null;
        }
        String pLineCode = viewRepository.getPlineCode(transactionPoid);
        String templatePath = "HANJN".equalsIgnoreCase(pLineCode) ?
                "Shipping/SH/Container_Delivery_ValidityHJS_Currently_not.jrxml" :
                "Shipping/SH/Container_Delivery_Validity.jrxml";
        JasperReport mainReport = printService.load(templatePath);

        String fslStamp="FSL_STAMP";

        try {
            InputStream stampStream = getClass().getClassLoader().getResourceAsStream("jasper/Shipping/jpg/FSL_STAMP.jpg");
            if (stampStream == null) {
                log.warn("FSL_STAMP.jpg not found in classpath");
                params.put(fslStamp, null);
            } else {
                log.info("FSL_STAMP.jpg loaded successfully");
                byte[] stampBytes = stampStream.readAllBytes();
                stampStream.close();
                params.put(fslStamp, new java.io.ByteArrayInputStream(stampBytes));
            }
        } catch (Exception e) {
            log.error("Error loading FSL_STAMP.jpg", e);
            params.put(fslStamp, null);
        }

        if ("HANJN".equalsIgnoreCase(pLineCode)) {
            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("jasper/Shipping/jpg/hidd_map4.jpg");
            if (imageStream != null) {
                params.put("IMAGE_MAP", imageStream);
            }
        }

        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    private byte[] generateReturnFormPrint(Long transactionPoid, Map<String, Object> params) throws Exception {
        if (!validatePrintDocument(transactionPoid, "RTNCNT")) {
            return null;
        }
        JasperReport mainReport = printService.load("Shipping/SH/Container_Return_Validity.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }

    private boolean validatePrintDocument(Long transactionPoid, String docType) {
        String printCheck = checkPrintDocumentData(transactionPoid, docType);
        if ("N".equalsIgnoreCase(printCheck)) {
            return false;
        }
        String alreadyPrinted = viewRepository.printDocumentAlreadyPrinted(docType, transactionPoid);
        return !"Y".equalsIgnoreCase(alreadyPrinted);
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
                                             String doCntToRegsMails, String deliverySentTo, String principalDoNumber) {

        try {
            String sql = "{call PROC_SHIP_DO_CNT_PRINT_AFTER(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";

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
                    cs.setString(11, StringUtils.defaultIfBlank(doPriority, "C"));

                    cs.setString(12, StringUtils.defaultIfBlank(doCntToConsignee, "N"));
                    cs.setString(13, StringUtils.defaultIfBlank(doCntToNotify, "N"));
                    cs.setString(14, StringUtils.defaultIfBlank(doCntToOthers, "N"));

                    cs.setString(15, StringUtils.defaultIfBlank(doCntToOthersMails, null));
                    cs.setString(16, StringUtils.defaultIfBlank(doCntToRegsMails, null));

                    cs.setString(17, StringUtils.defaultIfBlank(deliverySentTo, "C"));

                    cs.setString(18, StringUtils.defaultIfBlank(principalDoNumber, null));

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
        try {
            List<Object[]> results = viewRepository.fetchShipLineDetails(transactionPoid);

            if (results.isEmpty()) {
                return "N";
            }

            Object[] row = results.get(0);
            String containerFormVhent = convertToString(row[0]);
            String containerFormRtn = convertToString(row[1]);
            String doPrintLine = convertToString(row[2]);
            String lineCode = convertToString(row[3]);
            String rcptPrintLine = convertToString(row[4]);

            return switch (printType.toUpperCase()) {
                case "DO" -> doPrintLine != null ? doPrintLine : "N";
                case "RTNCNT" -> containerFormRtn != null ? containerFormRtn : "N";
                case "DLVCNT" -> containerFormVhent != null ? containerFormVhent : "N";
                case "RCPCNT" -> rcptPrintLine != null ? rcptPrintLine : "N";
                default -> "N";
            };

        } catch (Exception e) {
            log.error("Error checking print document data", e);
            return "N";
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
}
