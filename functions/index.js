/**
 * Cloud Functions de Lumi Bonds / Modo Pareja.
 *
 * Incluye:
 *   1. Canje atómico y seguro de invitaciones con rate-limiting (redeemBondsInvitation).
 *   2. Revocación atómica que invalida snapshots y accesos (revokeBondsLink).
 *   3. Envío de notificaciones push FCM en eventos autorizados (onPartnerLinkUpdated).
 */
const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { getFirestore } = require("firebase-admin/firestore");
const { logger } = require("firebase-functions");

initializeApp();

// Umbral de "retraso significativo" en días
const UMBRAL_RETRASO = 3;

// ── Rate Limiting Helper ──────────────────────────────────────────────────────
const RATE_LIMIT_WINDOW_MS = 15 * 60 * 1000; // 15 minutos
const MAX_FAILED_ATTEMPTS = 5;

async function checkAndRecordFailedAttempt(db, uid) {
  if (!uid) return;
  const rateLimitRef = db.collection("_rate_limits").doc(`redeem_${uid}`);
  const now = Date.now();

  await db.runTransaction(async (transaction) => {
    const doc = await transaction.get(rateLimitRef);
    let attempts = [];
    if (doc.exists) {
      const data = doc.data();
      attempts = (data.attempts || []).filter((ts) => now - ts < RATE_LIMIT_WINDOW_MS);
    }
    attempts.push(now);
    transaction.set(rateLimitRef, { attempts, updatedAtMs: now });
  });
}

async function verifyRateLimit(db, uid) {
  if (!uid) return;
  const rateLimitRef = db.collection("_rate_limits").doc(`redeem_${uid}`);
  const doc = await rateLimitRef.get();
  if (doc.exists) {
    const now = Date.now();
    const attempts = (doc.data().attempts || []).filter((ts) => now - ts < RATE_LIMIT_WINDOW_MS);
    if (attempts.length >= MAX_FAILED_ATTEMPTS) {
      throw new HttpsError(
        "resource-exhausted",
        "Demasiados intentos fallidos. Por seguridad, espera 15 minutos antes de volver a intentar."
      );
    }
  }
}

async function resetRateLimit(db, uid) {
  if (!uid) return;
  try {
    await db.collection("_rate_limits").doc(`redeem_${uid}`).delete();
  } catch (_) {}
}

