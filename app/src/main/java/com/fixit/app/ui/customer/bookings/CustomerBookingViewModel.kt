package com.fixit.app.ui.customer.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.booking.BookingRepository
import com.fixit.app.data.location.LocationRepository
import com.fixit.app.data.payment.PaymentRepository
import com.fixit.app.data.promo.PromoRepository
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.domain.model.ActivePromo
import com.fixit.app.domain.model.ProviderDetail
import com.fixit.app.domain.model.SavedLocation
import com.fixit.app.domain.model.SavedPaymentMethod
import com.fixit.app.domain.model.Service
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.net.URLDecoder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * One selectable hour in the time row. [time] is the slot's start; [enabled]
 * is false for slots that have already passed today (kept visible but greyed,
 * matching the mock's disabled-pill look).
 */
data class TimeSlot(
    val time: LocalTime,
    val enabled: Boolean,
) {
    /** "1:00 PM" — 12-hour, no leading zero on the hour, no minutes shown beyond :MM. */
    val label: String
        get() {
            val h12 = ((time.hour + 11) % 12) + 1
            val ampm = if (time.hour < 12) "AM" else "PM"
            return if (time.minute == 0) "$h12:00 $ampm"
            else "%d:%02d %s".format(h12, time.minute, ampm)
        }
}

/** One pickable date in the horizontally-scrolling date row. */
data class DateOption(
    val date: LocalDate,
    /** True when the provider has at least one availability window that weekday. */
    val available: Boolean,
)

data class CustomerBookingState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,

    val detail: ProviderDetail? = null,

    /** serviceId → quantity, seeded from the cart passed in via nav. */
    val quantities: Map<String, Int> = emptyMap(),

    /** The visible date strip (today forward); unavailable weekdays are disabled. */
    val dates: List<DateOption> = emptyList(),
    val selectedDate: LocalDate? = null,

    /** Hourly slots for [selectedDate], already filtered for past + end-inclusive. */
    val slots: List<TimeSlot> = emptyList(),
    val selectedTime: LocalTime? = null,

    /** Saved locations; the selected one provides the booking's coordinates. */
    val locations: List<SavedLocation> = emptyList(),
    val selectedLocationId: String? = null,

    val notes: String = "",

    /** Active promos available to apply, plus the one the customer chose. */
    val promos: List<ActivePromo> = emptyList(),
    val appliedPromo: ActivePromo? = null,
    val promoInput: String = "",
    val promoError: String? = null,

    val paymentMethods: List<SavedPaymentMethod> = emptyList(),
    val selectedMethodId: String? = null,

    /** Server-authoritative subtotal (no promo). Falls back to client calc until loaded. */
    val serverSubtotal: BigDecimal? = null,

    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    /** Set once the booking is created; the screen navigates away on this. */
    val createdBookingId: String? = null,
) {
    val selectedServices: List<Pair<Service, Int>>
        get() {
            val byId = detail?.services?.associateBy { it.id } ?: return emptyList()
            return quantities.mapNotNull { (id, qty) ->
                val s = byId[id] ?: return@mapNotNull null
                if (qty > 0) s to qty else null
            }
        }

    /** Client subtotal — instant feedback before/if the server preview lands. */
    val clientSubtotal: BigDecimal
        get() = selectedServices.fold(BigDecimal.ZERO) { acc, (s, qty) ->
            acc + s.price.multiply(BigDecimal(qty))
        }

    val subtotal: BigDecimal
        get() = serverSubtotal ?: clientSubtotal

    /** Promo discount preview — % of subtotal. Server recomputes authoritatively. */
    val discount: BigDecimal
        get() {
            val promo = appliedPromo ?: return BigDecimal.ZERO
            return subtotal
                .multiply(promo.discountPercentage)
                .divide(BigDecimal(100))
        }

    val total: BigDecimal
        get() = (subtotal - discount).max(BigDecimal.ZERO)

    val selectedMethodApiValue: String?
        get() = paymentMethods.firstOrNull { it.id == selectedMethodId }
            ?.let { if (it.type == com.fixit.app.domain.model.PaymentMethodType.BANK) "bank" else "card" }

    /** Everything required before "Confirm & pay" can fire. */
    val canConfirm: Boolean
        get() = !isSubmitting &&
                selectedServices.isNotEmpty() &&
                selectedDate != null &&
                selectedTime != null &&
                selectedLocationId != null &&
                selectedMethodId != null
}

