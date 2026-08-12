package com.asg.shipping.common.lov;

import com.asg.shipping.common.cache.MasterDataCache;
import com.asg.shipping.common.dto.LovItem;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterLovLookupTest {

    @Mock private JdbcTemplate jdbcTemplate;

    private MasterLovLookup lookup;

    @BeforeEach
    void setUp() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                new CaffeineCache(MasterDataCache.PORTS, Caffeine.newBuilder().build()),
                new CaffeineCache(MasterDataCache.CONTAINER_TYPES, Caffeine.newBuilder().build())));
        cacheManager.afterPropertiesSet();
        lookup = new MasterLovLookup(new MasterDataCache(cacheManager), jdbcTemplate);
    }

    @SuppressWarnings("unchecked")
    private void stubRows(List<Object[]> rows) {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn((List) rows);
    }

    @Test
    void resolvesABatchOfPortsInOneQuery() {
        stubRows(List.<Object[]>of(new Object[]{1L, "JEA", "JEBEL ALI"}, new Object[]{2L, "AUH", "ABU DHABI"}));

        Map<Long, LovItem> ports = lookup.portsByPoid(List.of(1L, 2L));

        assertEquals("JEBEL ALI", ports.get(1L).getDescription());
        assertEquals("AUH", ports.get(2L).getCode());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void emptyKeysNeverTouchTheDatabase() {
        assertTrue(lookup.portsByPoid(List.of()).isEmpty());
        assertTrue(lookup.commoditiesByPoid(null).isEmpty());
        assertTrue(lookup.currenciesByCode(List.of()).isEmpty());

        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    /** Codes are matched case-insensitively, so the map is keyed by the uppercase form. */
    @Test
    void codeLookupsAreKeyedUppercase() {
        stubRows(List.<Object[]>of(new Object[]{7L, "20gp", "20 FT GENERAL PURPOSE"}));

        Map<String, LovItem> types = lookup.containerTypesByCode(List.of("20gp"));

        assertEquals("20 FT GENERAL PURPOSE", types.get("20GP").getDescription());
        assertNull(types.get("20gp"));
    }

    @Test
    void basisLayersStaticRowsOverTheContainerTypeTable() {
        stubRows(List.<Object[]>of(new Object[]{7L, "20GP", "20 FT GENERAL PURPOSE"}));

        Map<String, LovItem> bases = lookup.basisByCode(List.of("AUTOBASIS", "NOBASIS", "20GP"));

        assertEquals(273L, bases.get("AUTOBASIS").getPoid());
        assertEquals(253L, bases.get("NOBASIS").getPoid());
        assertEquals("20 FT GENERAL PURPOSE", bases.get("20GP").getDescription());
    }

    /** The synthetic basis rows are not in any table, so they must not reach the database. */
    @Test
    void staticBasisRowsAloneIssueNoQuery() {
        Map<String, LovItem> bases = lookup.basisByCode(List.of("AUTOBASIS", "NOBASIS"));

        assertEquals(2, bases.size());
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    /** Basis and container-type codes come from one table, so the second lookup is a cache hit. */
    @Test
    void basisAndContainerTypesShareOneCache() {
        stubRows(List.<Object[]>of(new Object[]{7L, "20GP", "20 FT GENERAL PURPOSE"}));

        lookup.containerTypesByCode(List.of("20GP"));
        Map<String, LovItem> bases = lookup.basisByCode(List.of("20GP"));

        assertEquals("20 FT GENERAL PURPOSE", bases.get("20GP").getDescription());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));
    }
}
