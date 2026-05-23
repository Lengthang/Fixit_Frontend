package com.fixit.app.ui.signup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fixit.app.ui.components.FixItScreen
import com.fixit.app.ui.components.FixItTextField
import com.fixit.app.ui.components.PrimaryButton
import com.fixit.app.ui.components.Progress
import com.fixit.app.ui.components.ScreenTitle
import com.fixit.app.ui.components.TopBar

@Composable
fun AboutYouScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    draftVm: SignupDraftViewModel,
    viewModel: AboutYouViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AboutYouEffect.Submitted -> {
                    draftVm.update {
                        it.copy(
                            name = state.name,
                            email = state.email,
                            dob = state.dateOfBirth,
                            referral = state.referralCode,
                        )
                    }
                    onContinue()
                }
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    FixItScreen {
        TopBar(step = 3, total = 11, onBack = onBack)
        Progress(step = 3, total = 11)
        ScreenTitle(
            "Tell us about yourself",
            "We'll personalize your experience based on your details.",
        )

        FixItTextField(
            label = "Full name",
            value = state.name,
            onValueChange = viewModel::onNameChange,
            placeholder = "Your name",
            enabled = !state.isSubmitting,
        )
        FixItTextField(
            label = "Email address",
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            placeholder = "you@example.com",
            keyboardType = KeyboardType.Email,
            enabled = !state.isSubmitting,
        )
        FixItTextField(
            label = "Date of birth",
            value = state.dateOfBirth,
            onValueChange = viewModel::onDobChange,
            placeholder = "MM / DD / YYYY",
            keyboardType = KeyboardType.Number,
            enabled = !state.isSubmitting,
        )
        FixItTextField(
            label = "Referral code (optional)",
            value = state.referralCode,
            onValueChange = viewModel::onReferralChange,
            placeholder = "Enter code",
            enabled = !state.isSubmitting,
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.imePadding())
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        PrimaryButton(
            text = if (state.isSubmitting) "Saving…" else "Continue",
            enabled = !state.isSubmitting,
            onClick = { viewModel.submit() },
        )
    }
}