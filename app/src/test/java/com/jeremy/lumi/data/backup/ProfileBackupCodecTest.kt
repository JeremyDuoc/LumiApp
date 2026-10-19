package com.jeremy.lumi.data.backup

import org.junit.Assert.*
import org.junit.Test

class ProfileBackupCodecTest {
    private fun sample() = ProfileBackup(
        userName = "Luna",
        isRegular = false,
        userGoal = "TRACK_CYCLE",
        defaultCycleLength = 32,
        defaultPeriodLength = 6,
        isPregnant = false,
        isOnContraceptive = true,
        userAge = 25,
        userHeightCm = 165f,
        userWeightKg = 63f,
        secondaryGoals = listOf("MANAGE_CRAMPS")
    )

    @Test fun nicknameAndCyclePreferencesSurviveRoundTrip() {
        val restored = ProfileBackupCodec.fromJson(ProfileBackupCodec.toJson(sample()))
        assertEquals("Luna", restored.userName)
        assertEquals(32, restored.defaultCycleLength)
        assertEquals(6, restored.defaultPeriodLength)
        assertEquals("TRACK_CYCLE", restored.userGoal)
        assertEquals(true, restored.isOnContraceptive)
        assertEquals(listOf("MANAGE_CRAMPS"), restored.secondaryGoals)
    }

    @Test fun emptyNicknameRemainsOptional() {
        val restored = ProfileBackupCodec.fromJson(
            ProfileBackupCodec.toJson(sample().copy(userName = null))
        )
        assertNull(restored.userName)
    }

    @Test fun invalidValuesAreRejectedBeforeRestoringData() {
        assertThrows(CorruptBackupException::class.java) {
            ProfileBackupCodec.validate(sample().copy(defaultCycleLength = 700))
        }
        assertThrows(CorruptBackupException::class.java) {
            ProfileBackupCodec.validate(sample().copy(userName = "x".repeat(120)))
        }
        assertThrows(CorruptBackupException::class.java) {
            ProfileBackupCodec.validate(sample().copy(userWeightKg = Float.NaN))
        }
        assertThrows(CorruptBackupException::class.java) {
            ProfileBackupCodec.validate(sample().copy(secondaryGoals = listOf("UNKNOWN_GOAL")))
        }
        assertThrows(CorruptBackupException::class.java) {
            ProfileBackupCodec.fromJson("""{"formatVersion":1,"userName":"malformed"}""")
        }
    }

    @Test fun newerProfileVersionRequiresNewerApp() {
        assertThrows(BackupVersionTooNewException::class.java) {
            ProfileBackupCodec.validate(sample().copy(formatVersion = 99))
        }
    }

    @Test fun noPasswordsKeysOrPartnerConsentsInProfileFormat() {
        val json = ProfileBackupCodec.toJson(sample())
        assertFalse(json.contains("wrappedKey"))
        assertFalse(json.contains("sharePhase"))
        assertFalse(json.contains("fcmToken"))
        assertFalse(json.contains("accessToken"))
        assertFalse(json.contains("googleAccount"))
        assertFalse(json.contains("healthConnect"))
    }
}
