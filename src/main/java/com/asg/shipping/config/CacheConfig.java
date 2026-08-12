package com.asg.shipping.config;

import com.asg.shipping.common.cache.MasterDataCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * In-memory caches for the reference tables that LOV enrichment reads on every document fetch
 * (ports, commodities, container types, IMCO/OOG types, charge masters, currencies, taxes).
 *
 * Only master data belongs here. None of these tables are group/company scoped, so a single
 * process-wide entry per poid/code is correct for every tenant; transactional lookups
 * (quotations, this BL's own containers, receipt invoices) are deliberately left uncached.
 *
 * The cached values populate display-only {@code *Det} fields — never an amount that is
 * computed or persisted — so a stale entry can at worst show an out-of-date label for up to
 * {@link #MASTER_DATA_TTL}, and never changes what the service saves.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Master data is edited rarely and through other screens, so this trades a bounded staleness
     * window for dropping the repeated per-request reads. Shorten it if a maintenance screen needs
     * its edits reflected sooner.
     */
    static final Duration MASTER_DATA_TTL = Duration.ofMinutes(30);

    private static final long MAX_ENTRIES = 10_000L;

    @Bean
    public CacheManager cacheManager() {
        List<String> cacheNames = List.of(
                MasterDataCache.PORTS,
                MasterDataCache.COMMODITIES,
                MasterDataCache.CONTAINER_TYPES,
                MasterDataCache.IMCO_CLASSES,
                MasterDataCache.OOG_TYPES,
                MasterDataCache.CHARGE_MASTERS,
                MasterDataCache.CURRENCIES,
                MasterDataCache.TAXES);

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(cacheNames.stream()
                .map(name -> new CaffeineCache(name, Caffeine.newBuilder()
                        .expireAfterWrite(MASTER_DATA_TTL)
                        .maximumSize(MAX_ENTRIES)
                        .build()))
                .map(org.springframework.cache.Cache.class::cast)
                .toList());
        cacheManager.afterPropertiesSet();

        // Every master-data write runs inside a transaction, so defer evictions until it commits.
        // Without this the eviction can land before the commit does, letting a concurrent reader
        // re-populate the cache from the pre-commit row and pin the stale value for a full TTL.
        return new TransactionAwareCacheManagerProxy(cacheManager);
    }
}
