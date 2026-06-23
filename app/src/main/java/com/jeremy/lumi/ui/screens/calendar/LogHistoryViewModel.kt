package com.jeremy.lumi.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.local.entity.DailyLogWithSymptoms
import com.jeremy.lumi.domain.repository.LumiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LogHistoryViewModel @Inject constructor(
    private val repository: LumiRepository
) : ViewModel() {

    private val _descending = MutableStateFlow(true)
    val descending: StateFlow<Boolean> = _descending.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val logs: StateFlow<List<DailyLogWithSymptoms>> = _descending
        .flatMapLatest { desc -> repository.getAllLogs(descending = desc) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleOrder() {
        _descending.value = !_descending.value
    }
}