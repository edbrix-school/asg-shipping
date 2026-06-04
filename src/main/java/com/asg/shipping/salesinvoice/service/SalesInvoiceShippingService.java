package com.asg.shipping.salesinvoice.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.salesinvoice.dto.*;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service interface for Sales Invoice Shipping operations
 */
public interface SalesInvoiceShippingService {

    Map<String, Object> searchSalesInvoice(String docId, FilterRequestDto request, Pageable pageable, String startDate, String endDate);

    SalesInvoiceShippingDto getSalesInvoice(Long id);

    SalesInvoiceShippingDto createSalesInvoice(SalesInvoiceShippingCreateDTO createDTO);

    SalesInvoiceShippingDto updateSalesInvoice(Long id, SalesInvoiceShippingUpdateDTO updateDTO);

    void deleteSalesInvoice(Long id, DeleteReasonDto deleteReasonDto);

    LoadContainerDemurrageResponseDTO loadContainerDemurrageData(Long id, LoadContainerDemurrageRequestDTO request);

    LoadChargeDataResponseDTO loadChargeData(Long id, LoadChargeDataRequestDTO request);

    ValidateCustomerResponseDTO validateCustomer(ValidateCustomerRequestDTO request);

    void verifyInvoice(Long id);

    CreateFFJobResponseDTO createFFJob(Long id, Long blPoid);

    CreateFFPurchaseJournalResponseDTO createFFPurchaseJournal(Long id, Long ffJobPoid);

    String updateBookingParty(Long id, UpdateBookingPartyRequestDTO request);

    CustomerAddressResponseDTO getCustomerAddress(Long addressMasterPoid, String addressType);

    LoadBlDataResponseDTO loadBlData(Long id, LoadBlDataRequestDTO request);

    GetBillCompanyResponseDTO getBillCompany(GetBillCompanyRequestDTO request);

    BigDecimal getBillCompany(Long blPoid, Long customerPoid);

    LoadPrintDataResponseDTO loadPrintData(Long id, LoadPrintDataRequestDTO request);

    byte[] printInvoice(Long transactionPoid,Long blPoid) throws Exception;

    byte[] printCustomerAutoCharge(Long transactionPoid, Long blPoid) throws Exception;

    byte[] print(Long transactionPoid, Long blPoid) throws Exception;

    /**
     * Get manifest details based on BL POID - opens Export or Import Manifest
     * @param blPoid BL POID to get manifest for
     * @return Map containing manifest type and redirect information
     */
    Map<String, Object> getManifestDetails(Long blPoid);
}

