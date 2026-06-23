package com.jeremy.lumi.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.dao.ChatDao
import com.jeremy.lumi.data.local.entity.ChatMessageEntity
import com.jeremy.lumi.data.local.entity.ChatMessageType
import com.jeremy.lumi.data.local.entity.ReminderEntity
import com.jeremy.lumi.data.local.entity.ReminderType
import com.jeremy.lumi.data.local.entity.SupplyType
import com.jeremy.lumi.domain.repository.LumiRepository
import com.jeremy.lumi.notifications.LumiNotificationChannel
import com.jeremy.lumi.notifications.NotificationChannels
import com.jeremy.lumi.reminders.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: LumiRepository
    @Inject lateinit var chatDao: ChatDao

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = AlarmScheduler.extractReminderId(intent)
        if (reminderId == -1) return

        CoroutineScope(Dispatchers.IO).launch {
            val reminder = repository.getReminderById(reminderId) ?: return@launch
            if (!reminder.isActive) return@launch

            // ── Las dos salidas nacen del mismo evento ──────────────────────────
            // Resolvemos los strings UNA vez aquí (con Context) y se los pasamos
            // a ambas funciones, para no repetir el when() de recursos dos veces.
            val (titleRes, descRes) = notificationStringsFor(reminder.type)
            val title = reminder.label ?: context.getString(titleRes)
            val body  = context.getString(descRes)

            showNotification(context, reminder, title, body)
            insertChatMessage(context, reminder, body)

            // ── Reprogramación ────────────────────────────────────────────────
            when {
                reminder.isCustomDate && reminder.customRepeatDays != null -> {
                    val next = reminder.nextTriggerAt +
                            (reminder.customRepeatDays * 24L * 60L * 60L * 1000L)
                    repository.updateNextTrigger(reminder.id, next)
                    AlarmScheduler.schedule(context, reminder.copy(nextTriggerAt = next))
                }
                reminder.isCustomDate -> {
                    repository.deactivateReminder(reminder.id)
                }
                reminder.type.defaultRepeatIntervalDays() != null -> {
                    val next = AlarmScheduler.computeFollowingTrigger(
                        reminder.type, reminder.nextTriggerAt
                    )
                    repository.updateNextTrigger(reminder.id, next)
                    AlarmScheduler.schedule(context, reminder.copy(nextTriggerAt = next))
                }
                // PERIOD_SOON / OVULATION_SOON / SUPPLY_REMINDER: se reprograman
                // desde RemindersViewModel.syncCycleReminders(), no aquí.
            }
        }
    }

    /** Un único lugar que mapea tipo → par de recursos de string. */
    private fun notificationStringsFor(type: ReminderType): Pair<Int, Int> = when (type) {
        ReminderType.PILL_DAILY            ->
            R.string.notif_pill_title        to R.string.notif_pill_desc
        ReminderType.PATCH_WEEKLY          ->
            R.string.notif_patch_title       to R.string.notif_patch_desc
        ReminderType.RING_MONTHLY          ->
            R.string.notif_ring_title        to R.string.notif_ring_desc
        ReminderType.INJECTION_MONTHLY,
        ReminderType.INJECTION_QUARTERLY   ->
            R.string.notif_injection_title   to R.string.notif_injection_desc
        ReminderType.PERIOD_SOON           ->
            R.string.notif_period_soon_title to R.string.notif_period_soon_desc
        ReminderType.OVULATION_SOON        ->
            R.string.notif_ovulation_title   to R.string.notif_ovulation_desc
        ReminderType.LOG_DAILY             ->
            R.string.notif_log_title         to R.string.notif_log_desc
        ReminderType.SUPPLY_REMINDER       ->
            R.string.notif_supply_title      to R.string.notif_supply_desc
        ReminderType.CUSTOM                ->
            R.string.notif_custom_title      to R.string.notif_custom_desc
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NOTIFICACIÓN DEL SISTEMA
    // ─────────────────────────────────────────────────────────────────────────
    private fun showNotification(
        context  : Context,
        reminder : ReminderEntity,
        title    : String,
        body     : String
    ) {
        NotificationChannels.ensureAllChannelsExist(context)
        val channel = LumiNotificationChannel.forType(reminder.type)

        val notification = NotificationCompat.Builder(context, channel.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            // ── Versión discreta para pantalla bloqueada ──────────────────────
            // Cuando el teléfono está bloqueado, Android muestra esta versión
            // en lugar del contenido real — protege recordatorios privados.
            .setPublicVersion(
                NotificationCompat.Builder(context, channel.channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notif_private_title))
                    .setContentText(context.getString(R.string.notif_private_body))
                    .build()
            )
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        NotificationManagerCompat.from(context).notify(reminder.type.ordinal, notification)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MENSAJE EN EL CHAT
    //  Reutiliza el mismo "body" de la notificación como base, salvo para
    //  SUPPLY_REMINDER, donde el texto depende del SupplyType elegido y por
    //  eso necesita su propio recurso de string parametrizado.
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun insertChatMessage(context: Context, reminder: ReminderEntity, defaultBody: String) {
        val text = if (reminder.type == ReminderType.SUPPLY_REMINDER) {
            context.getString(supplyChatTextRes(reminder.supplyType))
        } else {
            reminder.label ?: defaultBody
        }

        chatDao.insertMessage(
            ChatMessageEntity(
                text        = text,
                messageType = chatMessageTypeFor(reminder.type),
                reminderId  = reminder.id,
                isRead      = false
            )
        )
    }

    private fun chatMessageTypeFor(type: ReminderType): ChatMessageType = when (type) {
        ReminderType.PERIOD_SOON          -> ChatMessageType.PERIOD_SOON
        ReminderType.OVULATION_SOON       -> ChatMessageType.OVULATION_SOON
        ReminderType.SUPPLY_REMINDER      -> ChatMessageType.SUPPLY_REMINDER
        ReminderType.LOG_DAILY            -> ChatMessageType.LOG_DAILY
        ReminderType.CUSTOM               -> ChatMessageType.CUSTOM
        ReminderType.PILL_DAILY,
        ReminderType.PATCH_WEEKLY,
        ReminderType.RING_MONTHLY,
        ReminderType.INJECTION_MONTHLY,
        ReminderType.INJECTION_QUARTERLY  -> ChatMessageType.METHOD_REMINDER
    }

    /** Texto del chat para SUPPLY_REMINDER, personalizado según el método elegido. */
    private fun supplyChatTextRes(supplyType: SupplyType?): Int = when (supplyType) {
        SupplyType.TAMPON            -> R.string.chat_supply_tampon
        SupplyType.PAD               -> R.string.chat_supply_pad
        SupplyType.MENSTRUAL_CUP     -> R.string.chat_supply_cup
        SupplyType.PERIOD_UNDERWEAR  -> R.string.chat_supply_underwear
        SupplyType.OTHER, null       -> R.string.chat_supply_other
    }
}