package com.jeremy.lumi.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para almacenamiento y caché local offline de los vínculos de Bonds.
 */
@Entity(tableName = "bonds_links")
data class BondLinkEntity(
    @PrimaryKey
    val linkId: String,
    val ownerUid: String,
    val partnerUid: String?,
    val linkCode: String,
    val status: String,
    val displayName: String?,
    val avatarId: String?,
    val relationLabel: String,
    val shareCyclePhase: Boolean,
    val shareMood: Boolean,
    val shareSymptoms: Boolean,
    val sharePredictions: Boolean,
    val cachedPhase: String?,
    val cachedCycleDay: Int?,
    val cachedMood: String?,
    val updatedAtMs: Long = System.currentTimeMillis()
)
