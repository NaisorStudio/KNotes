package com.knotes.app.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min
import kotlin.random.Random

/* ============================================================
   KNotes — modelo de datos
   · Todo lo que hay en la página es un "elemento" independiente.
     Ej.: Stroke = un trazo vectorial (lista de puntos), por eso
     SIEMPRE es editable/borrable incluso tras guardar, salir
     y volver a entrar: no se "aplasta" en un bitmap, se guarda en JSON.
   ============================================================ */

abstract class PaperElement {
    abstract val id: String
    /** giro en grados (0 = derecho) — lo aplican los dibujados; los trazos se giran transformando puntos */
    var rot: Float = 0f
    abstract fun bounds(): AuxRectF
    abstract fun moveTo(dx: Float, dy: Float)
    abstract fun toJson(): JSONObject
    abstract fun clone(): PaperElement
    abstract fun invalidatePathCache()
}

data class Aux(val unused: Int = 0)

class AuxRectF(var left: Float = 0f, var top: Float = 0f, var right: Float = 0f, var bottom: Float = 0f) {
    fun width() = right - left
    fun height() = bottom - top
    fun inflate(d: Float): AuxRectF {
        left -= d; top -= d; right += d; bottom += d
        return this
    }
}

class Pt(var x: Float = 0f, var y: Float = 0f)

/** Tipos de pincel */
object Brush {
    const val PEN = 0        // bolígrafo
    const val MARKER = 1     // rotulador (trazo plano semitransparente)
    const val PENCIL = 2     // lápiz (grano detalles)
    const val HIGHLIGHTER = 3 // subrayador translúcido
}

class Stroke(
    override val id: String,
    var color: Int,
    var width: Float,
    var brush: Int,
    val points: MutableList<Pt> = ArrayList()
) : PaperElement() {

    override fun bounds(): AuxRectF {
        if (points.isEmpty()) return AuxRectF()
        var l = Float.MAX_VALUE; var t = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
        for (p in points) {
            l = min(l, p.x); t = min(t, p.y); r = kotlin.math.max(r, p.x); b = kotlin.math.max(b, p.y)
        }
        return AuxRectF(l, t, r, b).inflate(width)
    }

    override fun moveTo(dx: Float, dy: Float) {
        for (p in points) { p.x += dx; p.y += dy }
        invalidatePathCache()
    }

    override fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("t", "s")
        o.put("id", id)
        o.put("c", color)
        o.put("w", width)
        o.put("b", brush)
        val arr = JSONArray()
        for (p in points) { val q = JSONObject(); q.put("x", p.x.toDouble()); q.put("y", p.y.toDouble()); arr.put(q) }
        o.put("pts", arr)
        return o
    }

    override fun clone(): Stroke {
        val s = Stroke(id + "_c", color, width, brush)
        for (p in points) s.points.add(Pt(p.x, p.y))
        return s
    }

    override fun invalidatePathCache() { pathCache = null }
    var pathCache: android.graphics.Path? = null

    companion object {
        fun fromJson(o: JSONObject): Stroke {
            val pts = ArrayList<Pt>()
            val arr = o.getJSONArray("pts")
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                pts.add(Pt(e.getDouble("x").toFloat(), e.getDouble("y").toFloat()))
            }
            return Stroke(o.getString("id"), o.getInt("c"), o.getDouble("w").toFloat(), o.getInt("b"), pts)
        }
    }
}

