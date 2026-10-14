const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { initializeApp, deleteApp } = require('firebase/app');
const { getAuth, connectAuthEmulator, signInAnonymously } = require('firebase/auth');
const {
  getFirestore,
  connectFirestoreEmulator,
  doc,
  setDoc,
  getDoc,
  updateDoc
} = require('firebase/firestore');
const {
  getFunctions,
  connectFunctionsEmulator,
  httpsCallable
} = require('firebase/functions');

const PROJECT_ID = 'demo-lumi-bonds';
const AUTH_EMULATOR_HOST = 'http://127.0.0.1:9099';
const FIRESTORE_HOST = '127.0.0.1';
const FIRESTORE_PORT = 8080;
const FUNCTIONS_HOST = '127.0.0.1';
const FUNCTIONS_PORT = 5001;

// Configurar Admin SDK para siembra de fixtures de prueba
process.env.FIRESTORE_EMULATOR_HOST = `${FIRESTORE_HOST}:${FIRESTORE_PORT}`;
process.env.GCLOUD_PROJECT = PROJECT_ID;
const admin = require(path.join(__dirname, '../../functions/node_modules/firebase-admin'));
if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}
const adminDb = admin.firestore();

// Device A (Alice) client instance
let appAlice, authAlice, dbAlice, fnAlice, aliceUid;

// Device B (Bob) client instance
let appBob, authBob, dbBob, fnBob, bobUid;

// Device C (Charlie) client instance (for unauthorized / 3rd party tests)
let appCharlie, authCharlie, dbCharlie, fnCharlie, charlieUid;

before(async () => {
  // Device A - Alice
  appAlice = initializeApp({ projectId: PROJECT_ID, apiKey: 'fake-api-key-alice' }, 'AliceApp');
  authAlice = getAuth(appAlice);
  connectAuthEmulator(authAlice, AUTH_EMULATOR_HOST, { disableWarnings: true });
  dbAlice = getFirestore(appAlice);
  connectFirestoreEmulator(dbAlice, FIRESTORE_HOST, FIRESTORE_PORT);
  fnAlice = getFunctions(appAlice, 'us-central1');
  connectFunctionsEmulator(fnAlice, FUNCTIONS_HOST, FUNCTIONS_PORT);

  const credA = await signInAnonymously(authAlice);
  aliceUid = credA.user.uid;

  // Device B - Bob
  appBob = initializeApp({ projectId: PROJECT_ID, apiKey: 'fake-api-key-bob' }, 'BobApp');
  authBob = getAuth(appBob);
  connectAuthEmulator(authBob, AUTH_EMULATOR_HOST, { disableWarnings: true });
  dbBob = getFirestore(appBob);
  connectFirestoreEmulator(dbBob, FIRESTORE_HOST, FIRESTORE_PORT);
  fnBob = getFunctions(appBob, 'us-central1');
  connectFunctionsEmulator(fnBob, FUNCTIONS_HOST, FUNCTIONS_PORT);

  const credB = await signInAnonymously(authBob);
  bobUid = credB.user.uid;

  // Device C - Charlie
  appCharlie = initializeApp({ projectId: PROJECT_ID, apiKey: 'fake-api-key-charlie' }, 'CharlieApp');
  authCharlie = getAuth(appCharlie);
  connectAuthEmulator(authCharlie, AUTH_EMULATOR_HOST, { disableWarnings: true });
  dbCharlie = getFirestore(appCharlie);
  connectFirestoreEmulator(dbCharlie, FIRESTORE_HOST, FIRESTORE_PORT);
  fnCharlie = getFunctions(appCharlie, 'us-central1');
  connectFunctionsEmulator(fnCharlie, FUNCTIONS_HOST, FUNCTIONS_PORT);

  const credC = await signInAnonymously(authCharlie);
  charlieUid = credC.user.uid;
});

test('1. [AUTH & ISOLATION] Dos dispositivos independientes obtienen credenciales y UIDs únicos', () => {
  assert.ok(aliceUid, 'Alice debe tener UID');
  assert.ok(bobUid, 'Bob debe tener UID');
  assert.ok(charlieUid, 'Charlie debe tener UID');
  assert.notEqual(aliceUid, bobUid, 'Alice y Bob deben tener UIDs distintos');
  assert.notEqual(aliceUid, charlieUid, 'Alice y Charlie deben tener UIDs distintos');
  console.log(`  ✓ Dispositivo A (Alice): ${aliceUid}`);
  console.log(`  ✓ Dispositivo B (Bob): ${bobUid}`);
  console.log(`  ✓ Dispositivo C (Charlie): ${charlieUid}`);
});

