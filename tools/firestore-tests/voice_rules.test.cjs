"use strict";
const { before, after, test } = require("node:test");
const { readFileSync } = require("node:fs");
const { join } = require("node:path");
const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { collection, doc, getDoc, getDocs, query, where, limit, setDoc } = require("firebase/firestore");

let env;
const projectId = "lumi-voice-rules-test";
const publicIdea = {
  title: "Calendario mejorado", description: "Una prueba de lectura de la comunidad",
  category: "Calendario", status: "Recibida",
  votes: 0, commentsCount: 0, visible: true, createdAtMs: 1, updatedAtMs: 1
};
before(async () => {
  env = await initializeTestEnvironment({
    projectId,
    firestore: {
      host: "127.0.0.1", port: 8080,
      rules: readFileSync(join(__dirname, "../../firestore.rules"), "utf8")
    }
  });
  await env.withSecurityRulesDisabled(async ctx => {
    const db = ctx.firestore();
    await setDoc(doc(db, "voice_ideas", "publicIdea"), publicIdea);
    await setDoc(doc(db, "voice_ideas", "hiddenIdea"), {...publicIdea, visible: false});
    await setDoc(doc(db, "voice_ideas", "publicIdea", "comments", "comment1"),
      {text: "Comentario compartido", team: false, createdAtMs: 1});
    await setDoc(doc(db, "voice_user_votes", "alice", "ideas", "publicIdea"),
      {createdAtMs: 1});
    await setDoc(doc(db, "voice_internal_owners", "publicIdea"),
      {uid: "alice"});
  });
});
after(async () => { if(env) await env.cleanup(); });

test("Authenticated readers see visible suggestions and comments", async () => {
  const db = env.authenticatedContext("bob").firestore();
  await assertSucceeds(getDocs(query(collection(db, "voice_ideas"),
    where("visible", "==", true), limit(80))));
  await assertSucceeds(getDoc(doc(db, "voice_ideas", "publicIdea")));
  await assertSucceeds(getDocs(query(collection(db, "voice_ideas", "publicIdea", "comments"), limit(80))));
});
test("Anonymous unauthenticated requests do not access the forum", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db, "voice_ideas", "publicIdea")));
  await assertFails(getDocs(query(collection(db, "voice_ideas"),
    where("visible", "==", true), limit(80))));
});
test("Invisible ideas do not leak and listing requires a limit and visible filter", async () => {
  const db = env.authenticatedContext("bob").firestore();
  await assertFails(getDoc(doc(db, "voice_ideas", "hiddenIdea")));
  await assertFails(getDocs(collection(db, "voice_ideas")));
});
test("Vote receipts are visible only to the owner", async () => {
  const alice = env.authenticatedContext("alice").firestore();
  const bob = env.authenticatedContext("bob").firestore();
  await assertSucceeds(getDoc(doc(alice, "voice_user_votes", "alice", "ideas", "publicIdea")));
  await assertFails(getDoc(doc(bob, "voice_user_votes", "alice", "ideas", "publicIdea")));
});
test("Clients cannot fabricate posts, votes, comments, roles, or reports", async () => {
  const db = env.authenticatedContext("alice").firestore();
  await assertFails(setDoc(doc(db, "voice_ideas", "forged"), publicIdea));
  await assertFails(setDoc(doc(db, "voice_ideas", "publicIdea", "comments", "forged"), {text:"hi"}));
  await assertFails(setDoc(doc(db, "voice_user_votes", "alice", "ideas", "forged"), {createdAtMs:2}));
  await assertFails(setDoc(doc(db, "voice_internal_owners", "forged"), {uid:"alice"}));
  await assertFails(setDoc(doc(db, "voice_internal_reports", "forged"), {uid:"alice"}));
});
