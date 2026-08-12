package com.asg.shipping.common.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Cache-aside lookup for the batched master-data reads used by LOV enrichment.
 *
 * {@code @Cacheable} is no help here: these loaders take a list of keys, so the cache key would be
 * the whole list and two documents sharing 90% of their ports would still both miss. This resolves
 * each key individually and calls the loader with only the keys that missed, so a cache entry is
 * reused across every document that references it.
 */
@Component
@RequiredArgsConstructor
public class MasterDataCache {

    public static final String PORTS = "shipPorts";
    public static final String COMMODITIES = "shipCommodities";
    public static final String CONTAINER_TYPES = "shipContainerTypes";
    public static final String IMCO_CLASSES = "shipImcoClasses";
    public static final String OOG_TYPES = "shipOogTypes";
    public static final String CHARGE_MASTERS = "shipChargeMasters";
    public static final String CURRENCIES = "globalCurrencies";
    public static final String TAXES = "globalTaxes";

    private final CacheManager cacheManager;

    /**
     * Returns a value for every requested key that could be resolved, from cache where possible and
     * from {@code loader} otherwise. Keys the loader does not return are left uncached rather than
     * remembered as absent, so a master row added later is picked up on the next request instead of
     * waiting out the TTL.
     *
     * @param loader called with the subset of keys that missed; never called with an empty list
     */
    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getAll(String cacheName, Collection<K> keys, Function<List<K>, Map<K, V>> loader) {
        if (keys == null || keys.isEmpty()) {
            return Map.of();
        }
        List<K> distinctKeys = keys.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctKeys.isEmpty()) {
            return Map.of();
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return loader.apply(distinctKeys);
        }

        Map<K, V> resolved = new HashMap<>();
        List<K> misses = new ArrayList<>();
        for (K key : distinctKeys) {
            Cache.ValueWrapper cached = cache.get(key);
            if (cached != null) {
                resolved.put(key, (V) cached.get());
            } else {
                misses.add(key);
            }
        }

        if (!misses.isEmpty()) {
            Map<K, V> loaded = loader.apply(misses);
            if (loaded != null) {
                loaded.forEach((key, value) -> {
                    cache.put(key, value);
                    resolved.put(key, value);
                });
            }
        }
        return resolved;
    }
}
