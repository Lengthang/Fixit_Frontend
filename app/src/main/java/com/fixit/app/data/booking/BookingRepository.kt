package com.fixit.app.data.booking

import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.BookingPayout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(private val api: BookingApi) {

    /** Raw provider bookings (for VMs that compute their own derived data). */
    suspend fun providerBookings(): List<BookingResponse> =
        api.providerBookings()

    /** Same list, mapped to domain — preferred for screens that just need to render. */
    suspend fun providerBookingsAsDomain(): List<Booking> =
        api.providerBookings().map { it.toDomain() }

    /** Single booking by id, mapped to domain. */
    suspend fun byId(id: String): Booking =
        api.byId(id).toDomain()

    /**
     * Send a raw status string so callers can request transitions the
     * BookingStatus enum doesn't model (e.g. "rejected"). VMs use this for
     * accept/decline/cancel/mark-done.
     */
    suspend fun updateStatus(id: String, status: String): Booking =
        api.updateStatus(id, BookingStatusUpdate(status)).toDomain()

    /**
     * Server-computed earnings breakdown for one booking. Always prefer this
     * to client-side calculations — commission rate is owned by the backend.
     */
    suspend fun payoutFor(id: String): BookingPayout =
        api.payoutFor(id).toDomain()
}