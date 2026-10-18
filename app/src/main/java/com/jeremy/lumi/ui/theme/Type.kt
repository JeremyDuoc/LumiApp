package com.jeremy.lumi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Sistema tipográfico Lumi — escala extraída del HTML.
 * El HTML declara Inter/DM Sans sin cargarlos por @font-face. Por eso aquí
 * utilizamos la familia sans del sistema para no fingir que son el mismo font.
 * Se pueden añadir las fuentes con licencia en res/font y cambiar LumiSans
 * si se desea fijar EXACTAMENTE el mismo archivo en Android y en HTML.
 */
val LumiSans: FontFamily = FontFamily.SansSerif
val LumiEditorialSerif: FontFamily = FontFamily.Serif

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 52.sp, letterSpacing = (-1.8).sp),
    displayMedium = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 44.sp, letterSpacing = (-1.4).sp),
    displaySmall = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-1.1).sp),
    headlineLarge = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.Bold, fontSize = 29.sp, lineHeight = 34.sp, letterSpacing = (-1.1).sp),
    headlineMedium = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 32.sp, letterSpacing = (-1.1).sp),
    headlineSmall = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 29.sp, letterSpacing = (-0.7).sp),
    titleLarge = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.6).sp),
    titleMedium = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp),
    titleSmall = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp, letterSpacing = 0.sp),
    bodyMedium = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = .08.sp),
    bodySmall = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = .08.sp),
    labelLarge = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = .1.sp),
    labelMedium = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = .15.sp),
    labelSmall = TextStyle(fontFamily = LumiSans, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = .2.sp)
)
