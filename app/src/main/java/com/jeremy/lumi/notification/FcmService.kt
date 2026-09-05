package com.jeremy.lumi.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.dao.ChatDao
import com.jeremy.lumi.data.local.entity.ChatMessageEntity
import com.jeremy.lumi.data.local.entity.ChatMessageType
import com.jeremy.lumi.domain.model.CareAction
import com.jeremy.lumi.domain.repository.PartnerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receptor de notificaciones push del Modo Pareja.
 *
 * Los mensajes los envía la Cloud Function `onPartnerLinkUpdated` (ver
 * carpeta functions/) cuando:
 *  - la otra persona envía un Detalle  → type = CARE_ACTION
 *  - el periodo se retrasa de forma significativa → type = PERIOD_LATE
 *
 * El cliente nunca envía pushes directamente: hacerlo requeriría incrustar
 * credenciales de servidor en el APK.
 */
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FcmService"
        private const val CHANNEL_ID = "partner_channel"

        private const val TYPE_CARE_ACTION = "CARE_ACTION"
        private const val TYPE_PERIOD_LATE = "PERIOD_LATE"
    }

    @Inject
    lateinit var chatDao: ChatDao

    @Inject
    lateinit var partnerRepository: PartnerRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // El token cambió: hay que refrescarlo en todos los vínculos para que
        // la Cloud Function siga pudiendo avisar a este dispositivo.
        serviceScope.launch {
            try {
                partnerRepository.refreshMyFcmToken()
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo refrescar el token FCM en los vínculos: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        when (message.data["type"]) {
            TYPE_CARE_ACTION -> handleCareAction(message.data)
            TYPE_PERIOD_LATE -> handlePeriodLate(message.data)
        }

        // Notificaciones "display" genéricas (p. ej. campañas desde la consola)
        message.notification?.let {
            showNotification(it.title ?: getString(R.string.app_name), it.body ?: "")
        }
    }

    private fun handleCareAction(data: Map<String, String>) {
        val action = data["action"]
            ?.let { runCatching { CareAction.valueOf(it) }.getOrNull() }
            ?: CareAction.HUG
        val senderName = data["senderName"]?.takeIf { it.isNotBlank() }
            ?: getString(R.string.pm_notif_sender_fallback)

        val body = getString(
            when (action) {
                CareAction.HUG -> R.string.pm_notif_care_hug
                CareAction.TEA -> R.string.pm_notif_care_tea
                CareAction.COFFEE -> R.string.pm_notif_care_coffee
                CareAction.CHOCOLATE -> R.string.pm_notif_care_chocolate
                CareAction.HEATING_PAD -> R.string.pm_notif_care_heating_pad
                CareAction.PHARMACY -> R.string.pm_notif_care_pharmacy
            },
            senderName
        )

        showNotification(getString(R.string.pm_notif_care_title), body)
        saveToChat(body)
    }

    private fun handlePeriodLate(data: Map<String, String>) {
        val name = data["name"]?.takeIf { it.isNotBlank() }
            ?: getString(R.string.pm_notif_sender_fallback)
        val delayDays = data["delayDays"]?.toIntOrNull() ?: 0
        if (delayDays <= 0) return

        showNotification(
            getString(R.string.pm_notif_late_title),
            getString(R.string.pm_notif_late_body, name, delayDays)
        )
    }

    private fun saveToChat(text: String) {
        serviceScope.launch {
            try {
                chatDao.insertMessage(
                    ChatMessageEntity(
                        text = text,
                        messageType = ChatMessageType.GREETING,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo guardar el mensaje en el chat: ${e.message}")
            }
        }
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.pm_notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.pm_notif_channel_desc)
        }
        manager.createNotificationChannel(channel)

        val openAppIntent = android.content.Intent(this, com.jeremy.lumi.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            openAppIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
