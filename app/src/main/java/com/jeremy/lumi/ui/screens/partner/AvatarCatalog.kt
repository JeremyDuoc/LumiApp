package com.jeremy.lumi.ui.screens.partner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Catálogo de avatares vectoriales del Modo Pareja.
 *
 * Cada avatar es un id estable (se guarda como String en Firestore), un
 * gradiente y un ícono vectorial. Sin emojis ni imágenes rasterizadas:
 * escalan perfecto en cualquier densidad y respetan la estética premium.
 */
data class AvatarSpec(
    val id: String,
    val gradient: List<Color>,
    val icon: ImageVector
)

object AvatarCatalog {

    val all: List<AvatarSpec> = listOf(
        AvatarSpec("luna",     listOf(Color(0xFF6C5CE7), Color(0xFF4834D4)), Icons.Rounded.Nightlight),
        AvatarSpec("sol",      listOf(Color(0xFFFFB75E), Color(0xFFED8F03)), Icons.Rounded.WbSunny),
        AvatarSpec("flor",     listOf(Color(0xFFFF8FA3), Color(0xFFE84393)), Icons.Rounded.LocalFlorist),
        AvatarSpec("oceano",   listOf(Color(0xFF48C6EF), Color(0xFF2F80ED)), Icons.Rounded.WaterDrop),
        AvatarSpec("estrella", listOf(Color(0xFFFAD961), Color(0xFFF76B1C)), Icons.Rounded.Star),
        AvatarSpec("bosque",   listOf(Color(0xFF56AB2F), Color(0xFF2E7D32)), Icons.Rounded.Spa),
        AvatarSpec("brisa",    listOf(Color(0xFF89F7FE), Color(0xFF66A6FF)), Icons.Rounded.Air),
        AvatarSpec("fuego",    listOf(Color(0xFFFF7E5F), Color(0xFFEB3349)), Icons.Rounded.LocalFireDepartment),
        AvatarSpec("nube",     listOf(Color(0xFFBDC3C7), Color(0xFF8E9EAB)), Icons.Rounded.Cloud),
        AvatarSpec("corazon",  listOf(Color(0xFFF857A6), Color(0xFFEF3B36)), Icons.Rounded.Favorite),
        AvatarSpec("melodia",  listOf(Color(0xFFA18CD1), Color(0xFF7F53AC)), Icons.Rounded.MusicNote),
        AvatarSpec("destello", listOf(Color(0xFF43E97B), Color(0xFF38B2AC)), Icons.Rounded.AutoAwesome)
    )

    fun byId(id: String?): AvatarSpec? = all.firstOrNull { it.id == id }

    /** Gradiente determinista para quien aún no eligió avatar (según su nombre). */
    fun fallbackGradient(seed: String?): List<Color> {
        val rawHash = (seed ?: "").hashCode()
        val positiveHash = if (rawHash == Int.MIN_VALUE) 0 else abs(rawHash)
        val index = positiveHash % all.size
        return all[index].gradient
    }
}

/**
 * Círculo de avatar reutilizable en todo el Modo Pareja.
 * Si no hay avatar elegido, muestra la inicial del nombre sobre un gradiente
 * determinista — nunca un hueco vacío.
 */
@Composable
fun PartnerAvatarView(
    avatarId: String?,
    fallbackName: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val spec = AvatarCatalog.byId(avatarId)
    val gradient = spec?.gradient ?: AvatarCatalog.fallbackGradient(fallbackName)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradient)),
        contentAlignment = Alignment.Center
    ) {
        if (spec != null) {
            Icon(
                imageVector = spec.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(size * 0.52f)
            )
        } else {
            Text(
                text = fallbackName?.trim()?.take(1)?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp
            )
        }
    }
}

/**
 * Rejilla de selección de avatar para el onboarding (4 columnas).
 * El seleccionado se marca con un anillo del color primario.
 */
@Composable
fun AvatarPickerGrid(
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AvatarCatalog.all.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { spec ->
                    val selected = spec.id == selectedId
                    val ringWidth by animateDpAsState(
                        targetValue = if (selected) 3.dp else 0.dp,
                        label = "anillo_avatar"
                    )
                    val ringColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        label = "color_anillo_avatar"
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .border(ringWidth, ringColor, CircleShape)
                            .padding(4.dp)
                            .clickable { onSelect(spec.id) }
                    ) {
                        PartnerAvatarView(
                            avatarId = spec.id,
                            fallbackName = null,
                            size = 56.dp
                        )
                    }
                }
            }
        }
    }
}
