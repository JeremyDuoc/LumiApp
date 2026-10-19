/**
 * Herramienta local del equipo Lumi. Requiere GOOGLE_APPLICATION_CREDENTIALS
 * o credenciales predeterminadas de aplicación de Google Cloud con permisos.
 * Uso: node moderate-voice.cjs <list|approve|reject|comments|approve-comment|reject-comment|reports|hide|status|reply> [id] [texto]
 * No subir credenciales al repositorio.
 */
const {initializeApp,applicationDefault}=require("firebase-admin/app");
const {getFirestore,FieldValue}=require("firebase-admin/firestore");
initializeApp({credential:applicationDefault(),projectId:process.env.GOOGLE_CLOUD_PROJECT || "lumi-7c924"});
const db=getFirestore(),[,,command,id,...rest]=process.argv;
const statuses=["Recibida","En revisión","Aceptada","En desarrollo","Disponible","No prevista"];
async function pending(collection) {
  const snap=await db.collection(collection).orderBy("createdAtMs","desc").limit(30).get();
  for(const d of snap.docs)console.log(d.id,JSON.stringify(d.data()));
}
async function idea(approve) {
  const ref=db.collection("voice_pending_ideas").doc(id);
  await db.runTransaction(async tx=>{
    const snap=await tx.get(ref);if(!snap.exists)throw Error("La propuesta ya fue revisada o no existe.");
    const p=snap.data();
    if(approve)tx.create(db.collection("voice_ideas").doc(id),{
      title:p.title,description:p.description,category:p.category,status:"Recibida",
      votesCount:0,commentsCount:0,visible:true,createdAtMs:p.createdAtMs,updatedAtMs:Date.now()
    });
    tx.update(db.collection("voice_my_submissions").doc(p.ownerUid).collection("items").doc(id),
      {status:approve?"Recibida":"No publicada",public:approve});
    tx.delete(ref);
  });
  console.log("Idea "+id+(approve?" aprobada":" rechazada"));
}
async function comment(approve) {
  const ref=db.collection("voice_pending_comments").doc(id);
  await db.runTransaction(async tx=>{
    const snap=await tx.get(ref);if(!snap.exists)throw Error("Comentario pendiente no encontrado.");
    const p=snap.data(),target=db.collection("voice_ideas").doc(p.ideaId),idea=await tx.get(target);
    if(approve){
      if(!idea.exists||idea.get("visible")!==true)throw Error("La idea no es pública.");
      tx.create(target.collection("comments").doc(id),{body:p.body,team:false,createdAtMs:p.createdAtMs});
      tx.update(target,{commentsCount:FieldValue.increment(1),updatedAtMs:Date.now()});
    }
    tx.delete(ref);
  });
  console.log("Comentario "+id+(approve?" aprobado":" rechazado"));
}
(async()=>{
  if(["approve","reject","approve-comment","reject-comment","hide","status","reply"].includes(command)
    &&(!id || !/^[\w-]{8,128}$/.test(id)))throw Error("Debes indicar un ID válido.");
  switch(command){
    case "list": await pending("voice_pending_ideas");break;
    case "comments":await pending("voice_pending_comments");break;
    case "reports":await pending("voice_reports");break;
    case "approve":await idea(true);break;
    case "reject":await idea(false);break;
    case "approve-comment":await comment(true);break;
    case "reject-comment":await comment(false);break;
    case "hide":await db.collection("voice_ideas").doc(id).update({visible:false,updatedAtMs:Date.now()});break;
    case "status":{
      const next=rest.join(" ");if(!statuses.includes(next))throw Error("Estado inválido.");
      await db.collection("voice_ideas").doc(id).update({status:next,updatedAtMs:Date.now()});break;
    }
    case "reply":{
      const body=rest.join(" ").trim();
      if(body.length<4||body.length>420)throw Error("Comentario oficial de 4 a 420 caracteres.");
      const ref=db.collection("voice_ideas").doc(id);
      await db.runTransaction(async tx=>{
        const snap=await tx.get(ref);
        if(!snap.exists||snap.get("visible")!==true)throw Error("Idea no publicada.");
        tx.create(ref.collection("comments").doc(),{body,team:true,createdAtMs:Date.now()});
        tx.update(ref,{commentsCount:FieldValue.increment(1),updatedAtMs:Date.now()});
      });
      break;
    }
    default:throw Error("Uso: list | approve ID | reject ID | comments | approve-comment ID | reject-comment ID | reports | hide ID | status ID ESTADO | reply ID MENSAJE");
  }
  console.log("Terminado.");
})().catch(err=>{console.error(err);process.exitCode=1});
