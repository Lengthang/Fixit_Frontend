package com.fixit.app.ui.signup.provider

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ScheduleState(
    /** Bit set across mon..sun (size 7). Defaults Mon–Fri on. */
    val activeDays: List<Boolean> = listOf(true, true, true, true, true, false, false),
    val openTime: String = "08:00",   // 24h "HH:MM"
    val closeTime: String = "18:00",
)

@HiltViewModel
class ScheduleViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ScheduleState())
    val state = _state.asStateFlow()

    fun toggleDay(index: Int) {
        val list = _state.value.activeDays.toMutableList()
        list[index] = !list[index]
        _state.value = _state.value.copy(activeDays = list)
    }

    fun setOpen(time: String)  { _state.value = _state.value.copy(openTime = time) }
    fun setClose(time: String) { _state.value = _state.value.copy(closeTime = time) }
}