package com.asg.shipping.shipcommisiontransferTest.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionDetailDto;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferCreateDTO;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferDto;
import com.asg.shipping.shipcommisiontransfer.dto.ShipCommissionTransferUpdateDTO;
import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionDtl;
import com.asg.shipping.shipcommisiontransfer.entity.ShipBlCommissionHdr;
import com.asg.shipping.shipcommisiontransfer.repository.ShipBlCommissionDtlRepository;
import com.asg.shipping.shipcommisiontransfer.repository.ShipBlCommissionHdrRepository;
import com.asg.shipping.shipcommisiontransfer.service.ShipCommissionTransferServiceImpl;
import com.asg.shipping.shipcommisiontransfer.util.ShipCommissionTransferMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipCommissionTransferServiceImplTest {

    @Mock
    private ShipBlCommissionHdrRepository headerRepository;
    @Mock
    private ShipBlCommissionDtlRepository detailRepository;
    @Mock
    private DocumentSearchService documentService;
    @Mock
    private LoggingService loggingService;
    @Mock
    private ShipCommissionTransferMapper mapper;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ShipCommissionTransferServiceImpl service;

    private ShipBlCommissionHdr hdrEntity;
    private ShipCommissionTransferDto dto;
    private ShipCommissionTransferCreateDTO createDTO;
    private ShipCommissionTransferUpdateDTO updateDTO;

    @BeforeEach
    void setup() {
        hdrEntity = ShipBlCommissionHdr.builder()
                .transactionPoid(1L)
                .groupPoid(10L)
                .companyPoid(20L)
                .docRef("COM-001")
                .transactionDate(LocalDate.now())
                .deleted("N")
                .build();

        dto = ShipCommissionTransferDto.builder()
                .transactionPoid(1L)
                .docRef("COM-001")
                .build();

        createDTO = ShipCommissionTransferCreateDTO.builder()
                .transactionDate(LocalDate.now())
                .voyageTransactionPoid(100L)
                .currencyCode("USD")
                .currencyExchange(BigDecimal.ONE)
                .commissionDetails(List.of())
                .build();

        updateDTO = ShipCommissionTransferUpdateDTO.builder()
                .transactionDate(LocalDate.now())
                .voyageTransactionPoid(100L)
                .currencyCode("USD")
                .currencyExchange(BigDecimal.ONE)
                .commissionDetails(List.of())
                .build();
    }

    @Test
    void testSearchShipCommissionTransfer() {
        FilterDto filter = new FilterDto("GLOBALSEARCH", "COM-001");
        FilterRequestDto filterRequest = new FilterRequestDto("OR", "N", List.of(filter));
        Pageable pageable = PageRequest.of(0, 10);

        RawSearchResult raw = new RawSearchResult(
                List.of(Map.of("DOC_REF", "COM-001")),
                Map.of("DOC_REF", "Document Reference"),
                1L
        );

        when(documentService.resolveOperator(filterRequest)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filterRequest)).thenReturn("N");
        when(documentService.resolveFilters(filterRequest)).thenReturn(List.of(filter));
        when(documentService.search(anyString(), any(), eq("OR"), eq(pageable), eq("N"), eq("DOC_REF"), eq("TRANSACTION_POID")))
                .thenReturn(raw);

        Map<String, Object> result = service.searchShipCommissionTransfer("DOC-1", filterRequest, pageable);

        assertNotNull(result);
        verify(documentService).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testGetShipCommissionTransfer_NotFound() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getShipCommissionTransfer(1L));
        }
    }

    @Test
    void testGetShipCommissionTransfer_Deleted() {
        hdrEntity.setDeleted("Y");
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.of(hdrEntity));

            assertThrows(ResourceNotFoundException.class, () -> service.getShipCommissionTransfer(1L));
        }
    }

    @Test
    void testUpdateShipCommissionTransfer_NotFound() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.updateShipCommissionTransfer(1L, updateDTO));
        }
    }



    @Test
    void testDeleteShipCommissionTransfer_NotFound() {
        try (var mockedUserContext = mockStatic(com.asg.common.lib.security.util.UserContext.class)) {
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getGroupPoid).thenReturn(10L);
            mockedUserContext.when(com.asg.common.lib.security.util.UserContext::getCompanyPoid).thenReturn(20L);

            when(headerRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(1L, 10L, 20L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.deleteShipCommissionTransfer(1L));
        }
    }
}
