package com.fixit.app.data.payment

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface PaymentApi {

    /** POST /payments/confirm/{booking_id} — provider half of dual confirmation. */
    @POST("payments/confirm/{bookingId}")
    suspend fun confirmCompletion(
        @Path("bookingId") bookingId: String,
    ): ConfirmationResponse

    /** GET /payments/methods — list the current user's saved payout methods. */
    @GET("payments/methods")
    suspend fun listMethods(): List<SavedPaymentMethodDto>

    /** POST /payments/methods — add a new payout method. */
    @POST("payments/methods")
    suspend fun addMethod(
        @Body body: SavedPaymentMethodCreateDto,
    ): SavedPaymentMethodDto

    /**
     * PATCH /payments/methods/{id} — update is_default.
     * Backend enforces a single default per user — all other methods are demoted.
     */
    @PATCH("payments/methods/{methodId}")
    suspend fun setDefault(
        @Path("methodId") methodId: String,
        @Body body: SetDefaultDto,
    ): SavedPaymentMethodDto

    /** DELETE /payments/methods/{id} — remove a saved method (204 No Content). */
    @DELETE("payments/methods/{methodId}")
    suspend fun deleteMethod(
        @Path("methodId") methodId: String,
    )

    /** POST /payments/withdraw — deducts balance and creates a withdrawal request. */
    @POST("payments/withdraw")
    suspend fun withdraw(
        @Body body: WithdrawalCreateDto,
    ): WithdrawalResponseDto
}