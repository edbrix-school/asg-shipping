package com.asg.shipping.common.repository;

import com.asg.shipping.common.dto.LovItem;
import com.asg.shipping.common.dto.LovResponse;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
@Slf4j
public class LovRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    public LovResponse getLovList(String lovName, Long docKeyPoid, String filterValue, Long groupPoid, Long companyPoid, Long userPoid) {
        try {
            final String sql = "BEGIN PROC_LOV_GETLIST(?,?,?,?,?,?,?); END;";
            log.info("Executing PROC_LOV_GETLIST for lovName={} docKeyPoid={} filterValue={} groupPoid={} companyPoid={} userId={}",
                    lovName, docKeyPoid, filterValue, groupPoid, companyPoid, userPoid);
            return jdbcTemplate.execute((Connection con) -> {
                try (CallableStatement cs = con.prepareCall(sql)) {

                    cs.setLong(1, groupPoid != null ? groupPoid : 1);
                    cs.setLong(2, companyPoid != null ? companyPoid : 1);
                    cs.setLong(3, userPoid != null ? userPoid : 1);
                    cs.setString(4, lovName);
                    if (docKeyPoid != null) {
                        cs.setString(5, "");
                    } else {
                        cs.setObject(5, null);
                    }
                    cs.setString(6, filterValue != null ? filterValue : "");
                    cs.registerOutParameter(7, OracleTypes.CURSOR);
                    cs.execute();

                    List<LovItem> items = new ArrayList<>();
                    try (ResultSet rs = (ResultSet) cs.getObject(7)) {
                        if (rs != null) {
                            while (rs.next()) {
                                Long poid = rs.getLong("POID");
                                String code = rs.getString("CODE");
                                String description = rs.getString("DESCRIPTION");
                                Integer seqNo;

                                try {
                                    seqNo = rs.getInt("SEQNO");
                                } catch (SQLException ignored) {
                                    seqNo = 0;
                                }
                                items.add(new LovItem(poid, code, description, description, poid, seqNo));
                            }
                        }
                    }
                    LovResponse response = new LovResponse(items);
                    log.info("PROC_LOV_GETLIST completed for lovName={} itemsFetched={}", lovName, items.size());
                    return response;
                } catch (SQLException ex) {
                    log.error("Error executing PROC_LOV_GETLIST for lovName={}: {}", lovName, ex.getMessage(), ex);
                    throw new RuntimeException("Error fetching LOV list: " + ex.getMessage(), ex);
                }
            });

        }catch (Exception e){
            log.error("Unexpected error fetching LOV list for lovName={}", lovName, e);
            throw new RuntimeException();
        }
    }
}
