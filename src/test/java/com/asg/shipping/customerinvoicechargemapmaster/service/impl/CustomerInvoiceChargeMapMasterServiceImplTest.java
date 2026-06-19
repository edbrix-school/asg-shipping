package com.asg.shipping.customerinvoicechargemapmaster.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
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
    private DocumentDeleteService documentDeleteService;

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

        assertThrows(IllegalArgumentException.class, () -> service.saveOrUpdate(request, 2L));
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

        CustomerInvoicePrtMasterEntity created = new CustomerInvoicePrtMasterEntity();
        created.setCustomerPoid(1L);
        created.setGroupPoid(2L);
        created.setDeleted("N");
        // first call: Optional.empty() triggers orElseGet -> createMaster
        // second call (inside getByCustomer): returns the created entity
        when(masterRepo.findById(1L)).thenReturn(Optional.empty(), Optional.of(created));
        when(masterRepo.save(any(CustomerInvoicePrtMasterEntity.class))).thenReturn(created);

        when(detailRepo.findMaxDetRowId(1L)).thenReturn(null); // => detRowId should become 1

        when(detailRepo.findById(any(CustomerInvoicePrtDtlId.class))).thenReturn(Optional.empty());
        when(detailRepo.save(any(CustomerInvoicePrtDtlEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerInvoicePrtDtlEntity savedDetail = new CustomerInvoicePrtDtlEntity();
        CustomerInvoicePrtDtlId savedId = new CustomerInvoicePrtDtlId();
        savedId.setCustomerPoid(1L);
        savedId.setDetRowId(1L);
        savedDetail.setId(savedId);
        savedDetail.setChargePoid(100L);
        savedDetail.setLineChargeDescription("Charge");
        savedDetail.setValidUntil(LocalDate.of(2026, 12, 31));
        when(detailRepo.findByIdCustomerPoid(1L)).thenReturn(List.of(savedDetail));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            service.saveOrUpdate(request, 2L);
        }

        verify(loggingService).createLogSummaryEntry(LogDetailsEnum.CREATED, "DOC123", "1");
        verify(detailRepo).save(argThat(entity -> entity.getId() != null
                && entity.getId().getCustomerPoid().equals(1L)
                && entity.getId().getDetRowId().equals(1L)
                && entity.getChargePoid().equals(100L)));
        verify(loggingService).createLogSummaryEntry(
                eq("DOC123"),
                eq("1"),
                contains("Row Created on Charge Detail with detRowId:"));
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

        CustomerInvoicePrtDtlEntity savedDetail = new CustomerInvoicePrtDtlEntity();
        CustomerInvoicePrtDtlId savedId = new CustomerInvoicePrtDtlId();
        savedId.setCustomerPoid(1L);
        savedId.setDetRowId(5L);
        savedDetail.setId(savedId);
        savedDetail.setChargePoid(100L);
        savedDetail.setLineChargeDescription("Charge");
        savedDetail.setValidUntil(LocalDate.of(2026, 12, 31));
        when(detailRepo.findByIdCustomerPoid(1L)).thenReturn(List.of(savedDetail));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            service.saveOrUpdate(request, 2L);
        }

        verify(loggingService).logChanges(
                any(CustomerInvoicePrtMasterEntity.class),
                any(CustomerInvoicePrtMasterEntity.class),
                eq(CustomerInvoicePrtMasterEntity.class),
                eq("DOC123"),
                eq("1"),
                eq(LogDetailsEnum.MODIFIED),
                eq("CUSTOMER_POID"));
    }

    @Test
    void deleteDetail_success_callsDocumentDeleteService() {
        Long customerPoid = 1L;
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();

        CustomerInvoicePrtMasterEntity master = new CustomerInvoicePrtMasterEntity();
        master.setCustomerPoid(customerPoid);
        when(masterRepo.findById(customerPoid)).thenReturn(Optional.of(master));

        service.deleteDetail(customerPoid, deleteReasonDto);

        verify(documentDeleteService).deleteDocument(
                eq(customerPoid),
                eq("CUSTOMER_INVOICE_PRT_MASTER"),
                eq("CUSTOMER_POID"),
                eq(deleteReasonDto),
                isNull()
        );
    }

    @Test
    void deleteDetail_masterNotFound_throwsResourceNotFound() {
        Long customerPoid = 99L;

        when(masterRepo.findById(customerPoid)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.deleteDetail(customerPoid, new DeleteReasonDto()));
        verify(documentDeleteService, never()).deleteDocument(any(), any(), any(), any(), any());
    }

    @Test
    void saveOrUpdate_isDeletedActionType_deletesDetailAndSkipsSave() {
        CustomerInvoiceChargeMapMasterRequest request = new CustomerInvoiceChargeMapMasterRequest();
        request.setCustomerPoid(1L);

        CustomerInvoiceChargeMapDetailDto deleteDto = new CustomerInvoiceChargeMapDetailDto();
        deleteDto.setDetRowId(1L);
        deleteDto.setChargePoid(701L);
        deleteDto.setLineChargeDescription("vida");
        deleteDto.setActionType("isDeleted");

        CustomerInvoiceChargeMapDetailDto keepDto = new CustomerInvoiceChargeMapDetailDto();
        keepDto.setDetRowId(2L);
        keepDto.setChargePoid(4771L);
        keepDto.setLineChargeDescription("DER");
        keepDto.setActionType("noChange"); // should be skipped

        request.setDetails(List.of(deleteDto, keepDto));

        CustomerInvoicePrtMasterEntity master = new CustomerInvoicePrtMasterEntity();
        master.setCustomerPoid(1L);
        master.setDeleted("N");

        when(masterRepo.existsById(1L)).thenReturn(true);
        when(masterRepo.findById(1L)).thenReturn(Optional.of(master));

        CustomerInvoicePrtDtlEntity existingDetail = new CustomerInvoicePrtDtlEntity();
        CustomerInvoicePrtDtlId deleteId = new CustomerInvoicePrtDtlId();
        deleteId.setCustomerPoid(1L);
        deleteId.setDetRowId(1L);
        existingDetail.setId(deleteId);

        when(detailRepo.findById(argThat(id -> id.getDetRowId().equals(1L)))).thenReturn(Optional.of(existingDetail));

        CustomerInvoicePrtDtlId keepId = new CustomerInvoicePrtDtlId();
        keepId.setCustomerPoid(1L);
        keepId.setDetRowId(2L);
        CustomerInvoicePrtDtlEntity remainingDetail = new CustomerInvoicePrtDtlEntity();
        remainingDetail.setId(keepId);
        remainingDetail.setChargePoid(4771L);
        remainingDetail.setLineChargeDescription("DER");
        when(detailRepo.findByIdCustomerPoid(1L)).thenReturn(List.of(remainingDetail));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            CustomerInvoiceChargeMapMasterResponse response = service.saveOrUpdate(request, 2L);

            assertEquals(1, response.getDetails().size());
            assertEquals(4771L, response.getDetails().get(0).getChargePoid());
        }

        verify(detailRepo).delete(existingDetail);
        verify(detailRepo, never()).save(any()); // noChange row skipped, nothing saved
    }

    @Test
    void saveOrUpdate_customerChanged_deletesOldAndCreatesNew() {
        Long oldCustomerPoid = 5933L;
        Long newCustomerPoid = 5928L;

        CustomerInvoiceChargeMapMasterRequest request = new CustomerInvoiceChargeMapMasterRequest();
        request.setOldCustomerPoid(oldCustomerPoid);
        request.setCustomerPoid(newCustomerPoid);

        CustomerInvoiceChargeMapDetailDto detailDto = new CustomerInvoiceChargeMapDetailDto();
        detailDto.setDetRowId(null);
        detailDto.setChargePoid(4771L);
        detailDto.setLineChargeDescription("Test");
        detailDto.setActionType("CREATE");
        request.setDetails(List.of(detailDto));

        // old master details to delete
        CustomerInvoicePrtDtlEntity oldDetail = buildDetail(oldCustomerPoid, 1L, 701L, "old");
        when(detailRepo.findByIdCustomerPoid(oldCustomerPoid)).thenReturn(List.of(oldDetail));

        CustomerInvoicePrtMasterEntity oldMaster = new CustomerInvoicePrtMasterEntity();
        oldMaster.setCustomerPoid(oldCustomerPoid);
        when(masterRepo.findById(oldCustomerPoid)).thenReturn(Optional.of(oldMaster));

        // new master
        when(masterRepo.existsById(newCustomerPoid)).thenReturn(false);
        CustomerInvoicePrtMasterEntity newMaster = new CustomerInvoicePrtMasterEntity();
        newMaster.setCustomerPoid(newCustomerPoid);
        newMaster.setDeleted("N");
        when(masterRepo.findById(newCustomerPoid)).thenReturn(Optional.empty(), Optional.of(newMaster));
        when(masterRepo.save(any())).thenReturn(newMaster);

        when(detailRepo.findMaxDetRowId(newCustomerPoid)).thenReturn(null);
        when(detailRepo.findById(any(CustomerInvoicePrtDtlId.class))).thenReturn(Optional.empty());
        when(detailRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerInvoicePrtDtlEntity savedDetail = buildDetail(newCustomerPoid, 1L, 4771L, "Test");
        when(detailRepo.findByIdCustomerPoid(newCustomerPoid)).thenReturn(List.of(savedDetail));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            CustomerInvoiceChargeMapMasterResponse response = service.saveOrUpdate(request, 1L);

            assertEquals(newCustomerPoid, response.getCustomerPoid());
            assertEquals(1, response.getDetails().size());
            assertEquals(4771L, response.getDetails().get(0).getChargePoid());
        }

        verify(detailRepo).deleteAll(List.of(oldDetail));
        verify(masterRepo).delete(oldMaster);
        verify(masterRepo).save(argThat(m -> m.getCustomerPoid().equals(newCustomerPoid)));
    }

    @Test
    void saveOrUpdate_customerNotChanged_noOldRecordDeleted() {
        CustomerInvoiceChargeMapMasterRequest request = new CustomerInvoiceChargeMapMasterRequest();
        request.setOldCustomerPoid(1L);
        request.setCustomerPoid(1L); // same — no change

        CustomerInvoiceChargeMapDetailDto detailDto = new CustomerInvoiceChargeMapDetailDto();
        detailDto.setDetRowId(5L);
        detailDto.setChargePoid(100L);
        detailDto.setLineChargeDescription("Charge");
        detailDto.setActionType("UPDATE");
        request.setDetails(List.of(detailDto));

        CustomerInvoicePrtMasterEntity master = new CustomerInvoicePrtMasterEntity();
        master.setCustomerPoid(1L);
        master.setDeleted("N");

        when(masterRepo.existsById(1L)).thenReturn(true);
        when(masterRepo.findById(1L)).thenReturn(Optional.of(master));
        when(detailRepo.findById(any())).thenReturn(Optional.of(buildDetail(1L, 5L, 100L, "Charge")));
        when(detailRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(detailRepo.findByIdCustomerPoid(1L)).thenReturn(List.of(buildDetail(1L, 5L, 100L, "Charge")));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            service.saveOrUpdate(request, 2L);
        }

        verify(detailRepo, never()).deleteAll(anyList());
        verify(masterRepo, never()).delete(any(CustomerInvoicePrtMasterEntity.class));
    }

    @Test
    void saveOrUpdate_isDeletedActionType_detailNotFound_skipsDeleteGracefully() {
        CustomerInvoiceChargeMapMasterRequest request = new CustomerInvoiceChargeMapMasterRequest();
        request.setCustomerPoid(1L);

        CustomerInvoiceChargeMapDetailDto deleteDto = new CustomerInvoiceChargeMapDetailDto();
        deleteDto.setDetRowId(99L);
        deleteDto.setChargePoid(701L);
        deleteDto.setActionType("isDeleted");
        request.setDetails(List.of(deleteDto));

        CustomerInvoicePrtMasterEntity master = new CustomerInvoicePrtMasterEntity();
        master.setCustomerPoid(1L);
        master.setDeleted("N");

        when(masterRepo.existsById(1L)).thenReturn(true);
        when(masterRepo.findById(1L)).thenReturn(Optional.of(master));
        when(detailRepo.findById(any(CustomerInvoicePrtDtlId.class))).thenReturn(Optional.empty());
        when(detailRepo.findByIdCustomerPoid(1L)).thenReturn(List.of(
                buildDetail(1L, 1L, 4771L, "DER")
        ));

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");
            service.saveOrUpdate(request, 2L);
        }

        verify(detailRepo, never()).delete(any(CustomerInvoicePrtDtlEntity.class));
        verify(detailRepo, never()).save(any());
    }

    private CustomerInvoicePrtDtlEntity buildDetail(Long customerPoid, Long detRowId, Long chargePoid, String desc) {
        CustomerInvoicePrtDtlEntity e = new CustomerInvoicePrtDtlEntity();
        CustomerInvoicePrtDtlId id = new CustomerInvoicePrtDtlId();
        id.setCustomerPoid(customerPoid);
        id.setDetRowId(detRowId);
        e.setId(id);
        e.setChargePoid(chargePoid);
        e.setLineChargeDescription(desc);
        return e;
    }
}

