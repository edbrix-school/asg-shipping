package com.asg.shipping.customerinvoicechargemapmaster.service.impl;

import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapDetailDto;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterRequest;
import com.asg.shipping.customerinvoicechargemapmaster.dto.CustomerInvoiceChargeMapMasterResponse;
import com.asg.shipping.customerinvoicechargemapmaster.entity.CustomerInvoicePrtDtlEntity;
import com.asg.shipping.customerinvoicechargemapmaster.entity.CustomerInvoicePrtDtlId;
import com.asg.shipping.customerinvoicechargemapmaster.entity.CustomerInvoicePrtMasterEntity;
import com.asg.shipping.customerinvoicechargemapmaster.repository.CustomerInvoicePrtDtlRepository;
import com.asg.shipping.customerinvoicechargemapmaster.repository.CustomerInvoicePrtMasterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerInvoiceChargeMapMasterServiceImplTest {

    @Mock
    private CustomerInvoicePrtMasterRepository masterRepo;

    @Mock
    private CustomerInvoicePrtDtlRepository detailRepo;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private CustomerInvoiceChargeMapMasterServiceImpl service;

    @Test
    void getByCustomer_success_returnsMappedResponse() {
        Long customerPoid = 1L;
        Long groupPoid = 2L;

        CustomerInvoicePrtMasterEntity master = new CustomerInvoicePrtMasterEntity();
        master.setCustomerPoid(customerPoid);
        master.setGroupPoid(groupPoid);
        master.setDeleted("N");

        CustomerInvoicePrtDtlEntity detail = new CustomerInvoicePrtDtlEntity();
        CustomerInvoicePrtDtlId id = new CustomerInvoicePrtDtlId();
        id.setCustomerPoid(customerPoid);
        id.setDetRowId(10L);
        detail.setId(id);
        detail.setChargePoid(100L);
        detail.setLineChargeDescription("Charge");
        detail.setValidUntil(LocalDate.of(2026, 12, 31));

        when(masterRepo.findById(customerPoid)).thenReturn(Optional.of(master));
        when(detailRepo.findByIdCustomerPoid(customerPoid)).thenReturn(List.of(detail));

        CustomerInvoiceChargeMapMasterResponse response = service.getByCustomer(customerPoid, groupPoid);

        assertNotNull(response);
        assertEquals(customerPoid, response.getCustomerPoid());
        assertEquals(1, response.getDetails().size());
        assertEquals(10L, response.getDetails().get(0).getDetRowId());
    }

    @Test
    void getByCustomer_masterDeleted_throwsRuntime() {
        Long customerPoid = 1L;

        CustomerInvoicePrtMasterEntity master = new CustomerInvoicePrtMasterEntity();
        master.setCustomerPoid(customerPoid);
        master.setDeleted("Y");

        when(masterRepo.findById(customerPoid)).thenReturn(Optional.of(master));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getByCustomer(customerPoid, 2L));
        assertTrue(ex.getMessage().contains("Customer invoice charge mapping not found"));
    }

    @Test
    void getByCustomer_detailsEmpty_throwsRuntime() {
        Long customerPoid = 1L;

        CustomerInvoicePrtMasterEntity master = new CustomerInvoicePrtMasterEntity();
        master.setCustomerPoid(customerPoid);
        master.setDeleted("N");

        when(masterRepo.findById(customerPoid)).thenReturn(Optional.of(master));
        when(detailRepo.findByIdCustomerPoid(customerPoid)).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getByCustomer(customerPoid, 2L));
        assertTrue(ex.getMessage().contains("Customer invoice charge mapping not found"));
    }

    @Test
    void saveOrUpdate_whenDetailsEmpty_throwsIllegalArgument() {
        CustomerInvoiceChargeMapMasterRequest request = new CustomerInvoiceChargeMapMasterRequest();
        request.setCustomerPoid(1L);
        request.setDetails(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.saveOrUpdate(request, 2L, "user1"));
        verifyNoInteractions(masterRepo);
    }

    @Test
    void saveOrUpdate_newRecord_callsCreateLogAndSavesDetails() {
        CustomerInvoiceChargeMapMasterRequest request = new CustomerInvoiceChargeMapMasterRequest();
        request.setCustomerPoid(1L);

        CustomerInvoiceChargeMapDetailDto detailDto = new CustomerInvoiceChargeMapDetailDto();
        detailDto.setDetRowId(null); // covers branch where detRowId is auto-generated
        detailDto.setChargePoid(100L);
        detailDto.setLineChargeDescription("Charge");
        detailDto.setValidUntil(LocalDate.of(2026, 12, 31));
        request.setDetails(List.of(detailDto));

        when(masterRepo.existsById(1L)).thenReturn(false);
        when(masterRepo.findById(1L)).thenReturn(Optional.empty());

        CustomerInvoicePrtMasterEntity created = new CustomerInvoicePrtMasterEntity();
        created.setCustomerPoid(1L);
        created.setGroupPoid(2L);
        created.setDeleted("N");
        when(masterRepo.save(any(CustomerInvoicePrtMasterEntity.class))).thenReturn(created);

        when(detailRepo.findMaxDetRowId(1L)).thenReturn(null); // => detRowId should become 1

        when(detailRepo.findById(any(CustomerInvoicePrtDtlId.class))).thenReturn(Optional.empty());
        when(detailRepo.save(any(CustomerInvoicePrtDtlEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            service.saveOrUpdate(request, 2L, "userABC");
        }

        verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "DOC123", "1");
        verify(detailRepo).save(argThat(entity -> entity.getId() != null
                && entity.getId().getCustomerPoid().equals(1L)
                && entity.getId().getDetRowId().equals(1L)
                && entity.getChargePoid().equals(100L)));
    }

    @Test
    void saveOrUpdate_existingRecord_callsModifiedLog() {
        CustomerInvoiceChargeMapMasterRequest request = new CustomerInvoiceChargeMapMasterRequest();
        request.setCustomerPoid(1L);

        CustomerInvoiceChargeMapDetailDto detailDto = new CustomerInvoiceChargeMapDetailDto();
        detailDto.setDetRowId(5L);
        detailDto.setChargePoid(100L);
        detailDto.setLineChargeDescription("Charge");
        detailDto.setValidUntil(LocalDate.of(2026, 12, 31));
        request.setDetails(List.of(detailDto));

        CustomerInvoicePrtMasterEntity master = new CustomerInvoicePrtMasterEntity();
        master.setCustomerPoid(1L);
        master.setDeleted("N");

        when(masterRepo.existsById(1L)).thenReturn(true);
        when(masterRepo.findById(1L)).thenReturn(Optional.of(master));

        when(detailRepo.findById(any(CustomerInvoicePrtDtlId.class)))
                .thenReturn(Optional.of(new CustomerInvoicePrtDtlEntity()));
        when(detailRepo.save(any(CustomerInvoicePrtDtlEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            service.saveOrUpdate(request, 2L, "userABC");
        }

        verify(loggingService).createLogSummaryEntry(LogDetailsEnum.MODIFIED, "DOC123", "1");
    }

    @Test
    void deleteDetail_success_hardDeletes_andLogs() {
        Long customerPoid = 1L;
        Long detRowId = 10L;
        Long groupPoid = 2L;

        CustomerInvoicePrtDtlEntity entity = new CustomerInvoicePrtDtlEntity();
        CustomerInvoicePrtDtlId id = new CustomerInvoicePrtDtlId();
        id.setCustomerPoid(customerPoid);
        id.setDetRowId(detRowId);
        entity.setId(id);

        when(detailRepo.findById(any(CustomerInvoicePrtDtlId.class))).thenReturn(Optional.of(entity));
        doNothing().when(detailRepo).delete(any(CustomerInvoicePrtDtlEntity.class));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            service.deleteDetail(customerPoid, detRowId, groupPoid, "userXYZ");
        }

        verify(detailRepo).delete(entity);
        verify(loggingService).createLogSummaryEntry(LogDetailsEnum.DELETED, "DOC123", "1");

        // Last two params are (logDetail, tableName)
        verify(loggingService).createLogDetailsEntry(
                eq("DOC123"),
                eq("1"),
                eq("Detail Deleted"),
                eq("EXISTS"),
                eq("DELETED"),
                anyString(),
                eq("CUSTOMER_INVOICE_PRT_DTL")
        );
    }
}

