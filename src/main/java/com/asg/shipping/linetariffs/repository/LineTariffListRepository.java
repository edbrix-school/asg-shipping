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
 * List search for doc 100-050 — legacy period overlap + exact COMPANY_POID / GROUP_POID.
 * DocumentSearchService cannot apply exact COMPANY_POID on master docs.
 */
@Repository
@Slf4j
public class LineTariffListRepository {

    private static final String TABLE = "SHIP_LINE_TARIFF_HDR";
    private static final String SELECT_COLUMNS = """
            TRANSACTION_POID, DESCRIPTION, PERIOD_FROM, PERIOD_TO, DELETED,
            DMG_FROM_NEXTDAY, DMG_SKIP_HOLIDAYS, DMG_SKIP_WEEKENDS,
            DTN_FROM_NEXTDAY, DTN_SKIP_HOLIDAYS, DTN_SKIP_WEEKENDS,
            DMG_BASESLAB_AFTER_FREE, DTN_BASESLAB_AFTER_FREE,
            PAYABLE_CURRENCY, RECEIVABLE_CURRENCY, DOC_REF, COMPANY_POID, LINE_POID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final String qualifiedTableName;

    public LineTariffListRepository(
            JdbcTemplate jdbcTemplate,
            @Value("${spring.jpa.properties.hibernate.default_schema}") String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.qualifiedTableName = schema + "." + TABLE;
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
            where.append(" AND DELETED = 'Y' ");
        } else {
            where.append(" AND (DELETED IS NULL OR DELETED = 'N') ");
        }

        if (companyPoid != null) {
            where.append(" AND COMPANY_POID IS NOT NULL AND COMPANY_POID = ? ");
            params.add(companyPoid);
        }

        if (groupPoid != null) {
            where.append(" AND GROUP_POID = ? ");
            params.add(groupPoid);
        }

        if (startDate != null && endDate != null) {
            where.append(" AND PERIOD_FROM <= ? AND PERIOD_TO >= ? AND PERIOD_TO <= ? ");
            params.add(java.sql.Date.valueOf(endDate));
            params.add(java.sql.Date.valueOf(startDate));
            params.add(java.sql.Date.valueOf(endDate));
        }

        appendExtraFilters(extraFilters, where, params);

        String fromClause = " FROM " + qualifiedTableName + where;
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
            if (value.isEmpty() || !"DESCRIPTION".equals(field)) {
                continue;
            }
            where.append(" AND UPPER(DESCRIPTION) LIKE ? ");
            params.add("%" + value.toUpperCase() + "%");
        }
    }

    private String buildOrderBy(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return " ORDER BY TRANSACTION_POID ASC";
        }
        List<String> parts = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            parts.add(mapSortColumn(order.getProperty()) + " " + order.getDirection().name());
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
