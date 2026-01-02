package com.asg.shipping.importManifestUpdate.respository;

import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.importManifestUpdate.dto.EmailVerificationRequestDto;
import com.asg.shipping.importManifestUpdate.dto.EmailVerificationResponseDto;
import com.asg.shipping.importManifestUpdate.dto.ResendCanResponseDto;
import com.asg.shipping.importManifestUpdate.dto.SendEdiEmailsResponseDto;
import com.asg.shipping.importManifestUpdate.dto.BlStatusResponseDto;
import com.asg.shipping.importManifestUpdate.dto.ImportManifestBlCreateDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ImportManifestBlProcRepositoryImpl implements ImportManifestBlProcRepository {

    private final EntityManager entityManager;

    @Override
    public EmailVerificationResponseDto updateEmailVerification(Long transactionPoId, EmailVerificationRequestDto request) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SH_BL_UPDATE_MAIL_VERIF_V2");

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_BL_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_MANIFEST_EMAIL_VERIFIED", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_EMAIL_VERIFIED_WITH_SPECIAL_C", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);

        query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        query.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        query.setParameter("P_DOC_ID", UserContext.getDocumentId());
        query.setParameter("P_BL_POID", transactionPoId);
        query.setParameter("P_MANIFEST_EMAIL_VERIFIED", request.isVerified() ? "Y" : "N");
        query.setParameter("P_EMAIL_VERIFIED_WITH_SPECIAL_C", request.isVerifiedWithSpecialC() ? "Y" : "N");

        query.execute();

        String status = (String) query.getOutputParameterValue("P_STATUS");
        log.info("Email verification update status for BL {} : {}", transactionPoId, status);

        return EmailVerificationResponseDto.builder().status(status).build();
    }

    @Override
    public ResendCanResponseDto resendCan(Long voyageTransactionPoId, Long transactionPoId) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROD_RESEND_CAN");

        query.registerStoredProcedureParameter("P_VOYAGE_TRANSACTION_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_TRANSACTION_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_UPDATE_DEMURRAGE", String.class, ParameterMode.IN);

        query.setParameter("P_VOYAGE_TRANSACTION_POID", voyageTransactionPoId);
        query.setParameter("P_TRANSACTION_POID", transactionPoId);
        query.setParameter("P_UPDATE_DEMURRAGE", "C");

        query.execute();
        log.info("CAN resent successfully for transactionPoId: {}", transactionPoId);

        return ResendCanResponseDto.builder().status("SUCCESS").build();
    }

    @Override
    public SendEdiEmailsResponseDto sendEdiEmails(Long transactionPoId) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_BL_EDI_EMAILS");

        query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_EMAI_IDS", String.class, ParameterMode.OUT);

        query.setParameter("P_GROUP_POID", UserContext.getGroupPoid());
        query.setParameter("P_COMPANY_POID", UserContext.getCompanyPoid());
        query.setParameter("P_DOC_KEY_POID", transactionPoId);

        query.execute();

        String emailIds = (String) query.getOutputParameterValue("P_EMAI_IDS");
        int emailsSent = emailIds != null && !emailIds.startsWith("ERRPR") ? emailIds.split(";").length : 0;
        log.info("EDI emails sent for transactionPoId: {}, count: {}", transactionPoId, emailsSent);

        return SendEdiEmailsResponseDto.builder().status("SUCCESS").emailsSent(emailsSent).build();
    }

    @Override
    public BlStatusResponseDto getBlStatus(Long transactionPoId) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_DO_BL_STATUS");

        query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

        query.setParameter("P_LOGIN_GROUP_POID", UserContext.getGroupPoid());
        query.setParameter("P_LOGIN_COMPANY_POID", UserContext.getCompanyPoid());
        query.setParameter("P_LOGIN_USER_POID", UserContext.getUserPoid());
        query.setParameter("P_DOC_KEY_POID", transactionPoId);

        query.execute();

        String result = (String) query.getOutputParameterValue("P_RESULT");
        boolean hasDo = result != null && !result.equals("FALSE") && result.contains("D/O");
        log.info("BL status retrieved for transactionPoId: {}, hasDo: {}", transactionPoId, hasDo);

        return BlStatusResponseDto.builder().status(result != null ? result : "NEW").hasDo(hasDo).build();
    }

    @Override
    public void processBlSaveAfter(Long transactionPoid, Long groupPoid, Long companyPoid, String processType) {
        try {
            Long userPoid = UserContext.getUserPoid();
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_BL_PAGE_SAVE_AFTER");

            query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DET_ROW_ID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_UPDATE_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER", Long.class, ParameterMode.IN);

            query.setParameter("P_GROUP_POID", groupPoid);
            query.setParameter("P_COMPANY_POID", companyPoid);
            query.setParameter("P_DOC_KEY_POID", transactionPoid);
            query.setParameter("P_DET_ROW_ID", 0L);
            query.setParameter("P_UPDATE_TYPE", processType);
            query.setParameter("P_LOGIN_USER", userPoid);

            query.execute();
            log.debug("PROC_SHIP_BL_PAGE_SAVE_AFTER executed for {}", transactionPoid);
        } catch (Exception e) {
            log.warn("PROC_SHIP_BL_PAGE_SAVE_AFTER error for transaction {}. Reason: {}", transactionPoid, e.getMessage());
        }
    }

    @Override
    public void validateBeforeSave(ImportManifestBlCreateDto dto, Long transactionPoid) {
        try {
            Long userPoid = UserContext.getUserPoid();
            Long groupPoid = UserContext.getGroupPoid();
            Long companyPoid = UserContext.getCompanyPoid();

            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PRODUCTION.PROC_SHIP_VALD_BEFORE_SAVE");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_KEY_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_OTHER_POID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_STRING", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_RESULT", String.class, ParameterMode.OUT);

            query.setParameter("P_LOGIN_GROUP_POID", groupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", companyPoid);
            query.setParameter("P_LOGIN_USER_POID", userPoid);
            query.setParameter("P_DOC_KEY_POID", String.valueOf(transactionPoid));
            query.setParameter("P_OTHER_POID", String.valueOf(dto.getVoyageTransactionPoid()));
            query.setParameter("P_STRING", "VLD_QUOTATION");

            query.execute();

            String result = (String) query.getOutputParameterValue("P_RESULT");

            if (!"TRUE".equalsIgnoreCase(result)) {
                if (dto.getQuotationTransactionPoid() == null
                        && "2".equals(dto.getFreightStatus())
                        && "N".equals(dto.getBookedByPp())) {
                    log.error("Validation failed: Quotation mapping required for transaction {}", transactionPoid);
                    throw new ValidationException("Map Quotation in manifest");
                }
            }

            log.debug("PROC_SHIP_VALD_BEFORE_SAVE executed successfully for transaction {}", transactionPoid);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing PROC_SHIP_VALD_BEFORE_SAVE for transaction {}", transactionPoid, e);
            throw new ValidationException("Quotation validation failed");
        }
    }

}
