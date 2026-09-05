package com.jeremy.lumi.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.local.entity.CycleEntity
import com.jeremy.lumi.domain.repository.LumiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CycleHistoryViewModel @Inject constructor(
    private val repository: LumiRepository
) : ViewModel() {

    val cycles: StateFlow<List<CycleEntity>> = repository.getAllCycles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateCycle(cycle: CycleEntity) {
        viewModelScope.launch {
            repository.updateCycle(cycle)
        }
    }

    fun deleteCycle(cycleId: Int) {
        viewModelScope.launch {
            repository.deleteCycle(cycleId)
        }
    }
}
