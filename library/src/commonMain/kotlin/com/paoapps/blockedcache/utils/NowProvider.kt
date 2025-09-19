package com.paoapps.blockedcache.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

interface NowProvider {
    fun now(): Long
    fun currentInstant(): Instant
}
