package com.jeremy.lumi.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoSecurityTest {
    @Test fun encryptedArchiveRoundTrip() {
        val password = "correct long backup password".toCharArray()
        val salt = BackupCrypto.generateSalt()
        val key = BackupCrypto.deriveKey(password, salt)
        try {
            val original = "sensitive health data".toByteArray()
            val ciphertext = BackupCrypto.encrypt(original, key)
            val header = BackupCrypto.readHeader(ciphertext)
            val restoredKey = BackupCrypto.deriveKey(password, header.salt, header.iterations)
            try {
                assertArrayEquals(original, BackupCrypto.decrypt(ciphertext, restoredKey.keyBytes))
            } finally {
                restoredKey.clear()
            }
        } finally {
            key.clear()
            password.fill('\u0000')
        }
    }

    @Test fun wrongPassphraseMustFailAuthentication() {
        val right = BackupCrypto.deriveKey("correct long password".toCharArray(), BackupCrypto.generateSalt())
        val ciphertext = BackupCrypto.encrypt("record".toByteArray(), right)
        val header = BackupCrypto.readHeader(ciphertext)
        val wrong = BackupCrypto.deriveKey("another wrong password".toCharArray(), header.salt, header.iterations)
        try {
            assertThrows(WrongPassphraseException::class.java) {
                BackupCrypto.decrypt(ciphertext, wrong.keyBytes)
            }
        } finally {
            right.clear()
            wrong.clear()
        }
    }

    @Test fun tamperingMustFailAuthentication() {
        val key = BackupCrypto.deriveKey("correct long password".toCharArray(), BackupCrypto.generateSalt())
        try {
            val ciphertext = BackupCrypto.encrypt("secret".toByteArray(), key)
            ciphertext[ciphertext.lastIndex] = (ciphertext.last().toInt() xor 1).toByte()
            assertThrows(WrongPassphraseException::class.java) {
                BackupCrypto.decrypt(ciphertext, key.keyBytes)
            }
        } finally {
            key.clear()
        }
    }

    @Test fun excessiveKdfIterationsAreRejectedBeforeDerivation() {
        val key = BackupCrypto.deriveKey("correct long password".toCharArray(), BackupCrypto.generateSalt())
        try {
            val ciphertext = BackupCrypto.encrypt("secret".toByteArray(), key)
            // Iteration count begins at byte 25 (big endian).
            ciphertext[25] = 0x7F.toByte()
            ciphertext[26] = 0x7F.toByte()
            ciphertext[27] = 0x7F.toByte()
            ciphertext[28] = 0x7F.toByte()
            assertThrows(CorruptBackupException::class.java) {
                BackupCrypto.readHeader(ciphertext)
            }
        } finally {
            key.clear()
        }
    }
}
