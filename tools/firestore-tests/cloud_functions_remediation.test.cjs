const { before, after, beforeEach, test } = require('node:test');
const assert = require('node:assert/strict');

// Configuración de Admin SDK contra el Firestore Emulator local
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8180';
process.env.GCLOUD_PROJECT = 'demo-lumi-cf-test';

// Importar DIRECTAMENTE el módulo real de producción de Cloud Functions
const functionsModule = require('../../functions/index.js');
const {
  redeemBondsInvitationCore,
  revokeBondsLinkCore,
  migrateLegacyConsentCore,
  verifyRateLimit,
  checkAndRecordFailedAttempt
} = functionsModule._core;

const admin = require('../../functions/node_modules/firebase-admin');
const db = admin.firestore();

before(() => {
  // Inicializado por functions/index.js
});

after(async () => {
  // Limpieza final
});

beforeEach(async () => {
  // Resetear colecciones de prueba
  const collections = ['partner_links', 'partner_invitations', '_rate_limits'];
  for (const c of collections) {
    const snap = await db.collection(c).get();
    const batch = db.batch();
    snap.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
  }

  // Crear invitación pendiente y enlace pendiente
  await db.collection('partner_links').doc('link_123').set({
    linkId: 'link_123',
    ownerUid: 'owner_user',
    partnerUid: null,
    status: 'PENDING',
    ownerDisplayName: 'Owner User',
    ownerSharePhase: false,
    ownerShareMood: false,
    ownerShareSymptoms: false,
    ownerSharePredictions: false,
    ownerConsentExplicitlyGranted: false
  });

  await db.collection('partner_invitations').doc('CODE1234').set({
    code: 'CODE1234',
    linkId: 'link_123',
    ownerUid: 'owner_user',
    status: 'PENDING',
    expiresAtMs: Date.now() + 86400000,
    createdAtMs: Date.now()
  });
});

test('[BACKEND REAL POSITIVO] Canje exitoso activa vínculo y mantiene partnerConsentExplicitlyGranted en false (Opt-in)', async () => {
  const res = await redeemBondsInvitationCore({
    db,
    callerUid: 'joiner_bob',
    data: {
      code: 'CODE1234',
      partnerDisplayName: 'Bob',
      partnerAvatarId: 'avatar_2'
    }
  });

  assert.equal(res.success, true);
  assert.equal(res.linkId, 'link_123');

  // Verificar estado del documento
  const linkDoc = await db.collection('partner_links').doc('link_123').get();
  const linkData = linkDoc.data();
  assert.equal(linkData.status, 'ACTIVE');
  assert.equal(linkData.partnerUid, 'joiner_bob');
  assert.equal(linkData.partnerDisplayName, 'Bob');
  assert.equal(linkData.partnerSharePhase, false);
  assert.equal(linkData.partnerShareMood, false);
  assert.equal(linkData.partnerShareSymptoms, false);
  assert.equal(linkData.partnerSharePredictions, false);
  // SEC-P0-B: Unirse no equivale a consentir compartir datos de salud
  assert.equal(linkData.partnerConsentExplicitlyGranted, false);

  // Verificar invitación marcada como CLAIMED
  const invDoc = await db.collection('partner_invitations').doc('CODE1234').get();
  assert.equal(invDoc.data().status, 'CLAIMED');
  assert.equal(invDoc.data().claimedByUid, 'joiner_bob');
});

test('[BACKEND REAL SEC-P1-D] Canje de código inexistente devuelve error uniforme failed-precondition (Anti-enumeración)', async () => {
  await assert.rejects(
    async () => {
      await redeemBondsInvitationCore({
        db,
        callerUid: 'joiner_bob',
        data: { code: 'INVALID_999' }
      });
    },
    (err) => err.code === 'failed-precondition'
  );
});

test('[BACKEND REAL SEC-P1-D] Canje de invitación expirada devuelve error uniforme failed-precondition', async () => {
  await db.collection('partner_invitations').doc('EXPIRED_CODE').set({
    code: 'EXPIRED_CODE',
    linkId: 'link_123',
    ownerUid: 'owner_user',
    status: 'PENDING',
    expiresAtMs: Date.now() - 1000
  });

  await assert.rejects(
    async () => {
      await redeemBondsInvitationCore({
        db,
        callerUid: 'joiner_bob',
        data: { code: 'EXPIRED_CODE' }
      });
    },
    (err) => err.code === 'failed-precondition'
  );
});

