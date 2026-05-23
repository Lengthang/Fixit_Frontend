package com.fixit.app.ui.signup.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.category.CategoryRepository
import com.fixit.app.data.category.CategoryResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServicesState(
    val categories: List<CategoryResponse> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val years: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val categoryRepo: CategoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ServicesState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { categoryRepo.list() }
                .onSuccess { _state.value = _state.value.copy(categories = it, loading = false) }
                .onFailure { _state.value = _state.value.copy(
                    error = it.message ?: "Couldn't load categories",
                    loading = false,
                ) }
        }
    }

    fun toggle(id: String) {
        val s = _state.value.selectedIds
        _state.value = _state.value.copy(
            selectedIds = if (id in s) s - id else s + id,
        )
    }

    fun onYearsChange(v: Int) {
        _state.value = _state.value.copy(years = v.coerceIn(0, 25))
    }

    fun dismissError() { _state.value = _state.value.copy(error = null) }
}