class TextElement(
    override val id: String,
    var x: Float, var y: Float,
    var text: String,
    var size: Float,
    var color: Int
) : PaperElement() {
    var pathCache: android.text.StaticLayout? = null
    var cacheTpl: Int = -1          // plantilla del papel para la que se creó el layout
    var baseY: Float = -1f          // NEW: y donde DEBE caer la PRIMERA línea base (== renglón del papel)
    override fun bounds(): AuxRectF =
        if (baseY >= 0f) AuxRectF(x, y - size, x + wrapWidth(size), y - size + estimatedHeight(size, text))
        else AuxRectF(x, y, x + wrapWidth(size), y + estimatedHeight(size, text))
    override fun moveTo(dx: Float, dy: Float) {
        x += dx; y += dy
        if (baseY >= 0f) baseY = y    // al mover el texto, su renglón de anclaje va con él
    }
    override fun invalidatePathCache() { pathCache = null }
    override fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("t", "te"); o.put("id", id); o.put("x", x.toDouble()); o.put("y", y.toDouble())
        o.put("text", text); o.put("size", size.toDouble()); o.put("c", color)
        if (baseY >= 0f) o.put("by", baseY.toDouble())
        if (rot != 0f) o.put("r", rot.toDouble())
        return o
    }
    override fun clone() = TextElement(id + "_c", x, y, text, size, color).also { it.baseY = baseY; it.rot = rot }
    companion object {
        fun wrapWidth(size: Float) = 620f * (size / 34f).coerceIn(0.7f, 2.4f)
        fun fromJson(o: JSONObject) = TextElement(o.getString("id"), o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.getString("text"), o.getDouble("size").toFloat(), o.getInt("c")).also {
            it.baseY = if (o.has("by")) o.getDouble("by").toFloat() else -1f
            it.rot = if (o.has("r")) o.getDouble("r").toFloat() else 0f
        }

        // Altura estimada simple (parpadeo evitación): densidad por líneas
        private fun lines(text: String): Int {
            var n = 1
            for (i in 0 until text.length) if (text[i] == '\n') n++
            return n
        }
        private fun wrapish(text: String, w: Float, size: Float): Int {
            return lines(text) * ((text.length.coerceAtMost(300) * size * 0.45f / w).toInt().coerceAtLeast(1))
        }
        fun estimatedHeight(size: Float, text: String): Float {
            val h = fontHeight(size)
            return h * (wrapish(text, wrapWidth(size), size) + 1) + 8
        }
        fun fontHeight(size: Float) = size * 1.5f
    }
}

class ImageElement(
    override val id: String,
    var x: Float, var y: Float,
    var w: Float, var h: Float,
    var path: String
) : PaperElement() {
    var bitmapCache: android.graphics.Bitmap? = null
    @Transient var failedLoad: Boolean = false
    override fun bounds() = AuxRectF(x, y, x + w, y + h)
    override fun moveTo(dx: Float, dy: Float) { x += dx; y += dy }
    override fun invalidatePathCache() { bitmapCache = null }
    override fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("t", "im"); o.put("id", id); o.put("x", x.toDouble()); o.put("y", y.toDouble())
        o.put("w", w.toDouble()); o.put("h", h.toDouble()); o.put("path", path)
        if (rot != 0f) o.put("r", rot.toDouble())
        return o
    }
    override fun clone() = ImageElement(id + "_c", x, y, w, h, path).also { it.rot = rot }
}

class LinkElement(
    override val id: String,
    var x: Float, var y: Float,
    var label: String,
    var url: String,
    var color: Int
) : PaperElement() {
    /** multiplicador de tamaño del chip (se le agranda/achica con el cursor) */
    var scale: Float = 1f
    override fun bounds(): AuxRectF =
        AuxRectF(x, y, x + (78f + label.length * 16f).coerceAtLeast(170f) * scale, y + 74f * scale)
    override fun moveTo(dx: Float, dy: Float) { x += dx; y += dy }
    override fun invalidatePathCache() {}
    override fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("t", "li"); o.put("id", id); o.put("x", x.toDouble()); o.put("y", y.toDouble())
        o.put("label", label); o.put("url", url); o.put("c", color)
        if (rot != 0f) o.put("r", rot.toDouble())
        if (scale != 1f) o.put("sc", scale.toDouble())
        return o
    }
    override fun clone() = LinkElement(id + "_c", x, y, label, url, color).also { it.rot = rot; it.scale = scale }
    companion object {
        fun fromJson(o: JSONObject) = LinkElement(o.getString("id"), o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.getString("label"), o.getString("url"), o.getInt("c")).also {
            it.rot = if (o.has("r")) o.getDouble("r").toFloat() else 0f
            it.scale = if (o.has("sc")) o.getDouble("sc").toFloat() else 1f
        }
    }
}

