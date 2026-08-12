package com.asg.shipping.common.lov;

import com.asg.shipping.common.cache.MasterDataCache;
import com.asg.shipping.common.dto.LovItem;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Batched, cached lookups for the shipping master-data LOVs.
 *
 * Document screens resolve the same handful of reference tables for every detail row, which turns
 * into an N+1 storm if each row queries for itself. Everything here takes a whole batch of keys and
 * goes through {@link MasterDataCache}, so a request costs at most one query per table and usually
 * none at all once the cache is warm.
 *
 * These are the tables with no group/company scoping, so a cache entry is valid for every tenant.
 * Transactional lookups (quotations, voyages, a document's own containers) do not belong here.
 */
@Component
@RequiredArgsConstructor
public class MasterLovLookup {

    private static final LovItem AUTO_BASIS = new LovItem(273L, "AUTOBASIS", "AUTOBASIS", "AUTOBASIS", 273L, 0);
    private static final LovItem NO_BASIS = new LovItem(253L, "NOBASIS", "NOBASIS", "NOBASIS", 253L, 0);

    private final MasterDataCache masterDataCache;
    private final JdbcTemplate jdbcTemplate;

    public Map<Long, LovItem> portsByPoid(List<Long> poids) {
        return masterDataCache.getAll(MasterDataCache.PORTS, poids, keys -> byPoid(keys, """
                SELECT PORT_POID AS POID, PORT_CODE AS CODE, PORT_NAME AS DESCRIPTION
                FROM SHIP_PORT_MASTER
                WHERE PORT_POID IN (%s)
                """));
    }

    public Map<Long, LovItem> commoditiesByPoid(List<Long> poids) {
        return masterDataCache.getAll(MasterDataCache.COMMODITIES, poids, keys -> byPoid(keys, """
                SELECT COMODITY_POID AS POID, COMODITY_CODE AS CODE, COMODITY_NAME AS DESCRIPTION
                FROM SHIP_COMODITY_MASTER
                WHERE COMODITY_POID IN (%s)
                """));
    }

    public Map<Long, LovItem> chargeMastersByPoid(List<Long> poids) {
        return masterDataCache.getAll(MasterDataCache.CHARGE_MASTERS, poids, keys -> byPoid(keys, """
                SELECT CHARGE_POID AS POID, CHARGE_CODE AS CODE, CHARGE_NAME AS DESCRIPTION
                FROM SHIP_CHARGE_MASTER
                WHERE CHARGE_POID IN (%s)
                """));
    }

    public Map<Long, LovItem> taxesByPoid(List<Long> poids) {
        return masterDataCache.getAll(MasterDataCache.TAXES, poids, keys -> byPoid(keys, """
                SELECT TAX_POID AS POID, TAX_CODE AS CODE,
                       TAX_NAME || ', PERCENTAGE=' || PERCENTAGE || ', CREDIT/DEBIT=' || GL_CREDIT_DEBIT AS DESCRIPTION
                FROM GLOBAL_TAX_MASTER
                WHERE TAX_POID IN (%s)
                """));
    }

    public Map<String, LovItem> containerTypesByCode(List<String> codes) {
        return masterDataCache.getAll(MasterDataCache.CONTAINER_TYPES, upperCodes(codes), this::fetchContainerTypes);
    }

    public Map<String, LovItem> imcoClassesByCode(List<String> codes) {
        return masterDataCache.getAll(MasterDataCache.IMCO_CLASSES, upperCodes(codes), keys -> byCode(keys, """
                SELECT IMCO_CLASS_TYPE_POID AS POID, IMCO_CLASS_TYPE_CODE AS CODE, IMCO_CLASS_TYPE_NAME AS DESCRIPTION
                FROM SHIP_IMCO_CLASS_TYPE_MASTER
                WHERE UPPER(IMCO_CLASS_TYPE_CODE) IN (%s)
                """));
    }

    public Map<String, LovItem> oogTypesByCode(List<String> codes) {
        return masterDataCache.getAll(MasterDataCache.OOG_TYPES, upperCodes(codes), keys -> byCode(keys, """
                SELECT OOG_TYPE_POID AS POID, OOG_TYPE_CODE AS CODE, OOG_TYPE_NAME AS DESCRIPTION
                FROM SHIP_OOG_TYPE_MASTER
                WHERE UPPER(OOG_TYPE_CODE) IN (%s)
                """));
    }

    public Map<String, LovItem> currenciesByCode(List<String> codes) {
        return masterDataCache.getAll(MasterDataCache.CURRENCIES, upperCodes(codes), keys -> byCode(keys, """
                SELECT CURRENCY_POID AS POID, CURRENCY_CODE AS CODE, CURRENCY_NAME AS DESCRIPTION
                FROM GLOBAL_CURRENCY_MASTER
                WHERE UPPER(CURRENCY_CODE) IN (%s)
                """));
    }

    /**
     * Charge basis codes come from the container-type table with two synthetic rows layered on top —
     * the rows the original CONTAINER_TYPE_MASTER LOV UNION ALLs in. The real codes share the
     * container-type cache, since it is the same table, key and row shape.
     */
    public Map<String, LovItem> basisByCode(List<String> codes) {
        List<String> upperCodes = upperCodes(codes);
        if (upperCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, LovItem> map = new HashMap<>();
        if (upperCodes.contains("AUTOBASIS")) {
            map.put("AUTOBASIS", AUTO_BASIS);
        }
        if (upperCodes.contains("NOBASIS")) {
            map.put("NOBASIS", NO_BASIS);
        }
        List<String> dbCodes = upperCodes.stream()
                .filter(code -> !code.equals("AUTOBASIS") && !code.equals("NOBASIS"))
                .collect(Collectors.toList());
        map.putAll(masterDataCache.getAll(MasterDataCache.CONTAINER_TYPES, dbCodes, this::fetchContainerTypes));
        return map;
    }

    private Map<String, LovItem> fetchContainerTypes(List<String> upperCodes) {
        return byCode(upperCodes, """
                SELECT CONTAINER_TYPE_POID AS POID, CONTAINER_TYPE_CODE AS CODE, CONTAINER_TYPE_NAME AS DESCRIPTION
                FROM SHIP_CONTAINER_TYPE_MASTER
                WHERE ACTIVE = 'Y' AND UPPER(CONTAINER_TYPE_CODE) IN (%s)
                """);
    }

    private static List<String> upperCodes(List<String> codes) {
        return codes == null ? List.of()
                : codes.stream().filter(c -> c != null && !c.isBlank())
                        .map(String::toUpperCase).distinct().collect(Collectors.toList());
    }

    private Map<Long, LovItem> byPoid(List<?> keys, String sqlTemplate) {
        Map<Long, LovItem> map = new HashMap<>();
        fetchRows(sqlTemplate, keys).forEach(row -> {
            Long poid = ((Number) row[0]).longValue();
            map.put(poid, toItem(poid, (String) row[1], (String) row[2]));
        });
        return map;
    }

    private Map<String, LovItem> byCode(List<?> keys, String sqlTemplate) {
        Map<String, LovItem> map = new HashMap<>();
        fetchRows(sqlTemplate, keys).forEach(row -> {
            Long poid = ((Number) row[0]).longValue();
            String code = (String) row[1];
            map.put(code.toUpperCase(), toItem(poid, code, (String) row[2]));
        });
        return map;
    }

    /** Every LOV query here selects the same (POID, CODE, DESCRIPTION) shape. */
    private List<Object[]> fetchRows(String sqlTemplate, List<?> keys) {
        String sql = String.format(sqlTemplate, String.join(",", Collections.nCopies(keys.size(), "?")));
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new Object[]{rs.getObject(1), rs.getString(2), rs.getString(3)}, keys.toArray());
    }

    private static LovItem toItem(Long poid, String code, String description) {
        return new LovItem(poid, code, description, description, poid, 0);
    }
}
