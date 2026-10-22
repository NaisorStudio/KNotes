package com.knotes.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import kotlin.math.roundToInt

/* ============================================================
   KNotes — portadas procedurales
   Cada cuaderno tiene un estilo + semilla: el arte se dibuja
   con gradientes, blobs orgánicos y anillados. Nada impacta.
   ============================================================ */

class NotebookCoverView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var style: Int = 0
    var seed: Long = 1

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        drawCover(c, width.toFloat(), height.toFloat(), style, seed)
    }

    companion object {

        /* --- utilidades --- */
        class MyRnd(private var s: Long) {
            fun next(): Float {
                s = s * 6364136223846793005L + 1442695040888963407L
                return ((s ushr 33).toDouble() / Int.MAX_VALUE).toFloat().mod2()
            }
            private fun Float.mod2(): Float = this % 1f + { if (this < 0) 1f else 0f }()
        }

        private fun paint(fill: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fill; style = Paint.Style.FILL
        }

        private fun blobPath(rnd: MyRnd, cx: Float, cy: Float, radius: Float): Path {
            val p = Path()
            val lobes = 5 + rnd.next() * 4f
            val seq = ArrayList<Pair<Float, Float>>()
            val r0 = radius * (0.75f + rnd.next() * 0.5f)
            val n = lobes.roundToInt()
            val pts = ArrayList<Canvas2.Pt2>()
            for (i in 0 until n) {
                val ang = i * (Math.PI * 2 / n).toFloat()
                val rr = r0 * (0.7f + rnd.next() * 0.55f)
                pts.add(Canvas2.Pt2(cx + Math.cos(ang.toDouble()).toFloat() * rr,
                    cy + Math.sin(ang.toDouble()).toFloat() * rr))
            }
            return Canvas2.smoothClosed(pts)
        }

        /* --- paleta por estilo --- */
        private data class StyleSpec(
            val baseA: Int, val baseB: Int,      // gradiente de fondo
            val blob: Int, val blob2: Int,       // blobs decorativos
            val binding: Int, val accent: Int,   // anillado / banda
            val kind: Int                        // 0=carrillo,1=anillado espiral,2=espiral+pocket,3=banda
        )

        private val SPECS = arrayOf(
            StyleSpec(0xFF2F2A34.toInt(), 0xFF241F2B.toInt(), 0xFFF3B7CB.toInt(), 0xFFE889B0.toInt(), 0xFF3A323E.toInt(), 0xFFF1A2C0.toInt(), 3), // oscuro con sello rosa
            StyleSpec(0xFF8FE0DC.toInt(), 0xFF6FCBC8.toInt(), 0xFFDFF4F1.toInt(), 0xFFB7E7E2.toInt(), 0xFF6FBCB8.toInt(), 0xFFE8F7F5.toInt(), 3), // menta con correa
            StyleSpec(0xFFF2E8D5.toInt(), 0xFFE7DDBE.toInt(), 0xFF20242C.toInt(), 0xFFC8A46B.toInt(), 0xFF8A5A3A.toInt(), 0xFF20242C.toInt(), 1), // arte abstracto con espiral
            StyleSpec(0xFF67C4EC.toInt(), 0xFF52B4E4.toInt(), 0xFF4FA8DC.toInt(), 0xFF7FD0EF.toInt(), 0xFF3E90C4.toInt(), 0xFF42AEE0.toInt(), 2), // azul anillado
            StyleSpec(0xFFDCC8E8.toInt(), 0xFFCEB8DC.toInt(), 0xFFF0CDD5.toInt(), 0xFFC8B4E8.toInt(), 0xFFB49CC8.toInt(), 0xFFE8E0D8.toInt(), 0),  // óleo pastel
            StyleSpec(0xFFF3A793.toInt(), 0xFFE98F76.toInt(), 0xFFB04A3C.toInt(), 0xFFF6D8A0.toInt(), 0xFF7E3830.toInt(), 0xFFF2B9A6.toInt(), 2),  // coral
            StyleSpec(0xFFB9DCC8.toInt(), 0xFFA3D0B8.toInt(), 0xFF67967E.toInt(), 0xFFEFF3EA.toInt(), 0xFF517A64.toInt(), 0xFFDCEFE2.toInt(), 1),  // verde salvia
            StyleSpec(0xFFE8D4C8.toInt(), 0xFFDCC0B4.toInt(), 0xFFC88A70.toInt(), 0xFFEFD8CC.toInt(), 0xFF9E6a5A.toInt(), 0xFFF0E0D6.toInt(), 0),  // terracota
            StyleSpec(0xFF2B3148.toInt(), 0xFF232838.toInt(), 0xFFD8DCEF.toInt(), 0xFF8D96C8.toInt(), 0xFF3A4058.toInt(), 0xFFE8EAF8.toInt(), 3),  // noche
            StyleSpec(0xFFF8E8C8.toInt(), 0xFFF0D8A8.toInt(), 0xFFE8B84A.toInt(), 0xFFF8F0D8.toInt(), 0xFFC8963A.toInt(), 0xFFF4CE68.toInt(), 2),  // mostaza
            StyleSpec(0xFFD8E2F0.toInt(), 0xFFC8D4EC.toInt(), 0xFF8DA2E8.toInt(), 0xFFEFF2FA.toInt(), 0xFF6A7EC4.toInt(), 0xFFE0E8F8.toInt(), 114), // lila
            StyleSpec(0xFF2E2E2E.toInt(), 0xFF232323.toInt(), 0xFFE8E8E8.toInt(), 0xFF444.toInt(), 0xFF383838.toInt(), 0xFFE8E8E8.toInt(), 1)       // minimal
        )

        fun drawCover(canvas: Canvas, w: Float, h: Float, style: Int, seedLong: Long) {
            // En modo HOMBRE, los 3 estilos más rosados se cambian por equivalentes azules/oscuros
            val remap = if (Gender.isMan) intArrayOf(8, 1, 2, 3, 9, 5, 6, 7, 8, 9, 3, 11) else null
            val spec = SPECS[remap?.get(style % SPECS.size) ?: (style % SPECS.size)]
            val rnd = MyRnd(seedLong * 31 + style)
            val wpaint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Fondo
            val g = LinearGradient(0f, 0f, w, h, spec.baseA, spec.baseB, Shader.TileMode.CLAMP)
            wpaint.shader = g
            canvas.drawRoundRect(0f, 0f, w, h, w * 0.07f, w * 0.07f, wpaint)
            wpaint.shader = null

            // Lomo de sombra
            wpaint.color = Color.argb(40, 0, 0, 0)
            canvas.drawRect(w - w * 0.035f, 0f, w, h, wpaint)

            // Blobs orgánicos estilo acuarela
            when (spec.kind % 3) {
                0 -> {
                    wpaint.shader = RadialGradient(w * 0.32f, h * 0.34f, w * 0.55f, spec.blob2, Color.TRANSPARENT, Shader.TileMode.CLAMP)
                    canvas.drawCircle(w * 0.3f, h * 0.3f, w * 0.42f, wpaint)
                    wpaint.shader = null
                    wpaint.color = Color.argb(95, (spec.blob shr 16) and 0xFF, (spec.blob shr 8) and 0xFF, spec.blob and 0xFF)
                    canvas.drawPath(blobPath(rnd, w * 0.68f, h * 0.3f, w * 0.3f), wpaint)
                    wpaint.color = Color.argb(90, (spec.blob2 shr 16) and 0xFF, (spec.blob2 shr 8) and 0xFF, spec.blob2 and 0xFF)
                    canvas.drawPath(blobPath(rnd, w * 0.45f, h * 0.78f, w * 0.34f), wpaint)
                    canvas.drawPath(blobPath(rnd, w * 0.8f, h * 0.72f, w * 0.2f), wpaint)
                }
                1 -> {
                    wpaint.color = Color.argb(120, (spec.blob shr 16) and 0xFF, (spec.blob shr 8) and 0xFF, spec.blob and 0xFF)
                    canvas.drawPath(blobPath(rnd, w * 0.62f, h * 0.42f, w * 0.46f), wpaint)
                    wpaint.color = Color.argb(140, (spec.blob2 shr 16) and 0xFF, (spec.blob2 shr 8) and 0xFF, spec.blob2 and 0xFF)
                    canvas.drawPath(blobPath(rnd, w * 0.7f, h * 0.8f, w * 0.3f), wpaint)
                    // líneas doradas finas
                    wpaint.color = spec.accent
                    wpaint.style = Paint.Style.STROKE
                    wpaint.strokeWidth = w * 0.012f
                    for (i in 0..2) {
                        val p = Canvas2.wavyLine(rnd, w * (0.2f + i * 0.13f), w * 0.95f, h * (0.5f + i * 0.13f), w * 0.07f)
                        wpaint.style = Paint.Style.STROKE
                        canvas.drawPath(p, wpaint)
                    }
                    wpaint.style = Paint.Style.FILL
                }
                else -> {
                    wpaint.color = Color.argb(178, (spec.blob shr 16) and 0xFF, (spec.blob shr 8) and 0xFF, spec.blob and 0xFF)
                    canvas.drawPath(blobPath(rnd, w * 0.66f, h * 0.36f, w * 0.34f), wpaint)
                }
            }

            // Anillado / espiral / banda lateral
            drawBinding(canvas, w, h, spec, rnd)
        }

        private fun drawBinding(canvas: Canvas, w: Float, h: Float, spec: StyleSpec, rnd: MyRnd) {
            val p = paint(spec.binding)
            when (spec.kind) {
                1 -> { // espiral vertical (izquierda)
                    val n = 11
                    val seg = h / (n + 0.4f)
                    val x = w * 0.1f
                    loop@ for (i in 0 until n) {
                        val y = seg * (i + 0.6f)
                        p.color = Color.argb(230, 250, 245, 235)
                        canvas.drawRoundRect(x - w * 0.022f, y, x + w * 0.022f, y + seg * 0.66f, w * 0.02f, w * 0.02f, p)
                    }
                }
                2 -> { // anillado abierto (arriba y abajo como libreta)
                    val n = 6
                    val seg = h / (n + 1f)
                    for (i in 0 until n) {
                        val y = seg * (i + 0.8f)
                        p.color = Color.argb(228, 252, 250, 245)
                        canvas.drawRoundRect(w * 0.05f, y, w * 0.13f, y + seg * 0.6f, w * 0.02f, w * 0.02f, p)
                    }
                    // broche elástico derecho
                    p.color = Color.argb(200, (spec.accent shr 16) and 0xFF, (spec.accent shr 8) and 0xFF, spec.accent and 0xFF)
                    canvas.drawRoundRect(w * 0.9f, h * 0.04f, w * 0.94f, h * 0.96f, w * 0.02f, w * 0.02f, p)
                }
                3 -> { // banda horizontal tipo cuaderno
                    p.color = Color.argb(255, (spec.accent shr 16) and 0xFF, (spec.accent shr 8) and 0xFF, spec.accent and 0xFF)
                    canvas.drawRoundRect(w * 0.04f, h * 0.06f, w * 0.065f, h * 0.94f, w * 0.012f, w * 0.012f, p)
                    // broche página
                    p.color = Color.argb(240, 255, 255, 255)
                    canvas.drawCircle(w * 0.5f, h * 0.5f, w * 0.1f, p)
                    p.color = Color.argb(70, 0, 0, 0)
                    val rp = paint(Color.argb(60, 0, 0, 0))
                    canvas.drawCircle(w * 0.5f, h * 0.5f, w * 0.1f, rp)
                    // correa/clip
                    p.color = Color.argb(235, (spec.accent shr 16) and 0xFF, (spec.accent shr 8) and 0xFF, spec.accent and 0xFF)
                    canvas.drawRoundRect(w * 0.475f, h * 0.42f, w * 0.525f, h * 0.58f, w * 0.02f, w * 0.02f, p)
                }
                else -> { // bolsillo suave superior
                    p.color = Color.argb(60, 255, 255, 255)
                    canvas.drawRoundRect(w * 0.08f, h * 0.05f, w * 0.92f, h * 0.35f, w * 0.05f, w * 0.05f, p)
                    // clip metálico
                    p.color = Color.argb(150, 255, 255, 255)
                    canvas.drawRoundRect(w * 0.42f, h * 0.2f, w * 0.58f, h * 0.3f, w * 0.02f, w * 0.02f, p)
                }
            }
            paintPass(canvas, spec, rnd)
        }

        private fun paintPass(canvas: Canvas, spec: StyleSpec, rnd: MyRnd) {}

        /** Helpers compartidos con el editor */
        object Canvas2 {
            class Pt2(val x: Float, val y: Float)

            fun smoothClosed(pts: List<Pt2>): Path {
                val p = Path()
                if (pts.isEmpty()) return p
                val n = pts.size
                val m = Matrix()
                p.moveTo((pts[0].x + pts[n - 1].x) / 2f, (pts[0].y + pts[n - 1].y) / 2f)
                for (i in 0 until n) {
                    val a = pts[i]
                    val b = pts[(i + 1) % n]
                    p.quadTo(a.x, a.y, (a.x + b.x) / 2f, (a.y + b.y) / 2f)
                }
                p.close()
                return p
            }

            fun wavyLine(rnd: MyRnd, x1: Float, x2: Float, yMid: Float, amp: Float): Path {
                val p = Path()
                p.moveTo(x1, yMid)
                val steps = 8
                var x = x1
                for (i in 0 until steps) {
                    val nx = x + (x2 - x1) / steps
                    p.quadTo(x + (nx - x) / 2f, yMid + amp * (rnd.next() - 0.5f) * 2f, nx, yMid + amp * (rnd.next() - 0.5f) * 2f)
                    x = nx
                }
                return p
            }
        }
    }
}
