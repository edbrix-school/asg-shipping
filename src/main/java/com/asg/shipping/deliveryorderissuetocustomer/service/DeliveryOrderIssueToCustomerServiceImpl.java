package com.asg.shipping.deliveryorderissuetocustomer.service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.PrintService;
import com.asg.shipping.deliveryorderissuetocustomer.dto.DeliveryOrderIssueToCustomerDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.IssueDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.UpdateDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.entity.ShipBlManifestHDR;
import com.asg.shipping.deliveryorderissuetocustomer.repository.DeliveryOrderIssueToCustomerRepository;
import com.asg.shipping.deliveryorderissuetocustomer.repository.ShipBlManifestHDRRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final LovDataService lovService;
    private final JdbcTemplate jdbcTemplate;
    private final PrintService printService;
    private final DataSource dataSource;


    @Override
    @Transactional(readOnly = true)
    public DeliveryOrderIssueToCustomerDto getDeliveryOrderIssueToCustomer(Long transactionPoid) {
        log.info("Getting delivery order with transactionPoid: {}", transactionPoid);

        DeliveryOrderIssueToCustomerDto dto = viewRepository.findByTransactionPoid(transactionPoid, getCompanyPoid())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Order", "transactionPoid", transactionPoid.toString()));

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

        if (request.getDeliverySentTo() == null || request.getDeliverySentTo().trim().isEmpty()) {
            throw new ValidationException("Delivery sent to is required");
        }

        validateEmailConfiguration(request);

        ShipBlManifestHDR blManifest = blManifestRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("BL Manifest", "transactionPoid", transactionPoid.toString()));

        if ("Y".equals(blManifest.getDeleted())) {
            throw new ResourceNotFoundException("BL Manifest", "transactionPoid", transactionPoid.toString());
        }

        blManifest.setLastModifiedBy(username);
        blManifest.setLastModifiedDate(LocalDateTime.now());
        blManifestRepository.save(blManifest);
    }

    @Override
    @Transactional
    public DeliveryOrderIssueToCustomerDto updateDeliveryOrder(Long transactionPoid, UpdateDeliveryOrderRequestDto request) {

        log.info("Updating delivery order for BL transaction: {}", transactionPoid);

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        String username = getUserName();

        ShipBlManifestHDR blManifest = blManifestRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("BL Manifest", "transactionPoid", transactionPoid.toString()));

        if ("Y".equals(blManifest.getDeleted())) {
            throw new ResourceNotFoundException("BL Manifest", "transactionPoid", transactionPoid.toString());
        }

        if (StringUtils.isNotBlank(request.getDoReleasedIdPerson())) {
            blManifest.setRelasedIdPerson(request.getDoReleasedIdPerson());
        }
        if (StringUtils.isNotBlank(request.getDoReleasedToPerson())) {
            blManifest.setRelasedToPerson(request.getDoReleasedToPerson());
        }
        if (StringUtils.isNotBlank(request.getDoReleasedAddressPerson())) {
            blManifest.setRelasedAddrsPerson(request.getDoReleasedAddressPerson());
        }

        if (StringUtils.isNotBlank(request.getDoPriority())) {
            blManifest.setDoPriority(request.getDoPriority());
        }
        if (StringUtils.isNotBlank(request.getDeliverySentTo())) {
            blManifest.setDeliverySentTo(request.getDeliverySentTo());
        }
        if (StringUtils.isNotBlank(request.getPrincipalDoNumber())) {
            blManifest.setPrincipalDoNumber(request.getPrincipalDoNumber());
        }
        if (StringUtils.isNotBlank(request.getDoCntToConsignee())) {
            blManifest.setDoCntToConsignee(request.getDoCntToConsignee());
        }
        if (StringUtils.isNotBlank(request.getDoCntToNotify())) {
            blManifest.setDoCntToNotify(request.getDoCntToNotify());
        }
        if (StringUtils.isNotBlank(request.getDoCntToOthers())) {
            blManifest.setDoCntToOthers(request.getDoCntToOthers());
        }
        if (StringUtils.isNotBlank(request.getDoCntToOthersMails())) {
            blManifest.setDoCntToOthersMails(request.getDoCntToOthersMails());
        }

        blManifest.setLastModifiedBy(username);
        blManifest.setLastModifiedDate(LocalDateTime.now());
        blManifestRepository.save(blManifest);

        return getDeliveryOrderIssueToCustomer(transactionPoid);
    }

    @Override
    public Map<String, byte[]> validateDocument(
            Long transactionPoid,
            IssueDeliveryOrderRequestDto request
    ) throws Exception {

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        String username = getUserName();

        if (request.getDeliverySentTo() == null || request.getDeliverySentTo().trim().isEmpty()) {
            throw new ValidationException("Delivery sent to is required");
        }

        validateEmailConfiguration(request);

        callProcShipDoCntPrintAfter(
                groupPoid, companyPoid, transactionPoid, null,
                "ARSHRCPTPRINTUPDATE", username,
                request.getDoReleasedIdPerson(),
                request.getDoReleasedToPerson(),
                request.getDoReleasedAddressPerson(),
                request.getOriginalBlReleaseCr(),
                request.getDoPriority(),
                request.getDoCntToConsignee(),
                request.getDoCntToNotify(),
                request.getDoCntToOthers(),
                request.getDoCntToOthersMails(),
                request.getEmailsDo(),
                request.getDeliverySentTo(),
                request.getPrincipalDoNumber()
        );

//        Map<String, JasperReport> printData = validateAndLoadPrintData(transactionPoid);
//        if (printData == null) {
//            return Map.of();
//        }

        callProcShipDoCntPrintAfterNotUpdate(
                groupPoid, companyPoid, transactionPoid, null,
                "ARSHRCPTPRINTUPDATE", username,
                request.getDoReleasedIdPerson(),
                request.getDoReleasedToPerson(),
                request.getDoReleasedAddressPerson(),
                request.getOriginalBlReleaseCr()
        );

        Map<String, byte[]> result = new LinkedHashMap<>();
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "100-414");
        params.put("P_TRAN_NO", transactionPoid);
      JasperReport mainReport =  printService.load("shipping/DO_SH.jrxml");
      printService.fillReportToPdf(mainReport,params,dataSource);




