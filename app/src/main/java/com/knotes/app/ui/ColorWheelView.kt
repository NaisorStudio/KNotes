package com.knotes.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/* ============================================================
   Rueda de colores estilo disco (como la de la captura):
   12 sectores × 4 anillos — el BORDE es intenso y hacia el
   CENTRO se aclara (pastel). Toca una porción → pulso
   animado + retorna el color al instante.
   ============================================================ */
class ColorWheelView @JvmOverloads constructor(
    context: Context, attrs: android.util.AttributeSet? = null
) : View(context, attrs) {

    var onPick: ((Int) -> Unit)? = null

    private val HUES = 12
    private val RINGS = 4

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var pulseT = 0f         // 0..1 animación al elegir
    private var selIdx = -1
    private var selRing = -1

    private val wedge = Array(RINGS) { Array(HUES) { Path() } }
    private val colorCache = Array(RINGS) { arrayOfNulls<Int>(HUES) }

    fun colorAt(ring: Int, hue: Int): Int {
        colorCache[ring][hue]?.let { return it }
        val base = (hue * 30f - 15f + 360f) % 360f
        val s = when (ring) { 3 -> 0.95f; 2 -> 0.72f; 1 -> 0.48f; else -> 0.30f }
        val v = when (ring) { 3 -> 1.00f; 2 -> 0.95f; 1 -> 0.92f; else -> 0.90f }
        val c = Color.HSVToColor(floatArrayOf(base, s, v))
        colorCache[ring][hue] = c
        return c
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        val r = minOf(w, h) / 2f - 6
        if (r <= 0) return
        val cx = w / 2f; val cy = h / 2f
        // RING 3 = exterior (intenso) · RING 0 = interior (pastel)
        val outer = Array(RINGS) { i -> r * (1f - i * 0.235f) }   // 3 -> r casi completo
        val inner = Array(RINGS) { i -> r * (1f - (i + 1) * 0.235f) }
        for (ri in 0 until RINGS) {
            for (hue in 0 until HUES) {
                val a0 = (hue * 30f - 15f - 90f).toFloat()
                val wp = Path()
                wp.arcTo(cx - outer[ri], cy - outer[ri], cx + outer[ri], cy + outer[ri], a0, 30f, true)
                if (inner[ri] > 0)
                    wp.arcTo(cx - inner[ri], cy - inner[ri], cx + inner[ri], cy + inner[ri], a0 + 30f, -30f, true)
                else wp.close()
                wedge[ri][hue] = wp
            }
        }
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f; val cy = height / 2f
        val r = minOf(width, height) / 2f - 6
        if (r <= 0) return

        // 1) tiles
        p.style = Paint.Style.FILL
        for (ri in 0 until RINGS) for (hue in 0 until HUES) {
            p.color = colorAt(ri, hue)
            c.drawPath(wedge[ri][hue], p)
        }
        // 2) separadores blancos (solo hasta el anillo interior)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3.5f
        p.color = Color.WHITE
        var ang = 0.0
        while (ang < 6.2831) {
            c.drawLine(cx, cy,
                cx + r * cos(ang).toFloat(), cy + r * sin(ang).toFloat(), p)
            ang += Math.PI / 6.0
        }
        // círculos blancos entre anillos
        for (i in 1..3) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3.5f
            c.drawCircle(cx, cy, r * (1f - i * 0.235f), p)
        }
        // 3) tile elegido: pulso blanco
        if (selRing >= 0) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 5f + 4f * pulseT
            p.color = 0xB3FFFFFF.toInt()
            c.drawPath(wedge[selRing][selIdx], p)
            p.style = Paint.Style.FILL
            p.color = 0x2EFFFFFF.toInt()
            c.save()
            c.scale(1f + 0.10f * pulseT, 1f + 0.10f * pulseT, cx, cy)
            c.drawPath(wedge[selRing][selIdx], p)
            c.restore()
        }
        // 4) centro con el color ELEGIDO vivo
        val cr = r * 0.06f + 10f
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        c.drawCircle(cx, cy, cr, p)
        p.color = 0xFF3A3540.toInt()
        c.drawCircle(cx, cy, cr - 7f, p)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked != MotionEvent.ACTION_DOWN) return performClick()
        val cx = width / 2f; val cy = height / 2f
        val dx = ev.x - cx; val dy = ev.y - cy
        val rad = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
        val rMax = minOf(width, height) / 2f - 6
        if (rad > rMax) return performClick()
        val rr = rad / rMax
        val ring = when { rr > 0.765f -> 3; rr > 0.53f -> 2; rr > 0.295f -> 1; else -> 0 }
        var deg = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f + 15f
        deg = (deg + 360f) % 360f
        val hue = (deg / 30f).toInt().coerceIn(0, HUES - 1)
        selRing = ring; selIdx = hue
        pulseT = 0f
        android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 280
            addUpdateListener { an -> pulseT = an.animatedValue as Float; invalidate() }
            start()
        }
        onPick?.invoke(colorAt(ring, hue))
        return performClick()
    }

    override fun performClick(): Boolean = super.performClick()
}
