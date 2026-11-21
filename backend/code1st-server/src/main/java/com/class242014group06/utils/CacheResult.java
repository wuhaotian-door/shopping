package com.class242014group06.utils;

/**
 * 缓存查询结果的三种状态，用来区分「没命中」和「命中了一个空值」。
 *
 * 为什么需要它：缓存穿透防护会把"数据库里确实没有"也写进 Redis（空值标记）。
 * 如果 get() 统一返回 null，调用方就分不清这两种情况，
 * 会把"数据库中没有"误判成"缓存没命中"，于是又去查一次库，防护就失效了。
 */
public class CacheResult<T> {

    private final boolean hit;
    private final T value;

    private CacheResult(boolean hit, T value) {
        this.hit = hit;
        this.value = value;
    }

    /** 缓存未命中（key 不存在，或 Redis 不可用降级） */
    public static <T> CacheResult<T> miss() {
        return new CacheResult<>(false, null);
    }

    /** 命中真实数据 */
    public static <T> CacheResult<T> hit(T value) {
        return new CacheResult<>(true, value);
    }

    /** 命中的是空值标记，表示"查过，数据库里没有" */
    public static <T> CacheResult<T> hitNull() {
        return new CacheResult<>(true, null);
    }

    public boolean isHit() {
        return hit;
    }

    public T getValue() {
        return value;
    }
}
