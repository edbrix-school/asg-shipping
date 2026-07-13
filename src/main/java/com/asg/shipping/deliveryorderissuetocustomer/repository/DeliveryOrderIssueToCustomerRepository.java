package com.asg.shipping.deliveryorderissuetocustomer.repository;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.shipping.deliveryorderissuetocustomer.dto.DeliveryOrderIssueToCustomerDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DeliveryOrderIssueToCustomerRepository {

    private final JdbcTemplate jdbcTemplate;
    @PersistenceContext
    private final EntityManager entityManager;

    /**
     * Find delivery order by transaction POID
     */
    public Optional<String> findRemarksByTransactionPoid(Long transactionPoid) {
        String sql = "SELECT REMARKS FROM SHIP_BL_MANIFEST_HDR WHERE TRANSACTION_POID = ?";
        try {
            String remarks = jdbcTemplate.queryForObject(sql, String.class, transactionPoid);
            return Optional.ofNullable(remarks);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<DeliveryOrderIssueToCustomerDto> findByTransactionPoid(Long transactionPoid) {
        String sql = "SELECT COMPANY_POID, TRANSACTION_POID, TRANSACTION_DATE, DOC_REF, JOBNO, " +
                "ARRIVAL_DATE, BL_NUMBER, LINE, CONSIGNEE, NOTIFY, C_20, C_40, HOLD_DO, " +
                "DO_RELASED_ID_PERSON, DO_RELASED_TO_PERSON, DO_RELASED_ADDRS_PERSON, " +
                "DELETED, SEQNO, BL_RELEASE_TYPE_OFFICE, ORIGNAL_BL_RELEASE_CR, DO_PRIORITY, " +
                "DO_ISSUE_AUTH, DO_ISSUE_AUTH_POID, DO_CNT_TO_CONSIGNEE, DO_CNT_TO_NOTIFY, " +
                "DO_CNT_TO_OTHERS, DO_CNT_TO_OTHERS_MAILS, DO_EMAILS, DELIVERY_SENT_TO, " +
                "PRINCIPAL_DO_NUMBER, PRINCIPAL_DO_REQUIRED " +
                "FROM VW_CREDIT_DELIVERY_ORDER_PEND " +
                "WHERE TRANSACTION_POID = ?";

        try {
            List<DeliveryOrderIssueToCustomerDto> results = jdbcTemplate.query(sql, new DeliveryOrderRowMapper(), transactionPoid);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
        } catch (Exception e) {
            log.error("Error fetching delivery order for transactionPoid={}: {}", transactionPoid, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Row mapper for DeliveryOrderIssueToCustomerDto
     */
    private static class DeliveryOrderRowMapper implements RowMapper<DeliveryOrderIssueToCustomerDto> {
        @Override
        public DeliveryOrderIssueToCustomerDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return DeliveryOrderIssueToCustomerDto.builder()
                    .companyPoid(rs.getLong("COMPANY_POID"))
                    .transactionPoid(rs.getLong("TRANSACTION_POID"))
                    .transactionDate(rs.getDate("TRANSACTION_DATE") != null ? rs.getDate("TRANSACTION_DATE").toLocalDate() : null)
                    .docRef(rs.getString("DOC_REF"))
                    .jobNo(rs.getString("JOBNO"))
                    .arrivalDate(rs.getDate("ARRIVAL_DATE") != null ? rs.getDate("ARRIVAL_DATE").toLocalDate() : null)
                    .blNumber(rs.getString("BL_NUMBER"))
                    .line(rs.getString("LINE"))
                    .consignee(rs.getString("CONSIGNEE"))
                    .notify(rs.getString("NOTIFY"))
                    .c20(rs.getInt("C_20"))
                    .c40(rs.getInt("C_40"))
                    .holdDo(rs.getString("HOLD_DO"))
                    .doReleasedIdPerson(rs.getString("DO_RELASED_ID_PERSON"))
                    .doReleasedToPerson(rs.getString("DO_RELASED_TO_PERSON"))
                    .doReleasedAddrsPerson(rs.getString("DO_RELASED_ADDRS_PERSON"))
                    .deleted(rs.getString("DELETED"))
                    .seqno(rs.getObject("SEQNO") != null ? rs.getLong("SEQNO") : null)
                    .blReleaseTypeOffice(rs.getString("BL_RELEASE_TYPE_OFFICE"))
                    .originalBlReleaseCr(rs.getString("ORIGNAL_BL_RELEASE_CR"))
                    .doPriority(rs.getString("DO_PRIORITY"))
                    .doIssueAuth(rs.getString("DO_ISSUE_AUTH"))
                    .doIssueAuthPoid(rs.getObject("DO_ISSUE_AUTH_POID") != null ? rs.getLong("DO_ISSUE_AUTH_POID") : null)
                    .doCntToConsignee(rs.getString("DO_CNT_TO_CONSIGNEE"))
                    .doCntToNotify(rs.getString("DO_CNT_TO_NOTIFY"))
                    .doCntToOthers(rs.getString("DO_CNT_TO_OTHERS"))
                    .doCntToOthersMails(rs.getString("DO_CNT_TO_OTHERS_MAILS"))
                    .doEmails(rs.getString("DO_EMAILS"))
                    .deliverySentTo(rs.getString("DELIVERY_SENT_TO"))
                    .principalDoNumber(rs.getString("PRINCIPAL_DO_NUMBER"))
                    .principalDoRequired(rs.getString("PRINCIPAL_DO_REQUIRED"))
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> fetchShipLineDetails(Long pBLPoid) {

        String sql = "SELECT DISTINCT " +
                " CONTAINER_FORM_VHENT, " +
                " CONTAINER_FORM_RTN, " +
                " DO_PRINT_LINE, " +
                " LINE_CODE, " +
                " RCPT_PRINT_LINE " +
                "FROM SHIP_LINE_MASTER " +
                "WHERE LINE_POID IN ( " +
                "   SELECT LINE_POID " +
                "   FROM SHIP_VOYAGE_HDR " +
                "   WHERE TRANSACTION_POID IN ( " +
                "       SELECT VOYAGE_TRANSACTION_POID " +
                "       FROM SHIP_BL_MANIFEST_HDR " +
                "       WHERE TRANSACTION_POID = :pBLPoid " +
                "   ) " +
                ")";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("pBLPoid", pBLPoid);

        return query.getResultList();
    }

    public String printDocumentAlreadyPrinted(String pTypeRequest, Long transactionPoid) {
        try {


            String pendingAmountQuery =
                    " SELECT NVL(SUM(PER_QUANTITY_AMOUNT),0) PENDING_AMOUNT FROM SHIP_BL_MANIFEST_HDR MHDR " +
                            " INNER JOIN SHIP_BL_MANIFEST_CHARGES_DTL MCDTL ON MHDR.TRANSACTION_POID=MCDTL.TRANSACTION_POID " +
                            " WHERE MCDTL.TRANSACTION_POID IN (SELECT TRANSACTION_POID FROM SHIP_BL_MANIFEST_CHARGES_DTL " +
                            " WHERE FREIGHT_TYPE='C' AND RECEIPT_INVOICE_POID IS NOT NULL " +
                            " AND TRANSACTION_POID IN (SELECT TRANSACTION_POID FROM SHIP_BL_MANIFEST_HDR WHERE BL_TYPE<>'EXPORT')) " +
                            " AND FREIGHT_TYPE='C' AND RECEIPT_INVOICE_POID IS NULL " +
                            " AND NVL(PER_QUANTITY_AMOUNT,0)<>0 AND MHDR.TRANSACTION_POID=?";

            BigDecimal pendingAmount = jdbcTemplate.queryForObject(pendingAmountQuery, BigDecimal.class, transactionPoid);

            if (pendingAmount != null && pendingAmount.compareTo(BigDecimal.ZERO) != 0) {
                throw new ValidationException("Total amount need to Collect for D/O...." + pendingAmount);
            }
            String printStatusQuery = " select DO_PRINTED ,CNT_FORM_DLV_PRINTED,CNT_FORM_RTN_PRINTED " + " FROM DO_sh_PRINTING_DTL WHERE TRANSACTION_POID=?";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(printStatusQuery, transactionPoid);

            if (rows.isEmpty()) {
                return "N";
            }

            Map<String, Object> row = rows.getFirst();

            String doPrinted = row.get("DO_PRINTED") != null ? row.get("DO_PRINTED").toString() : "N";

            String dlvCntPrinted = row.get("CNT_FORM_DLV_PRINTED") != null ? row.get("CNT_FORM_DLV_PRINTED").toString() : "N";

            String rtnCntPrinted = row.get("CNT_FORM_RTN_PRINTED") != null ? row.get("CNT_FORM_RTN_PRINTED").toString() : "N";

            /* ===============================
             * 3. Return strictly Y / N
             * =============================== */
            if ("DO".equalsIgnoreCase(pTypeRequest)) {
                return "Y".equalsIgnoreCase(doPrinted) ? "Y" : "N";
            }

            if ("DLVCNT".equalsIgnoreCase(pTypeRequest)) {
                return "Y".equalsIgnoreCase(dlvCntPrinted) ? "Y" : "N";
            }

            if ("RTNCNT".equalsIgnoreCase(pTypeRequest)) {
                return "Y".equalsIgnoreCase(rtnCntPrinted) ? "Y" : "N";
            }

            return "N";

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            return "Y";
        }
    }


    public String getPlineCode(Long pBLPoid) {

        String sql = " select distinct " +
                "CONTAINER_FORM_VHENT," +
                "CONTAINER_FORM_RTN," +
                "DO_PRINT_LINE," +
                "LINE_CODE," +
                "RCPT_PRINT_LINE from SHIP_LINE_MASTER where line_poid in " +
                "(select line_poid from SHIP_VOYAGE_HDR where transaction_poid in " +
                "(select voyage_transaction_poid from SHIP_BL_MANIFEST_HDR where transaction_poid=" +
                pBLPoid.toString() + "))";

        try {
            @SuppressWarnings("unchecked")
            List<Object[]> result = entityManager
                    .createNativeQuery(sql)
                    .getResultList();

            if (!result.isEmpty()) {
                Object lineCode = result.get(0)[3];
                return lineCode != null ? lineCode.toString() : "ALL";
            }

            return "ALL";

        } catch (Exception e) {
            e.printStackTrace();
            return "ALL";
        }

    }

    public String getGlobalParameterValue(String parameterName, String parameterKeyIdType, String parameterKeyId, String defaultValue) {

        String sql = "SELECT RTN_GLOBAL_PARAMETER(?, ?, ?, ?, ?) FROM DUAL";

        try {
            return jdbcTemplate.queryForObject(
                    sql,
                    String.class,
                    UserContext.getGroupPoid(),
                    parameterName,
                    parameterKeyIdType,
                    parameterKeyId,
                    defaultValue
            );
        } catch (EmptyResultDataAccessException e) {
            return defaultValue;
        }
    }
}

