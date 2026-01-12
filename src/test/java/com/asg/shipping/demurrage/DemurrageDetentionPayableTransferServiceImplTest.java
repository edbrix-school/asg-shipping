package com.asg.shipping.demurrage;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.DemurrageDetentionPayableTransferCreateDTO;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.DemurrageDetentionPayableTransferDto;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.ProcessDataRequestDTO;
import com.asg.shipping.demurragedetentionpayabletransfer.dto.UpdateFreeDaysRequestDTO;
import com.asg.shipping.demurragedetentionpayabletransfer.entity.ShipDemDetnTransferHdr;
import com.asg.shipping.demurragedetentionpayabletransfer.repository.ShipDemDetnTransferHdrRepository;
import com.asg.shipping.demurragedetentionpayabletransfer.repository.ShipDemDetnTransferDtlRepository;
import com.asg.shipping.demurragedetentionpayabletransfer.repository.ShipDemDtnTransferBillDtlRepository;
import com.asg.shipping.demurragedetentionpayabletransfer.service.DemurrageDetentionPayableTransferServiceImpl;
import com.asg.shipping.demurragedetentionpayabletransfer.util.DemurrageDetentionPayableTransferMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemurrageDetentionPayableTransferServiceImplTest {

    @Mock
    private DocumentSearchService documentService;
    @Mock
    private ShipDemDetnTransferHdrRepository headerRepository;
    @Mock
    private ShipDemDetnTransferDtlRepository transferDtlRepository;
    @Mock
    private ShipDemDtnTransferBillDtlRepository billDtlRepository;
    @Mock
    private LovDataService lovService;
    @Mock
    private DemurrageDetentionPayableTransferMapper mapper;
    @Mock
    private EntityManager entityManager;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DemurrageDetentionPayableTransferServiceImpl service;

    private DemurrageDetentionPayableTransferCreateDTO request;
    private ShipDemDetnTransferHdr hdrEntity;
    private DemurrageDetentionPayableTransferDto response;

    @BeforeEach
    void setup() {
        request = new DemurrageDetentionPayableTransferCreateDTO();
        request.setLinePoid(1001L);
        request.setTransactionDate(LocalDate.now());
        request.setBlType("IMPORT");
        request.setEmptyFromDate(LocalDate.now());
        request.setEmptyToDate(LocalDate.now().plusDays(30));

        hdrEntity = new ShipDemDetnTransferHdr();
        hdrEntity.setTransactionPoid(1L);
        hdrEntity.setGroupPoid(100L);
        hdrEntity.setDeleted("N");

        response = new DemurrageDetentionPayableTransferDto();
        response.setTransactionPoid(1L);
    }

    @Test
    void testSearchRecords() {
        FilterDto filter = new FilterDto("GLOBALSEARCH", "DEM");
        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of(filter));
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("DOC_REF", "DEM001")),
                Map.of("DOC_REF", "Document Reference"),
                1L
        );

        when(documentService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveFilters(filterRequest)).thenReturn(List.of(filter));
        when(documentService.search(
                anyString(), any(), eq("OR"), eq(pageable), eq("N"), any(), any()
        )).thenReturn(raw);

        Map<String, Object> result = service.searchDemurrageDetentionPayableTransfer("DOC-1", filterRequest, pageable);

        assertNotNull(result);
        verify(documentService).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testGetById_Success() {
        when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                .thenReturn(Optional.of(hdrEntity));
        when(transferDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of());
        when(billDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of());
        when(mapper.mapToDto(any()))
                .thenReturn(response);

        // Mock UserContext
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            DemurrageDetentionPayableTransferDto result = service.getDemurrageDetentionPayableTransfer(1L);

            assertNotNull(result);
            assertEquals(1L, result.getTransactionPoid());
        }
    }

    @Test
    void testGetById_NotFound() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getDemurrageDetentionPayableTransfer(1L));
        }
    }

    @Test
    @Disabled
    void testCreate_Success() {
        when(headerRepository.save(any()))
                .thenReturn(hdrEntity);
        when(transferDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of());
        when(billDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                .thenReturn(List.of());
        when(mapper.mapToDto(any()))
                .thenReturn(response);

        DemurrageDetentionPayableTransferDto result = 
                service.createDemurrageDetentionPayableTransfer(request, 1L, 100L);

        assertNotNull(result);
        verify(headerRepository).save(any());
    }

    @Test
    void testUpdate_NotFound() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.updateDemurrageDetentionPayableTransfer(1L, null, 1L, 100L));
        }
    }

    @Test
    void testDelete_Success() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.of(hdrEntity));

            assertDoesNotThrow(() -> service.deleteDemurrageDetentionPayableTransfer(1L, 1L, 100L));

            verify(headerRepository).save(argThat(e -> "Y".equals(e.getDeleted())));
        }
    }

    @Test
    void testDelete_NotFound() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.deleteDemurrageDetentionPayableTransfer(1L, 1L, 100L));
        }
    }

    @Test
    void testProcessData_Success() {
        ProcessDataRequestDTO processRequest = new ProcessDataRequestDTO();
        processRequest.setLinePoid(1001L);
        processRequest.setBlType("IMPORT");
        processRequest.setEmptyFromDate(LocalDate.now());
        processRequest.setEmptyToDate(LocalDate.now().plusDays(30));

        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.of(hdrEntity));
            when(transferDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(List.of());
            when(billDtlRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(List.of());
            when(mapper.mapToDto(any()))
                    .thenReturn(response);

            DemurrageDetentionPayableTransferDto result = service.processData(1L, processRequest);

            assertNotNull(result);
        }
    }

    @Test
    void testProcessData_NotFound() {
        ProcessDataRequestDTO processRequest = new ProcessDataRequestDTO();
        
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.processData(1L, processRequest));
        }
    }

    @Test
    @Disabled
    void testUpdateFreeDays_Success() {
        UpdateFreeDaysRequestDTO updateRequest = new UpdateFreeDaysRequestDTO();
        updateRequest.setContainerUpdates(List.of());
        
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.of(hdrEntity));

            assertDoesNotThrow(() -> service.updateFreeDays(1L, updateRequest));
        }
    }

    @Test
    void testUpdateFreeDays_NotFound() {
        UpdateFreeDaysRequestDTO updateRequest = new UpdateFreeDaysRequestDTO();
        
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(1L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 100L, 1L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.updateFreeDays(1L, updateRequest));
        }
    }
}