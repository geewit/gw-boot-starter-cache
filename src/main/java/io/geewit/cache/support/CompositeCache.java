package io.geewit.cache.support;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.AbstractValueAdaptingCache;

import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple {@link org.springframework.cache.Cache} implementation based on the
 * core JDK {@code java.util.concurrent} package.
 *
 * <p>Useful for testing or simple caching scenarios, typically in combination
 * with {@link org.springframework.cache.support.SimpleCacheManager} or
 * dynamically through {@link ConcurrentMapCacheManager}.
 *
 * <p><b>Note:</b> As {@link ConcurrentHashMap} (the default implementation used)
 * does not allow for {@code null} values to be stored, this class will replace
 * them with a predefined internal object. This behavior can be changed through the
 * {@link #CompositeCache(String, List, boolean)} constructor.
 *
 * @author geewit
 * @since 202-07-22
 * @see ConcurrentMapCacheManager
 */
public class CompositeCache extends AbstractValueAdaptingCache {

    private final String name;

    private final List<Cache> caches;

    /**
     * Create a new ConcurrentMapCache with the specified name.
     * @param name the name of the cache
     */
    public CompositeCache(String name, List<Cache> caches, boolean allowNullValues) {
        super(allowNullValues);
        this.name = name;
        this.caches = caches;
    }


    @Override
    public Object lookup(Object key) {
        Stack<Cache> noCacheStack = new Stack<>();
        for(Cache cache : this.caches) {
            if(cache == null) {
                continue;
            }
            if(cache instanceof AbstractValueAdaptingCache) {
                Object value = ((AbstractValueAdaptingCache)cache).lookup(key);
                if(value != null) {
                    while (!noCacheStack.isEmpty()) {
                        Cache noCache = noCacheStack.pop();
                        noCache.put(key, value);
                    }
                    return value;
                } else {
                    noCacheStack.push(cache);
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return this.caches.stream().filter(Objects::nonNull).map(cache -> cache.get(key, valueLoader)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public void put(Object key, Object value) {
        if(value != null) {
            this.caches.stream().filter(Objects::nonNull).forEachOrdered(cache -> cache.put(key, value));
        }
    }

    @Override
    public void evict(Object key) {
        this.caches.stream().filter(Objects::nonNull).forEachOrdered(cache -> cache.evict(key));
    }

    @Override
    public void clear() {
        this.caches.stream().filter(Objects::nonNull).forEachOrdered(Cache::clear);
    }
}
