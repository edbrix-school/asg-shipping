package com.asg.shipping.vvc.bl.repository.jdbc;

import com.asg.shipping.vvc.bl.dto.VoyageBlFilter;
import com.asg.shipping.vvc.bl.dto.VoyageBlRow;
import com.asg.shipping.vvc.bl.dto.VoyageBlTab;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class VoyageBillsJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<VoyageBlRow> ROW_MAPPER = (rs, rowNum) -> VoyageBlRow.builder()
            .transactionPoid(rs.getLong("TRANSACTION_POID"))
            .blType(rs.getString("BL_TYPE"))
            .blIssueType(safeGet(rs, "BL_ISSUE_TYPE"))
            .blNumber(rs.getString("BL_NUMBER"))
            .shipperEdiName(safeGet(rs, "SHIPPER_EDI_NAME"))
            .consigneeEdiName(safeGet(rs, "CONSIGNEE_EDI_NAME"))
            .notify1EdiName(safeGet(rs, "NOTIFY1_EDI_NAME"))
            .notify2EdiName(safeGet(rs, "NOTIFY2_EDI_NAME"))
            .salesman(safeGet(rs, "SALESMAN"))
            .blPoid(safeGetLong(rs, "BL_POID"))
            .drilldownLinkInfo(safeGet(rs, "DRILLDOWN_LINK_INFO"))
            .isReffer(safeGet(rs, "IS_REFFER"))
            .isImco(safeGet(rs, "IS_IMCO"))
            .doPrinted(safeGet(rs, "DO_PRINTED"))
            .build();

    public Page<VoyageBlRow> listBills(Long voyagePoid, VoyageBlTab tab, VoyageBlFilter filter, Pageable pageable) {
        String view = switch (tab) {
            case HOLD -> "VOYAGEWISEBILLS";
            case APPROVAL1 -> "VOYAGEWISEBILLS_APPROVALONE";
            case APPROVAL2 -> "VOYAGEWISEBILLS_APPROVALTWO";
            case APPROVED -> "VOYAGEWISEBILLS_APPROVED";
        };

        StringBuilder where = new StringBuilder(" WHERE TRANSACTION_POID = ? ");
        List<Object> params = new ArrayList<>();
        params.add(voyagePoid);

        if (filter != null && filter != VoyageBlFilter.ALL) {
            switch (filter) {
                case EXPORT -> where.append(" AND BL_TYPE LIKE '%EXPORT%' ");
                case IMPORT -> where.append(" AND BL_TYPE NOT LIKE '%EXPORT%' ");
                case REFFER -> where.append(" AND NVL(IS_REFFER,'N') = 'Y' ");
                case IMCO -> where.append(" AND NVL(IS_IMCO,'N') = 'Y' ");
                default -> { }
            }
        }

        String baseSql = "SELECT * FROM " + view + where;
        String countSql = "SELECT COUNT(*) FROM " + view + where;

        long total = jdbcTemplate.queryForObject(countSql, params.toArray(), Long.class);

        String pageSql = baseSql + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add((long) pageable.getPageNumber() * pageable.getPageSize());
        pageParams.add(pageable.getPageSize());

        List<VoyageBlRow> rows = jdbcTemplate.query(pageSql, pageParams.toArray(), ROW_MAPPER);
        return new PageImpl<>(rows, pageable, total);
    }

    private static String safeGet(java.sql.ResultSet rs, String col) {
        try { return rs.getString(col); } catch (Exception e) { return null; }
    }

    private static Long safeGetLong(java.sql.ResultSet rs, String col) {
        try {
            long v = rs.getLong(col);
            return rs.wasNull() ? null : v;
        } catch (Exception e) {
            return null;
        }
    }
}


