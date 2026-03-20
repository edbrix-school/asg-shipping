package com.asg.shipping.vesselvoyagecreation.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.enums.LogDetailsEnum;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.PrintService;
import com.asg.common.lib.utility.DateUtil;
import com.asg.common.lib.utility.PaginationUtil;
import com.asg.shipping.exceptions.ResourceAlreadyExistsException;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.vesselvoyagecreation.dto.CurrencyUpdateItem;
import com.asg.shipping.vesselvoyagecreation.dto.CurrencyUpdateRequest;
import com.asg.shipping.vesselvoyagecreation.dto.TranshipmentTransferRequest;
import com.asg.shipping.vesselvoyagecreation.dto.TranshipmentUpdateItem;
import com.asg.shipping.vesselvoyagecreation.dto.TranshipmentUpdateRequest;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageResponse;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageUpsertRequest;
import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageHdrEntity;
import com.asg.shipping.vesselvoyagecreation.entity.ShipVoyageTranshipDtlEntity;
import com.asg.shipping.vesselvoyagecreation.entity.VwShipEdiExceptionUploadEntity;
import com.asg.shipping.vesselvoyagecreation.entity.VwShipVoyageCurrencyEntity;
import com.asg.shipping.vesselvoyagecreation.repository.*;
import com.asg.shipping.vesselvoyagecreation.service.impl.VesselVoyageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VesselVoyageServiceImplTest {

    @Mock
    private ShipVoyageHdrRepository voyageHdrRepository;
    @Mock
    private VoyageLineMasterRepository voyageLineMasterRepository;
    @Mock
    private ShipVoyageTranshipDtlRepository transhipDtlRepository;
    @Mock
    private VwShipEdiExceptionUploadRepository ediExceptionUploadRepository;
    @Mock
    private VwShipVoyageCurrencyRepository voyageCurrencyRepository;
    @Mock
    private StoredProcedureRepository storedProcedureRepository;
    @Mock
    private DocumentSearchService documentSearchService;
    @Mock
    private VoyageBillsRepository voyageBillsRepository;
    @Mock
    private LoggingService loggingService;
    @Mock
    private PrintService printService;
    @Mock
    private DataSource dataSource;

    @InjectMocks
    private VesselVoyageServiceImpl service;

    @BeforeEach
    void init() {
        // no-op; @Value fields will use defaults defined in implementation
    }

    @Test
    void listVoyages_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class);
             MockedStatic<PaginationUtil> mockedPagination = mockStatic(PaginationUtil.class)) {

            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-101");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(10L);

            FilterRequestDto filterRequest = new FilterRequestDto("AND", "N", new ArrayList<>());
            Pageable pageable = PageRequest.of(0, 20);

            when(documentSearchService.resolveOperator(any())).thenReturn("AND");
            when(documentSearchService.resolveIsDeleted(any())).thenReturn("N");
            when(documentSearchService.resolveFilters(any())).thenReturn(Collections.emptyList());
            when(documentSearchService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                    .thenReturn(new RawSearchResult(Collections.emptyList(), new HashMap<>(), 0L));

            mockedPagination.when(() -> PaginationUtil.wrapPage(any(), any()))
                    .thenReturn(Map.of("content", Collections.emptyList()));

            Map<String, Object> result = service.listVoyages(filterRequest, pageable, null, null, null);

            assertNotNull(result);
            verify(documentSearchService).search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString());
        }
    }

    @Test
    void getVoyage_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-101");

            ShipVoyageHdrEntity entity = new ShipVoyageHdrEntity();
            entity.setTransactionPoid(1L);
            entity.setLinePoid(10L);
            when(voyageHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L))
                    .thenReturn(Optional.of(entity));
            when(voyageLineMasterRepository.findLineCodeByLinePoid(10L)).thenReturn(Optional.of("LC"));

            VoyageResponse response = service.getVoyage(1L);

            assertNotNull(response);
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.VIEWED), eq("100-101"), eq("1"));
        }
    }

    @Test
    void getVoyage_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            when(voyageHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getVoyage(1L));
        }
    }

    @Test
    void createVoyage_Duplicate_ThrowsResourceAlreadyExists() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserId).thenReturn("user1");

            VoyageUpsertRequest request = new VoyageUpsertRequest();
            request.setLinePoid(10L);
            request.setVesselPoid(20L);
            request.setVoyageNo("V001");

            when(voyageLineMasterRepository.findCompanyPoidByLinePoid(10L)).thenReturn(Optional.of(1L));
            when(voyageHdrRepository.existsByGroupPoidAndLinePoidAndVesselPoidAndVoyageNo(1L, 10L, 20L, "V001"))
                    .thenReturn(true);

            assertThrows(ResourceAlreadyExistsException.class, () -> service.createVoyage(request));
        }
    }

    @Test
    void updateVoyage_NotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserId).thenReturn("user1");

            VoyageUpsertRequest request = new VoyageUpsertRequest();
            request.setLinePoid(10L);
            request.setVesselPoid(20L);
            request.setVoyageNo("V001");

            when(voyageHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.updateVoyage(1L, request));
        }
    }

    @Test
    void reprocessEdi_VoyageNotFound() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-101");
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(10L);

            when(voyageHdrRepository.existsById(1L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () -> service.reprocessEdi(1L));
        }
    }

    @Test
    void listTranshipments_Success() {
        when(transhipDtlRepository.findByTransactionPoidOrderByDetRowIdAsc(1L))
                .thenReturn(Collections.emptyList());

        assertNotNull(service.listTranshipments(1L));
        verify(transhipDtlRepository).findByTransactionPoidOrderByDetRowIdAsc(1L);
    }

    @Test
    void updateTranshipments_RowNotFound_Throws() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn("user1");

            ShipVoyageTranshipDtlEntity existing = new ShipVoyageTranshipDtlEntity();
            existing.setDetRowId(1L);
            when(transhipDtlRepository.findByTransactionPoidOrderByDetRowIdAsc(1L))
                    .thenReturn(List.of(existing));

            TranshipmentUpdateItem item = new TranshipmentUpdateItem();
            item.setDetRowId(2L);
            TranshipmentUpdateRequest req = new TranshipmentUpdateRequest();
            req.setItems(List.of(item));

            assertThrows(ResourceNotFoundException.class, () -> service.updateTranshipments(1L, req));
        }
    }

    @Test
    void transferTranshipments_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserId).thenReturn("user1");

            ShipVoyageTranshipDtlEntity e1 = new ShipVoyageTranshipDtlEntity();
            e1.setDetRowId(1L);
            e1.setIsLoaded("N");
            when(transhipDtlRepository.findByTransactionPoidOrderByDetRowIdAsc(1L))
                    .thenReturn(List.of(e1));

            TranshipmentTransferRequest req = new TranshipmentTransferRequest();
            req.setTargetVoyagePoid(2L);
            req.setDetRowIds(List.of(1L));

            String msg = service.transferTranshipments(1L, req);

            assertTrue(msg.contains("1 rows"));
            verify(transhipDtlRepository).saveAll(anyList());
        }
    }

    @Test
    void listVoyageCurrencies_Success() {
        when(voyageCurrencyRepository.findByTransactionPoidOrderByDetRowIdAsc(1L))
                .thenReturn(Collections.emptyList());

        assertNotNull(service.listVoyageCurrencies(1L));
        verify(voyageCurrencyRepository).findByTransactionPoidOrderByDetRowIdAsc(1L);
    }

    @Test
    void updateCurrencyRates_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserId).thenReturn("user1");

            CurrencyUpdateItem item = new CurrencyUpdateItem();
            item.setCurrencyCode("USD");
            item.setNewCurrencyExchange(3.5);
            CurrencyUpdateRequest req = new CurrencyUpdateRequest();
            req.setItems(List.of(item));

            String msg = service.updateCurrencyRates(1L, req);

            assertEquals("Currency updated", msg);
            verify(storedProcedureRepository).procShipVoyageCurrencyUpd(eq(1L), eq(1L), eq(1L), anyString(), anyString());
        }
    }

    @Test
    void deleteVoyage_SoftDelete_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getUserId).thenReturn("user1");
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("100-101");

            ShipVoyageHdrEntity entity = new ShipVoyageHdrEntity();
            entity.setTransactionPoid(1L);
            entity.setDeleted("N");
            when(voyageHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L))
                    .thenReturn(Optional.of(entity));

            service.deleteVoyage(1L);

            assertEquals("Y", entity.getDeleted());
            verify(loggingService).createLogSummaryEntry(eq(LogDetailsEnum.DELETED), eq("100-101"), eq("1"));
        }
    }

    @Test
    void deleteVoyage_AlreadyDeleted_NoOp() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(1L);

            ShipVoyageHdrEntity entity = new ShipVoyageHdrEntity();
            entity.setTransactionPoid(1L);
            entity.setDeleted("Y");
            when(voyageHdrRepository.findByTransactionPoidAndGroupPoid(1L, 1L))
                    .thenReturn(Optional.of(entity));

            service.deleteVoyage(1L);

            verify(voyageHdrRepository, never()).save(any());
        }
    }
}

