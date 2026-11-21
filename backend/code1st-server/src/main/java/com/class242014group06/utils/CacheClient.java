package com.class242014group06.utils;

import com.class242014group06.json.JacksonObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/** Redis operations for versioned cache reads and rebuilds. */
@Slf4j
@Component
public class CacheClient {

    /** 空值标记：用来做缓存穿透防护，表示"这个 key 查过，数据库里确实没有" */
    public static final String NULL_MARKER = "__NULL__";

    /** 熔断时长：Redis 失败后 10 秒内不再尝试，直接走数据库 */
    private static final long CIRCUIT_OPEN_MILLIS = 10_000L;

    private static final int MAX_CACHE_VALUE_BYTES = 1024 * 1024;

    private static final DefaultRedisScript<String> GET_OR_CREATE_VERSION = new DefaultRedisScript<>("""
            local version = redis.call('GET', KEYS[1])
            if version then return version end
            redis.call('SET', KEYS[1], ARGV[1])
            return ARGV[1]
            """, String.class);

    private static final DefaultRedisScript<Long> SET_WHILE_LEASED = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[2]) ~= ARGV[1] or redis.call('GET', KEYS[3]) ~= ARGV[2] then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[3], 'EX', ARGV[4])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_LEASE = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 复用项目已有的 ObjectMapper，保证 LocalDate/LocalDateTime 的序列化格式和接口返回完全一致 */
    private final ObjectMapper objectMapper = new JacksonObjectMapper();

    /** 熔断截止时间戳（毫秒）。当前时间小于它时，视为 Redis 不可用，直接短路 */
    private volatile long circuitOpenUntil = 0L;

    /** 读单个对象 */
    public <T> CacheResult<T> get(String key, Class<T> type) {
        if (isCircuitOpen()) {
            return CacheResult.miss();
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            return parse(json, () -> objectMapper.readValue(json, type));
        } catch (Exception e) {
            onRedisFailure("get", key, e);
            return CacheResult.miss();
        }
    }

    /** 读列表（泛型需要 TypeReference，否则拿不到元素类型） */
    public <T> CacheResult<List<T>> getList(String key, TypeReference<List<T>> type) {
        if (isCircuitOpen()) {
            return CacheResult.miss();
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            return parse(json, () -> objectMapper.readValue(json, type));
        } catch (Exception e) {
            onRedisFailure("getList", key, e);
            return CacheResult.miss();
        }
    }

    private <T> CacheResult<T> parse(String json, JsonSupplier<T> supplier) throws Exception {
        if (json == null) {
            return CacheResult.miss();
        }
        if (NULL_MARKER.equals(json)) {
            return CacheResult.hitNull();
        }
        return CacheResult.hit(supplier.get());
    }

    @FunctionalInterface
    private interface JsonSupplier<T> {
        T get() throws Exception;
    }

    /** 读取当前版本，或在当前版本已失效时原子创建一个新版本。 */
    public String getOrCreateVersion(String versionKey) {
        if (isCircuitOpen()) {
            return null;
        }
        try {
            String version = stringRedisTemplate.opsForValue().get(versionKey);
            if (version != null) {
                return version;
            }
            return stringRedisTemplate.execute(GET_OR_CREATE_VERSION, List.of(versionKey), UUID.randomUUID().toString());
        } catch (RuntimeException e) {
            onRedisFailure("getOrCreateVersion", versionKey, e);
            return null;
        }
    }

    /** Redis 不可用时返回 false，调用方可以在恢复后重试失效。 */
    public boolean invalidateVersion(String versionKey) {
        if (isCircuitOpen()) {
            return false;
        }
        try {
            stringRedisTemplate.delete(versionKey);
            return true;
        } catch (RuntimeException e) {
            onRedisFailure("invalidateVersion", versionKey, e);
            return false;
        }
    }

    public enum LeaseStatus {
        ACQUIRED, BUSY, UNAVAILABLE
    }

    public LeaseStatus tryAcquireLease(String key, String token, long ttlSeconds) {
        if (isCircuitOpen()) {
            return LeaseStatus.UNAVAILABLE;
        }
        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, token, ttlSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(acquired) ? LeaseStatus.ACQUIRED : LeaseStatus.BUSY;
        } catch (RuntimeException e) {
            onRedisFailure("tryAcquireLease", key, e);
            return LeaseStatus.UNAVAILABLE;
        }
    }

    /** 重建锁和缓存版本都仍有效时，才允许回填。 */
    public void setWhileLeased(String key, String lockKey, String token,
                              String versionKey, String version, Object value, long ttlSeconds) {
        if (isCircuitOpen()) {
            return;
        }
        try {
            String json = value == null ? NULL_MARKER : objectMapper.writeValueAsString(value);
            if (json.getBytes(StandardCharsets.UTF_8).length <= MAX_CACHE_VALUE_BYTES) {
                stringRedisTemplate.execute(SET_WHILE_LEASED, List.of(key, lockKey, versionKey),
                        token, version, json, Long.toString(withJitter(ttlSeconds)));
            }
        } catch (JsonProcessingException e) {
            log.warn("缓存序列化失败 | key={}", key, e);
        } catch (RuntimeException e) {
            onRedisFailure("setWhileLeased", key, e);
        }
    }

    public void releaseLease(String key, String token) {
        if (isCircuitOpen()) {
            return;
        }
        try {
            stringRedisTemplate.execute(RELEASE_LEASE, List.of(key), token);
        } catch (RuntimeException e) {
            onRedisFailure("releaseLease", key, e);
        }
    }

    private boolean isCircuitOpen() {
        return System.currentTimeMillis() < circuitOpenUntil;
    }

    /**
     * 记录一次 Redis 失败并打开熔断开关。
     * 只在"从正常进入熔断"的那一刻打日志，避免每个请求刷屏。
     */
    private void onRedisFailure(String op, String key, Exception e) {
        boolean entering = !isCircuitOpen();
        circuitOpenUntil = System.currentTimeMillis() + CIRCUIT_OPEN_MILLIS;
        if (entering) {
            log.warn("Redis 不可用，已熔断 {}ms，期间所有缓存操作直接跳过并走数据库 | op={} | key={} | cause={}",
                    CIRCUIT_OPEN_MILLIS, op, key, e.toString());
        }
    }

    /** 给 TTL 加 0~10% 的随机抖动，避免同一批 key 同时过期 */
    private long withJitter(long ttlSeconds) {
        long base = Math.max(ttlSeconds, 1L);
        long jitter = Math.max(1L, base / 10L);
        return base + ThreadLocalRandom.current().nextLong(jitter + 1L);
    }
}
