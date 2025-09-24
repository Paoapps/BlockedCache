@file:OptIn(ExperimentalTime::class)

package com.paoapps.blockedcache.utils

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class DatetimeNowProvider : NowProvider {

    override fun now() = currentInstant().toEpochMilliseconds()

    override fun currentInstant(): Instant = Clock.System.now()
}
