package com.jeremy.lumi.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.domain.model.CycleInsights
import com.jeremy.lumi.domain.repository.InsightsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val isLoading : Boolean       = true,
    val insights  : CycleInsights? = null
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insightsRepo: InsightsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val insights = insightsRepo.getInsights()
            // Si no hay datos suficientes (0 ciclos cerrados) devolvemos null
            val result = if (insights.historicalStats == null &&
                insights.symptomCorrelations.isEmpty() &&
                insights.moodDistribution?.totalDays == 0) null
            else insights
            _uiState.update { it.copy(isLoading = false, insights = result) }
        }
    }
}