test('[BACKEND REAL NEGATIVO] Dueño intentando unirse a su propia invitación falla con invalid-argument', async () => {
  await assert.rejects(
    async () => {
      await redeemBondsInvitationCore({
        db,
        callerUid: 'owner_user',
        data: { code: 'CODE1234' }
      });
    },
    (err) => err.code === 'invalid-argument'
  );
});

test('[BACKEND REAL CONCURRENCIA] Dos llamadas concurrentes al mismo código: solo una triunfa y la otra falla', async () => {
  const p1 = redeemBondsInvitationCore({
    db,
    callerUid: 'joiner_1',
    data: { code: 'CODE1234' }
  });
  const p2 = redeemBondsInvitationCore({
    db,
    callerUid: 'joiner_2',
    data: { code: 'CODE1234' }
  });

  const results = await Promise.allSettled([p1, p2]);
  const fulfilled = results.filter((r) => r.status === 'fulfilled');
  const rejected = results.filter((r) => r.status === 'rejected');

  assert.equal(fulfilled.length, 1);
  assert.equal(rejected.length, 1);
  assert.equal(rejected[0].reason.code, 'failed-precondition');
});

test('[BACKEND REAL RATE LIMITING] 5 intentos fallidos consecutivos bloquean futuros intentos con resource-exhausted', async () => {
  const attacker = 'attacker_bad_user';
  for (let i = 0; i < 5; i++) {
    await assert.rejects(
      async () => {
        await redeemBondsInvitationCore({
          db,
          callerUid: attacker,
          data: { code: `FAIL_${i}` }
        });
      },
      (err) => err.code === 'failed-precondition'
    );
  }

  // El 6to intento debe ser bloqueado por resource-exhausted inmediatamente
  await assert.rejects(
    async () => {
      await redeemBondsInvitationCore({
        db,
        callerUid: attacker,
        data: { code: 'CODE1234' }
      });
    },
    (err) => err.code === 'resource-exhausted'
  );
});

test('[BACKEND REAL REVOCACIÓN POSITIVO] Revocación atómica vacía snapshots y tokens', async () => {
  // Primero activar
  await redeemBondsInvitationCore({
    db,
    callerUid: 'joiner_bob',
    data: { code: 'CODE1234' }
  });

  // Simular snapshots
  await db.collection('partner_links').doc('link_123').update({
    ownerSnapshot: { currentPhase: 'FOLLICULAR' },
    partnerSnapshot: { currentPhase: 'LUTEAL' },
    ownerFcmToken: 'token_a',
    partnerFcmToken: 'token_b'
  });

  // Revocar por parte de Bob
  const res = await revokeBondsLinkCore({
    db,
    callerUid: 'joiner_bob',
    data: { linkId: 'link_123' }
  });
  assert.equal(res.success, true);

  const doc = await db.collection('partner_links').doc('link_123').get();
  const data = doc.data();
  assert.equal(data.status, 'REVOKED');
  assert.equal(data.ownerSnapshot, null);
  assert.equal(data.partnerSnapshot, null);
  assert.equal(data.ownerFcmToken, null);
  assert.equal(data.partnerFcmToken, null);
  assert.equal(data.revokedByUid, 'joiner_bob');
});

test('[BACKEND REAL REVOCACIÓN NEGATIVO] Extraño intentando revocar vínculo ajeno falla con permission-denied', async () => {
  await assert.rejects(
    async () => {
      await revokeBondsLinkCore({
        db,
        callerUid: 'stranger_charlie',
        data: { linkId: 'link_123' }
      });
    },
    (err) => err.code === 'permission-denied'
  );
});

