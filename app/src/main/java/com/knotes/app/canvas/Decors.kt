package com.knotes.app.canvas

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

/* ============================================================
   Adornos PROCEDURALES en las esquinas del papel
   Cada plantilla puede llevar un estilo: florecita, frutita,
   corazones, luna, arcoíris, café, pastelito, girasoles,
   hojitas, mariposas, estrellitas, huellitas u osito.
   100 % Canvas vectorial (nítido a cualquier zoom), original.
   ============================================================ */

object Decors {

    const val NONE = 0

    fun name(id: Int): String = when (id) {
        1 -> "🌸 Flores románticas"; 2 -> "🍓 Fresitas"; 3 -> "💕 Corazones"
        4 -> "🌙 Luna y estrellas"; 5 -> "🌈 Arcoíris"; 6 -> "☕ Cafecito"
        7 -> "🧁 Pastelito"; 8 -> "🌻 Girasoles"; 9 -> "🍃 Hojitas"
        10 -> "🦋 Mariposas"; 11 -> "⭐ Estrellitas"; 12 -> "🐾 Huellitas"
        13 -> "🧸 Osito"; else -> "Ninguna"
    }

    fun shortEmoji(id: Int): String = when (id) {
        1 -> "🌸"; 2 -> "🍓"; 3 -> "💕"; 4 -> "🌙"; 5 -> "🌈"; 6 -> "☕"
        7 -> "🧁"; 8 -> "🌻"; 9 -> "🍃"; 10 -> "🦋"; 11 -> "⭐"; 12 -> "🐾"
        13 -> "🧸"; else -> ""
    }

    /** Dibuja el adorno (esquina superior derecha + inferior izquierda). */
    fun draw(c: Canvas, style: Int, W: Float, H: Float) {
        when (style) {
            1 -> romance(c, W, H)
            2 -> berries(c, W, H)
            3 -> hearts(c, W, H)
            4 -> moon(c, W, H)
            5 -> rainbow(c, W, H)
            6 -> coffee(c, W, H)
            7 -> cupcake(c, W, H)
            8 -> sunflowers(c, W, H)
            9 -> leaves(c, W, H)
            10 -> butterflies(c, W, H)
            11 -> stars(c, W, H)
            12 -> paws(c, W, H)
            13 -> teddy(c, W, H)
        }
    }

