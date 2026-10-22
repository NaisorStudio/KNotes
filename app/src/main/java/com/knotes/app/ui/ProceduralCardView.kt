package com.knotes.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** Miniatura procedural de tarjetas de plantillas: página + emojis + marco suave. */
class ProceduralCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var cardColor: Int = Color.WHITE
    var emoji: String = "🗒"
    var template: Int = 0
    var isSquare: Boolean = true

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        p.style = Paint.Style.FILL
        p.color = cardColor
        c.drawRoundRect(0f, 0f, w, h, w * 0.1f, w * 0.1f, p)

        // mini página con plantilla
        val mx = w * 0.16f; val my = if (isSquare) w * 0.2f else h * 0.18f
        val mw = w - mx * 2; val mh = h - my - h * 0.12f
        p.color = Color.WHITE
        c.drawRoundRect(mx, my, mx + mw, my + mh, w * 0.05f, w * 0.05f, p)
        p.color = Color.argb(70, 0, 0, 0)
        when (template) {
            0 -> repeat(7) { i ->
                val y = my + mh * (0.12f + i * 0.125f)
                c.drawLine(mx + mw * 0.1f, y, mx + mw * 0.9f, y, p)
            }
            1 -> {
                val n = 5
                for (i in 0..n) {
                    val x = mx + mw * (0.12f + i * 0.76f / n)
                    c.drawLine(x, my + mh * 0.08f, x, my + mh * 0.92f, p)
                }
                for (i in 0..6) {
                    val y = my + mh * (0.1f + i * 0.13f)
                    c.drawLine(mx + mw * 0.1f, y, mx + mw * 0.92f, y, p)
                }
            }
            2 -> for (iy in 0..4) for (ix in 0..4) {
                c.drawCircle(mx + mw * (0.15f + ix * 0.175f), my + mh * (0.12f + iy * 0.19f), w * 0.008f + 1.5f, p)
            }
            else -> { }
        }

        // banda lateral
        p.color = Color.argb(200, 255, 255, 255)
        c.drawRoundRect(mx - w * 0.05f, my, mx, my + mh, w * 0.025f, w * 0.025f, p)
        p.color = Color.argb(90, 0, 0, 0)
        c.drawRoundRect(mx - w * 0.05f, my, mx, my + mh * 0.5f, w * 0.025f, w * 0.025f, p)

        // emoji art
        tp.color = Color.DKGRAY
        tp.textSize = if (isSquare) w * 0.16f else w * 0.085f
        val ey = if (isSquare) my + mh * 0.55f else my + mh * 0.7f
        c.drawText(emoji, w / 2f, ey, tp)

        // esquinas decorativas
        p.color = Color.argb(120, 120, 90, 60)
        tp.textSize = w * 0.06f
        tp.textAlign = Paint.Align.LEFT
        c.drawText("✿", mx + mw * 0.04f, my + mh * 0.9f, tp)
        tp.textAlign = Paint.Align.RIGHT
        c.drawText("✎", mx + mw * 0.96f, my + mh * 0.9f, tp)
        tp.textAlign = Paint.Align.CENTER
    }
}