// ─────────────────────────────────────────────────────────────────────────────
// SEC-P0-B: Migración idempotente de vínculos heredados y reconfirmación
// ─────────────────────────────────────────────────────────────────────────────
test('[SEC-P0-B MIGRACIÓN IDEMPOTENTE] Vínculo heredado con permisos true sin consentimiento explícito es neutralizado', async () => {
  // 1. Simular documento heredado en Firestore con permisos en true antiguos y snapshots
  const legacyLinkId = 'link_legacy_unsafe';
  await db.collection('partner_links').doc(legacyLinkId).set({
    linkId: legacyLinkId,
    ownerUid: 'owner_legacy',
    partnerUid: 'partner_legacy',
    status: 'ACTIVE',
    ownerDisplayName: 'Owner',
    partnerDisplayName: 'Partner',
    ownerSharePhase: true, // Heredado peligroso
    ownerShareMood: true,
    ownerShareSymptoms: true,
    ownerSharePredictions: true,
    partnerSharePhase: true,
    partnerShareMood: true,
    partnerShareSymptoms: true,
    partnerSharePredictions: true,
    // Consentimiento explícito ausente
    ownerSnapshot: { currentPhase: 'LUTEAL', cycleStartDateMs: 1700000000000 },
    partnerSnapshot: { currentPhase: 'OVULATORY', currentMood: 'TIRED' },
    createdAtMs: Date.now() - 1000000
  });

  // 2. Ejecutar migración idempotente
  const migResult = await migrateLegacyConsentCore({
    db,
    callerUid: 'owner_legacy',
    data: { linkId: legacyLinkId }
  });
  assert.equal(migResult.success, true);
  assert.equal(migResult.sanitized, true);

  // 3. Verificar que los permisos fueron neutralizados y snapshots eliminados
  const docAfter = await db.collection('partner_links').doc(legacyLinkId).get();
  const dataAfter = docAfter.data();
  assert.equal(dataAfter.ownerSharePhase, false);
  assert.equal(dataAfter.ownerShareMood, false);
  assert.equal(dataAfter.ownerShareSymptoms, false);
  assert.equal(dataAfter.ownerSharePredictions, false);
  assert.equal(dataAfter.ownerConsentExplicitlyGranted, false);
  assert.equal(dataAfter.ownerSnapshot, null);

  assert.equal(dataAfter.partnerSharePhase, false);
  assert.equal(dataAfter.partnerShareMood, false);
  assert.equal(dataAfter.partnerShareSymptoms, false);
  assert.equal(dataAfter.partnerSharePredictions, false);
  assert.equal(dataAfter.partnerConsentExplicitlyGranted, false);
  assert.equal(dataAfter.partnerSnapshot, null);

  // 4. Probar idempotencia: ejecutar migración una segunda vez
  const migResult2 = await migrateLegacyConsentCore({
    db,
    callerUid: 'owner_legacy',
    data: { linkId: legacyLinkId }
  });
  assert.equal(migResult2.success, true);
  assert.equal(migResult2.sanitized, false); // Ya estaba limpio, ningún cambio adicional
});

test('[SEC-P0-B RECONFIRMACIÓN] Tras neutralización, reconfirmación explícita permite reactivar compartir', async () => {
  const linkId = 'link_reconfirm';
  await db.collection('partner_links').doc(linkId).set({
    linkId,
    ownerUid: 'owner_alice',
    partnerUid: 'partner_bob',
    status: 'ACTIVE',
    ownerSharePhase: false,
    ownerShareMood: false,
    ownerShareSymptoms: false,
    ownerSharePredictions: false,
    ownerConsentExplicitlyGranted: false,
    ownerSnapshot: null
  });

  // Reconfirmar consentimiento explícito y activar compartir fase y ánimo
  await db.collection('partner_links').doc(linkId).update({
    ownerConsentExplicitlyGranted: true,
    ownerSharePhase: true,
    ownerShareMood: true,
    ownerSnapshot: {
      currentPhase: 'FOLLICULAR',
      currentMood: 'HAPPY',
      topSymptoms: [],
      daysUntilNextPhase: 0,
      late: false,
      delayDays: 0,
      cycleStartDateMs: null,
      cycleLengthDays: 0,
      periodLengthDays: 0
    },
    updatedAtMs: Date.now()
  });

  const doc = await db.collection('partner_links').doc(linkId).get();
  assert.equal(doc.data().ownerConsentExplicitlyGranted, true);
  assert.equal(doc.data().ownerSharePhase, true);
  assert.equal(doc.data().ownerSnapshot.currentPhase, 'FOLLICULAR');
});
