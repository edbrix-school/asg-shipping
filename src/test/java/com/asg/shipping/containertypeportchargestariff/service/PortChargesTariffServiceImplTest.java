package com.asg.shipping.containertypeportchargestariff.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.shipping.containertypeportchargestariff.dto.*;
import com.asg.shipping.containertypeportchargestariff.entity.ShipPortChargesDtl;
import com.asg.shipping.containertypeportchargestariff.entity.ShipPortChargesHdr;
import com.asg.shipping.containertypeportchargestariff.repository.ShipPortChargesDtlRepository;
import com.asg.shipping.containertypeportchargestariff.repository.ShipPortChargesHdrRepository;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.linemasterthirdparty.repository.ShipLineMasterThirdPartyRepository;
import com.asg.shipping.portMaster.repository.PortMasterRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortChargesTariffServiceImplTest {

    @Mock
    private DocumentSearchService documentService;
    @Mock
    private ShipPortChargesHdrRepository hdrRepository;
    @Mock
    private ShipPortChargesDtlRepository dtlRepository;
    @Mock
    private PortMasterRepository portMasterRepository;
    @Mock
    private ShipLineMasterThirdPartyRepository lineMasterThirdPartyRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private com.asg.common.lib.service.DocumentDeleteService documentDeleteService;
    @Mock
    private com.asg.common.lib.service.LoggingService loggingService;

    @InjectMocks
    private PortChargesTariffServiceImpl service;

    private ShipPortChargesHdr mockHdr;
    private ShipPortChargesDtl mockDtl;
    private PortChargesTariffCreateDto createDto;
    private CopyTariffRequestDto copyRequest;

    @BeforeEach
    void setUp() {
        mockHdr = new ShipPortChargesHdr();
        mockHdr.setTransactionPoid(1L);
        mockHdr.setGroupPoid(100L);
        mockHdr.setPortPoid(200L);
        mockHdr.setDescription("Test Tariff");
        mockHdr.setPeriodFrom(LocalDate.of(2024, 1, 1));
        mockHdr.setPeriodTo(LocalDate.of(2024, 12, 31));
        mockHdr.setChargeLinePoid(300L);
        mockHdr.setChargeDivision("DIV1");
        mockHdr.setDeleted("N");

        mockDtl = new ShipPortChargesDtl();
        mockDtl.setTransactionPoid(1L);
        mockDtl.setDetRowId(1L);
        mockDtl.setAmount20(BigDecimal.valueOf(100));

        createDto = new PortChargesTariffCreateDto();
        createDto.setPortPoid(200L);
        createDto.setDescription("New Tariff");
        createDto.setPeriodFrom(LocalDate.of(2024, 1, 1));
        createDto.setPeriodTo(LocalDate.of(2024, 12, 31));
        createDto.setChargeLinePoid(300L);
        createDto.setChargeDivision("DIV1");

        copyRequest = new CopyTariffRequestDto();
        copyRequest.setDescription("Copied Tariff");
        copyRequest.setUseStoredProcedure(false);
    }

    @Test
    void testGetPortChargesTariff_Success() {
        Long transactionPoid = 1L;
        Long groupPoid = 100L;

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(groupPoid);
            
            when(hdrRepository.findById(transactionPoid)).thenReturn(Optional.of(mockHdr));
            when(dtlRepository.findByTransactionPoid(transactionPoid)).thenReturn(List.of(mockDtl));

            PortChargesTariffDto result = service.getPortChargesTariff(transactionPoid);

            assertNotNull(result);
            assertEquals(transactionPoid, result.getTransactionPoid());
            assertEquals("Test Tariff", result.getDescription());
        }
    }

    @Test
    void testGetPortChargesTariff_NotFound() {
        Long transactionPoid = 999L;
        Long groupPoid = 100L;

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(groupPoid);
            when(hdrRepository.findById(transactionPoid)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                () -> service.getPortChargesTariff(transactionPoid));
        }
    }

    @Test
    void testCreatePortChargesTariff_Success() {
        Long groupPoid = 100L;
        Long userPoid = 50L;
        
        when(portMasterRepository.existsByPortPoidAndGroupPoid(200L, groupPoid)).thenReturn(true);
        when(lineMasterThirdPartyRepository.existsByLinePoid(300L)).thenReturn(true);
        when(hdrRepository.findOverlappingTariffs(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of());
        when(hdrRepository.save(any(ShipPortChargesHdr.class))).thenReturn(mockHdr);
        when(hdrRepository.findById(1L)).thenReturn(Optional.of(mockHdr));
        when(dtlRepository.findByTransactionPoid(1L)).thenReturn(List.of());

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(groupPoid);

            PortChargesTariffDto result = service.createPortChargesTariff(createDto, groupPoid, userPoid);

            assertNotNull(result);
            verify(hdrRepository).save(any(ShipPortChargesHdr.class));
        }
    }

    @Test
    void testDeletePortChargesTariff_Success() {
        Long transactionPoid = 1L;
        Long groupPoid = 100L;
        DeleteReasonDto deleteReasonDto = new DeleteReasonDto();

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(groupPoid);
            when(hdrRepository.findById(transactionPoid)).thenReturn(Optional.of(mockHdr));

            service.deletePortChargesTariff(transactionPoid, deleteReasonDto);

            // Verify that the method completes without throwing exceptions
            verify(hdrRepository).findById(transactionPoid);
        }
    }

    @Test
    void testValidateOverlap_NoOverlap() {
        ValidateOverlapRequestDto request = new ValidateOverlapRequestDto();
        request.setPortPoid(200L);
        request.setChargeLinePoid(300L);
        request.setChargeDivision("DIV1");
        request.setPeriodFrom(LocalDate.of(2025, 1, 1));
        request.setPeriodTo(LocalDate.of(2025, 12, 31));
        
        Long groupPoid = 100L;

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getGroupPoid).thenReturn(groupPoid);
            when(hdrRepository.findOverlappingTariffs(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

            ValidateOverlapResponseDto result = service.validateOverlap(request);

            assertNotNull(result);
            assertFalse(result.isOverlapping());
            assertTrue(result.getConflicts().isEmpty());
        }
    }
}