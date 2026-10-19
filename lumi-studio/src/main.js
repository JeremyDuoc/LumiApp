import { createClient } from '@supabase/supabase-js'
import './style.css'

const URL = import.meta.env.VITE_SUPABASE_URL
const KEY = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY
const app = document.querySelector('#app')
const supabase = URL && KEY ? createClient(URL, KEY, {
  auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true }
}) : null

const STEPS = ['Recibida', 'En revisión', 'Aceptada', 'En desarrollo', 'Disponible', 'No prevista']
const TABS = [
  { key:'pendientes',label:'Bandeja de entrada',icon:'inbox' },
  { key:'roadmap',label:'Hoja de ruta',icon:'route' },
  { key:'comments',label:'Comentarios',icon:'message' },
  { key:'reports',label:'Reportes',icon:'shield' },
]
const SYMBOLS = {
  inbox:'<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 13h5l2 3h4l2-3h5"/>',
  route:'<circle cx="6" cy="18" r="2"/><circle cx="18" cy="6" r="2"/><path d="M8 18h6a4 4 0 0 0 4-4v-6M6 16V6h8"/>',
  message:'<path d="M21 11.5a8.4 8.4 0 0 1-8.5 8.5 9 9 0 0 1-4-.9L3 21l1.9-5.5a9 9 0 0 1-.9-4A8.4 8.4 0 0 1 12.5 3 8.4 8.4 0 0 1 21 11.5Z"/>',
  shield:'<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z"/><path d="M12 8v5m0 3h.01"/>',
  refresh:'<path d="M3 12a9 9 0 0 1 15-6l3 3M21 3v6h-6M21 12a9 9 0 0 1-15 6l-3-3M3 21v-6h6"/>',
  check:'<path d="m5 12 4 4L19 6"/>',
  x:'<path d="M18 6 6 18M6 6l12 12"/>',
  arrow:'<path d="m9 18 6-6-6-6"/>',
  circle:'<circle cx="12" cy="12" r="9"/>',
  lock:'<rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 1 1 8 0v3"/>',
  logout:'<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>',
  eye:'<path d="M2 12s3.8-7 10-7 10 7 10 7-3.8 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>',
  eyeoff:'<path d="m3 3 18 18M10.6 5.1A12 12 0 0 1 12 5c6.2 0 10 7 10 7a17 17 0 0 1-3.1 3.8M6.4 6.4C3.7 8.1 2 12 2 12s3.8 7 10 7a10 10 0 0 0 4.3-.9"/>',
  send:'<path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>',
  spark:'<path d="m12 2 2.7 6.3L21 11l-6.3 2.7L12 20l-2.7-6.3L3 11l6.3-2.7L12 2Z"/>',
  search:'<circle cx="11" cy="11" r="8"/><path d="m21 21-4.4-4.4"/>',
  clock:'<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  external:'<path d="M13 5h6v6M19 5l-9 9M19 13v6H5V5h6"/>',
}
const svg = (name, size=18) => '<svg width="'+size+'" height="'+size+'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'+(SYMBOLS[name]||SYMBOLS.circle)+'</svg>'
const clean = x => String(x ?? '').replace(/[&<>"']/g, a => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[a]))
const fmt = n => n ? new Intl.DateTimeFormat('es-CL',{dateStyle:'medium',timeStyle:'short'}).format(new Date(Number(n))) : 'Sin fecha'
const cut = (s, max=130) => String(s||'').length>max ? String(s).slice(0,max)+'…' : String(s||'')
const badge = status => '<span class="status status--'+(['En desarrollo','Disponible','Aceptada'].includes(status)?'active':status==='No prevista'||status==='No publicada'?'muted':'pending')+'"><span class="status-dot"></span>'+clean(status)+'</span>'
const state = {
  session:null, authorized:false, checking:true, fetching:false, busy:false,
  tab:'pendientes', query:'', dialog:null, error:null,
  data:{ submissions:[], ideas:[], pendingComments:[], comments:[], reports:[] },
  draftStatus:{}, changedAt:null
}
let realtimeChannel = null
let poll = null
let toastTimer = null

function toast(message, type='success') {
  clearTimeout(toastTimer)
  let area = document.querySelector('#toast-region')
  if (!area) {
    area=document.createElement('div');area.id='toast-region';area.setAttribute('role','status')
    document.body.append(area)
  }
  area.textContent=message
  area.className='toast show '+(type==='error'?'toast-error':'')
  toastTimer=setTimeout(()=>area.classList.remove('show'),4800)
}
function loadingBlock(text='Cargando Lumi Studio…') {
  return '<div class="full-loading"><div class="logo-orbit"></div><div><b>Lumi Studio</b><p>'+clean(text)+'</p></div></div>'
}
function loginView(mode='login') {
  const blocked=mode==='blocked'
  return '<main class="auth-page"><div class="auth-light"></div><section class="auth-panel">'+
   '<div class="brand auth-brand"><span class="brand-mark">'+svg('spark',24)+'</span><span>Lumi <em>Studio</em></span></div>'+
   '<p class="eyebrow">ESPACIO PRIVADO · TU VOZ</p>'+
   '<h1>'+(blocked?'Sin acceso de moderación':'Un espacio para escuchar.')+'</h1>'+
   '<p class="lead">'+(blocked?'Tu cuenta no figura entre los administradores autorizados de Lumi. No puedes consultar ni modificar las sugerencias.':'El lugar donde las propuestas de la comunidad se convierten en mejoras reales.')+'</p>'+
   (blocked?'<div class="notice"><span>'+svg('lock')+'</span><span>El acceso se concede por UID desde Supabase, nunca desde el navegador.</span></div><button class="btn primary wide" data-action="logout">Cerrar sesión</button>':
    '<form id="login-form" class="login-form"><label>Correo administrativo<input name="email" type="email" placeholder="tu@correo.com" autocomplete="email" required /></label>'+
    '<label>Contraseña<input name="password" type="password" placeholder="••••••••" autocomplete="current-password" required /></label>'+
    '<button class="btn primary wide" type="submit">'+svg('lock')+' Entrar al estudio '+svg('arrow')+'</button></form>')+
   '<p class="auth-foot">No se permite crear cuentas desde este panel. Solo el titular del proyecto puede autorizar moderadores.</p>'+
  '</section><aside class="auth-aside"><span class="vertical-caption">LUMI · ESCUCHAR · CONSTRUIR</span><div class="quote"><span class="quote-mark">“</span><p>Las mejores ideas empiezan cuando alguien se siente escuchada.</p><span>Un lugar para construir juntas.</span></div></aside></main>'
}
function counts() {
  return {
    pending:state.data.submissions.filter(x=>x.status==='Pendiente de revisión').length,
    comments:state.data.pendingComments.length,
    reports:state.data.reports.filter(x=>x.status==='Abierto').length,
    ideas:state.data.ideas.filter(x=>x.visible).length,
    active:state.data.ideas.filter(x=>x.status==='En desarrollo'&&x.visible).length
  }
}
function nav() {
 const c=counts()
 const numbers={pendientes:c.pending,roadmap:0,comments:c.comments,reports:c.reports}
 return TABS.map(t=>'<button class="nav-item '+(state.tab===t.key?'selected':'')+'" data-action="tab" data-tab="'+t.key+'" aria-current="'+(state.tab===t.key?'page':'false')+'">'+svg(t.icon)+' <span>'+t.label+'</span>'+(numbers[t.key]?'<span class="nav-count">'+numbers[t.key]+'</span>':'')+'</button>').join('')
}
function dashboardFrame(){
 const c=counts()
 const headings={
  pendientes:['Bandeja de entrada','Ideas por descubrir','Lee cada propuesta antes de que forme parte de la conversación.'],
  roadmap:['Hoja de ruta','Del “¿y si…?” al “ya está”.','Da seguimiento público a lo que estamos construyendo.'],
  comments:['Moderación','Conversaciones que cuidan.','Revisa y responde antes de publicar en Tu voz.'],
  reports:['Seguridad','Mantengamos este espacio seguro.','Atiende las señales de la comunidad y actúa con criterio.']
 }
 const current=headings[state.tab]
 return '<div class="shell"><aside class="sidebar">'+
 '<div class="brand"><span class="brand-mark">'+svg('spark',23)+'</span><span>Lumi <em>Studio</em></span></div>'+
 '<span class="sidebar-kicker">ADMINISTRACIÓN</span><nav class="sidebar-nav" aria-label="Secciones">'+nav()+'</nav>'+
 '<div class="sidebar-note"><span>'+svg('lock',16)+' ACCESO PROTEGIDO</span><p>Solo la cuenta autorizada puede publicar cambios.</p></div>'+
 '<button class="profile" data-action="logout"><span class="avatar">'+clean(state.session?.user?.email?.charAt(0).toUpperCase() || 'L')+'</span><span class="profile-copy"><b>Equipo Lumi</b><small>'+clean(state.session?.user?.email||'Moderación')+'</small></span>'+svg('logout',17)+'</button>'+
 '</aside><div class="main-container"><header class="topbar"><span><span class="live-dot"></span> TU VOZ <span class="topbar-divider">/</span> LUMI STUDIO</span>'+
 '<div class="top-actions"><span class="sync-hint">'+(state.changedAt?'Sincronizado '+clean(new Intl.DateTimeFormat('es-CL',{timeStyle:'short'}).format(state.changedAt)):'Preparando datos')+'</span>'+
 '<button class="icon-button" title="Actualizar contenido" aria-label="Actualizar contenido" data-action="refresh">'+svg('refresh')+'</button></div></header>'+
 '<main class="workspace"><div class="page-intro"><div><p class="eyebrow">'+current[0].toUpperCase()+'</p><h1>'+current[1]+'</h1><p class="page-description">'+current[2]+'</p></div>'+
 '<div class="intro-ornament">'+svg(TABS.find(t=>t.key===state.tab).icon,30)+'</div></div>'+
 '<div class="metric-grid">'+
 metric('Por revisar',c.pending,'inbox')+metric('En desarrollo',c.active,'route')+
 metric('Comentarios pendientes',c.comments,'message')+metric('Reportes abiertos',c.reports,'shield')+
 '</div>'+renderSection()+'</main></div></div>'+renderDialog()
}
function metric(label,num,icon){return '<div class="metric"><div class="metric-top"><span>'+clean(label)+'</span>'+svg(icon)+'</div><b>'+num+'</b><div class="metric-line"></div></div>'}
function toolbar(title,count,placeholder='Buscar por título o categoría…'){
return '<div class="section-toolbar"><div><h2>'+title+' <span class="small-count">'+count+'</span></h2></div>'+
'<label class="search">'+svg('search',17)+'<input id="list-search" type="search" placeholder="'+placeholder+'" value="'+clean(state.query)+'" aria-label="Buscar en sección"/></label></div>'
}
function empty(title,description){return '<div class="empty">'+svg('spark',30)+'<h3>'+clean(title)+'</h3><p>'+clean(description)+'</p></div>'}
function matches(x){const q=state.query.trim().toLocaleLowerCase('es');return !q || JSON.stringify(x).toLocaleLowerCase('es').includes(q)}
function entry({title,description,category,date,chip,actions,meta=''}) {
 return '<article class="entry"><div class="entry-body"><div class="entry-meta"><span class="category">'+clean(category||'Comunidad')+'</span><span class="entry-date">'+fmt(date)+'</span></div>'+
 '<h3>'+clean(title)+'</h3><p>'+clean(description||'')+'</p>'+ (meta?'<div class="entry-sub">'+meta+'</div>':'')+'</div>'+
 '<div class="entry-actions">'+(chip||'')+'<div class="action-row">'+actions+'</div></div></article>'
}
function act(name,id,label,kind='light',icon='check',disabled=false){
return '<button class="btn '+kind+'" data-action="'+name+'" data-id="'+clean(id)+'"'+(disabled||state.busy?' disabled':'')+'>'+svg(icon,16)+' '+clean(label)+'</button>'
}
function renderPending(){
 const items=state.data.submissions.filter(x=>x.status==='Pendiente de revisión'&&matches(x))
 return '<section class="surface">'+toolbar('Ideas pendientes',items.length)+
 (items.length?items.map(x=>entry({
  title:x.title,description:x.description,category:x.category,date:x.created_at_ms,
  actions:act('reject-idea',x.id,'Rechazar','ghost','x')+act('approve-idea',x.id,'Aprobar','primary','check')
 })).join(''):empty('Todo al día','Las sugerencias nuevas que envíen las usuarias aparecerán aquí.'))+'</section>'
}
function renderRoadmap() {
 const list=state.data.ideas.filter(matches).sort((a,b)=>b.created_at_ms-a.created_at_ms)
 return '<section class="surface">'+toolbar('Todas las propuestas',list.length)+
 (list.length?list.map(x=>'<article class="roadmap-row"><div class="roadmap-copy">'+
 '<div class="entry-meta"><span class="category">'+clean(x.category)+'</span> '+badge(x.status)+' '+(!x.visible?'<span class="status status--muted">Oculta</span>':'')+'</div>'+
 '<h3>'+clean(x.title)+'</h3><p>'+clean(cut(x.description,200))+'</p>'+
 '<div class="roadmap-numbers"><span>'+svg('check',15)+' '+Number(x.votes_count||0)+' apoyos</span><span>'+svg('message',15)+' '+Number(x.comments_count||0)+' comentarios</span></div></div>'+
 '<div class="roadmap-tools"><label>ESTADO <select data-action="draft-status" data-id="'+clean(x.id)+'" aria-label="Estado de '+clean(x.title)+'">'+
 STEPS.map(s=>'<option value="'+clean(s)+'"'+(s===(state.draftStatus[x.id]||x.status)?' selected':'')+'>'+clean(s)+'</option>').join('')+'</select></label>'+
 act('save-status',x.id,'Guardar estado','primary','check')+
 '<div class="roadmap-secondary">'+act('open-reply',x.id,'Responder','light','message')+
 act('toggle-visibility',x.id,x.visible?'Ocultar':'Mostrar','ghost',x.visible?'eyeoff':'eye')+'</div></div></article>').join(''):
 empty('Aún no hay propuestas públicas','Las ideas aprobadas aparecerán aquí con su estado y los apoyos recibidos.'))+'</section>'
}
function renderComments(){
 const pending=state.data.pendingComments.filter(matches).sort((a,b)=>b.created_at_ms-a.created_at_ms)
 const published=state.data.comments.filter(x=>x.visible&&matches(x)).sort((a,b)=>b.created_at_ms-a.created_at_ms).slice(0,70)
 return '<section class="surface">'+toolbar('Comentarios pendientes',pending.length,'Buscar comentarios…')+
 (pending.length?pending.map(x=>entry({title:'Comentario por revisar',description:x.body,category:'Propuesta '+x.idea_id?.slice(0,8),date:x.created_at_ms,
 actions:act('reject-comment',x.id,'Rechazar','ghost','x')+act('approve-comment',x.id,'Publicar','primary','check')})).join(''):
 empty('No hay comentarios pendientes','Cuando alguien comente una propuesta, podrás revisarlo aquí.'))+
 '<div class="secondary-heading"><h2>Comentarios publicados <span class="small-count">'+published.length+'</span></h2><p>También puedes ocultar contenido aprobado si recibes una denuncia.</p></div>'+
 (published.length?published.map(x=>entry({title:x.is_team?'Respuesta del equipo Lumi':'Comentario de la comunidad',description:x.body,
 category:x.is_team?'Oficial':'Publicado',date:x.created_at_ms,
 actions:act('hide-comment',x.id,'Ocultar comentario','ghost','eyeoff')})).join(''):
 '<div class="simple-empty">Todavía no hay comentarios públicos.</div>')+'</section>'
}
function renderReports(){
 const rows=state.data.reports.filter(matches).sort((a,b)=>Number(b.status==='Abierto')-Number(a.status==='Abierto')||b.created_at_ms-a.created_at_ms)
 return '<section class="surface">'+toolbar('Reportes de la comunidad',rows.length,'Buscar por motivo…')+
 (rows.length?rows.map(r=>entry({
  title:'Contenido reportado',description:r.reason,category:r.status||'Abierto',date:r.created_at_ms,
  meta:'ID de la propuesta: <code>'+clean(r.idea_id)+'</code>',
  actions:(r.status==='Abierto'?
     act('hide-reported',r.idea_id,'Ocultar propuesta','ghost','eyeoff')+
     act('resolve-report',r.id,'Marcar revisado','primary','check'):badge(r.status||'Revisado'))
 })).join(''):empty('Sin reportes por revisar','Las denuncias de las usuarias aparecerán aquí.'))+'</section>'
}
function renderSection() {
 switch(state.tab){
   case 'pendientes':return renderPending()
   case 'roadmap':return renderRoadmap()
   case 'comments':return renderComments()
   case 'reports':return renderReports()
   default:return ''
 }
}
function renderDialog(){
 const dialog=state.dialog
 if(!dialog)return ''
 const idea=state.data.ideas.find(x=>x.id===dialog.idea)
 if(!idea)return ''
 return '<div class="modal-backdrop" data-action="dismiss-dialog"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title"><button class="close-modal icon-button" data-action="close-dialog" aria-label="Cerrar">'+svg('x')+'</button>'+
 '<span class="eyebrow">RESPONDER COMO EQUIPO LUMI</span><h2 id="modal-title">Una respuesta que importa.</h2>'+
 '<p class="modal-context">'+clean(idea.title)+'</p><form id="reply-form" data-id="'+clean(idea.id)+'"><label>Tu mensaje oficial<textarea name="body" maxlength="420" minlength="4" required placeholder="Cuéntales qué estamos preparando, explica una decisión o agradece una idea…"></textarea></label>'+
 '<p class="input-hint">Esta respuesta será pública y estará marcada como «Equipo Lumi».</p>'+
 '<div class="modal-actions"><button class="btn light" type="button" data-action="close-dialog">Cancelar</button><button class="btn primary" type="submit">'+svg('send')+' Publicar respuesta</button></div></form></section></div>'
}
function render() {
 if(!supabase){app.innerHTML=loginView(); app.querySelector('.auth-panel').insertAdjacentHTML('beforeend','<div class="error-banner">Faltan VITE_SUPABASE_URL y VITE_SUPABASE_PUBLISHABLE_KEY en el archivo .env local.</div>'); return}
 if(state.checking){app.innerHTML=loadingBlock();return}
 if(!state.session){app.innerHTML=loginView();return}
 if(!state.authorized){app.innerHTML=loginView('blocked');return}
 app.innerHTML=dashboardFrame()
}
async function authorize(){
 const {data,error}=await supabase.rpc('voice_is_admin')
 if(error)throw error
 state.authorized=data===true
}
async function loadAll(showSuccess=false) {
 if(!supabase || !state.session || !state.authorized || state.fetching)return
 state.fetching=true
 try {
  const req=[
   supabase.from('voice_submissions').select('id,title,description,category,status,is_public,created_at_ms').order('created_at_ms',{ascending:false}).limit(250),
   supabase.from('voice_ideas').select('id,title,description,category,status,visible,votes_count,comments_count,created_at_ms').order('created_at_ms',{ascending:false}).limit(250),
   supabase.from('voice_pending_comments').select('id,idea_id,body,created_at_ms').order('created_at_ms',{ascending:false}).limit(250),
   supabase.from('voice_comments').select('id,idea_id,body,is_team,visible,created_at_ms').order('created_at_ms',{ascending:false}).limit(250),
   supabase.from('voice_reports').select('id,idea_id,reason,status,created_at_ms').order('created_at_ms',{ascending:false}).limit(250)
  ]
  const res=await Promise.all(req)
  for(const r of res)if(r.error)throw r.error
  ;['submissions','ideas','pendingComments','comments','reports'].forEach((name,i)=>state.data[name]=res[i].data||[])
  state.changedAt=new Date()
  render()
  if(showSuccess)toast('Información actualizada.')
 } catch(err) {toast('No se pudieron actualizar los datos: '+(err.message||String(err)),'error')}
 finally {state.fetching=false}
}
function subscribe(){
 if(realtimeChannel) supabase.removeChannel(realtimeChannel)
 realtimeChannel=supabase.channel('lumi-studio-public-updates')
   .on('postgres_changes',{event:'*',schema:'public',table:'voice_ideas'},()=>loadAll())
   .on('postgres_changes',{event:'*',schema:'public',table:'voice_comments'},()=>loadAll())
   .subscribe()
 clearInterval(poll)
 poll=setInterval(()=>{if(!document.hidden&&state.authorized)loadAll()},45000)
}
async function signIn(email,password) {
 state.checking=true;render()
 try{
  const {data,error}=await supabase.auth.signInWithPassword({email,password})
  if(error)throw error
  state.session=data.session
  await authorize()
  if(state.authorized){state.tab='pendientes';subscribe();await loadAll()}
  else render()
 }catch(e){state.checking=false;state.session=null;state.authorized=false;render();toast(e.message||'No se pudo iniciar sesión','error');return}
 state.checking=false;render()
}
async function logout(){
 if(supabase) {
  if(realtimeChannel) {supabase.removeChannel(realtimeChannel);realtimeChannel=null}
  clearInterval(poll)
  await supabase.auth.signOut()
 }
 state.session=null;state.authorized=false;state.dialog=null;state.tab='pendientes';state.checking=false
 state.data={submissions:[],ideas:[],pendingComments:[],comments:[],reports:[]}
 render()
}
const rpcActions={
 'approve-idea':['voice_admin_review_idea',(id)=>({p_id:id,p_approve:true})],
 'reject-idea':['voice_admin_review_idea',(id)=>({p_id:id,p_approve:false})],
 'approve-comment':['voice_admin_review_comment',(id)=>({p_id:id,p_approve:true})],
 'reject-comment':['voice_admin_review_comment',(id)=>({p_id:id,p_approve:false})],
 'hide-comment':['voice_admin_hide_comment',(id)=>({p_id:id})],
 'resolve-report':['voice_admin_review_report',(id)=>({p_id:id,p_status:'Revisado'})],
}
async function perform(fn,params,success){
 if(state.busy)return
 state.busy=true
 try{
  const {error}=await supabase.rpc(fn,params)
  if(error)throw error
  state.dialog=null
  await loadAll()
  toast(success)
 }catch(e){toast(e.message||'No se pudo completar la operación.','error')}
 finally{state.busy=false;render()}
}
async function action(e){
 const target=e.target.closest('[data-action]')
 if(!target)return
 const name=target.dataset.action,id=target.dataset.id
 if(name==='dismiss-dialog'&&e.target!==target)return
 if(name==='close-dialog'||name==='dismiss-dialog'){state.dialog=null;render();return}
 if(name==='tab'){state.tab=target.dataset.tab;state.query='';render();return}
 if(name==='logout'){await logout();return}
 if(name==='refresh'){await loadAll(true);return}
 if(name==='draft-status')return
 if(name==='open-reply'){state.dialog={idea:id};render();return}
 if(!state.authorized)return
 if(rpcActions[name]){
  const confirmed={
    'reject-idea':'¿Rechazar esta propuesta? No se publicará.',
    'reject-comment':'¿Rechazar este comentario? Se eliminará de la cola.',
    'hide-comment':'¿Ocultar este comentario público?',
  }[name]
  if(confirmed&&!window.confirm(confirmed))return
  const [fn,params]=rpcActions[name]
  await perform(fn,params(id),'Operación completada.')
  return
 }
 if(name==='save-status'){
  const idea=state.data.ideas.find(x=>x.id===id)
  const status=state.draftStatus[id]||idea?.status
  if(!idea || !STEPS.includes(status))return
  await perform('voice_admin_set_status',{p_id:id,p_status:status},'Estado actualizado a «'+status+'».')
  return
 }
 if(name==='toggle-visibility'){
  const idea=state.data.ideas.find(x=>x.id===id)
  if(!idea)return
  if(idea.visible&&!window.confirm('¿Ocultar esta propuesta de todas las usuarias?'))return
  await perform('voice_admin_set_visibility',{p_id:id,p_visible:!idea.visible},idea.visible?'Propuesta ocultada.':'Propuesta visible.')
  return
 }
 if(name==='hide-reported'){
  if(!window.confirm('¿Ocultar esta propuesta reportada para todas las usuarias?'))return
  await perform('voice_admin_set_visibility',{p_id:id,p_visible:false},'Propuesta ocultada. Recuerda revisar o cerrar el reporte.')
 }
}
document.addEventListener('click',e=>{action(e).catch(err=>toast(err.message||String(err),'error'))})
document.addEventListener('change',e=>{
 if(e.target?.dataset?.action==='draft-status')state.draftStatus[e.target.dataset.id]=e.target.value
})
document.addEventListener('input',e=>{
 if(e.target?.id==='list-search'){
  const start=e.target.selectionStart
  state.query=e.target.value
  const list=e.target.closest('.surface')
  if(list){
    const before=window.scrollY
    list.outerHTML=renderSection()
    const input=document.querySelector('#list-search')
    if(input){
      input.focus()
      if(typeof input.setSelectionRange==='function'){
        try { input.setSelectionRange(start,start) } catch(_) {}
      }
    }
    window.scrollTo(0,before)
  }
 }
})
document.addEventListener('submit',async e=>{
 if(e.target?.id==='login-form'){
  e.preventDefault()
  const data=new FormData(e.target)
  await signIn(String(data.get('email')||'').trim(),String(data.get('password')||''))
 }
 if(e.target?.id==='reply-form'){
  e.preventDefault()
  const id=e.target.dataset.id
  const body=String(new FormData(e.target).get('body')||'').trim()
  if(body.length<4 || body.length>420){toast('El mensaje debe tener entre 4 y 420 caracteres.','error');return}
  await perform('voice_admin_reply',{p_idea_id:id,p_body:body},'Respuesta oficial publicada.')
 }
})
document.addEventListener('keydown',e=>{
 if(e.key==='Escape'&&state.dialog){state.dialog=null;render()}
})
document.addEventListener('visibilitychange',()=>{
 if(!document.hidden&&state.authorized)loadAll()
})
async function start(){
 if(!supabase){state.checking=false;render();return}
 try{
  const {data,error}=await supabase.auth.getSession()
  if(error)throw error
  state.session=data.session
  if(state.session){
    await authorize()
    if(state.authorized){subscribe();await loadAll()}
  }
 } catch(err){
  state.session=null
  state.authorized=false
  toast('No se pudo verificar la sesión. Vuelve a iniciar sesión.','error')
 }
 state.checking=false
 render()
}
render()
start()
