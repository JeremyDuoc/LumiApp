package com.jeremy.lumi.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.preferences.OnboardingPreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel mínimo que expone el estado del onboarding al composable de Splash.
 * Necesario porque el composable no puede leer DataStore directamente.
 *
 * Todos los usuarios (incluidos los observadores) van a MAIN.
 * La lógica de qué muestra el Home según el rol es responsabilidad de HomeViewModel.
 */
@HiltViewModel
class NavDecisionViewModel @Inject constructor(
    prefs: OnboardingPreferenceManager
) : ViewModel() {

    val onboardingDone: StateFlow<Boolean?> = prefs.isOnboardingCompleted
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = null
        )
}
