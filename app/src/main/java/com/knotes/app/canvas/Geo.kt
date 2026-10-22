package com.knotes.app.canvas

import com.knotes.app.data.Pt
import com.knotes.app.data.Stroke
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/* ============================================================
   Geometría para goma y lazo
   ============================================================ */
object Geo {

    fun dist(p: Pt, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1; val dy = y2 - y1
        val l2 = dx * dx + dy * dy
        var t = if (l2 == 0f) 0f else ((p.x - x1) * dx + (p.y - y1) * dy) / l2
        t = max(0f, min(1f, t))
        val cx = x1 + t * dx; val cy = y1 + t * dy
        return sqrt((p.x - cx) * (p.x - cx) + (p.y - cy) * (p.y - cy))
    }

    /** Distancia mínima de un punto al trazo */
    fun distToStroke(p: Pt, s: Stroke): Float {
        if (s.points.isEmpty()) return Float.MAX_VALUE
        if (s.points.size == 1) return dist(p, s.points[0].x, s.points[0].y, s.points[0].x, s.points[0].y)
        var d = Float.MAX_VALUE
        for (i in 0 until s.points.size - 1) {
            val a = s.points[i]; val b = s.points[i + 1]
            val g = dist(p, a.x, a.y, b.x, b.y)
            if (g < d) d = g
        }
        return d
    }

    /** ¿Dentro de un polígono (ray casting)? */
    fun insidePolygon(poly: MutableList<Pt>, x: Float, y: Float): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val xi = poly[i].x; val yi = poly[i].y
            val xj = poly[j].x; val yj = poly[j].y
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) inside = !inside
            j = i
        }
        return inside
    }

    fun polyBounds(poly: List<Pt>): AuxPolyBounds {
        if (poly.isEmpty()) return AuxPolyBounds(0f, 0f, 0f, 0f)
        var l = Float.MAX_VALUE; var t = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
        for (p in poly) { l = min(l, p.x); t = min(t, p.y); r = max(r, p.x); b = max(b, p.y) }
        return AuxPolyBounds(l, t, r, b)
    }

    fun newId(): String = "e${System.nanoTime().toString(36)}${(0..1000).random().toString(36)}"
}

class AuxPolyBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
}
