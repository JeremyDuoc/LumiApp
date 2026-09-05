/**
 * Cloud Functions del Modo Pareja de Lumi.
 *
 * Envía notificaciones push (FCM) cuando cambia un documento de
 * `partner_links`:
 *   1. Alguien envió un Detalle (care action) → avisar a la otra persona.
 *   2. El periodo se retrasó de forma significativa → avisar al vínculo
 *      (solo si esa persona comparte predicciones).
 *
 * Los textos visibles se resuelven EN EL CLIENTE (strings.xml): aquí solo
 * viajan mensajes de datos, para respetar el idioma y el formato de la app.
 *
 * Despliegue:  firebase deploy --only functions
 */
const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { getFirestore } = require("firebase-admin/firestore");
const { logger } = require("firebase-functions");

initializeApp();

// Umbral de "retraso significativo" en días
const UMBRAL_RETRASO = 3;

exports.onPartnerLinkUpdated = onDocumentUpdated(
  "partner_links/{linkId}",
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    if (!before || !after || after.status !== "ACTIVE") return;

    const linkId = event.params.linkId;
    const envios = [];

    // ── 1. Detalles ──────────────────────────────────────────────────────
    // La titular envió → notificar a la pareja
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
    // La pareja envió → notificar a la titular
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

    // ── 2. Retraso significativo ─────────────────────────────────────────
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

/** Notifica un Detalle nuevo (compara timestamps para detectar el cambio). */
async function notificarDetalle(linkId, antes, despues, tokenDestino, campoToken, nombreRemitente) {
  if (!despues || !tokenDestino || typeof tokenDestino !== "string" || !tokenDestino.trim()) return;
  const timestampAntes = antes ? antes.timestamp : 0;
  if (despues.timestamp === timestampAntes) return; // sin cambio

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

/**
 * Notifica cuando el retraso CRUZA el umbral (no en cada actualización,
 * para no bombardear con el mismo aviso).
 */
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

/** Limpia el token de Firestore si FCM reporta que ya no es válido o está desregistrado */
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
