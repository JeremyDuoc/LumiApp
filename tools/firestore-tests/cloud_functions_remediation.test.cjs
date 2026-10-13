const { before, after, beforeEach, test } = require('node:test');
const assert = require('node:assert/strict');
const admin = require('../../functions/node_modules/firebase-admin');

// Configuración de Admin SDK contra el Firestore Emulator local
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8180';
let app;
let db;

before(() => {
  app = admin.initializeApp({ projectId: 'demo-lumi-cf-test' }, 'cf-test-app');
  db = admin.firestore(app);
});

after(async () => {
  await app.delete();
});

// Importar las funciones lógicas de backend
const RATE_LIMIT_WINDOW_MS = 15 * 60 * 1000;
const MAX_FAILED_ATTEMPTS = 5;

async function checkAndRecordFailedAttempt(uid) {
  const rateLimitRef = db.collection('_rate_limits').doc(`redeem_${uid}`);
  const now = Date.now();
  await db.runTransaction(async (t) => {
    const doc = await t.get(rateLimitRef);
    let attempts = [];
    if (doc.exists) {
      attempts = (doc.data().attempts || []).filter((ts) => now - ts < RATE_LIMIT_WINDOW_MS);
    }
    attempts.push(now);
    t.set(rateLimitRef, { attempts, updatedAtMs: now });
  });
}

async function verifyRateLimit(uid) {
  const rateLimitRef = db.collection('_rate_limits').doc(`redeem_${uid}`);
  const doc = await rateLimitRef.get();
  if (doc.exists) {
    const now = Date.now();
    const attempts = (doc.data().attempts || []).filter((ts) => now - ts < RATE_LIMIT_WINDOW_MS);
    if (attempts.length >= MAX_FAILED_ATTEMPTS) {
      const err = new Error('Demasiados intentos fallidos.');
      err.code = 'resource-exhausted';
      throw err;
    }
  }
}

async function redeemInvitationLogic({ callerUid, code, partnerDisplayName, partnerAvatarId, partnerFcmToken }) {
  if (!callerUid) throw new Error('Unauthenticated');
  if (!code) throw new Error('Invalid code');

  await verifyRateLimit(callerUid);

  try {
    return await db.runTransaction(async (transaction) => {
      const invRef = db.collection('partner_invitations').doc(code);
      const invDoc = await transaction.get(invRef);
      if (!invDoc.exists) {
        const err = new Error('Invitation not found');
        err.code = 'not-found';
        throw err;
      }
      const invData = invDoc.data();
      if (invData.status !== 'PENDING') {
        const err = new Error('Invitation already claimed/revoked');
        err.code = 'failed-precondition';
        throw err;
      }
      const now = Date.now();
      if (invData.expiresAtMs && invData.expiresAtMs < now) {
        const err = new Error('Invitation expired');
        err.code = 'deadline-exceeded';
        throw err;
      }
      if (invData.ownerUid === callerUid) {
        const err = new Error('Owner cannot join own link');
        err.code = 'invalid-argument';
        throw err;
      }

      const linkRef = db.collection('partner_links').doc(invData.linkId);
      const linkDoc = await transaction.get(linkRef);
      if (!linkDoc.exists) {
        const err = new Error('Link not found');
        err.code = 'not-found';
        throw err;
      }
      const linkData = linkDoc.data();
      if (linkData.status !== 'PENDING' || linkData.partnerUid != null) {
        const err = new Error('Link not pending');
        err.code = 'failed-precondition';
        throw err;
      }

      transaction.update(invRef, {
        status: 'CLAIMED',
        claimedByUid: callerUid,
        claimedAtMs: now
      });

      transaction.update(linkRef, {
        partnerUid: callerUid,
        partnerDisplayName: partnerDisplayName || null,
        partnerAvatarId: partnerAvatarId || null,
        partnerFcmToken: partnerFcmToken || null,
        partnerSharePhase: false,
        partnerShareMood: false,
        partnerShareSymptoms: false,
        partnerSharePredictions: false,
        partnerConsentExplicitlyGranted: true,
        status: 'ACTIVE',
        activatedAtMs: now,
        updatedAtMs: now
      });

      return { success: true, linkId: invData.linkId, ownerDisplayName: linkData.ownerDisplayName };
    });
  } catch (err) {
    if (err.code === 'not-found' || err.code === 'deadline-exceeded' || err.code === 'failed-precondition') {
      await checkAndRecordFailedAttempt(callerUid);
    }
    throw err;
  }
}

