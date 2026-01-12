package com.asg.shipping.portstoragetariffsmaster.service;

import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.portstoragetariffsmaster.dto.*;
import com.asg.shipping.portstoragetariffsmaster.entity.ShipPortTariffDtl;
import com.asg.shipping.portstoragetariffsmaster.entity.ShipPortTariffHdr;
import com.asg.shipping.portstoragetariffsmaster.repository.ShipPortTariffDtlRepository;
import com.asg.shipping.portstoragetariffsmaster.repository.ShipPortTariffHdrRepository;
import com.asg.shipping.portstoragetariffsmaster.util.PortStorageTariffMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortStorageTariffsServiceImplTest {

    @Mock
    private ShipPortTariffHdrRepository tariffHdrRepository;

    @Mock
    private ShipPortTariffDtlRepository tariffDtlRepository;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private LovDataService lovService;

    @Mock
    private PortStorageTariffMapper mapper;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private PortStorageTariffsServiceImpl service;

    private ShipPortTariffHdr testTariffHdr;
    private PortStorageTariffDto testDto;
    private PortStorageTariffCreateDTO createDto;
    private PortStorageTariffUpdateDTO updateDto;
    private TariffDetailCreateDTO detailCreateDto;

    @BeforeEach
    void setUp() {
        testTariffHdr = ShipPortTariffHdr.builder()
                .transactionPoid(1L)
                .groupPoid(1L)
                .portPoid(100L)
                .description("Test Tariff")
                .tariffType("EXPORT")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .transactionDate(LocalDate.now())
                .docRef("DOC001")
                .companyPoid(1L)
                .deleted("N")
                .createdBy("testuser")
                .createdDate(LocalDateTime.now())
                .build();

        testDto = PortStorageTariffDto.builder()
                .transactionPoid(1L)
                .portPoid(100L)
                .description("Test Tariff")
                .tariffType("EXPORT")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .build();

        createDto = PortStorageTariffCreateDTO.builder()
                .portPoid(100L)
                .description("Test Tariff")
                .tariffType("EXPORT")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .build();

        updateDto = PortStorageTariffUpdateDTO.builder()
                .portPoid(100L)
                .description("Updated Tariff")
                .tariffType("EXPORT")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .build();

        detailCreateDto = TariffDetailCreateDTO.builder()
                .containerTypePoid(200L)
                .containerSize(new BigDecimal("20"))
                .freeDays(5)
                .slab1Tilldays(10)
                .slab1Rate(new BigDecimal("100.00"))
                .build();
    }

    private LovGetListDto createLovDto() {
        LovGetListDto lov = new LovGetListDto();
        lov.setPoid(100L);
        lov.setCode("CODE");
        lov.setLabel("Name");
        return lov;
    }

    @Test
    void searchTariffs_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", new ArrayList<>());
            Pageable pageable = PageRequest.of(0, 20);

            when(documentService.resolveOperator(any())).thenReturn("AND");
            when(documentService.resolveIsDeleted(any())).thenReturn("N");
            when(documentService.resolveFilters(any())).thenReturn(new ArrayList<>());
            when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn(new RawSearchResult(Collections.emptyList(), new HashMap<>(), 0L));

            Map<String, Object> result = service.searchTariffs("DOC_ID", filterRequest, pageable);

            assertNotNull(result);
            verify(documentService).search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void getTariff_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC_ID");

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testTariffHdr));
            when(mapper.mapToDto(testTariffHdr)).thenReturn(testDto);
            when(tariffDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(mapper.mapDetailsToDto(anyList())).thenReturn(Collections.emptyList());
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());

            PortStorageTariffDto result = service.getTariff(1L);

            assertNotNull(result);
            assertEquals(1L, result.getTransactionPoid());
            verify(tariffHdrRepository).findByTransactionPoidAndGroupPoid(1L, 1L);
        }
    }

    @Test
    void getTariff_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getTariff(1L));
        }
    }

    @Test
    void createTariff_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC_ID");

            when(lovService.getDetailsByPoidAndLovName(anyLong(), eq("PORT_MASTER"))).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), eq("PORT_TARIFF_TYPES"))).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), any())).thenReturn(false);
            lenient().when(tariffHdrRepository.existsByDocRef(anyString())).thenReturn(false);
            when(tariffHdrRepository.save(any(ShipPortTariffHdr.class))).thenReturn(testTariffHdr);
            when(mapper.mapToDto(testTariffHdr)).thenReturn(testDto);
            when(tariffDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(mapper.mapDetailsToDto(anyList())).thenReturn(Collections.emptyList());

            PortStorageTariffDto result = service.createTariff(createDto, 1L, 1L, 1L);

            assertNotNull(result);
            verify(tariffHdrRepository).save(any(ShipPortTariffHdr.class));
        }
    }

    @Test
    void createTariff_WithDetails_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC_ID");

            createDto.setTariffDetails(List.of(detailCreateDto));

            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), any())).thenReturn(false);
            when(tariffHdrRepository.save(any(ShipPortTariffHdr.class))).thenReturn(testTariffHdr);
            when(mapper.mapToDto(testTariffHdr)).thenReturn(testDto);
            when(mapper.mapDetailCreateDTOToEntity(any(), anyLong(), anyString())).thenReturn(new ShipPortTariffDtl());
            when(tariffDtlRepository.save(any(ShipPortTariffDtl.class))).thenReturn(new ShipPortTariffDtl());
            when(tariffDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(mapper.mapDetailsToDto(anyList())).thenReturn(Collections.emptyList());

            PortStorageTariffDto result = service.createTariff(createDto, 1L, 1L, 1L);

            assertNotNull(result);
            verify(tariffDtlRepository).save(any(ShipPortTariffDtl.class));
        }
    }

    @Test
    void createTariff_InvalidPeriod() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            createDto.setPeriodFrom(LocalDate.of(2024, 12, 31));
            createDto.setPeriodTo(LocalDate.of(2024, 1, 1));

            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());

            assertThrows(ValidationException.class, () -> service.createTariff(createDto, 1L, 1L, 1L));
        }
    }

    @Test
    void createTariff_OverlappingPeriod() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), any())).thenReturn(true);

            assertThrows(ValidationException.class, () -> service.createTariff(createDto, 1L, 1L, 1L));
        }
    }

    @Test
    void createTariff_InvalidPort() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(lovService.getDetailsByPoidAndLovName(anyLong(), eq("PORT_MASTER"))).thenReturn(null);

            assertThrows(ValidationException.class, () -> service.createTariff(createDto, 1L, 1L, 1L));
        }
    }

    @Test
    void createTariff_InvalidSlabSequence() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            detailCreateDto.setSlab1Tilldays(20);
            detailCreateDto.setSlab2Tilldays(10);
            createDto.setTariffDetails(List.of(detailCreateDto));

            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), any())).thenReturn(false);
            when(tariffHdrRepository.save(any(ShipPortTariffHdr.class))).thenReturn(testTariffHdr);

            assertThrows(ValidationException.class, () -> service.createTariff(createDto, 1L, 1L, 1L));
        }
    }

    @Test
    void createTariff_MissingSlabRate() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            detailCreateDto.setSlab1Rate(null);
            createDto.setTariffDetails(List.of(detailCreateDto));

            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), any())).thenReturn(false);
            when(tariffHdrRepository.save(any(ShipPortTariffHdr.class))).thenReturn(testTariffHdr);

            assertThrows(ValidationException.class, () -> service.createTariff(createDto, 1L, 1L, 1L));
        }
    }

    @Test
    void updateTariff_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC_ID");

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testTariffHdr));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), anyLong())).thenReturn(false);
            when(tariffHdrRepository.save(any(ShipPortTariffHdr.class))).thenReturn(testTariffHdr);
            when(mapper.mapToDto(testTariffHdr)).thenReturn(testDto);
            when(tariffDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(mapper.mapDetailsToDto(anyList())).thenReturn(Collections.emptyList());

            PortStorageTariffDto result = service.updateTariff(1L, updateDto, 1L, 1L, 1L);

            assertNotNull(result);
            verify(tariffHdrRepository).save(any(ShipPortTariffHdr.class));
        }
    }

    @Test
    void updateTariff_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.updateTariff(1L, updateDto, 1L, 1L, 1L));
        }
    }

    @Test
    void deleteTariff_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC_ID");

            testTariffHdr.setDeleted("N");
            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testTariffHdr));
            when(tariffHdrRepository.save(any(ShipPortTariffHdr.class))).thenReturn(testTariffHdr);

            service.deleteTariff(1L);

            verify(tariffHdrRepository).save(argThat(tariff -> "Y".equals(tariff.getDeleted())));
        }
    }

    @Test
    void deleteTariff_AlreadyDeleted() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            testTariffHdr.setDeleted("Y");
            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testTariffHdr));

            service.deleteTariff(1L);

            verify(tariffHdrRepository, never()).save(any(ShipPortTariffHdr.class));
        }
    }

    @Test
    void deleteTariff_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.deleteTariff(1L));
        }
    }
}
