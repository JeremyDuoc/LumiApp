const {before,after,beforeEach,test}=require("node:test");
const {readFileSync}=require("node:fs");
const path=require("node:path");
const {initializeTestEnvironment,assertSucceeds,assertFails}=require("@firebase/rules-unit-testing");
const {doc,setDoc,updateDoc,getDoc,getDocs,query,where,collection}=require("firebase/firestore");
let env;
const idea={title:"Mejorar calendario",description:"Un calendario semanal",category:"Calendario",
  status:"Recibida",visible:true,votesCount:3,commentsCount:0,createdAtMs:100,updatedAtMs:100};
before(async()=>{
  env=await initializeTestEnvironment({projectId:"demo-lumi-voice",
    firestore:{host:"127.0.0.1",port:8080,rules:readFileSync(path.join(__dirname,"../../firestore.rules"),"utf8")}});
});
after(async()=>{await env?.cleanup()});
beforeEach(async()=>{
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async ctx=>{
    const db=ctx.firestore();
    await setDoc(doc(db,"voice_ideas/approved123"),idea);
    await setDoc(doc(db,"voice_ideas/hidden123"),{...idea,visible:false});
    await setDoc(doc(db,"voice_votes/alice/items/approved123"),{createdAtMs:100});
    await setDoc(doc(db,"voice_my_submissions/alice/items/approved123"),
      {title:idea.title,category:idea.category,status:"Recibida",public:true,createdAtMs:100});
    await setDoc(doc(db,"voice_ideas/approved123/comments/comment01"),
      {body:"Estoy de acuerdo",team:false,createdAtMs:101});
    await setDoc(doc(db,"voice_pending_ideas/pending123"),{title:"private",ownerUid:"alice"});
    await setDoc(doc(db,"voice_reports/report123"),{reporterUid:"alice"});
  });
});
test("authenticated users may read visible ideas and approved comments",async()=>{
  const db=env.authenticatedContext("bob").firestore();
  await assertSucceeds(getDoc(doc(db,"voice_ideas/approved123")));
  await assertSucceeds(getDocs(query(collection(db,"voice_ideas"),where("visible","==",true))));
  await assertSucceeds(getDocs(collection(db,"voice_ideas/approved123/comments")));
});
test("hidden posts and moderation queues are private",async()=>{
  const db=env.authenticatedContext("bob").firestore();
  await assertFails(getDoc(doc(db,"voice_ideas/hidden123")));
  await assertFails(getDocs(collection(db,"voice_ideas")));
  await assertFails(getDoc(doc(db,"voice_pending_ideas/pending123")));
  await assertFails(getDoc(doc(db,"voice_reports/report123")));
  await assertFails(getDocs(collection(db,"voice_ideas/hidden123/comments")));
});
test("users cannot manipulate public vote counts or inject an idea",async()=>{
  const db=env.authenticatedContext("alice").firestore();
  await assertFails(updateDoc(doc(db,"voice_ideas/approved123"),{votesCount:999999}));
  await assertFails(setDoc(doc(db,"voice_ideas/evil12345"),{...idea,visible:true}));
  await assertFails(setDoc(doc(db,"voice_ideas/approved123/comments/evil12345"),
    {body:"spam",team:true}));
  await assertFails(setDoc(doc(db,"voice_votes/alice/items/evil12345"),{createdAtMs:1}));
});
test("votes and authored ideas are readable only by owner",async()=>{
  const alice=env.authenticatedContext("alice").firestore();
  const bob=env.authenticatedContext("bob").firestore();
  await assertSucceeds(getDoc(doc(alice,"voice_votes/alice/items/approved123")));
  await assertSucceeds(getDocs(collection(alice,"voice_my_submissions/alice/items")));
  await assertFails(getDoc(doc(bob,"voice_votes/alice/items/approved123")));
  await assertFails(getDocs(collection(bob,"voice_my_submissions/alice/items")));
});
test("unauthenticated viewers cannot enumerate public ideas",async()=>{
  const db=env.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(db,"voice_ideas/approved123")));
});
