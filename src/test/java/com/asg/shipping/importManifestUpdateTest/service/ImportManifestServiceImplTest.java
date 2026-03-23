package com.asg.shipping.importManifestUpdateTest.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.exception.ResourceNotFoundException;
import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.service.DocumentDeleteService;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.common.lib.service.LoggingService;
import com.asg.shipping.address.entity.AddressDetailsRepository;
import com.asg.shipping.importmanifestupdate.dto.ImportManifestBlRequestDto;
import com.asg.shipping.importmanifestupdate.dto.ImportManifestBlUpdateDTO;
import com.asg.shipping.importmanifestupdate.entity.ShipBlManifestHdr;
import com.asg.shipping.importmanifestupdate.respository.*;
import com.asg.shipping.importmanifestupdate.service.ImportManifestBlServiceImpl;
import com.asg.shipping.importmanifestupdate.util.ImportManifestBlMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ImportManifestServiceImplTest {

    @Mock
    private ShipBlManifestHdrRepository repository;
    @Mock
    private ShipBlManifestGeneralDtlRepository generalDtlRepository;
    @Mock
    private ShipBlManifestCargoDtlRepository cargoDtlRepository;
    @Mock
    private ShipBlManifestContainerDtlRepository containerDtlRepository;
    @Mock
    private ShipBlManifestChargesDtlRepository chargesDtlRepository;
    @Mock
    private ShipBlManifestPartBLRepository containerPrtRepository;
    @Mock
    private ShipBlManifestEmailFaxDtlRepository emailFaxDtlRepository;
    @Mock
    private ShipBlManifestMafiDtlRepository mafiDtlRepository;
    @Mock
    private DocumentSearchService documentService;
    @Mock
    private ImportManifestBlMapper mapper;
    @Mock
    private EntityManager entityManager;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private StoredProcedureQuery storedProcedureQuery;

    @Mock
    private BlManifestValidationRepository validationRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ImportManifestBlProcRepository procRepository;
    @Mock
    private AddressDetailsRepository addressDetailsRepository;
    @Mock
    private LoggingService loggingService;
    @Mock
    private DocumentDeleteService documentDeleteService;

    @InjectMocks
    private ImportManifestBlServiceImpl service;

    private ShipBlManifestHdr mockEntity;
    private ImportManifestBlUpdateDTO mockUpdateDto;
    private ImportManifestBlRequestDto mockRequestDto;

    @BeforeEach
    void setUp() {
        mockEntity = new ShipBlManifestHdr();
        mockEntity.setTransactionPoid(1L);
        mockEntity.setBlNumber("TEST123");
        mockEntity.setFreightStatus("PAID");
        mockEntity.setDoNo("DO001");
        mockEntity.setDeleted("N");
        mockEntity.setTransactionDate(LocalDateTime.now());

        mockUpdateDto = ImportManifestBlUpdateDTO.builder()
                .blNumber("TEST123")
                .agentReference("AGENT001")
                .voyageTransactionPoid(100L)
                .cargoType("FCL")
                .blType("IMPORT")
                .build();

        mockRequestDto = ImportManifestBlRequestDto.builder()
                .transactionPoid(1L)
                .blNumber("TEST123")
                .build();
    }

    @Test
    void updateImportManifestBl_Success() {
        // Given
        Long id = 1L;
        Long companyPoid = 100L;
        Long groupPoid = 200L;

        lenient().when(repository.findByTransactionPoid(id)).thenReturn(Optional.of(mockEntity));
        lenient().when(entityManager.createStoredProcedureQuery("PROC_SHIP_VALD_BEFORE_SAVE")).thenReturn(storedProcedureQuery);
        lenient().when(storedProcedureQuery.getOutputParameterValue("P_RESULT")).thenReturn("TRUE");
        lenient().when(repository.save(any(ShipBlManifestHdr.class))).thenReturn(mockEntity);

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            // When & Then - Expect TransactionSynchronization exception
            assertThrows(ValidationException.class, () -> {
                service.updateImportManifestBl(id, mockUpdateDto, companyPoid, groupPoid);
            });

            verify(repository).findByTransactionPoid(id);
        }
    }

    @Test
    void updateImportManifestBl_NotFound() {
        // Given
        Long id = 999L;
        when(repository.findByTransactionPoid(id)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            service.updateImportManifestBl(id, mockUpdateDto, 100L, 200L);
        });
        verify(repository).findByTransactionPoid(id);
    }


    @Test
    void updateImportManifestBl_ValidationFailed() {
        // Given
        Long id = 1L;
        when(repository.findByTransactionPoid(id)).thenReturn(Optional.of(mockEntity));
        lenient().when(entityManager.createStoredProcedureQuery("PROC_SHIP_VALD_BEFORE_SAVE")).thenReturn(storedProcedureQuery);
        lenient().when(storedProcedureQuery.getOutputParameterValue("P_RESULT")).thenReturn("Validation Error");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            // When & Then
            assertThrows(ValidationException.class, () -> {
                service.updateImportManifestBl(id, mockUpdateDto, 100L, 200L);
            });
        }
    }

    @Test
    void getImportManifestBl_Success() {
        // Given
        Long id = 1L;
        when(repository.findByTransactionPoid(id)).thenReturn(Optional.of(mockEntity));
        when(mapper.mapToDto(mockEntity)).thenReturn(mockRequestDto);
        when(generalDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(id)).thenReturn(Collections.emptyList());
        when(cargoDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(id)).thenReturn(Collections.emptyList());
        when(containerDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(id)).thenReturn(Collections.emptyList());
        when(chargesDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(id)).thenReturn(Collections.emptyList());
        when(containerPrtRepository.findByIdTransactionPoidOrderByIdDetRowId(id)).thenReturn(Collections.emptyList());
        when(emailFaxDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(id)).thenReturn(Collections.emptyList());
        when(mafiDtlRepository.findByIdTransactionPoidOrderByIdDetRowId(id)).thenReturn(Collections.emptyList());

        // When
        ImportManifestBlRequestDto result = service.getImportManifestBl(id);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getTransactionPoid());
        verify(repository).findByTransactionPoid(id);
        verify(mapper).mapToDto(mockEntity);
    }

    @Test
    void getImportManifestBl_NotFound() {
        // Given
        Long id = 999L;
        when(repository.findByTransactionPoid(id)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            service.getImportManifestBl(id);
        });
        verify(repository).findByTransactionPoid(id);
    }

    @Test
    void deleteImportManifestBl_AlreadyDeleted() {
        // Given
        Long id = 1L;
        mockEntity.setDeleted("Y");
        when(repository.findByTransactionPoid(id)).thenReturn(Optional.of(mockEntity));

        // When
        service.deleteImportManifestBl(id,new DeleteReasonDto());

        // Then
        verify(repository).findByTransactionPoid(id);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteImportManifestBl_NotFound() {
        Long id = 999L;
        when(repository.findByTransactionPoid(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            service.deleteImportManifestBl(id, new DeleteReasonDto());
        });
    }

    @Test
    void listOfImportManifest_Success() {
        when(documentService.resolveOperator(any())).thenReturn("OR");
        when(documentService.resolveIsDeleted(any())).thenReturn("N");
        when(documentService.resolveFilters(any())).thenReturn(List.of());
        when(documentService.search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(new com.asg.common.lib.dto.RawSearchResult(List.of(), Map.of(), 0L));

        var filterRequest = new com.asg.common.lib.dto.FilterRequestDto("OR", "N", List.of());
        var result = service.listOfImportManifest("DOC123", filterRequest, org.springframework.data.domain.PageRequest.of(0, 10));

        assertNotNull(result);
        verify(documentService).search(anyString(), anyList(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void updateEmailVerification_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockEntity));
        when(procRepository.updateEmailVerification(eq(1L), any()))
                .thenReturn(com.asg.shipping.importmanifestupdate.dto.EmailVerificationResponseDto.builder().status("SUCCESS").build());

        var request = com.asg.shipping.importmanifestupdate.dto.EmailVerificationRequestDto.builder()
                .transactionPoId(1L)
                .verified(true)
                .build();

        var response = service.updateEmailVerification(1L, request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void resendCan_Success() {
        mockEntity.setVoyageTransactionPoid(100L);
        when(repository.findById(1L)).thenReturn(Optional.of(mockEntity));
        when(procRepository.resendCan(100L, 1L))
                .thenReturn(com.asg.shipping.importmanifestupdate.dto.ResendCanResponseDto.builder().status("SUCCESS").build());

        var response = service.resendCan(1L);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void sendEdiEmails_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockEntity));
        when(procRepository.getEdiEmails(1L))
                .thenReturn(com.asg.shipping.importmanifestupdate.dto.SendEdiEmailsResponseDto.builder().emailsSent(2).build());

        var response = service.sendEdiEmails(1L);

        assertNotNull(response);
        assertEquals(2, response.getEmailsSent());
    }

    @Test
    void loadEmailFax_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockEntity));
        when(addressDetailsRepository.findByAddressMasterPoidAndAddressType(anyLong(), anyString()))
                .thenReturn(List.of());

        var request = com.asg.shipping.importmanifestupdate.dto.LoadEmailFaxRequestDto.builder()
                .addressMasterPoid(100L)
                .addressType("CAN")
                .build();

        var response = service.loadEmailFax(1L, request);

        assertNotNull(response);
        verify(addressDetailsRepository).findByAddressMasterPoidAndAddressType(anyLong(), anyString());
    }

    @Test
    void getBlStatus_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockEntity));
        when(procRepository.getBlStatus(1L))
                .thenReturn(com.asg.shipping.importmanifestupdate.dto.BlStatusResponseDto.builder().status("NEW").build());

        var response = service.getBlStatus(1L);

        assertNotNull(response);
        assertEquals("NEW", response.getStatus());
    }

    @Test
    void updateImportManifestBl_WithDetailTables() {
        Long id = 1L;

        mockUpdateDto.setGeneralCargoDetails(List.of(
                com.asg.shipping.importmanifestupdate.dto.GeneralCargoRequestDto.builder()
                        .detRowId(1L)
                        .actionType("ISCREATED")
                        .cargoDescription("Test")
                        .build()
        ));

        when(repository.findByTransactionPoid(id)).thenReturn(Optional.of(mockEntity));
        when(entityManager.createStoredProcedureQuery("PROC_SHIP_VALD_BEFORE_SAVE"))
                .thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.getOutputParameterValue("P_RESULT"))
                .thenReturn("Validation Error");

        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            assertThrows(ValidationException.class, () ->
                    service.updateImportManifestBl(id, mockUpdateDto, 100L, 200L)
            );
        }
    }
}
