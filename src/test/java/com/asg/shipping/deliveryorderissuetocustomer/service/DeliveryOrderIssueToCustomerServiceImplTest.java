package com.asg.shipping.deliveryorderissuetocustomer.service;

import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.LoggingService;
import com.asg.common.lib.service.LovDataService;
import com.asg.shipping.deliveryorderissuetocustomer.dto.DeliveryOrderIssueToCustomerDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.IssueDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.UpdateDeliveryOrderRequestDto;
import com.asg.shipping.deliveryorderissuetocustomer.dto.ValidateDocumentDto;
import com.asg.shipping.deliveryorderissuetocustomer.entity.DoShPrintingDtl;
import com.asg.shipping.deliveryorderissuetocustomer.entity.ShipBlManifestHDR;
import com.asg.shipping.deliveryorderissuetocustomer.repository.DeliveryOrderIssueToCustomerRepository;
import com.asg.shipping.deliveryorderissuetocustomer.repository.DoShPrintingDtlRepository;
import com.asg.shipping.deliveryorderissuetocustomer.repository.ShipBlManifestHDRRepository;
import com.asg.shipping.receipts.repository.ReceiptHdrRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private DoShPrintingDtlRepository doShPrintingDtlRepository;

    @Mock
    private LovDataService lovService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private LoggingService loggingService;

    @Mock
    private ReceiptHdrRepository receiptHdrRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private DeliveryOrderIssueToCustomerServiceImpl service;

    private DeliveryOrderIssueToCustomerDto mockDto;
    private ShipBlManifestHDR mockEntity;
    private DoShPrintingDtl mockPrintingDtl;
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
                .blReleaseTypeOffice("OFFICE")
                .build();

        mockEntity = new ShipBlManifestHDR();
        mockEntity.setTransactionPoid(1L);
        mockEntity.setGroupPoid(1L);
        mockEntity.setCompanyPoid(100L);
        mockEntity.setBlNumber("BL001");
        mockEntity.setDeleted("N");

        mockPrintingDtl = new DoShPrintingDtl();
        mockPrintingDtl.setTransactionPoid(1L);
        mockPrintingDtl.setCompanyPoid(100L);

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

        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    @Test
    void getDeliveryOrderIssueToCustomer_Success() {
        Long transactionPoid = 1L;
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            
            when(viewRepository.findByTransactionPoid(transactionPoid)).thenReturn(Optional.of(mockDto));
            when(lovService.getDetailsByCodeAndLovName(any(), any())).thenReturn(null);
            when(lovService.getDetailsByPoidAndLovName(any(), any())).thenReturn(null);

            DeliveryOrderIssueToCustomerDto result = service.getDeliveryOrderIssueToCustomer(transactionPoid);

            assertNotNull(result);
            assertEquals(transactionPoid, result.getTransactionPoid());
            verify(viewRepository).findByTransactionPoid(transactionPoid);
        }
    }

    @Test
    void getDeliveryOrderIssueToCustomer_NotFound() {
        Long transactionPoid = 1L;
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            
            when(viewRepository.findByTransactionPoid(transactionPoid)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> service.getDeliveryOrderIssueToCustomer(transactionPoid));
        }
    }

    @Test
    @Disabled
    void issueDeliveryOrder_ValidationError_MissingDeliverySentTo() {
        Long transactionPoid = 1L;
        IssueDeliveryOrderRequestDto invalidRequest = IssueDeliveryOrderRequestDto.builder()
                .doReleasedIdPerson("REL001")
                .doReleasedToPerson("John Doe")
                .doReleasedAddressPerson("123 Test Street")
                .doPriority("HIGH")
                .emailsDo("do@example.com")
                .originalBlReleaseCr("OFFICE")
                .build();

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getGroupPoid).thenReturn(1L);
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            userContextMock.when(UserContext::getUserName).thenReturn("testuser");
            
            when(viewRepository.findByTransactionPoid(transactionPoid)).thenReturn(Optional.of(mockDto));

            ValidationException exception = assertThrows(ValidationException.class, 
                    () -> service.issueDeliveryOrder(transactionPoid, invalidRequest));
            
            assertEquals("Select delivery send to from dropdown list", exception.getMessage());
        }
    }

    @Test
    @Disabled
    void issueDeliveryOrder_ValidationError_MissingDoEmails() {
        Long transactionPoid = 1L;
        IssueDeliveryOrderRequestDto invalidRequest = IssueDeliveryOrderRequestDto.builder()
                .doReleasedIdPerson("REL001")
                .doReleasedToPerson("John Doe")
                .doReleasedAddressPerson("123 Test Street")
                .doPriority("HIGH")
                .deliverySentTo("C")
                .originalBlReleaseCr("OFFICE")
                .build();

        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getGroupPoid).thenReturn(1L);
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            userContextMock.when(UserContext::getUserName).thenReturn("testuser");
            
            when(viewRepository.findByTransactionPoid(transactionPoid)).thenReturn(Optional.of(mockDto));

            ValidationException exception = assertThrows(ValidationException.class, 
                    () -> service.issueDeliveryOrder(transactionPoid, invalidRequest));
            
            assertEquals("Delivery emails not added for customer", exception.getMessage());
        }
    }

    @Test
    void issueDeliveryOrder_EntityNotFound() {
        Long transactionPoid = 1L;
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            
            when(viewRepository.findByTransactionPoid(transactionPoid)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> service.issueDeliveryOrder(transactionPoid, issueRequest));
        }
    }

    @Test
    void issueDeliveryOrder_EntityDeleted() {
        Long transactionPoid = 1L;
        mockDto = DeliveryOrderIssueToCustomerDto.builder()
                .transactionPoid(1L)
                .companyPoid(100L)
                .deleted("Y")
                .build();
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            
            when(viewRepository.findByTransactionPoid(transactionPoid)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> service.issueDeliveryOrder(transactionPoid, issueRequest));
        }
    }

    @Test
    void updateDeliveryOrder_Success() {
        Long transactionPoid = 1L;
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserName).thenReturn("testuser");
            userContextMock.when(UserContext::getGroupPoid).thenReturn(1L);
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            userContextMock.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(blManifestRepository.findById(transactionPoid)).thenReturn(Optional.of(mockEntity));
            when(doShPrintingDtlRepository.findByTransactionPoid(transactionPoid)).thenReturn(Optional.of(mockPrintingDtl));
            doNothing().when(entityManager).refresh(any());
            when(viewRepository.getGlobalParameterValue(any(), any(), any(), any())).thenReturn("N");

            ValidateDocumentDto result = service.updateDeliveryOrder(transactionPoid, updateRequest);

            assertNotNull(result);
        }
    }

    @Test
    void updateDeliveryOrder_PrintingDtlNotFound() {
        Long transactionPoid = 1L;
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserName).thenReturn("testuser");
            userContextMock.when(UserContext::getGroupPoid).thenReturn(1L);
            userContextMock.when(UserContext::getCompanyPoid).thenReturn(100L);
            userContextMock.when(UserContext::getDocumentId).thenReturn("DOC001");

            when(blManifestRepository.findById(transactionPoid)).thenReturn(Optional.of(mockEntity));
            when(doShPrintingDtlRepository.findByTransactionPoid(transactionPoid)).thenReturn(Optional.empty());
            doNothing().when(entityManager).refresh(any());
            when(viewRepository.getGlobalParameterValue(any(), any(), any(), any())).thenReturn("N");

            // proc-based path treats missing DoShPrintingDtl as null (orElse(null)), not an exception
            ValidateDocumentDto result = service.updateDeliveryOrder(transactionPoid, updateRequest);
            assertNotNull(result);
        }
    }

    @Test
    void updateDeliveryOrder_EntityNotFound() {
        Long transactionPoid = 1L;
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserName).thenReturn("testuser");
            
            when(blManifestRepository.findById(transactionPoid)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, 
                    () -> service.updateDeliveryOrder(transactionPoid, updateRequest));
        }
    }
}
