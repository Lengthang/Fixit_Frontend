package com.fixit.app.data.booking

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface BookingApi {
    /** GET /bookings/provider — every booking assigned to the signed-in provider. */
    @GET("bookings/provider")
    suspend fun providerBookings(): List<BookingResponse>

    /** GET /bookings/{id} — single booking detail (includes embedded photos). */
    @GET("bookings/{id}")
    suspend fun byId(@Path("id") id: String): BookingResponse

    /** PATCH /bookings/{id}/status — moves the booking through its state machine. */
    @PATCH("bookings/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body body: BookingStatusUpdate,
    ): BookingResponse

    /**
     * GET /bookings/{id}/payout — server-computed earnings breakdown.
     * Provider-only; admins can also call this. Returns 403 otherwise.
     */
    @GET("bookings/{id}/payout")
    suspend fun payoutFor(@Path("id") id: String): BookingPayoutResponse

    /**
     * POST /bookings/{id}/photos — attach a before/after photo. The image
     * itself must already be uploaded via POST /uploads/image; this call
     * just records the URL and kind. Server enforces:
     *  - caller must be the assigned provider
     *  - booking status must be in_progress or awaiting_confirmation
     */
    @POST("bookings/{id}/photos")
    suspend fun addPhoto(
        @Path("id") id: String,
        @Body body: BookingPhotoCreate,
    ): BookingPhotoResponse

    /** GET /bookings/{id}/photos — list a booking's photos. */
    @GET("bookings/{id}/photos")
    suspend fun listPhotos(@Path("id") id: String): List<BookingPhotoResponse>

    /** DELETE /bookings/{id}/photos/{photoId} — remove a photo (provider-only). */
    @DELETE("bookings/{id}/photos/{photoId}")
    suspend fun deletePhoto(
        @Path("id") id: String,
        @Path("photoId") photoId: String,
    )
}