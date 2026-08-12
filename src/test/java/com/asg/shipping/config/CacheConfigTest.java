package com.asg.shipping.config;

import com.asg.shipping.common.cache.MasterDataCache;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CacheConfigTest {

    private final CacheManager cacheManager = new CacheConfig().cacheManager();

    @Test
    void definesACacheForEveryMasterDataLookup() {
        List<String> expected = List.of(
                MasterDataCache.PORTS,
                MasterDataCache.COMMODITIES,
                MasterDataCache.CONTAINER_TYPES,
                MasterDataCache.IMCO_CLASSES,
                MasterDataCache.OOG_TYPES,
                MasterDataCache.CHARGE_MASTERS,
                MasterDataCache.CURRENCIES,
                MasterDataCache.TAXES);

        expected.forEach(name -> assertNotNull(cacheManager.getCache(name), () -> "missing cache: " + name));
        assertEquals(expected.size(), cacheManager.getCacheNames().size(),
                "only the master-data caches should be declared");
    }

    /** Evictions must be deferred to commit, otherwise a concurrent read can pin a stale value. */
    @Test
    void cacheManagerIsTransactionAware() {
        assertInstanceOf(TransactionAwareCacheManagerProxy.class, cacheManager);
    }

    @Test
    void evictRemovesTheEntryWhenNoTransactionIsActive() {
        Cache cache = cacheManager.getCache(MasterDataCache.PORTS);

        cache.put(1L, "value-1");
        assertNotNull(cache.get(1L));

        cache.evict(1L);
        assertNull(cache.get(1L));
    }
}
