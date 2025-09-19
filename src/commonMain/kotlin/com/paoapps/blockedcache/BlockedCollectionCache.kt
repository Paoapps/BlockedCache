package com.paoapps.blockedcache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex

/**
 * Manages a collection of [BlockedCache] instances, each associated with a unique key.
 * This class provides a way to dynamically create and retrieve [BlockedCache] instances for different keys
 * using a factory function. It ensures that only one cache instance is created per key.
 *
 * @param K The type of the key used to identify each cache.
 * @param T The type of data stored in each cache.
 * @property cacheFactory A factory function to create a new [BlockedCache] instance for a given key.
 */
class BlockedCollectionCache<K, T: Any>(private val cacheFactory: (K) -> BlockedCache<T>) {

    private val caches = MutableStateFlow<Map<K, BlockedCache<T>>>(emptyMap())
    private val mutex = Mutex()

    /**
     * Retrieves or creates a [BlockedCache] for the specified key.
     * If a cache for the key does not exist, it is created using the cacheFactory.
     * This method ensures that only one instance of [BlockedCache] is created per key.
     *
     * @param key The key for which the cache is to be retrieved or created.
     * @return The [BlockedCache] instance associated with the specified key.
     */
    suspend fun cache(key: K): BlockedCache<T> {
        var cache: BlockedCache<T>? = caches.value[key]
        if (cache == null) {
            mutex.lock()
            cache = caches.value[key]
            if (cache == null) {
                cache = cacheFactory.invoke(key)
                caches.value = caches.value.plus(Pair(key, cache))
            }
            mutex.unlock()
        }
        return cache
    }
}