@HiltViewModel
class CustomerBookingViewModel @Inject constructor(
    private val providerRepo: ProviderRepository,
    private val bookingRepo: BookingRepository,
    private val locationRepo: LocationRepository,
    private val promoRepo: PromoRepository,
    private val paymentRepo: PaymentRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val providerId: String =
        checkNotNull(savedState["providerId"]) { "providerId nav arg missing" }

    /** Parsed from the "serviceId:qty,serviceId:qty" nav arg. */
    private val initialCart: Map<String, Int> = run {
        val raw = savedState.get<String>("items").orEmpty()
        val decoded = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
        decoded.split(",")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size != 2) return@mapNotNull null
                val id = parts[0].trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val qty = parts[1].trim().toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
                id to qty
            }
            .toMap()
    }

    private val _state = MutableStateFlow(
        CustomerBookingState(quantities = initialCart),
    )
    val state = _state.asStateFlow()

    /** How many days forward the horizontal date strip runs. */
    private val dateWindowDays = 30

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { loadAll() }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't start booking",
                    )
                }
                .onSuccess { _state.value = _state.value.copy(isLoading = false) }
        }
    }

    private suspend fun loadAll() = coroutineScope {
        val detailDeferred   = async { providerRepo.byId(providerId) }
        val locationsDeferred = async { runCatching { locationRepo.list() }.getOrDefault(emptyList()) }
        val promosDeferred   = async { runCatching { promoRepo.active() }.getOrDefault(emptyList()) }
        val methodsDeferred  = async { runCatching { paymentRepo.listMethods() }.getOrDefault(emptyList()) }

        val detail    = detailDeferred.await()
        val locations = locationsDeferred.await()
        val promos    = promosDeferred.await()
        val methods   = methodsDeferred.await()

        // Build the date strip and pick the first available date as default.
        val dates = buildDateStrip(detail)
        val firstAvailable = dates.firstOrNull { it.available }?.date

        val defaultLocationId =
            (locations.firstOrNull { it.isDefault } ?: locations.firstOrNull())?.id
        val defaultMethodId =
            (methods.firstOrNull { it.isDefault } ?: methods.firstOrNull())?.id

        _state.value = _state.value.copy(
            detail = detail,
            dates = dates,
            selectedDate = firstAvailable,
            slots = firstAvailable?.let { slotsFor(detail, it) } ?: emptyList(),
            selectedTime = null,
            locations = locations,
            selectedLocationId = defaultLocationId,
            promos = promos,
            paymentMethods = methods,
            selectedMethodId = defaultMethodId,
        )

        // Fire the server price-preview in the background; it only refines the
        // subtotal label, so a failure leaves the client calc in place.
        refreshServerSubtotal()
    }

    // ── Date / time ──────────────────────────────────────────────────────

    /**
     * Today forward [dateWindowDays] days. A date is `available` only if the
     * provider has at least one availability window for that weekday — disabled
     * dates render greyed and aren't selectable (per spec).
     */
    private fun buildDateStrip(detail: ProviderDetail): List<DateOption> {
        val availableDays: Set<DayOfWeek> = detail.availability
            .mapNotNull { parseDayOfWeek(it.dayOfWeek) }
            .toSet()
        val today = LocalDate.now()
        return (0 until dateWindowDays).map { offset ->
            val date = today.plusDays(offset.toLong())
            DateOption(date = date, available = date.dayOfWeek in availableDays)
        }
    }

    /**
     * Hourly slots for [date], generated from the provider's weekly window(s)
     * for that weekday:
     *   • hour-aligned starts only (open_time rounded UP to the next whole hour),
     *   • inclusive of the close-time hour (backend allows scheduled == close_time),
     *   • past hours dropped only when [date] is today.
     * Slots from multiple windows on the same weekday are merged + de-duped.
     */
    private fun slotsFor(detail: ProviderDetail, date: LocalDate): List<TimeSlot> {
        val windows = detail.availability.filter {
            parseDayOfWeek(it.dayOfWeek) == date.dayOfWeek
        }
        if (windows.isEmpty()) return emptyList()

        val isToday = date == LocalDate.now()
        val now = LocalTime.now()

        val hours = sortedSetOf<Int>()
        for (w in windows) {
            val open = parseTime(w.openTime) ?: continue
            val close = parseTime(w.closeTime) ?: continue
            // First whole hour at or after open_time.
            var h = if (open.minute == 0) open.hour else open.hour + 1
            // Inclusive of the close hour (e.g. close 18:00 → last slot 6:00 PM).
            val lastHour = close.hour
            while (h <= lastHour && h <= 23) {
                hours.add(h)
                h++
            }
        }

        return hours.map { hour ->
            val t = LocalTime.of(hour, 0)
            val enabled = !(isToday && !t.isAfter(now))
            TimeSlot(time = t, enabled = enabled)
        }
    }
    // ── Quantity ──────────────────────────────────────────────────────────

    /**
     * Bump a service's quantity by one. Used by the editable service-list
     * column on the booking screen. Re-fires the server price-preview so the
     * subtotal label stays server-authoritative as the cart changes.
     */
    fun increment(serviceId: String) {
        val current = _state.value.quantities[serviceId] ?: 0
        val next = current + 1
        _state.value = _state.value.copy(
            quantities = _state.value.quantities + (serviceId to next),
        )
        refreshServerSubtotal()
    }

    /**
     * Drop a service's quantity by one, floored at its [Service.minQuantity]
     * (matching the provider-detail stepper). Going below the floor removes the
     * service from the cart entirely. Re-fires the server price-preview.
     */
    fun decrement(serviceId: String) {
        val current = _state.value.quantities[serviceId] ?: 0
        if (current <= 0) return

        val minQty = _state.value.detail?.services
            ?.firstOrNull { it.id == serviceId }?.minQuantity ?: 1
        val next = current - 1

        val newQuantities = if (next < minQty) {
            // Below the service's minimum → remove it from the cart.
            _state.value.quantities - serviceId
        } else {
            _state.value.quantities + (serviceId to next)
        }
        _state.value = _state.value.copy(quantities = newQuantities)
        refreshServerSubtotal()
    }
    fun selectDate(date: LocalDate) {
        val detail = _state.value.detail ?: return
        val option = _state.value.dates.firstOrNull { it.date == date } ?: return
        if (!option.available) return
        _state.value = _state.value.copy(
            selectedDate = date,
            slots = slotsFor(detail, date),
            selectedTime = null,           // force a fresh time pick for the new day
        )
    }

    fun selectTime(time: LocalTime) {
        val slot = _state.value.slots.firstOrNull { it.time == time } ?: return
        if (!slot.enabled) return
        _state.value = _state.value.copy(selectedTime = time)
    }

    // ── Location / notes / payment ────────────────────────────────────────

    fun selectLocation(id: String) {
        _state.value = _state.value.copy(selectedLocationId = id)
    }

    fun updateNotes(text: String) {
        _state.value = _state.value.copy(notes = text)
    }

    fun selectMethod(id: String) {
        _state.value = _state.value.copy(selectedMethodId = id)
    }

    // ── Promo ─────────────────────────────────────────────────────────────

    fun updatePromoInput(text: String) {
        _state.value = _state.value.copy(promoInput = text, promoError = null)
    }

    /** Match the typed code against the active-promo list (case-insensitive). */
    fun applyPromo() {
        val code = _state.value.promoInput.trim()
        if (code.isEmpty()) return
        val match = _state.value.promos.firstOrNull { it.code.equals(code, ignoreCase = true) }
        if (match == null) {
            _state.value = _state.value.copy(
                appliedPromo = null,
                promoError = "That code isn't valid right now",
            )
        } else {
            _state.value = _state.value.copy(
                appliedPromo = match,
                promoInput = match.code,
                promoError = null,
            )
        }
    }

    fun clearPromo() {
        _state.value = _state.value.copy(
            appliedPromo = null,
            promoInput = "",
            promoError = null,
        )
    }

    // ── Subtotal ──────────────────────────────────────────────────────────

    private fun refreshServerSubtotal() {
        val cart = _state.value.selectedServices.map { (s, qty) -> s.id to qty }
        if (cart.isEmpty()) return
        viewModelScope.launch {
            runCatching { bookingRepo.pricePreview(cart) }
                .onSuccess { preview ->
                    _state.value = _state.value.copy(
                        serverSubtotal = BigDecimal.valueOf(preview.subtotal),
                    )
                }
            // On failure we keep the client subtotal — no user-facing error.
        }
    }

    // ── Confirm ───────────────────────────────────────────────────────────

    fun confirm() {
        val s = _state.value
        if (!s.canConfirm) return
        val date = s.selectedDate ?: return
        val time = s.selectedTime ?: return
        val method = s.selectedMethodApiValue ?: return
        val cart = s.selectedServices.map { (svc, qty) -> svc.id to qty }

        // ISO local datetime, NO zone suffix — the backend reads weekday +
        // wall-clock time straight off this to validate availability.
        val scheduledAt = LocalDateTime.of(date, time).withSecond(0).withNano(0).toString()

        _state.value = s.copy(isSubmitting = true, submitError = null)
        viewModelScope.launch {
            runCatching {
                bookingRepo.createBooking(
                    items = cart,
                    scheduledAt = scheduledAt,
                    method = method,
                    savedLocationId = s.selectedLocationId,
                    notes = s.notes,
                    promoCode = s.appliedPromo?.code,
                )
            }.onSuccess { booking ->
                _state.value = _state.value.copy(
                    isSubmitting = false,
                    createdBookingId = booking.id,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isSubmitting = false,
                    submitError = e.message ?: "Couldn't complete booking",
                )
            }
        }
    }

    fun consumeSubmitError() {
        _state.value = _state.value.copy(submitError = null)
    }

    // ── Parsing helpers ───────────────────────────────────────────────────

    /** Backend day_of_week is a lowercase English weekday name. */
    private fun parseDayOfWeek(day: String?): DayOfWeek? = when (day?.lowercase()) {
        "monday"    -> DayOfWeek.MONDAY
        "tuesday"   -> DayOfWeek.TUESDAY
        "wednesday" -> DayOfWeek.WEDNESDAY
        "thursday"  -> DayOfWeek.THURSDAY
        "friday"    -> DayOfWeek.FRIDAY
        "saturday"  -> DayOfWeek.SATURDAY
        "sunday"    -> DayOfWeek.SUNDAY
        else        -> null
    }

    /** Backend serialises time as "HH:MM:SS" (sometimes "HH:MM"). */
    private fun parseTime(raw: String?): LocalTime? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        if (h !in 0..23 || m !in 0..59) return null
        return LocalTime.of(h, m)
    }
}