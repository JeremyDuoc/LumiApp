package com.jeremy.lumi.ui.screens.forum

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.jeremy.lumi.ui.components.LumiBrandLockup
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.ui.theme.LumiDesignTokens

private val TABS = listOf("Ideas","En camino","Mis votos")
private val SORTS = listOf("Más apoyadas","Más recientes","Más comentadas")
private val CATEGORIES = listOf("Calendario","Registro diario","Apariencia","Privacidad","Recordatorios","Otro")
private val STEPS = listOf("Recibida","En revisión","Aceptada","En desarrollo","Disponible")
private val CTA = Brush.horizontalGradient(listOf(Color(0xFFEAB0CE),Color(0xFFC6B1F4)))

private fun voiceFormattedTime(ms:Long):String =
    SimpleDateFormat("d MMM yyyy · HH:mm",Locale.forLanguageTag("es-CL")).format(Date(ms))

private fun voiceFormattedEstimate(iso:String?):String? {
    if(iso.isNullOrBlank())return null
    return runCatching {
        val utc=TimeZone.getTimeZone("UTC")
        val parsed=SimpleDateFormat("yyyy-MM-dd",Locale.US).apply {
            isLenient=false
            timeZone=utc
        }.parse(iso) ?: return null
        SimpleDateFormat("d MMM yyyy",Locale.forLanguageTag("es-CL")).apply {
            timeZone=utc
        }.format(parsed)
    }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumScreen(viewModel:ForumViewModel = hiltViewModel()) {
    val theme=LumiThemeTokens.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sorting by rememberSaveable { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    var mainMenu by remember { mutableStateOf(false) }
    var create by rememberSaveable { mutableStateOf(false) }
    var help by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var reportId by rememberSaveable { mutableStateOf<String?>(null) }
    val snack=remember{SnackbarHostState()}
    LaunchedEffect(state.notice) {
        state.notice?.let { message ->
            if(message.startsWith("Idea enviada.")) create=false
            if(message.startsWith("Comentario enviado")) selectedId=null
            snack.showSnackbar(message)
            viewModel.dismissNotice()
        }
    }
    LaunchedEffect(selectedId) {viewModel.showComments(selectedId)}
    val hero=state.ideas.firstOrNull{it.status=="En desarrollo"}
        ?:state.ideas.firstOrNull{it.status=="Aceptada"}
    val authored=state.mine.map{it.id}.toSet()
    val shown=when(tab) {
        0->state.ideas.filter{it.id!=hero?.id}
        1->state.ideas.filter{it.status in listOf("Aceptada","En desarrollo","Disponible")}
        else->state.ideas.filter{it.id in authored || it.id in state.voted}+
            state.mine.filter{!it.public}.map{
                VoiceIdea(it.id,it.title,it.description,it.category,it.status,0,0,it.createdAtMs,false)
            }
    }.let { list->when(sorting){
        1->list.sortedByDescending{it.createdAtMs}
        2->list.sortedWith(compareByDescending<VoiceIdea>{it.comments}.thenByDescending{it.createdAtMs})
        else->list.sortedWith(compareByDescending<VoiceIdea>{it.votes}.thenByDescending{it.createdAtMs})
    }}
    Box(Modifier.fillMaxSize().background(theme.bg)){
        Column(Modifier.fillMaxSize().statusBarsPadding()){
            Row(Modifier.fillMaxWidth().padding(start=22.dp,end=18.dp,top=8.dp,bottom=8.dp),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                LumiBrandLockup(flowerSize=21,fontSize=24.sp,animated=true)
                Box{
                    IconButton(onClick={mainMenu=true},modifier=Modifier.size(42.dp).background(theme.panel,CircleShape)){
                        Icon(Icons.Outlined.MoreHoriz,"Opciones de Tu voz",tint=theme.accent)
                    }
                    DropdownMenu(expanded=mainMenu,onDismissRequest={mainMenu=false}){
                        DropdownMenuItem(text={Text("Proponer una idea")},onClick={mainMenu=false;create=true})
                        DropdownMenuItem(text={Text("Cómo funciona")},onClick={mainMenu=false;help=true})
                    }
                }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal=22.dp).padding(bottom=28.dp)){
                Spacer(Modifier.height(15.dp))
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text("Tu voz",fontSize=32.sp,fontWeight=FontWeight.ExtraBold,
                        letterSpacing=(-1.4).sp,color=theme.text,modifier=Modifier.weight(1f))
                    IconButton(onClick={help=true},modifier=Modifier.size(35.dp).border(1.dp,theme.line,CircleShape)){
                        Icon(Icons.Outlined.Info,"Cómo funciona",tint=theme.muted,modifier=Modifier.size(19.dp))
                    }
                }
                Text("Propón una mejora, apoya otras ideas y descubre cómo avanzan.",
                    fontSize=12.sp,lineHeight=19.sp,color=theme.muted,modifier=Modifier.padding(top=6.dp))
                Row(Modifier.fillMaxWidth().padding(top=16.dp,bottom=20.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    Row(Modifier.height(44.dp).clip(CircleShape).background(CTA).clickable{create=true}
                        .padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Icon(Icons.Outlined.Add,null,modifier=Modifier.size(17.dp),tint=Color(0xFF21172A))
                        Text("Proponer una idea",fontSize=12.sp,fontWeight=FontWeight.ExtraBold,color=Color(0xFF21172A))
                    }
                    Text("Sin nombre público.\nTu salud se queda en tu dispositivo.",
                        fontSize=10.sp,lineHeight=15.sp,color=theme.muted,modifier=Modifier.weight(1f))
                }
                HorizontalDivider(color=theme.line)
                if(tab==0){
                    Row(Modifier.fillMaxWidth().padding(top=19.dp,bottom=11.dp),
                        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                        Text("Lo que estamos creando",fontSize=13.sp,fontWeight=FontWeight.Bold,color=theme.text)
                        Text("Hoja de ruta",fontSize=10.sp,color=theme.muted)
                    }
                    if(hero!=null) FeaturedIdea(hero,theme){selectedId=hero.id}
                    else Surface(shape=RoundedCornerShape(17.dp),color=theme.panel,border=BorderStroke(1.dp,theme.line)){
                        Column(Modifier.fillMaxWidth().padding(16.dp)){
                            Text("Próximamente",fontSize=10.sp,fontWeight=FontWeight.Bold,color=theme.accent)
                            Spacer(Modifier.height(9.dp))
                            Text("Aquí verás las mejoras que el equipo esté desarrollando.",
                                fontSize=12.sp,lineHeight=18.sp,color=theme.muted)
                        }
                    }
                }
                Spacer(Modifier.height(21.dp))
                VoiceTabs(tab,theme){tab=it}
                Row(Modifier.fillMaxWidth().padding(top=21.dp,bottom=6.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                    Text(when(tab){0->"Más ideas";1->"En camino";else->"Tus ideas y apoyos"},
                        fontSize=14.sp,fontWeight=FontWeight.Bold,color=theme.text)
                    Box{
                        TextButton(onClick={sortMenu=true}){
                            Text(SORTS[sorting]+" ⌄",fontSize=11.sp,color=theme.muted)
                        }
                        DropdownMenu(expanded=sortMenu,onDismissRequest={sortMenu=false}){
                            SORTS.forEachIndexed{i,s->
                                DropdownMenuItem(text={Text(s)},onClick={sorting=i;sortMenu=false})
                            }
                        }
                    }
                }
                if(state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(),color=theme.accent,trackColor=theme.line)
                else if(state.offlineMessage!=null) VoiceEmpty("No pudimos cargar las ideas",state.offlineMessage.orEmpty(),theme)
                else if(shown.isEmpty()) VoiceEmpty(
                    when(tab){2->"Todavía no has apoyado ideas";1->"Aún no hay mejoras en camino";else->"Tu primera idea puede comenzar aquí"},
                    when(tab){2->"Apoya una propuesta o publica la primera para verla aquí."
                        1->"Las propuestas aprobadas y en desarrollo aparecerán aquí."
                        else->"Sé la primera persona en proponer una mejora."},theme)
                else shown.forEach{post->
                    key(post.id){
                        VoicePostRow(post,post.id in state.voted,theme,state.busy,
                            onVote={viewModel.vote(post.id)},onOpen={selectedId=post.id})
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Solo se publican ideas y comentarios aprobados. No compartas datos personales o de salud.",
                    fontSize=10.sp,lineHeight=15.sp,color=theme.dim)
            }
        }
        SnackbarHost(snack,modifier=Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
    if(create) NewIdeaSheet(theme,state.busy,state.notice,onDismiss={create=false},onSubmit=viewModel::suggest)
    val detail=state.ideas.firstOrNull{it.id==selectedId}
    if(detail!=null) DetailSheet(detail,theme,state.comments,state.history,detail.id in state.voted,state.busy,state.notice,
        onDismiss={selectedId=null},onVote={viewModel.vote(detail.id)},
        onComment={viewModel.comment(detail.id,it)},onReport={reportId=detail.id})
    if(help) AlertDialog(onDismissRequest={help=false},containerColor=theme.panel,
        title={Text("De una idea a una mejora",color=theme.text)},
        text={Text("Propón sin mostrar tu nombre. El equipo revisa la idea antes de publicarla. Otras usuarias pueden apoyar y comentar. Los votos ayudan a priorizar, pero no garantizan que se implemente. Los datos de tu ciclo nunca se envían a Tu voz.",color=theme.muted)},
        confirmButton={TextButton(onClick={help=false}){Text("Entendido",color=theme.accent)}})
    if(reportId!=null) AlertDialog(onDismissRequest={reportId=null},containerColor=theme.panel,
        title={Text("Reportar publicación",color=theme.text)},
        text={Text("¿Tiene contenido abusivo, datos personales o spam? El equipo recibirá el reporte.",color=theme.muted)},
        confirmButton={TextButton(onClick={
            reportId?.let{viewModel.report(it,"Contenido abusivo, personal o spam")};reportId=null
        }){Text("Reportar",color=theme.accent)}},
        dismissButton={TextButton(onClick={reportId=null}){Text("Cancelar",color=theme.muted)}})
}
@Composable
private fun VoiceTabs(current:Int,theme:LumiDesignTokens,onSelect:(Int)->Unit){
    BoxWithConstraints(Modifier.fillMaxWidth().height(47.dp).clip(RoundedCornerShape(13.dp))
        .background(theme.panel).border(1.dp,theme.line,RoundedCornerShape(13.dp)).padding(4.dp)){
        val width=maxWidth/3
        val x by animateDpAsState(width*current,
            animationSpec=tween(500,easing=CubicBezierEasing(.33f,.83f,.99f,.98f)),label="voice_slider")
        Box(Modifier.offset(x=x).width(width).fillMaxHeight().background(theme.panel2,RoundedCornerShape(9.dp))
            .border(1.dp,theme.accent.copy(alpha=.4f),RoundedCornerShape(9.dp)))
        Row(Modifier.fillMaxSize()){
            TABS.forEachIndexed{i,label->
                Box(Modifier.weight(1f).fillMaxHeight().semantics{role=Role.Tab;selected=(i==current)}
                    .clickable{onSelect(i)},contentAlignment=Alignment.Center){
                    Text(label,fontSize=11.sp,fontWeight=FontWeight.Bold,
                        color=if(i==current)theme.text else theme.muted)
                }
            }
        }
    }
}
@Composable
private fun FeaturedIdea(idea:VoiceIdea,theme:LumiDesignTokens,onOpen:()->Unit){
    Surface(onClick=onOpen,shape=RoundedCornerShape(17.dp),color=theme.panel,
        border=BorderStroke(1.dp,theme.accent.copy(alpha=.45f))){
        Column(Modifier.fillMaxWidth().padding(16.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(idea.status,fontSize=10.sp,fontWeight=FontWeight.Bold,color=theme.accent)
                Text("Ver avance ›",fontSize=10.sp,color=theme.muted)
            }
            Text(idea.title,fontSize=16.sp,lineHeight=21.sp,fontWeight=FontWeight.Bold,
                color=theme.text,modifier=Modifier.padding(top=10.dp,bottom=13.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                ProgressDots(idea.status,theme)
                Text(idea.votes.toString()+" apoyos",fontSize=10.sp,color=theme.muted)
            }
            val eta=voiceFormattedEstimate(idea.estimatedAvailableOn)
            if(eta!=null && idea.status!="Disponible" && idea.status!="No prevista") {
                Text("Disponibilidad estimada: "+eta,
                    fontSize=10.sp,lineHeight=15.sp,color=theme.accent,
                    modifier=Modifier.padding(top=12.dp))
            }
        }
    }
}
@Composable
private fun ProgressDots(status:String,theme:LumiDesignTokens){
    val reached=STEPS.indexOf(status).coerceAtLeast(0)
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
        repeat(5){i->
            if(i>0)Box(Modifier.width(19.dp).height(1.dp).background(if(i<=reached)theme.accent else theme.line))
            Box(Modifier.size(6.dp).background(if(i<=reached)theme.accent else theme.panel2,CircleShape)
                .border(1.dp,if(i<=reached)theme.accent else theme.dim,CircleShape))
        }
    }
}
@Composable
private fun VoicePostRow(idea:VoiceIdea,voted:Boolean,theme:LumiDesignTokens,busy:Boolean,
                         onVote:()->Unit,onOpen:()->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=114.dp).padding(vertical=15.dp),
        horizontalArrangement=Arrangement.spacedBy(13.dp),verticalAlignment=Alignment.Top){
        VotePill(idea.votes,voted,theme,idea.public&&!busy,onVote)
        Column(Modifier.weight(1f).clickable(enabled=idea.public,onClick=onOpen).padding(top=2.dp)){
            Text(idea.title,fontSize=14.sp,fontWeight=FontWeight.Bold,lineHeight=19.sp,
                color=theme.text,maxLines=3,overflow=TextOverflow.Ellipsis)
            Spacer(Modifier.height(7.dp))
            Text(idea.status+"  ·  "+idea.category+"  ·  "+idea.comments+" comentarios",
                fontSize=10.sp,lineHeight=15.sp,color=theme.muted)
            if(!idea.public) Text("Solo tú puedes verla mientras se revisa.",
                fontSize=10.sp,color=theme.accent,modifier=Modifier.padding(top=5.dp))
        }
    }
    HorizontalDivider(color=theme.line)
}
@Composable
private fun VotePill(count:Int,voted:Boolean,theme:LumiDesignTokens,enabled:Boolean,onClick:()->Unit){
    Column(Modifier.width(48.dp).height(63.dp).clip(RoundedCornerShape(13.dp))
        .background(if(voted)theme.accentSoft else theme.panel)
        .border(1.dp,if(voted)theme.accent else theme.line,RoundedCornerShape(13.dp))
        .clickable(enabled=enabled,onClick=onClick),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        Box(Modifier.size(23.dp)
            .then(if(voted)Modifier.shadow(8.dp,CircleShape,ambientColor=theme.accent,spotColor=theme.rose) else Modifier)
            .background(Color(0xFF09080D),CircleShape).border(1.dp,theme.line,CircleShape),
            contentAlignment=Alignment.Center){
            if(voted)Icon(Icons.Outlined.Check,null,tint=Color.White,modifier=Modifier.size(15.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(count.toString(),fontSize=12.sp,fontWeight=FontWeight.Bold,
            color=if(voted)theme.accent else theme.muted)
    }
}
@Composable
private fun VoiceEmpty(title:String,description:String,theme:LumiDesignTokens){
    Column(Modifier.fillMaxWidth().padding(vertical=44.dp,horizontal=12.dp),
        horizontalAlignment=Alignment.CenterHorizontally){
        Text(title,fontSize=15.sp,fontWeight=FontWeight.Bold,color=theme.text)
        Spacer(Modifier.height(9.dp))
        Text(description,fontSize=12.sp,lineHeight=18.sp,color=theme.muted)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewIdeaSheet(theme:LumiDesignTokens,busy:Boolean,notice:String?,onDismiss:()->Unit,
                         onSubmit:(String,String,String)->Unit){
    var title by rememberSaveable{mutableStateOf("")}
    var description by rememberSaveable{mutableStateOf("")}
    var category by rememberSaveable{mutableStateOf(CATEGORIES.first())}
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=theme.bg){
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal=22.dp).padding(bottom=40.dp)){
            SheetHeading("Nueva sugerencia",onDismiss,theme)
            Text("¿Qué mejorarías de Lumi?",fontSize=23.sp,fontWeight=FontWeight.ExtraBold,color=theme.text)
            Text("Explica qué cambiarías y para qué te serviría.",
                fontSize=12.sp,color=theme.muted,modifier=Modifier.padding(top=8.dp,bottom=15.dp))
            Text("Título de la idea",color=theme.text,fontSize=11.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(value=title,onValueChange={if(it.length<=95)title=it},singleLine=true,
                placeholder={Text("Ej. Ordenar mis síntomas favoritos",fontSize=12.sp)},
                modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp))
            Text(title.length.toString()+"/95",fontSize=10.sp,color=theme.dim,modifier=Modifier.align(Alignment.End))
            Text("Categoría",color=theme.text,fontSize=11.sp,fontWeight=FontWeight.Bold,
                modifier=Modifier.padding(top=12.dp,bottom=8.dp))
            CATEGORIES.chunked(3).forEach{row->
                Row(Modifier.fillMaxWidth().padding(bottom=6.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    row.forEach{label->
                        val active=category==label
                        Surface(onClick={category=label},modifier=Modifier.weight(1f),
                            shape=RoundedCornerShape(11.dp),
                            color=if(active)theme.accentSoft else theme.panel,
                            border=BorderStroke(1.dp,if(active)theme.accent else theme.line)){
                            Box(Modifier.heightIn(min=37.dp).padding(4.dp),contentAlignment=Alignment.Center){
                                Text(label,fontSize=10.sp,lineHeight=12.sp,color=theme.text,maxLines=2)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Describe tu idea",fontSize=11.sp,color=theme.text,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(value=description,onValueChange={if(it.length<=650)description=it},
                minLines=4,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),
                placeholder={Text("¿Cómo te ayudaría esta mejora?",fontSize=12.sp)})
            Text(description.length.toString()+"/650",fontSize=10.sp,color=theme.dim,
                modifier=Modifier.align(Alignment.End))
            Text("Tu nombre no será visible. No incluyas síntomas, datos médicos, teléfonos o nombres. La idea se publicará después de ser revisada.",
                fontSize=11.sp,lineHeight=16.sp,color=theme.muted,modifier=Modifier.padding(vertical=14.dp))
            Button(onClick={onSubmit(title,description,category)},
                enabled=!busy&&title.trim().length in 8..95&&description.trim().length in 20..650,
                modifier=Modifier.fillMaxWidth().height(50.dp),shape=CircleShape,
                colors=ButtonDefaults.buttonColors(containerColor=theme.accent,contentColor=theme.bg)){
                Text(if(busy)"Enviando…" else "Enviar idea a revisión",fontWeight=FontWeight.Bold)
            }
            if(notice!=null && !notice.startsWith("Idea enviada.")) Text(notice,color=theme.rose,fontSize=11.sp,modifier=Modifier.padding(top=10.dp))
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailSheet(idea:VoiceIdea,theme:LumiDesignTokens,comments:List<VoiceComment>,
                        history:List<VoiceStatusChange>,
                        voted:Boolean,busy:Boolean,notice:String?,onDismiss:()->Unit,onVote:()->Unit,
                        onComment:(String)->Unit,onReport:()->Unit){
    var message by rememberSaveable(idea.id){mutableStateOf("")}
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=theme.bg){
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal=22.dp).padding(bottom=42.dp)){
            SheetHeading(idea.category,onDismiss,theme)
            Text(idea.status,fontSize=11.sp,fontWeight=FontWeight.Bold,color=theme.accent)
            Text(idea.title,fontSize=23.sp,lineHeight=28.sp,fontWeight=FontWeight.ExtraBold,
                color=theme.text,modifier=Modifier.padding(top=8.dp))
            Text(idea.description,color=theme.muted,fontSize=12.sp,lineHeight=19.sp,
                modifier=Modifier.padding(top=10.dp,bottom=12.dp))
            Row(verticalAlignment=Alignment.CenterVertically){
                VotePill(idea.votes,voted,theme,!busy,onVote)
                Spacer(Modifier.weight(1f))
                TextButton(onClick=onReport){
                    Icon(Icons.Outlined.Report,null,tint=theme.muted,modifier=Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Reportar",color=theme.muted,fontSize=11.sp)
                }
            }
            HorizontalDivider(Modifier.padding(vertical=20.dp),color=theme.line)
            Text("El camino de esta idea",fontSize=14.sp,fontWeight=FontWeight.Bold,color=theme.text)
            Text("Sigue cada etapa, desde que la recibimos hasta que llega a Lumi.",
                fontSize=11.sp,lineHeight=16.sp,color=theme.muted,
                modifier=Modifier.padding(top=5.dp,bottom=15.dp))
            VoiceProgressStepper(idea.status,history,idea.estimatedAvailableOn,theme)
            HorizontalDivider(Modifier.padding(vertical=17.dp),color=theme.line)
            Text("Conversación · "+idea.comments+" comentarios",fontSize=13.sp,
                fontWeight=FontWeight.Bold,color=theme.text)
            if(comments.isEmpty())Text("Todavía no hay comentarios aprobados.",
                fontSize=12.sp,color=theme.muted,modifier=Modifier.padding(vertical=16.dp))
            comments.forEach{comment->
                Column(Modifier.fillMaxWidth().padding(vertical=11.dp)){
                    Text(if(comment.team)"Equipo Lumi" else "Anónima",fontSize=11.sp,
                        fontWeight=FontWeight.Bold,color=if(comment.team)theme.accent else theme.text)
                    Spacer(Modifier.height(5.dp))
                    Text(comment.body,fontSize=12.sp,lineHeight=18.sp,color=theme.muted)
                }
                HorizontalDivider(color=theme.line)
            }
            Text("Añadir un comentario",fontSize=11.sp,fontWeight=FontWeight.Bold,
                color=theme.text,modifier=Modifier.padding(top=16.dp,bottom=8.dp))
            OutlinedTextField(value=message,onValueChange={if(it.length<=420)message=it},
                minLines=3,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),
                placeholder={Text("Aporta una idea o haz una pregunta respetuosa.",fontSize=12.sp)})
            Text("Los comentarios se publican sin nombre y después de revisión.",
                fontSize=10.sp,lineHeight=15.sp,color=theme.muted,modifier=Modifier.padding(vertical=12.dp))
            Button(onClick={onComment(message);message=""},enabled=!busy&&message.trim().length in 4..420,
                shape=CircleShape,modifier=Modifier.fillMaxWidth().height(48.dp),
                colors=ButtonDefaults.buttonColors(containerColor=theme.accent,contentColor=theme.bg)){
                Text("Enviar comentario a revisión")
            }
            if(notice!=null && !notice.startsWith("Comentario enviado")) Text(notice,color=theme.rose,fontSize=11.sp,modifier=Modifier.padding(top=10.dp))
        }
    }
}
/**
 * Adaptación del stepper vertical de PriyanshuGupta28 (Uiverse) a Lumi.
 * Las fechas proceden EXCLUSIVAMENTE de eventos persistidos en Supabase.
 * Etapas anteriores sin evento no reciben fechas estimadas ni retroactivas.
 */
@Composable
private fun VoiceProgressStepper(
    status:String,
    history:List<VoiceStatusChange>,
    estimatedAvailableOn:String?,
    theme:LumiDesignTokens
) {
    val current=STEPS.indexOf(status)
    val notPlanned=status=="No prevista"
    val descriptions=listOf(
        "La sugerencia fue aprobada y publicada.",
        "Estamos evaluando cómo encaja en Lumi.",
        "La mejora forma parte de nuestros planes.",
        "Estamos construyendo esta funcionalidad.",
        "La mejora ya está disponible en Lumi."
    )
    Column(
        modifier=Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(theme.panel)
            .border(1.dp,theme.line,RoundedCornerShape(18.dp))
            .padding(horizontal=17.dp,vertical=20.dp)
    ) {
        if(notPlanned) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.panel2)
                    .padding(12.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(10.dp)
            ){
                Icon(Icons.Outlined.Info,null,tint=theme.accent,modifier=Modifier.size(19.dp))
                Column{
                    Text("No prevista por ahora",fontSize=12.sp,
                        fontWeight=FontWeight.Bold,color=theme.text)
                    Text("El equipo no tiene previsto desarrollar esta propuesta. Puede revisarse más adelante.",
                        fontSize=10.sp,lineHeight=15.sp,color=theme.muted)
                    val pausedAt=history.lastOrNull { it.toStatus=="No prevista" }?.changedAtMs
                    if(pausedAt!=null) Text(voiceFormattedTime(pausedAt),
                        fontSize=10.sp,color=theme.accent,modifier=Modifier.padding(top=5.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        val estimate=voiceFormattedEstimate(estimatedAvailableOn)
        if(estimate!=null && !notPlanned && status!="Disponible") {
            Column(
                Modifier.fillMaxWidth()
                    .background(theme.accentSoft,RoundedCornerShape(12.dp))
                    .padding(13.dp)
            ) {
                Text("Disponibilidad estimada · "+estimate,
                    fontSize=12.sp,fontWeight=FontWeight.Bold,color=theme.accent)
                Text("Fecha orientativa; podría cambiar durante el desarrollo.",
                    fontSize=10.sp,lineHeight=15.sp,color=theme.muted,
                    modifier=Modifier.padding(top=4.dp))
            }
            Spacer(Modifier.height(19.dp))
        }
        STEPS.forEachIndexed{index,label->
            val completed=!notPlanned && index<current
            val active=!notPlanned && index==current
            val recordedAt=history.lastOrNull { it.toStatus==label }?.changedAtMs
            val stageColor=when{
                completed->theme.accent
                active->theme.accent
                else->theme.dim
            }
            Row(
                modifier=Modifier.fillMaxWidth(),
                verticalAlignment=Alignment.Top
            ){
                Column(
                    horizontalAlignment=Alignment.CenterHorizontally,
                    modifier=Modifier.width(38.dp)
                ){
                    Box(
                        Modifier.size(36.dp)
                            .background(
                                if(completed)theme.accent else if(active)theme.accentSoft else theme.panel2,
                                CircleShape
                            )
                            .border(
                                if(active)2.dp else 1.dp,
                                when{
                                    completed||active->theme.accent
                                    else->theme.line
                                },
                                CircleShape
                            ),
                        contentAlignment=Alignment.Center
                    ){
                        if(completed)Icon(Icons.Outlined.Check,null,
                            tint=theme.bg,modifier=Modifier.size(19.dp))
                        else Text((index+1).toString(),fontSize=13.sp,
                            fontWeight=if(active)FontWeight.ExtraBold else FontWeight.SemiBold,
                            color=stageColor)
                    }
                    if(index<STEPS.lastIndex) {
                        Box(
                            Modifier.width(2.dp).height(30.dp)
                                .background(
                                    if(completed)theme.accent.copy(alpha=.70f)
                                    else theme.line,
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
                Spacer(Modifier.width(13.dp))
                Column(
                    Modifier.weight(1f)
                        .padding(top=1.dp,bottom=if(index==STEPS.lastIndex)0.dp else 11.dp)
                ){
                    Text(
                        label,fontSize=12.sp,fontWeight=FontWeight.Bold,
                        color=if(active||completed)theme.text else theme.dim
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape=CircleShape,
                        color=if(active||completed)theme.accentSoft else theme.panel2,
                    ){
                        Text(
                            when{
                                completed->"Completada"
                                active->"Etapa actual"
                                else->"Por venir"
                            },
                            fontSize=10.sp,
                            fontWeight=if(active)FontWeight.Bold else FontWeight.Medium,
                            color=if(active||completed)theme.accent else theme.muted,
                            modifier=Modifier.padding(horizontal=10.dp,vertical=4.dp)
                        )
                    }
                    if(active){
                        Text(descriptions[index],fontSize=10.sp,lineHeight=15.sp,
                            color=theme.muted,modifier=Modifier.padding(top=7.dp))
                    }
                    if(recordedAt!=null && (completed||active)) {
                        Text("Registrado: "+voiceFormattedTime(recordedAt),
                            fontSize=10.sp,lineHeight=15.sp,color=theme.accent,
                            modifier=Modifier.padding(top=6.dp))
                    } else if(completed||active) {
                        Text("Fecha anterior no registrada",
                            fontSize=10.sp,color=theme.dim,
                            modifier=Modifier.padding(top=6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeading(title:String,onClose:()->Unit,theme:LumiDesignTokens){
    Row(Modifier.fillMaxWidth().padding(bottom=13.dp),verticalAlignment=Alignment.CenterVertically){
        Text(title,color=theme.muted,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
        IconButton(onClick=onClose,modifier=Modifier.size(35.dp).background(theme.panel2,CircleShape)){
            Icon(Icons.Outlined.Close,"Cerrar",tint=theme.text,modifier=Modifier.size(18.dp))
        }
    }
}
