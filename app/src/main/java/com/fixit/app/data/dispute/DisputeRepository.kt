package com.fixit.app.data.dispute

import com.fixit.app.data.booking.BookingApi
import com.fixit.app.domain.model.Dispute
import com.fixit.app.domain.model.DisputeStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Singleton
class DisputeRepository @Inject constructor(
    private val disputeApi: DisputeApi,
    private val bookingApi: BookingApi,
) {
    /**
     * Customer raises a dispute against an awaiting_confirmation booking.
     * The server enforces:
     *  - caller must be the customer on the booking
     *  - booking.status must be "awaiting_confirmation"
     *  - no existing dispute on this booking
     * Returns the dispute id so the caller can navigate to a detail screen
     * later if needed.
     */
    suspend fun raiseDispute(
        bookingId: String,
        reason: String,
        imageUrls: List<String> = emptyList(),
    ): String {
        val response = disputeApi.raiseDispute(
            DisputeCreateRequest(
                bookingId = bookingId,
                reason = reason,
                reasonImageUrls = imageUrls,
            )
        )
        return response.id
    }

    /**
     * Fetches all disputes for the signed-in provider, then enriches each one
     * with booking context (customer name, service title, scheduled time) via
     * parallel GET /bookings/{id} calls. Individual booking failures are
     * swallowed so a single bad booking id never blanks the whole list.
     */
    suspend fun providerDisputes(): List<Dispute> = coroutineScope {
        val responses = disputeApi.providerDisputes()
        responses
            .map { dto ->
                async {
                    val booking = runCatching { bookingApi.byId(dto.bookingId) }.getOrNull()
                    dto.toDomain(booking)
                }
            }
            .map { it.await() }
    }

    /** Single dispute, enriched with its booking. */
    suspend fun byId(disputeId: String): Dispute = coroutineScope {
        val dtoDeferred     = async { disputeApi.byId(disputeId) }
        val dto             = dtoDeferred.await()
        val booking         = runCatching { bookingApi.byId(dto.bookingId) }.getOrNull()
        dto.toDomain(booking)
    }

    /** Submit the provider's response to a dispute. Returns the updated dispute. */
    suspend fun respond(disputeId: String, responseText: String, imageUrls: List<String>): Dispute {
        val dto     = disputeApi.respond(disputeId, DisputeRespondRequest(responseText, imageUrls))
        val booking = runCatching { bookingApi.byId(dto.bookingId) }.getOrNull()
        return dto.toDomain(booking)
    }

    // ── Mapping ───────────────────────────────────────────────────────────

    private fun DisputeResponse.toDomain(
        booking: com.fixit.app.data.booking.BookingResponse?,
    ): Dispute = Dispute(
        id                        = id,
        bookingId                 = bookingId,
        raisedBy                  = raisedBy,
        reason                    = reason,
        reasonImageUrls           = reasonImageUrls,
        providerResponse          = providerResponse,
        providerResponseImageUrls = providerResponseImageUrls,
        providerRespondedAt       = parseInstantSafe(providerRespondedAt),
        disputeStatus             = DisputeStatus.from(status, providerResponse),
        resolution                = resolution,
        resolutionNote            = resolutionNote,
        providerPayout            = providerPayout?.let { BigDecimal.valueOf(it) },
        customerRefund            = customerRefund?.let { BigDecimal.valueOf(it) },
        platformCommission        = platformCommission?.let { BigDecimal.valueOf(it) },
        resolvedAt                = parseInstantSafe(resolvedAt),
        createdAt                 = parseInstantSafe(createdAt) ?: Clock.System.now(),
        // Booking-enriched fields
        customerName    = booking?.customer?.name,
        customerPhotoUrl = booking?.customer?.profilePhotoUrl,
        serviceName     = booking?.items?.firstOrNull()?.title,
        scheduledAt     = booking?.scheduledAt?.let { parseInstantSafe(it) },
        totalAmount     = booking?.totalAmount?.let { BigDecimal.valueOf(it) },
    )

    private fun parseInstantSafe(iso: String?): Instant? =
        iso?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
}