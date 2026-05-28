package com.fixit.app.ui.util

import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.BookingStatus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Singapore-localised currency formatter. Matches what ProviderHomeScreen
 * uses inline; centralised so jobs/earnings/job-detail render the same way.
 */
fun formatMoney(amount: BigDecimal): String {
    val nf = NumberFormat.getCurrencyInstance(Locale("en", "SG"))
    nf.maximumFractionDigits = 2
    return nf.format(amount)
}

/** "10:00 AM" — used by the calendar row and upcoming list. */
fun formatTime(
    instant: Instant,
    tz: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val ldt = instant.toLocalDateTime(tz)
    val h12 = when {
        ldt.hour == 0 -> 12
        ldt.hour > 12 -> ldt.hour - 12
        else          -> ldt.hour
    }
    val ampm = if (ldt.hour < 12) "AM" else "PM"
    return "%d:%02d %s".format(h12, ldt.minute, ampm)
}

/**
 * "Today at 10:00 AM" / "Tomorrow at 3:30 PM" / "Mon, May 5 · 9:00 AM".
 * Used by the jobs list and job-detail screens.
 */
fun formatScheduled(
    instant: Instant,
    tz: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val nowDate    = Clock.System.now().toLocalDateTime(tz).date
    val targetDate = instant.toLocalDateTime(tz).date
    val daysDiff   = targetDate.toEpochDays() - nowDate.toEpochDays()
    val time       = formatTime(instant, tz)

    return when (daysDiff) {
        0L    -> "Today at $time"
        1L    -> "Tomorrow at $time"
        -1L   -> "Yesterday at $time"
        else  -> {
            val day = targetDate.dayOfWeek.name.take(3).lowercase()
                .replaceFirstChar { it.uppercase() }
            val month = targetDate.month.name.take(3).lowercase()
                .replaceFirstChar { it.uppercase() }
            "$day, $month ${targetDate.dayOfMonth} · $time"
        }
    }
}

/**
 * Customer-bookings card meta line. Picks a phrasing per booking status:
 *  - PENDING / IN_PROGRESS    → "Sun, Apr 21 · 1:00 PM"   (scheduled time)
 *  - AWAITING_CONFIRMATION    → "Today · Finished 2:45 PM" (when the provider
 *                                marked done — approximated by scheduledAt
 *                                since the backend doesn't expose the exact
 *                                completion timestamp on BookingResponse)
 *  - COMPLETED                → "Apr 15 · Completed"       (no time, since
 *                                history rows care about the date, not the hour)
 *  - CANCELLED / DISPUTED     → falls back to formatScheduled
 *
 * Kept here rather than inside the screen because the same line is used by
 * both CustomerBookingsScreen (rows) and CustomerBookingDetailScreen (header).
 */
fun formatBookingMeta(
    booking: Booking,
    tz: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val nowDate    = Clock.System.now().toLocalDateTime(tz).date
    val targetDate = booking.scheduledAt.toLocalDateTime(tz).date
    val daysDiff   = targetDate.toEpochDays() - nowDate.toEpochDays()
    val time       = formatTime(booking.scheduledAt, tz)

    val dayLabel: String = when (daysDiff) {
        0L  -> "Today"
        1L  -> "Tomorrow"
        -1L -> "Yesterday"
        else -> {
            val month = targetDate.month.name.take(3).lowercase()
                .replaceFirstChar { it.uppercase() }
            if (targetDate.year == nowDate.year) {
                val dow = targetDate.dayOfWeek.name.take(3).lowercase()
                    .replaceFirstChar { it.uppercase() }
                "$dow, $month ${targetDate.dayOfMonth}"
            } else {
                "$month ${targetDate.dayOfMonth}, ${targetDate.year}"
            }
        }
    }

    return when (booking.status) {
        BookingStatus.AWAITING_CONFIRMATION -> "$dayLabel · Finished $time"
        BookingStatus.COMPLETED             -> "$dayLabel · Completed"
        BookingStatus.CANCELLED             -> "$dayLabel · Cancelled"
        BookingStatus.DISPUTED              -> "$dayLabel · In dispute"
        else                                -> "$dayLabel · $time"
    }
}

/** "Alex Wong" → "AW"; "Alex" → "AL"; null/blank → "?". */
fun initialsFor(name: String?): String {
    if (name.isNullOrBlank()) return "?"
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else            -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

/**
 * Deterministic avatar background based on a seed string (customer id or
 * name). Returns the 0xAARRGGBB Long the Compose Color(Long) overload wants.
 */
fun avatarColorFor(seed: String?): Long {
    val key = seed?.takeIf { it.isNotBlank() } ?: return AVATAR_PALETTE[0]
    val idx = ((key.hashCode().toLong() and 0x7FFFFFFFL) % AVATAR_PALETTE.size).toInt()
    return AVATAR_PALETTE[idx]
}

private val AVATAR_PALETTE = longArrayOf(
    0xFF2563EB, // blue
    0xFFF97316, // orange
    0xFF10B981, // emerald
    0xFF8B5CF6, // violet
    0xFFEC4899, // pink
    0xFF06B6D4, // cyan
    0xFFEAB308, // amber
)