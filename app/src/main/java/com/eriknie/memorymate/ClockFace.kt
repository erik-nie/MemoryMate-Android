package com.eriknie.memorymate

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Analog clock face in several styles. Direct port of the iOS Clock view. */
object ClockFace {

    private sealed class Shape {
        class Tapered(val base: Float, val tip: Float) : Shape()
        class Bar(val width: Float, val round: Boolean) : Shape()
        class Leaf(val width: Float) : Shape()
    }

    private enum class Numerals { ARABIC, QUARTERS, ROMAN, NONE }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** Angle in radians for [value] out of [units] steps; 0 points up, clockwise. */
    private fun angle(value: Float, units: Float) = (value / units * 2 * Math.PI).toFloat()

    private fun px(cx: Float, a: Float, r: Float) = cx + r * sin(a)
    private fun py(cy: Float, a: Float, r: Float) = cy - r * cos(a)

    private fun line(c: Canvas, cx: Float, cy: Float, a: Float, from: Float, to: Float, width: Float, color: Int,
                     cap: Paint.Cap = Paint.Cap.BUTT) {
        paint.reset(); paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = cap
        paint.strokeWidth = width
        paint.color = color
        c.drawLine(px(cx, a, from), py(cy, a, from), px(cx, a, to), py(cy, a, to), paint)
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean, serif: Boolean = false) {
        paint.reset(); paint.isAntiAlias = true
        paint.textSize = size
        paint.color = color
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(if (serif) Typeface.SERIF else Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        val fm = paint.fontMetrics
        c.drawText(s, x, y - (fm.ascent + fm.descent) / 2, paint)
    }

    private fun applyShadow(p: Paint, r: Float) {
        val s = r * 0.025f
        p.setShadowLayer(s * 1.5f, s * 0.6f, s, 0x73000000)
    }

    private fun hand(c: Canvas, cx: Float, cy: Float, a: Float, length: Float, tail: Float, shape: Shape,
                     color: Int, r: Float) {
        val dx = sin(a); val dy = -cos(a)      // along the hand
        val sx = cos(a); val sy = sin(a)       // across the hand
        fun x(along: Float, across: Float) = cx + dx * along + sx * across
        fun y(along: Float, across: Float) = cy + dy * along + sy * across

        paint.reset(); paint.isAntiAlias = true
        paint.color = color
        applyShadow(paint, r)
        paint.strokeJoin = Paint.Join.ROUND
        when (shape) {
            is Shape.Tapered -> {
                val b = shape.base * r; val t = shape.tip * r
                val path = Path()
                path.moveTo(x(-tail, -b * 0.35f), y(-tail, -b * 0.35f))
                path.lineTo(x(0f, -b / 2), y(0f, -b / 2))
                path.lineTo(x(length, -t / 2), y(length, -t / 2))
                path.lineTo(x(length, t / 2), y(length, t / 2))
                path.lineTo(x(0f, b / 2), y(0f, b / 2))
                path.lineTo(x(-tail, b * 0.35f), y(-tail, b * 0.35f))
                path.close()
                paint.style = Paint.Style.FILL_AND_STROKE
                paint.strokeWidth = t
                c.drawPath(path, paint)
            }
            is Shape.Bar -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = shape.width * r
                paint.strokeCap = if (shape.round) Paint.Cap.ROUND else Paint.Cap.BUTT
                c.drawLine(x(-tail, 0f), y(-tail, 0f), x(length, 0f), y(length, 0f), paint)
            }
            is Shape.Leaf -> {
                val w = shape.width * r
                val path = Path()
                path.moveTo(x(-tail, 0f), y(-tail, 0f))
                path.quadTo(x(length * 0.3f, w * 1.6f), y(length * 0.3f, w * 1.6f), x(length, 0f), y(length, 0f))
                path.quadTo(x(length * 0.3f, -w * 1.6f), y(length * 0.3f, -w * 1.6f), x(-tail, 0f), y(-tail, 0f))
                paint.style = Paint.Style.FILL
                c.drawPath(path, paint)
            }
        }
    }

