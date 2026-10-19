const {before,after,beforeEach,test}=require("node:test");
const {readFileSync}=require("node:fs");
const assert=require("node:assert/strict");
const path=require("node:path");
const {initializeTestEnvironment,assertSucceeds,assertFails}=require("@firebase/rules-unit-testing");
const {
  doc,setDoc,updateDoc,getDoc,getDocs,runTransaction,writeBatch,
  query,where,collection
}=require("firebase/firestore");

let env;
const idea={
  title:"Mejorar calendario",description:"Un calendario semanal de uso diario",
  category:"Calendario",status:"Recibida",visible:true,
  votesCount:3,commentsCount:1,createdAtMs:100,updatedAtMs:100
};
const ideaRef=db=>doc(db,"voice_ideas/approved123");
const voteRef=(db,uid)=>doc(db,"voice_votes/"+uid+"/items/approved123");
const now=()=>Date.now();
const payload=uid=>({
  title:"Agregar widgets nuevos",
  description:"Quisiera un widget con información visible.",
  category:"Apariencia",ownerUid:uid,createdAtMs:now()
});
before(async()=>{
  env=await initializeTestEnvironment({projectId:"demo-lumi-voice-spark",
    firestore:{host:"127.0.0.1",port:8080,
      rules:readFileSync(path.join(__dirname,"../../firestore.rules"),"utf8")}});
});
after(async()=>{await env?.cleanup()});
beforeEach(async()=>{
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx=>{
    const db=ctx.firestore();
    await setDoc(doc(db,"voice_ideas/approved123"),idea);
    await setDoc(doc(db,"voice_ideas/hidden123"),{...idea,visible:false});
    await setDoc(doc(db,"voice_ideas/approved123/comments/comment01"),{
      body:"Estoy de acuerdo",team:false,createdAtMs:101
    });
    await setDoc(doc(db,"voice_votes/alice/items/other12345"),{active:true});
    await setDoc(doc(db,"voice_pending_ideas/hiddenQueue123"),{
      title:"Solo moderación",ownerUid:"alice"
    });
  });
});
test("lectura pública de ideas aprobadas, no de ideas ocultas",async()=>{
  const db=env.authenticatedContext("bob").firestore();
  await assertSucceeds(getDoc(ideaRef(db)));
  await assertSucceeds(getDocs(query(collection(db,"voice_ideas"),where("visible","==",true))));
  await assertSucceeds(getDocs(collection(db,"voice_ideas/approved123/comments")));
  await assertFails(getDoc(doc(db,"voice_ideas/hidden123")));
  await assertFails(getDocs(collection(db,"voice_ideas")));
  await assertFails(getDoc(doc(db,"voice_pending_ideas/hiddenQueue123")));
  await assertFails(getDocs(collection(db,"voice_ideas/hidden123/comments")));
  await assertFails(getDoc(ideaRef(env.unauthenticatedContext().firestore())));
});
test("crear propuesta exige las dos copias en un mismo batch",async()=>{
  const db=env.authenticatedContext("alice").firestore(),p=payload("alice");
  const pending=doc(db,"voice_pending_ideas/newIdea123"),own=doc(db,"voice_my_submissions/alice/items/newIdea123");
  const batch=writeBatch(db);
  batch.set(pending,p);
  batch.set(own,{
    title:p.title,description:p.description,category:p.category,
    status:"Pendiente de revisión",public:false,createdAtMs:p.createdAtMs
  });
  await assertSucceeds(batch.commit());
  await assertSucceeds(getDoc(own));
  await assertFails(getDoc(pending)); // Solo el administrador puede leer pendientes.
  const bad=doc(db,"voice_pending_ideas/incomplete123");
  await assertFails(setDoc(bad,p)); // No hay copia de estado privado.
});
test("no se permite crear ideas con UID ajeno o campos falsificados",async()=>{
  const db=env.authenticatedContext("alice").firestore(),p=payload("bob");
  const pending=doc(db,"voice_pending_ideas/impersonated123"),own=doc(db,"voice_my_submissions/alice/items/impersonated123");
  const batch=writeBatch(db);
  batch.set(pending,p);
  batch.set(own,{title:p.title,description:p.description,category:p.category,
    status:"Pendiente de revisión",public:false,createdAtMs:p.createdAtMs});
  await assertFails(batch.commit());
  await assertFails(setDoc(doc(db,"voice_ideas/malicious123"),{...idea,visible:true}));
  await assertFails(updateDoc(ideaRef(db),{status:"En desarrollo"}));
  await assertFails(updateDoc(ideaRef(db),{votesCount:999999}));
  await assertFails(setDoc(doc(db,"voice_votes/alice/items/approved123"),{active:true}));
});
async function toggle(uid,active){
  const db=env.authenticatedContext(uid).firestore();
  const res=await assertSucceeds(runTransaction(db,async tx=>{
    const post=await tx.get(ideaRef(db));
    const vote=await tx.get(voteRef(db,uid));
    const was=vote.exists() && vote.data().active===true;
    assert.equal(was,!active);
    if(vote.exists())tx.update(voteRef(db,uid),{active});
    else tx.set(voteRef(db,uid),{active});
    tx.update(ideaRef(db),{votesCount:post.data().votesCount+(active?1:-1)});
    return active;
  }));
  return res;
}
test("voto+contador son atómicos; quitar y repetir voto conserva integridad",async()=>{
  await toggle("bob",true);
  await toggle("bob",false);
  await toggle("bob",true);
  const db=env.authenticatedContext("alice").firestore();
  const post=await assertSucceeds(getDoc(ideaRef(db)));
  assert.equal(post.data().votesCount,4);
  await assertFails(getDoc(voteRef(db,"bob")));
  await assertSucceeds(getDoc(voteRef(db,"alice"))); // Existente de otra idea no tiene acceso ajeno.
});
test("comentarios nuevos solo pueden entrar a moderación",async()=>{
  const db=env.authenticatedContext("alice").firestore();
  await assertSucceeds(setDoc(doc(db,"voice_pending_comments/newComment123"),{
    ideaId:"approved123",ownerUid:"alice",body:"Me gustaría esta mejora.",createdAtMs:now()
  }));
  await assertFails(getDoc(doc(db,"voice_pending_comments/newComment123")));
  await assertFails(setDoc(doc(db,"voice_ideas/approved123/comments/forged123"),{
    body:"Soy el administrador",team:true,createdAtMs:now()
  }));
  await assertFails(setDoc(doc(db,"voice_pending_comments/privateComment123"),{
    ideaId:"hidden123",ownerUid:"alice",body:"Mensaje no permitido",createdAtMs:now()
  }));
});
test("reportes auténticos son privados e inmutables",async()=>{
  const db=env.authenticatedContext("bob").firestore();
  const ref=doc(db,"voice_reports/bob_approved123");
  await assertSucceeds(setDoc(ref,{
    ideaId:"approved123",reporterUid:"bob",reason:"Posible spam",createdAtMs:now()
  }));
  await assertFails(getDoc(ref));
  await assertFails(updateDoc(ref,{reason:"Alteración"}));
  await assertFails(setDoc(doc(db,"voice_reports/alice_approved123"),{
    ideaId:"approved123",reporterUid:"alice",reason:"Suplantación",createdAtMs:now()
  }));
});
