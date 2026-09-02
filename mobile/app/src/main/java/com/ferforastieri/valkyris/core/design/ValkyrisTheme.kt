package com.ferforastieri.valkyris.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

val CanvasLight=Color(0xFFE8E9E5);val SurfaceLight=Color(0xFFF9FAF7);val InkLight=Color(0xFF242724);val MutedLight=Color(0xFF70766F);val SignalLight=Color(0xFF7BD66F);val AlarmLight=Color(0xFFEC625B)
val CanvasDark=Color(0xFF101310);val SurfaceDark=Color(0xFF1A1F1A);val InkDark=Color(0xFFF1F4EF);val MutedDark=Color(0xFFA6AFA5);val SignalDark=Color(0xFF8AE07D);val AlarmDark=Color(0xFFFF766E)
object ColorTokens { val BrandTile=Color(0xFF202420) }
private val light=lightColorScheme(primary=InkLight,onPrimary=SurfaceLight,secondary=SignalLight,onSecondary=InkLight,error=AlarmLight,background=CanvasLight,onBackground=InkLight,surface=SurfaceLight,onSurface=InkLight,surfaceVariant=Color(0xFFF0F1EE),onSurfaceVariant=MutedLight,outline=Color(0xFFD2D5D0),outlineVariant=Color(0xFFE0E2DD))
private val dark=darkColorScheme(primary=InkDark,onPrimary=CanvasDark,secondary=SignalDark,onSecondary=CanvasDark,error=AlarmDark,background=CanvasDark,onBackground=InkDark,surface=SurfaceDark,onSurface=InkDark,surfaceVariant=Color(0xFF222822),onSurfaceVariant=MutedDark,outline=Color(0xFF343A34),outlineVariant=Color(0xFF2B312B))
private val typography=Typography(
    headlineLarge=TextStyle(fontSize=40.sp,lineHeight=42.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-1.2).sp),
    headlineMedium=TextStyle(fontSize=30.sp,lineHeight=34.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-.7).sp),
    titleLarge=TextStyle(fontSize=22.sp,lineHeight=27.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-.3).sp),
    titleMedium=TextStyle(fontSize=16.sp,lineHeight=21.sp,fontWeight=FontWeight.SemiBold),
    labelLarge=TextStyle(fontSize=14.sp,lineHeight=18.sp,fontWeight=FontWeight.SemiBold),
)
@Composable
fun ValkyrisTheme(mode:String="system",content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (mode=="dark"||(mode=="system"&&isSystemInDarkTheme())) dark else light,
        shapes = Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        ),
        typography = typography,
        content = content,
    )
}
@Composable fun SignalLine(modifier:Modifier=Modifier,color:Color=MaterialTheme.colorScheme.secondary){Canvas(modifier){val p=Path();p.moveTo(0f,size.height*.58f);p.cubicTo(size.width*.12f,size.height*.58f,size.width*.12f,size.height*.18f,size.width*.24f,size.height*.18f);p.cubicTo(size.width*.36f,size.height*.18f,size.width*.34f,size.height*.84f,size.width*.48f,size.height*.84f);p.cubicTo(size.width*.62f,size.height*.84f,size.width*.6f,size.height*.38f,size.width*.72f,size.height*.38f);p.cubicTo(size.width*.84f,size.height*.38f,size.width*.86f,size.height*.58f,size.width,size.height*.58f);drawPath(p,color,style=Stroke(width=4.dp.toPx(),cap=StrokeCap.Round))}}
