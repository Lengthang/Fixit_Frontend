package com.fixit.app.data.booking

import retrofit2.http.GET

interface BookingApi {
    /** GET /bookings/provider — every booking assigned to the signed-in provider. */
    @GET("bookings/provider")
    suspend fun providerBookings(): List<BookingResponse>
}