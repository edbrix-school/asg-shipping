package com.asg.shipping.containerinventorymovementupdate.repository.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;


import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CimuDemurrageRepository {

    private final JdbcTemplate jdbcTemplate;

    public BigDecimal calculateDemurrage(Long transactionPoid, String containerNo, String demDt) {
        String sql = "SELECT FUNC_SHIP_CNT_DEM_RTN(?, ?, ?) FROM DUAL";
        log.info("Calling FUNC_SHIP_CNT_DEM_RTN | transactionPoid={} containerNo={} demDt={}",
                transactionPoid, containerNo, demDt);
        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) return rs.getBigDecimal(1);
            return BigDecimal.ZERO;
        }, transactionPoid, containerNo, demDt);
    }

    /**
     * Calls FUNC_SHIP_CNT_IMPORT_TOTAL to get formatted collection summary message.
     * Returns: "Total amount need to collect =X, Total pending amount need to collect =Y"
     *
     * Function exists in DDL (line 2361) and calculates totals from SHIP_BL_MANIFEST_charges_DTL.
     */
    public String getImportTotalMessage(Long transactionPoid, String containerNo, String demDt) {
        String sql = "BEGIN ? := FUNC_SHIP_CNT_IMPORT_TOTAL(?, ?, ?); END;";
        log.info("Calling FUNC_SHIP_CNT_IMPORT_TOTAL | transactionPoid={} containerNo={} demDt={}",
                transactionPoid, containerNo, demDt);
        return jdbcTemplate.execute((Connection con) -> {
            try (CallableStatement cs = con.prepareCall(sql)) {
                cs.registerOutParameter(1, java.sql.Types.VARCHAR);
                cs.setObject(2, transactionPoid);
                cs.setObject(3, containerNo);
                cs.setObject(4, demDt);
                cs.execute();
                return cs.getString(1);
            } catch (SQLException e) {
                log.error("Error calling FUNC_SHIP_CNT_IMPORT_TOTAL: {}", e.getMessage(), e);
                throw new RuntimeException("Error calling FUNC_SHIP_CNT_IMPORT_TOTAL: " + e.getMessage(), e);
            }
        });
    }

    public BigDecimal getPortDays(Long transactionPoid, String containerNo, String emptyIn) {
        String sql = "SELECT FUNC_SHIP_CNT_PORT_DAYS(?, ?, ?) FROM DUAL";
        log.info("Calling FUNC_SHIP_CNT_PORT_DAYS | transactionPoid={} containerNo={} emptyIn={}",
                transactionPoid, containerNo, emptyIn);
        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) return rs.getBigDecimal(1);
            return BigDecimal.ZERO;
        }, transactionPoid, containerNo, emptyIn);
    }
}