test('2. [CREACIÓN DE INVITACIÓN] Alice genera vínculo PENDING y código de vinculación en Firestore', async () => {
  const linkId = `link_${Date.now()}_test`;
  const code = Math.floor(10000000 + Math.random() * 90000000).toString();
  const now = Date.now();

  const linkData = {
    linkId,
    ownerUid: aliceUid,
    partnerUid: null,
    status: 'PENDING',
    linkCode: code,
    linkType: 'CYCLE_SYNC',
    ownerDisplayName: 'Alice',
    partnerDisplayName: null,
    ownerAvatarId: 'avatar_alice',
    partnerAvatarId: null,
    relationLabel: 'Pareja',
    ownerFcmToken: 'token_fcm_alice',
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
  };

  const invData = {
    code,
    linkId,
    ownerUid: aliceUid,
    status: 'PENDING',
    expiresAtMs: now + 48 * 3600 * 1000,
    createdAtMs: now
  };

  await setDoc(doc(dbAlice, 'partner_links', linkId), linkData);
  await setDoc(doc(dbAlice, 'partner_invitations', code), invData);

  // Verificación de lectura por Alice
  const linkSnap = await getDoc(doc(dbAlice, 'partner_links', linkId));
  assert.ok(linkSnap.exists());
  assert.equal(linkSnap.data().status, 'PENDING');
  assert.equal(linkSnap.data().linkCode, code);

  const invSnap = await getDoc(doc(dbAlice, 'partner_invitations', code));
  assert.ok(invSnap.exists());
  assert.equal(invSnap.data().status, 'PENDING');
  console.log(`  ✓ Vínculo creado: ${linkId} con código ${code}`);
});

test('3. [SEGURIDAD - REGLAS] Bob no puede saltarse la Cloud Function para activarse directamente', async () => {
  const linkId = `link_bypass_${Date.now()}`;
  const code = Math.floor(10000000 + Math.random() * 90000000).toString();
  const now = Date.now();

  await setDoc(doc(dbAlice, 'partner_links', linkId), {
    linkId,
    ownerUid: aliceUid,
    partnerUid: null,
    status: 'PENDING',
    linkCode: code,
    linkType: 'CYCLE_SYNC',
    createdAtMs: now
  });

  // Bob intenta actualizar directamente el documento para asociarse
  let errorCaught = false;
  try {
    await updateDoc(doc(dbBob, 'partner_links', linkId), {
      partnerUid: bobUid,
      status: 'ACTIVE'
    });
  } catch (err) {
    errorCaught = true;
    assert.ok(err.code === 'permission-denied' || /permission-denied|PERMISSION_DENIED|false for 'update'/i.test(err.message));
  }
  assert.ok(errorCaught, 'Las reglas de Firestore deben denegar la activación directa por un usuario no vinculado');
  console.log('  ✓ Intento de bypass de Cloud Function bloqueado por Firestore Rules (PERMISSION_DENIED)');
});

