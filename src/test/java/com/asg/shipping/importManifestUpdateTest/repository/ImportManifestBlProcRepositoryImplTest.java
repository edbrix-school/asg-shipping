package com.asg.shipping.importManifestUpdateTest.repository;

import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.importManifestUpdate.dto.*;
import com.asg.shipping.importManifestUpdate.respository.ImportManifestBlProcRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

        ResendCanResponseDto response = repository.resendCan(100L, 1L);

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
}
