package com.asg.shipping.containerinventorymovementupdate.repository.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.jdbc.OracleTypes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CimuLovSuggestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<String> suggestContainerNos(Long groupPoid, Long companyPoid, Long userPoid, String partial) {
        final String sql = "BEGIN PROC_sh_bl_relsed_querylist(?,?,?,?,?); END;";
        String pGetEnterValue = "CONTAINERNO" + (partial != null ? partial.trim() : "");

        log.info("Suggest containers via PROC_sh_bl_relsed_querylist | groupPoid={} companyPoid={} userPoid={} query={}",
                groupPoid, companyPoid, userPoid, partial);

        return jdbcTemplate.execute((Connection con) -> {
            try (CallableStatement cs = con.prepareCall(sql)) {
                cs.setLong(1, groupPoid != null ? groupPoid : 1L);
                cs.setLong(2, companyPoid != null ? companyPoid : 1L);
                cs.setLong(3, userPoid != null ? userPoid : 1L);
                cs.setString(4, pGetEnterValue);
                cs.registerOutParameter(5, OracleTypes.CURSOR);
                cs.execute();

                List<String> items = new ArrayList<>();
                try (ResultSet rs = (ResultSet) cs.getObject(5)) {
                    if (rs != null) {
                        while (rs.next()) {
                            // proc returns rownum, CONTAINER_NO (alias)
                            String value = rs.getString(2);
                            if (value != null && !value.isBlank()) items.add(value.trim());
                        }
                    }
                }
                log.info("Suggest containers completed | count={}", items.size());
                return items;
            }
        });
    }
}


