package com.asg.shipping.salesinvoice.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.salesinvoice.dto.*;
import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceChargDtl;
import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceContnrDtl;
import com.asg.shipping.salesinvoice.entity.ArShSalesInvoiceHdr;
import com.asg.shipping.salesinvoice.repository.ArShSalesInvoiceChargDtlRepository;
import com.asg.shipping.salesinvoice.repository.ArShSalesInvoiceContnrDtlRepository;
import com.asg.shipping.salesinvoice.repository.ArShSalesInvoiceHdrRepository;
import com.asg.shipping.salesinvoice.util.SalesInvoiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import oracle.jdbc.OracleTypes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.asg.common.lib.security.util.UserContext.*;
import static com.asg.common.lib.utility.ASGHelperUtils.getCurrentUser;


/**
 * Service implementation for Sales Invoice Shipping operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalesInvoiceShippingServiceImpl implements SalesInvoiceShippingService {

    private final ArShSalesInvoiceHdrRepository hdrRepository;
    private final ArShSalesInvoiceContnrDtlRepository contnrDtlRepository;
    private final ArShSalesInvoiceChargDtlRepository chargDtlRepository;
    private final DocumentSearchService documentService;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final PrintService printService;
    private final DocumentDeleteService documentDeleteService;
    
    // Container quantity tracking variables for demurrage calculations
    private BigDecimal totalQtyValidate20 = BigDecimal.ZERO;
    private BigDecimal totalQtyValidate40 = BigDecimal.ZERO;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchSalesInvoice(String docId, FilterRequestDto request, Pageable pageable, String startDate, String endDate) {
        log.info("Searching Sales Invoice with docId: {}, page: {}, size: {}, startDate: {}, endDate: {}", docId, pageable.getPageNumber(), pageable.getPageSize(), startDate, endDate);

        String operator = documentService.resolveOperator(request);
        String isDeleted = documentService.resolveIsDeleted(request);
        
        // Parse date strings to LocalDate if provided
        LocalDate startDateValue = null;
        LocalDate endDateValue = null;
        
        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                startDateValue = LocalDate.parse(startDate.trim());
            } catch (Exception e) {
                log.warn("Invalid start date format: {}", startDate, e);
            }
        }
        
        if (endDate != null && !endDate.trim().isEmpty()) {
            try {
                endDateValue = LocalDate.parse(endDate.trim());
            } catch (Exception e) {
                log.warn("Invalid end date format: {}", endDate, e);
            }
        }
        
        // Use the proper resolveDateFilters method for date filtering
        List<FilterDto> filters = documentService.resolveDateFilters(
                request,
                "INV_DATE",  // date field for filtering
                startDateValue,
                endDateValue
        );

        RawSearchResult raw = documentService.search(
                docId,
                filters,
                operator,
                pageable,
                isDeleted,
                "DOC_REF",
                "TRANSACTION_POID"
        );

        Page<Map<String, Object>> page = new PageImpl<>(
                raw.records(),
                pageable,
                raw.totalRecords()
        );

        return PaginationUtil.wrapPage(page, raw.displayFields());
    }

    @Override
    @Transactional(readOnly = true)
    public SalesInvoiceShippingDto getSalesInvoice(Long id) {
        log.info("Getting Sales Invoice with id: {}", id);

        ArShSalesInvoiceHdr entity = hdrRepository.findActiveByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Invoice", "transactionPoid", id.toString()));

        SalesInvoiceShippingDto dto = SalesInvoiceMapper.mapToDto(entity);

        loadDetailTables(dto, id);
        
        // Add manifest details if BL POID is available
        if (dto.getBlPoid() != null) {
            Map<String, Object> manifestDetails = getManifestDetails(dto.getBlPoid());
            dto.setManifestDetails(manifestDetails);
        }
//        enrichLovData(dto);

        log.info("Successfully retrieved Sales Invoice with id: {}", id);
        return dto;
    }

    @Override
    @Transactional
    public SalesInvoiceShippingDto createSalesInvoice(SalesInvoiceShippingCreateDTO createDTO) {
        log.info("Creating Sales Invoice");

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();

        validateCreateDTO(createDTO, companyPoid);

        ArShSalesInvoiceHdr entity = new ArShSalesInvoiceHdr();
        SalesInvoiceMapper.mapCreateDTOToEntity(createDTO, entity, groupPoid, companyPoid);

        autoPopulateDefaults(entity, companyPoid);

        String docRef = generateDocRef(entity, companyPoid);
        entity.setDocRef(docRef);

        var saved = new ArShSalesInvoiceHdr();

        saved = hdrRepository.saveAndFlush(entity);
        saveDetailTables(createDTO, saved.getTransactionPoid());

        BigDecimal invAmount = calculateInvoiceAmount(saved.getTransactionPoid());
        saved.setInvAmount(invAmount);

        saved = hdrRepository.saveAndFlush(saved);
        
        // Re-fetch the entity to get the actual database values after trigger execution
        saved = hdrRepository.findById(saved.getTransactionPoid()).orElse(saved);

        callProcShipBlPageSaveAfter(groupPoid, companyPoid, saved.getTransactionPoid(), "INVSHRCPTPRINTUPDATE");

        SalesInvoiceShippingDto result = SalesInvoiceMapper.mapToDto(saved);
        loadDetailTables(result, saved.getTransactionPoid());
//        enrichLovData(result);

        log.info("Successfully created Sales Invoice with id: {}", saved.getTransactionPoid());
        return result;
    }

    @Override
    @Transactional
    public SalesInvoiceShippingDto updateSalesInvoice(Long id, SalesInvoiceShippingUpdateDTO updateDTO) {
        log.info("Updating Sales Invoice with id: {}", id);

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();

        ArShSalesInvoiceHdr entity = hdrRepository.findActiveByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Invoice", "transactionPoid", id.toString()));

        validateUpdateDTO(updateDTO, id, companyPoid);

        SalesInvoiceMapper.mapUpdateDTOToEntity(updateDTO, entity);
        autoPopulateDefaults(entity, companyPoid);

        ArShSalesInvoiceHdr saved = hdrRepository.saveAndFlush(entity);

        contnrDtlRepository.deleteByTransactionPoid(id);
        chargDtlRepository.deleteByTransactionPoid(id);

        saveDetailTables(updateDTO, id);

        BigDecimal invAmount = calculateInvoiceAmount(id);
        saved.setInvAmount(invAmount);
        saved = hdrRepository.saveAndFlush(saved);
        
        // Re-fetch the entity to get the actual database values after trigger execution
        saved = hdrRepository.findById(saved.getTransactionPoid()).orElse(saved);

        callProcShipBlPageSaveAfter(groupPoid, companyPoid, saved.getTransactionPoid(), "INVSHRCPTPRINTUPDATE");

        SalesInvoiceShippingDto result = SalesInvoiceMapper.mapToDto(saved);
        loadDetailTables(result, id);
//        enrichLovData(result);

        log.info("Successfully updated Sales Invoice with id: {}", id);
        return result;
    }

    @Override
    @Transactional
    public void deleteSalesInvoice(Long id, DeleteReasonDto deleteReasonDto) {
        log.info("Deleting Sales Invoice with id: {}", id);

        ArShSalesInvoiceHdr entity = hdrRepository.findActiveByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Invoice", "transactionPoid", id.toString()));

        if ("Y".equals(entity.getDeleted())) {
            log.info("Sales Invoice with id: {} is already deleted", id);
            return;
        }

        documentDeleteService.deleteDocument(
                id,
                "AR_SH_SALES_INVOICE_HDR",
                "TRANSACTION_POID",
                deleteReasonDto,
                LocalDate.now()
        );

        log.info("Successfully deleted Sales Invoice with id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public LoadContainerDemurrageResponseDTO loadContainerDemurrageData(Long id, LoadContainerDemurrageRequestDTO request) {
        log.info("Loading container demurrage data for invoice id: {}, BL POID: {}", id, request.getBlPoid());

        ArShSalesInvoiceHdr invoice = new ArShSalesInvoiceHdr();
        invoice.setTransactionPoid(-999L);
        if(!id.equals(-999L)) {
            invoice = hdrRepository.findActiveByTransactionPoid(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Sales Invoice", "transactionPoid", id.toString()));
        }

        List<SalesInvoiceContainerDtlDto> containers = executeLoadContainerDemurrageQuery(
                request.getBlPoid(),
                invoice.getTransactionPoid(),
                request.getBlTypeInvoice()
        );

        return LoadContainerDemurrageResponseDTO.builder()
                .containers(containers)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LoadChargeDataResponseDTO loadChargeData(Long id, LoadChargeDataRequestDTO request) {
        log.info("Loading charge data for invoice id: {}, BL POID: {}, BL Type: {}", id, request.getBlPoid(), request.getBlTypeInvoice());

        Long companyPoid = getCompanyPoid();
        ArShSalesInvoiceHdr invoice = new ArShSalesInvoiceHdr();
        invoice.setTransactionPoid(-999L);
        if(!id.equals(-999L)) {
            invoice = hdrRepository.findActiveByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Invoice", "transactionPoid", id.toString()));
        }
        

        List<SalesInvoiceChargesDtlDto> charges = executeLoadChargeDataQuery(
                request.getBlPoid(),
                request.getBlTypeInvoice(),
                invoice.getTransactionPoid()
        );

        // Load container demurrage data first
        List<SalesInvoiceContainerDtlDto> containerDemurrageData = executeLoadContainerDemurrageQuery(
                request.getBlPoid(),
                invoice.getTransactionPoid(),
                request.getBlTypeInvoice()
        );

        // Calculate demurrage amount and track container quantities based on actual container data
        BigDecimal demurrageAmount = createDemurrageDettention(containerDemurrageData, request.getBlPoid());

        // Load demurrage/detention charge with tax information
        List<SalesInvoiceChargesDtlDto> demurrageCharges = loadDemurrageCharges(
                demurrageAmount,
                request.getBlTypeInvoice(),
                companyPoid
        );
        if (!demurrageCharges.isEmpty()) {
            charges.addAll(demurrageCharges);
        }

        List<SalesInvoiceChargesDtlDto> lateCharges = loadLateCollectionCharges(
                request.getBlPoid(),
                request.getBlTypeInvoice(),
                companyPoid
        );

        return LoadChargeDataResponseDTO.builder()
                .charges(charges)
                .demurrageAmount(demurrageAmount)
                .lateCharges(lateCharges)
                .containers(containerDemurrageData)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateCustomerResponseDTO validateCustomer(ValidateCustomerRequestDTO request) {
        log.info("Validating customer: {}", request.getCustomerPoid());

        String validationResult = callProcValidateCustomer(
                request.getCustomerPoid(),
                "BOTH",
                request.getAuthorizedId()
        );

        ValidateCustomerResponseDTO response = ValidateCustomerResponseDTO.builder()
                .valid(false)
                .warnings(new ArrayList<>())
                .build();

        if (validationResult == null || "NO_DATA".equals(validationResult) || "ERROR".equals(validationResult)) {
            response.setValid(false);
            response.setErrorMessage(validationResult);
            return response;
        }

        if (validationResult.contains("CREDIT_LIMIT_OVER")) {
            response.getWarnings().add("Customer credit limit exceeded");
            response.setValid(false);
            response.setErrorMessage("CREDIT_LIMIT_OVER");
        } else if (validationResult.contains("CONTRACT_EXPIRY")) {
            response.getWarnings().add("Customer contract expired");
            response.setValid(false);
            response.setErrorMessage("CONTRACT_EXPIRY");
        } else if (validationResult.contains("BLOCKED_CUSTOMER")) {
            response.getWarnings().add("Customer is blocked");
            response.setValid(false);
            response.setErrorMessage("BLOCKED_CUSTOMER");
        } else if (validationResult.contains("DELETED_CUSTOMER")) {
            response.getWarnings().add("Customer is deleted");
            response.setValid(false);
            response.setErrorMessage("DELETED_CUSTOMER");
        } else if (validationResult.contains("NO_GL_POID")) {
            response.getWarnings().add("Customer GL is not present");
            response.setValid(false);
            response.setErrorMessage("NO_GL_POID");
        } else {
            response.setValid(true);
            Integer creditDays = callProcGetCustomerCreditDays(request.getCustomerPoid(), request.getCreditType());
            response.setCreditDays(creditDays);
        }

        return response;
    }

    @Override
    @Transactional
    public void verifyInvoice(Long id) {
        log.info("Verifying invoice: {}", id);

        callProcInvoiceVerify(id, getDocumentId(), getUserId());

        log.info("Successfully verified invoice: {}", id);
    }


    @Override
    public byte[] print(Long transactionPoid, Long blPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "300-102");
        params.put("DOC_BL_POID", blPoid);
        JasperReport mainReport = printService.load("Shipping/SH/SH_INVOICE_IMP_EXP.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);
    }


    @Override
    @Transactional
    public CreateFFJobResponseDTO createFFJob(Long id, Long blPoid) {
        log.info("Creating FF job for invoice: {}, BL POID: {}", id, blPoid);

        Long groupPoid = getGroupPoid();
        String username = getCurrentUser();

        Map<String, Object> result = callProcFfJobCreateFromSh(groupPoid, id, blPoid, username);

        Long ffJobPoid = result.get("ffJobPoid") != null ? ((Number) result.get("ffJobPoid")).longValue() : null;
        String status = (String) result.get("status");

        return CreateFFJobResponseDTO.builder()
                .ffJobPoid(ffJobPoid)
                .status(status != null ? status : "SUCCESS")
                .build();
    }

    @Override
    @Transactional
    public CreateFFPurchaseJournalResponseDTO createFFPurchaseJournal(Long id, Long ffJobPoid) {
        log.info("Creating FF purchase journal for invoice: {}, FF Job POID: {}", id, ffJobPoid);

        Long groupPoid = getGroupPoid();
        String username = getCurrentUser();

        Map<String, Object> result = callProcFfToShPurchaseJou(groupPoid, id, ffJobPoid, username);

        String ffPjNo = (String) result.get("ffPjNo");
        String ffJobNo = (String) result.get("ffJobNo");
        String status = (String) result.get("status");

        return CreateFFPurchaseJournalResponseDTO.builder()
                .ffPjNo(ffPjNo)
                .ffJobNo(ffJobNo)
                .status(status != null ? status : "SUCCESS")
                .build();
    }

    @Override
    @Transactional
    public String updateBookingParty(Long id, UpdateBookingPartyRequestDTO request) {
        log.info("Updating booking party for invoice: {}, BL POID: {}, Customer POID: {}", id, request.getBlPoid(), request.getCustomerPoid());

        Long userPoid = getUserPoid();
        String userPoidStr = userPoid != null ? userPoid.toString() : getCurrentUser();

        var result = callProcShipUpdateBooking(request.getBlPoid(), request.getCustomerPoid(), userPoidStr);

        log.info("Successfully updated booking party");
        return result.substring("INFO:".length()).trim();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerAddressResponseDTO getCustomerAddress(Long addressMasterPoid, String addressType) {
        log.info("Getting customer address for addressMasterPoid: {}, addressType: {}", addressMasterPoid, addressType);

        Long userPoid = getUserPoid();

        Map<String, Object> result = callProcGetInvCustAddressV2(userPoid, addressMasterPoid, addressType);

        return CustomerAddressResponseDTO.builder()
                .contactPerson((String) result.get("contactPerson"))
                .email1((String) result.get("email1"))
                .mobile((String) result.get("mobile"))
                .build();
    }

    @Override
    @Transactional
    public LoadBlDataResponseDTO loadBlData(Long id, LoadBlDataRequestDTO request) {
        log.info("Loading BL data for invoice: {}, BL POID: {}", id, request.getLovName());

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();

        var procResult = callProcLovAfterBrws300103(groupPoid, companyPoid, userPoid, getDocumentId(), request.getTransactionPoid(), request.getLovName(), String.valueOf(id));

        var result = LoadBlDataResponseDTO.builder()
                .companyPoid(procResult.get("companyPoid"))
                .customerPoid(procResult.get("customerPoid"))
                .bookingPartyPoid(procResult.get("bookingPartyPoid"))
                .creditDays(procResult.get("creditDays"))
                .invDate(procResult.get("invDate"))
                .ownInvoiceNo(procResult.get("ownInvoiceNo"))
                .blTypeInvoice(procResult.get("blTypeInvoice"))
                .build();

        log.info("Successfully loaded BL data");
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public GetBillCompanyResponseDTO getBillCompany(GetBillCompanyRequestDTO request) {
        log.info("Getting bill company for BL POID: {}, Customer POID: {}", request.getBlPoid(), request.getCustomerPoid());

        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();

        try {
            String sql = "{call PROC_SH_GET_CUS_BILL_COMPANY(?, ?, ?, ?, ?, ?)}";
            String billCompanyPoidStr = jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, userPoid);
                cs.setLong(4, request.getBlPoid());
                cs.setLong(5, request.getCustomerPoid());
                cs.registerOutParameter(6, Types.VARCHAR);
                cs.execute();
                return cs.getString(6);
            });

            Long billCompanyPoid = null;
            if (billCompanyPoidStr != null && !billCompanyPoidStr.trim().isEmpty()) {
                try {
                    billCompanyPoid = Long.parseLong(billCompanyPoidStr.trim());
                } catch (NumberFormatException e) {
                    log.warn("Invalid bill company POID format: {}", billCompanyPoidStr);
                }
            }

            GetBillCompanyResponseDTO response = GetBillCompanyResponseDTO.builder()
                    .billCompanyPoid(billCompanyPoid)
                    .build();

            if (billCompanyPoid != null) {
                enrichCompanyFields(billCompanyPoid,
                        response::setBillCompanyName,
                        response::setBillCompanyCode);
            }

            log.info("Successfully retrieved bill company POID: {} for BL POID: {}, Customer POID: {}",
                    billCompanyPoid, request.getBlPoid(), request.getCustomerPoid());

            return response;
        } catch (Exception e) {
            log.error("Error calling PROC_SH_GET_CUS_BILL_COMPANY", e);
            throw new ValidationException("Error getting bill company: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LoadPrintDataResponseDTO loadPrintData(Long id, LoadPrintDataRequestDTO request) {
        log.info("Loading print data for invoice id: {}, customer POID: {}", id, request.getCustomerPoid());

        ArShSalesInvoiceHdr invoice = hdrRepository.findActiveByTransactionPoid(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Invoice", "transactionPoid", id.toString()));

        Long blPoid = invoice.getBlPoid();
        if (blPoid == null) {
            throw new ValidationException("BL POID is required for loading print data");
        }

        try {
            String sql = "{call PROC_SHIP_BL_PRINT_LOAD(?, ?, ?)}";
            List<InvoicePrintDetailDto> printDetails = jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, request.getCustomerPoid());
                cs.setLong(2, id);
                cs.registerOutParameter(3, Types.REF_CURSOR);
                cs.execute();

                ResultSet rs = (ResultSet) cs.getObject(3);
                if (rs == null || !rs.next()) {
                    log.info("No custom print data found for customer POID: {}", request.getCustomerPoid());
                    return new ArrayList<>();
                }

                log.info("Result Set : {}", rs);
                List<InvoicePrintDetailDto> details = new ArrayList<>();
                int detRowId = 0;
                do {
                    detRowId++;
                    InvoicePrintDetailDto detail = InvoicePrintDetailDto.builder()
                            .detRowId((long) detRowId)
                            .blPoid(blPoid)
                            .lineChargeDescription(rs.getString("LINE_CHARGE_DESCRIPTION"))
                            .quantity(rs.getString("QUANTITY"))
                            .orgCurrencyCode(rs.getString("ORG_CURRENCY_CODE"))
                            .orgCurrencyExchange(getBigDecimalOrNull(rs, "ORG_CURRENCY_EXCHANGE"))
                            .currencyCode(rs.getString("CURRENCY_CODE"))
                            .currencyExchange(getBigDecimalOrNull(rs, "CURRENCY_EXCHANGE"))
                            .perQtySellAmt(getBigDecimalOrNull(rs, "PER_QTY_SELL_AMT"))
                            .perQtyUsdAmt(getBigDecimalOrNull(rs, "PER_QTY_USD_AMT"))
                            .totalUsd(getBigDecimalOrNull(rs, "TOTAL_USD"))
                            .build();
                    log.info("Detail : {}", detail);
                    details.add(detail);
                } while (rs.next());
                rs.close();
                log.info("Details : {}", details);
                return details;
            });

            String message = printDetails.isEmpty()
                    ? "No custom print data found for customer"
                    : "Print data loaded successfully";

            log.info("Successfully loaded {} print detail records for invoice id: {}", printDetails.size(), id);

            return LoadPrintDataResponseDTO.builder()
                    .printDetails(printDetails)
                    .message(message)
                    .build();
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_BL_PRINT_LOAD", e);
            throw new ValidationException("Error loading print data: " + e.getMessage());
        }
    }

    @Override
    public byte[] printInvoice(Long transactionPoid, Long blPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "300-102");
        params.put("DOC_BL_POID", blPoid);
        JasperReport mainReport = printService.load("Shipping/SH/SH_INVOICE_IMP_EXP_USD.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);

    }

    @Override
    public byte[] printCustomerAutoCharge(Long transactionPoid, Long blPoid) throws Exception {
        Map<String, Object> params = printService.buildBaseParams(transactionPoid, "300-102");
        params.put("DOC_BL_POID", blPoid);
        JasperReport mainReport = printService.load("Shipping/SH/SH_INVOICE_IMP_EXP_CUSTOMER.jrxml");
        return printService.fillReportToPdf(mainReport, params, dataSource);

    }

    private void enrichCompanyFields(Long companyPoid, Consumer<String> setName, Consumer<String> setCode) {
        try {
            String sql = "SELECT COMPANY_NAME, COMPANY_CODE " +
                    "FROM GLOBAL_COMPANY_MASTER WHERE COMPANY_POID = ?";
            jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                setName.accept(rs.getString("COMPANY_NAME"));
                setCode.accept(rs.getString("COMPANY_CODE"));
                return null;
            }, companyPoid);
        } catch (Exception e) {
            log.warn("Failed to fetch company fields for POID: {}", companyPoid, e);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Validate Create DTO
     */
    private void validateCreateDTO(SalesInvoiceShippingCreateDTO dto, Long companyPoid) {
        if (dto.getBlPoid() != null) {
            validateInvoiceDeleted(dto.getBlPoid(), dto.getInvDate(), "VALIDATE", null);
        }

        if (dto.getCustomerPoid() != null) {
            ValidateCustomerRequestDTO validateRequest = ValidateCustomerRequestDTO.builder()
                    .customerPoid(dto.getCustomerPoid())
                    .authorizedId("NONE")
                    .creditType("BOTH")
                    .build();
            ValidateCustomerResponseDTO validation = validateCustomer(validateRequest);
            if (!validation.getValid()) {
                log.error("Customer Validation Failed with {}", validation);
                throw new ValidationException(validation.getErrorMessage() != null ? validation.getErrorMessage() : "Customer validation failed");
            }
        }

        if (dto.getBlPoid() != null) {
            validateBlApproved(dto.getBlPoid());
        }

        if (dto.getChargesDetails() == null || dto.getChargesDetails().isEmpty()) {
            throw new ValidationException("Charge not select for invoice");
        }

        if (dto.getBlPoid() != null) {
            String blType = getBlTypeFromBlPoid(dto.getBlPoid());
            if (!"EXPORT".equalsIgnoreCase(blType)) {
                validateDemurrageTotals(dto);
            }
        }

        validateGainLoss(dto);

        validateImcoChargeCustomer(dto, companyPoid);
    }

    /**
     * Validate Update DTO
     */
    private void validateUpdateDTO(SalesInvoiceShippingUpdateDTO dto, Long id, Long companyPoid) {
        ArShSalesInvoiceHdr existing = hdrRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales Invoice", "transactionPoid", id.toString()));

        if (dto.getTransactionDate() != null && !dto.getTransactionDate().equals(existing.getTransactionDate())) {
            validateTransactionPeriod(companyPoid, dto.getTransactionDate());
        }

        if (dto.getCustomerPoid() != null && !dto.getCustomerPoid().equals(existing.getCustomerPoid())) {
            ValidateCustomerRequestDTO validateRequest = ValidateCustomerRequestDTO.builder()
                    .customerPoid(dto.getCustomerPoid())
                    .creditType("BOTH")
                    .build();
            ValidateCustomerResponseDTO validation = validateCustomer(validateRequest);
            if (!validation.getValid()) {
                throw new ValidationException(validation.getErrorMessage() != null ? validation.getErrorMessage() : "Customer validation failed");
            }
        }

        if (dto.getContainerDetails() != null || dto.getChargesDetails() != null) {
            if (!"EXPORT".equalsIgnoreCase(existing.getBlTypeInvoice())) {
                validateDemurrageTotalsForUpdate(dto, existing);
            }
        }
    }

    /**
     * Auto-populate defaults
     */
    private void autoPopulateDefaults(ArShSalesInvoiceHdr entity, Long companyPoid) {
        if (entity.getCustomerPoid() != null) {
            autoPopulateInvoiceTo(entity, companyPoid);
        }

        if ("AUTOCAN".equals(entity.getInvoiceType())) {
            entity.setVerifiedByAccount("Y");
        }

        if (entity.getInvDate() != null && entity.getCreditDays() != null && entity.getCreditDays() > 0) {
            entity.setDueDate(entity.getInvDate().plusDays(entity.getCreditDays()));
        } else if (entity.getCreditDays() != null && entity.getCreditDays() == 0) {
            entity.setDueDate(null);
        }
    }

    /**
     * Generate DOC_REF based on INV_DATE and INV_AMOUNT
     */
    private String generateDocRef(ArShSalesInvoiceHdr entity, Long companyPoid) {
        try {
            String companyCode = jdbcTemplate.queryForObject(
                    "SELECT GET_COMPANY_CODE(?) FROM DUAL",
                    String.class,
                    companyPoid
            );

            LocalDate invDate = entity.getInvDate();
            BigDecimal invAmount = entity.getInvAmount() != null ? entity.getInvAmount() : BigDecimal.ZERO;

            if (invDate == null) {
                throw new ValidationException("Invoice date is required to generate DOC_REF");
            }

            String prefix = companyCode != null ? companyCode : "DEFAULT";
            String docRef;

            if (invDate.isBefore(LocalDate.of(2019, 1, 1))) {
                docRef = jdbcTemplate.queryForObject(
                        "SELECT RTN_GLOBAL_SEQ_NO('SHINV', ?, NULL) FROM DUAL",
                        String.class,
                        prefix + "SH"
                );
                if (invAmount.compareTo(BigDecimal.ZERO) < 0) {
                    docRef = docRef != null ? docRef.replace("SH", "SHCR") : docRef;
                }
            } else {
                String year = String.valueOf(invDate.getYear()).substring(2);
                if (invAmount.compareTo(BigDecimal.ZERO) > 0) {
                    docRef = jdbcTemplate.queryForObject(
                            "SELECT RTN_GLOBAL_SEQ_NO('SHINV', ?, 5) FROM DUAL",
                            String.class,
                            prefix + "SH" + year
                    );
                } else {
                    docRef = jdbcTemplate.queryForObject(
                            "SELECT RTN_GLOBAL_SEQ_NO('SHINV', ?, 5) FROM DUAL",
                            String.class,
                            prefix + "SHCR" + year
                    );
                }
            }

            return docRef != null ? docRef : "SHINV-" + System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Error generating DOC_REF for companyPoid: {}", companyPoid, e);
            return "SHINV-" + System.currentTimeMillis();
        }
    }

    /**
     * Load detail tables into DTO
     */
    private void loadDetailTables(SalesInvoiceShippingDto dto, Long transactionPoid) {
        List<ArShSalesInvoiceContnrDtl> containerDetails = contnrDtlRepository.findByTransactionPoid(transactionPoid);
        dto.setContainerDetails(containerDetails.stream()
                .map(SalesInvoiceMapper::mapContainerDtlToDto)
                .collect(Collectors.toList()));

        List<ArShSalesInvoiceChargDtl> chargesDetails = chargDtlRepository.findByTransactionPoid(transactionPoid);
        dto.setChargesDetails(chargesDetails.stream()
                .map(SalesInvoiceMapper::mapChargesDtlToDto)
                .collect(Collectors.toList()));
    }

    /**
     * Save detail tables from Create/Update DTO
     */
    private void saveDetailTables(SalesInvoiceShippingCreateDTO dto, Long transactionPoid) {
        if (dto.getContainerDetails() != null && !dto.getContainerDetails().isEmpty()) {
            Long maxDetRowId = contnrDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
            maxDetRowId = maxDetRowId != null ? maxDetRowId : 0L;
            for (SalesInvoiceContainerDtlDto detailDto : dto.getContainerDetails()) {
                ArShSalesInvoiceContnrDtl entity = SalesInvoiceMapper.mapContainerDtlFromDto(detailDto, transactionPoid);
                if (entity.getDetRowId() == null) {
                    entity.setDetRowId(++maxDetRowId);
                }
                contnrDtlRepository.saveAndFlush(entity);
            }
        }

        if (dto.getChargesDetails() != null && !dto.getChargesDetails().isEmpty()) {
            Long maxDetRowId = chargDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
            maxDetRowId = maxDetRowId != null ? maxDetRowId : 0L;
            for (SalesInvoiceChargesDtlDto detailDto : dto.getChargesDetails()) {
                ArShSalesInvoiceChargDtl entity = SalesInvoiceMapper.mapChargesDtlFromDto(detailDto, transactionPoid);
                if (entity.getDetRowId() == null) {
                    entity.setDetRowId(++maxDetRowId);
                }
                var savedEntity = hdrRepository.findById(transactionPoid);
                chargDtlRepository.saveAndFlush(entity);
            }
        }
    }

    private void saveDetailTables(SalesInvoiceShippingUpdateDTO dto, Long transactionPoid) {
        if (dto.getContainerDetails() != null && !dto.getContainerDetails().isEmpty()) {
            Long maxDetRowId = contnrDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
            maxDetRowId = maxDetRowId != null ? maxDetRowId : 0L;
            for (SalesInvoiceContainerDtlDto detailDto : dto.getContainerDetails()) {
                ArShSalesInvoiceContnrDtl entity = SalesInvoiceMapper.mapContainerDtlFromDto(detailDto, transactionPoid);
                if (entity.getDetRowId() == null) {
                    entity.setDetRowId(++maxDetRowId);
                }
                contnrDtlRepository.saveAndFlush(entity);
            }
        }

        if (dto.getChargesDetails() != null && !dto.getChargesDetails().isEmpty()) {
            Long maxDetRowId = chargDtlRepository.findMaxDetRowIdByTransactionPoid(transactionPoid);
            maxDetRowId = maxDetRowId != null ? maxDetRowId : 0L;
            for (SalesInvoiceChargesDtlDto detailDto : dto.getChargesDetails()) {
                ArShSalesInvoiceChargDtl entity = SalesInvoiceMapper.mapChargesDtlFromDto(detailDto, transactionPoid);
                if (entity.getDetRowId() == null) {
                    entity.setDetRowId(++maxDetRowId);
                }
                chargDtlRepository.saveAndFlush(entity);
            }
        }
    }

    /**
     * Calculate invoice amount from charges
     */
    private BigDecimal calculateInvoiceAmount(Long transactionPoid) {
        List<ArShSalesInvoiceChargDtl> charges = chargDtlRepository.findByTransactionPoid(transactionPoid);
        return charges.stream()
                .filter(c -> "Y".equals(c.getAmountSelect()))
                .map(c -> c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate demurrage amount from container details
     */
    private BigDecimal calculateDemurrageAmountFromContainers(Long transactionPoid) {
        List<ArShSalesInvoiceContnrDtl> containers = contnrDtlRepository.findByTransactionPoid(transactionPoid);
        return containers.stream()
                .map(c -> c.getDmChargeAmt() != null ? c.getDmChargeAmt() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== Stored Procedure Calls ====================

    /**
     * Call PROC_SHIP_BL_PAGE_SAVE_AFTER
     */
    private void callProcShipBlPageSaveAfter(Long groupPoid, Long companyPoid, Long transactionPoid, String flag) {
        try {
            String sql = "{call PROC_SHIP_BL_PAGE_SAVE_AFTER(?, ?, ?, ?, ?, ?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, transactionPoid);
                cs.setString(4, null);
                cs.setString(5, flag);
                cs.setLong(6, getUserPoid());
                cs.execute();
                return null;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_BL_PAGE_SAVE_AFTER", e);
            throw new ValidationException("Error in post-save processing: " + e.getMessage());
        }
    }

    /**
     * Call PROC_SHIP_INVOICE_DELETED
     */
    private void validateInvoiceDeleted(Long blPoid, LocalDate invDate, String valType, Long transactionPoid) {
        try {
            String sql = "{call PROC_SHIP_INVOICE_DELETED(?, ?, ?, ?, ?, ?)}";
            String result = jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, transactionPoid != null ? transactionPoid : -999L);
                cs.setString(2, getUserId());
                cs.registerOutParameter(3, Types.VARCHAR);
                cs.setString(4, valType);
                cs.setLong(5, blPoid);
                cs.setDate(6, invDate != null ? java.sql.Date.valueOf(invDate) : null);
                cs.execute();
                return cs.getString(3);
            });
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_INVOICE_DELETED", e);
        }
    }

    /**
     * Call PROC_VALIDATE_CUSTOMER
     */
    private String callProcValidateCustomer(Long customerPoid, String creditType, String authorizedId) {
        try {
            String sql = "{call PROC_VALIDATE_CUSTOMER(?, ?, ?, ?)}";
            return jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, customerPoid);
                cs.registerOutParameter(2, Types.VARCHAR);
                cs.setString(3, creditType != null ? creditType : "BOTH");
                cs.setString(4, authorizedId != null ? authorizedId : "NONE");
                cs.execute();
                return cs.getString(2);
            });
        } catch (Exception e) {
            log.error("Error calling PROC_VALIDATE_CUSTOMER", e);
            return "ERROR";
        }
    }

    /**
     * Call PROC_GET_CUSTOMER_CREDIT_DAYS
     */
    private Integer callProcGetCustomerCreditDays(Long customerPoid, String blType) {
        try {
            String sql = "{call PROC_GET_CUSTOMER_CREDIT_DAYS(?, ?, ?)}";
            return jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, customerPoid);
                cs.setString(2, blType != null ? blType : "IMPORT");
                cs.registerOutParameter(3, Types.VARCHAR);
                cs.execute();
                String result = cs.getString(3);
                return result != null ? Integer.parseInt(result) : null;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_GET_CUSTOMER_CREDIT_DAYS", e);
            return null;
        }
    }

    /**
     * Call PROC_INVOICE_VERIFY
     */
    private void callProcInvoiceVerify(Long transactionPoid, String docId, String userId) {
        try {
            String sql = "{call PROC_INVOICE_VERIFY(?, ?, ?, ?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, transactionPoid);
                cs.setString(2, docId);
                cs.setString(3, userId);
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.execute();
                String result = cs.getString(4);
                log.info("PROC_INVOICE_VERIFY result: {}", result);
                return null;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_INVOICE_VERIFY", e);
            throw new ValidationException("Error verifying invoice: " + e.getMessage());
        }
    }

    /**
     * Call PROC_FF_JOB_CREATE_FROM_SH
     */
    private Map<String, Object> callProcFfJobCreateFromSh(Long groupPoid, Long transactionPoid, Long blPoid, String username) {
        try {
            String sql = "{call PROC_FF_JOB_CREATE_FROM_SH(?, ?, ?, ?, ?, ?)}";
            return jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, groupPoid);
                cs.setLong(2, transactionPoid);
                cs.setLong(3, blPoid);
                cs.setString(4, username);
                cs.registerOutParameter(5, Types.NUMERIC);
                cs.registerOutParameter(6, Types.VARCHAR);
                cs.execute();
                Map<String, Object> result = new HashMap<>();
                result.put("ffJobPoid", cs.getObject(5));
                result.put("status", cs.getString(6));
                return result;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_FF_JOB_CREATE_FROM_SH", e);
            throw new ValidationException("Error creating FF job: " + e.getMessage());
        }
    }

    /**
     * Call PROC_FF_TO_SH_PURCHASE_JOU
     */
    private Map<String, Object> callProcFfToShPurchaseJou(Long groupPoid, Long transactionPoid, Long ffJobPoid, String username) {
        try {
            String sql = "{call PROC_FF_TO_SH_PURCHASE_JOU(?, ?, ?, ?, ?, ?, ?)}";
            return jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, groupPoid);
                cs.setLong(2, transactionPoid);
                cs.setLong(3, ffJobPoid);
                cs.setString(4, username);
                cs.registerOutParameter(5, Types.VARCHAR);
                cs.registerOutParameter(6, Types.VARCHAR);
                cs.registerOutParameter(7, Types.VARCHAR);
                cs.execute();
                Map<String, Object> result = new HashMap<>();
                result.put("status", cs.getString(5));
                result.put("ffPjNo", cs.getString(6));
                result.put("ffJobNo", cs.getString(7));
                return result;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_FF_TO_SH_PURCHASE_JOU", e);
            throw new ValidationException("Error creating FF purchase journal: " + e.getMessage());
        }
    }

    /**
     * Call PROC_SHIP_UPDATE_BOOKING
     */
    private String callProcShipUpdateBooking(Long blPoid, Long customerPoid, String userPoid) {
        try {
            final String[] statusHolder = new String[1];
            String sql = "{call PROC_SHIP_UPDATE_BOOKING(?, ?, ?, ?)}";
            jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, blPoid);
                cs.setLong(2, customerPoid);
                cs.registerOutParameter(3, Types.VARCHAR);
                cs.setString(4, userPoid);
                cs.execute();
                String status = cs.getString(3);
                if (status != null && !status.isEmpty() && status.contains("ERROR")) {
                    throw new ValidationException("Error updating booking party: " + status);
                }
                return statusHolder[0] = cs.getString(3);
            });
            return statusHolder[0];
        } catch (Exception e) {
            log.error("Error calling PROC_SHIP_UPDATE_BOOKING", e);
            if (e instanceof ValidationException) {
                throw e;
            }
            throw new ValidationException("Error updating booking party: " + e.getMessage());
        }
    }

    /**
     * Call PROC_GET_INV_CUST_ADDRESS_V2
     */
    private Map<String, Object> callProcGetInvCustAddressV2(Long userPoid, Long addressMasterPoid, String addressType) {
        try {
            String sql = "{call PROC_GET_INV_CUST_ADDRESS_V2(?, ?, ?, ?)}";
            return jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, userPoid);
                cs.setLong(2, addressMasterPoid);
                cs.setString(3, addressType != null ? addressType : "INVOICE");
                cs.registerOutParameter(4, Types.REF_CURSOR);
                cs.execute();
                ResultSet rs = (ResultSet) cs.getObject(4);
                Map<String, Object> result = new HashMap<>();
                if (rs != null && rs.next()) {
                    result.put("contactPerson", rs.getString("CONTACT_PERSON"));
                    result.put("email1", rs.getString("EMAIL1"));
                    result.put("mobile", rs.getString("MOBILE"));
                }
                return result;
            });
        } catch (Exception e) {
            log.error("Error calling PROC_GET_INV_CUST_ADDRESS_V2", e);
            return new HashMap<>();
        }
    }

    /**
     * Call PROC_LOV_AFTER_BRWS_300_103
     */
    private Map<String, String> callProcLovAfterBrws300103(
            Long groupPoid,
            Long companyPoid,
            Long userPoid,
            String docId,
            Long docKeyPoid,
            String lovName,
            String lovValue
    ) {

        String sql = "{call PROC_LOV_AFTER_BRWS_300_103(?, ?, ?, ?, ?, ?, ?, ?)}";
        Map<String, String> result = new HashMap<>();
        jdbcTemplate.execute(sql, (CallableStatement cs) -> {

            try {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, userPoid);
                cs.setString(4, docId);
                cs.setLong(5, docKeyPoid);
                cs.setString(6, lovName);
                cs.setString(7, lovValue);
                cs.registerOutParameter(8, OracleTypes.CURSOR);
                cs.execute();

                ResultSet rs = (ResultSet) cs.getObject(8);
                if (rs != null) {
                    while (rs.next()) {
                        if (lovName.equals("ALLBLNUMBER_INV")) {

                            result.put("companyPoid", rs.getString("COMPANY_POID"));
                            result.put("blTypeInvoice", rs.getString("BL_TYPE_INVOICE"));
                            result.put("customerPoid", rs.getString("CUSTOMER_POID"));
                            result.put("invDate", rs.getString("INV_DATE"));
                            result.put("ownInvoiceNo", rs.getString("OWN_INVOICE_NO"));
                            result.put("bookingPartyPoid", rs.getString("BOOKING_PARTY_POID"));
                            result.put("creditDays", rs.getString("CREDIT_DAYS"));
                        } else if (lovName.equals("ALLBLNUMBER")) {

                            result.put("blTypeInvoice", rs.getString("BL_TYPE_INVOICE"));
                            result.put("customerPoid", rs.getString("CUSTOMER_POID"));
                            result.put("invDate", rs.getString("INV_DATE"));
                        }
                    }
                }
                log.info("Result Map : {}", result);
            } catch (Exception e) {
                log.error("Error : ", e);
            }
            return result;
        });
        return result;
    }

    // ==================== Complex SQL Query Execution ====================

    /**
     * Execute load container demurrage query (complex UNION ALL query from legacy)
     * Handles 3 scenarios:
     * 1. IMPORT BL - Demurrage calculation
     * 2. EXPORT BL with EXTRA_TARIFF='N' - Detention calculation
     * 3. EXPORT BL with EXTRA_TARIFF='Y' - Detention calculation (one-time)
     */
    private List<SalesInvoiceContainerDtlDto> executeLoadContainerDemurrageQuery(Long blPoid, Long transactionPoid, String blType) {
        log.info("Loading container demurrage data for BL POID: {}, Transaction POID: {}, BL Type: {}",
                blPoid, transactionPoid, blType);

        // Build complex UNION ALL query based on BL type
        StringBuilder sql = new StringBuilder();

        if ("IMPORT".equalsIgnoreCase(blType)) {
            // IMPORT BL - Demurrage calculation
            sql.append(
                    "SELECT BL_POID, EQUIPMENT_SHIPPER_OWN, CONTAINER_NO, " +
                            "NVL(DM_TILL_DATE, FROMDATE) FMDATE, NVL(DM_TILL_DATE, TODATE) TODATE, " +
                            "(TO_DATE(NVL(DM_TILL_DATE, TODATE)) - NVL(DM_TILL_DATE, FROMDATE)) + 1 DAYS, " +
                            "FUNC_RTN_DEM_DETTN_FULL(GROUP_POID, COMPANY_POID, ?, BL_POID, CONTAINER_NO, " +
                            "GET_CONTAINER_CODE_POID(EQUIPMENT_ISO_TYPE), LINE_POID, TO_DATE(ARRIVAL_DATE), " +
                            "TO_DATE(NVL(DM_TILL_DATE, TODATE)), 'DEMM', NVL(EXTRA_FREE_DAYS,0)) DM_AMT, " +
                            "EQUIPMENT_ISO_TYPE, FREE_DAYS, EMPTY_IN " +
                            "FROM ( " +
                            "  SELECT TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)) ARRIVAL_DATE, BLHDR.GROUP_POID, " +
                            "  BLHDR.COMPANY_POID, EQUIPMENT_ISO_TYPE, VHDR.LINE_POID, EXTRA_FREE_DAYS, " +
                            "  CONTAINERDTL.TRANSACTION_POID BL_POID, EQUIPMENT_SHIPPER_OWN, CONTAINER_NO, " +
                            "  TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)) + " +
                            "  DECODE(NVL(EXTRA_FREE_DAYS,0),0,FREE_DAYS,NVL(EXTRA_FREE_DAYS,0)) FROMDATE, " +
                            "  CASE WHEN (TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE))+DECODE(NVL(EXTRA_FREE_DAYS,0),0,FREE_DAYS,NVL(EXTRA_FREE_DAYS,0)))<=TO_DATE(SYSDATE) " +
                            "  THEN TO_DATE(SYSDATE) ELSE NULL END AS TODATE, " +
                            "  (SELECT MAX(DM_TO_DATE)+1 FROM VW_AR_SH_CONTAINER_DEMG_DTTN ARCONTAINERDTL " +
                            "   WHERE ARCONTAINERDTL.BL_POID=CONTAINERDTL.TRANSACTION_POID " +
                            "   AND ARCONTAINERDTL.CONTAINER_NO=CONTAINERDTL.CONTAINER_NO " +
                            "   AND TRANSACTION_POID<>?) DM_TILL_DATE, " +
                            "  DECODE(NVL(EXTRA_FREE_DAYS,0),0,FREE_DAYS,NVL(EXTRA_FREE_DAYS,0)) FREE_DAYS, " +
                            "  (SELECT TO_DATE(TRUNC(MOVES_DATE_TIME)) FROM SHIP_CONTAINER_INVENTORY " +
                            "   WHERE MOVES_TYPE='MTIN' AND LINK_TRANSACTION_POID=BLHDR.TRANSACTION_POID " +
                            "   AND CONTAINER_NO=CONTAINERDTL.CONTAINER_NO) EMPTY_IN " +
                            "  FROM SHIP_VOYAGE_HDR VHDR " +
                            "  INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID=BLHDR.VOYAGE_TRANSACTION_POID " +
                            "  INNER JOIN SHIP_BL_MANIFEST_CONTAINER_DTL CONTAINERDTL ON CONTAINERDTL.TRANSACTION_POID=BLHDR.TRANSACTION_POID " +
                            "  INNER JOIN SHIP_LINE_TARIFF_HDR SHLNTFHDR ON SHLNTFHDR.LINE_POID=VHDR.LINE_POID " +
                            "  AND DECODE(VHDR.LINE_POID,'1123',TO_DATE(SYSDATE),TO_DATE(TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)))) " +
                            "  BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO) " +
                            "  INNER JOIN SHIP_LINE_TARIFF_IMP_DTL CONTAINERTRIFIMP ON " +
                            "  SHLNTFHDR.TRANSACTION_POID=CONTAINERTRIFIMP.TRANSACTION_POID " +
                            "  AND CONTAINERTRIFIMP.CONTAINER_TYPE_POID=GET_CONTAINER_CODE_POID(CONTAINERDTL.EQUIPMENT_ISO_TYPE) " +
                            "  WHERE BL_TYPE='IMPORT' AND BLHDR.TRANSACTION_POID=? " +
                            ")");
        } else if ("EXPORT".equalsIgnoreCase(blType)) {
            // EXPORT BL - Two parts: EXTRA_TARIFF='N' and EXTRA_TARIFF='Y'
            // Part 1: EXTRA_TARIFF='N'
            sql.append(
                    "SELECT BL_POID, EQUIPMENT_SHIPPER_OWN, CONTAINER_NO, " +
                            "NVL(DM_TILL_DATE, FROMDATE) FMDATE, NVL(DM_TILL_DATE, TODATE) TODATE, " +
                            "(TO_DATE(NVL(DM_TILL_DATE, TODATE)) - NVL(DM_TILL_DATE, FROMDATE)) + 1 DAYS, " +
                            "FUNC_RTN_DEM_DETTN_FULL(GROUP_POID, COMPANY_POID, ?, BL_POID, CONTAINER_NO, " +
                            "GET_CONTAINER_CODE_POID(EQUIPMENT_ISO_TYPE), LINE_POID, TO_DATE(ISSUE_DATE), " +
                            "TO_DATE(NVL(DM_TILL_DATE, TODATE)), 'DETN', NVL(EXTRA_FREE_DAYS,0)) DM_AMT, " +
                            "EQUIPMENT_ISO_TYPE, FREE_DAYS, NULL EMPTY_IN " +
                            "FROM ( " +
                            "  SELECT TO_DATE(NVL(SAIL_DATE,BERTH_DATE)) SAIL_DATE, BLHDR.GROUP_POID, " +
                            "  BLHDR.COMPANY_POID, EQUIPMENT_ISO_TYPE, VHDR.LINE_POID, EXTRA_FREE_DAYS, " +
                            "  CONTAINERDTL.TRANSACTION_POID BL_POID, EQUIPMENT_SHIPPER_OWN, CONTAINER_NO, " +
                            "  (SELECT TRUNC(MOVES_DATE_TIME) FROM SHIP_CONTAINER_INVENTORY " +
                            "   WHERE BOOKING_TRANSACTION_POID=CONTAINERDTL.MATE_TRANSACTION_POID " +
                            "   AND MOVES_TYPE='VAN' AND CONTAINER_NO=CONTAINERDTL.CONTAINER_NO) ISSUE_DATE, " +
                            "  (SELECT TRUNC(MOVES_DATE_TIME) FROM SHIP_CONTAINER_INVENTORY " +
                            "   WHERE BOOKING_TRANSACTION_POID=CONTAINERDTL.MATE_TRANSACTION_POID " +
                            "   AND MOVES_TYPE='VAN' AND CONTAINER_NO=CONTAINERDTL.CONTAINER_NO) + " +
                            "  DECODE(NVL(EXTRA_FREE_DAYS,0),0,FREE_DAYS,NVL(EXTRA_FREE_DAYS,0)) FROMDATE, " +
                            "  (SELECT TRUNC(MOVES_DATE_TIME) FROM SHIP_CONTAINER_INVENTORY " +
                            "   WHERE BOOKING_TRANSACTION_POID=CONTAINERDTL.MATE_TRANSACTION_POID " +
                            "   AND MOVES_TYPE=DECODE(VHDR.LINE_POID,'114','LDFULL','EXPIN') " +
                            "   AND CONTAINER_NO=CONTAINERDTL.CONTAINER_NO) TODATE, " +
                            "  (SELECT MAX(DM_TO_DATE)+1 FROM VW_AR_SH_CONTAINER_DEMG_DTTN ARCONTAINERDTL " +
                            "   WHERE ARCONTAINERDTL.BL_POID=CONTAINERDTL.TRANSACTION_POID " +
                            "   AND ARCONTAINERDTL.CONTAINER_NO=CONTAINERDTL.CONTAINER_NO " +
                            "   AND TRANSACTION_POID<>?) DM_TILL_DATE, " +
                            "  DECODE(NVL(EXTRA_FREE_DAYS,0),0,FREE_DAYS,NVL(EXTRA_FREE_DAYS,0)) FREE_DAYS " +
                            "  FROM SHIP_VOYAGE_HDR VHDR " +
                            "  INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID=BLHDR.VOYAGE_TRANSACTION_POID " +
                            "  INNER JOIN SHIP_BL_MANIFEST_CONTAINER_DTL CONTAINERDTL ON CONTAINERDTL.TRANSACTION_POID=BLHDR.TRANSACTION_POID " +
                            "  INNER JOIN SHIP_LINE_TARIFF_HDR SHLNTFHDR ON SHLNTFHDR.LINE_POID=VHDR.LINE_POID " +
                            "  AND DECODE(VHDR.LINE_POID,'1123',TO_DATE(SYSDATE),TO_DATE(TO_DATE(NVL(SAIL_DATE,BERTH_DATE)))) " +
                            "  BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO) " +
                            "  INNER JOIN SHIP_LINE_TARIFF_EXP_DTL CONTAINERTRIFIMP ON " +
                            "  SHLNTFHDR.TRANSACTION_POID=CONTAINERTRIFIMP.TRANSACTION_POID " +
                            "  AND CONTAINERTRIFIMP.CONTAINER_TYPE_POID=GET_CONTAINER_CODE_POID(CONTAINERDTL.EQUIPMENT_ISO_TYPE) " +
                            "  WHERE BL_TYPE='EXPORT' AND EXTRA_TARIFF='N' AND BLHDR.TRANSACTION_POID=? " +
                            ") " +
                            "UNION ALL " +
                            // Part 2: EXTRA_TARIFF='Y'
                            "SELECT BL_POID, EQUIPMENT_SHIPPER_OWN, CONTAINER_NO, " +
                            "NVL(DM_TILL_DATE, FROMDATE) FMDATE, NVL(DM_TILL_DATE, TODATE) TODATE, " +
                            "(TO_DATE(NVL(DM_TILL_DATE, TODATE)) - NVL(DM_TILL_DATE, FROMDATE)) + 1 DAYS, " +
                            "FUNC_RTN_DEM_DETTN_FULL(GROUP_POID, COMPANY_POID, ?, BL_POID, CONTAINER_NO, " +
                            "GET_CONTAINER_CODE_POID(EQUIPMENT_ISO_TYPE), LINE_POID, TO_DATE(ISSUE_DATE), " +
                            "TO_DATE(NVL(DM_TILL_DATE, TODATE)), 'DETN', NVL(EXTRA_FREE_DAYS,0)) DM_AMT, " +
                            "EQUIPMENT_ISO_TYPE, 0 FREE_DAYS, NULL EMPTY_IN " +
                            "FROM ( " +
                            "  SELECT TO_DATE(NVL(SAIL_DATE,BERTH_DATE)) SAIL_DATE, BLHDR.GROUP_POID, " +
                            "  BLHDR.COMPANY_POID, EQUIPMENT_ISO_TYPE, VHDR.LINE_POID, EXTRA_FREE_DAYS, " +
                            "  CONTAINERDTL.TRANSACTION_POID BL_POID, EQUIPMENT_SHIPPER_OWN, CONTAINER_NO, " +
                            "  (SELECT TRUNC(MOVES_DATE_TIME) FROM SHIP_CONTAINER_INVENTORY " +
                            "   WHERE BOOKING_TRANSACTION_POID=CONTAINERDTL.MATE_TRANSACTION_POID " +
                            "   AND MOVES_TYPE='EXPIN' AND CONTAINER_NO=CONTAINERDTL.CONTAINER_NO) ISSUE_DATE, " +
                            "  (SELECT TRUNC(MOVES_DATE_TIME) FROM SHIP_CONTAINER_INVENTORY " +
                            "   WHERE BOOKING_TRANSACTION_POID=CONTAINERDTL.MATE_TRANSACTION_POID " +
                            "   AND MOVES_TYPE='EXPIN' AND CONTAINER_NO=CONTAINERDTL.CONTAINER_NO) FROMDATE, " +
                            "  (SELECT TRUNC(MOVES_DATE_TIME) FROM SHIP_CONTAINER_INVENTORY " +
                            "   WHERE BOOKING_TRANSACTION_POID=CONTAINERDTL.MATE_TRANSACTION_POID " +
                            "   AND MOVES_TYPE='LDFULL' AND CONTAINER_NO=CONTAINERDTL.CONTAINER_NO) TODATE, " +
                            "  (SELECT MAX(DM_TO_DATE)+1 FROM VW_AR_SH_CONTAINER_DEMG_DTTN ARCONTAINERDTL " +
                            "   WHERE ARCONTAINERDTL.BL_POID=CONTAINERDTL.TRANSACTION_POID " +
                            "   AND ARCONTAINERDTL.CONTAINER_NO=CONTAINERDTL.CONTAINER_NO " +
                            "   AND TRANSACTION_POID<>?) DM_TILL_DATE " +
                            "  FROM SHIP_VOYAGE_HDR VHDR " +
                            "  INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID=BLHDR.VOYAGE_TRANSACTION_POID " +
                            "  INNER JOIN SHIP_BL_MANIFEST_CONTAINER_DTL CONTAINERDTL ON CONTAINERDTL.TRANSACTION_POID=BLHDR.TRANSACTION_POID " +
                            "  INNER JOIN SHIP_LINE_TARIFF_HDR SHLNTFHDR ON SHLNTFHDR.LINE_POID=VHDR.LINE_POID " +
                            "  AND DECODE(VHDR.LINE_POID,'1123',TO_DATE(SYSDATE),TO_DATE(TO_DATE(NVL(SAIL_DATE,BERTH_DATE)))) " +
                            "  BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO) " +
                            "  INNER JOIN SHIP_LINE_TARIFF_EXP_DTL CONTAINERTRIFIMP ON " +
                            "  SHLNTFHDR.TRANSACTION_POID=CONTAINERTRIFIMP.TRANSACTION_POID " +
                            "  AND CONTAINERTRIFIMP.CONTAINER_TYPE_POID=GET_CONTAINER_CODE_POID(CONTAINERDTL.EQUIPMENT_ISO_TYPE) " +
                            "  WHERE BL_TYPE='EXPORT' AND EXTRA_TARIFF='Y' AND BLHDR.TRANSACTION_POID=? " +
                            ")");
        } else {
            // Unknown BL type, return empty list
            return new ArrayList<>();
        }

        List<Object> params = new ArrayList<>();
        if ("IMPORT".equalsIgnoreCase(blType)) {
            params.add(transactionPoid);
            params.add(transactionPoid);
            params.add(blPoid);
        } else if ("EXPORT".equalsIgnoreCase(blType)) {
            params.add(transactionPoid);
            params.add(transactionPoid);
            params.add(blPoid);
            params.add(transactionPoid);
            params.add(transactionPoid);
            params.add(blPoid);
        }

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> SalesInvoiceContainerDtlDto.builder()
                .blPoid(rs.getLong("BL_POID"))
                .containerSocYn(rs.getString("EQUIPMENT_SHIPPER_OWN"))
                .containerNo(rs.getString("CONTAINER_NO"))
                .dmFrmDate(getLocalDateOrNull(rs, "FMDATE"))
                .dmToDate(getLocalDateOrNull(rs, "TODATE"))
                .dmDays(getIntegerOrNull(rs, "DAYS"))
                .dmChargeAmt(getBigDecimalOrNull(rs, "DM_AMT"))
                .equipmentIsoType(rs.getString("EQUIPMENT_ISO_TYPE"))
                .freeDays(getIntegerOrNull(rs, "FREE_DAYS"))
                .emptyIn(getLocalDateOrNull(rs, "EMPTY_IN"))
                .build(), params.toArray());
    }

    /**
     * Execute load charge data query
     */
    private List<SalesInvoiceChargesDtlDto> executeLoadChargeDataQuery(Long blPoid, String blTypeInvoice, Long transactionPoid) {
        String freightType = "IMPORT".equalsIgnoreCase(blTypeInvoice) ? "C" : "P";
        String sql = "SELECT CHARGEDTL.TRANSACTION_POID BL_POID, CHARGEDTL.CHARGE_POID, DET_ROW_ID, " +
                "round((NVL(CURRENCY_EXCHANGE,1)*NVL(QUANTITY,1)* NVL(PER_QUANTITY_AMOUNT,0)),3) AMOUNT, " +
                "round((NVL(CURRENCY_EXCHANGE,1)*NVL(QUANTITY,1)* NVL(BUY_PERCHARGE,0)),3) BuyAmount, " +
                "CHARGEDTL.CURRENCY_CODE, CURRENCY_EXCHANGE, QUANTITY, " +
                "round(NVL(BUY_PERCHARGE,0),3) PerQtyBuy, round(NVL(PER_QUANTITY_AMOUNT,0),3) PerQtySell, " +
                "CHARGEDTL.CHARGE_TYPE, decode(EDI_CHARGE_CODE,'ADDFROMRECEIPT','Y','ADDFROMINVOICE','Y','N') CHARGE_NEW_RECORD, " +
                "TAX_PERCENTAGE, TAX_AMOUNT, TAX_POID " +
                "FROM SHIP_VOYAGE_HDR VHDR " +
                "INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID=BLHDR.VOYAGE_TRANSACTION_POID " +
                "INNER JOIN SHIP_BL_MANIFEST_CHARGES_DTL CHARGEDTL ON CHARGEDTL.TRANSACTION_POID=BLHDR.TRANSACTION_POID " +
                "WHERE FREIGHT_TYPE=? AND (CHARGEDTL.TRANSACTION_POID,DET_ROW_ID, CHARGEDTL.CHARGE_POID) NOT IN( " +
                "select NVL(ARSHINVCHD.BL_POID,0),NVL(CHARGES_DET_ROW_ID,0),NVL(CHARGE_POID,0) from " +
                "AR_SH_SALES_INVOICE_HDR ARSHINV, AR_SH_SALES_INVOICE_CHARG_DTL ARSHINVCHD " +
                "WHERE ARSHINV.TRANSACTION_POID=ARSHINVCHD.TRANSACTION_POID AND NVL(DELETED,'N')='N' and ARSHINV.transaction_poid <> ? " +
                "union all select NVL(ARSHRCPCHD.BL_POID,0),NVL(CHARGES_DET_ROW_ID,0),NVL(CHARGE_POID,0) from " +
                "AR_SH_RECEIPT_HDR ARSHRCP, AR_SH_RECEIPT_CHARGES_DTL ARSHRCPCHD " +
                "WHERE ARSHRCP.TRANSACTION_POID=ARSHRCPCHD.TRANSACTION_POID AND NVL(DELETED,'N')='N' ) " +
                "AND CHARGEDTL.AR_SH_RECEIPT_TRANSACTION_POID IS NULL AND BLHDR.TRANSACTION_POID=?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> SalesInvoiceChargesDtlDto.builder()
                .blPoid(rs.getLong("BL_POID"))
                .chargePoid(rs.getLong("CHARGE_POID"))
                .chargesDetRowId(rs.getLong("DET_ROW_ID"))
                .amount(rs.getBigDecimal("AMOUNT"))
                .buyAmount(rs.getBigDecimal("BuyAmount"))
                .currencyCode(rs.getString("CURRENCY_CODE"))
                .currencyExchange(rs.getBigDecimal("CURRENCY_EXCHANGE"))
                .quantity(rs.getBigDecimal("QUANTITY"))
                .perQtyBuyAmt(rs.getBigDecimal("PerQtyBuy"))
                .perQtySellAmt(rs.getBigDecimal("PerQtySell"))
                .chargeType(rs.getString("CHARGE_TYPE"))
                .chargeNewRecord(rs.getString("CHARGE_NEW_RECORD"))
                .taxPercentage(rs.getBigDecimal("TAX_PERCENTAGE"))
                .taxAmount(rs.getBigDecimal("TAX_AMOUNT"))
                .taxPoid(rs.getLong("TAX_POID"))
                .amountSelect("Y")
                .build(), freightType, transactionPoid, blPoid);
    }

    /**
     * Calculate demurrage/detention amount from container list and track container quantities by size
     * Mirrors legacy implementation: iterates through provided container details
     * 1. Accumulates total demurrage charge amounts for containers with non-zero charges
     * 2. Tracks container quantities by size (20ft vs 40ft) using GET_CONTAINER_TYPE function
     * Returns total demurrage amount
     * 
     * This method uses the actual container data passed from loadChargeData rather than fetching from DB
     */
    private BigDecimal createDemurrageDettention(List<SalesInvoiceContainerDtlDto> containerList, Long blPoid) {
        log.info("Creating demurrage/detention calculation from container list with {} containers", 
                containerList != null ? containerList.size() : 0);
        
        // Initialize counters (matching legacy FtotalQtyValidate20 and FtotalQtyValidate40)
        totalQtyValidate20 = BigDecimal.ZERO;
        totalQtyValidate40 = BigDecimal.ZERO;
        
        BigDecimal FTotalRcpAmount = BigDecimal.ZERO;
        
        // If no containers provided, return zero
        if (containerList == null || containerList.isEmpty()) {
            log.warn("No containers provided for demurrage calculation");
            return FTotalRcpAmount;
        }
        
        try {
            // Iterate through provided container list (already filtered for this BL)
            for (SalesInvoiceContainerDtlDto containerDtl : containerList) {
                BigDecimal TotalRcpAmount = BigDecimal.ZERO;
                
                // Extract demurrage amount if present
                if (blPoid.equals(containerDtl.getBlPoid()) && containerDtl.getDmChargeAmt() != null) {
                    TotalRcpAmount = containerDtl.getDmChargeAmt();
                }
                
                // Accumulate only if amount is non-zero (matching legacy logic)
                if ((TotalRcpAmount != null) && (TotalRcpAmount.compareTo(BigDecimal.ZERO) != 0)) {
                    FTotalRcpAmount = FTotalRcpAmount.add(TotalRcpAmount);
                    
                    // Determine container size and track quantities
                    String equipmentIsoType = containerDtl.getEquipmentIsoType();
                    if (equipmentIsoType != null && !equipmentIsoType.isEmpty()) {
                        String containerSizeQuery = "SELECT GET_CONTAINER_TYPE(?, 'SIZE') FROM DUAL";
                        try {
                            String containerSize = jdbcTemplate.queryForObject(
                                containerSizeQuery,
                                String.class,
                                equipmentIsoType
                            );
                            
                            BigDecimal FaddDecimal = BigDecimal.ONE;
                            if (containerSize != null) {
                                if ("20".equalsIgnoreCase(containerSize.trim())) {
                                    totalQtyValidate20 = totalQtyValidate20.add(FaddDecimal);
                                    log.debug("Added 20ft container: {}, DmChargeAmt: {}, running total 20ft: {}", 
                                        containerDtl.getContainerNo(), TotalRcpAmount, totalQtyValidate20);
                                } else {
                                    // Default to 40ft for any other size
                                    totalQtyValidate40 = totalQtyValidate40.add(FaddDecimal);
                                    log.debug("Added 40ft container: {}, DmChargeAmt: {}, running total 40ft: {}", 
                                        containerDtl.getContainerNo(), TotalRcpAmount, totalQtyValidate40);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Could not determine container size for equipment type: {}", equipmentIsoType, e);
                        }
                    }
                }
            }
            
            log.info("Demurrage/detention calculation complete - Total Amount: {}, 20ft Qty: {}, 40ft Qty: {}",
                    FTotalRcpAmount, totalQtyValidate20, totalQtyValidate40);
        } catch (Exception e) {
            log.error("Error calculating demurrage/detention from container list", e);
        }
        
        return FTotalRcpAmount;
    }
    
    /**
     * Get total quantity of 20ft containers from last demurrage calculation
     */
    public BigDecimal getTotalQtyValidate20() {
        return totalQtyValidate20;
    }
    
    /**
     * Get total quantity of 40ft containers from last demurrage calculation
     */
    public BigDecimal getTotalQtyValidate40() {
        return totalQtyValidate40;
    }

    /**
     * Load demurrage/detention charge with tax information
     * Queries GLOBAL_PARAMETERS for demurrage charge based on BL type:
     * 1. IMPORT BL - uses SHDEMURRAGE parameter
     * 2. EXPORT BL - uses SHDETTENTION parameter
     * Then retrieves associated tax information and creates charge entry
     */
    private List<SalesInvoiceChargesDtlDto> loadDemurrageCharges(BigDecimal demurrageAmount, String blTypeInvoice, Long companyPoid) {
        List<SalesInvoiceChargesDtlDto> result = new ArrayList<>();
        
        if (demurrageAmount == null || demurrageAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return result;
        }
        
        log.info("Loading demurrage charge with amount: {}, BL Type: {}", demurrageAmount, blTypeInvoice);
        
        String parameterType = "IMPORT".equalsIgnoreCase(blTypeInvoice) ? "SHDEMURRAGE" : "SHDETTENTION";
        
        String sql = "SELECT PARAMETER_VALUE, " +
                "(SELECT TAX_POID FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN " +
                "  (SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH " +
                "   INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD ON GTH.TRANSACTION_POID=GTD.TRANSACTION_POID " +
                "   WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO) " +
                "   AND CHARGE_POID=PARAMETER_VALUE)) TAX_POID, " +
                "(SELECT PERCENTAGE FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN " +
                "  (SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH " +
                "   INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD ON GTH.TRANSACTION_POID=GTD.TRANSACTION_POID " +
                "   WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO) " +
                "   AND CHARGE_POID=PARAMETER_VALUE)) TAX_PERCENTAGE, " +
                "RTN_GLOBAL_PARAMETER('1', 'GLOBAL_TAX_APPLICABLE', 'TAX', ?, 'N') TAX_APPLICABLE " +
                "FROM GLOBAL_PARAMETERS WHERE PARAMETER_KEYID_TYPE = ?";
        
        try {
            result = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Long chargePoid = rs.getLong("PARAMETER_VALUE");
                Long taxPoid = getLongOrNull(rs, "TAX_POID");
                BigDecimal taxPercentage = getBigDecimalOrNull(rs, "TAX_PERCENTAGE");
                String taxApplicable = rs.getString("TAX_APPLICABLE");
                
                BigDecimal taxAmount = BigDecimal.ZERO;
                if (taxPoid != null && taxPercentage != null && "Y".equalsIgnoreCase(taxApplicable)) {
                    taxAmount = demurrageAmount.multiply(taxPercentage.divide(new BigDecimal("100")));
                }
                
                return SalesInvoiceChargesDtlDto.builder()
                        .chargePoid(chargePoid)
                        .chargesDetRowId(0L)
                        .amount(demurrageAmount)
                        .amountSelect("Y")
                        .chargeType("LOCAL")
                        .chargeNewRecord("Y")
                        .taxPoid(taxPoid)
                        .taxPercentage(taxPercentage)
                        .taxAmount(taxAmount)
                        .build();
            }, companyPoid, parameterType);
        } catch (Exception e) {
            log.warn("Could not load demurrage charges", e);
        }
        
        return result;
    }

    /**
     * Load late collection charges
     * Queries SHIP_PORT_CHARGES_HDR and SHIP_PORT_CHARGES_DTL for:
     * 1. LATECOLLECTIONIMP/LATECOLLECTIONBOTH charges (when days since arrival >= threshold)
     * 2. REVALIDATEIMP/REVALIDATEBOTH charges (when BL has receipt/invoice)
     */
    private List<SalesInvoiceChargesDtlDto> loadLateCollectionCharges(Long blPoid, String blTypeInvoice, Long companyPoid) {
        log.info("Loading late collection charges for BL POID: {}, BL Type: {}", blPoid, blTypeInvoice);

        String blTypePrefix = blTypeInvoice != null && blTypeInvoice.length() >= 3
                ? blTypeInvoice.substring(0, 3) : "IMP";

        String thresholdSql = "SELECT TO_NUMBER(PARAMETER_VALUE) FROM GLOBAL_PARAMETERS " +
                "WHERE PARAMETER_NAME LIKE '%SHIPLATEDOCOLLECTION%' AND ROWNUM = 1";
        BigDecimal thresholdDays = null;
        try {
            thresholdDays = jdbcTemplate.queryForObject(thresholdSql, BigDecimal.class);
        } catch (Exception e) {
            log.warn("Could not get late collection threshold, defaulting to 0", e);
            thresholdDays = BigDecimal.ZERO;
        }

        // Build UNION ALL query for late collection and revalidation charges
        String sql =
                "SELECT CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE, CHARGE_CODE_POID, AMOUNT_20, AMOUNT_40, AMOUNT_OTHER, " +
                        "(SELECT TAX_POID FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN " +
                        "  (SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH " +
                        "   INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD ON GTH.TRANSACTION_POID=GTD.TRANSACTION_POID " +
                        "   WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO) " +
                        "   AND CHARGE_POID=MCDTL.CHARGE_CODE_POID)) TAX_POID, " +
                        "(SELECT PERCENTAGE FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN " +
                        "  (SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH " +
                        "   INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD ON GTH.TRANSACTION_POID=GTD.TRANSACTION_POID " +
                        "   WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO) " +
                        "   AND CHARGE_POID=MCDTL.CHARGE_CODE_POID)) TAX_PERCENTAGE, " +
                        "RTN_GLOBAL_PARAMETER('1', 'GLOBAL_TAX_APPLICABLE', 'TAX', ?, 'N') TAX_APPLICABLE " +
                        "FROM SHIP_PORT_CHARGES_HDR MCHDR " +
                        "INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL ON MCHDR.TRANSACTION_POID=MCDTL.TRANSACTION_POID " +
                        "WHERE (SELECT TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)) " +
                        "       FROM SHIP_VOYAGE_HDR VHDR " +
                        "       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID=BLHDR.VOYAGE_TRANSACTION_POID " +
                        "       WHERE SUBSTR(CHARGE_TYPE_APPLICABLE,-3) IN (SUBSTR(?,1,3),'OTH') " +
                        "       AND BLHDR.TRANSACTION_POID=?) " +
                        "BETWEEN PERIOD_FROM AND PERIOD_TO " +
                        "AND NVL(CHARGE_LINE_POID,'0')='0' " +
                        "AND CHARGE_TYPE_APPLICABLE IN ('LATECOLLECTIONIMP','LATECOLLECTIONBOTH') " +
                        "AND NVL(DELETED,'N')='N' " +
                        "AND (SELECT (TO_DATE(SYSDATE)-TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)))+1 " +
                        "     FROM SHIP_VOYAGE_HDR VHDR " +
                        "     INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID=BLHDR.VOYAGE_TRANSACTION_POID " +
                        "     INNER JOIN SHIP_BL_MANIFEST_CHARGES_DTL CNTDTL ON CNTDTL.TRANSACTION_POID=BLHDR.TRANSACTION_POID " +
                        "     WHERE NVL(BLHDR.TRANSACTION_POID,0) NOT IN " +
                        "       (SELECT NVL(BL_POID,0) FROM AR_SH_RECEIPT_HDR " +
                        "        UNION ALL SELECT NVL(BL_POID,0) FROM AR_SH_SALES_INVOICE_HDR " +
                        "        WHERE NVL(INVOICE_TYPE,'xx')<>'AUTOCAN') " +
                        "     AND BLHDR.TRANSACTION_POID=? " +
                        "     GROUP BY TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)),BLHDR.TRANSACTION_POID) " +
                        ">= (SELECT TO_NUMBER(PARAMETER_VALUE) FROM GLOBAL_PARAMETERS " +
                        "    WHERE PARAMETER_NAME LIKE '%SHIPLATEDOCOLLECTION%' AND ROWNUM=1) " +
                        "UNION ALL " +
                        "SELECT CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE, CHARGE_CODE_POID, AMOUNT_20, AMOUNT_40, AMOUNT_OTHER, " +
                        "(SELECT TAX_POID FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN " +
                        "  (SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH " +
                        "   INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD ON GTH.TRANSACTION_POID=GTD.TRANSACTION_POID " +
                        "   WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO) " +
                        "   AND CHARGE_POID=MCDTL.CHARGE_CODE_POID)) TAX_POID, " +
                        "(SELECT PERCENTAGE FROM GLOBAL_TAX_MASTER WHERE TAX_POID IN " +
                        "  (SELECT TAX_POID FROM GLOBAL_TAX_PERIOD_HDR GTH " +
                        "   INNER JOIN GLOBAL_TAX_PERIOD_CHARGE_DTL GTD ON GTH.TRANSACTION_POID=GTD.TRANSACTION_POID " +
                        "   WHERE TO_DATE(SYSDATE) BETWEEN TO_DATE(PERIOD_FROM) AND TO_DATE(PERIOD_TO) " +
                        "   AND CHARGE_POID=MCDTL.CHARGE_CODE_POID)) TAX_PERCENTAGE, " +
                        "RTN_GLOBAL_PARAMETER('1', 'GLOBAL_TAX_APPLICABLE', 'TAX', ?, 'N') TAX_APPLICABLE " +
                        "FROM SHIP_PORT_CHARGES_HDR MCHDR " +
                        "INNER JOIN SHIP_PORT_CHARGES_DTL MCDTL ON MCHDR.TRANSACTION_POID=MCDTL.TRANSACTION_POID " +
                        "WHERE (SELECT TO_DATE(NVL(ARRIVAL_DATE,EXPECTED_DATE)) " +
                        "       FROM SHIP_VOYAGE_HDR VHDR " +
                        "       INNER JOIN SHIP_BL_MANIFEST_HDR BLHDR ON VHDR.TRANSACTION_POID=BLHDR.VOYAGE_TRANSACTION_POID " +
                        "       WHERE SUBSTR(CHARGE_TYPE_APPLICABLE,-3) IN (SUBSTR(?,1,3),'OTH') " +
                        "       AND BLHDR.TRANSACTION_POID IN " +
                        "         (SELECT TRANSACTION_POID FROM SHIP_BL_MANIFEST_CHARGES_DTL " +
                        "          WHERE RECEIPT_INVOICE_POID IS NOT NULL) " +
                        "       AND BLHDR.TRANSACTION_POID=?) " +
                        "BETWEEN PERIOD_FROM AND PERIOD_TO " +
                        "AND NVL(CHARGE_LINE_POID,'0')='0' " +
                        "AND CHARGE_TYPE_APPLICABLE IN ('REVALIDATEIMP','REVALIDATEBOTH') " +
                        "AND NVL(DELETED,'N')='N' " +
                        "ORDER BY CHARGE_TYPE_APPLICABLE, CHARGE_APPLICABLE";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String chargeApplicable = rs.getString("CHARGE_APPLICABLE");
            Long chargePoid = rs.getLong("CHARGE_CODE_POID");
            BigDecimal amount20 = rs.getBigDecimal("AMOUNT_20");
            BigDecimal amount40 = rs.getBigDecimal("AMOUNT_40");
            BigDecimal amountOther = rs.getBigDecimal("AMOUNT_OTHER");
            Long taxPoid = getLongOrNull(rs, "TAX_POID");
            BigDecimal taxPercentage = getBigDecimalOrNull(rs, "TAX_PERCENTAGE");
            String taxApplicable = rs.getString("TAX_APPLICABLE");

            BigDecimal amount = BigDecimal.ZERO;
            if ("PERBL".equalsIgnoreCase(chargeApplicable)) {
                amount = amountOther != null ? amountOther : BigDecimal.ZERO;
            } else if ("PERQUENTITY".equalsIgnoreCase(chargeApplicable)) {
                amount = amount20 != null ? amount20 : BigDecimal.ZERO;
            }

            BigDecimal taxAmount = BigDecimal.ZERO;
            if (taxPoid != null && taxPercentage != null && "Y".equalsIgnoreCase(taxApplicable) && amount != null) {
                taxAmount = amount.multiply(taxPercentage.divide(new BigDecimal("100")));
            }

            return SalesInvoiceChargesDtlDto.builder()
                    .blPoid(blPoid)
                    .chargePoid(chargePoid)
                    .chargesDetRowId(0L)
                    .amount(amount)
                    .amountSelect("Y")
                    .chargeNewRecord("Y")
                    .taxPoid(taxPoid)
                    .taxPercentage(taxPercentage)
                    .taxAmount(taxAmount)
                    .build();
        }, companyPoid, blTypeInvoice, blPoid, blPoid, companyPoid, blTypeInvoice, blPoid);
    }

    // ==================== Validation Helper Methods ====================

    private void validateBlApproved(Long blPoid) {
        String sql = "SELECT COUNT(*) FROM VOYAGEWISEBILLS_APPROVED WHERE BL_POID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, blPoid);
        if (count == null || count == 0) {
            throw new ValidationException("BL not Approved");
        }
    }

    private void validateDemurrageTotals(SalesInvoiceShippingCreateDTO dto) {
        String demChargeCode = getDemurrageChargeCode(getGroupPoid());

        BigDecimal totalDemCharge = BigDecimal.ZERO;
        if (dto.getChargesDetails() != null && demChargeCode != null) {
            for (SalesInvoiceChargesDtlDto charge : dto.getChargesDetails()) {
                if ("Y".equals(charge.getAmountSelect()) &&
                        demChargeCode.equals(charge.getChargePoid().toString()) &&
                        charge.getAmount() != null) {
                    totalDemCharge = totalDemCharge.add(charge.getAmount());
                }
            }
        }

        BigDecimal totalContainerDem = dto.getContainerDetails() != null ? dto.getContainerDetails().stream()
                .map(c -> c.getDmChargeAmt() != null ? c.getDmChargeAmt() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        if (totalDemCharge.compareTo(totalContainerDem) != 0) {
            throw new ValidationException("Demurrage total(" + totalContainerDem + ") not match with Charge Demurrage total (" + totalDemCharge + ")");
        }
    }

    private void validateDemurrageTotalsForUpdate(SalesInvoiceShippingUpdateDTO dto, ArShSalesInvoiceHdr existing) {
        BigDecimal totalDemCharge = BigDecimal.ZERO;
        BigDecimal totalContainerDem = BigDecimal.ZERO;

        if (dto.getChargesDetails() == null || dto.getChargesDetails().isEmpty()) {
            List<ArShSalesInvoiceChargDtl> existingCharges = chargDtlRepository
                    .findByTransactionPoidOrderByDetRowId(existing.getTransactionPoid());
            for (ArShSalesInvoiceChargDtl charge : existingCharges) {
                // Check if this is a demurrage charge
                String demChargeCode = getDemurrageChargeCode(existing.getGroupPoid());
                if (demChargeCode != null && demChargeCode.equals(charge.getChargePoid().toString())) {
                    if ("Y".equals(charge.getAmountSelect()) && charge.getAmount() != null) {
                        totalDemCharge = totalDemCharge.add(charge.getAmount());
                    }
                }
            }
        } else {
            for (SalesInvoiceChargesDtlDto charge : dto.getChargesDetails()) {
                if ("Y".equals(charge.getAmountSelect()) && charge.getAmount() != null) {
                    String demChargeCode = getDemurrageChargeCode(existing.getGroupPoid());
                    if (demChargeCode != null && demChargeCode.equals(charge.getChargePoid().toString())) {
                        totalDemCharge = totalDemCharge.add(charge.getAmount());
                    }
                }
            }
        }

        if (dto.getContainerDetails() == null || dto.getContainerDetails().isEmpty()) {
            List<ArShSalesInvoiceContnrDtl> existingContainers = contnrDtlRepository
                    .findByTransactionPoidOrderByDetRowId(existing.getTransactionPoid());
            for (ArShSalesInvoiceContnrDtl container : existingContainers) {
                if (container.getDmChargeAmt() != null) {
                    totalContainerDem = totalContainerDem.add(container.getDmChargeAmt());
                }
            }
        } else {
            for (SalesInvoiceContainerDtlDto container : dto.getContainerDetails()) {
                if (container.getDmChargeAmt() != null) {
                    totalContainerDem = totalContainerDem.add(container.getDmChargeAmt());
                }
            }
        }

        if (!"EXPORT".equalsIgnoreCase(existing.getBlTypeInvoice())) {
            if (totalDemCharge.compareTo(totalContainerDem) != 0) {
                throw new ValidationException("Demurrage total(" + totalContainerDem +
                        ") not match with Charge Demurrage total (" + totalDemCharge + ")");
            }
        }
    }

    /**
     * Get demurrage charge code from GLOBAL_PARAMETERS
     */
    private String getDemurrageChargeCode(Long groupPoid) {
        String sql = "SELECT PARAMETER_VALUE FROM GLOBAL_PARAMETERS " +
                "WHERE PARAMETER_KEYID_TYPE='SHDEMURRAGE' AND GROUP_POID=? AND ROWNUM=1";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, groupPoid);
        } catch (Exception e) {
            log.warn("Could not get demurrage charge code", e);
            return null;
        }
    }

    /**
     * Validate gain/loss for Sales Invoice
     * <p>
     * IMPORTANT: InvAmount is CALCULATED, not an input field from frontend.
     * - InvAmount = Sum of all selected charge amounts (where amountSelect = 'Y')
     * - This is calculated in calculateInvoiceAmount() method and set after save
     * - Frontend should NOT send InvAmount in the DTO - it will be calculated automatically
     * <p>
     * Based on legacy code logic (shipArInvoiceDemurrage.java DocumentBeforeSave):
     * 1. Loop through all charges where amountSelect = 'Y'
     * 2. Sum their amounts to get FTotalRcpAmount (which becomes InvAmount)
     * 3. For gain/loss calculation, only include charges where ChargeType = 'MANIFEST'
     * 4. Gain/Loss = Amount - BuyAmount for each MANIFEST charge
     * 5. If totalGainLoss < 0 AND totalInvoiceAmount >= 0, user must have special rights (doc 000-210)
     */
    private void validateGainLoss(SalesInvoiceShippingCreateDTO dto) {
        // Calculate total invoice amount from selected charges
        // InvAmount = Sum of all selected charge amounts (where amountSelect = 'Y')
        // This matches legacy: FTotalRcpAmount = sum of all selected charge amounts
        BigDecimal totalInvoiceAmount = BigDecimal.ZERO;
        BigDecimal totalGainLoss = BigDecimal.ZERO;

        if (dto.getChargesDetails() != null) {
            for (SalesInvoiceChargesDtlDto charge : dto.getChargesDetails()) {
                if ("Y".equals(charge.getAmountSelect())) {
                    // Add to total invoice amount (InvAmount calculation)
                    BigDecimal amount = charge.getAmount() != null ? charge.getAmount() : BigDecimal.ZERO;
                    totalInvoiceAmount = totalInvoiceAmount.add(amount);

                    // Calculate gain/loss only for MANIFEST charges
                    // This matches legacy: if (Chargetype.equals("MANIFEST"))
                    // Gain_Loss = Amount - BuyAmount (as per view definition)
                    if ("MANIFEST".equals(charge.getChargeType())) {
                        BigDecimal buyAmount = charge.getBuyAmount() != null ? charge.getBuyAmount() : BigDecimal.ZERO;
                        BigDecimal gainLoss = amount.subtract(buyAmount);
                        totalGainLoss = totalGainLoss.add(gainLoss);
                    }
                }
            }
        }

        // Validation: If invoice is in loss (negative gain/loss) and total invoice amount is non-negative,
        // user must have proper rights to save (document 000-210 with VIEW action)
        // This matches legacy: if (totalGainLoss.compareTo(new BigDecimal(0)) == -1 && TotalRcpAmount.compareTo(new BigDecimal(0)) != -1)
        if (totalGainLoss.compareTo(BigDecimal.ZERO) < 0 && totalInvoiceAmount.compareTo(BigDecimal.ZERO) >= 0) {
            // Check if user has rights for document 000-210 with VIEW action
            // Note: This would typically be checked via UserContext or security service
            // For now, we'll throw a validation exception that can be handled by the controller
            throw new ValidationException("Invoice is in Loss (excluded LOCAL charges): " + totalGainLoss +
                    ". User does not have sufficient rights to save invoice with loss.");
        }
    }

    private void validateImcoChargeCustomer(SalesInvoiceShippingCreateDTO dto, Long companyPoid) {
        String sql = "SELECT SUM(DECODE(PARAMETER_KEYID_TYPE,'SHDEMURRAGE',PARAMETER_VALUE,0)) DEM, " +
                "SUM(DECODE(PARAMETER_KEYID_TYPE,'CHARGE_IMCO_POID',PARAMETER_VALUE,0)) IMCO, " +
                "SUM(DECODE(PARAMETER_KEYID_TYPE,'CAN_GL_CASHAC_SHIPPING',PARAMETER_VALUE,0)) CASH_CUSTOMER " +
                "FROM GLOBAL_PARAMETERS " +
                "WHERE PARAMETER_KEYID_TYPE IN ('SHDEMURRAGE','CHARGE_IMCO_POID','CAN_GL_CASHAC_SHIPPING')";

        Map<String, Object> params = jdbcTemplate.queryForMap(sql);
        String imcoChargePoid = params.get("IMCO") != null ? params.get("IMCO").toString() : null;
        String cashCustomerPoid = params.get("CASH_CUSTOMER") != null ? params.get("CASH_CUSTOMER").toString() : null;

        if (imcoChargePoid != null && dto.getChargesDetails() != null) {
            boolean hasImcoCharge = dto.getChargesDetails().stream()
                    .anyMatch(c -> imcoChargePoid.equals(c.getChargePoid().toString()) && "Y".equals(c.getAmountSelect()));

            if (hasImcoCharge && cashCustomerPoid != null && !cashCustomerPoid.equals(dto.getCustomerPoid().toString())) {
                throw new ValidationException("Invoice contain, Imco charge always cash customer");
            }
        }
    }

    private void validateTransactionPeriod(Long companyPoid, LocalDate transactionDate) {
        String sql = "SELECT FUNC_GLOB_TRANSACTN_YEAR_VALID(?, ?) FROM DUAL";
        String result = jdbcTemplate.queryForObject(sql, String.class, companyPoid, java.sql.Date.valueOf(transactionDate));
        if (result != null && result.contains("ERROR")) {
            throw new ValidationException("Transaction date can not update");
        }
    }

    // ==================== Auto-Populate Helper Methods ====================

    private void autoPopulateInvoiceTo(ArShSalesInvoiceHdr entity, Long companyPoid) {
        String sql = "SELECT DISTINCT 'X', TIN_NUMBER FROM SHIP_PRINCIPAL_MASTER " +
                "INNER JOIN GL_MASTER GLMAST ON GLMAST.GL_POID = SHIP_PRINCIPAL_MASTER.GL_CODE_POID " +
                "WHERE UPPER(PRINCIPAL_POID) = UPPER(?)";
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(sql, entity.getCustomerPoid());
            entity.setInvoiceTo("P");
            entity.setTinNumber((String) result.get("TIN_NUMBER"));
        } catch (Exception e) {
            entity.setInvoiceTo("C");
        }

        if (entity.getBlPoid() != null) {
            String bookingSql = "SELECT DISTINCT BOOKING_PARTY_POID FROM SHIP_BL_MANIFEST_HDR WHERE TRANSACTION_POID = ?";
            try {
                Long bookingPartyPoid = jdbcTemplate.queryForObject(bookingSql, Long.class, entity.getBlPoid());
                if (bookingPartyPoid != null && bookingPartyPoid.equals(entity.getCustomerPoid())) {
                    entity.setInvoiceTo("C");
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        if ("C".equals(entity.getInvoiceTo())) {
            String customerSql = "SELECT MAX(NVL(CREDIT_PERIOD, 0)), MIN(TIN_NUMBER) " +
                    "FROM SALES_CUSTOMER_MASTER WHERE CUSTOMER_POID = ?";
            try {
                Map<String, Object> customerResult = jdbcTemplate.queryForMap(customerSql, entity.getCustomerPoid());
                entity.setTinNumber((String) customerResult.get("TIN_NUMBER"));
                int creditPeriod = ((Number) customerResult.get("CREDIT_PERIOD")).intValue();
                if (entity.getCreditDays() == null && creditPeriod > 0) {
                    entity.setCreditDays(creditPeriod);
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    // ==================== LOV Enrichment ====================

/*
    private void enrichLovData(SalesInvoiceShippingDto dto) {

        try {
            if (dto.getGroupPoid() != null) {
                dto.setGroupDet(lovService.getDetailsByPoidAndLovName(dto.getGroupPoid(), "GROUP"));
            }
            if (dto.getCompanyPoid() != null) {
                dto.setCompanyDet(lovService.getDetailsByPoidAndLovName(dto.getCompanyPoid(), "COMPANY"));
            }
            if (dto.getCustomerPoid() != null) {
                dto.setCustomerDet(lovService.getDetailsByPoidAndLovName(dto.getCustomerPoid(), "CUSTOMER_MASTER_PRINCIPAL"));
            }
            if (dto.getCurrencyCode() != null) {
                dto.setCurrencyDet(lovService.getDetailsByCodeAndLovName(dto.getCurrencyCode(), "CURRENCY"));
            }
            if (dto.getBlPoid() != null) {
                dto.setBlDet(lovService.getDetailsByPoidAndLovName(dto.getBlPoid(), "ALLBLNUMBER"));
            }
            if (dto.getBlTypeInvoice() != null) {
                dto.setBlTypeInvoiceDet(lovService.getDetailsByCodeAndLovName(dto.getBlTypeInvoice(), "BL_TYPE_EXPORT"));
            }
            if (dto.getBookingPartyPoid() != null) {
                dto.setBookingPartyDet(lovService.getDetailsByPoidAndLovName(dto.getBookingPartyPoid(), "CUSTOMER_MASTER"));
            }
            if (dto.getPrintInvoiceBankPoid() != null) {
                dto.setPrintInvoiceBankDet(lovService.getDetailsByPoidAndLovName(dto.getPrintInvoiceBankPoid(), "BANK_MASTER"));
            }

            if (dto.getChargesDetails() != null) {
                enrichChargeFields(dto.getChargesDetails());
            }

            if (dto.getContainerDetails() != null) {
                enrichContainerFields(dto.getContainerDetails());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch LOV data for Invoice ID: {}", dto.getTransactionPoid(), e);
        }
    }

    private void enrichContainerFields(List<SalesInvoiceContainerDtlDto> containerDtlsDto) {
        try {
            containerDtlsDto.forEach(containerDtlDto -> {
                if (containerDtlDto.getBlPoid() != null) {
                    containerDtlDto.setBlDet(lovService.getDetailsByPoidAndLovName(containerDtlDto.getBlPoid(), "ALLBLNUMBER"));
                }
                if (containerDtlDto.getCntTaxPoid() != null) {
                    containerDtlDto.setCntTaxDet(lovService.getDetailsByPoidAndLovName(containerDtlDto.getCntTaxPoid(), "TAX_MASTER"));
                }
            });
        } catch (Exception e) {
            log.warn("Failed to fetch LOV data for container", e);
        }
    }

    private void enrichChargeFields(List<SalesInvoiceChargesDtlDto> chargesDtlsDto) {
        try {
            chargesDtlsDto.forEach(chargesDtlDto -> {
                if (chargesDtlDto.getBlPoid() != null) {
                    chargesDtlDto.setBlDet(lovService.getDetailsByPoidAndLovName(chargesDtlDto.getBlPoid(), "ALLBLNUMBER"));
                }
                if (chargesDtlDto.getChargePoid() != null) {
                    chargesDtlDto.setChargeDet(lovService.getDetailsByPoidAndLovName(chargesDtlDto.getChargePoid(), "CHARGE_MASTER"));
                }
                if (chargesDtlDto.getCurrencyCode() != null) {
                    chargesDtlDto.setCurrencyDet(lovService.getDetailsByCodeAndLovName(chargesDtlDto.getCurrencyCode(), "CURRENCY"));
                }
                if (chargesDtlDto.getTaxPoid() != null) {
                    chargesDtlDto.setTaxDet(lovService.getDetailsByPoidAndLovName(chargesDtlDto.getTaxPoid(), "TAX_MASTER"));
                }
                if (chargesDtlDto.getPrintCurrencyCode() != null) {
                    chargesDtlDto.setPrintCurrencyDet(lovService.getDetailsByCodeAndLovName(chargesDtlDto.getPrintCurrencyCode(), "CURRENCY"));
                }
            });
        } catch (Exception e) {
            log.warn("Failed to fetch LOV data for charges", e);
        }
    }
*/

    private String getBlTypeFromBlPoid(Long blPoid) {
        try {
            String sql = "SELECT BL_TYPE FROM SHIP_BL_MANIFEST_HDR WHERE TRANSACTION_POID = ?";
            return jdbcTemplate.queryForObject(sql, String.class, blPoid);
        } catch (Exception e) {
            log.warn("Failed to get BL_TYPE for BL_POID: {}", blPoid, e);
            return "IMPORT";
        }
    }

    // Helper methods for result set mapping
    private Long getLongOrNull(ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getIntegerOrNull(ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private BigDecimal getBigDecimalOrNull(ResultSet rs, String column) throws java.sql.SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return rs.wasNull() ? null : value;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBillCompany(Long blPoid, Long customerPoid) {
        log.info("Getting bill company for BL POID: {}, Customer POID: {}", blPoid, customerPoid);
        Long groupPoid = getGroupPoid();
        Long companyPoid = getCompanyPoid();
        Long userPoid = getUserPoid();
        try {
            String sql = "{call PROC_SH_GET_CUS_BILL_COMPANY(?, ?, ?, ?, ?, ?)}";
            String billCompanyPoidStr = jdbcTemplate.execute(sql, (CallableStatement cs) -> {
                cs.setLong(1, groupPoid);
                cs.setLong(2, companyPoid);
                cs.setLong(3, userPoid);
                cs.setLong(4, blPoid);
                cs.setLong(5, customerPoid);
                cs.registerOutParameter(6, Types.VARCHAR);
                cs.execute();
                return cs.getString(6);
            });

            if (billCompanyPoidStr != null && !billCompanyPoidStr.trim().isEmpty()) {
                try {
                    return new BigDecimal(billCompanyPoidStr.trim());
                } catch (NumberFormatException e) {
                    log.warn("Invalid bill company POID format: {}", billCompanyPoidStr);
                }
            }
            return companyPoid != null ? new BigDecimal(companyPoid) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Error calling PROC_SH_GET_CUS_BILL_COMPANY", e);
            throw new ValidationException("Error getting bill company: " + e.getMessage());
        }
    }

    private LocalDate getLocalDateOrNull(ResultSet rs, String column) throws java.sql.SQLException {
        java.sql.Date date = rs.getDate(column);
        return date != null ? date.toLocalDate() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getManifestDetails(Long blPoid) {
        log.info("Getting manifest details for BL POID: {}", blPoid);

        try {
            // Get BL Type from SHIP_BL_MANIFEST_HDR
            String query = "SELECT BL_TYPE FROM SHIP_BL_MANIFEST_HDR WHERE TRANSACTION_POID = ?";

            String blType = jdbcTemplate.queryForObject(query, String.class, blPoid);

            String documentId;
            String docname;

            if ("EXPORT".equalsIgnoreCase(blType)) {
                documentId = "100-104";
                docname = "Export Manifest - BL";
            } else {
                documentId = "100-102";
                docname = "Import Manifest - BL";
            }

            Map<String, Object> response = new HashMap<>();
            response.put("documentId", documentId);
            response.put("docname", docname);

            log.info("Successfully retrieved manifest details for BL POID: {}, DocumentId: {}, Docname: {}",
                    blPoid, documentId, docname);

            return response;

        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.error("BL not found with POID: {}", blPoid);
            throw new ValidationException("BL not found with POID: " + blPoid);
        } catch (Exception e) {
            log.error("Error getting manifest details for BL POID: {}", blPoid, e);
            throw new RuntimeException("Failed to get manifest details: " + e.getMessage());
        }
    }

}