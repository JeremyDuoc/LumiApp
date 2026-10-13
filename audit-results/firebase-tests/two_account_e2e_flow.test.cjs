const { before, after, beforeEach, test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { doc, setDoc, updateDoc, getDoc } = require('firebase/firestore');

// Módulo real de producción de Cloud Functions
process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8180';
process.env.GCLOUD_PROJECT = 'demo-lumi-e2e-test';

const functionsModule = require('../../functions/index.js');
const { redeemBondsInvitationCore, revokeBondsLinkCore } = functionsModule._core;

const admin = require('../../functions/node_modules/firebase-admin');
const adminDb = admin.firestore();

let env;

const linkDocRef = (uid, linkId) =>
  doc(env.authenticatedContext(uid).firestore(), `partner_links/${linkId}`);

const invDocRef = (uid, code) =>
  doc(env.authenticatedContext(uid).firestore(), `partner_invitations/${code}`);

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-lumi-e2e-test',
    firestore: {
      host: '127.0.0.1',
      port: 8180,
      rules: readFileSync(path.join(__dirname, '../../firestore.rules'), 'utf8')
    }
  });
});

after(async () => {
  await env?.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

test('[E2E DOS CUENTAS] Flujo completo: Invitación -> Canje -> Consentimiento -> Gestos -> Revocación -> Bloqueo', async () => {
  const aliceUid = 'user_alice_account';
  const bobUid = 'user_bob_account';
  const linkId = 'link_e2e_flow_1';
  const inviteCode = '87654321';
  const now = Date.now();

  // ── PASO 1: Alice crea vínculo PENDING e invitación bajo reglas de Firestore ──
  await assertSucceeds(
    setDoc(linkDocRef(aliceUid, linkId), {
      linkId,
      ownerUid: aliceUid,
      partnerUid: null,
      status: 'PENDING',
      linkCode: inviteCode,
      linkType: 'CYCLE_SYNC',
      ownerDisplayName: 'Alice',
      partnerDisplayName: null,
      ownerAvatarId: 'avatar_alice',
      partnerAvatarId: null,
      relationLabel: 'Amiga',
      ownerFcmToken: 'token_alice',
      partnerFcmToken: null,
      ownerSharePhase: false,
      ownerShareMood: false,
      ownerShareSymptoms: false,
      ownerSharePredictions: false,
      ownerConsentExplicitlyGranted: false,
      partnerSharePhase: false,
      partnerShareMood: false,
      partnerShareSymptoms: false,
      partnerSharePredictions: false,
      partnerConsentExplicitlyGranted: false,
      ownerSnapshot: null,
      partnerSnapshot: null,
      createdAtMs: now
    })
  );

  await assertSucceeds(
    setDoc(invDocRef(aliceUid, inviteCode), {
      code: inviteCode,
      linkId,
      ownerUid: aliceUid,
      status: 'PENDING',
      expiresAtMs: now + 48 * 3600 * 1000,
      createdAtMs: now
    })
  );

  // Bob intenta unirse directamente saltándose la Cloud Function: DEBE FALLAR por reglas
  await assertFails(
    updateDoc(linkDocRef(bobUid, linkId), {
      partnerUid: bobUid,
      status: 'ACTIVE'
    })
  );

  // ── PASO 2: Bob canjea la invitación a través de la Cloud Function real ──
  const redeemResult = await redeemBondsInvitationCore({
    db: adminDb,
    callerUid: bobUid,
    data: {
      code: inviteCode,
      partnerDisplayName: 'Bob',
      partnerAvatarId: 'avatar_bob',
      partnerFcmToken: 'token_bob'
    }
  });

  assert.equal(redeemResult.success, true);
  assert.equal(redeemResult.linkId, linkId);

  // ── PASO 3: Verificación de estado ACTIVE y Opt-in estricto ──
  const aliceView = await getDoc(linkDocRef(aliceUid, linkId));
  const bobView = await getDoc(linkDocRef(bobUid, linkId));

  assert.equal(aliceView.data().status, 'ACTIVE');
  assert.equal(bobView.data().status, 'ACTIVE');
  assert.equal(bobView.data().partnerUid, bobUid);
  // Unirse a la conexión social NO equivale a consentir compartir datos de salud
  assert.equal(bobView.data().partnerConsentExplicitlyGranted, false);
  assert.equal(aliceView.data().ownerConsentExplicitlyGranted, false);
  assert.equal(bobView.data().ownerSnapshot, null);
  assert.equal(aliceView.data().partnerSnapshot, null);

  // Intento de cliente de enviar snapshot con datos de salud sin consentimiento: DEBE FALLAR
  await assertFails(
    updateDoc(linkDocRef(aliceUid, linkId), {
      ownerSharePhase: true,
      ownerSnapshot: {
        currentPhase: 'FOLLICULAR',
        currentMood: null,
        topSymptoms: [],
        daysUntilNextPhase: 0,
        late: false,
        delayDays: 0,
        cycleStartDateMs: null,
        cycleLengthDays: 0,
        periodLengthDays: 0
      },
      updatedAtMs: Date.now()
    })
  );

  // ── PASO 4: Alice otorga consentimiento explícito y publica snapshot granular ──
  await assertSucceeds(
    updateDoc(linkDocRef(aliceUid, linkId), {
      ownerConsentExplicitlyGranted: true,
      ownerSharePhase: true,
      ownerShareMood: true,
      ownerShareSymptoms: false,
      ownerSharePredictions: false,
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
    })
  );

  // Bob lee el snapshot autorizado de Alice
  const bobReadUpdated = await getDoc(linkDocRef(bobUid, linkId));
  const aliceSnapshotForBob = bobReadUpdated.data().ownerSnapshot;
  assert.equal(aliceSnapshotForBob.currentPhase, 'FOLLICULAR');
  assert.equal(aliceSnapshotForBob.currentMood, 'HAPPY');
  assert.deepEqual(aliceSnapshotForBob.topSymptoms, []);
  assert.equal(aliceSnapshotForBob.cycleStartDateMs, null);

  // ── PASO 5: Interacción afectiva real: Bob envía un Care Action (HUG) ──
  const careActionTimestamp = Date.now();
  await assertSucceeds(
    updateDoc(linkDocRef(bobUid, linkId), {
      lastPartnerCareAction: {
        action: 'HUG',
        message: 'Un abrazo fuerte!',
        timestamp: careActionTimestamp
      },
      updatedAtMs: careActionTimestamp
    })
  );

  // Alice recibe el gesto afectivo
  const aliceReadAction = await getDoc(linkDocRef(aliceUid, linkId));
  assert.equal(aliceReadAction.data().lastPartnerCareAction.action, 'HUG');
  assert.equal(aliceReadAction.data().lastPartnerCareAction.message, 'Un abrazo fuerte!');

  // ── PASO 6: Revocación atómica por parte de Alice mediante Cloud Function ──
  const revokeResult = await revokeBondsLinkCore({
    db: adminDb,
    callerUid: aliceUid,
    data: { linkId }
  });
  assert.equal(revokeResult.success, true);

  // ── PASO 7: Bloqueo post-revocación en Firestore Rules para AMBAS cuentas ──
  // Bob NO puede leer el vínculo revocado
  await assertFails(getDoc(linkDocRef(bobUid, linkId)));
  // Alice NO puede leer el vínculo revocado desde cliente
  await assertFails(getDoc(linkDocRef(aliceUid, linkId)));

  // Bob NO puede escribir en el vínculo revocado
  await assertFails(
    updateDoc(linkDocRef(bobUid, linkId), {
      lastPartnerCareAction: { action: 'FLOWERS', timestamp: Date.now() }
    })
  );

  // Bob NO puede reactivar el vínculo revocado
  await assertFails(
    updateDoc(linkDocRef(bobUid, linkId), {
      status: 'ACTIVE'
    })
  );
});
