package com.fixit.app.data.booking

import com.fixit.app.domain.model.Booking
import com.fixit.app.domain.model.BookingPayout
import com.fixit.app.domain.model.BookingPhoto
import com.fixit.app.domain.model.PhotoKind
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(private val api: BookingApi) {
    // ── Customer ─────────────────────────────────────────────────────────

    /** Raw customer bookings. */
    suspend fun myBookings(): List<BookingResponse> =
        api.myBookings()

    /** Customer bookings mapped to domain — preferred for screens. */
    suspend fun myBookingsAsDomain(): List<Booking> =
        api.myBookings().map { it.toDomain() }

    // ── Provider ─────────────────────────────────────────────────────────
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

    /**
     * Attach a previously-uploaded image URL to the booking as a
     * before/after photo. Caller must have already pushed the bytes via
     * UploadRepository.uploadImage and have the absolute URL in hand.
     *
     * Returns the newly-created photo (mapped to domain) so callers can
     * optimistically append it without re-fetching, though refreshing the
     * full booking afterwards is the simpler pattern most VMs use.
     */
    suspend fun addPhoto(bookingId: String, url: String, kind: PhotoKind): BookingPhoto {
        val response = api.addPhoto(
            id = bookingId,
            body = BookingPhotoCreate(url = url, kind = kind.apiValue),
        )
        // Server just validated `kind`, so toDomainOrNull cannot reasonably
        // return null here — but fall back to a manual construction rather
        // than crashing on the unlikely null path.
        return response.toDomainOrNull() ?: BookingPhoto(
            id         = response.id,
            url        = response.url,
            kind       = kind,
            uploadedAt = kotlin.time.Clock.System.now(),
        )
    }

    /** Remove a photo from a booking. Provider-only (enforced server-side). */
    suspend fun deletePhoto(bookingId: String, photoId: String) {
        api.deletePhoto(bookingId, photoId)
    }

    /**
     * Standalone photo list. Not used on JobDetailScreen (BookingResponse
     * embeds the gallery) but exposed for screens that only need the
     * photos — e.g. a future customer-side completion-review screen.
     */
    suspend fun photosFor(bookingId: String): List<BookingPhoto> =
        api.listPhotos(bookingId).mapNotNull { it.toDomainOrNull() }
}