// ── 1. Canje Atómico de Invitaciones (Bonds / Secure Redeem) ──────────────────
exports.redeemBondsInvitation = onCall(
  { cors: true },
  async (request) => {
    if (!request.auth || !request.auth.uid) {
      throw new HttpsError("unauthenticated", "Se requiere una sesión autenticada para canjear invitaciones.");
    }

    const callerUid = request.auth.uid;
    const { code, partnerDisplayName, partnerAvatarId, partnerFcmToken } = request.data || {};

    if (!code || typeof code !== "string" || !code.trim()) {
      throw new HttpsError("invalid-argument", "El código de invitación es obligatorio.");
    }

    const cleanCode = code.trim();
    const db = getFirestore();

    // Comprobar abuso antes de procesar
    await verifyRateLimit(db, callerUid);

    try {
      const result = await db.runTransaction(async (transaction) => {
        const invRef = db.collection("partner_invitations").doc(cleanCode);
        const invDoc = await transaction.get(invRef);

        if (!invDoc.exists) {
          throw new HttpsError("not-found", "El código de invitación no existe.");
        }

        const invData = invDoc.data();

        if (invData.status !== "PENDING") {
          throw new HttpsError("failed-precondition", "La invitación ya fue utilizada o revocada.");
        }

        const now = Date.now();
        if (invData.expiresAtMs && invData.expiresAtMs < now) {
          throw new HttpsError("deadline-exceeded", "El código de invitación ha expirado.");
        }

        if (invData.ownerUid === callerUid) {
          throw new HttpsError("invalid-argument", "No puedes unirte a una invitación que tú misma creaste.");
        }

        const linkRef = db.collection("partner_links").doc(invData.linkId);
        const linkDoc = await transaction.get(linkRef);

        if (!linkDoc.exists) {
          throw new HttpsError("not-found", "El vínculo asociado a esta invitación no existe.");
        }

        const linkData = linkDoc.data();

        if (linkData.status !== "PENDING" || linkData.partnerUid != null) {
          throw new HttpsError("failed-precondition", "El vínculo ya no está disponible para conexión.");
        }

        if (linkData.ownerUid !== invData.ownerUid) {
          throw new HttpsError("permission-denied", "Inconsistencia de propiedad en el vínculo.");
        }

        // Marcar invitación como reclamada
        transaction.update(invRef, {
          status: "CLAIMED",
          claimedByUid: callerUid,
          claimedAtMs: now
        });

        // Activar el vínculo con defaults estrictos de privacidad en false (Opt-in)
        transaction.update(linkRef, {
          partnerUid: callerUid,
          partnerDisplayName: (typeof partnerDisplayName === "string" ? partnerDisplayName.slice(0, 50) : null),
          partnerAvatarId: (typeof partnerAvatarId === "string" ? partnerAvatarId.slice(0, 30) : null),
          partnerFcmToken: (typeof partnerFcmToken === "string" ? partnerFcmToken.slice(0, 500) : null),
          partnerSharePhase: false,
          partnerShareMood: false,
          partnerShareSymptoms: false,
          partnerSharePredictions: false,
          partnerConsentExplicitlyGranted: true,
          status: "ACTIVE",
          activatedAtMs: now,
          updatedAtMs: now
        });

        return {
          linkId: invData.linkId,
          ownerDisplayName: linkData.ownerDisplayName || ""
        };
      });

      // Éxito: limpiar contador de intentos fallidos
      await resetRateLimit(db, callerUid);

      return {
        success: true,
        linkId: result.linkId,
        ownerDisplayName: result.ownerDisplayName
      };
    } catch (err) {
      if (err instanceof HttpsError && (err.code === "not-found" || err.code === "deadline-exceeded" || err.code === "failed-precondition")) {
        await checkAndRecordFailedAttempt(db, callerUid);
      }
      throw err;
    }
  }
);

// ── 2. Revocación Atómica y Segura (Bonds / Secure Revoke) ─────────────────────
exports.revokeBondsLink = onCall(
  { cors: true },
  async (request) => {
    if (!request.auth || !request.auth.uid) {
      throw new HttpsError("unauthenticated", "Se requiere una sesión autenticada para revocar vínculos.");
    }

    const callerUid = request.auth.uid;
    const { linkId } = request.data || {};

    if (!linkId || typeof linkId !== "string" || !linkId.trim()) {
      throw new HttpsError("invalid-argument", "El identificador del vínculo es obligatorio.");
    }

    const db = getFirestore();
    const linkRef = db.collection("partner_links").doc(linkId.trim());

    await db.runTransaction(async (transaction) => {
      const linkDoc = await transaction.get(linkRef);
      if (!linkDoc.exists) {
        throw new HttpsError("not-found", "El vínculo no existe.");
      }

      const linkData = linkDoc.data();
      if (linkData.ownerUid !== callerUid && linkData.partnerUid !== callerUid) {
        throw new HttpsError("permission-denied", "No tienes permisos para revocar este vínculo.");
      }

      if (linkData.status === "REVOKED") {
        return; // Idempotente
      }

      const now = Date.now();
      // Invalidar inmediatamente snapshots compartidos de salud y tokens
      transaction.update(linkRef, {
        status: "REVOKED",
        revokedByUid: callerUid,
        revokedAtMs: now,
        ownerSnapshot: null,
        partnerSnapshot: null,
        ownerFcmToken: null,
        partnerFcmToken: null,
        updatedAtMs: now
      });

      // Si existía una invitación pendiente asociada, cancelarla
      if (linkData.linkCode) {
        const invRef = db.collection("partner_invitations").doc(linkData.linkCode);
        transaction.set(invRef, { status: "REVOKED", revokedAtMs: now }, { merge: true });
      }
    });

    return { success: true };
  }
);

