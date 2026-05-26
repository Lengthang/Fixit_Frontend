package com.fixit.app.domain.model

import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * UI-friendly dispute shape. Enriched at the repository layer with booking
 * context (service name, customer name, scheduled time) so screens never
 * need to fetch the booking separately.
 */
data class Dispute(
    val id: String,
    val bookingId: String,
    val raisedBy: String,
    val reason: String,
    val reasonImageUrls: List<String>,
    val providerResponse: String?,
    val providerResponseImageUrls: List<String>,
    val providerRespondedAt: Instant?,
    val disputeStatus: DisputeStatus,
    val resolution: String?,
    val resolutionNote: String?,
    val providerPayout: BigDecimal?,
    val customerRefund: BigDecimal?,
    val platformCommission: BigDecimal?,
    val resolvedAt: Instant?,
    val createdAt: Instant,
    // Enriched from booking fetch
    val customerName: String?,
    val customerPhotoUrl: String?,
    val serviceName: String?,
    val scheduledAt: Instant?,
    val totalAmount: BigDecimal?,
)

/**
 * Three-state machine derived from the backend's two raw fields
 * (status + provider_response). The backend only knows "open" or "resolved";
 * we derive the finer-grained state here so screens never recalculate it.
 *
 *   open  + response == null  →  PENDING_RESPONSE  (provider must act)
 *   open  + response != null  →  AWAITING_REVIEW   (locked, admin reviewing)
 *   resolved                  →  RESOLVED
 */
enum class DisputeStatus {
    PENDING_RESPONSE,
    AWAITING_REVIEW,
    RESOLVED;

    companion object {
        fun from(rawStatus: String, providerResponse: String?): DisputeStatus = when {
            rawStatus == "resolved"       -> RESOLVED
            providerResponse != null      -> AWAITING_REVIEW
            else                          -> PENDING_RESPONSE
        }
    }
}