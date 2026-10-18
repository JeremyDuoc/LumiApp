package com.jeremy.lumi.ui.screens.forum

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeremy.lumi.ui.theme.LumiThemeTokens

/** Tu voz · Migración de Lumi_Tu_Voz_Animaciones_v3.html; modo local explícito. */
@Composable
fun ForumScreen(viewModel: ForumViewModel = hiltViewModel()) {
    val t = LumiThemeTokens.current
    val posts by viewModel.proposals.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var order by rememberSaveable { mutableStateOf("Más apoyadas") }
    var expanded by remember { mutableStateOf(false) }
    var create by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var reportId by rememberSaveable { mutableStateOf<String?>(null) }
    val list = remember(posts, tab, order) {
        val filtered = posts.filter { !it.hiddenByMe && when(tab) {
            1 -> it.status in ForumViewModel.developmentStates
            2 -> it.votedByMe
            else -> true
        } }
        when(order) {
            "Más recientes" -> filtered.sortedByDescending { it.createdAt }
            "Más comentadas" -> filtered.sortedWith(compareByDescending<VoiceProposal> { it.comments.orEmpty().size }.thenByDescending { it.createdAt })
            else -> filtered.sortedWith(compareByDescending<VoiceProposal> { it.votes }.thenByDescending { it.createdAt })
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(t.bg),
        contentPadding = PaddingValues(start=22.dp, end=22.dp, top=17.dp, bottom=28.dp)
    ) {
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("Tu voz", fontSize=32.sp, fontWeight=FontWeight.ExtraBold,
                    letterSpacing=(-1.45).sp, color=t.text, modifier=Modifier.weight(1f))
                Box(Modifier.size(35.dp).clip(CircleShape).background(t.panel)
                    .border(1.dp,t.line,CircleShape)
                    .clickable(role=Role.Button, onClickLabel="Cómo funciona") { help=true },
                    contentAlignment=Alignment.Center) {
                    Icon(Icons.Outlined.Info, "Cómo funciona", tint=t.muted, modifier=Modifier.size(18.dp))
                }
            }
            Text("Las mejores ideas nacen cuando nos escuchamos.",
                fontSize=12.sp, lineHeight=19.sp, color=t.muted,
                modifier=Modifier.padding(top=8.dp,bottom=16.dp))
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Box(Modifier.height(44.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(t.rose,t.accent)))
                    .clickable(role=Role.Button,onClickLabel="Proponer una idea") { create=true }
                    .padding(horizontal=17.dp),contentAlignment=Alignment.Center) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Add,null,tint=Color(0xFF21172A),modifier=Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Proponer una idea",fontSize=12.sp,fontWeight=FontWeight.ExtraBold,color=Color(0xFF21172A))
                    }
                }
                Text("Sin nombre público\nTus ideas importan",fontSize=10.sp,lineHeight=15.sp,color=t.muted)
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color=t.line.copy(alpha=.7f))
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("Lo que estamos creando",fontSize=13.sp,fontWeight=FontWeight.Bold,color=t.text)
                Text("Hoja de ruta",fontSize=10.sp,color=t.muted)
            }
            Spacer(Modifier.height(11.dp))
            val featured=posts.firstOrNull { it.status=="En desarrollo" && !it.hiddenByMe }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                    .background(Brush.horizontalGradient(listOf(t.accent.copy(alpha=.12f),t.panel)))
                    .border(1.dp,t.accent.copy(alpha=.35f),RoundedCornerShape(17.dp))
                    .then(if(featured!=null) Modifier.clickable { selectedId=featured.id } else Modifier)
                    .padding(16.dp)
            ) {
                Text(if(featured!=null) "EN DESARROLLO" else "PRÓXIMAMENTE",
                    fontSize=10.sp,fontWeight=FontWeight.Bold,color=t.accent)
                Spacer(Modifier.height(10.dp))
                Text(featured?.title ?: "Novedades del equipo Lumi",
                    fontSize=16.sp,fontWeight=FontWeight.Bold,color=t.text,lineHeight=21.sp)
                Spacer(Modifier.height(8.dp))
                Text(if(featured!=null) "Ver seguimiento de esta idea  ›"
                    else "Las actualizaciones aparecerán aquí cuando exista un canal comunitario verificado.",
                    fontSize=11.sp,color=t.muted,lineHeight=17.sp)
            }
            Spacer(Modifier.height(21.dp))
            VoiceTabs(tab) { tab=it }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(when(tab){0->"Ideas de Lumi";1->"En camino";else->"Ideas que apoyaste"},
                    color=t.text,fontSize=14.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                Box {
                    TextButton(onClick={expanded=true}) { Text(order,color=t.muted,fontSize=10.sp) }
                    DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},
                        modifier=Modifier.background(t.panel)) {
                        listOf("Más apoyadas","Más recientes","Más comentadas").forEach { label ->
                            DropdownMenuItem(text={Text(label,color=t.text,fontSize=12.sp)},
                                onClick={order=label;expanded=false})
                        }
                    }
                }
            }
        }
        if(list.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical=30.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text(when(tab){0->"Tu primera idea puede comenzar aquí";1->"Todavía no hay ideas en camino";else->"Todavía no has apoyado ideas"},
                    color=t.text,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(if(tab==0) "Propón una mejora para probar este espacio en el dispositivo."
                    else "Aquí verás las propuestas cuando cumplan esta condición.",
                    color=t.muted,fontSize=12.sp,lineHeight=18.sp)
            }
        } else items(list,key={it.id}) { post ->
            VoiceProposalRow(post,onVote={viewModel.toggleVote(post.id)},onDetail={selectedId=post.id})
        }
        item {
            Text("Modo local de prueba · Los votos, las ideas y los comentarios no se sincronizan con otras usuarias.",
                color=t.dim,fontSize=10.sp,lineHeight=16.sp,
                modifier=Modifier.padding(top=19.dp,bottom=10.dp))
        }
    }
    if(create) VoiceCreateSheet(onDismiss={create=false}) { a,b,c ->
        if(viewModel.publish(a,b,c)){create=false;tab=0;order="Más recientes"}
    }
    if(help) AlertDialog(onDismissRequest={help=false},
        title={Text("Cómo funciona Tu voz")},
        text={Text("Propón mejoras, apoya propuestas y comenta sin mostrar tu nombre. Los votos ayudan a priorizar, pero no garantizan el desarrollo.\n\nEsta versión almacena todo solo en el dispositivo. No existe aún una comunidad sincronizada ni moderación remota. No publiques datos médicos o identificables.")},
        confirmButton={TextButton(onClick={help=false}){Text("Entendido")}})
    posts.firstOrNull { it.id==selectedId }?.let { current ->
        VoiceDetailSheet(current,onDismiss={selectedId=null},
            onVote={viewModel.toggleVote(current.id)},
            onComment={viewModel.comment(current.id,it)},
            onReport={reportId=current.id},
            onHide={viewModel.hide(current.id);selectedId=null})
    }
    if(reportId!=null) AlertDialog(onDismissRequest={reportId=null},
        title={Text("Reportar propuesta")},
        text={Text("Este reporte se marcará solo en tu dispositivo. No se enviará a moderadores hasta que exista un backend seguro.")},
        confirmButton={TextButton(onClick={reportId?.let(viewModel::report);reportId=null}){Text("Marcar localmente")}},
        dismissButton={TextButton(onClick={reportId=null}){Text("Cancelar")}})
}

