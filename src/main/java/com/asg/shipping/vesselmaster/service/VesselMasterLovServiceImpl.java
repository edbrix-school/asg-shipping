package com.asg.shipping.vesselmaster.service;

import com.asg.shipping.common.dto.LovItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation for Vessel Master LOV operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VesselMasterLovServiceImpl implements VesselMasterLovService {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<LovItem> LOV_ITEM_ROW_MAPPER = new RowMapper<LovItem>() {
        @Override
        public LovItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            LovItem item = new LovItem();
            item.setPoid(rs.getLong("POID"));
            item.setCode(rs.getString("CODE"));
            item.setDescription(rs.getString("DESCRIPTION"));
            return item;
        }
    };

    @Override
    @Transactional(readOnly = true)
    public List<LovItem> getLineMasterLov(Long linePoid) {
        log.info("Fetching LINE_MASTER LOV with linePoid filter: {}", linePoid);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT LINE_POID AS POID, ");
        sql.append("       LINE_CODE AS CODE, ");
        sql.append("       LINE_NAME AS DESCRIPTION ");
        sql.append("FROM SHIP_LINE_MASTER ");
        sql.append("WHERE ACTIVE = 'Y' ");

        List<Object> params = new ArrayList<>();

        if (linePoid != null) {
            sql.append("AND LINE_POID = ? ");
            params.add(linePoid);
        }

        sql.append("ORDER BY LINE_NAME");

        List<LovItem> result;
        if (params.isEmpty()) {
            result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER);
        } else {
            result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
        }
        log.info("Fetched {} LINE_MASTER LOV items", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LovItem> getVesselTypeLov(Long vesselTypePoid) {
        log.info("Fetching VESSEL_TYPE LOV with vesselTypePoid filter: {}", vesselTypePoid);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT VESSEL_TYPE_POID AS POID, ");
        sql.append("       VESSEL_TYPE_CODE AS CODE, ");
        sql.append("       VESSEL_TYPE_NAME AS DESCRIPTION ");
        sql.append("FROM SHIP_VESSEL_TYPE_MASTER ");
        sql.append("WHERE ACTIVE = 'Y' ");

        List<Object> params = new ArrayList<>();

        if (vesselTypePoid != null) {
            sql.append("AND VESSEL_TYPE_POID = ? ");
            params.add(vesselTypePoid);
        }

        sql.append("ORDER BY VESSEL_TYPE_NAME");

        List<LovItem> result;
        if (params.isEmpty()) {
            result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER);
        } else {
            result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
        }
        log.info("Fetched {} VESSEL_TYPE LOV items", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LovItem> getAgentMasterLov(Long agentPoid) {
        log.info("Fetching AGENT_MASTER LOV with agentPoid filter: {}", agentPoid);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT AGENT_POID AS POID, ");
        sql.append("       'LINECODE=' || LINE_CODE || ', PORTCODE=' || PORT_CODE AS CODE, ");
        sql.append("       AGENT_NAME || ' ' || REPLACE(REPLACE(DETAILS, CHR(10)), CHR(13)) AS DESCRIPTION ");
        sql.append("FROM SHIP_AGENT_MASTER SAM ");
        sql.append("LEFT JOIN SHIP_LINE_MASTER SLM ON SAM.LINE_POID = SLM.LINE_POID ");
        sql.append("LEFT JOIN SHIP_PORT_MASTER SPM ON SAM.PORT_POID = SPM.PORT_POID ");
        sql.append("WHERE NVL(SAM.ACTIVE, 'Y') = 'Y' ");

        List<Object> params = new ArrayList<>();

        if (agentPoid != null) {
            sql.append("AND AGENT_POID = ? ");
            params.add(agentPoid);
        }

        sql.append("ORDER BY AGENT_NAME");

        List<LovItem> result;
        if (params.isEmpty()) {
            result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER);
        } else {
            result = jdbcTemplate.query(sql.toString(), LOV_ITEM_ROW_MAPPER, params.toArray());
        }
        log.info("Fetched {} AGENT_MASTER LOV items", result.size());
        return result;
    }
}

