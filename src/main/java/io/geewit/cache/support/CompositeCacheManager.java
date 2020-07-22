package io.geewit.cache.support;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * Composite {@link CacheManager} implementation that iterates over
 * a given collection of delegate {@link CacheManager} instances.
 *
 * <p>Allows {@link NoOpCacheManager} to be automatically added to the end of
 * the list for handling cache declarations without a backing store. Otherwise,
 * any custom {@link CacheManager} may play that role of the last delegate as
 * well, lazily creating cache regions for any requested name.
 *
 * <p>Note: Regular CacheManagers that this composite manager delegates to need
 * to return {@code null} from {@link #getCache(String)} if they are unaware of
 * the specified cache name, allowing for iteration to the next delegate in line.
 * However, most {@link CacheManager} implementations fall back to lazy creation
 * of named caches once requested; check out the specific configuration details
 * for a 'static' mode with fixed cache names, if available.
 *
 * @author geewit
 * @author Juergen Hoeller
 * @since 2020-07-22
 * @see #setFallbackToNoOpCache
 * @see org.springframework.cache.concurrent.ConcurrentMapCacheManager#setCacheNames
 */
public class CompositeCacheManager implements CacheManager, InitializingBean {

    private final List<CacheManager> cacheManagers = new ArrayList<>();

    private boolean fallbackToNoOpCache = false;


    /**
     * Construct an empty CompositeCacheManager, with delegate CacheManagers to
     * be added via the {@link #setCacheManagers "cacheManagers"} property.
     */
    public CompositeCacheManager() {
    }

    /**
     * Construct a CompositeCacheManager from the given delegate CacheManagers.
     * @param cacheManagers the CacheManagers to delegate to
     */
    public CompositeCacheManager(CacheManager... cacheManagers) {
        setCacheManagers(Arrays.asList(cacheManagers));
    }


    /**
     * Specify the CacheManagers to delegate to.
     */
    public void setCacheManagers(Collection<CacheManager> cacheManagers) {
        this.cacheManagers.addAll(cacheManagers);
    }

    /**
     * Indicate whether a {@link NoOpCacheManager} should be added at the end of the delegate list.
     * In this case, any {@code getCache} requests not handled by the configured CacheManagers will
     * be automatically handled by the {@link NoOpCacheManager} (and hence never return {@code null}).
     */
    public void setFallbackToNoOpCache(boolean fallbackToNoOpCache) {
        this.fallbackToNoOpCache = fallbackToNoOpCache;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.fallbackToNoOpCache) {
            this.cacheManagers.add(new NoOpCacheManager());
        }
    }

    @Override
    @Nullable
    public CompositeCache getCache(String name) {
        List<Cache> caches = new ArrayList<>();
        for (CacheManager manager : this.cacheManagers) {
            Cache cache = manager.getCache(name);
            if(cache != null) {
                caches.add(manager.getCache(name));
            }
        }
        CompositeCache compositeCache = new CompositeCache(name, caches, false);
        return compositeCache;
    }

    @Override
    public Collection<String> getCacheNames() {
        Set<String> names = new LinkedHashSet<>();
        for (CacheManager manager : this.cacheManagers) {
            names.addAll(manager.getCacheNames());
        }
        return Collections.unmodifiableSet(names);
    }

}