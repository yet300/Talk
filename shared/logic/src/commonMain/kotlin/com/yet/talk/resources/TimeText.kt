package com.yet.talk.resources

import dev.icerock.moko.resources.desc.Raw
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** A post's time as the design shows it: `now`, `5m`, `2h`, `3d`, then `Oct 28` (with the year once it differs). */
fun Instant.toRelativeDesc(now: Instant = Clock.System.now(), zone: TimeZone = TimeZone.currentSystemDefault()): StringDesc {
    val age = now - this
    return when {
        age < 1.minutes -> StringDesc.Resource(MR.strings.time_now)
        age < 1.hours -> StringDesc.ResourceFormatted(MR.strings.time_minutes, age.inWholeMinutes.toInt())
        age < 1.days -> StringDesc.ResourceFormatted(MR.strings.time_hours, age.inWholeHours.toInt())
        age < 7.days -> StringDesc.ResourceFormatted(MR.strings.time_days, age.inWholeDays.toInt())
        else -> StringDesc.Raw(toDateText(now, zone))
    }
}

/** `Oct 28`, or `Oct 28, 2025` outside the current year. */
fun Instant.toDateText(now: Instant = Clock.System.now(), zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val date = toLocalDateTime(zone).date
    val sameYear = date.year == now.toLocalDateTime(zone).date.year
    return kotlinx.datetime.LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        day(padding = kotlinx.datetime.format.Padding.NONE)
        if (!sameYear) {
            chars(", ")
            year()
        }
    }.format(date)
}
