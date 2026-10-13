// Review-only tests: assertions describe the required behavior, not current behavior.
const { before, after, beforeEach, test } = require('node:test');
const { createRequire } = require('node:module');
const { resolve } = require('node:path');
const { readFileSync } = require('node:fs');
const projectRequire = createRequire(resolve(process.cwd(), 'tools/firestore-tests/package.json'));
const { initializeTestEnvironment, assertSucceeds, assertFails } = projectRequire('@firebase/rules-unit-testing');
const { doc, setDoc, updateDoc, getDoc, runTransaction } = projectRequire('firebase/firestore');
let env;
before(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-lumi-audit', firestore: {
    host: '127.0.0.1', port: 8180, rules: readFileSync('firestore.rules', 'utf8')
  } });
});
after(async () => { await env?.cleanup(); });
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx => {
    await setDoc(doc(ctx.firestore(), 'partner_links/link'), {
      linkId: 'link', ownerUid: 'owner', partnerUid: null, status: 'PENDING',
      ownerSnapshot: null, partnerSnapshot: null
    });
    await setDoc(doc(ctx.firestore(), 'partner_invitations/123456'), {
      code: '123456', ownerUid: 'owner', linkId: 'link', status: 'PENDING',
      expiresAtMs: Date.now() + 86400000
    });
  });
});
test('SEC-01: the transaction used by the Android client must complete', async () => {
  const db = env.authenticatedContext('joiner').firestore();
  const invitationRef = doc(db, 'partner_invitations/123456');
  const linkRef = doc(db, 'partner_links/link');
  await getDoc(invitationRef);
  await assertSucceeds(runTransaction(db, async transaction => {
    await transaction.get(invitationRef);
    transaction.update(invitationRef, { status: 'CLAIMED', claimedByUid: 'joiner', claimedAtMs: Date.now() });
    await transaction.get(linkRef);
    transaction.update(linkRef, { partnerUid: 'joiner', status: 'ACTIVE' });
  }));
});
test('SEC-02: joining with only a link id and without claiming an invitation must be denied', async () => {
  const db = env.authenticatedContext('stranger').firestore();
  await assertFails(updateDoc(doc(db, 'partner_links/link'), { partnerUid: 'stranger', status: 'ACTIVE' }));
});
test('SEC-03: claiming an invitation as a different uid must be denied', async () => {
  const db = env.authenticatedContext('stranger').firestore();
  await assertFails(updateDoc(doc(db, 'partner_invitations/123456'), {
    status: 'CLAIMED', claimedByUid: 'victim', claimedAtMs: Date.now()
  }));
});
test('SEC-04: a known link id must not bypass invitation expiry', async () => {
  await env.withSecurityRulesDisabled(async ctx => {
    await updateDoc(doc(ctx.firestore(), 'partner_invitations/123456'), { expiresAtMs: 1 });
  });
  const db = env.authenticatedContext('stranger').firestore();
  await assertFails(updateDoc(doc(db, 'partner_links/link'), { partnerUid: 'stranger', status: 'ACTIVE' }));
});

