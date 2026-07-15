package com.asg.shipping.importManifestUpdateTest.repository;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.importmanifestupdate.dto.*;
import com.asg.shipping.importmanifestupdate.respository.ImportManifestBlProcRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportManifestBlProcRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private StoredProcedureQuery storedProcedureQuery;

    @InjectMocks
    private ImportManifestBlProcRepositoryImpl repository;

    @Test
    void updateEmailVerification_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(200L);
            mockedUserContext.when(UserContext::getDocumentId).thenReturn("DOC123");

            when(entityManager.createStoredProcedureQuery("PROC_SH_BL_UPDATE_MAIL_VERIF_V2"))
                    .thenReturn(storedProcedureQuery);
            when(storedProcedureQuery.getOutputParameterValue("P_STATUS")).thenReturn("SUCCESS");

            EmailVerificationRequestDto request = EmailVerificationRequestDto.builder()
                    .transactionPoId(1L)
                    .verified(true)
                    .verifiedWithSpecialC(false)
                    .build();

            EmailVerificationResponseDto response = repository.updateEmailVerification(1L, request);

            assertNotNull(response);
            assertEquals("SUCCESS", response.getStatus());
            verify(storedProcedureQuery).execute();
        }
    }

    @Test
    void resendCan_Success() {
        when(entityManager.createStoredProcedureQuery("PROD_RESEND_CAN"))
                .thenReturn(storedProcedureQuery);

        ResendCanResponseDto response = repository.resendCan(100L, 1L, null);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        verify(storedProcedureQuery).execute();
    }

    @Test
    void getEdiEmails_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(200L);

            when(entityManager.createStoredProcedureQuery("PROC_SHIP_BL_EDI_EMAILS"))
                    .thenReturn(storedProcedureQuery);
            when(storedProcedureQuery.getOutputParameterValue("P_EMAI_IDS"))
                    .thenReturn("test@test.com,test2@test.com");

            SendEdiEmailsResponseDto response = repository.getEdiEmails(1L);

            assertNotNull(response);
            assertEquals(2, response.getEmailsSent());
            verify(storedProcedureQuery).execute();
        }
    }

    @Test
    void getBlStatus_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
            mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(200L);

            when(entityManager.createStoredProcedureQuery("PROC_SHIP_DO_BL_STATUS"))
                    .thenReturn(storedProcedureQuery);
            when(storedProcedureQuery.getOutputParameterValue("P_RESULT"))
                    .thenReturn("D/O Issued");

            BlStatusResponseDto response = repository.getBlStatus(1L);

            assertNotNull(response);
            assertTrue(response.getHasDo());
            verify(storedProcedureQuery).execute();
        }
    }

    @Test
    void processBlSaveAfter_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);

            when(entityManager.createStoredProcedureQuery("PROC_SHIP_BL_PAGE_SAVE_AFTER"))
                    .thenReturn(storedProcedureQuery);

            repository.processBlSaveAfter(1L, 100L, 200L, "AUTOSUMWEIGHTPACKATE");

            verify(storedProcedureQuery).execute();
        }
    }

    private void stubValidateBeforeSaveProc(MockedStatic<UserContext> mockedUserContext, String procResult) {
        mockedUserContext.when(UserContext::getGroupPoid).thenReturn(100L);
        mockedUserContext.when(UserContext::getUserPoid).thenReturn(1L);
        mockedUserContext.when(UserContext::getCompanyPoid).thenReturn(200L);

        when(entityManager.createStoredProcedureQuery("PROC_SHIP_VALD_BEFORE_SAVE"))
                .thenReturn(storedProcedureQuery);
        when(storedProcedureQuery.getOutputParameterValue("P_RESULT")).thenReturn(procResult);
    }

    /**
     * Legacy GetBlValidate() returns true when the proc answers "TRUE", and the bean
     * then raises "Map Quotation in manifest" for an unmapped collect BL that is not
     * booked by principal.
     */
    @Test
    void validateBeforeSave_ProcTrueAndQuotationUnmapped_Fail() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            stubValidateBeforeSaveProc(mockedUserContext, "TRUE");

            ValidationException ex = assertThrows(ValidationException.class,
                    () -> repository.validateBeforeSave(100L, 1L, null, "2", "N"));
            assertTrue(ex.getMessage().contains("quotation must be mapped"));
        }
    }

    @Test
    void validateBeforeSave_ProcTrueButQuotationMapped_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            stubValidateBeforeSaveProc(mockedUserContext, "TRUE");

            repository.validateBeforeSave(100L, 1L, 55L, "2", "N");

            verify(storedProcedureQuery).execute();
        }
    }

    @Test
    void validateBeforeSave_ProcTrueButBookedByPrincipal_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            stubValidateBeforeSaveProc(mockedUserContext, "TRUE");

            repository.validateBeforeSave(100L, 1L, null, "2", "Y");

            verify(storedProcedureQuery).execute();
        }
    }

    @Test
    void validateBeforeSave_ProcTrueButFreightNotCollect_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            stubValidateBeforeSaveProc(mockedUserContext, "TRUE");

            repository.validateBeforeSave(100L, 1L, null, "1", "N");

            verify(storedProcedureQuery).execute();
        }
    }

    /** Proc says no quotation is expected: the BL saves even with nothing mapped. */
    @Test
    void validateBeforeSave_ProcFalse_Success() {
        try (MockedStatic<UserContext> mockedUserContext = mockStatic(UserContext.class)) {
            stubValidateBeforeSaveProc(mockedUserContext, "FALSE");

            repository.validateBeforeSave(100L, 1L, null, "2", "N");

            verify(storedProcedureQuery).execute();
        }
    }
}
