package com.asg.shipping.deliveryorderissuetocustomer.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.LovGetListDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.deliveryorderissuetocustomer.dto.DeliveryOrderIssueToCustomerDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.IssueDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.UpdateDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.entity.ShipBlManifestHDR;
import com.asg.shipping.deliveryorderissuetocustomer.repository.DeliveryOrderIssueToCustomerRepository;
import com.asg.shipping.deliveryorderissuetocustomer.repository.ShipBlManifestHDRRepository;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeliveryOrderIssueToCustomerServiceImplTest {

    @Mock
    private DeliveryOrderIssueToCustomerRepository viewRepository;

    @Mock
    private ShipBlManifestHDRRepository blManifestRepository;

    @Mock
    private DocumentSearchService documentService;

    @Mock
    private LovDataService lovService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DeliveryOrderIssueToCustomerServiceImpl service;

    private DeliveryOrderIssueToCustomerDto mockDto;
    private ShipBlManifestHDR mockEntity;
    private IssueDeliveryOrderRequestDto issueRequest;
    private UpdateDeliveryOrderRequestDto updateRequest;

    @BeforeEach
    void setUp() {
        mockDto = DeliveryOrderIssueToCustomerDto.builder()
                .transactionPoid(1L)
                .companyPoid(100L)
                .docRef("DO-001")
                .transactionDate(LocalDate.now())
                .jobNo("JOB001")
                .blNumber("BL001")
                .consignee("Test Consignee")
                .doPriority("HIGH")
                .doIssueAuthPoid(200L)
                .deliverySentTo("C")
                .build();

        mockEntity = new ShipBlManifestHDR();
        mockEntity.setTransactionPoid(1L);
        mockEntity.setGroupPoid(1L);
        mockEntity.setCompanyPoid(100L);
        mockEntity.setBlNumber("BL001");
        mockEntity.setDeleted("N");
        mockEntity.setCreatedBy("testuser");
        mockEntity.setCreatedDate(LocalDateTime.now());

        issueRequest = IssueDeliveryOrderRequestDto.builder()
                .doReleasedIdPerson("REL001")
                .doReleasedToPerson("John Doe")
                .doReleasedAddressPerson("123 Test Street")
                .originalBlReleaseCr("Y")
                .doPriority("HIGH")
                .emailsConsg("consignee@example.com")
                .emailsDo("do@example.com")
                .deliverySentTo("C")
                .doCntToConsignee("Y")
                .doCntToNotify("N")
                .doCntToOthers("N")
                .build();

        updateRequest = UpdateDeliveryOrderRequestDto.builder()
                .doReleasedIdPerson("REL002")
                .doReleasedToPerson("Jane Doe")
                .doPriority("MEDIUM")
                .deliverySentTo("N")
                .build();
    }

    @Test
    void getDeliveryOrderIssueToCustomer_Success() {
        Long transactionPoid = 1L;
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            
            when(viewRepository.findByTransactionPoid(transactionPoid, 100L)).thenReturn(Optional.of(mockDto));
            when(lovService.getDetailsByCodeAndLovName("HIGH", "DO_PRIORITY_SH"))
                    .thenReturn(new LovGetListDto(1L, "HIGH", "High Priority", null, null, null, null));
            when(lovService.getDetailsByPoidAndLovName(200L, "USER_MASTER"))
                    .thenReturn(new LovGetListDto(2L, "AUTH001", "Authorized Person", null, null, null, null));
            when(lovService.getDetailsByCodeAndLovName("C", "DELIVERY_SENT_TO"))
                    .thenReturn(new LovGetListDto(3L, "C", "Consignee", null, null, null, null));

            DeliveryOrderIssueToCustomerDto result = service.getDeliveryOrderIssueToCustomer(transactionPoid);

            assertNotNull(result);
            assertEquals(transactionPoid, result.getTransactionPoid());
            verify(viewRepository).findByTransactionPoid(transactionPoid, 100L);
        }
    }

    @Test
    void getDeliveryOrderIssueToCustomer_NotFound() {
        Long transactionPoid = 1L;
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            
            when(viewRepository.findByTransactionPoid(transactionPoid, 100L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> service.getDeliveryOrderIssueToCustomer(transactionPoid));
        }
    }

    @Test
    void issueDeliveryOrder_ValidationError_MissingDeliverySentTo() {
        Long transactionPoid = 1L;
        IssueDeliveryOrderRequestDto invalidRequest = IssueDeliveryOrderRequestDto.builder()
                .doReleasedIdPerson("REL001")
                .doPriority("HIGH")
                .emailsDo("do@example.com")
                .build();

        ValidationException exception = assertThrows(ValidationException.class, 
                () -> service.issueDeliveryOrder(transactionPoid, invalidRequest));
        
        assertEquals("Delivery sent to is required", exception.getMessage());
    }

    @Test
    void issueDeliveryOrder_ValidationError_MissingDoEmails() {
        Long transactionPoid = 1L;
        IssueDeliveryOrderRequestDto invalidRequest = IssueDeliveryOrderRequestDto.builder()
                .doReleasedIdPerson("REL001")
                .doPriority("HIGH")
                .deliverySentTo("C")
                .build();

        ValidationException exception = assertThrows(ValidationException.class, 
                () -> service.issueDeliveryOrder(transactionPoid, invalidRequest));
        
        assertEquals("Delivery emails not added for customer", exception.getMessage());
    }

    @Test
    void issueDeliveryOrder_EntityNotFound() {
        Long transactionPoid = 1L;
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getGroupPoid).thenReturn(1L);
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            userContextMock.when(UserContext::getUserName).thenReturn("testuser");
            
            when(blManifestRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(transactionPoid, 1L, 100L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> service.issueDeliveryOrder(transactionPoid, issueRequest));
        }
    }

    @Test
    void issueDeliveryOrder_EntityDeleted() {
        Long transactionPoid = 1L;
        mockEntity.setDeleted("Y");
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getGroupPoid).thenReturn(1L);
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            userContextMock.when(UserContext::getUserName).thenReturn("testuser");
            
            when(blManifestRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(transactionPoid, 1L, 100L))
                    .thenReturn(Optional.of(mockEntity));

            assertThrows(ResourceNotFoundException.class, 
                    () -> service.issueDeliveryOrder(transactionPoid, issueRequest));
        }
    }

    @Test
    void updateDeliveryOrder_Success() {
        Long transactionPoid = 1L;
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getGroupPoid).thenReturn(1L);
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            userContextMock.when(UserContext::getUserName).thenReturn("testuser");
            
            when(blManifestRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(transactionPoid, 1L, 100L))
                    .thenReturn(Optional.of(mockEntity));
            when(blManifestRepository.save(any(ShipBlManifestHDR.class))).thenReturn(mockEntity);
            when(viewRepository.findByTransactionPoid(transactionPoid, 100L)).thenReturn(Optional.of(mockDto));
            when(lovService.getDetailsByCodeAndLovName(any(), any())).thenReturn(new LovGetListDto());
            when(lovService.getDetailsByPoidAndLovName(any(), any())).thenReturn(new LovGetListDto());

            DeliveryOrderIssueToCustomerDto result = service.updateDeliveryOrder(transactionPoid, updateRequest);

            assertNotNull(result);
            verify(blManifestRepository).save(any(ShipBlManifestHDR.class));
        }
    }

    @Test
    void updateDeliveryOrder_EntityNotFound() {
        Long transactionPoid = 1L;
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getGroupPoid).thenReturn(1L);
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            userContextMock.when(UserContext::getUserName).thenReturn("testuser");
            
            when(blManifestRepository.findByTransactionPoidAndGroupPoidAndCompanyPoid(transactionPoid, 1L, 100L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> service.updateDeliveryOrder(transactionPoid, updateRequest));
        }
    }
}