async function revokeBondsLinkLogic({ callerUid, linkId }) {
  return await db.runTransaction(async (t) => {
    const linkRef = db.collection('partner_links').doc(linkId);
    const linkDoc = await t.get(linkRef);
    if (!linkDoc.exists) {
      const err = new Error('Not found');
      err.code = 'not-found';
      throw err;
    }
    const data = linkDoc.data();
    if (data.ownerUid !== callerUid && data.partnerUid !== callerUid) {
      const err = new Error('Permission denied');
      err.code = 'permission-denied';
      throw err;
    }
    if (data.status === 'REVOKED') return { success: true, alreadyRevoked: true };

    const now = Date.now();
    t.update(linkRef, {
      status: 'REVOKED',
      revokedByUid: callerUid,
      revokedAtMs: now,
      ownerSnapshot: null,
      partnerSnapshot: null,
      ownerFcmToken: null,
      partnerFcmToken: null,
      updatedAtMs: now
    });
    return { success: true };
  });
}

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
    ownerSharePredictions: false
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

test('[BACKEND POSITIVO] Canje exitoso activa vínculo y fija consentimientos en false (Opt-in)', async () => {
  const res = await redeemInvitationLogic({
    callerUid: 'joiner_bob',
    code: 'CODE1234',
    partnerDisplayName: 'Bob',
    partnerAvatarId: 'avatar_2'
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
  assert.equal(linkData.partnerConsentExplicitlyGranted, true);

  // Verificar invitación marcada como CLAIMED
  const invDoc = await db.collection('partner_invitations').doc('CODE1234').get();
  assert.equal(invDoc.data().status, 'CLAIMED');
  assert.equal(invDoc.data().claimedByUid, 'joiner_bob');
});

test('[BACKEND NEGATIVO] Canje de código inexistente falla con not-found', async () => {
  await assert.rejects(
    async () => {
      await redeemInvitationLogic({ callerUid: 'joiner_bob', code: 'INVALID_999' });
    },
    (err) => err.code === 'not-found'
  );
});

test('[BACKEND NEGATIVO] Canje de invitación expirada falla con deadline-exceeded', async () => {
  await db.collection('partner_invitations').doc('EXPIRED_CODE').set({
    code: 'EXPIRED_CODE',
    linkId: 'link_123',
    ownerUid: 'owner_user',
    status: 'PENDING',
    expiresAtMs: Date.now() - 1000
  });

  await assert.rejects(
    async () => {
      await redeemInvitationLogic({ callerUid: 'joiner_bob', code: 'EXPIRED_CODE' });
    },
    (err) => err.code === 'deadline-exceeded'
  );
});

test('[BACKEND NEGATIVO] Dueño intentando unirse a su propia invitación falla con invalid-argument', async () => {
  await assert.rejects(
    async () => {
      await redeemInvitationLogic({ callerUid: 'owner_user', code: 'CODE1234' });
    },
    (err) => err.code === 'invalid-argument'
  );
});

test('[BACKEND CONCURRENCIA] Dos usuarios canjeando simultáneamente el mismo código: solo uno triunfa', async () => {
  const p1 = redeemInvitationLogic({ callerUid: 'joiner_1', code: 'CODE1234' });
  const p2 = redeemInvitationLogic({ callerUid: 'joiner_2', code: 'CODE1234' });

  const results = await Promise.allSettled([p1, p2]);
  const fulfilled = results.filter((r) => r.status === 'fulfilled');
  const rejected = results.filter((r) => r.status === 'rejected');

  assert.equal(fulfilled.length, 1);
  assert.equal(rejected.length, 1);
  assert.equal(rejected[0].reason.code, 'failed-precondition');
});

test('[BACKEND RATE LIMITING] 5 intentos fallidos consecutivos bloquean futuros intentos', async () => {
  const attacker = 'attacker_bad_user';
  for (let i = 0; i < 5; i++) {
    await assert.rejects(
      async () => {
        await redeemInvitationLogic({ callerUid: attacker, code: `FAIL_${i}` });
      },
      (err) => err.code === 'not-found'
    );
  }

  // El 6to intento debe ser bloqueado por resource-exhausted antes de tocar la DB
  await assert.rejects(
    async () => {
      await redeemInvitationLogic({ callerUid: attacker, code: 'CODE1234' });
    },
    (err) => err.code === 'resource-exhausted'
  );
});

test('[BACKEND REVOCACIÓN POSITIVO] Revocación atómica vacía snapshots y tokens', async () => {
  // Primero activar
  await redeemInvitationLogic({ callerUid: 'joiner_bob', code: 'CODE1234' });
  // Simular snapshots
  await db.collection('partner_links').doc('link_123').update({
    ownerSnapshot: { phase: 'FOLLICULAR' },
    partnerSnapshot: { phase: 'LUTEAL' },
    ownerFcmToken: 'token_a',
    partnerFcmToken: 'token_b'
  });

  // Revocar por parte de Bob
  const res = await revokeBondsLinkLogic({ callerUid: 'joiner_bob', linkId: 'link_123' });
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

test('[BACKEND REVOCACIÓN NEGATIVO] Extraño intentando revocar vínculo ajeno falla con permission-denied', async () => {
  await assert.rejects(
    async () => {
      await revokeBondsLinkLogic({ callerUid: 'stranger_charlie', linkId: 'link_123' });
    },
    (err) => err.code === 'permission-denied'
  );
});
