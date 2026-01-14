package com.asg.shipping.salesinvoice.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.shipping.salesinvoice.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Service interface for Sales Invoice Shipping operations
 */
public interface SalesInvoiceShippingService {

    Map<String, Object> searchSalesInvoice(String docId, FilterRequestDto request, Pageable pageable);

    SalesInvoiceShippingDto getSalesInvoice(Long id);

    SalesInvoiceShippingDto createSalesInvoice(SalesInvoiceShippingCreateDTO createDTO);

    SalesInvoiceShippingDto updateSalesInvoice(Long id, SalesInvoiceShippingUpdateDTO updateDTO);

    void deleteSalesInvoice(Long id);

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

    LoadPrintDataResponseDTO loadPrintData(Long id, LoadPrintDataRequestDTO request);
}

