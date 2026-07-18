package com.asg.shipping.exportManifestBl.repository;

import com.asg.shipping.exportManifestBl.dto.BookingSelectionItemDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ExportManifestBlLoadBookingRepositoryImpl implements ExportManifestBlLoadBookingRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final String defaultSchema;

    private final JdbcTemplate jdbcTemplate;

    public ExportManifestBlLoadBookingRepositoryImpl(
            JdbcTemplate jdbcTemplate,
            @Value("${spring.jpa.properties.hibernate.default_schema}") String defaultSchema) {
        this.jdbcTemplate = jdbcTemplate;
        this.defaultSchema = defaultSchema;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void stageBookingSelections(Long groupPoid, Long companyPoid, List<BookingSelectionItemDto> selections) {
        entityManager.createNativeQuery("DELETE FROM GLOBAL_TEMP_BOOKING_SELECTED").executeUpdate();
        for (BookingSelectionItemDto row : selections) {
            insertRow(groupPoid, companyPoid, row);
        }
        entityManager.flush();
    }

    private void insertRow(Long groupPoid, Long companyPoid, BookingSelectionItemDto row) {
        String sql = """
                INSERT INTO GLOBAL_TEMP_BOOKING_SELECTED (
                    GROUP_POID, COMPANY_POID, TRANSACTION_POID, VOYAGE_NO,
                    CONTAINER_NO, BOOKING_ISSUE_NO, IS_SELECTED)
                VALUES (:groupPoid, :companyPoid, :transactionPoid, :voyageNo,
                    :containerNo, :bookingIssueNo, :isSelected)
                """;
        entityManager.createNativeQuery(sql)
                .setParameter("groupPoid", groupPoid)
                .setParameter("companyPoid", companyPoid)
                .setParameter("transactionPoid", row.getTransactionPoid())
                .setParameter("voyageNo", row.getVoyageNo())
                .setParameter("containerNo", row.getContainerNo())
                .setParameter("bookingIssueNo", row.getBookingIssueNo())
                .setParameter("isSelected", row.getIsSelected() != null ? row.getIsSelected() : "Y")
                .executeUpdate();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String funcLoadBookingToBl(String loginUser, Long voyageTransactionPoid) {
        return jdbcTemplate.execute(
                (ConnectionCallback<String>) connection -> callFuncLoadBookingToBl(connection, loginUser, voyageTransactionPoid));
    }

   
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String stageAndLoadBookingToBl(
            Long groupPoid,
            Long companyPoid,
            List<BookingSelectionItemDto> selections,
            String loginUser,
            Long voyageTransactionPoid) {
        return jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (BookingSelectionItemDto row : selections) {
                    if (row.getIsSelected() == null || !"Y".equalsIgnoreCase(row.getIsSelected())) {
                        continue;
                    }
                    String plsql = """
                            BEGIN INSERT INTO GLOBAL_TEMP_BOOKING_SELECTED (
                                GROUP_POID, COMPANY_POID, TRANSACTION_POID, VOYAGE_NO,
                                CONTAINER_NO, BOOKING_ISSUE_NO, IS_SELECTED)
                            VALUES (?, ?, ?, ?, ?, ?, ?); END;
                            """;
                    try (CallableStatement insert = connection.prepareCall(plsql)) {
                        insert.setLong(1, groupPoid);
                        insert.setLong(2, companyPoid);
                        insert.setLong(3, row.getTransactionPoid());
                        insert.setString(4, row.getVoyageNo());
                        insert.setString(5, row.getContainerNo());
                        insert.setString(6, row.getBookingIssueNo());
                        insert.setString(7, row.getIsSelected());
                        insert.executeUpdate();
                    }
                }
                return callFuncLoadBookingToBl(connection, loginUser, voyageTransactionPoid);
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });
    }

    private String callFuncLoadBookingToBl(Connection connection, String loginUser, Long voyageTransactionPoid)
            throws SQLException {
        String call = "BEGIN ? := " + defaultSchema + ".FUNC_LOAD_BOOKING_TO_BL(?, ?); END;";
        try (CallableStatement cs = connection.prepareCall(call)) {
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.setString(2, loginUser);
            cs.setLong(3, voyageTransactionPoid);
            cs.execute();
            Object out = cs.getObject(1);
            return out != null ? out.toString() : null;
        }
    }
}
