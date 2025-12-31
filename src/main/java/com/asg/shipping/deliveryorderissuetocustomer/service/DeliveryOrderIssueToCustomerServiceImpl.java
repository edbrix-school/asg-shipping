package com.asg.shipping.deliveryorderissuetocustomer.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.deliveryorderissuetocustomer.dto.DeliveryOrderIssueToCustomerDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.IssueDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.UpdateDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.entity.ShipBlManifestHDR;
import com.asg.shipping.deliveryorderissuetocustomer.repository.DeliveryOrderIssueToCustomerRepository;
import com.asg.shipping.deliveryorderissuetocustomer.repository.ShipBlManifestHDRRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final DocumentSearchService documentService;
    private final LovDataService lovService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listDeliveryOrderIssueToCustomer(String docId, FilterRequestDto request, Pageable pageable) {
        log.info("Searching pending delivery orders with docId: {}, page: {}, size: {}", docId, pageable.getPageNumber(), pageable.getPageSize());

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        List<FilterDto> filters = documentService.resolveFilters(request);

        RawSearchResult raw = documentService.search(docId, filters, operator, pageable, isDeleted, "BL_NUMBER", "TRANSACTION_POID");

        Page<Map<String, Object>> page = new PageImpl<>(raw.records(), pageable, raw.totalRecords());

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

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

        // Validate delivery sent to
        if (request.getDeliverySentTo() == null || request.getDeliverySentTo().trim().isEmpty()) {
            throw new ValidationException("Delivery sent to is required");
        }

        // Validate email configuration
        validateEmailConfiguration(request);

        // Get BL manifest header
        ShipBlManifestHDR blManifest = blManifestRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("BL Manifest", "transactionPoid", transactionPoid.toString()));

        if ("Y".equals(blManifest.getDeleted())) {
            throw new ResourceNotFoundException("BL Manifest", "transactionPoid", transactionPoid.toString());
        }

        // NOTE:
// DO-related fields are updated inside PROC_SHIP_DO_CNT_PRINT_AFTER.
// Do NOT update SHIP_BL_MANIFEST_HDR here to avoid duplicate updates.

