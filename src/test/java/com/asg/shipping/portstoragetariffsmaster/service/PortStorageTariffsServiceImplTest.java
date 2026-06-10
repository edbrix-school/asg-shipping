package com.asg.shipping.portstoragetariffsmaster.service;

import com.asg.common.lib.dto.DeleteReasonDto;
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
import com.asg.shipping.portstoragetariffsmaster.entity.ShipPortTariffDtlId;
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

    @Mock
    private com.asg.common.lib.service.DocumentDeleteService documentDeleteService;

    @InjectMocks
    private PortStorageTariffsServiceImpl service;

    private ShipPortTariffHdr testTariffHdr;
    private PortStorageTariffDto testDto;
    private PortStorageTariffCreateDTO createDto;
    private PortStorageTariffUpdateDTO updateDto;
    private TariffDetailCreateDTO detailCreateDto;
    private TariffDetailUpdateDTO detailUpdateDto;
    private DeleteReasonDto deleteReasonDto;

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
                .docRef(null)
                .build();

        updateDto = PortStorageTariffUpdateDTO.builder()
                .portPoid(100L)
                .description("Updated Tariff")
                .tariffType("EXPORT")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .docRef(null)
                .build();

        detailCreateDto = TariffDetailCreateDTO.builder()
                .containerTypePoid(200L)
                .containerSize(new BigDecimal("20"))
                .freeDays(5)
                .slab1Tilldays(10)
                .slab1Rate(new BigDecimal("100.00"))
                .build();

        detailUpdateDto = TariffDetailUpdateDTO.builder()
                .detRowId(1L)
                .containerTypePoid(200L)
                .containerSize(new BigDecimal("20"))
                .freeDays(5)
                .slab1Tilldays(10)
                .slab1Rate(new BigDecimal("100.00"))
                .build();

        deleteReasonDto = new DeleteReasonDto();
        deleteReasonDto.setDeleteReason("Test deletion reason");
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

            FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", Collections.emptyList());
            Pageable pageable = PageRequest.of(0, 20);

            when(documentService.resolveOperator(any())).thenReturn("AND");
            when(documentService.resolveIsDeleted(any())).thenReturn("N");
            when(documentService.resolveFilters(any())).thenReturn(Collections.emptyList());
            when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn(new RawSearchResult(Collections.emptyList(), new HashMap<>(), 0L));

            Map<String, Object> result = service.searchTariffs("DOC_ID", filterRequest, null, null, pageable);

            assertNotNull(result);
            verify(documentService).search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString());
            verify(documentService).resolveFilters(filterRequest);
        }
    }

    @Test
    void searchTariffs_AppliesPeriodOverlapFilters() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", Collections.emptyList());
            Pageable pageable = PageRequest.of(0, 20);
            LocalDate periodFrom = LocalDate.of(2026, 1, 1);
            LocalDate periodTo = LocalDate.of(2026, 1, 30);

            when(documentService.resolveOperator(any())).thenReturn("AND");
            when(documentService.resolveIsDeleted(any())).thenReturn("N");
            when(documentService.resolveFilters(any())).thenReturn(new ArrayList<>());
            when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn(new RawSearchResult(Collections.emptyList(), new HashMap<>(), 0L));

            service.searchTariffs("DOC_ID", filterRequest, periodFrom, periodTo, pageable);

            verify(documentService).search(
                    eq("DOC_ID"),
                    argThat(filters -> filters.size() == 2
                            && filters.stream().anyMatch(f -> "PERIOD_FROM".equals(f.searchField()) && "<=2026-01-30".equals(f.searchValue()))
                            && filters.stream().anyMatch(f -> "PERIOD_TO".equals(f.searchField()) && ">=2026-01-01".equals(f.searchValue()))),
                    eq("AND"),
                    eq(pageable),
                    eq("N"),
                    eq("DESCRIPTION"),
                    eq("TRANSACTION_POID")
            );
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

            service.deleteTariff(1L, 1L, 1L, null);

            verify(documentDeleteService).deleteDocument(eq(1L), eq("SHIP_PORT_TARIFF_HDR"), eq("TRANSACTION_POID"), isNull(), any(LocalDate.class));
        }
    }

    @Test
    void deleteTariff_AlreadyDeleted() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            testTariffHdr.setDeleted("Y");
            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testTariffHdr));

            assertThrows(com.asg.shipping.exceptions.CustomException.class, 
                () -> service.deleteTariff(1L, 1L, 1L, null));

            verify(tariffHdrRepository, never()).save(any(ShipPortTariffHdr.class));
        }
    }

    @Test
    void deleteTariff_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.deleteTariff(1L, 1L, 1L, null));
        }
    }

    // Additional tests for 100% coverage
    @Test
    void createTariff_DuplicateDocRef() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            createDto.setDocRef("DOC001");

            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), any())).thenReturn(false);
            when(tariffHdrRepository.existsByDocRef("DOC001")).thenReturn(true);

            assertThrows(ValidationException.class, () -> service.createTariff(createDto, 1L, 1L, 1L));
        }
    }

    @Test
    void createTariff_InvalidTariffType() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(lovService.getDetailsByPoidAndLovName(anyLong(), eq("PORT_MASTER"))).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), eq("PORT_TARIFF_TYPES"))).thenReturn(null);

            assertThrows(ValidationException.class, () -> service.createTariff(createDto, 1L, 1L, 1L));
        }
    }

    @Test
    void createTariff_InvalidContainerType() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            createDto.setTariffDetails(List.of(detailCreateDto));

            when(lovService.getDetailsByPoidAndLovName(anyLong(), eq("PORT_MASTER"))).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), eq("PORT_TARIFF_TYPES"))).thenReturn(createLovDto());
            when(lovService.getDetailsByPoidAndLovName(200L, "CONTAINER_TYPE_MASTER")).thenReturn(null);
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), any())).thenReturn(false);

            assertThrows(ValidationException.class, () -> service.createTariff(createDto, 1L, 1L, 1L));
        }
    }

    @Test
    void createTariff_InvalidContainerSize() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            createDto.setTariffDetails(List.of(detailCreateDto));

            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), eq("PORT_TARIFF_TYPES"))).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName("20", "SHIP_CONTAINER_SIZE_PORT")).thenReturn(null);
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), any())).thenReturn(false);

            assertThrows(ValidationException.class, () -> service.createTariff(createDto, 1L, 1L, 1L));
        }
    }

    @Test
    void updateTariff_DuplicateDocRef() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            updateDto.setDocRef("DOC002");

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testTariffHdr));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), anyLong())).thenReturn(false);
            when(tariffHdrRepository.existsByDocRefExcludingPoid("DOC002", 1L)).thenReturn(true);

            assertThrows(ValidationException.class, () -> service.updateTariff(1L, updateDto, 1L, 1L, 1L));
        }
    }

    @Test
    void updateTariff_WithDetails() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC_ID");

            detailUpdateDto.setActionType("ACTION_ISUPDATED");
            updateDto.setTariffDetails(List.of(detailUpdateDto));

            ShipPortTariffDtl existingDetail = new ShipPortTariffDtl();
            existingDetail.setDetRowId(1L);

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testTariffHdr));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), anyLong())).thenReturn(false);
            when(tariffHdrRepository.save(any(ShipPortTariffHdr.class))).thenReturn(testTariffHdr);
            when(mapper.mapToDto(testTariffHdr)).thenReturn(testDto);
            when(tariffDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(tariffDtlRepository.findById(new ShipPortTariffDtlId(1L, 1L))).thenReturn(Optional.of(existingDetail));
            when(mapper.mapDetailsToDto(anyList())).thenReturn(Collections.emptyList());

            PortStorageTariffDto result = service.updateTariff(1L, updateDto, 1L, 1L, 1L);

            assertNotNull(result);
            verify(tariffDtlRepository).saveAll(anyList());
        }
    }

    @Test
    void updateTariff_WithNewDetails() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC_ID");

            TariffDetailUpdateDTO newDetail = TariffDetailUpdateDTO.builder()
                    .actionType("ACTION_ISCREATED")
                    .containerTypePoid(200L)
                    .containerSize(new BigDecimal("40"))
                    .freeDays(7)
                    .slab1Tilldays(15)
                    .slab1Rate(new BigDecimal("150.00"))
                    .build();

            updateDto.setTariffDetails(List.of(newDetail));

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testTariffHdr));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), anyLong())).thenReturn(false);
            when(tariffHdrRepository.save(any(ShipPortTariffHdr.class))).thenReturn(testTariffHdr);
            when(mapper.mapToDto(testTariffHdr)).thenReturn(testDto);
            when(tariffDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(tariffDtlRepository.getMaxDetRowId(1L)).thenReturn(null);
            when(tariffDtlRepository.save(any(ShipPortTariffDtl.class))).thenReturn(new ShipPortTariffDtl());
            when(mapper.mapDetailUpdateDTOToEntity(any(), anyLong(), anyString())).thenReturn(new ShipPortTariffDtl());
            when(mapper.mapDetailsToDto(anyList())).thenReturn(Collections.emptyList());

            PortStorageTariffDto result = service.updateTariff(1L, updateDto, 1L, 1L, 1L);

            assertNotNull(result);
            verify(tariffDtlRepository).save(any(ShipPortTariffDtl.class));
        }
    }

    @Test
    void updateTariff_DeleteExistingDetails() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC_ID");

            TariffDetailUpdateDTO deleteDetail = TariffDetailUpdateDTO.builder()
                    .actionType("ACTION_ISDELETED")
                    .detRowId(1L)
                    .build();

            updateDto.setTariffDetails(List.of(deleteDetail));

            ShipPortTariffDtl existingDetail = new ShipPortTariffDtl();
            existingDetail.setDetRowId(1L);

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testTariffHdr));
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), anyLong())).thenReturn(false);
            when(tariffHdrRepository.save(any(ShipPortTariffHdr.class))).thenReturn(testTariffHdr);
            when(mapper.mapToDto(testTariffHdr)).thenReturn(testDto);
            when(tariffDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(tariffDtlRepository.findAllById(anyList())).thenReturn(List.of(existingDetail));
            when(mapper.mapDetailsToDto(anyList())).thenReturn(Collections.emptyList());

            PortStorageTariffDto result = service.updateTariff(1L, updateDto, 1L, 1L, 1L);

            assertNotNull(result);
            verify(tariffDtlRepository).deleteAllInBatch(anyList());
        }
    }

    @Test
    void getTariff_WithLovEnrichmentErrors() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            TariffDetailDto detailDto = TariffDetailDto.builder()
                    .containerTypePoid(200L)
                    .containerSize(new BigDecimal("20"))
                    .build();
            testDto.setTariffDetails(List.of(detailDto));

            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testTariffHdr));
            when(mapper.mapToDto(testTariffHdr)).thenReturn(testDto);
            when(tariffDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(mapper.mapDetailsToDto(anyList())).thenReturn(List.of(detailDto));
            
            // Mock LOV service to throw exception
            when(lovService.getDetailsByPoidAndLovName(100L, "PORT_MASTER")).thenThrow(new RuntimeException("LOV error"));
            when(lovService.getDetailsByPoidAndLovName(200L, "CONTAINER_TYPE_MASTER")).thenThrow(new RuntimeException("LOV error"));
            when(lovService.getDetailsByCodeAndLovName("20", "SHIP_CONTAINER_SIZE_PORT")).thenThrow(new RuntimeException("LOV error"));

            PortStorageTariffDto result = service.getTariff(1L);

            assertNotNull(result);
            // Should still return result even with LOV errors
        }
    }

    @Test
    void deleteTariff_WithDeleteReason() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC_ID");

            testTariffHdr.setDeleted("N");
            when(tariffHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.of(testTariffHdr));

            service.deleteTariff(1L, 1L, 1L, deleteReasonDto);

            verify(documentDeleteService).deleteDocument(eq(1L), eq("SHIP_PORT_TARIFF_HDR"), eq("TRANSACTION_POID"), eq(deleteReasonDto), any(LocalDate.class));
        }
    }

    @Test
    void validateSlabDetails_AllSlabValidations() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC_ID");

            // Test all slab validations
            TariffDetailCreateDTO validDetail = TariffDetailCreateDTO.builder()
                    .containerTypePoid(200L)
                    .containerSize(new BigDecimal("20"))
                    .freeDays(5)
                    .slab1Tilldays(10)
                    .slab1Rate(new BigDecimal("100.00"))
                    .slab2Tilldays(20)
                    .slab2Rate(new BigDecimal("200.00"))
                    .slab3Tilldays(30)
                    .slab3Rate(new BigDecimal("300.00"))
                    .slab4Tilldays(40)
                    .slab4Rate(new BigDecimal("400.00"))
                    .slab5Tilldays(50)
                    .slab5Rate(new BigDecimal("500.00"))
                    .slab6Tilldays(60)
                    .slab6Rate(new BigDecimal("600.00"))
                    .slab7Tilldays(70)
                    .slab7Rate(new BigDecimal("700.00"))
                    .build();

            createDto.setTariffDetails(List.of(validDetail));

            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), any())).thenReturn(false);
            when(tariffHdrRepository.save(any(ShipPortTariffHdr.class))).thenReturn(testTariffHdr);
            when(mapper.mapToDto(testTariffHdr)).thenReturn(testDto);
            when(mapper.mapDetailCreateDTOToEntity(any(), anyLong(), anyString())).thenReturn(new ShipPortTariffDtl());
            when(tariffDtlRepository.save(any(ShipPortTariffDtl.class))).thenReturn(new ShipPortTariffDtl());
            when(tariffDtlRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
            when(mapper.mapDetailsToDto(anyList())).thenReturn(Collections.emptyList());

            // Should succeed with valid slab sequence
            PortStorageTariffDto result = service.createTariff(createDto, 1L, 1L, 1L);
            assertNotNull(result);
        }
    }

    @Test
    void validateSlabDetails_MissingRateForSlab() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            TariffDetailCreateDTO invalidDetail = TariffDetailCreateDTO.builder()
                    .containerTypePoid(200L)
                    .containerSize(new BigDecimal("20"))
                    .freeDays(5)
                    .slab2Tilldays(20)
                    // Missing slab2Rate
                    .build();

            createDto.setTariffDetails(List.of(invalidDetail));

            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(createLovDto());
            when(lovService.getDetailsByCodeAndLovName(anyString(), anyString())).thenReturn(createLovDto());
            when(tariffHdrRepository.existsOverlappingPeriod(anyLong(), anyString(), anyLong(), any(), any(), any())).thenReturn(false);
            when(tariffHdrRepository.save(any(ShipPortTariffHdr.class))).thenReturn(testTariffHdr);

            assertThrows(ValidationException.class, () -> service.createTariff(createDto, 1L, 1L, 1L));
        }
    }
}