    /* ---------- pinturas compartidas ---------- */

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }

    private inline fun at(c: Canvas, x: Float, y: Float, deg: Float, block: () -> Unit) {
        c.save(); c.translate(x, y); c.rotate(deg); block(); c.restore()
    }

    /* ---------- motivos ---------- */

    private fun flower(c: Canvas, petal: Int, center: Int, r: Float, petals: Int = 6) {
        fill.color = petal
        val step = (Math.PI * 2 / petals).toFloat()
        var i = 0
        while (i < petals) {
            val a = step * i
            c.drawCircle(cos(a.toDouble()).toFloat() * r, sin(a.toDouble()).toFloat() * r, r * 0.62f, fill)
            i++
        }
        fill.color = center
        c.drawCircle(0f, 0f, r * 0.52f, fill)
        fill.color = Color.argb(190, 255, 255, 255)
        c.drawCircle(-r * 0.12f, -r * 0.14f, r * 0.14f, fill)
    }

    private fun leaf(c: Canvas, color: Int, s: Float, x: Float, y: Float, deg: Float) {
        at(c, x, y, deg) {
            val p = Path()
            p.moveTo(0f, 0f)
            p.quadTo(s * 0.7f, -s * 0.55f, s, 0f)
            p.quadTo(s * 0.7f, s * 0.55f, 0f, 0f)
            fill.color = color
            c.drawPath(p, fill)
            line.color = Color.argb(140, 255, 255, 255)
            line.strokeWidth = 2.6f
            c.drawLine(s * 0.12f, 0f, s * 0.85f, 0f, line)
        }
    }

    private fun heart(c: Canvas, color: Int, s: Float, x: Float, y: Float, deg: Float) {
        at(c, x, y, deg) {
            val p = Path()
            p.moveTo(0f, s * 0.62f)
            p.cubicTo(-s * 0.9f, -s * 0.1f, -s * 0.45f, -s * 0.85f, 0f, -s * 0.28f)
            p.cubicTo(s * 0.45f, -s * 0.85f, s * 0.9f, -s * 0.1f, 0f, s * 0.62f)
            fill.color = color
            c.drawPath(p, fill)
        }
    }

    private fun star(c: Canvas, color: Int, s: Float, x: Float, y: Float, deg: Float) {
        at(c, x, y, deg) {
            val p = Path()
            var i = 0
            while (i < 10) {
                val rr = if (i % 2 == 0) s else s * 0.42f
                val a = Math.PI * (i / 10.0) - Math.PI / 2
                val px = (cos(a) * rr * 100).toInt() / 100f
                val py = (sin(a) * rr * 100).toInt() / 100f
                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                i++
            }
            p.close()
            fill.color = color
            c.drawPath(p, fill)
        }
    }

    private fun strawberry(c: Canvas, s: Float, x: Float, y: Float, deg: Float) {
        at(c, x, y, deg) {
            val p = Path()
            p.moveTo(0f, -s * 0.55f)
            p.cubicTo(s * 0.85f, -s * 0.62f, s * 0.62f, s * 0.6f, 0f, s * 0.85f)
            p.cubicTo(-s * 0.62f, s * 0.6f, -s * 0.85f, -s * 0.62f, 0f, -s * 0.55f)
            fill.color = 0xFFE8494E.toInt()
            c.drawPath(p, fill)
            fill.color = 0xFFFFE8A3.toInt()
            c.drawCircle(-s * 0.28f, -s * 0.05f, s * 0.055f, fill)
            c.drawCircle(s * 0.22f, 0f, s * 0.055f, fill)
            c.drawCircle(-s * 0.1f, s * 0.32f, s * 0.055f, fill)
            c.drawCircle(s * 0.3f, s * 0.33f, s * 0.05f, fill)
            fill.color = 0xFF69B566.toInt()
            c.drawCircle(-s * 0.22f, -s * 0.58f, s * 0.2f, fill)
            c.drawCircle(s * 0.22f, -s * 0.58f, s * 0.2f, fill)
            c.drawCircle(0f, -s * 0.66f, s * 0.18f, fill)
        }
    }

    /* ---------- estilos ---------- */

    private fun romance(c: Canvas, W: Float, H: Float) {
        at(c, W - 165f, 165f, -12f) { flower(c, 0xFFF27FB2.toInt(), 0xFFF2CE5E.toInt(), 62f) }
        leaf(c, 0xFF7BBE71.toInt(), 40f, W - 262f, 205f, 130f)
        leaf(c, 0xFF7BBE71.toInt(), 34f, W - 70f, 250f, 60f)
        at(c, W - 96f, 275f, 18f) { flower(c, 0xFFFBB1CC.toInt(), 0xFFEE9BB5.toInt(), 32f) }
        at(c, 135f, H - 165f, 14f) { flower(c, 0xFFE9619A.toInt(), 0xFFF2CE5E.toInt(), 46f) }
        leaf(c, 0xFF7BBE71.toInt(), 30f, 216f, H - 128f, 215f)
        fill.color = 0x55F27FB2.toInt()
        c.drawCircle(W - 236f, 100f, 9f, fill)
        c.drawCircle(W - 56f, 350f, 7f, fill)
        c.drawCircle(232f, H - 232f, 8f, fill)
    }

    private fun berries(c: Canvas, W: Float, H: Float) {
        strawberry(c, 88f, W - 160f, 165f, -10f)
        strawberry(c, 54f, W - 256f, 236f, 22f)
        strawberry(c, 62f, 140f, H - 170f, 12f)
        strawberry(c, 40f, 234f, H - 236f, -18f)
        leaf(c, 0xFF69B566.toInt(), 46f, W - 74f, 92f, 40f)
        fill.color = 0x44E8494E.toInt()
        c.drawCircle(W - 288f, 140f, 8f, fill)
        c.drawCircle(252f, H - 118f, 7f, fill)
    }

    private fun hearts(c: Canvas, W: Float, H: Float) {
        heart(c, 0xFFEE8CAC.toInt(), 96f, W - 160f, 160f, -12f)
        heart(c, 0xFFF7C6D8.toInt(), 56f, W - 262f, 232f, 16f)
        heart(c, 0xFFE1648F.toInt(), 40f, W - 84f, 300f, -22f)
        heart(c, 0xFFEE8CAC.toInt(), 66f, 140f, H - 165f, 10f)
        heart(c, 0xFFF7C6D8.toInt(), 44f, 240f, H - 238f, -16f)
        heart(c, 0xFFE1648F.toInt(), 34f, 80f, H - 262f, 20f)
    }

    private fun moon(c: Canvas, W: Float, H: Float) {
        val crescent = { r: Float ->
            val p = Path()
            p.addCircle(0f, 0f, r, Path.Direction.CW)
            p.addCircle(r * 0.45f, -r * 0.32f, r * 0.82f, Path.Direction.CW)
            p.fillType = Path.FillType.EVEN_ODD
            p
        }
        at(c, W - 180f, 190f, -15f) {
            fill.color = 0xFFF2CE5E.toInt()
            c.drawPath(crescent(74f), fill)
        }
        star(c, 0xFFFFD980.toInt(), 26f, W - 88f, 108f, 8f)
        star(c, 0xFFFFD980.toInt(), 18f, W - 280f, 262f, -10f)
        star(c, 0xFFFFD980.toInt(), 20f, W - 236f, 88f, 30f)
        star(c, 0xFFFFD980.toInt(), 14f, W - 130f, 340f, 0f)
        at(c, 150f, H - 180f, 12f) {
            fill.color = 0xFFF2CE5E.toInt()
            c.drawPath(crescent(46f), fill)
        }
        star(c, 0xFFFFD980.toInt(), 18f, 232f, H - 252f, 0f)
        star(c, 0xFFFFD980.toInt(), 14f, 88f, H - 282f, 12f)
    }

    private fun rainbow(c: Canvas, W: Float, H: Float) {
        val cols = intArrayOf(0xFFEF7370.toInt(), 0xFFF5B25E.toInt(), 0xFFF2CE5E.toInt(),
            0xFF8CC98F.toInt(), 0xFF7FB3E8.toInt(), 0xFFB58ECC.toInt())
        at(c, W - 175f, 285f, -10f) {
            var i = cols.size - 1
            while (i >= 0) {
                line.color = cols[i]
                line.strokeWidth = 13f
                val r = 42f + i * 15f
                c.drawArc(RectF(-r, -r, r, r), 185f, 170f, false, line)
                i--
            }
        }
        fill.color = 0xFFFFFFFF.toInt()
        c.drawCircle(W - 248f, 284f, 20f, fill)
        c.drawCircle(W - 215f, 290f, 24f, fill)
        c.drawCircle(W - 105f, 290f, 20f, fill)
        at(c, 145f, H - 300f, 8f) {
            var i = cols.size - 1
            while (i >= 0) {
                line.color = cols[i]
                line.strokeWidth = 9f
                val r = 26f + i * 9f
                c.drawArc(RectF(-r, -r, r, r), 185f, 170f, false, line)
                i--
            }
        }
    }

    private fun coffee(c: Canvas, W: Float, H: Float) {
        at(c, W - 175f, 210f, -10f) {
            line.color = 0x66868B8E.toInt(); line.strokeWidth = 5f
            c.drawArc(RectF(-30f, -150f, -6f, -104f), 110f, 140f, false, line)
            c.drawArc(RectF(8f, -156f, 32f, -110f), 110f, 140f, false, line)
            fill.color = 0xFF8A5A3A.toInt()
            c.drawCircle(-8f, -46f, 62f, fill)
            fill.color = 0xFFFFFFFF.toInt()
            c.drawCircle(-8f, -46f, 58f, fill)
            fill.color = 0xFF8A5A3A.toInt()
            c.drawCircle(-8f, -46f, 48f, fill)
            fill.color = 0xFFFFFFFF.toInt()
            c.drawRoundRect(-66f, -52f, 50f, 84f, 12f, 12f, fill)
            line.color = 0xFF8A5A3A.toInt(); line.strokeWidth = 6f
            c.drawRoundRect(-66f, -52f, 50f, 84f, 12f, 12f, line)
            c.drawArc(RectF(52f, -26f, 108f, 40f), -80f, 160f, false, line)
            line.color = 0xFFB98A6A.toInt(); line.strokeWidth = 5f
            c.drawOval(RectF(-96f, 84f, 80f, 96f), line)
        }
        at(c, 145f, H - 198f, 10f) {
            fill.color = 0xFF8A5A3A.toInt()
            c.drawCircle(-4f, -28f, 40f, fill)
            fill.color = 0xFFFFFFFF.toInt()
            c.drawCircle(-4f, -28f, 36f, fill)
            fill.color = 0xFF8A5A3A.toInt()
            c.drawCircle(-4f, -28f, 28f, fill)
            fill.color = 0xFFFFFFFF.toInt()
            c.drawRoundRect(-44f, -32f, 36f, 48f, 9f, 9f, fill)
            line.color = 0xFF8A5A3A.toInt(); line.strokeWidth = 4.5f
            c.drawRoundRect(-44f, -32f, 36f, 48f, 9f, 9f, line)
            c.drawArc(RectF(38f, -20f, 76f, 26f), -80f, 160f, false, line)
        }
        at(c, W - 312f, 258f, 24f) {
            fill.color = 0xFF6B4423.toInt()
            c.drawCircle(0f, 0f, 9f, fill)
            line.color = 0xFF4A2B13.toInt(); line.strokeWidth = 3f
            c.drawLine(-7f, 3f, 7f, -3f, line)
        }
        at(c, 252f, H - 272f, -20f) {
            fill.color = 0xFF6B4423.toInt()
            c.drawCircle(0f, 0f, 7f, fill)
        }
    }

    private fun cupcake(c: Canvas, W: Float, H: Float) {
        at(c, W - 175f, 215f, -10f) {
            fill.color = 0xFFF0C987.toInt()
            c.drawRoundRect(-52f, 10f, 44f, 82f, 10f, 10f, fill)
            line.color = 0xFFC98F4A.toInt(); line.strokeWidth = 4f
            c.drawLine(-40f, 46f, 34f, 46f, line)
            c.drawLine(-43f, 10f, -35f, 82f, line)
            c.drawLine(30f, 10f, 22f, 82f, line)
            fill.color = 0xFFFDFDFD.toInt()
            c.drawCircle(-22f, -34f, 34f, fill)
            c.drawCircle(16f, -30f, 30f, fill)
            c.drawCircle(-6f, -56f, 26f, fill)
            line.color = 0xFFE8C7D5.toInt(); line.strokeWidth = 4f
            c.drawCircle(-22f, -34f, 34f, line)
            fill.color = 0xFFE8494E.toInt()
            c.drawCircle(-4f, -78f, 13f, fill)
            line.color = 0xFF69B566.toInt(); line.strokeWidth = 4f
            c.drawLine(-4f, -90f, 8f, -106f, line)
            fill.color = 0xFF8CC98F.toInt()
            c.drawCircle(30f, -62f, 8f, fill)
        }
        at(c, 150f, H - 212f, 10f) {
            fill.color = 0xFFF0C987.toInt()
            c.drawRoundRect(-34f, 6f, 30f, 54f, 8f, 8f, fill)
            fill.color = 0xFFFDFDFD.toInt()
            c.drawCircle(-16f, -20f, 22f, fill)
            c.drawCircle(8f, -14f, 18f, fill)
            fill.color = 0xFFE8494E.toInt()
            c.drawCircle(-4f, -46f, 9f, fill)
        }
    }

    private fun sunflowers(c: Canvas, W: Float, H: Float) {
        at(c, W - 170f, 170f, -12f) { flower(c, 0xFFF2CE5E.toInt(), 0xFF6B4423.toInt(), 58f, 12) }
        leaf(c, 0xFF7BBE71.toInt(), 44f, W - 268f, 220f, 120f)
        at(c, W - 88f, 262f, 16f) { flower(c, 0xFFEFC345.toInt(), 0xFF6B4423.toInt(), 34f, 10) }
        at(c, 150f, H - 170f, 12f) { flower(c, 0xFFF2CE5E.toInt(), 0xFF6B4423.toInt(), 42f, 10) }
        leaf(c, 0xFF7BBE71.toInt(), 32f, 216f, H - 134f, 210f)
        fill.color = 0x55EFC345.toInt()
        c.drawCircle(W - 250f, 112f, 8f, fill)
    }

    private fun leaves(c: Canvas, W: Float, H: Float) {
        leaf(c, 0xFF8CC98F.toInt(), 70f, W - 262f, 150f, -30f)
        leaf(c, 0xFF5FA366.toInt(), 52f, W - 185f, 235f, 35f)
        leaf(c, 0xFF8CC98F.toInt(), 44f, W - 322f, 275f, -75f)
        leaf(c, 0xFF5FA366.toInt(), 52f, 118f, H - 196f, 200f)
        leaf(c, 0xFF8CC98F.toInt(), 40f, 190f, H - 276f, 250f)
        leaf(c, 0xFF5FA366.toInt(), 34f, 96f, H - 288f, 130f)
    }

    private fun butterflyMotif(c: Canvas, colorA: Int, colorB: Int, s: Float, x: Float, y: Float, deg: Float) {
        at(c, x, y, deg) {
            fill.color = colorA
            c.drawOval(RectF(-s, -s * 0.85f, -s * 0.08f, s * 0.15f), fill)
            fill.color = colorB
            c.drawOval(RectF(s * 0.08f, -s * 0.85f, s, s * 0.15f), fill)
            fill.color = colorA
            c.drawOval(RectF(-s * 0.8f, s * 0.08f, -s * 0.06f, s * 0.72f), fill)
            fill.color = colorB
            c.drawOval(RectF(s * 0.06f, s * 0.08f, s * 0.8f, s * 0.72f), fill)
            fill.color = 0xFF5C4B3A.toInt()
            c.drawRoundRect(-s * 0.07f, -s * 0.85f, s * 0.07f, s * 0.55f, s * 0.07f, s * 0.07f, fill)
            line.color = 0xFF5C4B3A.toInt(); line.strokeWidth = s * 0.06f
            c.drawLine(0f, -s * 0.85f, -s * 0.18f, -s * 1.1f, line)
            c.drawLine(0f, -s * 0.85f, s * 0.18f, -s * 1.1f, line)
        }
    }

    private fun butterflies(c: Canvas, W: Float, H: Float) {
        butterflyMotif(c, 0xFFF27FB2.toInt(), 0xFFB58ECC.toInt(), 46f, W - 175f, 175f, -12f)
        butterflyMotif(c, 0xFF7FB3E8.toInt(), 0xFF8CE0F2.toInt(), 30f, W - 272f, 258f, 18f)
        butterflyMotif(c, 0xFFEE9BB5.toInt(), 0xFFF7C6D8.toInt(), 24f, W - 96f, 300f, -24f)
        butterflyMotif(c, 0xFF7FB3E8.toInt(), 0xFFF27FB2.toInt(), 36f, 150f, H - 190f, 12f)
        butterflyMotif(c, 0xFFB58ECC.toInt(), 0xFFF7C6D8.toInt(), 26f, 250f, H - 268f, -20f)
    }

    private fun stars(c: Canvas, W: Float, H: Float) {
        star(c, 0xFFF2CE5E.toInt(), 54f, W - 165f, 168f, -8f)
        star(c, 0xFFFFD980.toInt(), 30f, W - 268f, 244f, 14f)
        star(c, 0xFFF2CE5E.toInt(), 20f, W - 92f, 306f, -18f)
        star(c, 0xFFFFD980.toInt(), 42f, 150f, H - 176f, 10f)
        star(c, 0xFFF2CE5E.toInt(), 24f, 248f, H - 252f, -14f)
        fill.color = 0x66F2CE5E.toInt()
        c.drawCircle(W - 334f, 158f, 7f, fill)
        c.drawCircle(102f, H - 306f, 6f, fill)
    }

    private fun pawMotif(c: Canvas, color: Int, s: Float, x: Float, y: Float, deg: Float) {
        at(c, x, y, deg) {
            fill.color = color
            c.drawCircle(0f, s * 0.28f, s * 0.42f, fill)
            c.drawCircle(-s * 0.42f, -s * 0.28f, s * 0.18f, fill)
            c.drawCircle(-s * 0.15f, -s * 0.45f, s * 0.19f, fill)
            c.drawCircle(s * 0.15f, -s * 0.45f, s * 0.19f, fill)
            c.drawCircle(s * 0.42f, -s * 0.28f, s * 0.18f, fill)
        }
    }

    private fun paws(c: Canvas, W: Float, H: Float) {
        pawMotif(c, 0xFFB98A6A.toInt(), 62f, W - 170f, 170f, -12f)
        pawMotif(c, 0xFFD9B08C.toInt(), 36f, W - 274f, 254f, 16f)
        pawMotif(c, 0xFFB98A6A.toInt(), 46f, 152f, H - 174f, 10f)
        pawMotif(c, 0xFFD9B08C.toInt(), 30f, 256f, H - 258f, -16f)
    }

    private fun teddy(c: Canvas, W: Float, H: Float) {
        at(c, W - 180f, 212f, -8f) {
            val body = 0xFFC99B72.toInt()
            fill.color = body
            c.drawCircle(-32f, -76f, 22f, fill)
            c.drawCircle(32f, -76f, 22f, fill)
            fill.color = 0xFFF6E7D4.toInt()
            c.drawCircle(-32f, -76f, 12f, fill)
            c.drawCircle(32f, -76f, 12f, fill)
            fill.color = body
            c.drawCircle(0f, -16f, 46f, fill)
            fill.color = 0xFFF6E7D4.toInt()
            c.drawCircle(0f, 0f, 26f, fill)
            fill.color = 0xFF523421.toInt()
            c.drawCircle(0f, -12f, 7f, fill)
            fill.color = 0xFF3A2618.toInt()
            c.drawCircle(-14f, -28f, 5f, fill)
            c.drawCircle(14f, -28f, 5f, fill)
            line.color = 0xFF9A7A55.toInt(); line.strokeWidth = 3f
            c.drawLine(0f, -6f, 0f, 8f, line)
            c.drawArc(RectF(-12f, 2f, 12f, 18f), 10f, 160f, false, line)
            heart(c, 0xFFE8899B.toInt(), 30f, 0f, 92f, 0f)
        }
        at(c, 155f, H - 208f, 10f) {
            fill.color = 0xFFC99B72.toInt()
            c.drawCircle(0f, -4f, 30f, fill)
            fill.color = 0xFFF6E7D4.toInt()
            c.drawCircle(0f, 4f, 17f, fill)
            fill.color = 0xFF523421.toInt()
            c.drawCircle(0f, -3f, 4f, fill)
            fill.color = 0xFF3A2618.toInt()
            c.drawCircle(-8f, -11f, 3f, fill)
            c.drawCircle(8f, -11f, 3f, fill)
        }
    }
}
