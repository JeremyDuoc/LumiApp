package com.jeremy.lumi.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jeremy.lumi.R
import com.jeremy.lumi.data.local.dao.ChatDao
import com.jeremy.lumi.data.local.entity.ChatMessageEntity
import com.jeremy.lumi.data.local.entity.ChatMessageType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var chatDao: ChatDao

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Aquí podríamos actualizar el token en Firestore si el PartnerLink está activo
        // Se hará luego desde el ViewModel o Repository.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Manejar mensajes de datos "Client-to-Client"
        if (message.data.isNotEmpty()) {
            val type = message.data["type"]
            when (type) {
                "HUG" -> {
                    showNotification("Abrazo Virtual", "Tu pareja te ha enviado un abrazo virtual ❤️")
                    saveToChat("Has recibido un abrazo virtual de tu pareja ❤️")
                }
                "SUPPLY_REQUEST" -> {
                    val supply = message.data["item"] ?: "toallitas/tampones"
                    showNotification("Misión Lumi", "Tu pareja necesita que compres $supply. ¡Sé su héroe!")
                    saveToChat("Misión de pareja: Comprar $supply.")
                }
                "SYNC_CYCLE" -> {
                    // La usuaria titular actualizó su estado, actualizamos la Room local del observador
                    // parsear datos del snapshot y actualizar...
                }
            }
        }
        
        // Manejar notificación normal si viene en el payload
        message.notification?.let {
            showNotification(it.title ?: "Lumi", it.body ?: "")
        }
    }

    private fun saveToChat(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            chatDao.insertMessage(
                ChatMessageEntity(
                    text = text,
                    messageType = ChatMessageType.GREETING, // O un nuevo tipo PARTNER
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "partner_channel"

        val channel = NotificationChannel(
            channelId,
            "Notificaciones de Pareja",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher) // Asegúrate de tener el icono correcto
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
