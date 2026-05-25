package com.fixit.app.ui.provider.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.data.review.ReviewRepository
import com.fixit.app.domain.model.RatingSummary
import com.fixit.app.domain.model.Review
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderReviewsState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val summary: RatingSummary? = null,
    val reviews: List<Review> = emptyList(),
) {
    /** Star rating → count. Always covers 1..5 so the bars draw consistently. */
    val distribution: Map<Int, Int>
        get() = (1..5).associateWith { s -> reviews.count { it.rating == s } }
}

@HiltViewModel
class ProviderReviewsViewModel @Inject constructor(
    private val providerRepo: ProviderRepository,
    private val reviewRepo: ReviewRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderReviewsState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val provider = providerRepo.me()
                coroutineScope {
                    val listDeferred    = async { reviewRepo.forProvider(provider.id) }
                    val summaryDeferred = async { reviewRepo.summaryForProvider(provider.id) }
                    listDeferred.await() to summaryDeferred.await()
                }
            }
                .onSuccess { (list, summary) ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        reviews   = list,
                        summary   = summary,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Couldn't load reviews",
                    )
                }
        }
    }
}