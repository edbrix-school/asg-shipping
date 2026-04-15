package com.asg.shipping.contractsandagreements.service.impl;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementHdrDto;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementPicDtlDto;
import com.asg.shipping.contractsandagreements.dto.AdminContractsAgreementRenewalDto;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementHdr;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementPicDtl;
import com.asg.shipping.contractsandagreements.entity.AdminContractsAgreementRenewalEntity;
import com.asg.shipping.contractsandagreements.repository.AdminContractsAgreementPicDtlRepository;
import com.asg.shipping.contractsandagreements.repository.AdminContractsAgreementRenewalDtlRepository;
import com.asg.shipping.contractsandagreements.repository.AdminContractsAgreementsHdrRepository;
import com.asg.shipping.contractsandagreements.service.ContractsAndAgreementsValidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private ContractsAndAgreementsServiceImpl service;

    private MockedStatic<UserContext> userContextMockedStatic;

    @BeforeEach
    void setUp() {
        userContextMockedStatic = mockStatic(UserContext.class);
        userContextMockedStatic.when(UserContext::getDocumentId).thenReturn("1001");
        userContextMockedStatic.when(UserContext::getCompanyPoid).thenReturn(1L);
        userContextMockedStatic.when(UserContext::getGroupPoid).thenReturn(2L);
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
        when(headerRepo.saveAndFlush(any())).thenReturn(savedEntity);
        when(headerRepo.findById(10L)).thenReturn(Optional.of(savedEntity));
        when(picDtlRepository.findMaxDetRowId(10L)).thenReturn(0L);
        when(renewalDtlRepository.findMaxDetRowId(10L)).thenReturn(0L);

        AdminContractsAgreementHdrDto result = service.createContractsAndAgreements(dto);

        assertNotNull(result);
        verify(headerRepo).saveAndFlush(any());
        verify(picDtlRepository).save(any());
        verify(renewalDtlRepository).save(any());
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

        AdminContractsAgreementRenewalEntity renEnt = new AdminContractsAgreementRenewalEntity();
        when(renewalDtlRepository.findByIdTransactionPoidAndIdDetRowId(10L, 2L)).thenReturn(Optional.of(renEnt));

        AdminContractsAgreementHdrDto result = service.updateContractsAndAgreements(10L, updateDto);
        
        assertNotNull(result);
        verify(headerRepo).save(any());
        verify(picDtlRepository).deleteByIdTransactionPoidAndIdDetRowId(10L, 1L);
        verify(picDtlRepository).saveAll(anyList());
        verify(renewalDtlRepository).deleteByIdTransactionPoidAndIdDetRowId(10L, 1L);
        verify(renewalDtlRepository).saveAll(anyList());
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
    void testUpdateRenewalDtl_ResourceNotFound() {
        AdminContractsAgreementHdr existing = new AdminContractsAgreementHdr();
        existing.setTransactionPoid(10L);
        existing.setAgreementName("OldName");

        AdminContractsAgreementHdrDto updateDto = new AdminContractsAgreementHdrDto();
        updateDto.setAgreementName("OldName");
        
        AdminContractsAgreementRenewalDto renUpDto = new AdminContractsAgreementRenewalDto();
        renUpDto.setDetRowId(2L);
        renUpDto.setActionType("ISUPDATED");

        updateDto.setRenewalDetails(List.of(renUpDto));

        when(headerRepo.findById(10L)).thenReturn(Optional.of(existing));
        when(renewalDtlRepository.findByIdTransactionPoidAndIdDetRowId(10L, 2L)).thenReturn(Optional.empty());

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
}
