package com.jeremy.lumi.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeremy.lumi.ui.theme.LumiThemeTokens

/**
 * Encabezado editorial que reproduce fielmente la identidad visual aprobada:
 * - Eyebrow en mayúsculas espaciado
 * - Título con contraste tipográfico (Sans-serif moderno + Serif Itálico de acento)
 * - Subtítulo / Descripción relajada
 */
@Composable
fun LumiEditorialHeading(
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    titleNormal: String,
    titleSerifItalic: String,
    titleNormalTrailing: String = "",
    subtitle: String? = null,
    description: String? = null,
    serifColor: Color? = null,
    titleSize: TextUnit = 36.sp,
    serifSize: TextUnit = 40.sp
) {
    val tokens = LumiThemeTokens.current
    val accentColor = serifColor ?: tokens.accent

    Column(modifier = modifier) {
        if (!eyebrow.isNullOrBlank()) {
            Text(
                text = eyebrow.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = tokens.muted
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        val annotatedTitle = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = tokens.text,
                    fontSize = titleSize,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1.5).sp,
                    fontFamily = FontFamily.SansSerif
                )
            ) {
                append(titleNormal)
            }

            withStyle(
                SpanStyle(
                    color = accentColor,
                    fontSize = serifSize,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-0.5).sp,
                    fontFamily = FontFamily.Serif
                )
            ) {
                append(titleSerifItalic)
            }

            if (titleNormalTrailing.isNotEmpty()) {
                withStyle(
                    SpanStyle(
                        color = tokens.text,
                        fontSize = titleSize,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1.5).sp,
                        fontFamily = FontFamily.SansSerif
                    )
                ) {
                    append(titleNormalTrailing)
                }
            }
        }

        Text(
            text = annotatedTitle,
            lineHeight = (titleSize.value * 1.08f).sp
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.accent
            )
        }

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = tokens.muted
            )
        }
    }
}
