package com.fixit.app.data.booking

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(private val api: BookingApi) {
    suspend fun providerBookings(): List<BookingResponse> = api.providerBookings()
}