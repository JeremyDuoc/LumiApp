# Lumi — Local-first security review (18 Oct 2026)

## Scope and constraints
Static review of repository source, not a production penetration test. No production Firestore rules, Cloud Functions, backups or user data were accessed or modified. This branch MUST NOT be deployed/merged without build, instrumented tests, migration checks and a second security review.

## Changes in this branch
1. Removed the **plaintext** manual `Lumi_Backup.zip` export entry from Settings. Existing legacy ZIP import remains available for users who need to recover old archives; imported legacy files are *not encrypted*. Do not promise old exported ZIP files are protected.
2. Added `BackupManager.exportEncryptedDatabaseArchive` and `importEncryptedDatabaseArchive` using PBKDF2-SHA256 / AES-256-GCM, with **manual .lbx files** (distinct from Drive's .lbk format). UI now prompts for a password (confirmation on export) and warns before overwriting data on restore. Export writes only ciphertext to external destinations; import authenticates before replacing Room data. Includes bounded input sizes and best-effort temporary-buffer wiping.
3. Limited PBKDF2 iterations in backup headers to 1,000,000 to reduce CPU denial-of-service by attacker-modified files.
4. Added crypto regression tests (round-trip, wrong password, tampering, untrusted KDF iteration count) plus GitHub Actions checks for JVM tests and the merged release manifest. Do not claim tests passed until CI reports success.

## Deferred or pending; verify before making broad privacy claims
- **Bonds deferred**: release has `ENABLE_BONDS=false`; Home will not create/use its partner repository and routes/tabs are disabled. Debug keeps Bonds for later development. Before ever enabling Bonds in release, note `PartnerRepositoryImpl.publishSnapshot` sends sensitive fields to Firestore without E2EE. This branch does **not** deploy Bonds or change Firestore rules.
- **Shared diary**: `diaries/{linkId}/entries` may contain sensitive user-authored text/phase. Consider end-to-end encryption, strict schema validation, and a deletion/lifecycle policy.
- **Analytics and FCM**: release manifest deactivates Firebase Analytics collection and FCM auto-init; release removes the partner messaging service. Validate the final merged manifest and network behavior of an installed release build. The library may remain a transitive dependency; deactivation is not a proof of zero traffic.
- **Database at rest**: Room's `lumi_db` is not configured with application-level SQLCipher encryption. Android app sandbox and OS encryption still apply; make no blanket on-device encryption guarantee.
- **Device backup rules**: existing database/DataStore exclusions require tests on multiple API levels, plus scrutiny of crash reports, cache and logs.
- **Legacy ZIP export files**: previously created plaintext ZIPs remain unprotected wherever users saved them; no code update can recall them.
- **GitHub**: repository is public. Audit *full Git history* for accidentally committed private keys, credentials or health data; the presence of `google-services.json` alone is not proof of a leaked private key.
- **Firestore tests**: `tools/firestore-tests/rules.test.cjs` still contains outdated expectations about direct client activation of pending Bonds links, which contradict current rules. Update and run the emulator suites before a Firebase deployment.
- **No live rules verification**: repository rules are not evidence of what is deployed in Firebase.

## Required next implementation steps
- Validate the new password-confirmation export UX and password restore UX on real Android devices, including rotation, activity process death, incorrect password, corrupted file, low-memory devices and restoration on another phone. Verify the legacy plaintext ZIP confirmation.
- Consider streaming encryption or a conservative size cap for larger data exports rather than keeping multiple copies in memory.
- Never log passphrases, keys, OAuth tokens, full medical payloads or user-created diary messages.
- Review Firestore rules with the emulator, test cross-account access/revocation, verify deployed rules, and assess E2EE for Bonds before privacy-related marketing.
- Verify the PR CI (`:app:testDebugUnitTest` and `:app:processReleaseMainManifest`) completes successfully and add relevant Android instrumented tests. Android device testing has **not** been performed in this environment.

## Privacy promise recommended
“Los registros principales se guardan en tu dispositivo. Si activas copias de seguridad, se cifra el archivo antes de subirlo a tu propia cuenta de Drive. Al usar Bonds, la información que elijas compartir se transmite a Firebase para sincronizar con tu pareja.” (Must be rechecked after final implementation and legal review.)

## Release behavior introduced in this branch
- Debug variant: Bonds remains available for internal development.
- Release variant: `ENABLE_BONDS=false`, Bonds navigation and automatic Home synchronization are bypassed, and partner repository is injected lazily to avoid initialization in Home.
- Manual files: `.lbx` are encrypted full-database archives; `.lbk` on Drive is a different encrypted JSON/GZIP backup format. Do not assume they are interchangeable.

## Profile backup recovery — fix for missing nickname (18 Oct 2026)
- **Root cause**: Room backup contains cycles/logs/reminders/chat while the nickname and cycle profile are stored separately in the `lumi_prefs` DataStore. Encrypted Google Drive JSON backups similarly included only cycles/logs/symptoms.
- **Manual .lbx**: the authenticated encrypted ZIP now contains `lumi_db` plus a small versioned `profile.json`. The file is validated and bounded before modifying Room. The profile is applied as part of the Room restoration operation when present.
- **Google Drive .lbk**: the same validated profile is an optional field in the existing JSON payload; original format version 1 remains readable.
- **Allowlist only**: nickname, cycle/period defaults, regularity, goal, pregnancy/contraception state and optional physical profile / secondary goals. Never copy raw DataStore, Firebase sharing preferences, access tokens, Google credentials, Health Connect grant state, device-specific encryption keys or prior medical consent flags.
- **Backward compatibility**: backups created before this change do **not contain the old nickname**, and cannot reconstruct it after reset. When profile information is missing, restoration retains the currently configured profile rather than inventing a nickname.
- **Validation boundaries**: cross-store changes (DataStore + Room) are not one ACID transaction. Manual restore writes DataStore while the Room transaction is in progress so a failed DataStore write rolls back Room. Drive restore writes Room and then DataStore; a device/storage failure between those commits still needs recovery testing. An installed app may need restart for screens that cache profile values.
- **Tests**: added profile round-trip, corrupt profile rejection, legacy cloud compatibility and Android integration test assertions on the saved nickname. Instrumented tests must still be executed on an Android device or emulator (compilation in CI alone does not run them).
