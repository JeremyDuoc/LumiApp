const { before, after, beforeEach, test } = require('node:test');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { doc, setDoc, updateDoc, getDoc, collection, query, where, getDocs } = require('firebase/firestore');

let env;

const pendingLinkData = {
  linkId: 'link_test_1',
  ownerUid: 'owner_alice',
  partnerUid: null,
  status: 'PENDING',
  linkCode: '88776655',
  linkType: 'CYCLE_SYNC',
  createdAtMs: Date.now(),
  ownerSnapshot: null,
  partnerSnapshot: null,
  ownerFcmToken: null,
  partnerFcmToken: null,
  ownerSharePhase: false,
  ownerShareMood: false,
  ownerShareSymptoms: false,
  ownerSharePredictions: false,
  partnerSharePhase: false,
  partnerShareMood: false,
  partnerShareSymptoms: false,
  partnerSharePredictions: false,
  ownerConsentExplicitlyGranted: false,
  partnerConsentExplicitlyGranted: false
};

const activeLinkData = {
  ...pendingLinkData,
  partnerUid: 'partner_bob',
  status: 'ACTIVE',
  ownerConsentExplicitlyGranted: true,
  partnerConsentExplicitlyGranted: true
};

const linkDocRef = (uid, linkId = 'link_test_1') =>
  doc(env.authenticatedContext(uid).firestore(), `partner_links/${linkId}`);