test('4. [SEGURIDAD - CASOS NEGATIVOS] Validación estricta en Cloud Functions', async () => {
  const redeemFnBob = httpsCallable(fnBob, 'redeemBondsInvitation');
  const redeemFnAlice = httpsCallable(fnAlice, 'redeemBondsInvitation');

  // Caso 4A: Código inexistente
  let errNonExistent = null;
  try {
    await redeemFnBob({ code: '00000000' });
  } catch (err) {
    errNonExistent = err;
  }
  assert.ok(errNonExistent);
  assert.equal(errNonExistent.code, 'functions/failed-precondition');
  console.log('  ✓ Código inexistente rechazado con FAILED_PRECONDITION');

  // Caso 4B: Código expirado (sembrado con Admin SDK)
  const expiredCode = Math.floor(10000000 + Math.random() * 90000000).toString();
  const expLinkId = `link_exp_${Date.now()}`;
  const past = Date.now() - 3600000; // 1 hora atrás
  await adminDb.collection('partner_links').doc(expLinkId).set({
    linkId: expLinkId,
    ownerUid: aliceUid,
    partnerUid: null,
    status: 'PENDING',
    linkCode: expiredCode,
    linkType: 'CYCLE_SYNC',
    createdAtMs: past - 1000
  });
  await adminDb.collection('partner_invitations').doc(expiredCode).set({
    code: expiredCode,
    linkId: expLinkId,
    ownerUid: aliceUid,
    status: 'PENDING',
    expiresAtMs: past,
    createdAtMs: past - 1000
  });

  let errExpired = null;
  try {
    await redeemFnBob({ code: expiredCode });
  } catch (err) {
    errExpired = err;
  }
  assert.ok(errExpired);
  assert.equal(errExpired.code, 'functions/failed-precondition');
  console.log('  ✓ Código expirado rechazado con FAILED_PRECONDITION');

  // Caso 4C: Auto-vinculación prohibida (Alice intenta canjear su propio código)
  const selfCode = Math.floor(10000000 + Math.random() * 90000000).toString();
  const selfLinkId = `link_self_${Date.now()}`;
  await setDoc(doc(dbAlice, 'partner_links', selfLinkId), {
    linkId: selfLinkId,
    ownerUid: aliceUid,
    partnerUid: null,
    status: 'PENDING',
    linkCode: selfCode,
    linkType: 'CYCLE_SYNC',
    createdAtMs: Date.now()
  });
  await setDoc(doc(dbAlice, 'partner_invitations', selfCode), {
    code: selfCode,
    linkId: selfLinkId,
    ownerUid: aliceUid,
    status: 'PENDING',
    expiresAtMs: Date.now() + 86400000,
    createdAtMs: Date.now()
  });

  let errSelf = null;
  try {
    await redeemFnAlice({ code: selfCode });
  } catch (err) {
    errSelf = err;
  }
  assert.ok(errSelf);
  assert.equal(errSelf.code, 'functions/invalid-argument');
  console.log('  ✓ Intento de auto-vinculación rechazado con INVALID_ARGUMENT');
});

test('5. [FLUJO COMPLETO HAPPY PATH] Canje exitoso entre Dispositivo A y Dispositivo B', async () => {
  const linkId = `link_happy_${Date.now()}`;
  const code = Math.floor(10000000 + Math.random() * 90000000).toString();
  const now = Date.now();

  // 1. Alice crea invitación válida
  await setDoc(doc(dbAlice, 'partner_links', linkId), {
    linkId,
    ownerUid: aliceUid,
    partnerUid: null,
    status: 'PENDING',
    linkCode: code,
    linkType: 'CYCLE_SYNC',
    ownerDisplayName: 'Alice',
    partnerDisplayName: null,
    ownerAvatarId: 'avatar_a',
    partnerAvatarId: null,
    relationLabel: 'Pareja',
    ownerFcmToken: 'token_a',
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
  });

  await setDoc(doc(dbAlice, 'partner_invitations', code), {
    code,
    linkId,
    ownerUid: aliceUid,
    status: 'PENDING',
    expiresAtMs: now + 48 * 3600 * 1000,
    createdAtMs: now
  });

  // 2. Bob canjea la invitación a través de la Cloud Function real
  const redeemFnBob = httpsCallable(fnBob, 'redeemBondsInvitation');
  const result = await redeemFnBob({
    code,
    partnerDisplayName: 'Bob',
    partnerAvatarId: 'avatar_b',
    partnerFcmToken: 'token_b'
  });

  assert.equal(result.data.success, true);
  assert.equal(result.data.linkId, linkId);

  // 3. Verificación de estado ACTIVE en Firestore para ambos dispositivos
  const linkAliceView = await getDoc(doc(dbAlice, 'partner_links', linkId));
  const linkBobView = await getDoc(doc(dbBob, 'partner_links', linkId));

  assert.equal(linkAliceView.data().status, 'ACTIVE');
  assert.equal(linkBobView.data().status, 'ACTIVE');
  assert.equal(linkAliceView.data().partnerUid, bobUid);
  assert.equal(linkBobView.data().partnerUid, bobUid);
  assert.equal(linkBobView.data().partnerDisplayName, 'Bob');

  // Opt-in estricto preservado
  assert.equal(linkBobView.data().partnerConsentExplicitlyGranted, false);
  assert.equal(linkAliceView.data().ownerConsentExplicitlyGranted, false);
  assert.equal(linkBobView.data().ownerSnapshot, null);
  assert.equal(linkAliceView.data().partnerSnapshot, null);

  // Invitación marcada como CLAIMED
  const invView = await getDoc(doc(dbAlice, 'partner_invitations', code));
  assert.equal(invView.data().status, 'CLAIMED');

  console.log(`  ✓ Vinculación completada exitosamente entre Alice y Bob (linkId: ${linkId})`);

  // 4. Intento de un tercer dispositivo (Charlie) de canjear el mismo código: debe ser rechazado
  const redeemFnCharlie = httpsCallable(fnCharlie, 'redeemBondsInvitation');
  let errCharlie = null;
  try {
    await redeemFnCharlie({ code });
  } catch (err) {
    errCharlie = err;
  }
  assert.ok(errCharlie);
  assert.equal(errCharlie.code, 'functions/failed-precondition');
  console.log('  ✓ Intento de reutilización de código por Charlie rechazado con FAILED_PRECONDITION');
});