    private fun disc(c: Canvas, x: Float, y: Float, radius: Float, color: Int, r: Float) {
        paint.reset(); paint.isAntiAlias = true
        paint.color = color
        applyShadow(paint, r)
        c.drawCircle(x, y, radius, paint)
    }

    private fun drawFace(c: Canvas, cx: Float, cy: Float, r: Float, style: ClockStyle, t: Theme) {
        val text = t.text.toArgb(); val tick = t.tick.toArgb()
        when (style) {
            ClockStyle.CLASSIC, ClockStyle.SLIM ->
                for (i in 1..60) line(c, cx, cy, angle(i.toFloat(), 60f), 0.96f * r, r, if (i % 5 == 0) 5f else 1f, tick)
            ClockStyle.RAILWAY ->
                for (i in 1..60) {
                    val hour = i % 5 == 0
                    line(c, cx, cy, angle(i.toFloat(), 60f), (if (hour) 0.80f else 0.90f) * r, 0.97f * r,
                        if (hour) r * 0.045f else r * 0.012f, text)
                }
            ClockStyle.ROMANTIC ->
                for (i in 1..60) {
                    val hour = i % 5 == 0
                    line(c, cx, cy, angle(i.toFloat(), 60f), (if (hour) 0.93f else 0.95f) * r, 0.98f * r,
                        if (hour) 3f else 1.5f, if (hour) text else tick)
                }
            ClockStyle.MINIMAL ->
                for (i in 1..12) {
                    val a = angle(i.toFloat(), 12f)
                    val d = if (i % 3 == 0) r * 0.03f else r * 0.016f
                    paint.reset(); paint.isAntiAlias = true
                    paint.color = if (i % 3 == 0) text else tick
                    c.drawCircle(px(cx, a, 0.9f * r), py(cy, a, 0.9f * r), d, paint)
                }
            ClockStyle.MODERN ->
                for (i in 1..12) {
                    line(c, cx, cy, angle(i.toFloat(), 12f), 0.90f * r, 0.97f * r,
                        if (i % 3 == 0) r * 0.035f else r * 0.02f, if (i % 3 == 0) text else tick)
                }
        }

        val numerals = when (style) {
            ClockStyle.CLASSIC, ClockStyle.SLIM -> Numerals.ARABIC
            ClockStyle.MODERN -> Numerals.QUARTERS
            ClockStyle.ROMANTIC -> Numerals.ROMAN
            else -> Numerals.NONE
        }
        when (numerals) {
            Numerals.ARABIC -> for (i in 1..12) {
                val a = angle(i.toFloat(), 12f)
                text(c, "$i", px(cx, a, 0.8f * r), py(cy, a, 0.8f * r), 0.27f * r * 0.95f, text, bold = true)
            }
            Numerals.QUARTERS -> for (i in listOf(3, 6, 9, 12)) {
                val a = angle(i.toFloat(), 12f)
                text(c, "$i", px(cx, a, 0.72f * r), py(cy, a, 0.72f * r), 0.24f * r, text, bold = true)
            }
            Numerals.ROMAN -> {
                val romans = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")
                for (i in 1..12) {
                    val a = angle(i.toFloat(), 12f)
                    text(c, romans[i - 1], px(cx, a, 0.75f * r), py(cy, a, 0.75f * r), 0.19f * r * 1.15f, text,
                        bold = false, serif = true)
                }
            }
            Numerals.NONE -> {}
        }

        if (style == ClockStyle.CLASSIC || style == ClockStyle.SLIM) {
            text(c, "MemoryMate", cx, cy - 0.36f * r, 0.12f * r, t.secondary.toArgb(), bold = true)
        }
    }