const invDocRef = (uid, code = '88776655') =>
  doc(env.authenticatedContext(uid).firestore(), `partner_invitations/${code}`);

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-lumi-remediation-test',
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
  await env.withSecurityRulesDisabled(async (ctx) => {
    // Vínculo 1 (PENDING)
    await setDoc(doc(ctx.firestore(), 'partner_links/link_test_1'), pendingLinkData);
    // Invitación del vínculo 1
    await setDoc(doc(ctx.firestore(), 'partner_invitations/88776655'), {
      code: '88776655',
      linkId: 'link_test_1',
      ownerUid: 'owner_alice',
      status: 'PENDING',
      expiresAtMs: Date.now() + 86400000,
      createdAtMs: Date.now()
    });

    // Vínculo 2 (ACTIVE entre Alice y Bob)
    await setDoc(doc(ctx.firestore(), 'partner_links/link_active'), {
      ...activeLinkData,
      linkId: 'link_active',
      ownerSnapshot: {
        currentPhase: 'UNKNOWN',
        currentMood: null,
        topSymptoms: [],
        daysUntilNextPhase: 0,
        late: false,
        delayDays: 0,
        cycleStartDateMs: null,
        cycleLengthDays: 0,
        periodLengthDays: 0
      }
    });

    // Vínculo 3 (ACTIVE entre Alice y Charlie)
    await setDoc(doc(ctx.firestore(), 'partner_links/link_charlie'), {
      ...activeLinkData,
      linkId: 'link_charlie',
      partnerUid: 'partner_charlie'
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 1. SEC-NEW-01: Prohibición de activación directa desde el cliente
// ─────────────────────────────────────────────────────────────────────────────
test('[SEC-NEW-01 NEGATIVO] Intento de cliente de activar un vínculo PENDING directamente con linkId conocido debe fallar', async () => {
  const attacker = 'attacker_mallory';
  await assertFails(
    updateDoc(linkDocRef(attacker, 'link_test_1'), {
      partnerUid: attacker,
      status: 'ACTIVE'
    })
  );
});

// ─────────────────────────────────────────────────────────────────────────────
// 2. P0-2: Bloqueo estricto de lectura tras revocación
// ─────────────────────────────────────────────────────────────────────────────
test('[P0-2 NEGATIVO] Usuario revocado NO puede leer partner_links tras revocación', async () => {
  // 1. Simular revocación segura en DB
  await env.withSecurityRulesDisabled(async (ctx) => {
    await updateDoc(doc(ctx.firestore(), 'partner_links/link_active'), {
      status: 'REVOKED',
      ownerSnapshot: null,
      partnerSnapshot: null,
      ownerFcmToken: null,
      partnerFcmToken: null
    });
  });

  // 2. Bob (expareja) intenta hacer getDoc
  await assertFails(getDoc(linkDocRef('partner_bob', 'link_active')));
  // 3. Alice (dueña) tampoco puede leer un documento REVOKED directamente desde cliente
  await assertFails(getDoc(linkDocRef('owner_alice', 'link_active')));
});

// ─────────────────────────────────────────────────────────────────────────────
// 3. P1-5: Protección anti-enumeración de invitaciones
// ─────────────────────────────────────────────────────────────────────────────
test('[P1-5 NEGATIVO] Extraño no puede leer directamente partner_invitations/{code}', async () => {
  const stranger = 'stranger_eve';
  await assertFails(getDoc(invDocRef(stranger, '88776655')));
});

test('[P1-5 POSITIVO] Solo el dueño de la invitación puede consultar su propio código', async () => {
  await assertSucceeds(getDoc(invDocRef('owner_alice', '88776655')));
});

// ─────────────────────────────────────────────────────────────────────────────
// 4. Integridad de Privacidad por Defecto (Opt-In Estricto)
// ─────────────────────────────────────────────────────────────────────────────
test('[P0-4 NEGATIVO] No se permite crear un vínculo con permisos en true por defecto', async () => {
  await assertFails(
    setDoc(linkDocRef('owner_alice', 'link_insecure'), {
      ...pendingLinkData,
      linkId: 'link_insecure',
      ownerSharePhase: true, // Violación de Privacy by Default
      ownerShareMood: true
    })
  );
});

test('[P0-4 POSITIVO] Creación válida con privacidad en false por defecto', async () => {
  await assertSucceeds(
    setDoc(linkDocRef('owner_alice', 'link_secure'), {
      ...pendingLinkData,
      linkId: 'link_secure'
    })
  );
});

// ─────────────────────────────────────────────────────────────────────────────
// 5. Inmutabilidad y Aislamiento de Identidades y Campos Ajenos
// ─────────────────────────────────────────────────────────────────────────────
test('[INTEGRIDAD NEGATIVO] Bob no puede alterar los permisos de Alice', async () => {
  await assertFails(
    updateDoc(linkDocRef('partner_bob', 'link_active'), {
      ownerSharePhase: true,
      updatedAtMs: Date.now()
    })
  );
});

test('[INTEGRIDAD NEGATIVO] Bob no puede suplantar la identidad o avatar de Alice', async () => {
  await assertFails(
    updateDoc(linkDocRef('partner_bob', 'link_active'), {
      ownerDisplayName: 'Hacked By Bob',
      updatedAtMs: Date.now()
    })
  );
});

test('[INTEGRIDAD POSITIVO] Bob puede actualizar sus propios datos permitidos', async () => {
  await assertSucceeds(
    updateDoc(linkDocRef('partner_bob', 'link_active'), {
      partnerDisplayName: 'Roberto',
      partnerSharePhase: false,
      partnerConsentExplicitlyGranted: true,
      updatedAtMs: Date.now()
    })
  );
});

// ─────────────────────────────────────────────────────────────────────────────
// 6. Prohibición de reactivación de vínculos revocados
// ─────────────────────────────────────────────────────────────────────────────
test('[REVERSIÓN NEGATIVO] No se puede reactivar un vínculo en estado REVOKED', async () => {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await updateDoc(doc(ctx.firestore(), 'partner_links/link_active'), {
      status: 'REVOKED'
    });
  });

  await assertFails(
    updateDoc(linkDocRef('owner_alice', 'link_active'), {
      status: 'ACTIVE',
      updatedAtMs: Date.now()
    })
  );
});

// ─────────────────────────────────────────────────────────────────────────────
// 7. Revocación atómica válida desde cliente
// ─────────────────────────────────────────────────────────────────────────────
test('[REVOCACIÓN POSITIVO] Revocación cliente autorizada vaciando snapshots y tokens', async () => {
  await assertSucceeds(
    updateDoc(linkDocRef('partner_bob', 'link_active'), {
      status: 'REVOKED',
      ownerSnapshot: null,
      partnerSnapshot: null,
      ownerFcmToken: null,
      partnerFcmToken: null,
      revokedAtMs: Date.now(),
      revokedByUid: 'partner_bob',
      updatedAtMs: Date.now()
    })
  );
});

// ─────────────────────────────────────────────────────────────────────────────
// 8. Aislamiento entre tres cuentas independientes (Alice, Bob, Charlie)
// ─────────────────────────────────────────────────────────────────────────────
test('[AISLAMIENTO TRIPLE NEGATIVO] Bob no puede leer el vínculo entre Alice y Charlie', async () => {
  await assertFails(getDoc(linkDocRef('partner_bob', 'link_charlie')));
});

test('[AISLAMIENTO TRIPLE NEGATIVO] Charlie no puede leer el vínculo entre Alice y Bob', async () => {
  await assertFails(getDoc(linkDocRef('partner_charlie', 'link_active')));
});

// ─────────────────────────────────────────────────────────────────────────────
// 9. SEC-P0-A: Bloqueo de inferencia indirecta de ciclo en servidor (validSnapshot)
// ─────────────────────────────────────────────────────────────────────────────
test('[SEC-P0-A NEGATIVO] Prohibir transmisión de cycleStartDateMs cuando ownerSharePhase es false, incluso si ownerSharePredictions es true', async () => {
  await assertFails(
    updateDoc(linkDocRef('owner_alice', 'link_active'), {
      ownerSharePhase: false,
      ownerSharePredictions: true,
      ownerSnapshot: {
        currentPhase: 'UNKNOWN',
        currentMood: null,
        topSymptoms: [],
        daysUntilNextPhase: 0,
        late: false,
        delayDays: 0,
        cycleStartDateMs: 1700000000000, // VIOLACIÓN: intento de inferir ciclo indirectamente
        cycleLengthDays: 0,
        periodLengthDays: 0
      },
      updatedAtMs: Date.now()
    })
  );
});

test('[SEC-P0-A NEGATIVO] Prohibir transmisión de cycleLengthDays cuando ownerSharePhase es false', async () => {
  await assertFails(
    updateDoc(linkDocRef('owner_alice', 'link_active'), {
      ownerSharePhase: false,
      ownerSharePredictions: true,
      ownerSnapshot: {
        currentPhase: 'UNKNOWN',
        currentMood: null,
        topSymptoms: [],
        daysUntilNextPhase: 0,
        late: false,
        delayDays: 0,
        cycleStartDateMs: null,
        cycleLengthDays: 28, // VIOLACIÓN
        periodLengthDays: 0
      },
      updatedAtMs: Date.now()
    })
  );
});

test('[SEC-P0-A NEGATIVO] Prohibir transmisión de periodLengthDays cuando ownerSharePhase es false', async () => {
  await assertFails(
    updateDoc(linkDocRef('owner_alice', 'link_active'), {
      ownerSharePhase: false,
      ownerSharePredictions: true,
      ownerSnapshot: {
        currentPhase: 'UNKNOWN',
        currentMood: null,
        topSymptoms: [],
        daysUntilNextPhase: 0,
        late: false,
        delayDays: 0,
        cycleStartDateMs: null,
        cycleLengthDays: 0,
        periodLengthDays: 5 // VIOLACIÓN
      },
      updatedAtMs: Date.now()
    })
  );
});

test('[SEC-P0-B NEGATIVO] Prohibir transmisión de datos de salud si ownerConsentExplicitlyGranted es false', async () => {
  await assertFails(
    updateDoc(linkDocRef('owner_alice', 'link_active'), {
      ownerConsentExplicitlyGranted: false,
      ownerSharePhase: true,
      ownerSnapshot: {
        currentPhase: 'FOLLICULAR', // VIOLACIÓN: sin consentimiento explícito
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
});

test('[SEC-P0-A POSITIVO] Transmisión autorizada con consentimiento explícito y permisos alineados', async () => {
  await assertSucceeds(
    updateDoc(linkDocRef('owner_alice', 'link_active'), {
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
});

