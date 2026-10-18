# Lumi — Local-first security review (18 Oct 2026)

## Scope and constraints
Static review of repository source, not a production penetration test. No production Firestore rules, Cloud Functions, backups or user data were accessed or modified. This branch MUST NOT be deployed/merged without build, instrumented tests, migration checks and a second security review.

## Changes in this branch
1. Removed the **plaintext** manual `Lumi_Backup.zip` export entry from Settings. Existing legacy ZIP import remains available for users who need to recover old archives; imported legacy files are *not encrypted*. Do not promise old exported ZIP files are protected.
2. Added `BackupManager.exportEncryptedDatabaseArchive` and `importEncryptedDatabaseArchive` using existing PBKDF2-SHA256 / AES-256-GCM envelope (`.lbk`). This API is **not yet connected to the UI**. Export uses an explicit password (at least 12 characters) and writes only encrypted bytes to the destination. Import authenticates before replacing Room data. Both bound input sizes and wipe temporary byte buffers where practical.
3. Limited PBKDF2 iterations in backup headers to 1,000,000 to reduce CPU denial-of-service by attacker-modified files.
4. Added crypto regression tests (round-trip, wrong password, tampering, untrusted KDF iteration count). These tests were not executed in this session.

## Not yet fixed; address before claiming 'all health data stays on device'
- **Bonds**: `PartnerRepositoryImpl.publishSnapshot` serializes cycle phase, dates, symptoms, mood and predictions to Firestore when sharing is enabled. These are ordinary Firestore fields, not end-to-end encrypted; their security relies on backend controls and Firestore rules. Do not claim all health data is exclusively on-device.
- **Shared diary**: `diaries/{linkId}/entries` may contain sensitive user-authored text/phase. Consider end-to-end encryption, strict schema validation, and a deletion/lifecycle policy.
- **Analytics**: Firebase Analytics is included in Gradle; audit the merged manifest, SDK auto-collection defaults, consent and sensitive event parameters. Presence of dependency does not prove sensitive information is being uploaded.
- **Database at rest**: Room's `lumi_db` is not configured with application-level SQLCipher encryption. Android app sandbox and OS encryption still apply; make no blanket on-device encryption guarantee.
- **Device backup rules**: existing database/DataStore exclusions require tests on multiple API levels, plus scrutiny of crash reports, cache and logs.
- **Legacy ZIP export files**: previously created plaintext ZIPs remain unprotected wherever users saved them; no code update can recall them.
- **GitHub**: repository is public. Audit *full Git history* for accidentally committed private keys, credentials or health data; the presence of `google-services.json` alone is not proof of a leaked private key.
- **Firestore tests**: `tools/firestore-tests/rules.test.cjs` still contains outdated expectations about direct client activation of pending Bonds links, which contradict current rules. Update and run the emulator suites before a Firebase deployment.
- **No live rules verification**: repository rules are not evidence of what is deployed in Firebase.

## Required next implementation steps
- Add a password + confirmation UX (never a password in logs/saved state) and wire the encrypted manual export and restore methods; test with low-memory devices and restoration on another handset. Prefer separate UX for explicitly marked legacy plaintext ZIP imports.
- Consider streaming encryption or a conservative size cap for larger data exports rather than keeping multiple copies in memory.
- Never log passphrases, keys, OAuth tokens, full medical payloads or user-created diary messages.
- Review Firestore rules with the emulator, test cross-account access/revocation, verify deployed rules, and assess E2EE for Bonds before privacy-related marketing.
- Build Android Debug/Release and execute `./gradlew testDebugUnitTest` and relevant instrumented tests. Not run in this environment.

## Privacy promise recommended
“Los registros principales se guardan en tu dispositivo. Si activas copias de seguridad, se cifra el archivo antes de subirlo a tu propia cuenta de Drive. Al usar Bonds, la información que elijas compartir se transmite a Firebase para sincronizar con tu pareja.” (Must be rechecked after final implementation and legal review.)
