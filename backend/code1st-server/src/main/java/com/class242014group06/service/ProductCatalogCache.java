package com.class242014group06.service;

import com.class242014group06.constant.CacheKey;
import com.class242014group06.exception.ProductCatalogBusyException;
import com.class242014group06.utils.CacheClient;
import com.class242014group06.utils.CacheClient.LeaseStatus;
import com.class242014group06.utils.CacheResult;
import com.class242014group06.vo.ProductVO;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class ProductCatalogCache {
    private static final long LIST_TTL_SECONDS = 300;
    private static final long DETAIL_TTL_SECONDS = 1800;
    private static final long EMPTY_TTL_SECONDS = 60;
    private static final long LEASE_TTL_SECONDS = 15;
    private static final long WAIT_MILLIS = 3000;
    private static final int MAX_CATEGORY_LENGTH = 100;
    private static final TypeReference<List<ProductVO>> PRODUCT_LIST_TYPE = new TypeReference<>() {};

    private final CacheClient cache;
    private final ConcurrentHashMap<String, CompletableFuture<Object>> inFlight = new ConcurrentHashMap<>();
    private final Semaphore databaseSlots;
    private final int loadsPerSecond;
    private double availableLoads;
    private long lastRefill = System.nanoTime();

    private final Object invalidationLock = new Object();
    private volatile boolean invalidationPending;

    public ProductCatalogCache(CacheClient cache,
                               @Value("${code1st.product-cache.max-concurrent-loads:4}") int maxConcurrentLoads,
                               @Value("${code1st.product-cache.loads-per-second:12}") int loadsPerSecond) {
        if (maxConcurrentLoads < 1 || loadsPerSecond < 1) {
            throw new IllegalArgumentException("Product cache database limits must be positive");
        }
        this.cache = cache;
        this.databaseSlots = new Semaphore(maxConcurrentLoads);
        this.loadsPerSecond = loadsPerSecond;
        this.availableLoads = loadsPerSecond;
    }

    public List<ProductVO> listAll(Supplier<List<ProductVO>> loader) {
        return getOrLoad("list:all", LIST_TTL_SECONDS, this::readList, loader);
    }

    public List<ProductVO> listByCategory(String category, Supplier<List<ProductVO>> loader) {
        if (category.length() > MAX_CATEGORY_LENGTH) {
            return loadFromDatabase(loader);
        }
        // Preserve the original MySQL comparison and keep user input out of the key structure.
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(category.getBytes(StandardCharsets.UTF_8));
        return getOrLoad("list:category:" + encoded, LIST_TTL_SECONDS, this::readList, loader);
    }

    public ProductVO getById(Long id, Supplier<ProductVO> loader) {
        return getOrLoad("detail:" + id, DETAIL_TTL_SECONDS, key -> cache.get(key, ProductVO.class), loader);
    }

    public void invalidateAfterCommit() {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidate();
                }
            });
        } else {
            invalidate();
        }
    }

    private void invalidate() {
        synchronized (invalidationLock) {
            invalidationPending = true;
            // Detach old fallback reads as well so callers after the write reload committed data.
            inFlight.clear();
            invalidationPending = !cache.invalidateVersion(CacheKey.PRODUCT_CATALOG_VERSION);
        }
    }

    @Scheduled(fixedDelay = 1000)
    void retryPendingInvalidation() {
        if (invalidationPending) {
            synchronized (invalidationLock) {
                if (invalidationPending) {
                    invalidationPending = !cache.invalidateVersion(CacheKey.PRODUCT_CATALOG_VERSION);
                }
            }
        }
    }

    private String version() {
        retryPendingInvalidation();
        return invalidationPending ? null : cache.getOrCreateVersion(CacheKey.PRODUCT_CATALOG_VERSION);
    }

    private CacheResult<List<ProductVO>> readList(String key) {
        return cache.getList(key, PRODUCT_LIST_TYPE);
    }

    private <T> T getOrLoad(String suffix, long ttl, Function<String, CacheResult<T>> read, Supplier<T> loader) {
        String version = version();
        String key = CacheKey.PRODUCT_CATALOG_PREFIX + (version == null ? "fallback" : version) + ":" + suffix;
        if (version != null) {
            CacheResult<T> cached = read.apply(key);
            if (cached.isHit()) {
                return cached.getValue();
            }
        }

        CompletableFuture<Object> own = new CompletableFuture<>();
        CompletableFuture<Object> current = inFlight.putIfAbsent(key, own);
        if (current != null) {
            return await(current);
        }
        try {
            T value = version == null ? loadFromDatabase(loader) : rebuild(key, version, ttl, read, loader);
            own.complete(value);
            return value;
        } catch (RuntimeException | Error e) {
            own.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key, own);
        }
    }

    @SuppressWarnings("unchecked") // A key suffix always has one result type.
    private <T> T await(CompletableFuture<Object> future) {
        try {
            return (T) future.get(WAIT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProductCatalogBusyException();
        } catch (TimeoutException e) {
            throw new ProductCatalogBusyException();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw (Error) e.getCause();
        }
    }

    private <T> T rebuild(String key, String version, long ttl, Function<String, CacheResult<T>> read, Supplier<T> loader) {
        String lockKey = key + ":rebuild";
        String token = UUID.randomUUID().toString();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(WAIT_MILLIS);
        do {
            CacheResult<T> cached = read.apply(key);
            if (cached.isHit()) {
                return cached.getValue();
            }
            LeaseStatus lease = cache.tryAcquireLease(lockKey, token, LEASE_TTL_SECONDS);
            if (lease == LeaseStatus.UNAVAILABLE) {
                return loadFromDatabase(loader);
            }
            if (lease == LeaseStatus.ACQUIRED) {
                try {
                    cached = read.apply(key);
                    if (cached.isHit()) {
                        return cached.getValue();
                    }
                    T value = loadFromDatabase(loader);
                    long expiry = value == null || (value instanceof List<?> list && list.isEmpty())
                            ? EMPTY_TTL_SECONDS : ttl;
                    cache.setWhileLeased(key, lockKey, token, CacheKey.PRODUCT_CATALOG_VERSION, version, value, expiry);
                    return value;
                } finally {
                    cache.releaseLease(lockKey, token);
                }
            }
            waitForPeer(deadline);
        } while (System.nanoTime() < deadline);
        throw new ProductCatalogBusyException();
    }

    private void waitForPeer(long deadline) {
        long remaining = deadline - System.nanoTime();
        if (remaining > 0) {
            long delay = TimeUnit.MILLISECONDS.toNanos(ThreadLocalRandom.current().nextLong(50, 101));
            try {
                TimeUnit.NANOSECONDS.sleep(Math.min(delay, remaining));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProductCatalogBusyException();
            }
        }
    }

    private <T> T loadFromDatabase(Supplier<T> loader) {
        if (!databaseSlots.tryAcquire()) {
            throw new ProductCatalogBusyException();
        }
        try {
            if (!takeLoadToken()) {
                throw new ProductCatalogBusyException();
            }
            return loader.get();
        } finally {
            databaseSlots.release();
        }
    }

    private synchronized boolean takeLoadToken() {
        long now = System.nanoTime();
        double refill = (now - lastRefill) * (loadsPerSecond / 1_000_000_000.0);
        availableLoads = Math.min(loadsPerSecond, availableLoads + refill);
        lastRefill = now;
        if (availableLoads < 1) {
            return false;
        }
        availableLoads--;
        return true;
    }
}