test('6. [PRIVACIDAD Y CONSENTIMIENTO GRANULAR] Publicación y filtrado de datos de salud', async () => {
  const linkId = `link_privacy_${Date.now()}`;
  const code = Math.floor(10000000 + Math.random() * 90000000).toString();
  const now = Date.now();

  // Crear y canjear vínculo
  await setDoc(doc(dbAlice, 'partner_links', linkId), {
    linkId,
    ownerUid: aliceUid,
    partnerUid: null,
    status: 'PENDING',
    linkCode: code,
    linkType: 'CYCLE_SYNC',
    createdAtMs: now
  });
  await setDoc(doc(dbAlice, 'partner_invitations', code), {
    code,
    linkId,
    ownerUid: aliceUid,
    status: 'PENDING',
    expiresAtMs: now + 48 * 3600 * 1000,
    createdAtMs: now
  });
  const redeemFnBob = httpsCallable(fnBob, 'redeemBondsInvitation');
  await redeemFnBob({ code, partnerDisplayName: 'Bob' });

  // Alice actualiza permisos: comparte fase y estado de ánimo, pero oculta síntomas y predicciones
  await updateDoc(doc(dbAlice, 'partner_links', linkId), {
    ownerConsentExplicitlyGranted: true,
    ownerSharePhase: true,
    ownerShareMood: true,
    ownerShareSymptoms: false,
    ownerSharePredictions: false,
    ownerSnapshot: {
      currentPhase: 'OVULATORY',
      currentMood: 'ENERGETIC',
      topSymptoms: [], // vacío porque no consintió compartir síntomas
      daysUntilNextPhase: 0,
      late: false,
      delayDays: 0,
      cycleStartDateMs: null,
      cycleLengthDays: 0,
      periodLengthDays: 0
    },
    updatedAtMs: Date.now()
  });

  // Bob lee el snapshot
  const linkForBob = await getDoc(doc(dbBob, 'partner_links', linkId));
  const snapshot = linkForBob.data().ownerSnapshot;
  assert.equal(snapshot.currentPhase, 'OVULATORY');
  assert.equal(snapshot.currentMood, 'ENERGETIC');
  assert.deepEqual(snapshot.topSymptoms, []);
  console.log('  ✓ Snapshot granular recibido correctamente por Bob respetando preferencias de Alice');
});

test('7. [GESTOS DE AFECTO] Intercambio bidireccional de gestos (Care Actions)', async () => {
  const linkId = `link_gestures_${Date.now()}`;
  const code = Math.floor(10000000 + Math.random() * 90000000).toString();
  const now = Date.now();

  await setDoc(doc(dbAlice, 'partner_links', linkId), {
    linkId,
    ownerUid: aliceUid,
    partnerUid: null,
    status: 'PENDING',
    linkCode: code,
    linkType: 'CYCLE_SYNC',
    createdAtMs: now
  });
  await setDoc(doc(dbAlice, 'partner_invitations', code), {
    code,
    linkId,
    ownerUid: aliceUid,
    status: 'PENDING',
    expiresAtMs: now + 48 * 3600 * 1000,
    createdAtMs: now
  });
  const redeemFnBob = httpsCallable(fnBob, 'redeemBondsInvitation');
  await redeemFnBob({ code, partnerDisplayName: 'Bob' });

  // Bob envía un abrazo (HUG)
  const hugTimestamp = Date.now();
  await updateDoc(doc(dbBob, 'partner_links', linkId), {
    lastPartnerCareAction: {
      action: 'HUG',
      message: 'Un abrazo con cariño',
      timestamp: hugTimestamp
    },
    updatedAtMs: hugTimestamp
  });

  // Alice recibe el gesto
  const readByAlice = await getDoc(doc(dbAlice, 'partner_links', linkId));
  assert.equal(readByAlice.data().lastPartnerCareAction.action, 'HUG');
  assert.equal(readByAlice.data().lastPartnerCareAction.message, 'Un abrazo con cariño');
  console.log('  ✓ Gesto HUG enviado por Bob y recibido en tiempo real por Alice');

  // Alice responde con un té (TEA)
  const teaTimestamp = Date.now();
  await updateDoc(doc(dbAlice, 'partner_links', linkId), {
    lastOwnerCareAction: {
      action: 'TEA',
      message: 'Te preparé una infusión caliente',
      timestamp: teaTimestamp
    },
    updatedAtMs: teaTimestamp
  });

  // Bob recibe la respuesta
  const readByBob = await getDoc(doc(dbBob, 'partner_links', linkId));
  assert.equal(readByBob.data().lastOwnerCareAction.action, 'TEA');
  assert.equal(readByBob.data().lastOwnerCareAction.message, 'Te preparé una infusión caliente');
  console.log('  ✓ Gesto TEA respondido por Alice y recibido en tiempo real por Bob');
});

