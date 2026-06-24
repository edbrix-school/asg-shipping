package com.asg.shipping.linetariffs.repository;

import com.asg.common.lib.dto.FilterDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * List search for doc 100-050 — exact COMPANY_POID / GROUP_POID and legacy period filter.
 * Master doc 100-050 does not scope correctly through DocumentSearchService alone.
 */
@Repository
@Slf4j
public class LineTariffListRepository {

    private static final String TABLE = "SHIP_LINE_TARIFF_HDR";
    private static final String ALIAS = "t";
    private static final String SELECT_COLUMNS = """
            t.TRANSACTION_POID, t.DESCRIPTION, t.PERIOD_FROM, t.PERIOD_TO, t.DELETED,
            t.DMG_FROM_NEXTDAY, t.DMG_SKIP_HOLIDAYS, t.DMG_SKIP_WEEKENDS,
            t.DTN_FROM_NEXTDAY, t.DTN_SKIP_HOLIDAYS, t.DTN_SKIP_WEEKENDS,
            t.DMG_BASESLAB_AFTER_FREE, t.DTN_BASESLAB_AFTER_FREE,
            t.PAYABLE_CURRENCY, t.RECEIVABLE_CURRENCY, t.DOC_REF, t.COMPANY_POID, t.LINE_POID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final String qualifiedTableName;
    private final String lineMasterTable;

    public LineTariffListRepository(
            JdbcTemplate jdbcTemplate,
            @Value("${spring.jpa.properties.hibernate.default_schema}") String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.qualifiedTableName = schema + "." + TABLE;
        this.lineMasterTable = schema + ".SHIP_LINE_MASTER";
    }

    public record ListSearchResult(List<Map<String, Object>> records, long totalRecords) {}

    public ListSearchResult search(Long companyPoid,
                                   Long groupPoid,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   String isDeleted,
                                   List<FilterDto> extraFilters,
                                   Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if ("Y".equalsIgnoreCase(isDeleted)) {
            where.append(" AND ").append(col("DELETED")).append(" = 'Y' ");
        } else {
            where.append(" AND (").append(col("DELETED")).append(" IS NULL OR ").append(col("DELETED")).append(" = 'N') ");
        }

        if (companyPoid != null) {
            where.append(" AND ").append(col("COMPANY_POID")).append(" IS NOT NULL AND ").append(col("COMPANY_POID")).append(" = ? ");
            params.add(companyPoid);
        }

        if (groupPoid != null) {
            where.append(" AND ").append(col("GROUP_POID")).append(" = ? ");
            params.add(groupPoid);
        }

        if (startDate != null && endDate != null) {
            where.append(" AND ").append(col("PERIOD_FROM")).append(" <= ? AND ")
                    .append(col("PERIOD_TO")).append(" >= ? ");
            params.add(java.sql.Date.valueOf(endDate));
            params.add(java.sql.Date.valueOf(startDate));
        }

        appendExtraFilters(extraFilters, where, params);

        String fromClause = " FROM " + qualifiedTableName + " " + ALIAS + where;
        log.debug("Line tariff list SQL: SELECT COUNT(*) {}", fromClause);

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) " + fromClause, Long.class, params.toArray());
        long totalRecords = total != null ? total : 0L;

        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageable.getOffset());
        dataParams.add(pageable.getPageSize());

        String dataSql = "SELECT " + SELECT_COLUMNS + fromClause + buildOrderBy(pageable)
                + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<Map<String, Object>> records = jdbcTemplate.query(dataSql, (rs, rowNum) -> mapRow(rs), dataParams.toArray());
        return new ListSearchResult(records, totalRecords);
    }

    private void appendExtraFilters(List<FilterDto> extraFilters, StringBuilder where, List<Object> params) {
        if (extraFilters == null) {
            return;
        }
        for (FilterDto filter : extraFilters) {
            if (filter == null || filter.searchField() == null || filter.searchValue() == null) {
                continue;
            }
            String field = filter.searchField().trim().toUpperCase();
            String value = filter.searchValue().trim();
            if (value.isEmpty()) {
                continue;
            }
            if ("GLOBALSEARCH".equals(field)) {
                appendGlobalSearch(where, params, stripComparisonPrefix(value));
            } else if ("DESCRIPTION".equals(field)) {
                where.append(" AND UPPER(").append(col("DESCRIPTION")).append(") LIKE ? ");
                params.add("%" + stripComparisonPrefix(value).toUpperCase() + "%");
            }
        }
    }

    private void appendGlobalSearch(StringBuilder where, List<Object> params, String term) {
        String like = "%" + term.toUpperCase() + "%";
        where.append(" AND (");
        where.append(" UPPER(").append(col("DESCRIPTION")).append(") LIKE ? ");
        params.add(like);
        where.append(" OR UPPER(").append(col("DOC_REF")).append(") LIKE ? ");
        params.add(like);
        where.append(" OR UPPER(").append(col("PAYABLE_CURRENCY")).append(") LIKE ? ");
        params.add(like);
        where.append(" OR UPPER(").append(col("RECEIVABLE_CURRENCY")).append(") LIKE ? ");
        params.add(like);
        where.append(" OR UPPER(TO_CHAR(").append(col("TRANSACTION_POID")).append(")) LIKE ? ");
        params.add(like);
        where.append(" OR UPPER(TO_CHAR(").append(col("LINE_POID")).append(")) LIKE ? ");
        params.add(like);
        where.append(" OR UPPER(TO_CHAR(").append(col("PERIOD_FROM")).append(", 'YYYY-MM-DD')) LIKE ? ");
        params.add(like);
        where.append(" OR UPPER(TO_CHAR(").append(col("PERIOD_TO")).append(", 'YYYY-MM-DD')) LIKE ? ");
        params.add(like);
        where.append(" OR EXISTS (SELECT 1 FROM ").append(lineMasterTable).append(" lm ");
        where.append(" WHERE lm.LINE_POID = ").append(col("LINE_POID"));
        where.append(" AND (UPPER(lm.LINE_NAME) LIKE ? OR UPPER(lm.LINE_CODE) LIKE ? OR UPPER(lm.LINE_SHORT_NAME) LIKE ?)) ");
        params.add(like);
        params.add(like);
        params.add(like);
        where.append(") ");
    }

    private static String col(String column) {
        return ALIAS + "." + column;
    }

    private static String stripComparisonPrefix(String value) {
        return value.replaceFirst("^[<>=]+", "").trim();
    }

    private String buildOrderBy(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return " ORDER BY " + col("TRANSACTION_POID") + " ASC";
        }
        List<String> parts = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            parts.add(col(mapSortColumn(order.getProperty())) + " " + order.getDirection().name());
        }
        return " ORDER BY " + String.join(", ", parts);
    }

    private String mapSortColumn(String property) {
        if (property == null || property.isBlank()) {
            return "TRANSACTION_POID";
        }
        String normalized = property.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        return switch (normalized) {
            case "LINE_POID", "TRANSACTION_POID", "DESCRIPTION", "PERIOD_FROM", "PERIOD_TO" -> normalized;
            default -> "TRANSACTION_POID";
        };
    }

    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("TRANSACTION_POID", rs.getObject("TRANSACTION_POID"));
        row.put("DESCRIPTION", rs.getString("DESCRIPTION"));
        row.put("PERIOD_FROM", toLocalDateTime(rs.getTimestamp("PERIOD_FROM")));
        row.put("PERIOD_TO", toLocalDateTime(rs.getTimestamp("PERIOD_TO")));
        row.put("DELETED", rs.getString("DELETED"));
        row.put("DMG_FROM_NEXTDAY", rs.getString("DMG_FROM_NEXTDAY"));
        row.put("DMG_SKIP_HOLIDAYS", rs.getString("DMG_SKIP_HOLIDAYS"));
        row.put("DMG_SKIP_WEEKENDS", rs.getString("DMG_SKIP_WEEKENDS"));
        row.put("DTN_FROM_NEXTDAY", rs.getString("DTN_FROM_NEXTDAY"));
        row.put("DTN_SKIP_HOLIDAYS", rs.getString("DTN_SKIP_HOLIDAYS"));
        row.put("DTN_SKIP_WEEKENDS", rs.getString("DTN_SKIP_WEEKENDS"));
        row.put("DMG_BASESLAB_AFTER_FREE", rs.getString("DMG_BASESLAB_AFTER_FREE"));
        row.put("DTN_BASESLAB_AFTER_FREE", rs.getString("DTN_BASESLAB_AFTER_FREE"));
        row.put("PAYABLE_CURRENCY", rs.getString("PAYABLE_CURRENCY"));
        row.put("RECEIVABLE_CURRENCY", rs.getString("RECEIVABLE_CURRENCY"));
        row.put("DOC_REF", rs.getString("DOC_REF"));
        row.put("COMPANY_POID", rs.getObject("COMPANY_POID"));
        row.put("LINE_POID", rs.getObject("LINE_POID"));

        Object label = row.get("DESCRIPTION");
        Object value = row.get("TRANSACTION_POID");
        if (label != null) {
            row.put("label", label);
        }
        if (value != null) {
            row.put("value", value);
        }
        return row;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
