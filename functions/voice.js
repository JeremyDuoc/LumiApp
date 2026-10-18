"use strict";
/**
 * Tu voz: public suggestions only. DO NOT import or transmit health, cycle,
 * backup, Bonds, or device-profile data. Only callables write these collections.
 */
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { getFirestore } = require("firebase-admin/firestore");

const CATEGORIES = new Set([
  "Calendario", "Registro diario", "Apariencia", "Privacidad",
  "Recordatorios", "Otro", "Seguimiento", "Diseño", "Accesibilidad", "Otros"
]);
const STATES = new Set([
  "Recibida", "En revisión", "Aceptada", "En desarrollo", "Disponible", "No prevista"
]);
const nowMs = () => Date.now();
const cleanText = (value, field, min, max) => {
  if (typeof value !== "string") throw new HttpsError("invalid-argument", field + " inválido");
  const text = value.trim();
  if (text.length < min || text.length > max || /[\u0000-\u0008\u000E-\u001F]/.test(text)) {
    throw new HttpsError("invalid-argument", field + " fuera de los límites permitidos");
  }
  return text;
};
const requireUser = (req) => {
  const uid = req.auth && req.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Activa una sesión de Firebase para participar.");
  return uid;
};
const validateId = (id) => {
  if (typeof id !== "string" || !/^[a-zA-Z0-9_-]{8,128}$/.test(id)) {
    throw new HttpsError("invalid-argument", "Identificador de propuesta inválido");
  }
  return id;
};
const limitConfig = {
  create: { windowMs: 24 * 60 * 60 * 1000, count: 5, gapMs: 60 * 1000 },
  comment: { windowMs: 60 * 60 * 1000, count: 35, gapMs: 5 * 1000 },
  vote: { windowMs: 60 * 60 * 1000, count: 70, gapMs: 500 },
  report: { windowMs: 24 * 60 * 60 * 1000, count: 10, gapMs: 15 * 1000 },
  status: { windowMs: 60 * 60 * 1000, count: 100, gapMs: 0 }
};
async function readLimit(tx, db, uid, action) {
  const ref = db.collection("voice_internal_limits").doc(uid + "_" + action);
  const snap = await tx.get(ref);
  const cfg = limitConfig[action];
  const now = nowMs();
  const value = snap.exists ? snap.data() : {};
  const history = (Array.isArray(value.history) ? value.history : [])
    .filter(ts => Number.isFinite(ts) && ts > now - cfg.windowMs).slice(-cfg.count);
  if (history.length >= cfg.count || (history.length && now - history[history.length - 1] < cfg.gapMs)) {
    throw new HttpsError("resource-exhausted", "Demasiadas acciones seguidas. Espera un momento.");
  }
  return { ref, history, now };
}
function stampLimit(tx, limit) {
  tx.set(limit.ref, { history: limit.history.concat([limit.now]), lastAtMs: limit.now });
}
async function createIdeaCore({db, uid, data}) {
  const title = cleanText(data.title, "Título", 8, 100);
  const description = cleanText(data.description, "Descripción", 15, 900);
  const category = data.category;
  if (!CATEGORIES.has(category)) throw new HttpsError("invalid-argument", "Categoría no válida");
  const ideaRef = db.collection("voice_ideas").doc();
  const ownerRef = db.collection("voice_internal_owners").doc(ideaRef.id);
  await db.runTransaction(async tx => {
    const limit = await readLimit(tx, db, uid, "create");
    tx.create(ideaRef, {
      title, description, category, status: "Recibida",
      votes: 0, commentsCount: 0, visible: true,
      createdAtMs: limit.now, updatedAtMs: limit.now
    });
    tx.create(ownerRef, { uid, createdAtMs: limit.now });
    stampLimit(tx, limit);
  });
  return {id: ideaRef.id};
}
async function voteCore({db, uid, data}) {
  const id = validateId(data.id);
  const idea = db.collection("voice_ideas").doc(id);
  const vote = db.collection("voice_user_votes").doc(uid).collection("ideas").doc(id);
  return db.runTransaction(async tx => {
    const [ideaSnap, voteSnap, limit] = await Promise.all([
      tx.get(idea), tx.get(vote), readLimit(tx, db, uid, "vote")
    ]);
    if (!ideaSnap.exists || ideaSnap.get("visible") !== true) {
      throw new HttpsError("not-found", "La idea ya no está disponible");
    }
    const wasVoted = voteSnap.exists;
    const amount = Math.max(0, Number(ideaSnap.get("votes") || 0) + (wasVoted ? -1 : 1));
    tx.update(idea, {votes: amount, updatedAtMs: limit.now});
    if (wasVoted) tx.delete(vote);
    else tx.create(vote, {createdAtMs: limit.now});
    stampLimit(tx, limit);
    return {voted: !wasVoted, votes: amount};
  });
}
async function addCommentCore({db, uid, data}) {
  const id = validateId(data.id);
  const text = cleanText(data.text, "Comentario", 3, 500);
  const idea = db.collection("voice_ideas").doc(id);
  const comment = idea.collection("comments").doc();
  return db.runTransaction(async tx => {
    const [ideaSnap, limit] = await Promise.all([
      tx.get(idea), readLimit(tx, db, uid, "comment")
    ]);
    if (!ideaSnap.exists || ideaSnap.get("visible") !== true) {
      throw new HttpsError("not-found", "La idea ya no está disponible");
    }
    tx.create(comment, {text, team: false, createdAtMs: limit.now});
    tx.update(idea, {
      commentsCount: Number(ideaSnap.get("commentsCount") || 0) + 1,
      updatedAtMs: limit.now
    });
    stampLimit(tx, limit);
    return {id: comment.id};
  });
}
async function reportCore({db, uid, data}) {
  const id = validateId(data.id);
  const reason = cleanText(data.reason || "Contenido inapropiado", "Motivo", 3, 200);
  const idea = db.collection("voice_ideas").doc(id);
  const report = db.collection("voice_internal_reports").doc(id + "_" + uid);
  return db.runTransaction(async tx => {
    const [ideaSnap, reportSnap, limit] = await Promise.all([
      tx.get(idea), tx.get(report), readLimit(tx, db, uid, "report")
    ]);
    if (!ideaSnap.exists) throw new HttpsError("not-found", "La idea no está disponible");
    if (reportSnap.exists) throw new HttpsError("already-exists", "Ya reportaste esta propuesta");
    tx.create(report, {ideaId: id, reporterUid: uid, reason, createdAtMs: limit.now, state:"open"});
    stampLimit(tx, limit);
    return {reported: true};
  });
}
async function statusCore({db, uid, claims, data}) {
  if (claims.voiceAdmin !== true) throw new HttpsError("permission-denied", "Solo el equipo verificado puede cambiar estados");
  const id = validateId(data.id);
  const status = data.status;
  if (!STATES.has(status)) throw new HttpsError("invalid-argument", "Estado no permitido");
  const idea = db.collection("voice_ideas").doc(id);
  return db.runTransaction(async tx => {
    const [doc, limit] = await Promise.all([tx.get(idea), readLimit(tx, db, uid, "status")]);
    if (!doc.exists) throw new HttpsError("not-found", "No existe la idea");
    tx.update(idea, {status, updatedAtMs: limit.now});
    stampLimit(tx, limit);
    return {status};
  });
}
const callable = (run) => onCall({cors: true, timeoutSeconds: 15}, async request => {
  const uid = requireUser(request);
  return run({db: getFirestore(), uid, claims: request.auth.token, data: request.data || {}});
});
exports.voiceCreateIdea = callable(createIdeaCore);
exports.voiceToggleVote = callable(voteCore);
exports.voiceAddComment = callable(addCommentCore);
exports.voiceReportIdea = callable(reportCore);
exports.voiceSetIdeaStatus = callable(statusCore);

// Pure functions exported only from this module for node tests; not deployed as callables.
exports._test = {CATEGORIES, STATES, cleanText, validateId};