// ── 3. Disparador de Notificaciones Push (FCM) ────────────────────────────────
exports.onPartnerLinkUpdated = onDocumentUpdated(
  "partner_links/{linkId}",
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    if (!before || !after || after.status !== "ACTIVE") return;

    const linkId = event.params.linkId;
    const envios = [];

    // Detalles afectivos (care actions)
    envios.push(
      notificarDetalle(
        linkId,
        before.lastOwnerCareAction,
        after.lastOwnerCareAction,
        after.partnerFcmToken,
        "partnerFcmToken",
        after.ownerDisplayName
      )
    );
    envios.push(
      notificarDetalle(
        linkId,
        before.lastPartnerCareAction,
        after.lastPartnerCareAction,
        after.ownerFcmToken,
        "ownerFcmToken",
        after.partnerDisplayName
      )
    );

    // Retraso significativo
    envios.push(
      notificarRetraso(
        linkId,
        before.ownerSnapshot,
        after.ownerSnapshot,
        after.ownerSharePredictions,
        after.partnerFcmToken,
        "partnerFcmToken",
        after.ownerDisplayName
      )
    );
    envios.push(
      notificarRetraso(
        linkId,
        before.partnerSnapshot,
        after.partnerSnapshot,
        after.partnerSharePredictions,
        after.ownerFcmToken,
        "ownerFcmToken",
        after.partnerDisplayName
      )
    );

    await Promise.allSettled(envios);
  }
);

async function notificarDetalle(linkId, antes, despues, tokenDestino, campoToken, nombreRemitente) {
  if (!despues || !tokenDestino || typeof tokenDestino !== "string" || !tokenDestino.trim()) return;
  const timestampAntes = antes ? antes.timestamp : 0;
  if (despues.timestamp === timestampAntes) return;

  try {
    await getMessaging().send({
      token: tokenDestino.trim(),
      data: {
        type: "CARE_ACTION",
        action: String(despues.action || "HUG"),
        senderName: String(nombreRemitente || ""),
      },
      android: { priority: "high" },
    });
  } catch (e) {
    logger.warn(`No se pudo enviar el Detalle: ${e.message}`);
    await limpiarTokenInvalidoSiAplica(linkId, campoToken, e);
  }
}

async function notificarRetraso(linkId, antes, despues, compartePredicciones, tokenDestino, campoToken, nombre) {
  if (!despues || !tokenDestino || typeof tokenDestino !== "string" || !tokenDestino.trim() || compartePredicciones === false) return;

  const retrasoAntes = antes && antes.late ? antes.delayDays || 0 : 0;
  const retrasoAhora = despues.late ? despues.delayDays || 0 : 0;

  const cruzaUmbral = retrasoAntes < UMBRAL_RETRASO && retrasoAhora >= UMBRAL_RETRASO;
  if (!cruzaUmbral) return;

  try {
    await getMessaging().send({
      token: tokenDestino.trim(),
      data: {
        type: "PERIOD_LATE",
        name: String(nombre || ""),
        delayDays: String(retrasoAhora),
      },
      android: { priority: "high" },
    });
  } catch (e) {
    logger.warn(`No se pudo enviar el aviso de retraso: ${e.message}`);
    await limpiarTokenInvalidoSiAplica(linkId, campoToken, e);
  }
}

async function limpiarTokenInvalidoSiAplica(linkId, campoToken, error) {
  if (!linkId || !campoToken) return;
  const errorCode = error && (error.code || (error.errorInfo && error.errorInfo.code));
  if (
    errorCode === "messaging/registration-token-not-registered" ||
    errorCode === "messaging/invalid-registration-token" ||
    errorCode === "messaging/invalid-argument"
  ) {
    try {
      logger.info(`Limpiando token expirado (${campoToken}) en partner_links/${linkId}`);
      await getFirestore().collection("partner_links").doc(linkId).update({
        [campoToken]: null,
      });
    } catch (dbErr) {
      logger.error(`Error al limpiar token en Firestore: ${dbErr.message}`);
    }
  }
}
