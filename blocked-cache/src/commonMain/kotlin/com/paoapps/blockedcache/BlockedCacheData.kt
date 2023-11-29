package com.paoapps.blockedcache

import kotlinx.serialization.Serializable

/**
 * Represents the data stored in a BlockedCache, including its creation time.
 * This class is designed to work with the caching mechanism of BlockedCache, providing a way to
 * store both the data and the metadata about the data (like its creation time).
 *
 * @param T The type of data that is being cached.
 * @property data The actual data being cached. It is nullable to allow representing the absence of data.
 * @property creationTime The timestamp (in milliseconds) when the data was created or cached.
 *        This is used to determine data freshness and expiration. It is nullable to represent scenarios
 *        where the creation time is unknown or irrelevant.
 */
@Serializable
data class BlockedCacheData<T>(
    val data: T? = null,
    val creationTime: Long? = null
)
