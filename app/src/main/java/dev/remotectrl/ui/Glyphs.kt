package dev.remotectrl.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class Glyph { POWER, KEYBOARD, BACK, HOME, PLAY_PAUSE, MUTE, PLUS, MINUS, CHEVRON_DOWN, CHECK, REFRESH }

/** Line icons drawn on a 24-unit grid, so the remote needs no icon library. */
@Composable
fun GlyphIcon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier) {
        val g = Grid(this, tint)
        when (glyph) {
            Glyph.POWER -> {
                g.arc(12f, 13f, 7.5f, -60f, 300f)
                g.line(12f, 3f, 12f, 12f)
            }
            Glyph.KEYBOARD -> {
                g.roundRect(2f, 4f, 20f, 16f, 2.5f)
                g.line(6f, 9f, 18f, 9f)
                g.line(6f, 13f, 6.1f, 13f)
                g.line(10f, 13f, 14f, 13f)
                g.line(18f, 13f, 18.1f, 13f)
                g.line(8f, 16.5f, 16f, 16.5f)
            }
            Glyph.BACK -> g.poly(15f to 4f, 7f to 12f, 15f to 20f)
            Glyph.HOME -> g.poly(3.5f to 11f, 12f to 3.5f, 20.5f to 11f, 20.5f to 20.5f, 14.5f to 20.5f, 14.5f to 14f, 9.5f to 14f, 9.5f to 20.5f, 3.5f to 20.5f, close = true)
            Glyph.PLAY_PAUSE -> {
                g.poly(3f to 5f, 13f to 12f, 3f to 19f, close = true)
                g.line(17f, 5f, 17f, 19f)
                g.line(21f, 5f, 21f, 19f)
            }
            Glyph.MUTE -> {
                g.poly(3f to 9.5f, 7.5f to 9.5f, 13f to 5f, 13f to 19f, 7.5f to 14.5f, 3f to 14.5f, close = true)
                g.line(17f, 9f, 22f, 15f)
                g.line(22f, 9f, 17f, 15f)
            }
            Glyph.PLUS -> {
                g.line(12f, 4f, 12f, 20f)
                g.line(4f, 12f, 20f, 12f)
            }
            Glyph.MINUS -> g.line(4f, 12f, 20f, 12f)
            Glyph.CHEVRON_DOWN -> g.poly(5f to 9f, 12f to 16f, 19f to 9f)
            Glyph.CHECK -> g.poly(4f to 12.5f, 9.5f to 18f, 20f to 6.5f)
            Glyph.REFRESH -> {
                g.arc(12f, 12f, 7.5f, 0f, 300f)
                g.poly(12.4f to 7.3f, 17.5f to 6.5f, 15.6f to 1.7f)
            }
        }
    }
}

/** Helpers that take coordinates in the 24-unit grid and scale them to the canvas. */
private class Grid(private val scope: DrawScope, private val tint: Color) {
    private val s = scope.size.minDimension / 24f
    private val stroke = Stroke(width = 1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

    private fun p(x: Float, y: Float) = Offset(x * s, y * s)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        scope.drawLine(tint, p(x1, y1), p(x2, y2), strokeWidth = 1.8f * s, cap = StrokeCap.Round)

    fun poly(vararg pts: Pair<Float, Float>, close: Boolean = false) {
        val path = Path().apply {
            moveTo(pts[0].first * s, pts[0].second * s)
            for (i in 1 until pts.size) lineTo(pts[i].first * s, pts[i].second * s)
            if (close) close()
        }
        scope.drawPath(path, tint, style = stroke)
    }

    fun arc(cx: Float, cy: Float, r: Float, start: Float, sweep: Float) =
        scope.drawArc(tint, start, sweep, false, p(cx - r, cy - r), Size(2 * r * s, 2 * r * s), style = stroke)

    fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        scope.drawRoundRect(tint, p(x, y), Size(w * s, h * s), CornerRadius(r * s), style = stroke)
}
