package com.fatmaemran.blueprints.catalog.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import static org.mockito.Mockito.mock;

class CacheErrorHandlerTest {

    @Test
    void cacheReadAndWriteFailuresDoNotEscapeToTheCaller() {
        Cache cache = mock(Cache.class);
        CacheErrorHandler handler = new CacheConfiguration().errorHandler();
        RuntimeException redisUnavailable = new IllegalStateException("Redis is unavailable");

        handler.handleCacheGetError(redisUnavailable, cache, "product-id");
        handler.handleCachePutError(redisUnavailable, cache, "product-id", new Object());
    }
}