class StickerElement(
    override val id: String,
    var x: Float, var y: Float,
    var emoji: String,
    var size: Float
) : PaperElement() {
    override fun bounds() = AuxRectF(x, y, x + size * 1.2f, y + size * 1.4f)
    override fun moveTo(dx: Float, dy: Float) { x += dx; y += dy }
    override fun invalidatePathCache() {}
    override fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("t", "st"); o.put("id", id); o.put("x", x.toDouble()); o.put("y", y.toDouble())
        o.put("emoji", emoji); o.put("size", size.toDouble())
        if (rot != 0f) o.put("r", rot.toDouble())
        return o
    }
    override fun clone() = StickerElement(id + "_c", x, y, emoji, size).also { it.rot = rot }
    companion object { fun fromJson(o: JSONObject) = StickerElement(o.getString("id"), o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.getString("emoji"), o.getDouble("size").toFloat()).also {
        it.rot = if (o.has("r")) o.getDouble("r").toFloat() else 0f
    } }
}

object Elements {
    fun fromJson(o: JSONObject): PaperElement = when (o.getString("t")) {
        "s" -> Stroke.fromJson(o)
        "te" -> TextElement.fromJson(o)
        "im" -> ImageElement(o.getString("id"), o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.getDouble("w").toFloat(), o.getDouble("h").toFloat(), o.getString("path")).also {
            it.rot = if (o.has("r")) o.getDouble("r").toFloat() else 0f
        }
        "li" -> LinkElement.fromJson(o)
        "st" -> StickerElement.fromJson(o)
        else -> throw IllegalArgumentException("tipo desconocido")
    }
}

/** Una página de papel */
class KPage(
    var id: String,
    var template: Int,           // 0 rayado, 1 cuadrícula, 2 puntos, 3 en blanco
    var paper: Int = 0,          // 0 crema, 1 blanco, 2 oscuro, 3 rosa, 4 menta, 5 lavanda, 6 celeste
    var decor: Int = 0,          // adorno de esquinas (ver canvas/Decors)
    val items: MutableList<PaperElement> = ArrayList()
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id); o.put("template", template)
        if (paper != 0) o.put("paper", paper)
        if (decor != 0) o.put("decor", decor)
        val arr = JSONArray()
        for (el in items) arr.put(el.toJson())
        o.put("items", arr)
        return o
    }
    companion object { fun fromJson(o: JSONObject): KPage {
        val items = ArrayList<PaperElement>()
        val arr = o.getJSONArray("items")
        for (i in 0 until arr.length()) items.add(Elements.fromJson(arr.getJSONObject(i)))
        return KPage(o.getString("id"), o.getInt("template"),
            if (o.has("paper")) o.getInt("paper") else 0,
            if (o.has("decor")) o.getInt("decor") else 0, items)
    } }
}

class Notebook(
    var id: String,
    var title: String,
    var coverStyle: Int,
    var coverSeed: Long,
    var tags: MutableList<String> = ArrayList(),
    var favorite: Boolean = false,
    var createdAt: Long = 0,
    var updatedAt: Long = 0,
    var deletedAt: Long? = null
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id); o.put("title", title); o.put("coverStyle", coverStyle)
        o.put("coverSeed", coverSeed); o.put("favorite", favorite)
        o.put("createdAt", createdAt); o.put("updatedAt", updatedAt)
        if (deletedAt != null) o.put("deletedAt", deletedAt)
        val arr = JSONArray()
        for (t in tags) arr.put(t)
        o.put("tags", arr)
        return o
    }
    companion object {
        fun fromJson(o: JSONObject): Notebook {
            val tags = ArrayList<String>()
            val arr = o.getJSONArray("tags")
            for (i in 0 until arr.length()) tags.add(arr.getString(i))
            return Notebook(
                o.getString("id"), o.getString("title"), o.getInt("coverStyle"), o.getLong("coverSeed"),
                tags, o.getBoolean("favorite"), o.getLong("createdAt"), o.getLong("updatedAt"),
                if (o.has("deletedAt")) o.getLong("deletedAt") else null
            )
        }
    }
}

object Ids {
    var counter = System.nanoTime()
    fun next(): String {
        counter++
        return "e${counter.toString(36)}${Random.nextInt(0, 46656).toString(36)}"
    }
}
