package com.asg.shipping.deliveryorderissuetocustomer.repository;

import com.asg.shipping.deliveryorderissuetocustomer.dto.DeliveryOrderIssueToCustomerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeliveryOrderIssueToCustomerRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Find delivery order by transaction POID
     */
    public Optional<DeliveryOrderIssueToCustomerDto> findByTransactionPoid(Long transactionPoid, Long companyPoid) {
        String sql = "SELECT COMPANY_POID, TRANSACTION_POID, TRANSACTION_DATE, DOC_REF, JOBNO, " +
                "ARRIVAL_DATE, BL_NUMBER, LINE, CONSIGNEE, NOTIFY, C_20, C_40, HOLD_DO, " +
                "DO_RELASED_ID_PERSON, DO_RELASED_TO_PERSON, DO_RELASED_ADDRS_PERSON, " +
                "BL_RELEASE_TYPE_OFFICE, ORIGNAL_BL_RELEASE_CR, DO_PRIORITY, DO_ISSUE_AUTH, " +
                "DO_ISSUE_AUTH_POID, DO_CNT_TO_CONSIGNEE, DO_CNT_TO_NOTIFY, DO_CNT_TO_OTHERS, " +
                "DO_CNT_TO_OTHERS_MAILS, DO_EMAILS, DELIVERY_SENT_TO, PRINCIPAL_DO_NUMBER, " +
                "PRINCIPAL_DO_REQUIRED " +
                "FROM VW_CREDIT_DELIVERY_ORDER_PEND " +
                "WHERE TRANSACTION_POID = ? AND COMPANY_POID = ?";

        try {
            List<DeliveryOrderIssueToCustomerDto> results = jdbcTemplate.query(sql, new DeliveryOrderRowMapper(), transactionPoid, companyPoid);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            // Log error and return empty
            return Optional.empty();
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
                    .transactionDate(rs.getDate("TRANSACTION_DATE") != null ?
                            rs.getDate("TRANSACTION_DATE").toLocalDate() : null)
                    .docRef(rs.getString("DOC_REF"))
                    .jobNo(rs.getString("JOBNO"))
                    .arrivalDate(rs.getDate("ARRIVAL_DATE") != null ?
                            rs.getDate("ARRIVAL_DATE").toLocalDate() : null)
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
                    .blReleaseTypeOffice(rs.getString("BL_RELEASE_TYPE_OFFICE"))
                    .originalBlReleaseCr(rs.getString("ORIGNAL_BL_RELEASE_CR"))
                    .doPriority(rs.getString("DO_PRIORITY"))
                    .doIssueAuth(rs.getString("DO_ISSUE_AUTH"))
                    .doIssueAuthPoid(rs.getObject("DO_ISSUE_AUTH_POID") != null ?
                            rs.getLong("DO_ISSUE_AUTH_POID") : null)
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
}