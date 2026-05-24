package com.fixit.app.domain.model

import java.math.BigDecimal

/** Rolled-up numbers shown in the provider dashboard stats row. */
data class DashboardStats(
    val weekEarnings: BigDecimal = BigDecimal.ZERO,
    val weekDeltaPercent: Int = 0,
    val jobsDoneTotal: Int = 0,
    val jobsDoneThisWeek: Int = 0,
    val ratingAverage: Double = 0.0,
    val ratingReviewCount: Int = 0,
)

/** One row in the provider dashboard's "New requests" list. */
data class JobRequest(
    val id: String,
    val customerName: String,
    val customerInitials: String,
    val avatarColorHex: Long,
    val price: BigDecimal,
    val issue: String,
    val meta: String,
)

/** One row in the provider dashboard's "Upcoming today" list. */
data class UpcomingJob(
    val id: String,
    val time: String,
    val title: String,
    val sub: String,
)