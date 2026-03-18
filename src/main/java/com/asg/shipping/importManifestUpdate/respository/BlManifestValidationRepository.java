package com.asg.shipping.importManifestUpdate.respository;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class BlManifestValidationRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean isValidFinancialYear(Long companyPoid, LocalDate transactionDate) {
        try {
            String sql = "SELECT FUNC_GLOB_FINANCIAL_YEAR_VALID(?, ?) FROM DUAL";
            String result = jdbcTemplate.queryForObject(sql, String.class, companyPoid,
                    java.sql.Date.valueOf(transactionDate));
            return result == null || !result.contains("ERROR");
        } catch (Exception e) {
            return false;
        }
    }

    public Long getVoyageCompanyPoid(Long voyageTransactionPoid) {
        try {
            String sql = "SELECT COMPANY_POID FROM SHIP_VOYAGE_HDR WHERE TRANSACTION_POID = ?";
            return jdbcTemplate.queryForObject(sql, Long.class, voyageTransactionPoid);
        } catch (Exception e) {
            return null;
        }
    }

    public String getLineCode(Long voyageTransactionPoid) {
        try {
            String sql = "SELECT GET_LINE_code(line_poid) FROM SHIP_VOYAGE_HDR WHERE transaction_poid = ?";
            return jdbcTemplate.queryForObject(sql, String.class, voyageTransactionPoid);
        } catch (Exception e) {
            return null;
        }
    }
}
