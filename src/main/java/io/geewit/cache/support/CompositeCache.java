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
                        if(noCache != null) {
                            try {
                                noCache.put(key, value);
                            } catch (Exception ignored) {
                            }
                        }
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
        for (Cache cache : this.caches) {
            if (cache != null) {
                try {
                    T value = cache.get(key, valueLoader);
                    if (value != null) {
                        return value;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    @Override
    public void put(Object key, Object value) {
        if(value != null) {
            this.caches.stream().filter(Objects::nonNull).forEachOrdered(cache -> {
                try {
                    cache.put(key, value);
                } catch (Exception ignored) {
                }
            });
        }
    }

    @Override
    public void evict(Object key) {
        this.caches.stream().filter(Objects::nonNull).forEachOrdered(cache -> {
            try {
                cache.evict(key);
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void clear() {
        this.caches.stream().filter(Objects::nonNull).forEachOrdered(cache -> {
            try {
                cache.clear();
            } catch (Exception ignored) {
            }
        });
    }
}
