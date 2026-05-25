package com.fixit.app.ui.provider.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.domain.model.Booking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class ProviderCalendarState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    private val allBookings: List<Booking> = emptyList(),
) {
    /** Day-of-month integers within [month] that have at least one booking. */
    val bookedDates: Set<Int>
        get() {
            val tz = TimeZone.currentSystemDefault()
            return allBookings
                .map { it.scheduledAt.toLocalDateTime(tz).date }
                .filter { it.year == month.year && it.monthNumber == month.monthValue }
                .map { it.dayOfMonth }
                .toSet()
        }

    /** Bookings on [selectedDate], earliest first. */
    val bookingsForSelectedDate: List<Booking>
        get() {
            val tz = TimeZone.currentSystemDefault()
            return allBookings
                .filter {
                    val d = it.scheduledAt.toLocalDateTime(tz).date
                    d.year == selectedDate.year &&
                            d.monthNumber == selectedDate.monthValue &&
                            d.dayOfMonth == selectedDate.dayOfMonth
                }
                .sortedBy { it.scheduledAt }
        }

    // The "bookings" the screen reads from is allBookings, but the screen only
    // references it via bookedDates / bookingsForSelectedDate which we already
    // expose. The private field keeps the public surface tidy.
}

@HiltViewModel
class ProviderCalendarViewModel @Inject constructor(
    private val bookingRepo: BookingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderCalendarState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun selectDate(date: LocalDate) {
        _state.value = _state.value.copy(
            selectedDate = date,
            month = YearMonth.of(date.year, date.monthValue),
        )
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { bookingRepo.providerBookingsAsDomain() }
                .onSuccess { list ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        allBookings = list,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load calendar",
                    )
                }
        }
    }
}