@Composable
private fun VoiceTabs(selected:Int,onSelect:(Int)->Unit) {
    val t=LumiThemeTokens.current
    val duration=500
    BoxWithConstraints(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(13.dp))
        .background(t.panel).border(1.dp,t.line,RoundedCornerShape(13.dp))) {
        val segment=(maxWidth-8.dp)/3
        val x by animateDpAsState(segment*selected,
            tween(duration,easing=CubicBezierEasing(.33f,.83f,.99f,.98f)),
            label="voice_slide_indicator")
        Box(Modifier.offset(x=x+4.dp,y=4.dp).width(segment).height(38.dp)
            .clip(RoundedCornerShape(9.dp)).background(t.panel2)
            .border(.7.dp,t.accent.copy(alpha=.3f),RoundedCornerShape(9.dp))) {
            Box(Modifier.align(Alignment.TopCenter).fillMaxWidth(.95f).height(2.dp)
                .background(t.accent.copy(alpha=.47f)))
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(.95f).height(2.dp)
                .background(t.accent.copy(alpha=.47f)))
        }
        Row(Modifier.fillMaxSize().padding(4.dp)) {
            listOf("Ideas","En camino","Mis votos").forEachIndexed { index,label ->
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(9.dp))
                    .clickable(role=Role.Tab,onClickLabel=label){onSelect(index)}
                    .semantics { stateDescription=if(index==selected)"Seleccionada" else "No seleccionada" },
                    contentAlignment=Alignment.Center) {
                    Text(label,color=if(index==selected)t.text else t.muted,
                        fontSize=11.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VoiceVote(post:VoiceProposal,onVote:()->Unit,detail:Boolean=false) {
    val t=LumiThemeTokens.current
    val duration=580
    val scale by animateFloatAsState(if(post.votedByMe)1.1f else 1f,
        tween(duration,easing=CubicBezierEasing(.18f,1f,.28f,1f)),
        label="voice_soft_vote")
    val shape=RoundedCornerShape(13.dp)
    Column(Modifier.width(if(detail)75.dp else 48.dp)
        .height(if(detail)49.dp else 63.dp)
        .clip(shape).background(if(post.votedByMe)t.accentSoft else t.panel)
        .border(1.dp,if(post.votedByMe)t.accent.copy(alpha=.36f)else t.line,shape)
        .clickable(role=Role.Button,onClickLabel=if(post.votedByMe)"Quitar apoyo" else "Apoyar"){onVote()},
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Box(Modifier.size(23.dp).graphicsLayer { scaleX=scale;scaleY=scale }
            .shadow(if(post.votedByMe)4.dp else 0.dp,CircleShape,
                ambientColor=t.rose.copy(alpha=.32f),spotColor=t.accent.copy(alpha=.3f))
            .clip(CircleShape).background(Color(0xFF09080D))
            .border(1.dp,t.line.copy(alpha=.8f),CircleShape),
            contentAlignment=Alignment.Center) {
            if(post.votedByMe)Icon(Icons.Outlined.Check,null,
                tint=Color(0xFFF6F2FA),modifier=Modifier.size(15.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(post.votes.toString(),fontSize=12.sp,fontWeight=FontWeight.Bold,
            color=if(post.votedByMe)t.accent else t.muted)
    }
}

@Composable
private fun VoiceProposalRow(post:VoiceProposal,onVote:()->Unit,onDetail:()->Unit) {
    val t=LumiThemeTokens.current
    Row(Modifier.fillMaxWidth().padding(vertical=15.dp),
        verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(13.dp)) {
        VoiceVote(post,onVote)
        Column(Modifier.weight(1f)
            .clickable(role=Role.Button,onClickLabel="Ver detalles de la idea"){onDetail()}) {
            Text(post.title,color=t.text,fontSize=13.sp,lineHeight=19.sp,fontWeight=FontWeight.Bold,
                maxLines=3,overflow=TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(post.category+" · ",fontSize=10.sp,color=t.muted)
                Icon(Icons.Outlined.ChatBubbleOutline,null,tint=t.muted,modifier=Modifier.size(13.dp))
                Text(" "+post.comments.orEmpty().size.toString(),fontSize=10.sp,color=t.muted)
            }
            Spacer(Modifier.height(5.dp))
            Text(post.status,fontSize=10.sp,fontWeight=FontWeight.Bold,color=t.accent)
        }
    }
    HorizontalDivider(color=t.line)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceCreateSheet(onDismiss:()->Unit,onPublish:(String,String,String)->Unit) {
    val t=LumiThemeTokens.current
    var title by rememberSaveable{mutableStateOf("")}
    var desc by rememberSaveable{mutableStateOf("")}
    var category by rememberSaveable{mutableStateOf("Seguimiento")}
    var categoryMenu by remember{mutableStateOf(false)}
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=t.bg) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
            .padding(start=22.dp,end=22.dp,bottom=26.dp)) {
            VoiceSheetTop("Nueva propuesta",onDismiss)
            Text("Proponer una idea",fontSize=23.sp,fontWeight=FontWeight.ExtraBold,color=t.text)
            Spacer(Modifier.height(12.dp))
            Text("Título",fontSize=12.sp,fontWeight=FontWeight.Bold,color=t.text)
            OutlinedTextField(title,{if(it.length<=100)title=it},
                modifier=Modifier.fillMaxWidth(),singleLine=true,
                placeholder={Text("¿Qué mejorarías en Lumi?")})
            Spacer(Modifier.height(11.dp))
            Text("Categoría",fontSize=12.sp,fontWeight=FontWeight.Bold,color=t.text)
            Box {
                TextButton(onClick={categoryMenu=true}){Text(category+"  ▾",color=t.accent)}
                DropdownMenu(expanded=categoryMenu,onDismissRequest={categoryMenu=false}) {
                    listOf("Seguimiento","Diseño","Privacidad","Accesibilidad","Otros").forEach { label ->
                        DropdownMenuItem(text={Text(label)},onClick={category=label;categoryMenu=false})
                    }
                }
            }
            Text("Describe tu idea",fontSize=12.sp,fontWeight=FontWeight.Bold,color=t.text)
            OutlinedTextField(desc,{if(it.length<=900)desc=it},
                modifier=Modifier.fillMaxWidth().heightIn(min=110.dp),minLines=3,maxLines=6,
                placeholder={Text("¿Cómo ayudaría a las usuarias?")})
            Spacer(Modifier.height(10.dp))
            Text("Sin nombre visible. No incluyas datos médicos o personales. Esta versión guarda solo en tu dispositivo.",
                fontSize=11.sp,lineHeight=17.sp,color=t.muted)
            Spacer(Modifier.height(16.dp))
            VoicePrimary("Guardar propuesta local",
                enabled=title.trim().length in 8..100 && desc.trim().length in 15..900) {
                onPublish(title,desc,category)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceDetailSheet(
    post:VoiceProposal,onDismiss:()->Unit,onVote:()->Unit,
    onComment:(String)->Boolean,onReport:()->Unit,onHide:()->Unit
) {
    val t=LumiThemeTokens.current
    var input by rememberSaveable(post.id){mutableStateOf("")}
    var more by remember{mutableStateOf(false)}
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=t.bg) {
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
            contentPadding=PaddingValues(start=22.dp,end=22.dp,bottom=32.dp)) {
            item {
                VoiceSheetTop(post.category,onDismiss)
                Text(post.status,color=t.accent,fontSize=11.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text(post.title,color=t.text,fontSize=23.sp,lineHeight=28.sp,
                    fontWeight=FontWeight.ExtraBold)
                Spacer(Modifier.height(10.dp))
                Text(post.description,color=t.muted,fontSize=12.sp,lineHeight=19.sp)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    VoiceVote(post,onVote,detail=true)
                    Spacer(Modifier.width(12.dp))
                    Text(if(post.votedByMe)"Ya apoyaste esta idea" else "Apoya esta propuesta",
                        color=t.muted,fontSize=11.sp,modifier=Modifier.weight(1f))
                    Box {
                        IconButton(onClick={more=true}) {
                            Icon(Icons.Outlined.MoreHoriz,"Opciones",tint=t.muted)
                        }
                        DropdownMenu(expanded=more,onDismissRequest={more=false}) {
                            DropdownMenuItem(text={Text("Reportar localmente")},
                                leadingIcon={Icon(Icons.Outlined.Flag,null)},onClick={more=false;onReport()})
                            DropdownMenuItem(text={Text("Ocultar en este dispositivo")},
                                onClick={more=false;onHide()})
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color=t.line)
                Spacer(Modifier.height(16.dp))
                Text("Seguimiento",color=t.text,fontSize=13.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                val timeline=listOf("Recibida","En revisión","Aceptada","En desarrollo","Disponible")
                val current=timeline.indexOf(post.status)
                timeline.forEachIndexed { i,label ->
                    Row(Modifier.padding(bottom=12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape)
                            .background(if(i<=current)t.accent else t.line))
                        Spacer(Modifier.width(11.dp))
                        Text(label,color=if(i<=current)t.text else t.dim,fontSize=11.sp)
                        if(i==current)Text(" · Actual",color=t.accent,fontSize=10.sp)
                    }
                }
                if(post.status=="No prevista")Text("No prevista",color=t.rose,fontSize=11.sp)
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color=t.line)
                Spacer(Modifier.height(17.dp))
                Text("Conversación · "+post.comments.orEmpty().size.toString(),
                    color=t.text,fontSize=13.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
            }
            items(post.comments.orEmpty(),key={it.id}) { comment ->
                Column(Modifier.fillMaxWidth().padding(vertical=11.dp)) {
                    Text(if(comment.team)"Equipo Lumi" else "Anónima",
                        color=if(comment.team)t.accent else t.text,
                        fontSize=11.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(comment.text,color=t.muted,fontSize=12.sp,lineHeight=18.sp)
                }
                HorizontalDivider(color=t.line.copy(alpha=.7f))
            }
            item {
                if(post.comments.orEmpty().isEmpty())Text("Todavía no hay comentarios.",
                    color=t.muted,fontSize=12.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(input,{if(it.length<=500)input=it},
                    modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=4,
                    label={Text("Escribe un comentario")})
                Spacer(Modifier.height(8.dp))
                Text("Tu comentario permanece en este dispositivo. No incluyas datos personales o médicos.",
                    color=t.muted,fontSize=10.sp,lineHeight=16.sp)
                Spacer(Modifier.height(12.dp))
                VoicePrimary("Guardar comentario local",enabled=input.trim().length in 3..500) {
                    if(onComment(input))input=""
                }
            }
        }
    }
}

@Composable
private fun VoiceSheetTop(label:String,onDismiss:()->Unit) {
    val t=LumiThemeTokens.current
    Row(Modifier.fillMaxWidth().padding(bottom=13.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(label,color=t.muted,fontSize=11.sp,fontWeight=FontWeight.Bold,
            modifier=Modifier.weight(1f))
        Box(Modifier.size(35.dp).clip(CircleShape).background(t.panel)
            .border(1.dp,t.line,CircleShape)
            .clickable(role=Role.Button,onClickLabel="Cerrar"){onDismiss()},
            contentAlignment=Alignment.Center) {
            Icon(Icons.Outlined.Close,"Cerrar",tint=t.text,modifier=Modifier.size(17.dp))
        }
    }
}

@Composable
private fun VoicePrimary(label:String,enabled:Boolean,onClick:()->Unit) {
    val t=LumiThemeTokens.current
    Box(Modifier.fillMaxWidth().height(49.dp).clip(CircleShape)
        .background(Brush.horizontalGradient(
            if(enabled)listOf(t.rose,t.accent) else listOf(t.line,t.line)))
        .clickable(enabled=enabled,role=Role.Button,onClickLabel=label){onClick()},
        contentAlignment=Alignment.Center) {
        Text(label,color=if(enabled)Color(0xFF21172A) else t.dim,
            fontSize=12.sp,fontWeight=FontWeight.ExtraBold)
    }
}