test('8. [REVOCACIÓN Y BLOQUEO POST-DESVINCULACIÓN] Revocación atómica vía Cloud Function', async () => {
  const linkId = `link_revoke_${Date.now()}`;
  const code = Math.floor(10000000 + Math.random() * 90000000).toString();
  const now = Date.now();

  await setDoc(doc(dbAlice, 'partner_links', linkId), {
    linkId,
    ownerUid: aliceUid,
    partnerUid: null,
    status: 'PENDING',
    linkCode: code,
    linkType: 'CYCLE_SYNC',
    createdAtMs: now
  });
  await setDoc(doc(dbAlice, 'partner_invitations', code), {
    code,
    linkId,
    ownerUid: aliceUid,
    status: 'PENDING',
    expiresAtMs: now + 48 * 3600 * 1000,
    createdAtMs: now
  });
  const redeemFnBob = httpsCallable(fnBob, 'redeemBondsInvitation');
  await redeemFnBob({ code, partnerDisplayName: 'Bob' });

  // Bob revoca el vínculo mediante la Cloud Function real
  const revokeFnBob = httpsCallable(fnBob, 'revokeBondsLink');
  const revokeResult = await revokeFnBob({ linkId });
  assert.equal(revokeResult.data.success, true);
  console.log('  ✓ Vínculo revocado por Cloud Function revokeBondsLink');

  // Comprobación de seguridad: Ninguno de los dos puede leer el vínculo revocado desde cliente
  let bobReadBlocked = false;
  try {
    await getDoc(doc(dbBob, 'partner_links', linkId));
  } catch (err) {
    bobReadBlocked = true;
    assert.ok(err.code === 'permission-denied' || /permission-denied|PERMISSION_DENIED|false for 'get'/i.test(err.message));
  }
  assert.ok(bobReadBlocked, 'Lectura por Bob debe ser denegada cuando el vínculo es REVOKED');

  let aliceReadBlocked = false;
  try {
    await getDoc(doc(dbAlice, 'partner_links', linkId));
  } catch (err) {
    aliceReadBlocked = true;
    assert.ok(err.code === 'permission-denied' || /permission-denied|PERMISSION_DENIED|false for 'get'/i.test(err.message));
  }
  assert.ok(aliceReadBlocked, 'Lectura por Alice debe ser denegada cuando el vínculo es REVOKED');

  // Bob no puede escribir en el vínculo revocado
  let bobWriteBlocked = false;
  try {
    await updateDoc(doc(dbBob, 'partner_links', linkId), {
      lastPartnerCareAction: { action: 'FLOWERS', timestamp: Date.now() }
    });
  } catch (err) {
    bobWriteBlocked = true;
    assert.ok(err.code === 'permission-denied' || /permission-denied|PERMISSION_DENIED|false for 'update'/i.test(err.message));
  }
  assert.ok(bobWriteBlocked, 'Escritura en vínculo REVOKED debe ser rechazada');

  // Bob no puede reactivar el vínculo revocado
  let bobReactivateBlocked = false;
  try {
    await updateDoc(doc(dbBob, 'partner_links', linkId), {
      status: 'ACTIVE'
    });
  } catch (err) {
    bobReactivateBlocked = true;
    assert.ok(err.code === 'permission-denied' || /permission-denied|PERMISSION_DENIED|false for 'update'/i.test(err.message));
  }
  assert.ok(bobReactivateBlocked, 'Reactivación de vínculo REVOKED debe ser rechazada');

  console.log('  ✓ Bloqueo total post-revocación verificado en Firestore Rules');
});

after(async () => {
  await Promise.allSettled([
    deleteApp(appAlice),
    deleteApp(appBob),
    deleteApp(appCharlie)
  ]);
});