//        updateBlManifestWithDoFields(blManifest, request);
        blManifest.setLastModifiedBy(username);
        blManifest.setLastModifiedDate(LocalDateTime.now());
        blManifestRepository.save(blManifest);

        // Call PROC_SHIP_DO_CNT_PRINT_AFTER with 18 parameters
        callProcShipDoCntPrintAfter(groupPoid, companyPoid, transactionPoid, null,
                "ARSHRCPTPRINTUPDATE", username, request.getDoReleasedIdPerson(),
                request.getDoReleasedToPerson(), request.getDoReleasedAddressPerson(),
                request.getOriginalBlReleaseCr(), request.getDoPriority(), request.getDoCntToConsignee(),
                request.getDoCntToNotify(), request.getDoCntToOthers(), request.getDoCntToOthersMails(),
                request.getEmailsDo(), request.getDeliverySentTo(), request.getPrincipalDoNumber());

        // Check if auto-print is enabled (simplified - would need parameter service)
        // For now, we'll skip auto-print and call the second stored procedure call
        // In real implementation, check system parameter START_DO_CNT_DIRECT_CUST

        // Call PROC_SHIP_DO_CNT_PRINT_AFTER again with 11 parameters (NOT_UPDATE)
        callProcShipDoCntPrintAfterNotUpdate(groupPoid, companyPoid, transactionPoid, null, "ARSHRCPTPRINTUPDATE",
                username, request.getDoReleasedIdPerson(), request.getDoReleasedToPerson(),
                request.getDoReleasedAddressPerson(), request.getOriginalBlReleaseCr()
        );
    }

    @Override
    @Transactional
    public DeliveryOrderIssueToCustomerDto updateDeliveryOrder(Long transactionPoid, UpdateDeliveryOrderRequestDto request) {

        log.info("Updating delivery order for BL transaction: {}", transactionPoid);

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        String username = getUserName();

        // Get BL manifest header
        ShipBlManifestHDR blManifest = blManifestRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(transactionPoid, groupPoid, companyPoid)
                .orElseThrow(() -> new ResourceNotFoundException("BL Manifest", "transactionPoid", transactionPoid.toString()));

        if ("Y".equals(blManifest.getDeleted())) {
            throw new ResourceNotFoundException("BL Manifest", "transactionPoid", transactionPoid.toString());
        }

        // Update only provided & non-blank fields
        if (StringUtils.isNotBlank(request.getDoReleasedIdPerson())) {
            blManifest.setRelasedIdPerson(request.getDoReleasedIdPerson());
        }
        if (StringUtils.isNotBlank(request.getDoReleasedToPerson())) {
            blManifest.setRelasedToPerson(request.getDoReleasedToPerson());
        }
        if (StringUtils.isNotBlank(request.getDoReleasedAddressPerson())) {
            blManifest.setRelasedAddrsPerson(request.getDoReleasedAddressPerson());
        }

        // Note: ORIGINAL_BL_RELEASE_CR is in AR_SH_SALES_INVOICE_HDR, not SHIP_BL_MANIFEST_HDR
        // The stored procedure will handle updating it

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

    /**
     * Update BL manifest header with DO-related fields
     */
    private void updateBlManifestWithDoFields(ShipBlManifestHDR blManifest, IssueDeliveryOrderRequestDto request) {

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
    }

    /**
     * Validate email configuration
     */
    private void validateEmailConfiguration(IssueDeliveryOrderRequestDto request) {
        // Validate additional emails if others is selected
        if (request.getEmailsAdditional() != null &&
                (request.getDoCntToOthers() == null || !"Y".equals(request.getDoCntToOthers()))) {
            throw new ValidationException("Select additional emails check box when providing additional emails");
        }

        // Validate additional emails format if provided
        if (request.getEmailsAdditional() != null && !request.getEmailsAdditional().trim().isEmpty()) {
            if (request.getEmailsAdditional().length() <= 5) {
                throw new ValidationException("Check additional emails value - must be longer than 5 characters");
            }
            if (!request.getEmailsAdditional().contains("@")) {
                throw new ValidationException("Check additional emails value - must contain @ symbol");
            }
        }

        // Validate DO emails are provided
        if (request.getEmailsDo() == null || request.getEmailsDo().trim().isEmpty()) {
            throw new ValidationException("Delivery emails not added for customer");
        }

        // If others is selected and DO emails are provided, additional emails must be provided
        if ("Y".equals(request.getDoCntToOthers()) && StringUtils.isNotBlank(request.getEmailsDo())) {
            if (request.getEmailsAdditional() == null || request.getEmailsAdditional().trim().isEmpty()) {
                throw new ValidationException("Additional emails need to be added when others is selected");
            }
        }
    }

    /**
     * Call PROC_SHIP_DO_CNT_PRINT_AFTER stored procedure with 18 parameters
     */
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

                    // Business default = 'C'
                    cs.setString(17, StringUtils.defaultIfBlank(deliverySentTo, "C"));

                    cs.setString(18, StringUtils.defaultIfBlank(principalDoNumber, null));

                    cs.execute();
                }
                return null;
            });

            log.debug("Successfully called PROC_SHIP_DO_CNT_PRINT_AFTER (18 params) for BL transaction: {}", blPoid);

        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_DO_CNT_PRINT_AFTER (18 params) for BL transaction: {}", blPoid, e);
            throw new ValidationException("Error processing delivery order: " + e.getMessage());
        }
    }

    /**
     * Call PROC_SHIP_DO_CNT_PRINT_AFTER stored procedure with 11 parameters (NOT_UPDATE)
     */
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

                    // P_DO_PRIORITY = 'NOT_UPDATE'
                    cs.setString(11, "NOT_UPDATE");

                    cs.execute();
                }
                return null;
            });

            log.debug("Successfully called PROC_SHIP_DO_CNT_PRINT_AFTER (11 params) for BL transaction: {}", blPoid);

        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_DO_CNT_PRINT_AFTER (11 params) for BL transaction: {}", blPoid, e);
            throw new ValidationException("Error processing delivery order: " + e.getMessage());
        }
    }

    /**
     * Enrich DTO with LOV data
     */
    private void enrichWithLovData(DeliveryOrderIssueToCustomerDto dto) {
        dto.setDoPriorityDet(lovService.getDetailsByCodeAndLovName(dto.getDoPriority(), "DO_PRIORITY_SH"));
        dto.setDoIssueAuthPoidDet(lovService.getDetailsByPoidAndLovName(dto.getDoIssueAuthPoid(), "USER_MASTER"));
        dto.setDeliverySentToDet(lovService.getDetailsByCodeAndLovName(dto.getDeliverySentTo(), "DELIVERY_SENT_TO"));
    }
}