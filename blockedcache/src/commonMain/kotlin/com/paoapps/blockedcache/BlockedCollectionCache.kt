package com.paoapps.blockedcache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex

class BlockedCollectionCache<K, T: Any>(private val cacheFactory: (K) -> BlockedCache<T>) {

    private val caches = MutableStateFlow<Map<K, BlockedCache<T>>>(emptyMap())
    private val mutex = Mutex()

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
