package com.jeremy.lumi.ui.components

import androidx.compose.animation.core.Animatable
import android.view.View
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.hypot
import kotlin.math.max

/**
 * Shape circular para la revelación de tema de 690ms según especificación HTML v11:
 * clipPath: circle(0px at cx cy) -> circle(radius at cx cy)
 * Easing: cubic-bezier(.2, .83, .2, 1)
 */
class CircularRevealShape(
    private val progress: Float,
    private val center: Offset?
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = center?.x ?: (size.width / 2f)
        val cy = center?.y ?: (size.height / 2f)
        val maxRadius = hypot(
            max(cx, size.width - cx),
            max(cy, size.height - cy)
        ) + 40f
        val currentRadius = maxRadius * progress.coerceIn(0f, 1f)

        val path = Path().apply {
            addOval(
                Rect(
                    center = Offset(cx, cy),
                    radius = currentRadius
                )
            )
        }
        return Outline.Generic(path)
    }
}

/**
 * Capa de revelación circular de 690ms con curva cubic-bezier(.2, .83, .2, 1)
 * fiel a la especificación del prototipo HTML v11:
 * - Mantiene la pantalla actual debajo sin destello blanco.
 * - Expande un círculo desde la posición tocada o el centro.
 * - Al completar los 690ms llama a [onFinished].
 */
@Composable
fun LumiThemeCircularRevealOverlay(
    targetBgColor: Color,
    targetAccentColor: Color,
    center: Offset?,
    isDark: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 690,
                easing = CubicBezierEasing(0.2f, 0.83f, 0.2f, 1f)
            )
        )
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(CircularRevealShape(progress.value, center))
            .background(targetBgColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        0f to targetAccentColor.copy(alpha = if (isDark) 0.16f else 0.08f),
                        0.7f to Color.Transparent,
                        center = center ?: Offset.Zero
                    )
                )
        )
    }
}

/**
 * Captura efímera en memoria de la interfaz REAL anterior (sin escribirla a disco).
 * Debe ejecutarse EN el callback de pulsación y ANTES de actualizar DataStore.
 * La ModalBottomSheet está en otra ventana, y por tanto no forma parte de esta captura:
 * coincide con el prototipo, cuyo clon también suprime .theme-layer.
 */
fun captureThemeRevealSnapshot(view: View): ImageBitmap? {
    if (view.width <= 0 || view.height <= 0) return null
    return runCatching {
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        view.draw(canvas)
        bmp.asImageBitmap()
    }.getOrNull()
}

/**
 * Equivalencia a cloneNode(theme=new) + clip-path circle() de HTML:
 * La escena NUEVA se recompone inmediatamente debajo; el bitmap ANTERIOR
 * solo cubre el exterior del círculo expansivo. No hay capas de color falso.
 * Por seguridad no guarda datos y libera el bitmap al terminar.
 */
@Composable
fun LumiThemeSnapshotReveal(
    oldFrame: ImageBitmap,
    sourceCenter: Offset?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember(oldFrame) { Animatable(0f) }
    LaunchedEffect(oldFrame) {
        progress.animateTo(1f, tween(690, easing = CubicBezierEasing(.2f, .83f, .2f, 1f)))
        onFinished()
    }
    val interaction = remember { MutableInteractionSource() }
    Canvas(
        modifier.fillMaxSize().clickable(
            interactionSource = interaction,
            indication = null,
            onClick = {} // Bloquea pulsaciones mientras la escena se revela.
        )
    ) {
        val cx = sourceCenter?.x?.takeIf { it.isFinite() }?.coerceIn(0f, size.width) ?: size.width / 2f
        val cy = sourceCenter?.y?.takeIf { it.isFinite() }?.coerceIn(0f, size.height) ?: size.height / 2f
        val radius = kotlin.math.hypot(
            max(cx, size.width - cx), max(cy, size.height - cy)
        ) + 18f
        val hole = Path().apply { addOval(Rect(Offset(cx, cy), radius * progress.value)) }
        clipPath(hole, clipOp = ClipOp.Difference) {
            drawImage(
                image = oldFrame,
                srcSize = IntSize(oldFrame.width, oldFrame.height),
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
            )
        }
    }
}
