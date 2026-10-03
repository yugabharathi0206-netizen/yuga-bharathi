package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Orb3DView(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    isListening: Boolean = false,
    primaryColor: Color = Color(0xFF00E676), // Homeopathic vibrant emerald
    secondaryColor: Color = Color(0xFF00B0FF), // Healing Cyan
    coreColor: Color = Color(0xFF1DE9B6)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 4000 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (isListening) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 800 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (isListening) 0.85f else 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 700 else 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    dragOffsetX = (dragOffsetX + dragAmount.x * 0.4f).coerceIn(-40f, 40f)
                    dragOffsetY = (dragOffsetY + dragAmount.y * 0.4f).coerceIn(-40f, 40f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f + dragOffsetX, this.size.height / 2f + dragOffsetY)
            val baseRadius = (this.size.minDimension / 2f) * 0.72f * pulseScale

            // 1. Ambient outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = glowAlpha * 0.6f),
                        secondaryColor.copy(alpha = glowAlpha * 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.55f
                ),
                radius = baseRadius * 1.55f,
                center = center
            )

            // 2. 3D Spherical Core with Radial Lighting & Depth
            val lightSource = Offset(center.x - baseRadius * 0.35f, center.y - baseRadius * 0.35f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        coreColor.copy(alpha = 0.9f),
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.7f),
                        Color(0xFF002244).copy(alpha = 0.95f)
                    ),
                    center = lightSource,
                    radius = baseRadius * 1.1f
                ),
                radius = baseRadius,
                center = center
            )

            // 3. 3D Rotating Orbital Energy Rings
            val radAngle1 = Math.toRadians((rotationAngle).toDouble())
            val radAngle2 = Math.toRadians((rotationAngle + 90.0).toDouble())

            val ringRadiusX = baseRadius * 1.25f
            val ringRadiusY = baseRadius * 0.42f

            // Ring 1 (Equatorial tilt)
            drawOval(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.1f),
                        primaryColor.copy(alpha = 0.9f),
                        secondaryColor.copy(alpha = 0.9f),
                        primaryColor.copy(alpha = 0.1f)
                    ),
                    center = center
                ),
                topLeft = Offset(center.x - ringRadiusX, center.y - ringRadiusY),
                size = androidx.compose.ui.geometry.Size(ringRadiusX * 2, ringRadiusY * 2),
                style = Stroke(width = 3.5f)
            )

            // Orbiting particle 1
            val px1 = (center.x + ringRadiusX * cos(radAngle1)).toFloat()
            val py1 = (center.y + ringRadiusY * sin(radAngle1)).toFloat()
            drawCircle(
                color = Color.White,
                radius = 5.5f,
                center = Offset(px1, py1)
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.6f),
                radius = 11f,
                center = Offset(px1, py1)
            )

            // Orbiting particle 2
            val px2 = (center.x - ringRadiusX * cos(radAngle2)).toFloat()
            val py2 = (center.y - ringRadiusY * sin(radAngle2)).toFloat()
            drawCircle(
                color = secondaryColor,
                radius = 4.5f,
                center = Offset(px2, py2)
            )

            // 4. Specular 3D Glass Highlights
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - baseRadius * 0.32f, center.y - baseRadius * 0.36f),
                    radius = baseRadius * 0.45f
                ),
                topLeft = Offset(center.x - baseRadius * 0.55f, center.y - baseRadius * 0.60f),
                size = androidx.compose.ui.geometry.Size(baseRadius * 0.7f, baseRadius * 0.42f)
            )
        }
    }
}
