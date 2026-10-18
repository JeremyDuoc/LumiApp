/**
 * Lumi · Tu voz: solo sugerencias de producto, nunca datos de salud.
 * Lectura pública de contenido aprobado; mutaciones con Admin SDK.
 */
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const CATEGORIES = ["Calendario","Registro diario","Apariencia","Privacidad","Recordatorios","Otro"];
const STATUSES = ["Recibida","En revisión","Aceptada","En desarrollo","Disponible","No prevista"];
const SENSITIVE_PATTERN = /(?:https?:\/\/|www\.|@[\w.-]+\.\w{2,}|\+?\d[\d\s()-]{7,}\d)/i;
const REGION = "us-central1";
function caller(req) {
  if (!req.auth?.uid) throw new HttpsError("unauthenticated","Inicia la sesión anónima de Tu voz.");
  return req.auth.uid;
}
function moderator(req) {
  caller(req);
  if (req.auth.token?.lumiVoiceModerator !== true)
    throw new HttpsError("permission-denied","Solo el equipo puede moderar.");
}
function clean(value, max, min = 1) {
  if (typeof value !== "string") throw new HttpsError("invalid-argument","Texto inválido.");
  const text = value.replace(/\s+/g," ").trim();
  if (text.length < min || text.length > max) throw new HttpsError("invalid-argument","Longitud permitida: "+min+"-"+max+".");
  if (SENSITIVE_PATTERN.test(text)) throw new HttpsError("invalid-argument","No publiques enlaces, correos ni teléfonos.");
  return text;
}
function docId(value) {
  if (typeof value !== "string" || !/^[A-Za-z0-9_-]{8,128}$/.test(value))
    throw new HttpsError("invalid-argument","Identificador inválido.");
  return value;
}
async function rateLimited(tx, db, uid, field, windowMs) {
  const ref = db.collection("voice_limits").doc(uid);
  const snap = await tx.get(ref);
  const now = Date.now();
  if (now - (Number(snap.get(field)) || 0) < windowMs)
    throw new HttpsError("resource-exhausted","Espera un momento antes de volver a publicar.");
  tx.set(ref,{[field]:now},{merge:true});
}
exports.submitVoiceIdea = onCall({region:REGION},async(req)=>{
  const uid = caller(req), db = getFirestore();
  const title = clean(req.data?.title,95,8), description = clean(req.data?.description,650,20);
  const category = req.data?.category;
  if (!CATEGORIES.includes(category)) throw new HttpsError("invalid-argument","Categoría inválida.");
  const ref = db.collection("voice_pending_ideas").doc(), now=Date.now();
  await db.runTransaction(async tx=>{
    await rateLimited(tx,db,uid,"lastIdeaAt",5*60*1000);
    tx.create(ref,{title,description,category,ownerUid:uid,createdAtMs:now});
    tx.set(db.collection("voice_my_submissions").doc(uid).collection("items").doc(ref.id),
       {title,description,category,status:"Pendiente de revisión",createdAtMs:now,public:false});
  });
  return {id:ref.id,submitted:true};
});
exports.voteVoiceIdea = onCall({region:REGION},async(req)=>{
  const uid=caller(req),db=getFirestore(),id=docId(req.data?.ideaId);
  const idea=db.collection("voice_ideas").doc(id),vote=db.collection("voice_votes").doc(uid).collection("items").doc(id);
  return db.runTransaction(async tx=>{
    const [post,prior]=await Promise.all([tx.get(idea),tx.get(vote)]);
    if(!post.exists || post.get("visible")!==true)throw new HttpsError("not-found","La idea ya no está disponible.");
    await rateLimited(tx,db,uid,"lastVoteAt",1000);
    const voted=!prior.exists, before=Number(post.get("votesCount"))||0;
    if(voted) tx.create(vote,{createdAtMs:Date.now()}); else tx.delete(vote);
    tx.update(idea,{votesCount:Math.max(0,before+(voted?1:-1))});
    return {voted,votesCount:Math.max(0,before+(voted?1:-1))};
  });
});
exports.submitVoiceComment = onCall({region:REGION},async(req)=>{
  const uid=caller(req),db=getFirestore(),id=docId(req.data?.ideaId),body=clean(req.data?.body,420,4);
  const idea=await db.collection("voice_ideas").doc(id).get();
  if(!idea.exists || idea.get("visible")!==true)throw new HttpsError("not-found","La idea no está disponible.");
  const ref=db.collection("voice_pending_comments").doc();
  await db.runTransaction(async tx=>{
    await rateLimited(tx,db,uid,"lastCommentAt",30*1000);
    tx.create(ref,{ideaId:id,body,ownerUid:uid,createdAtMs:Date.now()});
  });
  return {submitted:true};
});
exports.reportVoiceIdea = onCall({region:REGION},async(req)=>{
  const uid=caller(req),db=getFirestore(),id=docId(req.data?.ideaId),reason=clean(req.data?.reason,160,4);
  await db.runTransaction(async tx=>{
    const target=await tx.get(db.collection("voice_ideas").doc(id));
    if(!target.exists || target.get("visible")!==true)throw new HttpsError("not-found","Publicación no disponible.");
    await rateLimited(tx,db,uid,"lastReportAt",60*1000);
    tx.set(db.collection("voice_reports").doc(uid+"_"+id),{ideaId:id,reporterUid:uid,reason,createdAtMs:Date.now()});
  });
  return {submitted:true};
});
exports.reviewVoiceIdea = onCall({region:REGION},async(req)=>{
  moderator(req);
  const db=getFirestore(),id=docId(req.data?.ideaId),approve=req.data?.approve===true;
  return db.runTransaction(async tx=>{
    const pending=db.collection("voice_pending_ideas").doc(id),snap=await tx.get(pending);
    if(!snap.exists)throw new HttpsError("not-found","Propuesta pendiente no encontrada.");
    const p=snap.data(),own=db.collection("voice_my_submissions").doc(p.ownerUid).collection("items").doc(id);
    if(approve) tx.create(db.collection("voice_ideas").doc(id),{
      title:p.title,description:p.description,category:p.category,status:"Recibida",
      votesCount:0,commentsCount:0,visible:true,createdAtMs:p.createdAtMs,updatedAtMs:Date.now()
    });
    tx.update(own,{status:approve?"Recibida":"No publicada",public:approve});
    tx.delete(pending);
    return {approved:approve};
  });
});
exports.reviewVoiceComment = onCall({region:REGION},async(req)=>{
  moderator(req);
  const db=getFirestore(),id=docId(req.data?.commentId),approve=req.data?.approve===true;
  return db.runTransaction(async tx=>{
    const pending=db.collection("voice_pending_comments").doc(id),snap=await tx.get(pending);
    if(!snap.exists)throw new HttpsError("not-found","Comentario pendiente no encontrado.");
    const comment=snap.data(),idea=db.collection("voice_ideas").doc(comment.ideaId),item=await tx.get(idea);
    if(approve && (!item.exists || item.get("visible")!==true))
      throw new HttpsError("failed-precondition","La idea no está publicada.");
    if(approve){
      tx.create(idea.collection("comments").doc(id),{body:comment.body,team:false,createdAtMs:comment.createdAtMs});
      tx.update(idea,{commentsCount:FieldValue.increment(1),updatedAtMs:Date.now()});
    }
    tx.delete(pending);
    return {approved:approve};
  });
});
exports.setVoiceIdeaStatus = onCall({region:REGION},async(req)=>{
  moderator(req);
  const db=getFirestore(),id=docId(req.data?.ideaId),status=req.data?.status;
  if(!STATUSES.includes(status))throw new HttpsError("invalid-argument","Estado inválido.");
  const ref=db.collection("voice_ideas").doc(id);
  await db.runTransaction(async tx=>{
    if(!(await tx.get(ref)).exists)throw new HttpsError("not-found","Idea no encontrada.");
    tx.update(ref,{status,updatedAtMs:Date.now()});
  });
  return {status};
});
exports.hideVoiceIdea = onCall({region:REGION},async(req)=>{
  moderator(req);
  const ref=getFirestore().collection("voice_ideas").doc(docId(req.data?.ideaId));
  await ref.update({visible:false,updatedAtMs:Date.now()});
  return {hidden:true};
});
exports._voice = {clean,docId,CATEGORIES,STATUSES};
