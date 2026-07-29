package com.asg.shipping.contractsandagreements.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementHdrDto;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementPicDtlDto;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementRenewalDto;
import com.asg.shipping.contractsandagreements.dto.ContractRenewalRequest;
import com.asg.shipping.contractsandagreements.dto.ContractRenewalResponse;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementHdr;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementPicDtl;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementRenewalEntity;
import com.asg.shipping.contractsandagreements.repository.AdminContractsAgreementPicDtlRepository;
import com.asg.shipping.contractsandagreements.repository.AdminContractsAgreementRenewalDtlRepository;
import com.asg.shipping.contractsandagreements.repository.AdminContractsAgreementsHdrRepository;
import com.asg.shipping.contractsandagreements.service.ContractsAndAgreementsValidationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractsAndAgreementsServiceImplTest {

    @Mock
    private DocumentSearchService documentSearchService;
    @Mock
    private AdminContractsAgreementsHdrRepository headerRepo;
    @Mock
    private AdminContractsAgreementRenewalDtlRepository renewalDtlRepository;
    @Mock
    private AdminContractsAgreementPicDtlRepository picDtlRepository;
    @Mock
    private DocumentDeleteService documentDeleteService;
    @Mock
    private ContractsAndAgreementsValidationService validationService;
    @Mock
    private LoggingService loggingService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ContractsAndAgreementsServiceImpl service;

    private MockedStatic<UserContext> userContextMockedStatic;

    @BeforeEach
    void setUp() {
        userContextMockedStatic = mockStatic(UserContext.class);
        userContextMockedStatic.when(UserContext::getDocumentId).thenReturn("1001");
        userContextMockedStatic.when(UserContext::getCompanyPoid).thenReturn(1L);
        userContextMockedStatic.when(UserContext::getGroupPoid).thenReturn(2L);
        userContextMockedStatic.when(UserContext::getUserName).thenReturn("testUser");
        userContextMockedStatic.when(UserContext::getTimeZoneCode).thenReturn("Asia/Kolkata");
    }

    @AfterEach
    void tearDown() {
        if (userContextMockedStatic != null) {
            userContextMockedStatic.close();
        }
    }

    @Test
    void testCreate_DuplicateName() {
        AdminContractsAgreementHdrDto dto = new AdminContractsAgreementHdrDto();
        dto.setAgreementName("Test");
        when(validationService.checkForDuplicateAgreementName("Test")).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.createContractsAndAgreements(dto));
    }

    @Test
    void testCreate_Success() {
        AdminContractsAgreementHdrDto dto = new AdminContractsAgreementHdrDto();
        dto.setAgreementName("Test");
        
        AdminContractsAgreementPicDtlDto picDto = new AdminContractsAgreementPicDtlDto();
        dto.setAgreementContentDetails(List.of(picDto));
        
        AdminContractsAgreementRenewalDto renDto = new AdminContractsAgreementRenewalDto();
        dto.setRenewalDetails(List.of(renDto));

        when(validationService.checkForDuplicateAgreementName("Test")).thenReturn(false);
        
        AdminContractsAgreementHdr savedEntity = new AdminContractsAgreementHdr();
        savedEntity.setTransactionPoid(10L);
        savedEntity.setDocRef("CA-001");
        when(headerRepo.saveAndFlush(any())).thenReturn(savedEntity);
        when(headerRepo.findById(10L)).thenReturn(Optional.of(savedEntity));
        when(picDtlRepository.findMaxDetRowId(10L)).thenReturn(0L);

        AdminContractsAgreementHdrDto result = service.createContractsAndAgreements(dto);

        assertNotNull(result);
        verify(headerRepo).saveAndFlush(any());
        verify(picDtlRepository).save(any());

        InOrder inOrder = inOrder(loggingService, picDtlRepository);
        inOrder.verify(loggingService).createLogSummaryEntry(eq("1001"), eq("10"), contains("Created"));
        inOrder.verify(picDtlRepository).save(any());
        inOrder.verify(loggingService).createLogSummaryEntry(eq("1001"), eq("10"), contains("Row Created"));
    }

    @Test
    void testGetById_NotFound() {
        when(headerRepo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getContractsAndAgreementsById(1L));
    }

    @Test
    void testUpdate_NotFound() {
        when(headerRepo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateContractsAndAgreements(1L, new AdminContractsAgreementHdrDto()));
    }

    @Test
    void testUpdate_Success() {
        AdminContractsAgreementHdr existing = new AdminContractsAgreementHdr();
        existing.setTransactionPoid(10L);
        existing.setAgreementName("OldName");

        AdminContractsAgreementHdrDto updateDto = new AdminContractsAgreementHdrDto();
        updateDto.setAgreementName("NewName");
        
        AdminContractsAgreementPicDtlDto picDelDto = new AdminContractsAgreementPicDtlDto();
        picDelDto.setDetRowId(1L);
        picDelDto.setActionType("ISDELETED");

        AdminContractsAgreementPicDtlDto picCreDto = new AdminContractsAgreementPicDtlDto();
        picCreDto.setActionType("ISCREATED");

        AdminContractsAgreementPicDtlDto picUpDto = new AdminContractsAgreementPicDtlDto();
        picUpDto.setDetRowId(2L);
        picUpDto.setActionType("ISUPDATED");

        AdminContractsAgreementPicDtlDto picNoDto = new AdminContractsAgreementPicDtlDto();
        picNoDto.setActionType("NOCHANGES");

        updateDto.setAgreementContentDetails(List.of(picDelDto, picCreDto, picUpDto, picNoDto));
        
        AdminContractsAgreementRenewalDto renDelDto = new AdminContractsAgreementRenewalDto();
        renDelDto.setDetRowId(1L);
        renDelDto.setActionType("DELETED");

        AdminContractsAgreementRenewalDto renCreDto = new AdminContractsAgreementRenewalDto();
        renCreDto.setActionType("NEW");

        AdminContractsAgreementRenewalDto renUpDto = new AdminContractsAgreementRenewalDto();
        renUpDto.setDetRowId(2L);
        renUpDto.setActionType("UPDATED");

        AdminContractsAgreementRenewalDto renNoDto = new AdminContractsAgreementRenewalDto();
        renNoDto.setActionType("NOCHANGES");

        updateDto.setRenewalDetails(List.of(renDelDto, renCreDto, renUpDto, renNoDto));

        when(headerRepo.findById(10L)).thenReturn(Optional.of(existing));
        when(validationService.checkForDuplicateAgreementName("NewName", 10L)).thenReturn(false);

        AdminContractsAgreementPicDtl picEnt = new AdminContractsAgreementPicDtl();
        when(picDtlRepository.findByIdTransactionPoidAndIdDetRowId(10L, 2L)).thenReturn(Optional.of(picEnt));

        AdminContractsAgreementHdrDto result = service.updateContractsAndAgreements(10L, updateDto);
        
        assertNotNull(result);
        verify(headerRepo).save(any());
        verify(picDtlRepository).deleteByIdTransactionPoidAndIdDetRowId(10L, 1L);
        verify(picDtlRepository).saveAll(anyList());
        verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.MODIFIED), eq("1001"), eq("10"));
        verify(loggingService, atLeastOnce()).createLogDetailsEntry(
                eq("1001"),
                eq("10"),
                eq("AGREEMENT_NAME"),
                any(),
                any(),
                anyString(),
                eq("ADMIN_CONTRACTS_AGREEMENT_HDR"));
    }

    @Test
    void testUpdate_DateFieldsLoggedOnceWithColumnNames() {
        AdminContractsAgreementHdr existing = new AdminContractsAgreementHdr();
        existing.setTransactionPoid(10L);
        existing.setAgreementName("SameName");
        existing.setReferenceDate(LocalDate.of(2026, 2, 5));
        existing.setEffectiveDate(LocalDate.of(2026, 2, 5));
        existing.setExpiryDate(LocalDate.of(2027, 2, 5));
        existing.setRenewalDueDate(LocalDate.of(2027, 1, 5));
        existing.setRenewalType("AUTO");
        existing.setRenewalCycle("YEARLY");
        existing.setAgreementType("SERVICE");
        existing.setAgreementStatus("ACTIVE");
        existing.setAgreementSource("MANUAL");
        existing.setPartyType("CUSTOMER");
        existing.setPartyPoid(1L);
        existing.setPartyContactPhone("123");

        AdminContractsAgreementHdrDto updateDto = new AdminContractsAgreementHdrDto();
        updateDto.setAgreementName("SameName");
        updateDto.setReferenceDate(LocalDate.of(2026, 3, 1));
        updateDto.setEffectiveDate(LocalDate.of(2026, 3, 1));
        updateDto.setExpiryDate(LocalDate.of(2027, 3, 1));
        updateDto.setRenewalDueDate(LocalDate.of(2027, 2, 1));
        updateDto.setRenewalType("AUTO");
        updateDto.setRenewalCycle("YEARLY");
        updateDto.setAgreementType("SERVICE");
        updateDto.setAgreementStatus("ACTIVE");
        updateDto.setAgreementSource("MANUAL");
        updateDto.setPartyType("CUSTOMER");
        updateDto.setPartyPoid(1L);
        updateDto.setPartyContactPhone("123");

        when(headerRepo.findById(10L)).thenReturn(Optional.of(existing));

        service.updateContractsAndAgreements(10L, updateDto);

        verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.MODIFIED), eq("1001"), eq("10"));
        verify(loggingService, times(1)).createLogDetailsEntry(
                eq("1001"), eq("10"), eq("REFERENCE_DATE"), any(), any(), anyString(), eq("ADMIN_CONTRACTS_AGREEMENT_HDR"));
        verify(loggingService, times(1)).createLogDetailsEntry(
                eq("1001"), eq("10"), eq("EFFECTIVE_DATE"), any(), any(), anyString(), eq("ADMIN_CONTRACTS_AGREEMENT_HDR"));
        verify(loggingService, times(1)).createLogDetailsEntry(
                eq("1001"), eq("10"), eq("EXPIRY_DATE"), any(), any(), anyString(), eq("ADMIN_CONTRACTS_AGREEMENT_HDR"));
        verify(loggingService, times(1)).createLogDetailsEntry(
                eq("1001"), eq("10"), eq("RENEWAL_DUE_DATE"), any(), any(), anyString(), eq("ADMIN_CONTRACTS_AGREEMENT_HDR"));
    }

    @Test
    void testUpdate_UnchangedDatesNotLoggedWhenOtherFieldChanges() {
        AdminContractsAgreementHdr existing = new AdminContractsAgreementHdr();
        existing.setTransactionPoid(10L);
        existing.setAgreementName("OldName");
        existing.setReferenceDate(LocalDate.of(2026, 2, 5));
        existing.setEffectiveDate(LocalDate.of(2026, 2, 5));
        existing.setExpiryDate(LocalDate.of(2027, 2, 5));
        existing.setRenewalDueDate(LocalDate.of(2027, 1, 5));
        existing.setRenewalType("AUTO");
        existing.setRenewalCycle("YEARLY");
        existing.setAgreementType("SERVICE");
        existing.setAgreementStatus("ACTIVE");
        existing.setAgreementSource("MANUAL");
        existing.setPartyType("CUSTOMER");
        existing.setPartyPoid(1L);
        existing.setPartyContactPhone("123");

        AdminContractsAgreementHdrDto updateDto = new AdminContractsAgreementHdrDto();
        updateDto.setAgreementName("NewName");
        // Same dates (and datetime-style round-trip equivalents would still map to same LocalDate)
        updateDto.setReferenceDate(LocalDate.of(2026, 2, 5));
        updateDto.setEffectiveDate(LocalDate.of(2026, 2, 5));
        updateDto.setExpiryDate(LocalDate.of(2027, 2, 5));
        updateDto.setRenewalDueDate(LocalDate.of(2027, 1, 5));
        updateDto.setRenewalType("AUTO");
        updateDto.setRenewalCycle("YEARLY");
        updateDto.setAgreementType("SERVICE");
        updateDto.setAgreementStatus("ACTIVE");
        updateDto.setAgreementSource("MANUAL");
        updateDto.setPartyType("CUSTOMER");
        updateDto.setPartyPoid(1L);
        updateDto.setPartyContactPhone("123");

        when(headerRepo.findById(10L)).thenReturn(Optional.of(existing));
        when(validationService.checkForDuplicateAgreementName("NewName", 10L)).thenReturn(false);

        service.updateContractsAndAgreements(10L, updateDto);

        verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.MODIFIED), eq("1001"), eq("10"));
        verify(loggingService, times(1)).createLogDetailsEntry(
                eq("1001"), eq("10"), eq("AGREEMENT_NAME"), any(), any(), anyString(), eq("ADMIN_CONTRACTS_AGREEMENT_HDR"));
        verify(loggingService, never()).createLogDetailsEntry(
                eq("1001"), eq("10"), eq("REFERENCE_DATE"), any(), any(), anyString(), anyString());
        verify(loggingService, never()).createLogDetailsEntry(
                eq("1001"), eq("10"), eq("EFFECTIVE_DATE"), any(), any(), anyString(), anyString());
        verify(loggingService, never()).createLogDetailsEntry(
                eq("1001"), eq("10"), eq("EXPIRY_DATE"), any(), any(), anyString(), anyString());
        verify(loggingService, never()).createLogDetailsEntry(
                eq("1001"), eq("10"), eq("RENEWAL_DUE_DATE"), any(), any(), anyString(), anyString());
    }
    
    @Test
    void testUpdate_DuplicateName() {
        AdminContractsAgreementHdr existing = new AdminContractsAgreementHdr();
        existing.setTransactionPoid(10L);
        existing.setAgreementName("OldName");

        AdminContractsAgreementHdrDto updateDto = new AdminContractsAgreementHdrDto();
        updateDto.setAgreementName("NewName");
        
        when(headerRepo.findById(10L)).thenReturn(Optional.of(existing));
        when(validationService.checkForDuplicateAgreementName("NewName", 10L)).thenReturn(true);
        
        assertThrows(ValidationException.class, () -> service.updateContractsAndAgreements(10L, updateDto));
    }

    @Test
    void testDelete() {
        AdminContractsAgreementHdr entity = new AdminContractsAgreementHdr();
        entity.setCreatedDate(LocalDateTime.now());
        when(headerRepo.findById(10L)).thenReturn(Optional.of(entity));

        DeleteReasonDto dto = new DeleteReasonDto();
        service.deleteContractsAndAgreements(10L, dto);

        verify(documentDeleteService).deleteDocument(eq(10L), any(), any(), eq(dto), any());
    }

    @Test
    void testList() {
        FilterRequestDto filters = new FilterRequestDto("AND", "N", java.util.List.of());
        Pageable pageable = mock(Pageable.class);
        
        RawSearchResult raw = mock(RawSearchResult.class);
        when(raw.records()).thenReturn(List.of(Collections.emptyMap()));
        when(raw.totalRecords()).thenReturn(1L);
        when(raw.displayFields()).thenReturn(java.util.Map.of("ID", "ID"));
        
        when(documentSearchService.search(any(), any(), any(), any(), any(), any(), any())).thenReturn(raw);

        var result = service.list(filters, pageable);
        assertNotNull(result);
    }
    
    @Test
    void testUpdatePicDtl_ResourceNotFound() {
        AdminContractsAgreementHdr existing = new AdminContractsAgreementHdr();
        existing.setTransactionPoid(10L);
        existing.setAgreementName("OldName");

        AdminContractsAgreementHdrDto updateDto = new AdminContractsAgreementHdrDto();
        updateDto.setAgreementName("OldName");
        
        AdminContractsAgreementPicDtlDto picUpDto = new AdminContractsAgreementPicDtlDto();
        picUpDto.setDetRowId(2L);
        picUpDto.setActionType("ISUPDATED");

        updateDto.setAgreementContentDetails(List.of(picUpDto));

        when(headerRepo.findById(10L)).thenReturn(Optional.of(existing));
        when(picDtlRepository.findByIdTransactionPoidAndIdDetRowId(10L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateContractsAndAgreements(10L, updateDto));
    }
    


    @Test
    void testCreate_NullLists() {
        AdminContractsAgreementHdrDto dto = new AdminContractsAgreementHdrDto();
        dto.setAgreementName("Test");
        dto.setAgreementContentDetails(null);
        dto.setRenewalDetails(Collections.emptyList());

        when(validationService.checkForDuplicateAgreementName("Test")).thenReturn(false);
        
        AdminContractsAgreementHdr savedEntity = new AdminContractsAgreementHdr();
        savedEntity.setTransactionPoid(10L);
        when(headerRepo.saveAndFlush(any())).thenReturn(savedEntity);
        when(headerRepo.findById(10L)).thenReturn(Optional.of(savedEntity));

        AdminContractsAgreementHdrDto result = service.createContractsAndAgreements(dto);

        assertNotNull(result);
        verify(picDtlRepository, never()).save(any());
        verify(renewalDtlRepository, never()).save(any());
    }

    @Test
    void testUpdate_EmptyLists() {
        AdminContractsAgreementHdr existing = new AdminContractsAgreementHdr();
        existing.setTransactionPoid(10L);
        existing.setAgreementName("OldName");

        AdminContractsAgreementHdrDto updateDto = new AdminContractsAgreementHdrDto();
        updateDto.setAgreementName("OldName");
        updateDto.setAgreementContentDetails(Collections.emptyList());
        updateDto.setRenewalDetails(null);

        when(headerRepo.findById(10L)).thenReturn(Optional.of(existing));

        AdminContractsAgreementHdrDto result = service.updateContractsAndAgreements(10L, updateDto);
        assertNotNull(result);
    }

    @Test
    void testRenew_HeaderNotFound() {
        ContractRenewalRequest req = new ContractRenewalRequest();
        req.setTransactionPoid(10L);
        when(headerRepo.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.renewContractsAndAgreements(req));
    }

    @Test
    void testRenew_DuplicateFound() {
        ContractRenewalRequest req = new ContractRenewalRequest();
        req.setTransactionPoid(10L);
        req.setEffectiveDate(java.time.LocalDate.of(2025, 1, 1));
        req.setExpiryDate(java.time.LocalDate.of(2025, 12, 31));

        AdminContractsAgreementHdr hdr = new AdminContractsAgreementHdr();
        hdr.setTransactionPoid(10L);

        when(headerRepo.findById(10L)).thenReturn(Optional.of(hdr));

        AdminContractsAgreementRenewalEntity existing = new AdminContractsAgreementRenewalEntity();
        existing.setEffectiveStartDate(java.time.LocalDate.of(2025, 1, 1));
        existing.setExpiryDate(java.time.LocalDate.of(2025, 12, 31));

        when(renewalDtlRepository.findByIdTransactionPoid(10L)).thenReturn(List.of(existing));

        assertThrows(ValidationException.class, () -> service.renewContractsAndAgreements(req));
        
        verify(validationService).expiryDateValidation(req.getExpiryDate(), req.getEffectiveDate(), null);
    }

    @Test
    void testCreate_InvalidPicPeriodDates() {
        AdminContractsAgreementHdrDto dto = new AdminContractsAgreementHdrDto();
        dto.setAgreementName("Test");

        AdminContractsAgreementPicDtlDto picDto = new AdminContractsAgreementPicDtlDto();
        picDto.setPeriodFrom(LocalDate.of(2026, 12, 31));
        picDto.setPeriodTo(LocalDate.of(2026, 1, 1));
        dto.setAgreementContentDetails(List.of(picDto));

        when(validationService.checkForDuplicateAgreementName("Test")).thenReturn(false);
        doThrow(new ValidationException("Period From cannot be after Period To"))
                .when(validationService).validatePicPeriodDates(picDto.getPeriodFrom(), picDto.getPeriodTo());

        AdminContractsAgreementHdr savedEntity = new AdminContractsAgreementHdr();
        savedEntity.setTransactionPoid(10L);
        when(headerRepo.saveAndFlush(any())).thenReturn(savedEntity);

        assertThrows(ValidationException.class, () -> service.createContractsAndAgreements(dto));
        verify(picDtlRepository, never()).save(any());
    }

    @Test
    void testUpdate_InvalidPicPeriodDates() {
        AdminContractsAgreementHdr existing = new AdminContractsAgreementHdr();
        existing.setTransactionPoid(10L);
        existing.setAgreementName("OldName");

        AdminContractsAgreementHdrDto updateDto = new AdminContractsAgreementHdrDto();
        updateDto.setAgreementName("OldName");

        AdminContractsAgreementPicDtlDto picUpDto = new AdminContractsAgreementPicDtlDto();
        picUpDto.setDetRowId(2L);
        picUpDto.setActionType("ISUPDATED");
        picUpDto.setPeriodFrom(LocalDate.of(2026, 12, 31));
        picUpDto.setPeriodTo(LocalDate.of(2026, 1, 1));
        updateDto.setAgreementContentDetails(List.of(picUpDto));

        when(headerRepo.findById(10L)).thenReturn(Optional.of(existing));
        doThrow(new ValidationException("Period From cannot be after Period To"))
                .when(validationService).validatePicPeriodDates(picUpDto.getPeriodFrom(), picUpDto.getPeriodTo());

        assertThrows(ValidationException.class, () -> service.updateContractsAndAgreements(10L, updateDto));
        verify(picDtlRepository, never()).saveAll(anyList());
    }

    @Test
    void testRenew_Success() {
        ContractRenewalRequest req = new ContractRenewalRequest();
        req.setTransactionPoid(10L);
        req.setEffectiveDate(java.time.LocalDate.of(2025, 1, 1));
        req.setExpiryDate(java.time.LocalDate.of(2025, 12, 31));

        AdminContractsAgreementHdr hdr = new AdminContractsAgreementHdr();
        hdr.setTransactionPoid(10L);
        hdr.setDocRef("REF-123");

        when(headerRepo.findById(10L)).thenReturn(Optional.of(hdr));
        when(renewalDtlRepository.findByIdTransactionPoid(10L)).thenReturn(Collections.emptyList());
        when(renewalDtlRepository.findMaxDetRowId(10L)).thenReturn(5L);

        try (MockedStatic<com.asg.common.lib.utility.DateUtil> dateUtilMock = mockStatic(com.asg.common.lib.utility.DateUtil.class)) {
            java.time.LocalDate mockNow = java.time.LocalDate.now();
            dateUtilMock.when(com.asg.common.lib.utility.DateUtil::getCurrentDateInUserTimeZone).thenReturn(mockNow);

            AdminContractsAgreementRenewalEntity savedRenewal = new AdminContractsAgreementRenewalEntity();
            savedRenewal.setEffectiveStartDate(req.getEffectiveDate());
            savedRenewal.setExpiryDate(req.getExpiryDate());

            when(renewalDtlRepository.saveAndFlush(any())).thenReturn(savedRenewal);

            ContractRenewalResponse res = service.renewContractsAndAgreements(req);

            assertNotNull(res);
            assertEquals(req.getEffectiveDate(), res.getEffectiveStartDate());
            assertEquals(req.getExpiryDate(), res.getExpiryDate());

            verify(headerRepo).saveAndFlush(hdr);
            verify(renewalDtlRepository).saveAndFlush(any());
            verify(loggingService, times(2)).createLogSummaryEntry(anyString(), anyString(), anyString());
        }
    }
}
