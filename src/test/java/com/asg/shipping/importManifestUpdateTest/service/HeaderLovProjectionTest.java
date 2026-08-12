package com.asg.shipping.importManifestUpdateTest.service;

import com.asg.shipping.importmanifestupdate.service.ImportManifestBlServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The header LOV projection is read positionally in Java ({@code lovAt(row, offset)}), so the
 * SELECT list and those offsets are a contract. Reordering the SELECT without moving the offsets
 * would put the consignee's name on the salesman and still pass every other test — the values are
 * all strings and all plausible. These assertions fail loudly if the two drift apart.
 *
 * Correctness of the joins themselves cannot be checked without a database; use
 * {@code src/test/resources/sql/verify_header_lov_projection.sql} against a real schema for that.
 */
class HeaderLovProjectionTest {

    private static final List<String> EXPECTED_BLOCKS_IN_ORDER = List.of(
            "QTN", "SLM", "CMD", "CNS", "NF1", "CUS", "RCP", "DLV", "LOD", "DIS");

    private static String projectionSql() throws Exception {
        Field field = ImportManifestBlServiceImpl.class.getDeclaredField("HEADER_LOV_PROJECTION_SQL");
        field.setAccessible(true);
        return (String) field.get(null);
    }

    private static String selectList(String sql) {
        int from = sql.indexOf("FROM SHIP_BL_MANIFEST_HDR");
        return sql.substring(sql.indexOf("SELECT") + "SELECT".length(), from);
    }

    /** Each LOV block contributes exactly three columns, in the order lovAt() steps through. */
    @Test
    void selectListProjectsThreeColumnsPerLovBlockInOffsetOrder() throws Exception {
        String select = selectList(projectionSql());

        // Alias occurrences in the select list, in order, ignoring the ones inside the quotation
        // description expression (those are all QTN and collapse into the QTN block anyway).
        Matcher m = Pattern.compile("\\b(QTN|SLM|CMD|CNS|NF1|CUS|RCP|DLV|LOD|DIS)\\.").matcher(select);
        List<String> blocksInOrder = new java.util.ArrayList<>();
        while (m.find()) {
            String alias = m.group(1);
            if (blocksInOrder.isEmpty() || !blocksInOrder.get(blocksInOrder.size() - 1).equals(alias)) {
                blocksInOrder.add(alias);
            }
        }

        assertEquals(EXPECTED_BLOCKS_IN_ORDER, blocksInOrder,
                "SELECT order must match the lovAt(row, offset) offsets 0,3,6,...,27");
    }

    @Test
    void everyProjectedTableIsLeftJoinedSoAMissingRowCannotDropTheHeader() throws Exception {
        String sql = projectionSql();

        long joinCount = sql.lines().filter(l -> l.contains("JOIN")).count();
        long leftJoinCount = sql.lines().filter(l -> l.contains("LEFT JOIN")).count();

        assertEquals(EXPECTED_BLOCKS_IN_ORDER.size(), joinCount, "one join per LOV block");
        assertEquals(joinCount, leftJoinCount, "an inner join here would drop the whole header row");
    }

    @Test
    void isBoundByTransactionPoidRatherThanInlined() throws Exception {
        String sql = projectionSql();

        assertTrue(sql.contains("WHERE HDR.TRANSACTION_POID = :poid"),
                "the header must be selected by bind parameter");
    }

    /** Voyage is intentionally absent: VESSAL_VOYAGE applies scoping a raw join would drop. */
    @Test
    void doesNotProjectTheVoyageLov() throws Exception {
        String sql = projectionSql();

        assertTrue(!sql.contains("SHIP_VOYAGE_HDR"),
                "voyage must stay on LovService so its group/company/user scoping is applied");
    }
}