//        if (printData.containsKey("mainReport")) {
//            result.put(
//                    "DELIVERY_ORDER",
//                    printService.fillReportToPdf(
//                            printData.get("mainReport"),
//                            params,
//                            dataSource
//                    )
//            );
//        }

//        if (printData.containsKey("containerReport")) {
//            result.put(
//                    "CONTAINER_DELIVERY",
//                    printService.fillReportToPdf(
//                           printData.get("containerReport"),
//                            params,
//                            dataSource
//                    )
//            );
//        }
//
//        if (printData.containsKey("returnReport")) {
//            result.put(
//                    "CONTAINER_RETURN",
//                    printService.fillReportToPdf(
//                             printData.get("returnReport"),
//                            params,
//                            dataSource
//                    )
//            );
//        }

        return result;
    }


    private  JasperReport validateAndLoadPrintData(Long transactionPoid) throws JRException {
        Map<String, JasperReport> result = new HashMap<>();

//        String doPrintCheck = checkPrintDocumentData(transactionPoid, "DO");
//        String dlvCntPrintCheck = checkPrintDocumentData(transactionPoid, "DLVCNT");
//        String rtnCntPrintCheck = checkPrintDocumentData(transactionPoid, "RTNCNT");
//
//        log.info("Print validation for transactionPoid {}: DO={}, DLVCNT={}, RTNCNT={}",
//                  transactionPoid, doPrintCheck, dlvCntPrintCheck, rtnCntPrintCheck);

//        if ("Y".equalsIgnoreCase(doPrintCheck) &&
//            "N".equalsIgnoreCase(viewRepository.printDocumentAlreadyPrinted("DO", transactionPoid))) {
            log.info("Loaded mainReport for transactionPoid {}", transactionPoid);
//        }

//        if ("Y".equalsIgnoreCase(dlvCntPrintCheck) &&
//            "N".equalsIgnoreCase(viewRepository.printDocumentAlreadyPrinted("DLVCNT", transactionPoid))) {
//            String pLineCode = viewRepository.getPlineCode(transactionPoid);
//            JasperReport containerReport = "HANJN".equalsIgnoreCase(pLineCode) ?
//                    printService.load("shipping/Container_Delivery_ValidityHJS_Currently_not.jrxml") :
//                    printService.load("shipping/Container_Delivery_Validity.jrxml");
//            result.put("containerReport", containerReport);
//            log.info("Loaded containerReport for transactionPoid {}", transactionPoid);
//        }
//
//        if ("Y".equalsIgnoreCase(rtnCntPrintCheck) &&
//            "N".equalsIgnoreCase(viewRepository.printDocumentAlreadyPrinted("RTNCNT", transactionPoid))) {
//            result.put("returnReport", printService.load("shipping/Container_Return_Validity.jrxml"));
//            log.info("Loaded returnReport for transactionPoid {}", transactionPoid);
//        }

//        boolean hasReports = result.containsKey("mainReport") || result.containsKey("containerReport") || result.containsKey("returnReport");
//        log.info("validateAndLoadPrintData result for transactionPoid {}: hasReports={}", transactionPoid, hasReports);
//        return hasReports ? result : null;
        return null;
    }

    private void validateEmailConfiguration(IssueDeliveryOrderRequestDto request) {
        if (request.getEmailsAdditional() != null &&
                (request.getDoCntToOthers() == null || !"Y".equals(request.getDoCntToOthers()))) {
            throw new ValidationException("Select additional emails check box when providing additional emails");
        }

        if (request.getEmailsAdditional() != null && !request.getEmailsAdditional().trim().isEmpty()) {
            if (request.getEmailsAdditional().length() <= 5) {
                throw new ValidationException("Check additional emails value - must be longer than 5 characters");
            }
            if (!request.getEmailsAdditional().contains("@")) {
                throw new ValidationException("Check additional emails value - must contain @ symbol");
            }
        }

        if (request.getEmailsDo() == null || request.getEmailsDo().trim().isEmpty()) {
            throw new ValidationException("Delivery emails not added for customer");
        }

        if ("Y".equals(request.getDoCntToOthers()) && StringUtils.isNotBlank(request.getEmailsDo())) {
            if (request.getEmailsAdditional() == null || request.getEmailsAdditional().trim().isEmpty()) {
                throw new ValidationException("Additional emails need to be added when others is selected");
            }
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

                    cs.setString(5, StringUtils.defaultIfBlank(actionType, "ARSHRCPTPRINTUPDATE"));
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

                    cs.setString(5, StringUtils.defaultIfBlank(actionType, "ARSHRCPTPRINTUPDATE"));
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
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Character) {
            return String.valueOf(value);
        }
        return value.toString();
    }
}