    private fun drawHands(c: Canvas, cx: Float, cy: Float, r: Float, style: ClockStyle, t: Theme, showSeconds: Boolean) {
        val cal = Calendar.getInstance()
        val sec = cal.get(Calendar.SECOND).toFloat()
        // Without a second hand the other hands step once per minute.
        val min = cal.get(Calendar.MINUTE) + if (showSeconds) sec / 60f else 0f
        val hr = (cal.get(Calendar.HOUR_OF_DAY) % 12) + min / 60f
        val ha = angle(hr, 12f); val ma = angle(min, 60f); val sa = angle(sec, 60f)

        val hourC = t.hourHand.toArgb(); val minC = t.minuteHand.toArgb(); val secC = t.secondHand.toArgb()
        val textC = t.text.toArgb(); val accentC = t.accent.toArgb()

        fun secondHand(length: Float, tail: Float, width: Float) {
            if (showSeconds) hand(c, cx, cy, sa, length * r, tail * r, Shape.Bar(width, false), secC, r)
        }

        when (style) {
            ClockStyle.CLASSIC -> {
                hand(c, cx, cy, ha, 0.6f * r, 0.1f * r, Shape.Tapered(0.085f, 0.03f), hourC, r)
                hand(c, cx, cy, ma, 0.9f * r, 0.1f * r, Shape.Tapered(0.065f, 0.022f), minC, r)
                secondHand(0.8f, 0.25f, 0.012f)
                disc(c, cx, cy, 0.045f * r, if (showSeconds) secC else minC, r)
            }
            ClockStyle.SLIM -> {
                hand(c, cx, cy, ha, 0.58f * r, 0.08f * r, Shape.Tapered(0.04f, 0.018f), hourC, r)
                hand(c, cx, cy, ma, 0.88f * r, 0.08f * r, Shape.Tapered(0.03f, 0.012f), minC, r)
                secondHand(0.8f, 0.2f, 0.008f)
                disc(c, cx, cy, 0.03f * r, textC, r)
            }
            ClockStyle.RAILWAY -> {
                hand(c, cx, cy, ha, 0.52f * r, 0.12f * r, Shape.Bar(0.075f, false), textC, r)
                hand(c, cx, cy, ma, 0.88f * r, 0.12f * r, Shape.Bar(0.055f, false), textC, r)
                secondHand(0.62f, 0.2f, 0.014f)
                if (showSeconds) disc(c, px(cx, sa, 0.62f * r), py(cy, sa, 0.62f * r), 0.075f * r, secC, r)
                disc(c, cx, cy, 0.05f * r, textC, r)
            }
            ClockStyle.ROMANTIC -> {
                hand(c, cx, cy, ha, 0.52f * r, 0.1f * r, Shape.Leaf(0.05f), textC, r)
                hand(c, cx, cy, ma, 0.78f * r, 0.1f * r, Shape.Leaf(0.04f), accentC, r)
                secondHand(0.82f, 0.18f, 0.006f)
                disc(c, cx, cy, 0.03f * r, accentC, r)
            }
            ClockStyle.MINIMAL -> {
                hand(c, cx, cy, ha, 0.5f * r, 0f, Shape.Bar(0.03f, true), textC, r)
                hand(c, cx, cy, ma, 0.78f * r, 0f, Shape.Bar(0.022f, true), textC, r)
                secondHand(0.8f, 0f, 0.008f)
                disc(c, cx, cy, 0.03f * r, textC, r)
            }
            ClockStyle.MODERN -> {
                hand(c, cx, cy, ha, 0.5f * r, 0.1f * r, Shape.Bar(0.04f, true), textC, r)
                hand(c, cx, cy, ma, 0.82f * r, 0.1f * r, Shape.Bar(0.03f, true), accentC, r)
                secondHand(0.84f, 0.18f, 0.01f)
                disc(c, cx, cy, 0.035f * r, if (showSeconds) secC else accentC, r)
            }
        }
    }

    /** Draws the clock into the square-ish area (left, top, width, height). */
    fun draw(c: Canvas, left: Float, top: Float, width: Float, height: Float, style: ClockStyle, t: Theme, showSeconds: Boolean) {
        val cx = left + width / 2; val cy = top + height / 2
        val r = min(width, height) / 2
        paint.reset(); paint.isAntiAlias = true
        paint.color = t.block.toArgb()
        c.drawCircle(cx, cy, r, paint)
        drawFace(c, cx, cy, r, style, t)
        drawHands(c, cx, cy, r, style, t, showSeconds)
    }
}
