package com.asg.shipping.linemasterthirdparty.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.dto.FilterDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.shipping.linemasterthirdparty.dto.*;
import com.asg.shipping.linemasterthirdparty.entity.ShipLineMaster;
import com.asg.shipping.linemasterthirdparty.repository.ShipLineMasterThirdPartyRepository;
import com.asg.shipping.linemasterthirdparty.util.LineMasterThirdPartyMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LineMasterThirdPartyServiceImplTest {

    @Mock
    private ShipLineMasterThirdPartyRepository lineRepository;

    @Mock
    private LovDataService lovService;

    @Mock
    private LineMasterThirdPartyMapper mapper;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @Mock
    private LoggingService loggingService;

    @Mock
    private DocumentSearchService documentSearchService;

    @InjectMocks
    private LineMasterThirdPartyServiceImpl service;

    private ShipLineMaster testLine;
    private LineMasterThirdPartyDto testDto;
    private LineMasterThirdPartyCreateDTO createDto;
    private LineMasterThirdPartyUpdateDTO updateDto;

    @BeforeEach
    void setUp() {
        testLine = ShipLineMaster.builder()
                .linePoid(1L)
                .lineCode("TP001")
                .lineName("Test Third Party Line")
                .lineName2("Test TP")
                .lineAddress("123 Test St")
                .countryPoid(100L)
                .currencyPoid(200L)
                .billTo("CUST001")
                .active("Y")
                .deleted("N")
                .lineType("THIRD_PARTY")
                .groupPoid(1L)
                .companyPoid(1L)
                .build();

        testDto = LineMasterThirdPartyDto.builder()
                .linePoid(1L)
                .lineCode("TP001")
                .lineName("Test Third Party Line")
                .active("Y")
                .build();

        createDto = LineMasterThirdPartyCreateDTO.builder()
                .lineCode("TP001")
                .lineName("Test Third Party Line")
                .active("Y")
                .build();

        updateDto = LineMasterThirdPartyUpdateDTO.builder()
                .lineName("Updated Third Party Line")
                .active("Y")
                .build();
    }

    @Test
    void searchThirdPartyLines_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            com.asg.common.lib.dto.FilterRequestDto filterRequest = new com.asg.common.lib.dto.FilterRequestDto("AND", "N", new ArrayList<>());


            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);

            when(documentSearchService.resolveOperator(any())).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(any())).thenReturn("N");
            when(documentSearchService.resolveFilters(any())).thenReturn(new ArrayList<>());
            when(documentSearchService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn(new RawSearchResult(Collections.emptyList(), new HashMap<>(), 0l));

            Map<String, Object> result = service.searchThirdPartyLines(filterRequest, pageable);

            assertNotNull(result);
        }
    }

    @Test
    void searchThirdPartyLines_WithFilters() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);

            FilterRequestDto filterRequest =new FilterRequestDto("OR", "N", List.of(new FilterDto("lineName", "Test")));

            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);

            when(documentSearchService.resolveOperator(any())).thenReturn("OR");
            when(documentSearchService.resolveIsDeleted(any())).thenReturn("N");
            when(documentSearchService.resolveFilters(any())).thenReturn(new ArrayList<>(Arrays.asList(new FilterDto("lineName", "Test"))));
            when(documentSearchService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn(new RawSearchResult(Collections.emptyList(), new HashMap<>(), 0L));

            Map<String, Object> result = service.searchThirdPartyLines(filterRequest, pageable);

            assertNotNull(result);
        }
    }

    @Test
    void getThirdPartyLine_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("SHIP_LINE_MASTER");

            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.of(testLine));
            when(mapper.mapToDto(testLine)).thenReturn(testDto);
            when(lovService.getDetailsByPoidAndLovName(anyLong(), anyString())).thenReturn(new LovGetListDto());
            lenient().when(lovService.getLovItemByCodeFast(anyString(), anyString())).thenReturn(new LovGetListDto());

            LineMasterThirdPartyDto result = service.getThirdPartyLine(1L);

            assertNotNull(result);
            assertEquals(1L, result.getLinePoid());
            verify(lineRepository).findByLinePoidAndGroupPoidAndThirdParty(1L, 1L);
        }
    }

    @Test
    void getThirdPartyLine_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getThirdPartyLine(1L));
        }
    }

    @Test
    void getThirdPartyLine_WrongLineType() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            testLine.setLineType("REGULAR");
            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.of(testLine));

            assertThrows(ResourceNotFoundException.class, () -> service.getThirdPartyLine(1L));
        }
    }

    @Test
    void createThirdPartyLine_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("SHIP_LINE_MASTER");

            when(lineRepository.existsByLineCodeAndGroupPoidAndThirdParty(anyString(), anyLong())).thenReturn(false);
            when(lineRepository.existsByLineNameAndGroupPoidAndThirdParty(anyString(), anyLong())).thenReturn(false);
            when(lineRepository.save(any(ShipLineMaster.class))).thenReturn(testLine);
            when(mapper.mapToDto(testLine)).thenReturn(testDto);

            LineMasterThirdPartyDto result = service.createThirdPartyLine(createDto);

            assertNotNull(result);
            verify(lineRepository).save(any(ShipLineMaster.class));
            verify(mapper).mapCreateDTOToEntity(eq(createDto), any(ShipLineMaster.class), eq(1L), eq(1L), eq(1L));
        }
    }

    @Test
    void createThirdPartyLine_DuplicateLineCode() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(lineRepository.existsByLineCodeAndGroupPoidAndThirdParty(anyString(), anyLong())).thenReturn(true);

            assertThrows(ValidationException.class, () -> service.createThirdPartyLine(createDto));
        }
    }

    @Test
    void createThirdPartyLine_DuplicateLineName() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(lineRepository.existsByLineCodeAndGroupPoidAndThirdParty(anyString(), anyLong())).thenReturn(false);
            when(lineRepository.existsByLineNameAndGroupPoidAndThirdParty(anyString(), anyLong())).thenReturn(true);

            assertThrows(ValidationException.class, () -> service.createThirdPartyLine(createDto));
        }
    }

    @Test
    void createThirdPartyLine_InvalidCountry() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            createDto.setCountryPoid(100L);
            when(lineRepository.existsByLineCodeAndGroupPoidAndThirdParty(anyString(), anyLong())).thenReturn(false);
            when(lineRepository.existsByLineNameAndGroupPoidAndThirdParty(anyString(), anyLong())).thenReturn(false);
            when(lovService.getDetailsByPoidAndLovName(anyLong(), eq("COUNTRY"))).thenReturn(null);

            assertThrows(ValidationException.class, () -> service.createThirdPartyLine(createDto));
        }
    }

    @Test
    void createThirdPartyLine_InvalidCurrency() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            createDto.setCurrencyPoid(200L);
            when(lineRepository.existsByLineCodeAndGroupPoidAndThirdParty(anyString(), anyLong())).thenReturn(false);
            when(lineRepository.existsByLineNameAndGroupPoidAndThirdParty(anyString(), anyLong())).thenReturn(false);
            when(lovService.getDetailsByPoidAndLovName(anyLong(), eq("CURRENCY"))).thenReturn(null);

            assertThrows(ValidationException.class, () -> service.createThirdPartyLine(createDto));
        }
    }



    @Test
    void updateThirdPartyLine_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.of(testLine));
            when(lineRepository.existsByLineNameAndGroupPoidAndThirdPartyExcluding(anyString(), anyLong(), anyLong())).thenReturn(false);
            when(lineRepository.save(any(ShipLineMaster.class))).thenReturn(testLine);
            when(mapper.mapToDto(testLine)).thenReturn(testDto);

            LineMasterThirdPartyDto result = service.updateThirdPartyLine(1L, updateDto);

            assertNotNull(result);
            verify(lineRepository).save(any(ShipLineMaster.class));
            verify(mapper).mapUpdateDTOToEntity(eq(updateDto), eq(testLine), eq(1L), eq(1L), eq(1L));
        }
    }

    @Test
    void updateThirdPartyLine_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.updateThirdPartyLine(1L, updateDto));
        }
    }

    @Test
    void updateThirdPartyLine_WrongLineType() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            testLine.setLineType("REGULAR");
            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.of(testLine));

            assertThrows(ResourceNotFoundException.class, () -> service.updateThirdPartyLine(1L, updateDto));
        }
    }

    @Test
    void updateThirdPartyLine_DuplicateLineName() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.of(testLine));
            when(lineRepository.existsByLineNameAndGroupPoidAndThirdPartyExcluding(anyString(), anyLong(), anyLong())).thenReturn(true);

            assertThrows(ValidationException.class, () -> service.updateThirdPartyLine(1L, updateDto));
        }
    }

    @Test
    void toggleActive_Success_FromYToN() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            testLine.setActive("Y");
            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.of(testLine));
            when(lineRepository.save(any(ShipLineMaster.class))).thenReturn(testLine);

            service.toggleActive(1L);

            verify(lineRepository).save(argThat(line -> "N".equals(line.getActive())));
        }
    }

    @Test
    void toggleActive_Success_FromNToY() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            testLine.setActive("N");
            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.of(testLine));
            when(lineRepository.save(any(ShipLineMaster.class))).thenReturn(testLine);

            service.toggleActive(1L);

            verify(lineRepository).save(argThat(line -> "Y".equals(line.getActive())));
        }
    }

    @Test
    void toggleActive_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.toggleActive(1L));
        }
    }

    @Test
    void toggleActive_WrongLineType() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            testLine.setLineType("REGULAR");
            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.of(testLine));

            assertThrows(ResourceNotFoundException.class, () -> service.toggleActive(1L));
        }
    }

    @Test
    void deleteThirdPartyLine_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            testLine.setDeleted("N");
            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.of(testLine));
            when(lineRepository.save(any(ShipLineMaster.class))).thenReturn(testLine);

            service.deleteThirdPartyLine(1L);

            verify(lineRepository).save(argThat(line -> 
                "Y".equals(line.getDeleted()) && "N".equals(line.getActive())
            ));
        }
    }

    @Test
    void deleteThirdPartyLine_AlreadyDeleted() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            testLine.setDeleted("Y");
            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.of(testLine));

            service.deleteThirdPartyLine(1L);

            verify(lineRepository, never()).save(any(ShipLineMaster.class));
        }
    }

    @Test
    void deleteThirdPartyLine_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.deleteThirdPartyLine(1L));
        }
    }

    @Test
    void deleteThirdPartyLine_WrongLineType() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            testLine.setLineType("REGULAR");
            when(lineRepository.findByLinePoidAndGroupPoidAndThirdParty(1L, 1L)).thenReturn(Optional.of(testLine));

            assertThrows(ResourceNotFoundException.class, () -> service.deleteThirdPartyLine(1L));
        }
    }
}
