package ru.routealert.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val GlassBlue = Color(0xFF2F6BFF)

object Glass {
    val LightFill = listOf(
        Color(0xEE1E1E22),
        Color(0xDD141418)
    )
    val DarkFill = listOf(
        Color(0xEE1E1E22),
        Color(0xDD141418)
    )
    val MapFill = listOf(
        Color(0xEE1E1E22),
        Color(0xDD16161A)
    )
    val MapFillStrong = listOf(
        Color(0xF51A1A1E),
        Color(0xEE141418)
    )
    val SheetFill = listOf(
        Color(0xF818181C),
        Color(0xF2121216)
    )
    val PanelFill = listOf(
        Color(0x22FFFFFF),
        Color(0x0CFFFFFF)
    )
    val Stroke = Color.White.copy(alpha = 0.18f)
    val StrokeSoft = Color.White.copy(alpha = 0.10f)
    val Selected = listOf(
        GlassBlue.copy(alpha = 0.95f),
        GlassBlue.copy(alpha = 0.80f)
    )
    val Danger = listOf(
        Signal.copy(alpha = 0.94f),
        Signal.copy(alpha = 0.74f)
    )
}

fun Modifier.liquidGlass(
    shape: Shape,
    colors: List<Color> = Glass.MapFill,
    stroke: Color = Glass.Stroke,
    elevation: Dp = 0.dp
): Modifier = this
    .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, clip = false) else Modifier)
    .clip(shape)
    .background(Brush.verticalGradient(colors), shape)
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                stroke,
                Color.White.copy(alpha = 0.08f)
            )
        ),
        shape = shape
    )

@Composable
fun LiquidGlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    colors: List<Color> = Glass.MapFill,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(shape = shape, colors = colors),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun LiquidGlassCircle(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    diameter: Dp = 52.dp,
    colors: List<Color> = Glass.MapFillStrong,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(diameter)
            .liquidGlass(shape = CircleShape, colors = colors)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun LiquidGlassChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .liquidGlass(
                shape = shape,
                colors = if (selected) Glass.Selected else Glass.MapFill,
                stroke = if (selected) Color.White.copy(alpha = 0.45f) else Glass.StrokeSoft
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            softWrap = false
        )
    }
}
