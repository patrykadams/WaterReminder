package com.patrykadamski.waterreminder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiPiece(
    val xFraction: Float,
    val startDelayFraction: Float,
    val swayAmplitudePx: Float,
    val swayFrequency: Float,
    val rotationSpeedDeg: Float,
    val sizePx: Float,
    val color: Color
)

private val CONFETTI_COLORS = listOf(
    Color(0xFFEF5350), Color(0xFFFFCA28), Color(0xFF66BB6A),
    Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFFF7043)
)

private const val PIECE_COUNT = 40
private const val DURATION_MS = 3500

/**
 * Brief, self-contained falling-confetti effect for when the daily goal is
 * reached. No external animation library - just a Canvas driven by a single
 * Animatable progress value. Meant to be composed conditionally (only while
 * WaterViewModel.showConfetti is true), which naturally restarts the
 * animation fresh each time it re-enters composition.
 */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val pieces = remember {
        List(PIECE_COUNT) {
            ConfettiPiece(
                xFraction = Random.nextFloat(),
                startDelayFraction = Random.nextFloat() * 0.3f,
                swayAmplitudePx = 20f + Random.nextFloat() * 30f,
                swayFrequency = 2f + Random.nextFloat() * 3f,
                rotationSpeedDeg = 180f + Random.nextFloat() * 360f,
                sizePx = 14f + Random.nextFloat() * 14f,
                color = CONFETTI_COLORS[Random.nextInt(CONFETTI_COLORS.size)]
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = DURATION_MS, easing = LinearEasing))
    }

    Canvas(modifier = modifier) {
        val h = size.height
        val w = size.width

        pieces.forEach { piece ->
            val pieceProgress = ((progress.value - piece.startDelayFraction) / (1f - piece.startDelayFraction))
                .coerceIn(0f, 1f)
            if (pieceProgress <= 0f) return@forEach

            val y = pieceProgress * (h + piece.sizePx * 2) - piece.sizePx
            val sway = sin(pieceProgress * piece.swayFrequency * 2 * Math.PI).toFloat() * piece.swayAmplitudePx
            val x = (piece.xFraction * w + sway).coerceIn(0f, w)
            val rotation = pieceProgress * piece.rotationSpeedDeg

            rotate(degrees = rotation, pivot = Offset(x, y)) {
                drawRect(
                    color = piece.color,
                    topLeft = Offset(x - piece.sizePx / 2, y - piece.sizePx / 2),
                    size = Size(piece.sizePx, piece.sizePx)
                )
            }
        }
    }
}
