package com.jeremy.lumi.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para registrar los gestos afectivos enviados y recibidos.
 */
@Entity(tableName = "bonds_gestures")
data class BondsGestureEntity(
    @PrimaryKey
    val gestureId: String,
    val linkId: String,
    val recipientName: String,
    val actionName: String,
    val message: String?,
    val timestampMs: Long = System.currentTimeMillis()
)
