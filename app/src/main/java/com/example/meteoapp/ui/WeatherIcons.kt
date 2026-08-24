package com.quaderno.appmeteo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.quaderno.appmeteo.data.Condition
import com.quaderno.appmeteo.ui.theme.CloudBottom
import com.quaderno.appmeteo.ui.theme.CloudOutline
import com.quaderno.appmeteo.ui.theme.CloudShadow
import com.quaderno.appmeteo.ui.theme.CloudTop
import com.quaderno.appmeteo.ui.theme.SunCore
import com.quaderno.appmeteo.ui.theme.SunEdge
import com.quaderno.appmeteo.ui.theme.SunGlow

/**
 * Icona meteo in stile "puffy" (morbida, quasi 3D): sole con bagliore radiale
 * e nuvola con leggero gradiente verticale, ispirata al mood dell'immagine di riferimento.
 * Nessun asset esterno: tutto disegnato a runtime, quindi si adatta a qualsiasi densità schermo.
 */
@Composable
fun WeatherIcon(condition: Condition, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        fun drawSun(cx: Float, cy: Float, r: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SunGlow, SunCore, SunEdge),
                    center = Offset(cx - r * 0.25f, cy - r * 0.3f),
                    radius = r * 1.3f
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }

        fun drawCloud(cx: Float, cy: Float, scale: Float, color1: Color = CloudTop, color2: Color = CloudBottom) {
            val brush = Brush.verticalGradient(listOf(color1, color2))
            val r = 0.16f * w * scale
            val lobes = listOf(
                Offset(cx - r * 1.3f, cy) to r * 1.1f,
                Offset(cx, cy - r * 0.4f) to r * 1.4f,
                Offset(cx + r * 1.3f, cy) to r * 1.15f,
                Offset(cx, cy + r * 0.5f) to r * 1.5f
            )
            // 1) ombra morbida spostata in basso: senza, la nuvola chiara si perde su sfondo chiaro
            val shadowDy = r * 0.22f
            lobes.forEach { (center, radius) ->
                drawCircle(CloudShadow, radius = radius, center = Offset(center.x, center.y + shadowDy))
            }
            // 2) riempimento
            lobes.forEach { (center, radius) -> drawCircle(brush, radius = radius, center = center) }
            // 3) contorno sottile per dare "corpo" al bordo
            lobes.forEach { (center, radius) ->
                drawCircle(CloudOutline, radius = radius, center = center, style = Stroke(width = w * 0.006f))
            }
        }

        when (condition) {
            Condition.SOLE -> {
                drawSun(w * 0.5f, h * 0.5f, w * 0.32f)
            }
            Condition.POCO_NUVOLOSO -> {
                drawSun(w * 0.4f, h * 0.42f, w * 0.24f)
                drawCloud(w * 0.58f, h * 0.62f, 0.9f)
            }
            Condition.NUVOLOSO, Condition.NEBBIA -> {
                drawCloud(w * 0.5f, h * 0.5f, 1.05f, CloudTop, CloudBottom)
            }
            Condition.PIOGGIA -> {
                drawCloud(w * 0.5f, h * 0.42f, 0.95f)
                val dropColor = Color(0xFF7FA1FF)
                val xs = listOf(0.35f, 0.5f, 0.65f)
                xs.forEach { fx ->
                    drawLine(
                        color = dropColor,
                        start = Offset(w * fx, h * 0.72f),
                        end = Offset(w * fx - w * 0.03f, h * 0.9f),
                        strokeWidth = 4f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            Condition.TEMPORALE -> {
                drawCloud(w * 0.5f, h * 0.4f, 0.95f, CloudTop, Color(0xFFB9C2DE))
                val boltPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.54f, h * 0.55f)
                    lineTo(w * 0.42f, h * 0.78f)
                    lineTo(w * 0.5f, h * 0.78f)
                    lineTo(w * 0.4f, h * 0.98f)
                    lineTo(w * 0.6f, h * 0.7f)
                    lineTo(w * 0.5f, h * 0.7f)
                    close()
                }
                drawPath(boltPath, color = SunCore)
            }
            Condition.NEVE -> {
                drawCloud(w * 0.5f, h * 0.42f, 0.95f)
                val flakeColor = Color(0xFFBFD4F5)
                val xs = listOf(0.35f, 0.5f, 0.65f)
                xs.forEach { fx ->
                    drawCircle(flakeColor, radius = w * 0.025f, center = Offset(w * fx, h * 0.82f))
                }
            }
        }
    }
}

/** Icona compatta per le card orarie/giornaliere, stessa logica del disegno principale ma più piccola. */
@Composable
fun WeatherIconSmall(condition: Condition, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        when (condition) {
            Condition.SOLE, Condition.POCO_NUVOLOSO -> {
                drawCircle(
                    brush = Brush.radialGradient(listOf(SunGlow, SunCore, SunEdge)),
                    radius = w * 0.32f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
            else -> {
                val brush = Brush.verticalGradient(listOf(CloudTop, CloudBottom))
                val lobes = listOf(
                    Offset(w * 0.38f, h * 0.5f) to w * 0.22f,
                    Offset(w * 0.58f, h * 0.44f) to w * 0.28f
                )
                val shadowDy = h * 0.05f
                lobes.forEach { (c, r) -> drawCircle(CloudShadow, radius = r, center = Offset(c.x, c.y + shadowDy)) }
                lobes.forEach { (c, r) -> drawCircle(brush, radius = r, center = c) }
                lobes.forEach { (c, r) -> drawCircle(CloudOutline, radius = r, center = c, style = Stroke(width = w * 0.01f)) }
            }
        }
    }
}
