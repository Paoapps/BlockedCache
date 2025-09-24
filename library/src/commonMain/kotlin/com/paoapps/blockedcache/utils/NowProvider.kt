@file:OptIn(ExperimentalTime::class)

package com.paoapps.blockedcache.utils

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface NowProvider {
    fun now(): Long
    fun currentInstant(): Instant
}
