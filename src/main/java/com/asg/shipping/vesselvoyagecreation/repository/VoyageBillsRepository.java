package com.asg.shipping.vesselvoyagecreation.repository;

import com.asg.shipping.vesselvoyagecreation.dto.VoyageBlFilter;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageBlRow;
import com.asg.shipping.vesselvoyagecreation.dto.VoyageBlTab;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class VoyageBillsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<VoyageBlRow> listBills(Long voyagePoid, VoyageBlTab tab, VoyageBlFilter filter, Pageable pageable) {
        String view = switch (tab) {
            case HOLD -> "VOYAGEWISEBILLS";
            case APPROVAL1 -> "VOYAGEWISEBILLS_APPROVALONE";
            case APPROVAL2 -> "VOYAGEWISEBILLS_APPROVALTWO";
            case APPROVED -> "VOYAGEWISEBILLS_APPROVED";
        };

        StringBuilder where = new StringBuilder(" WHERE TRANSACTION_POID = :voyagePoid ");

        if (filter != null && filter != VoyageBlFilter.ALL) {
            switch (filter) {
                case EXPORT -> where.append(" AND BL_TYPE LIKE '%EXPORT%' ");
                case IMPORT -> where.append(" AND BL_TYPE NOT LIKE '%EXPORT%' ");
                case REFFER -> where.append(" AND NVL(IS_REFFER,'N') = 'Y' ");
                case IMCO -> where.append(" AND NVL(IS_IMCO,'N') = 'Y' ");
                default -> {
                }
            }
        }

        String countSql = "SELECT COUNT(*) FROM " + view + where;
        Query countQ = entityManager.createNativeQuery(countSql);
        countQ.setParameter("voyagePoid", voyagePoid);
        long total = ((Number) countQ.getSingleResult()).longValue();

        String pageSql = "SELECT * FROM " + view + where;
        Query pageQ = entityManager.createNativeQuery(pageSql, Tuple.class);
        pageQ.setParameter("voyagePoid", voyagePoid);
        pageQ.setFirstResult((int) pageable.getOffset());
        pageQ.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Tuple> tuples = pageQ.getResultList();
        List<VoyageBlRow> rows = new ArrayList<>(tuples.size());
        for (Tuple t : tuples) {
            rows.add(toRow(t));
        }
        return new PageImpl<>(rows, pageable, total);
    }

    private static VoyageBlRow toRow(Tuple t) {
        return VoyageBlRow.builder()
                .transactionPoid(getLong(t, "TRANSACTION_POID"))
                .blType(getString(t, "BL_TYPE"))
                .blIssueType(getString(t, "BL_ISSUE_TYPE"))
                .blNumber(getString(t, "BL_NUMBER"))
                .shipperEdiName(getString(t, "SHIPPER_EDI_NAME"))
                .consigneeEdiName(getString(t, "CONSIGNEE_EDI_NAME"))
                .notify1EdiName(getString(t, "NOTIFY1_EDI_NAME"))
                .notify2EdiName(getString(t, "NOTIFY2_EDI_NAME"))
                .salesman(getString(t, "SALESMAN"))
                .blPoid(getLong(t, "BL_POID"))
                .drilldownLinkInfo(getString(t, "DRILLDOWN_LINK_INFO"))
                .isReffer(getString(t, "IS_REFFER"))
                .isImco(getString(t, "IS_IMCO"))
                .doPrinted(getString(t, "DO_PRINTED"))
                .build();
    }

    private static String getString(Tuple t, String col) {
        try {
            return t.get(col, String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static Long getLong(Tuple t, String col) {
        try {
            Object v = t.get(col);
            if (v == null) return null;
            if (v instanceof Number n) return n.longValue();
            return Long.valueOf(v.toString());
        } catch (Exception e) {
            return null;
        }
    }
}


