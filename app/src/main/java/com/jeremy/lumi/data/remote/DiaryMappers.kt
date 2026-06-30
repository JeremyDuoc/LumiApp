package com.jeremy.lumi.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.jeremy.lumi.domain.model.CyclePhase
import com.jeremy.lumi.ui.screens.partner.DiaryEntry

fun DocumentSnapshot.toDiaryEntry(): DiaryEntry? {
    return try {
        DiaryEntry(
            id          = id,
            authorUid   = getString("authorUid")   ?: return null,
            authorName  = getString("authorName")  ?: return null,
            text        = getString("text")        ?: return null,
            phase       = runCatching {
                            CyclePhase.valueOf(getString("phase") ?: "UNKNOWN")
                          }.getOrDefault(CyclePhase.UNKNOWN),
            timestampMs = getLong("timestampMs")   ?: return null
        )
    } catch (e: Exception) {
        null
    }
}

fun DiaryEntry.toMap(): Map<String, Any> = mapOf(
    "authorUid"   to authorUid,
    "authorName"  to authorName,
    "text"        to text,
    "phase"       to phase.name,
    "timestampMs" to timestampMs
)
