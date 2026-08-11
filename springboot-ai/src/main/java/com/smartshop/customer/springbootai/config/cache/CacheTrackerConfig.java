package com.smartshop.customer.springbootai.config.cache;

public class CacheTrackerConfig {

    private static final ThreadLocal<String> CURRENT_CACHE_TYPE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> LLM_CALLED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<String> RESOLVED_SOURCE = new ThreadLocal<>();

    private CacheTrackerConfig() {}

    public static void setCurrentCacheType(String type) {
        CURRENT_CACHE_TYPE.set(type);
        LLM_CALLED.set(false);
        RESOLVED_SOURCE.remove();
    }

    public static void markLlmCalled() {
        LLM_CALLED.set(true);
    }

    public static boolean isLlmCalled() {
        return Boolean.TRUE.equals(LLM_CALLED.get());
    }

    public static String getCurrentCacheType() {
        return CURRENT_CACHE_TYPE.get();
    }

    public static void setResolvedSource(String source) {
        RESOLVED_SOURCE.set(source);
    }

    public static String getResolvedSource() {
        return RESOLVED_SOURCE.get();
    }

    public static void clear() {
        CURRENT_CACHE_TYPE.remove();
        LLM_CALLED.remove();
        RESOLVED_SOURCE.remove();
    }
}
