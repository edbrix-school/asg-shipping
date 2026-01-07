package com.asg.shipping.containerinventorymovementupdate.repository.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CimuUpdateRepository {

    private final JdbcTemplate jdbcTemplate;

    public String callProcShipCntInvtUpdate(
            String runStatement,
            Long transactionPoid,
            Long freeDays,
            Long extraFreeDaysPrnpls,
            String blIssueType,
            String containerNoOrAll,
            Long loginUserPoid,
            String cntRtnHold
    ) {
        final String sql = "BEGIN PROC_SHIP_CNT_INVT_UPDATE(?,?,?,?,?,?,?,?,?); END;";
        log.info("Calling PROC_SHIP_CNT_INVT_UPDATE | transactionPoid={} containerNoOrAll={} userPoid={}",
                transactionPoid, containerNoOrAll, loginUserPoid);

        return jdbcTemplate.execute((Connection con) -> {
            try (CallableStatement cs = con.prepareCall(sql)) {
                cs.setString(1, runStatement != null ? runStatement : "");
                cs.setString(2, toStringOrNull(transactionPoid));
                cs.setString(3, toStringOrNull(freeDays));
                cs.setString(4, toStringOrNull(extraFreeDaysPrnpls));
                cs.setString(5, blIssueType);
                cs.setString(6, containerNoOrAll);
                cs.registerOutParameter(7, OracleTypes.VARCHAR);
                cs.setString(8, toStringOrNull(loginUserPoid));
                cs.setString(9, cntRtnHold != null ? cntRtnHold : "N");
                cs.execute();
                return cs.getString(7);
            } catch (SQLException e) {
                throw new RuntimeException("Error calling PROC_SHIP_CNT_INVT_UPDATE: " + e.getMessage(), e);
            }
        });
    }
    private String toStringOrNull(Long value) {
        return value != null ? value.toString() : null;
    }


    public String callProcShipCntSocUpdate(String blNumber, Long loginUserPoid) {
        final String sql = "BEGIN PROC_SHIP_CNT_SOC_UPDATE(?,?,?,?); END;";
        log.info("Calling PROC_SHIP_CNT_SOC_UPDATE | blNumber={} userPoid={}", blNumber, loginUserPoid);

        return jdbcTemplate.execute((Connection con) -> {
            try (CallableStatement cs = con.prepareCall(sql)) {
                cs.setString(1, blNumber);
                cs.setString(2, " "); // legacy passes blank container
                cs.registerOutParameter(3, OracleTypes.VARCHAR);
                cs.setString(4, loginUserPoid != null ? loginUserPoid.toString() : null);
                cs.execute();
                return cs.getString(3);
            } catch (SQLException e) {
                throw new RuntimeException("Error calling PROC_SHIP_CNT_SOC_UPDATE: " + e.getMessage(), e);
            }
        });
    }

    public java.util.Map<String, String> callProcShCntInspectXlUpload(Long groupPoid, Long userPoid, Long companyPoid) {
        final String sql = "BEGIN PROC_SH_CNT_INSPECT_XL_UPLOAD(?,?,?,?,?); END;";
        log.info("Calling PROC_SH_CNT_INSPECT_XL_UPLOAD | groupPoid={} userPoid={} companyPoid={}",
                groupPoid, userPoid, companyPoid);

        return jdbcTemplate.execute((Connection con) -> {
            try (CallableStatement cs = con.prepareCall(sql)) {
                cs.setLong(1, groupPoid != null ? groupPoid : 1L);
                cs.setLong(2, userPoid != null ? userPoid : 1L);
                cs.setLong(3, companyPoid != null ? companyPoid : 1L);
                cs.registerOutParameter(4, OracleTypes.VARCHAR);
                cs.registerOutParameter(5, OracleTypes.VARCHAR);
                cs.execute();

                java.util.Map<String, String> result = new java.util.HashMap<>();
                result.put("transactionPoid", cs.getString(4));
                result.put("status", cs.getString(5));
                return result;
            } catch (SQLException e) {
                throw new RuntimeException("Error calling PROC_SH_CNT_INSPECT_XL_UPLOAD: " + e.getMessage(), e);
            }
        });
    }
}


