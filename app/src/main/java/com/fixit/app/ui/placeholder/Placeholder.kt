package com.fixit.app.ui.placeholder

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixit.app.data.auth.AuthRepository
import com.fixit.app.data.customer.CustomerRepository
import com.fixit.app.data.customer.UserResponse
import com.fixit.app.data.local.TokenStorage
import com.fixit.app.data.provider.ProviderRepository
import com.fixit.app.domain.model.UserRole
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.OutlineButton
import com.fixit.app.ui.components.TopBar
import com.fixit.app.ui.theme.C
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaceholderViewModel @Inject constructor(
    private val customerRepo: CustomerRepository,
    private val providerRepo: ProviderRepository,
    private val authRepo: AuthRepository,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    data class Snapshot(
        val name: String? = null,
        val phone: String? = null,
        val providerStatus: String? = null,
    )

    private val _data = MutableStateFlow(Snapshot())
    val data = _data.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { customerRepo.me() }.onSuccess { me ->
                _data.value = _data.value.copy(name = me.name, phone = me.phone)
            }
            if (tokenStorage.roleBlocking() == UserRole.PROVIDER) {
                runCatching { providerRepo.me() }.onSuccess { p ->
                    _data.value = _data.value.copy(providerStatus = p.status)
                }
            }
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch { authRepo.logout(); onDone() }
    }
}

@Composable
fun UserPlaceholderScreen(
    role: UserRole,
    onSignedOut: () -> Unit,
    vm: PlaceholderViewModel = hiltViewModel(),
) {
    val data by vm.data.collectAsState()
    FixItScreen {
        TopBar(showBack = false)
        Column(
            Modifier.weight(1f).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("You're in!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = C.Ink)
            Spacer(Modifier.height(8.dp))
            Text("Signed in as ${data?.name ?: "—"} (${role.api})",
                fontSize = 14.sp, color = C.Slate)
            Spacer(Modifier.height(4.dp))
            Text(data?.phone ?: "", fontSize = 13.sp, color = C.Mute)
        }
        OutlineButton("Sign out") { vm.signOut(onSignedOut) }
    }
}