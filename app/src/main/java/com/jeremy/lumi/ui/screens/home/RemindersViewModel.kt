package com.jeremy.lumi.ui.screens.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremy.lumi.data.local.entity.ReminderEntity
import com.jeremy.lumi.data.local.entity.ReminderType
import com.jeremy.lumi.data.local.entity.SupplyType
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.domain.usecase.CyclePrediction
import com.jeremy.lumi.reminders.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val application: Application,
    private val repository: LumiRepository
) : ViewModel() {

    val activeReminders: StateFlow<List<ReminderEntity>> = repository.getActiveReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Recordatorio "normal" (tipos con intervalo predeterminado de fábrica, incluido SUPPLY_REMINDER). */
    fun createReminder(
        type            : ReminderType,
        hourOfDay       : Int,
        minute          : Int,
        methodStartDate : Long? = null,
        label           : String? = null,
        supplyType      : SupplyType? = null
    ) {
        viewModelScope.launch {
            val nextTrigger = if (methodStartDate != null && type.requiresStartDate()) {
                val baseDate = java.time.Instant.ofEpochMilli(methodStartDate)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                val targetDate = baseDate.plusDays(type.defaultRepeatIntervalDays() ?: 0L)
                AlarmScheduler.computeNextTrigger(type, hourOfDay, minute, targetDate)
            } else if (type.isAutomatic() && type != ReminderType.LOG_DAILY) {
                // PERIOD_SOON, OVULATION_SOON y SUPPLY_REMINDER dependen del ciclo.
                // Los mandamos al "futuro lejano" al crearse. En cuanto el ciclo
                // se calcule (HomeViewModel / Calendario), syncCycleReminders los
                // traerá de vuelta a la fecha exacta que corresponde (ej. -3 días).
                Long.MAX_VALUE
            } else {
                // LOG_DAILY y PILL_DAILY (que no tienen methodStartDate)
                AlarmScheduler.computeNextTrigger(type, hourOfDay, minute)
            }

            val reminder = ReminderEntity(
                type            = type,
                hourOfDay       = hourOfDay,
                minute          = minute,
                methodStartDate = methodStartDate,
                label           = label,
                supplyType      = supplyType,
                isCustomDate    = false,
                isActive        = true,
                nextTriggerAt   = nextTrigger
            )
            val newId = repository.saveReminder(reminder)
            AlarmScheduler.schedule(application, reminder.copy(id = newId.toInt()))
        }
    }

    /**
     * Recordatorio con fecha y hora 100% elegidas por la usuaria.
     * Ej: "cambiar el parche en 3 meses" en vez del intervalo sugerido de 3 semanas.
     */
    fun createCustomReminder(
        customDate     : LocalDate,
        hourOfDay      : Int,
        minute         : Int,
        repeatEveryDays: Long?,
        label          : String?,
        type           : ReminderType = ReminderType.CUSTOM
    ) {
        viewModelScope.launch {
            val triggerMillis = AlarmScheduler.epochMillisAt(customDate, hourOfDay, minute)

            val reminder = ReminderEntity(
                type             = type,
                hourOfDay        = hourOfDay,
                minute           = minute,
                label            = label,
                isCustomDate     = true,
                customRepeatDays = repeatEveryDays,
                isActive         = true,
                nextTriggerAt    = triggerMillis
            )
            val newId = repository.saveReminder(reminder)
            AlarmScheduler.schedule(application, reminder.copy(id = newId.toInt()))
        }
    }

    fun cancelReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deactivateReminder(reminder.id)
            AlarmScheduler.cancel(application, reminder.id)
        }
    }

    /**
     * Sincroniza los recordatorios automáticos de ciclo con la predicción más
     * reciente. PERIOD_SOON y OVULATION_SOON se reprograman 2 días y 1 día
     * antes respectivamente; SUPPLY_REMINDER se reprograma 3 días antes para
     * dar tiempo de prepararse incluso antes del aviso de regla próxima.
     */
    fun syncCycleReminders(prediction: CyclePrediction) {
        viewModelScope.launch {
            val current = activeReminders.value

            current.find { it.type == ReminderType.PERIOD_SOON }?.let { reminder ->
                val triggerMillis = AlarmScheduler.epochMillisAt(
                    prediction.nextPeriodDate.minusDays(2), reminder.hourOfDay, reminder.minute
                )
                repository.updateNextTrigger(reminder.id, triggerMillis)
                AlarmScheduler.schedule(application, reminder.copy(nextTriggerAt = triggerMillis))
            }

            current.find { it.type == ReminderType.OVULATION_SOON }?.let { reminder ->
                val triggerMillis = AlarmScheduler.epochMillisAt(
                    prediction.nextOvulationDate.minusDays(1), reminder.hourOfDay, reminder.minute
                )
                repository.updateNextTrigger(reminder.id, triggerMillis)
                AlarmScheduler.schedule(application, reminder.copy(nextTriggerAt = triggerMillis))
            }

            current.find { it.type == ReminderType.SUPPLY_REMINDER }?.let { reminder ->
                val triggerMillis = AlarmScheduler.epochMillisAt(
                    prediction.nextPeriodDate.minusDays(3), reminder.hourOfDay, reminder.minute
                )
                repository.updateNextTrigger(reminder.id, triggerMillis)
                AlarmScheduler.schedule(application, reminder.copy(nextTriggerAt = triggerMillis))
            }
        }
    }
}