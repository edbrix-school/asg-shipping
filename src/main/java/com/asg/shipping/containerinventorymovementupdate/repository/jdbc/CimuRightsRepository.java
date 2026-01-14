package com.asg.shipping.containerinventorymovementupdate.repository.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CimuRightsRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * Legacy DB procedure uses FN_GET_USER_HAVE_DOC_RIGHT('000-248', user) = 'TRUE'
     * We reuse same function for rights checks. If it doesn't exist in target DB, this will fail fast.
     */
    public boolean hasDocRight(String docRightId, Long userPoid) {
        String sql = "SELECT FN_GET_USER_HAVE_DOC_RIGHT(?, ?) FROM DUAL";
        String v = jdbcTemplate.query(sql, rs -> {
            if (rs.next()) return rs.getString(1);
            return null;
        }, docRightId, userPoid != null ? userPoid.toString() : null);
        boolean ok = v != null && "TRUE".equalsIgnoreCase(v.trim());
        log.info("Rights check FN_GET_USER_HAVE_DOC_RIGHT | docRightId={} userPoid={} => {}", docRightId, userPoid, ok);
        return ok;
    }
}


