package com.fixit.app.data.dispute

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire representation of DisputeOut returned by the backend.
 * Mirrors the Pydantic schema exactly — no client-side business logic here.
 */
@JsonClass(generateAdapter = true)
data class DisputeResponse(
    val id: String,
    @Json(name = "booking_id")   val bookingId: String,
    @Json(name = "raised_by")    val raisedBy: String,
    val reason: String,
    @Json(name = "reason_image_urls")             val reasonImageUrls: List<String> = emptyList(),
    @Json(name = "provider_response")             val providerResponse: String? = null,
    @Json(name = "provider_response_image_urls")  val providerResponseImageUrls: List<String> = emptyList(),
    @Json(name = "provider_responded_at")         val providerRespondedAt: String? = null,
    val status: String,                           // "open" | "resolved"
    val resolution: String? = null,               // "release" | "refund" | "partial"
    @Json(name = "resolution_note")               val resolutionNote: String? = null,
    @Json(name = "provider_payout")               val providerPayout: Double? = null,
    @Json(name = "customer_refund")               val customerRefund: Double? = null,
    @Json(name = "platform_commission")           val platformCommission: Double? = null,
    @Json(name = "resolved_by")                   val resolvedBy: String? = null,
    @Json(name = "resolved_at")                   val resolvedAt: String? = null,
    @Json(name = "created_at")                    val createdAt: String,
)

/**
 * Restricted view returned to the customer — no provider_response, no commission
 * breakdown, no provider_payout. Mirrors CustomerDisputeOut on the server.
 */
@JsonClass(generateAdapter = true)
data class CustomerDisputeResponse(
    val id: String,
    @Json(name = "booking_id") val bookingId: String,
    @Json(name = "raised_by")  val raisedBy: String,
    val reason: String,
    @Json(name = "reason_image_urls") val reasonImageUrls: List<String> = emptyList(),
    val status: String,
    val resolution: String? = null,
    @Json(name = "resolution_note") val resolutionNote: String? = null,
    @Json(name = "customer_refund") val customerRefund: Double? = null,
    @Json(name = "resolved_by") val resolvedBy: String? = null,
    @Json(name = "resolved_at") val resolvedAt: String? = null,
    @Json(name = "created_at")  val createdAt: String,
)

/** Body for POST /disputes/ */
@JsonClass(generateAdapter = true)
data class DisputeCreateRequest(
    @Json(name = "booking_id") val bookingId: String,
    val reason: String,
    @Json(name = "reason_image_urls") val reasonImageUrls: List<String> = emptyList(),
)

/** Body for PATCH /disputes/{id}/respond */
@JsonClass(generateAdapter = true)
data class DisputeRespondRequest(
    val response: String,
    @Json(name = "response_image_urls") val responseImageUrls: List<String> = emptyList(),
)