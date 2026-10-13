const { before, after, beforeEach, test } = require('node:test');
const { readFileSync } = require('node:fs');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { doc, setDoc, updateDoc, getDoc, collection, query, where, getDocs } = require('firebase/firestore');

const path = require('node:path');

let env;
const pending = {
  linkId: 'link_1', ownerUid: 'owner_A', partnerUid: null, status: 'PENDING',
  linkCode: '654321', linkType: 'CYCLE_SYNC', createdAtMs: Date.now(),
  ownerSnapshot: { currentPhase: 'FOLLICULAR', currentMood: 'Sensitive', topSymptoms: [], cycleStartDateMs: Date.now() - 864000000, cycleLengthDays: 28, periodLengthDays: 5 },
  partnerSnapshot: null,
  ownerSharePhase: true, ownerShareMood: true, ownerShareSymptoms: false, ownerSharePredictions: true,
  partnerSharePhase: true, partnerShareMood: true, partnerShareSymptoms: false, partnerSharePredictions: true
};

const linkRef = (uid, linkId = 'link_1') => doc(env.authenticatedContext(uid).firestore(), `partner_links/${linkId}`);

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-lumi-audit-deep', firestore: {
      host: '127.0.0.1', port: 8180,
      rules: readFileSync(path.join(__dirname, '../../firestore.rules'), 'utf8')
    }
  });
});

after(async () => { await env?.cleanup(); });

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx => {
    await setDoc(doc(ctx.firestore(), 'partner_links/link_1'), pending);
    await setDoc(doc(ctx.firestore(), 'partner_invitations/654321'), {
      code: '654321',
      linkId: 'link_1',
      ownerUid: 'owner_A',
      status: 'PENDING',
      expiresAtMs: Date.now() + 86400000,
      createdAtMs: Date.now()
    });
  });
});

// ── VULNERABILIDAD P0-2: Lectura permitida tras revocación ─────────────────────
test('[P0-2 CONFIRMADO] Un participante revocado TODAVÍA puede leer los datos de salud del documento', async () => {
  // 1. Joiner se une y activa el vínculo
  await assertSucceeds(updateDoc(linkRef('joiner_B'), {
    partnerUid: 'joiner_B',
    status: 'ACTIVE'
  }));

  // 2. Set snapshot
  await env.withSecurityRulesDisabled(async ctx => {
    await updateDoc(doc(ctx.firestore(), 'partner_links/link_1'), {
      ownerSnapshot: { currentPhase: 'FOLLICULAR', currentMood: 'Sensitive', topSymptoms: [] }
    });
  });

  // 3. Owner revoca el vínculo
  await assertSucceeds(updateDoc(linkRef('owner_A'), {
    status: 'REVOKED'
  }));

  // 4. Verificamos que el documento sigue existiendo y tiene status REVOKED
  let adminDoc;
  await env.withSecurityRulesDisabled(async ctx => {
    const snap = await getDoc(doc(ctx.firestore(), 'partner_links/link_1'));
    adminDoc = snap.data();
  });
  if (!adminDoc || adminDoc.status !== 'REVOKED') throw new Error(`El status es ${adminDoc?.status}`);

  // 5. COMPROBACIÓN CRÍTICA: ¿Las reglas permiten que joiner_B (ahora revocado) siga leyendo el documento?
  // SÍ: allow get/list solo comprueba si partnerUid == auth.uid sin comprobar status == 'ACTIVE'
  const readResult = await assertSucceeds(getDoc(linkRef('joiner_B')));
  const leakedData = readResult.data();

  // 6. Evidencia: los datos médicos y el snapshot siguen expuestos tras la revocación
  if (!leakedData.ownerSnapshot || leakedData.ownerSnapshot.currentPhase !== 'FOLLICULAR') {
    throw new Error('Fallo al recuperar snapshot filtrado');
  }
});

// ── VULNERABILIDAD P0-3: Transacción de canje bloqueada ─────────────────────────
test('[P0-3 CONFIRMADO] El futuro participante NO puede hacer get(linkRef) mientras está PENDING', async () => {
  // En PartnerRepositoryImpl.kt:268, la transacción ejecuta:
  // val currentLink = transaction.get(linkRef)
  // Sin embargo, las reglas de partner_links líneas 28-31 exigen:
  // resource.data.ownerUid == request.auth.uid || resource.data.partnerUid == request.auth.uid
  // Como partnerUid aún es null, el get directo falla con PERMISSION_DENIED
  await assertFails(getDoc(linkRef('future_joiner')));
});

// ── VULNERABILIDAD P1-5: Adivinanza de código expone linkId y owner ─────────────
test('[P1-5 CONFIRMADO] Cualquier usuario autenticado que acierte el código de 6 dígitos obtiene el linkId y ownerUid', async () => {
  const dbStranger = env.authenticatedContext('attacker_stranger').firestore();
  const invitationDoc = await assertSucceeds(getDoc(doc(dbStranger, 'partner_invitations/654321')));
  const invData = invitationDoc.data();
  if (invData.linkId !== 'link_1' || invData.ownerUid !== 'owner_A') {
    throw new Error('No se expuso linkId o ownerUid');
  }
});

// ── CASO MULTI-VÍNCULO (FASE E): Aislamiento entre vínculos ────────────────────
test('[FASE E - AISLAMIENTO] El participante B de un vínculo no puede leer el vínculo de C con el mismo titular', async () => {
  // Crear vínculo entre owner_A y user_C
  await env.withSecurityRulesDisabled(async ctx => {
    await setDoc(doc(ctx.firestore(), 'partner_links/link_2_with_C'), {
      ...pending,
      linkId: 'link_2_with_C',
      partnerUid: 'user_C',
      status: 'ACTIVE'
    });
  });

  // Activar link_1 con joiner_B
  await assertSucceeds(updateDoc(linkRef('joiner_B', 'link_1'), {
    partnerUid: 'joiner_B',
    status: 'ACTIVE'
  }));

  // joiner_B intenta leer link_2_with_C -> DEBE SER RECHAZADO (PERMISSION_DENIED)
  await assertFails(getDoc(linkRef('joiner_B', 'link_2_with_C')));
});

// ── INTEGRIDAD DE GESTOS: No se puede suplantar el remitente del gesto ─────────
test('[INTEGRIDAD GESTOS] Joiner no puede modificar lastOwnerCareAction para suplantar al owner', async () => {
  await assertSucceeds(updateDoc(linkRef('joiner_B'), {
    partnerUid: 'joiner_B',
    status: 'ACTIVE'
  }));

  // joiner_B intenta escribir en lastOwnerCareAction -> DEBE FALLAR
  await assertFails(updateDoc(linkRef('joiner_B'), {
    lastOwnerCareAction: { action: 'HUG', timestamp: Date.now() }
  }));
});
