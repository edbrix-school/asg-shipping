package com.asg.shipping.importmanifestupdate.respository;

import com.asg.common.lib.security.util.UserContext;
import com.asg.common.lib.exception.ValidationException;
import com.asg.shipping.exceptions.ResourceNotFoundException;
import com.asg.shipping.importmanifestupdate.dto.EmailVerificationRequestDto;
import com.asg.shipping.importmanifestupdate.dto.EmailVerificationResponseDto;
import com.asg.shipping.importmanifestupdate.dto.ResendCanResponseDto;
import com.asg.shipping.importmanifestupdate.dto.SendEdiEmailsResponseDto;
import com.asg.shipping.importmanifestupdate.dto.BlStatusResponseDto;
import com.asg.shipping.importmanifestupdate.dto.ImportManifestBlCreateDto;
import com.asg.shipping.importmanifestbl.dto.DefaultValueDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

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
        log.info("Email verification update status for BL {} : {}", request.getTransactionPoId(), status);

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
    public SendEdiEmailsResponseDto getEdiEmails(Long transactionPoId) {
        try {

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

            if (emailIds != null && emailIds.startsWith("ERROR")) {
                throw new RuntimeException("EDI email processing failed: " + emailIds);
            }

            if (emailIds == null || "@".equals(emailIds) || emailIds.trim().isEmpty()) {
                emailIds = "";
            }

            int emailsSent = emailIds.isEmpty() ? 0 : emailIds.split(",").length;

            return SendEdiEmailsResponseDto.builder()
                    .emailIds(emailIds)
                    .emailsSent(emailsSent)
                    .build();

        } catch (ResourceNotFoundException e) {
            log.error("Failed to send EDI emails: Entity not found for transactionPoId: {}", transactionPoId);
            throw e;
        } catch (Exception e) {
            log.error("Error sending EDI emails for transactionPoId: {}", transactionPoId, e);
            throw new RuntimeException("Failed to send EDI emails: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveEmailsToDb(Long transactionPoId, String addressType,
                               String email1, String email2, String scope) {
        try {
            StoredProcedureQuery query = entityManager
                    .createStoredProcedureQuery("SP_Save_Emails_Data_DB");

            query.registerStoredProcedureParameter("P_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_KEY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_ADDRESS_TYPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_EMAIL1", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_EMAIL2", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_SCOPE", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_STATUS", String.class, ParameterMode.OUT);

            query.setParameter("P_GROUP_POID", UserContext.getGroupPoid());
            query.setParameter("P_COMPANY_POID", UserContext.getCompanyPoid());
            query.setParameter("P_USER_POID", UserContext.getUserPoid());
            query.setParameter("P_DOC_ID", UserContext.getDocumentId()); // Document ID for manifest
            query.setParameter("P_DOC_KEY_POID", transactionPoId);
            query.setParameter("P_ADDRESS_TYPE", addressType);
            query.setParameter("P_EMAIL1", email1);
            query.setParameter("P_EMAIL2", email2);
            query.setParameter("P_SCOPE", scope);

            query.execute();

            String status = (String) query.getOutputParameterValue("P_STATUS");

            if (status != null && status.contains("ERROR")) {
                throw new RuntimeException("Failed to save emails: " + status);
            }

        } catch (Exception e) {
            log.error("Error saving emails for transactionPoId: {}", transactionPoId, e);
            throw new RuntimeException("Failed to save emails: " + e.getMessage(), e);
        }
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
    public void validateBeforeSave(Long voyageTransactionPoid, Long transactionPoid,Long quotationPoid,String freight,String bookedByPrincipal) {
        try {
            Long userPoid = UserContext.getUserPoid();
            Long groupPoid = UserContext.getGroupPoid();
            Long companyPoid = UserContext.getCompanyPoid();

            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("PROC_SHIP_VALD_BEFORE_SAVE");

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
            query.setParameter("P_OTHER_POID", String.valueOf(voyageTransactionPoid));
            query.setParameter("P_STRING", "VLD_QUOTATION");

            query.execute();

            String result = (String) query.getOutputParameterValue("P_RESULT");

            if (!"TRUE".equalsIgnoreCase(result)) {
                if (quotationPoid == null
                        && "2".equals(freight)
                        && "N".equals(bookedByPrincipal)) {
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

    @Override
    public DefaultValueDto callDefaultGetValue(Long loginGroupPoid, Long loginCompanyPoid, Long loginUserPoid, String docId) {

            StoredProcedureQuery query = entityManager
                    .createStoredProcedureQuery("PRODUCTION.PROC_DEFAULT_GETVALUE");

            query.registerStoredProcedureParameter("P_LOGIN_GROUP_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_COMPANY_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_LOGIN_USER_POID", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("P_DOC_ID", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

            query.setParameter("P_LOGIN_GROUP_POID", loginGroupPoid);
            query.setParameter("P_LOGIN_COMPANY_POID", loginCompanyPoid);
            query.setParameter("P_LOGIN_USER_POID", loginUserPoid);
            query.setParameter("P_DOC_ID", docId);

            query.execute();

            ResultSet rs = (ResultSet) query.getOutputParameterValue("OUTDATA");
            log.info("Default values ResultSet: {}", rs);

            DefaultValueDto dto = null;

            try {
                if (rs != null && rs.next()) {

                    if (hasColumn(rs, "NO_RECORD")) {
                        return null;
                    }

                    dto = DefaultValueDto.builder()
                            .blType(rs.getString("BL_TYPE"))
                            .salesmanPoid(rs.getString("SALESMAN_POID"))
                            .cargoType(rs.getString("CARGO_TYPE"))
                            .blIssueType(rs.getString("BL_ISSUE_TYPE"))
                            .freightStatus(rs.getString("FREIGHT_STATUS"))
                            .holdReason(rs.getString("HOLD_REASON"))
                            .holdCanDo(rs.getString("HOLD_CAN_DO"))
                            .canSentQueue(rs.getString("CAN_SENT_QUEUE"))
                            .bookedByPp(rs.getString("BOOKED_BY_PP"))
                            .manuallyCanSend(rs.getString("MANUALLY_CAN_SEND"))
                            .allInOneFreight(rs.getString("ALL_IN_ONE_FREIGHT"))
                            .issueManualInvoice(rs.getString("ISSUE_MANUAL_INVOICE"))
                            .deliverySentTo(rs.getString("DELIVERY_SENT_TO"))
                            .manifestEmailVerified(rs.getString("MANIFEST_EMAIL_VERIFIED"))
                            .emailVerifiedWithSpecialC(rs.getString("EMAIL_VERIFIED_WITH_SPECIAL_C"))
                            .stopUcanAlert(rs.getString("STOP_UCAN_ALERT"))
                            .transactionDate(
                                    rs.getDate("TRANSACTION_DATE") != null
                                            ? String.valueOf(rs.getDate("TRANSACTION_DATE").toLocalDate())
                                            : null
                            )
                            .printFreightDetails(rs.getString("PRINT_FREIGHT_DETAILS"))
                            .portOfDischargePoid(rs.getString("PORT_OF_DISCHARGE_POID"))
                            .placeOfDeliveryPoid(rs.getString("PLACE_OF_DELIEVERY_POID"))
                            .blStatus(rs.getString("BL_STATUS"))
                            .blOriginalPrint(rs.getString("BL_ORGINAL_PRINT"))
                            .releasedStatus(rs.getString("RELEASED_STATUS"))
                            .build();
                }

            } catch (Exception e) {
                throw new RuntimeException(
                        "Error fetching default values from PROC_DEFAULT_GETVALUE: " + e.getMessage(),
                        e
                );
            }

            return dto;
        }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch ( SQLException e) {
            return false;
        }
    }



}
