const { before, after, beforeEach, test } = require('node:test');
const { readFileSync } = require('node:fs');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { doc, setDoc, updateDoc, getDoc, getDocs, collection, query, where, limit } = require('firebase/firestore');
const path = require('node:path');
let env;
const pending = {
  linkId: 'link', ownerUid: 'owner', partnerUid: null, status: 'PENDING',
  linkCode: '123456', linkType: 'CYCLE_SYNC', createdAtMs: 1,
  ownerSnapshot: null, partnerSnapshot: null,
  ownerSharePhase: true, ownerShareMood: true, ownerShareSymptoms: false, ownerSharePredictions: true,
  partnerSharePhase: true, partnerShareMood: true, partnerShareSymptoms: false, partnerSharePredictions: true
};
const ref = uid => doc(env.authenticatedContext(uid).firestore(), 'partner_links/link');
before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-lumi-bonds', firestore: {
      host: '127.0.0.1', port: 8080,
      rules: readFileSync(path.join(__dirname, '../../firestore.rules'), 'utf8')
    }
  });
});
after(async () => { await env?.cleanup(); });
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx => {
    await setDoc(doc(ctx.firestore(), 'partner_links/link'), pending);
    await setDoc(doc(ctx.firestore(), 'partner_invitations/123456'), {
      code: '123456',
      linkId: 'link',
      ownerUid: 'owner',
      status: 'PENDING',
      expiresAtMs: Date.now() + 86400000,
      createdAtMs: Date.now()
    });
  });
});
test('owner can create a pending invitation', async () => {
  await assertSucceeds(setDoc(doc(env.authenticatedContext('owner').firestore(), 'partner_links/new'), { ...pending, linkId: 'new' }));
});
test('cannot create an active link impersonating a partner', async () => {
  await assertFails(setDoc(doc(env.authenticatedContext('owner').firestore(), 'partner_links/new'), { ...pending, linkId: 'new', partnerUid: 'victim', status: 'ACTIVE' }));
});
test('joining cannot replace the owner', async () => {
  await assertFails(updateDoc(ref('joiner'), { ownerUid: 'joiner', partnerUid: 'joiner', status: 'ACTIVE' }));
});
test('owner cannot join their own link', async () => {
  await assertFails(updateDoc(ref('owner'), { partnerUid: 'owner', status: 'ACTIVE' }));
});
test('join, edit own privacy and revoke remain available', async () => {
  await assertSucceeds(updateDoc(ref('joiner'), { partnerUid: 'joiner', status: 'ACTIVE' }));
  await assertSucceeds(updateDoc(ref('joiner'), { partnerShareMood: false, partnerSnapshot: null }));
  await assertSucceeds(updateDoc(ref('owner'), { ownerDisplayName: 'Test' }));
  await assertSucceeds(updateDoc(ref('joiner'), { status: 'REVOKED' }));
  await assertFails(updateDoc(ref('owner'), { status: 'ACTIVE' }));
});
test('a participant cannot edit the other participants privacy or identity', async () => {
  await updateDoc(ref('joiner'), { partnerUid: 'joiner', status: 'ACTIVE' });
  await assertFails(updateDoc(ref('joiner'), { ownerShareSymptoms: true }));
  await assertFails(updateDoc(ref('owner'), { partnerUid: 'someone-else' }));
});
test('a second joiner cannot take over an active invitation', async () => {
  await updateDoc(ref('joiner'), { partnerUid: 'joiner', status: 'ACTIVE' });
  await assertFails(updateDoc(ref('second'), { partnerUid: 'second', status: 'ACTIVE' }));
  await assertFails(getDoc(ref('second')));
});

test('snapshot fields must be cleared when consent is withdrawn', async () => {
  await updateDoc(ref('joiner'), { partnerUid: 'joiner', status: 'ACTIVE' });
  await assertSucceeds(updateDoc(ref('owner'), { ownerSnapshot: { currentMood: 'Test', topSymptoms: [] } }));
  await assertFails(updateDoc(ref('owner'), { ownerShareMood: false }));
  await assertSucceeds(updateDoc(ref('owner'), { ownerShareMood: false, ownerSnapshot: { currentMood: null, topSymptoms: [] } }));
  await assertFails(updateDoc(ref('owner'), { ownerSnapshot: { currentMood: null, topSymptoms: ['Test'] } }));
  await assertFails(updateDoc(ref('owner'), { ownerSharePredictions: false, ownerSnapshot: { currentMood: null, topSymptoms: [], cycleLengthDays: 28 } }));
});

test('LUM-01 RESOLVED: stranger CANNOT discover or list pending links without code', async () => {
  const db = env.authenticatedContext('stranger').firestore();
  await assertFails(getDocs(query(collection(db, 'partner_links'), where('status', '==', 'PENDING'), limit(1))));
  await assertFails(getDocs(collection(db, 'partner_invitations')));
  await assertFails(getDoc(ref('stranger')));
});

test('LUM-01 RESOLVED: joining requires the explicit invitation code and can claim atomically', async () => {
  const db = env.authenticatedContext('joiner').firestore();
  const invDoc = await assertSucceeds(getDoc(doc(db, 'partner_invitations/123456')));
  const linkId = invDoc.data().linkId;
  await assertSucceeds(updateDoc(invDoc.ref, { status: 'CLAIMED', claimedByUid: 'joiner', claimedAtMs: Date.now() }));
  await assertSucceeds(updateDoc(doc(db, `partner_links/${linkId}`), { partnerUid: 'joiner', status: 'ACTIVE' }));
  await assertSucceeds(getDoc(doc(db, `partner_links/${linkId}`)));
});
