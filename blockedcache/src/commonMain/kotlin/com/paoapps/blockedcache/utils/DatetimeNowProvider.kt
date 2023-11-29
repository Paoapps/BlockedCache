package com.paoapps.blockedcache.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class DatetimeNowProvider : NowProvider {

    override fun now() = currentInstant().toEpochMilliseconds()

    override fun currentInstant(): Instant = Clock.System.now()
}
