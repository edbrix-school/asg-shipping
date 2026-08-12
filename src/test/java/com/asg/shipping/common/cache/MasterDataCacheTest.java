package com.asg.shipping.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasterDataCacheTest {

    private static final String CACHE = MasterDataCache.PORTS;

    private MasterDataCache cache;
    private List<List<Long>> loaderCalls;

    @BeforeEach
    void setUp() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(new CaffeineCache(CACHE, Caffeine.newBuilder().build())));
        cacheManager.afterPropertiesSet();
        cache = new MasterDataCache(cacheManager);
        loaderCalls = new ArrayList<>();
    }

    /** Records what it was asked for, and resolves every key except 99 (the "no such row" key). */
    private Map<Long, String> loader(List<Long> keys) {
        loaderCalls.add(List.copyOf(keys));
        Map<Long, String> loaded = new HashMap<>();
        keys.stream().filter(k -> k != 99L).forEach(k -> loaded.put(k, "value-" + k));
        return loaded;
    }

    @Test
    void loadsEverythingOnColdCache() {
        Map<Long, String> result = cache.getAll(CACHE, List.of(1L, 2L), this::loader);

        assertEquals(Map.of(1L, "value-1", 2L, "value-2"), result);
        assertEquals(List.of(List.of(1L, 2L)), loaderCalls);
    }

    @Test
    void secondCallIsServedEntirelyFromCache() {
        cache.getAll(CACHE, List.of(1L, 2L), this::loader);
        loaderCalls.clear();

        Map<Long, String> result = cache.getAll(CACHE, List.of(1L, 2L), this::loader);

        assertEquals(Map.of(1L, "value-1", 2L, "value-2"), result);
        assertTrue(loaderCalls.isEmpty(), "a fully cached request must not hit the loader");
    }

    @Test
    void loaderIsCalledWithOnlyTheMissingKeys() {
        cache.getAll(CACHE, List.of(1L, 2L), this::loader);
        loaderCalls.clear();

        Map<Long, String> result = cache.getAll(CACHE, List.of(1L, 2L, 3L), this::loader);

        assertEquals(Map.of(1L, "value-1", 2L, "value-2", 3L, "value-3"), result);
        assertEquals(List.of(List.of(3L)), loaderCalls, "cached keys must not be re-fetched");
    }

    @Test
    void unresolvedKeysAreNotRememberedAsAbsent() {
        cache.getAll(CACHE, List.of(99L), this::loader);
        loaderCalls.clear();

        Map<Long, String> result = cache.getAll(CACHE, List.of(99L), this::loader);

        assertFalse(result.containsKey(99L));
        assertEquals(List.of(List.of(99L)), loaderCalls,
                "a key with no master row must be retried so a later insert is picked up");
    }

    @Test
    void deduplicatesAndSkipsNullKeys() {
        Map<Long, String> result = cache.getAll(CACHE, java.util.Arrays.asList(1L, 1L, null), this::loader);

        assertEquals(Map.of(1L, "value-1"), result);
        assertEquals(List.of(List.of(1L)), loaderCalls);
    }

    @Test
    void emptyRequestSkipsTheLoaderEntirely() {
        assertTrue(cache.getAll(CACHE, List.of(), this::loader).isEmpty());
        assertTrue(cache.getAll(CACHE, null, this::loader).isEmpty());
        assertTrue(loaderCalls.isEmpty());
    }

    @Test
    void fallsBackToTheLoaderWhenTheCacheIsNotConfigured() {
        Map<Long, String> result = cache.getAll("noSuchCache", List.of(1L), this::loader);

        assertEquals(Map.of(1L, "value-1"), result);
        assertEquals(List.of(List.of(1L)), loaderCalls);
    }
}
