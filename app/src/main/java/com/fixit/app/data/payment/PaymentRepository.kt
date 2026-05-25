package com.fixit.app.data.payment

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(private val api: PaymentApi) {
    suspend fun confirmCompletion(bookingId: String): ConfirmationResponse =
        api.confirmCompletion(bookingId)
}