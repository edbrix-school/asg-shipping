package com.asg.shipping.customerautochargeexportbl.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeDetailDto;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLCreateDTO;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLDto;
import com.asg.shipping.customerautochargeexportbl.dto.CustomerAutoChargeExportBLUpdateDTO;
import com.asg.shipping.customerautochargeexportbl.entity.ShipCustomerChargesDtlEntity;
import com.asg.shipping.customerautochargeexportbl.entity.ShipCustomerChargesHdrEntity;
import com.asg.shipping.customerautochargeexportbl.repository.ShipCustomerChargesDtlRepository;
import com.asg.shipping.customerautochargeexportbl.repository.ShipCustomerChargesHdrRepository;
import com.asg.shipping.customerautochargeexportbl.service.impl.CustomerAutoChargeExportBlServiceImpl;
import com.asg.shipping.customerautochargeexportbl.util.CustomerAutoChargeExportBLMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAutoChargeExportBlServiceImplTest {

    @Mock
    private DocumentSearchService documentSearchService;

    @Mock
    private ShipCustomerChargesHdrRepository headerRepository;

    @Mock
    private ShipCustomerChargesDtlRepository detailRepository;

    @Mock
    private CustomerAutoChargeExportBLMapper mapper;

    @Mock
    private LoggingService loggingService;

    @InjectMocks
    private CustomerAutoChargeExportBlServiceImpl service;

    private CustomerAutoChargeExportBLCreateDTO createDTO;
    private CustomerAutoChargeExportBLUpdateDTO updateDTO;
    private ShipCustomerChargesHdrEntity headerEntity;
    private CustomerAutoChargeExportBLDto responseDto;

    @BeforeEach
    void setUp() {
        createDTO = new CustomerAutoChargeExportBLCreateDTO();
        createDTO.setCustomerPoid(100L);
        createDTO.setDescription("Test Charge");
        createDTO.setPeriodFrom(LocalDate.of(2024, 1, 1));
        createDTO.setPeriodTo(LocalDate.of(2024, 12, 31));
        createDTO.setDocRef("DOC001");

        updateDTO = new CustomerAutoChargeExportBLUpdateDTO();
        updateDTO.setDescription("Updated Charge");
        updateDTO.setPeriodFrom(LocalDate.of(2024, 1, 1));
        updateDTO.setPeriodTo(LocalDate.of(2024, 12, 31));

        headerEntity = new ShipCustomerChargesHdrEntity();
        headerEntity.setTransactionPoid(1L);
        headerEntity.setCustomerPoid(100L);
        headerEntity.setDescription("Test Charge");
        headerEntity.setDocRef("DOC001");
        headerEntity.setDeleted("N");
        headerEntity.setCreatedBy("user1");
        headerEntity.setCreatedDate(LocalDateTime.now());

        responseDto = CustomerAutoChargeExportBLDto.builder()
                .transactionPoid(1L)
                .customerPoid(100L)
                .description("Test Charge")
                .docRef("DOC001")
                .periodFrom(LocalDate.of(2024, 1, 1))
                .periodTo(LocalDate.of(2024, 12, 31))
                .createdBy("user1")
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    void testListSuccess() {
        RawSearchResult rawResult = new RawSearchResult(
                Collections.emptyList(),
                Collections.emptyMap(),
                0L
        );

        when(documentSearchService.resolveOperator(any())).thenReturn("AND");
        when(documentSearchService.resolveIsDeleted(any())).thenReturn("N");
        when(documentSearchService.resolveFilters(any())).thenReturn(Collections.emptyList());

        when(documentSearchService.search(
                anyString(),
                anyList(),
                anyString(),
                any(Pageable.class),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(rawResult);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-102");

            FilterRequestDto filters = new FilterRequestDto(null, null, Collections.emptyList());
            Pageable pageable = PageRequest.of(0, 10);

            Map<String, Object> result = service.list(filters, pageable);

            assertNotNull(result);

            verify(documentSearchService).search(
                    anyString(),
                    anyList(),
                    anyString(),
                    any(Pageable.class),
                    anyString(),
                    anyString(),
                    anyString()
            );
        }
    }


    @Test
    void testGetSuccess() {
        when(headerRepository.findById(1L)).thenReturn(Optional.of(headerEntity));
        when(detailRepository.findByTransactionPoidOrderByDetRowId(1L)).thenReturn(Collections.emptyList());
        when(mapper.mapToDto(any())).thenReturn(responseDto);
        when(mapper.mapDtlListToDto(any())).thenReturn(Collections.emptyList());

        CustomerAutoChargeExportBLDto result = service.getCustomerAutoChargeExportBL(1L);

        assertNotNull(result);
        assertEquals(1L, result.getTransactionPoid());
        verify(headerRepository).findById(1L);
        verify(detailRepository).findByTransactionPoidOrderByDetRowId(1L);
    }

    @Test
    void testGetNotFound() {
        when(headerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getCustomerAutoChargeExportBL(1L));
    }

    @Test
    void testCreateSuccess() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {

            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(10L);



            doAnswer(invocation -> {
                ShipCustomerChargesHdrEntity entity = invocation.getArgument(1);
                entity.setTransactionPoid(1L);
                return null;
            }).when(mapper).mapCreateDTOToEntity(any(), any(), anyLong());

            when(headerRepository.save(any()))
                    .thenReturn(headerEntity);

            when(headerRepository.findById(1L))
                    .thenReturn(Optional.of(headerEntity));

            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(Collections.emptyList());

            when(mapper.mapToDto(any()))
                    .thenReturn(responseDto);

            when(mapper.mapDtlListToDto(any()))
                    .thenReturn(Collections.emptyList());


            CustomerAutoChargeExportBLDto result =
                    service.createCustomerAutoChargeExportBL(createDTO);

            assertNotNull(result);
            verify(headerRepository).save(any());
            verify(headerRepository).findById(1L);
        }
    }



    @Test
    void testCreateWithoutDocRef() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {

            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(10L);

            createDTO.setDocRef(null);

            doAnswer(invocation -> {
                ShipCustomerChargesHdrEntity entity = invocation.getArgument(1);
                entity.setTransactionPoid(1L);
                return null;
            }).when(mapper).mapCreateDTOToEntity(any(), any(), anyLong());

            when(headerRepository.save(any()))
                    .thenReturn(headerEntity);

            when(headerRepository.findById(1L))
                    .thenReturn(Optional.of(headerEntity));

            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(Collections.emptyList());

            when(mapper.mapToDto(any()))
                    .thenReturn(responseDto);

            when(mapper.mapDtlListToDto(any()))
                    .thenReturn(Collections.emptyList());



            CustomerAutoChargeExportBLDto result =
                    service.createCustomerAutoChargeExportBL(createDTO);

            assertNotNull(result);

            verify(headerRepository).save(any());

            verify(headerRepository, never())
                    .findByDocRefAndDeleted(anyString(), anyString());
        }
    }





    @Test
    void testCreateInvalidPeriodDates() {
        createDTO.setPeriodFrom(LocalDate.of(2024, 12, 31));
        createDTO.setPeriodTo(LocalDate.of(2024, 1, 1));

        assertThrows(ValidationException.class, () -> service.createCustomerAutoChargeExportBL(createDTO));
    }

    @Test
    void testUpdateNotFound() {
        when(headerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateCustomerAutoChargeExportBL(1L, updateDTO));
    }

    @Test
    void testUpdateInvalidPeriodDates() {
        when(headerRepository.findById(1L)).thenReturn(Optional.of(headerEntity));

        updateDTO.setPeriodFrom(LocalDate.of(2024, 12, 31));
        updateDTO.setPeriodTo(LocalDate.of(2024, 1, 1));

        assertThrows(ValidationException.class, () -> service.updateCustomerAutoChargeExportBL(1L, updateDTO));
    }

    @Test
    void testUpdateWithChargeDetails_create() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {

            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC1");

            CustomerAutoChargeDetailDto detailDto = new CustomerAutoChargeDetailDto();
            detailDto.setChargeCodePoid(50L);
            detailDto.setActionType("ISCREATED");   // ✅ REQUIRED

            updateDTO.setChargeDetails(List.of(detailDto));

            when(headerRepository.findById(1L))
                    .thenReturn(Optional.of(headerEntity));

            doNothing().when(mapper).mapUpdateDTOToEntity(any(), any());

            when(headerRepository.save(any()))
                    .thenReturn(headerEntity);

            when(detailRepository.getMaxDetRowId(1L))
                    .thenReturn(0L);

            when(mapper.mapDtlFromDto(any(), anyLong(), anyLong()))
                    .thenReturn(new ShipCustomerChargesDtlEntity());

            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(Collections.emptyList());

            when(mapper.mapToDto(any()))
                    .thenReturn(responseDto);

            when(mapper.mapDtlListToDto(any()))
                    .thenReturn(Collections.emptyList());

            CustomerAutoChargeExportBLDto result =
                    service.updateCustomerAutoChargeExportBL(1L, updateDTO);

            assertNotNull(result);

            verify(detailRepository).getMaxDetRowId(1L);
            verify(detailRepository).save(any(ShipCustomerChargesDtlEntity.class));

            verify(detailRepository, never()).deleteByTransactionPoid(anyLong());
        }
    }
    @Test
    void testUpdateWithChargeDetails_delete() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {

            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC1");

            CustomerAutoChargeDetailDto detailDto = new CustomerAutoChargeDetailDto();
            detailDto.setActionType("ISDELETED");
            detailDto.setDetRowId(10L);

            updateDTO.setChargeDetails(List.of(detailDto));

            when(headerRepository.findById(1L))
                    .thenReturn(Optional.of(headerEntity));

            when(headerRepository.save(any()))
                    .thenReturn(headerEntity);

            when(detailRepository.findByTransactionPoidOrderByDetRowId(1L))
                    .thenReturn(Collections.emptyList());

            when(mapper.mapToDto(any()))
                    .thenReturn(responseDto);

            when(mapper.mapDtlListToDto(any()))
                    .thenReturn(Collections.emptyList());

            service.updateCustomerAutoChargeExportBL(1L, updateDTO);

            verify(detailRepository)
                    .deleteByTransactionPoidAndDetRowId(1L, 10L);
        }
    }




    @Test
    void testDeleteNotFound() {
        when(headerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteCustomerAutoChargeExportBL(1L,new DeleteReasonDto()));
    }

    @Test
    void testCreateFailsWhenPeriodDatesAreNull() {
        createDTO.setPeriodFrom(null);
        createDTO.setPeriodTo(null);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {

            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(10L);

            assertThrows(ValidationException.class, () ->
                    service.createCustomerAutoChargeExportBL(createDTO)
            );

            verify(headerRepository, never()).save(any());
        }
    }



}
