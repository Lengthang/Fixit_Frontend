package com.fixit.app.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.local.TokenStorage
import com.fixit.app.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Picks the launch destination once we know whether there's a persisted token. */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val tokenStorage: TokenStorage,
) : ViewModel() {
    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val token = tokenStorage.tokenBlocking()
            _startDestination.value = if (token.isNullOrBlank()) {
                Routes.WELCOME
            } else {
                Routes.home(tokenStorage.roleBlocking())
            }
        }
    }
}