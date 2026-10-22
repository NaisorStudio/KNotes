package com.knotes.app.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.knotes.app.data.AuxRectF
import com.knotes.app.data.Brush
import com.knotes.app.data.ImageElement
import com.knotes.app.data.KPage
import com.knotes.app.data.LinkElement
import com.knotes.app.data.Notebook
import com.knotes.app.data.PaperElement
import com.knotes.app.data.Pt
import com.knotes.app.data.StickerElement
import com.knotes.app.data.Stroke
import com.knotes.app.data.TextElement
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/* ============================================================
   CanvasPageView — papel interactivo
   · Cada trazo es un objeto vectorial => tras guardar y volver,
     puede borrarse (goma por zona o por trazo) o moverse (lazo).
   · Gestos: 1 dedo dibuja/usar herramienta; 2 dedos zoom+pan.
   ============================================================ */

object Tools {
    const val PEN = 0
    const val MARKER = 1
    const val PENCIL = 2
    const val HIGHLIGHTER = 3
    const val ERASER = 4
    const val LASSO = 5
    const val TEXT = 6
    const val IMAGE = 7
    const val LINK = 8
    const val STICKER = 9
    const val SELECT = 10
}

interface CanvasHost {
    fun onPageChanged(idx: Int, count: Int)
    fun onHistoryChanged(canUndo: Boolean, canRedo: Boolean)
    fun onSelectionChanged(hasSel: Boolean)
    fun editText(el: TextElement?, pt: Pt)
    fun pickImage()
    fun linkAt(el: LinkElement?, pt: Pt)
    fun openStickers()
    fun saveNow()
}

class CanvasPageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var host: CanvasHost? = null

    /* ---------- página actual ---------- */
    lateinit var notebook: Notebook
    var pages: MutableList<KPage> = ArrayList()
    var pageIndex = 0
        private set

    val curPage: KPage get() = pages[pageIndex]
    val pageItems: MutableList<PaperElement> get() = curPage.items

    /* ---------- configuración herramienta ---------- */
    var tool = Tools.PEN
    var penColor = 0xFF2B2B2B.toInt()
    var penWidth = 5f
    var brushType = Brush.PEN
    var eraserWhole = false          // false: borra por zona (partes del trazo)
    var eraserRadius = 18f           // en unidades de papel
    var stickerPending: String? = null

    /* ---------- transformación ---------- */
    companion object {
        const val PAPER_W = 1000f
        const val PAPER_H = 1414f
        val PAPER_COLORS = intArrayOf(
            0xFFFBF7ED.toInt(),  // 0 crema
            0xFFFFFFFF.toInt(),  // 1 blanco
            0xFF2E2A33.toInt(),  // 2 oscuro
            0xFFFCEDF1.toInt(),  // 3 rosa pastel
            0xFFEAF6EE.toInt(),  // 4 menta
            0xFFF1ECFA.toInt(),  // 5 lavanda
            0xFFE8F1FA.toInt()   // 6 celeste
        )
        val TEMPLATES = arrayOf("Rayado", "Cuadrícula", "Puntos", "En blanco")
    }

    private var scale = 1f
    private var tx = 0f
    private var ty = 0f
    private var vmW = 1000f
    private var vmH = 1414f

    /* ---------- estado de dibujo ---------- */
    private val MODE_IDLE = 0
    private val MODE_DRAW = 1
    private val MODE_ERASE = 2
    private val MODE_LASSO = 3
    private val MODE_MOVESEL = 4
    private val MODE_SCALEEL = 5
    private val MODE_GEST = 6

    private var liveStroke: Stroke? = null
    private var mode = MODE_IDLE

    private val lasso = ArrayList<Pt>()
    private val selection = ArrayList<PaperElement>()
    private var lastSelected: PaperElement? = null
    private var downPt = PointF()
    private var movedFar = false
    private var scaleAnchor = RectF()

    /* ---------- historial ---------- */
    private val undoStack = ArrayList<HEntry>()
    private val redoStack = ArrayList<HEntry>()

    private class HEntry(val pageIdx: Int, val snapshot: List<PaperElement>)

    /* ---------- caché de páginas activas no necesaria: se recarga por elemento ---------- */

    /* ================== setup ================== */

    fun setup(nb: Notebook, pages: MutableList<KPage>, startIndex: Int) {
        this.notebook = nb
        this.pages = pages
        isSetup = true
        pageIndex = startIndex.coerceIn(0, pages.size - 1)
        undoStack.clear(); redoStack.clear()
        selection.clear(); lasso.clear()
        if (width > 0) fitPage() else requestLayout()
        invalidate()
        host?.onPageChanged(pageIndex, pages.size)
        host?.onHistoryChanged(false, false)
    }

    private fun fitPage() {
        vmW = width.toFloat(); vmH = height.toFloat()
        if (vmW <= 0) return
        scale = min(vmW * 0.97f / PAPER_W, vmH * 0.93f / PAPER_H)
        tx = (vmW - PAPER_W * scale) / 2f
        ty = (vmH - PAPER_H * scale) / 2f
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        vmW = w.toFloat(); vmH = h.toFloat()
        fitPage()
    }

    /* ================== coords ================== */
    private fun paperX(sx: Float) = (sx - tx) / scale
    private fun paperY(sy: Float) = (sy - ty) / scale

    /* ================== historial ================== */

    private fun record() {
        undoStack.add(HEntry(pageIndex, pageItems.map { it.clone() }))
        if (undoStack.size > 40) undoStack.removeAt(0)
        redoStack.clear()
        host?.onHistoryChanged(true, false)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val e = undoStack.removeAt(undoStack.size - 1)
        redoStack.add(HEntry(e.pageIdx, pages[e.pageIdx].items.toList()))
        pages[e.pageIdx].items.apply { clear(); addAll(e.snapshot) }
        clearSelection()
        invalidate()
        host?.onHistoryChanged(undoStack.isNotEmpty(), redoStack.isNotEmpty())
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val e = redoStack.removeAt(redoStack.size - 1)
        undoStack.add(HEntry(e.pageIdx, pages[e.pageIdx].items.toList()))
        pages[e.pageIdx].items.apply { clear(); addAll(e.snapshot) }
        clearSelection()
        invalidate()
        host?.onHistoryChanged(undoStack.isNotEmpty(), redoStack.isNotEmpty())
    }

    /* ================== páginas ================== */

    fun goToPage(i: Int) {
        if (i < 0 || i >= pages.size || i == pageIndex) return
        pageIndex = i
        clearSelection()
        fitPage()
        host?.onPageChanged(pageIndex, pages.size)
    }

    fun addPage(template: Int, decor: Int = 0) {
        pages.add(KPage(Geo.newId(), template, curPage.paper, decor))
        goToPage(pages.size - 1)
    }

    fun deleteCurrentPage(): Boolean {
        if (pages.size <= 1) return false
        pages.removeAt(pageIndex)
        goToPage(if (pageIndex >= pages.size) pages.size - 1 else pageIndex)
        return true
    }

    fun setTemplate(t: Int) {
        curPage.template = t
        invalidate()
    }

    /** color de papel de la página actual (0 crema, 1 blanco, 2 oscuro, 3 rosa, 4 menta, 5 lavanda, 6 celeste) */
    fun setPaperColor(idx: Int) {
        curPage.paper = idx.coerceIn(0, PAPER_COLORS.size - 1)
        invalidate()
    }

    /** adorno de esquinas del papel actual (ver canvas/Decors) */
    fun setPageDecor(d: Int) {
        curPage.decor = d.coerceIn(0, 13)
        invalidate()
    }

    fun curDecor(): Int = curPage.decor

    fun clearPage() {
        record()
        pageItems.clear()
        clearSelection()
        invalidate()
    }

    /* ================== acciones de escritorio ================== */

    fun addStickerAtPaper(emoji: String, x: Float, y: Float) {
        record()
        val size = 90f
        // el sticker SE MANTIENE dentro del papel (el límite es el papel)
        val cx = x.coerceIn(size * 0.45f, PAPER_W - size * 0.75f)
        val cy = y.coerceIn(size * 0.45f, PAPER_H - size * 1.05f)
        pageItems.add(StickerElement(Geo.newId(), cx, cy, emoji, size))
        invalidate()
    }

    fun addImageElement(path: String, w: Float, h: Float) {
        record()
        // SIEMPRE en lo que el usuario ve ahora (centro del papel visible),
        // no en el centro absoluto de la página que podía quedar fuera de la vista
        val cx = paperX(width / 2f)
        val cy = paperY(height / 2f)
        val x = (cx - w / 2f).coerceIn(40f, (PAPER_W - w - 40f).coerceAtLeast(40f))
        val y = (cy - h / 2f).coerceIn(40f, (PAPER_H - h - 40f).coerceAtLeast(40f))
        pageItems.add(ImageElement(Geo.newId(), x, y, w, h, path))
        invalidate()
    }

    /** separación de líneas del papel según plantilla (0 rayado, 1 cuadrícula, 2 puntos, 3 blanco) */
    fun lineSpacingOf(tpl: Int): Float = when (tpl.coerceIn(0, 3)) {
        0 -> 76f; 1 -> 74f; 2 -> 80f; else -> 76f
    }
    fun lineStartOf(tpl: Int): Float = when (tpl.coerceIn(0, 3)) {
        0 -> 130f; 1 -> 60f; 2 -> 110f; else -> 130f
    }

    /** y del renglón más cercano al toque (la escritura SIGUE las líneas del papel) */
    fun snapLineY(y: Float): Float {
        val tpl = curPage.template
        val sp = lineSpacingOf(tpl)
        val base = lineStartOf(tpl)
        val row = Math.round((y - base) / sp)
        return (base + row * sp).coerceIn(40f, PAPER_H - 60f)
    }

    fun addTextElement(text: String, size: Float, color: Int, x: Float, y: Float) {
        record()
        // el textó EMPIEZA donde tocaste y su PRIMERA línea cae EXACTAMENTE
        // sobre el renglón del papel más cercano (todos por igual, sin salto arriba/abajo)
        val rowY = snapLineY(y)
        val el = TextElement(Geo.newId(), x.coerceIn(40f, PAPER_W - 80f), rowY, text, size, color)
        el.baseY = rowY
        el.cacheTpl = curPage.template
        pageItems.add(el)
        invalidate()
    }

    fun replaceTextElement(el: TextElement, newText: String, newSize: Float, newColor: Int) {
        el.text = newText; el.size = newSize; el.color = newColor
        el.invalidatePathCache()
        invalidate()
    }

    fun addLinkElement(label: String, url: String, x: Float, y: Float) {
        record()
        pageItems.add(LinkElement(Geo.newId(), x, y, label, url,
            if (com.knotes.app.ui.Gender.isMan) 0xFF4A90D9.toInt() else 0xFFEE5F8B.toInt()))
        invalidate()
    }

    fun clearSelection() {
        selection.clear(); lastSelected = null; lasso.clear()
        host?.onSelectionChanged(false)
        invalidate()
    }

    fun deleteSelection() {
        if (selection.isEmpty()) return
        record()
        pageItems.removeAll(selection.toSet())
        clearSelection()
        invalidate()
    }

    fun deleteElement(el: PaperElement) {
        record()
        pageItems.remove(el)
        clearSelection()
        invalidate()
    }

    fun editSelected() {
        val el = lastSelected ?: return
        if (el is TextElement) host?.editText(el, Pt(el.x, el.y))
        else if (el is LinkElement) host?.linkAt(el, Pt(el.x, el.y))
    }

    fun selectedCount() = selection.size

    /* ================== goma ==================
       · UNA sola instantánea de deshacer POR ARRASTRÓN completo
         (borrar con fuerza ya no llena el historial hasta desbordarlo).
       · Opera por OBJETO, no por índice: nunca más crash por índices corridos.
       · ¡Borra de TODO!: trazos por zona/trazo y, si tocas un sticker,
         un texto, una imagen o un enlace, los quita directo. */
    private var erasedDirty = false

    private fun eraseRecord() {
        if (!erasedDirty) {
            record()
            erasedDirty = true
        }
    }

    private fun eraseAt(p: Pt) {
        val r = eraserRadius * 2.2f
        val remove = HashSet<PaperElement>()
        val splitOps = ArrayList<Pair<PaperElement, List<Stroke>>>()

        if (eraserWhole) {
            for (el in pageItems) {
                when (el) {
                    is Stroke -> if (Geo.distToStroke(p, el) < r + el.width) remove.add(el)
                    else -> if (hitBounds(el.bounds(), p.x, p.y, r * 0.7f)) remove.add(el)
                }
            }
        } else {
            // goma por zona: parte los trazos que toca el borrador
            for (el in pageItems) {
                when (el) {
                    is Stroke -> {
                        val out = ArrayList<MutableList<Pt>>()
                        var cur: MutableList<Pt>? = null
                        for (pt in el.points) {
                            val near = hypot((pt.x - p.x).toDouble(), (pt.y - p.y).toDouble()) < r
                            if (!near) {
                                if (cur == null) cur = ArrayList()
                                cur.add(Pt(pt.x, pt.y))
                            } else {
                                if (cur != null && cur.size > 1) out.add(cur)
                                cur = null
                            }
                        }
                        if (cur != null && cur.size > 1) out.add(cur)
                        val changed = out.size > 1 || (out.size == 1 && out[0].size != el.points.size) || out.isEmpty()
                        if (changed) {
                            val newStrokes = ArrayList<Stroke>()
                            for (run in out) newStrokes.add(Stroke(Geo.newId(), el.color, el.width, el.brush, run))
                            splitOps.add(el to newStrokes)
                        }
                    }
                    else -> if (hitBounds(el.bounds(), p.x, p.y, r * 0.7f)) remove.add(el)
                }
            }
        }
        if (remove.isEmpty() && splitOps.isEmpty()) return

        // snapshot ANTES de mutar (solo la primera vez de cada arrastrón)
        eraseRecord()

        // aplicar partiendo los índices-robusto: busco el índice del objeto en ese momento
        for ((old, replaces) in splitOps) {
            val i = pageItems.indexOf(old)
            if (i < 0) continue
            pageItems.removeAt(i)
            if (replaces.isNotEmpty()) pageItems.addAll(i, replaces)
        }
        if (remove.isNotEmpty()) pageItems.removeAll(remove)
        invalidate()
    }

    /* ================== toque ================== */

    override fun onTouchEvent(e: MotionEvent): Boolean {
        // —— PALM REJECTION —— si en la sesión ya detectamos UN LÁPIZ/stylus,
        // los toques con DEDO/PALMA no dibujan (ni manchan); el dedo vuelve a
        // dibujar cuando NO haya habido ningún stylus tocando. El pinch-zoom
        // de dos dedos sigue funcionando con ambos escenarios.
        if (e.actionMasked == MotionEvent.ACTION_DOWN || e.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            for (i in 0 until e.pointerCount) {
                val t = e.getToolType(i)
                if (t == MotionEvent.TOOL_TYPE_STYLUS || t == MotionEvent.TOOL_TYPE_ERASER) stylusSeen = true
            }
        }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                downPt = PointF(e.x, e.y)
                movedFar = false
                val onlyTouch = stylusSeen && !isStylus(e, 0)
                if (onlyTouch && tool in intArrayOf(Tools.PEN, Tools.MARKER, Tools.PENCIL, Tools.HIGHLIGHTER, Tools.ERASER)) {
                    mode = MODE_IDLE          // palma mientras escribes con lápiz: ignorar
                    return true
                }
                drawPtr = 0
                handleDown(e.x, e.y, e.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER)
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                if (i < e.pointerCount && isStylus(e, i)) {
                    // el STYLUS entra mientras un dedo estaba abajo → el dedo cede
                    liveStroke?.let { commitLiveStrokeCancelled() }
                    if (mode == MODE_LASSO) lasso.clear()
                    drawPtr = i
                    handleDown(e.getX(i), e.getY(i), e.getToolType(i) == MotionEvent.TOOL_TYPE_ERASER)
                    return true
                }
                if (e.pointerCount >= 2 && drawPtr < 0) {
                    liveStroke?.let { if (it.points.size > 12) commitLiveStroke() else commitLiveStrokeCancelled() }
                    if (mode == MODE_LASSO) lasso.clear()
                    startGesture(e)
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (e.actionIndex == drawPtr) {
                    handleUp(e.getX(e.actionIndex), e.getY(e.actionIndex))
                    drawPtr = -1
                    return true
                }
                if (mode == MODE_GEST && e.pointerCount - 1 < 2) mode = MODE_IDLE
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (drawPtr >= 0 && drawPtr < e.pointerCount) {
                    if (mode != MODE_GEST) handleMove(e.getX(drawPtr), e.getY(drawPtr))
                    return true
                }
                if (e.pointerCount >= 2 && mode != MODE_GEST) {
                    liveStroke?.let { commitLiveStroke() }
                    if (mode == MODE_LASSO) lasso.clear()
                    startGesture(e)
                }
                if (mode == MODE_GEST) gestureMove(e)
                else handleMove(e.x, e.y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                handleUp(e.x, e.y)
                drawPtr = -1
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (liveStroke != null) commitLiveStroke()
                mode = MODE_IDLE
                erasedDirty = false
                drawPtr = -1
                return true
            }
        }
        return super.onTouchEvent(e)
    }

    /* —— palm rejection helpers —— */
    private var stylusSeen = false   // ¿Ya vimos un lápiz en esta sesión del editor?
    private var drawPtr = -1         // índice del puntero que dibuja
    private fun isStylus(e: MotionEvent, i: Int): Boolean {
        val t = e.getToolType(i)
        return t == MotionEvent.TOOL_TYPE_STYLUS || t == MotionEvent.TOOL_TYPE_ERASER
    }

    private fun startGesture(e: MotionEvent) {
        mode = MODE_GEST
        gestDist0 = spacing(e); gestScale0 = scale
        gestMid0 = midPt(e); gest0x = tx; gest0y = ty
    }

    private var gestDist0 = 1f
    private var gestScale0 = 1f
    private var gestMid0 = PointF()
    private var gest0x = 0f
    private var gest0y = 0f

    private fun spacing(e: MotionEvent): Float = hypot(
        (e.getX(0) - e.getX(1)).toDouble(),
        (e.getY(0) - e.getY(1)).toDouble()
    ).toFloat().coerceAtLeast(1f)

    private fun midPt(e: MotionEvent) = PointF((e.getX(0) + e.getX(1)) / 2f, (e.getY(0) + e.getY(1)) / 2f)

    private fun gestureMove(e: MotionEvent) {
        val d = spacing(e)
        var s2 = gestScale0 * (d / gestDist0)
        s2 = min(3.2f, max(fitScale() * 0.9f, s2))
        val m = midPt(e)
        // ancla: el punto que estaba bajo el medio inicial se mantiene
        tx = m.x - ((gestMid0.x - gest0x) / gestScale0) * s2
        ty = m.y - ((gestMid0.y - gest0y) / gestScale0) * s2
        scale = s2
        clampMat()
        invalidate()
    }

    private fun fitScale() = min(vmW / PAPER_W, vmH / PAPER_H).coerceAtLeast(0.2f)

    private fun clampMat() {
        val s = scale
        tx = tx.coerceIn(70f - PAPER_W * s, vmW - 70f)
        ty = ty.coerceIn(90f - PAPER_H * s, vmH - 90f)
    }

    /* —— límites del papel: TODO pasa por aquí —— */
    private fun insidePaper(p: Pt): Boolean =
        p.x >= 0f && p.y >= 0f && p.x <= PAPER_W && p.y <= PAPER_H
    private fun clampPaper(p: Pt) = Pt(p.x.coerceIn(0f, PAPER_W), p.y.coerceIn(0f, PAPER_H))

    /* ------- pulsaciones simples ------- */
    private fun handleDown(sx: Float, sy: Float, forceErase: Boolean = false) {
        val raw = Pt(paperX(sx), paperY(sy))
        // FUERA del papel no pasa NADA (el límite es el papel físico)
        if (!insidePaper(raw)) {
            mode = MODE_IDLE
            return
        }
        val p = clampPaper(raw)
        when (tool) {
            Tools.PEN, Tools.MARKER, Tools.PENCIL, Tools.HIGHLIGHTER, Tools.ERASER -> {
                if (forceErase) {                      // pUNTA trasera del stylus = goma
                    mode = MODE_ERASE; eraseAt(p); return
                }
                if (tool == Tools.ERASER) { mode = MODE_ERASE; eraseAt(p); return }
                mode = MODE_DRAW
                val w = when (tool) {
                    Tools.PEN -> penWidth
                    Tools.MARKER -> penWidth * 2.4f
                    Tools.PENCIL -> penWidth * 0.9f
                    else -> penWidth * 3.2f
                }
                val c = if (tool == Tools.PENCIL) blendColor(penColor, 0f) else penColor
                liveStroke = Stroke(Geo.newId(), c, w, when (tool.toBrushType()) {
                    Brush.PEN -> Brush.PEN
                    else -> tool.toBrushType()
                }, ArrayList())
                liveStroke?.points?.add(jitter(p, w))
                invalidate()
            }
            Tools.LASSO -> beginSelectionGesture(sx, p)
            Tools.SELECT -> beginSelectGesture(sx, p)
            Tools.TEXT -> host?.editText(textHit(p), p)
            Tools.STICKER -> {
                val s = stickerPending
                if (s == null) host?.openStickers()
                // queda EN MODO STICKER: tocar el papel cuantas veces se quiera;
                // se cambia/apaga manteniendo presionado el botón 🖊 del editor
                else addStickerAtPaper(s, p.x, p.y)
            }
            Tools.IMAGE -> host?.pickImage()
            Tools.LINK -> {
                val hit = linkHit(p)
                if (hit != null) host?.linkAt(hit, p) else host?.linkAt(null, p)
            }
            else -> {}
        }
    }

    private fun Int.toBrushType() = when (this) {
        Tools.PEN -> Brush.PEN
        Tools.MARKER -> Brush.MARKER
        Tools.PENCIL -> Brush.PENCIL
        else -> Brush.HIGHLIGHTER
    }

    private fun blendColor(c: Int, extra: Float) = c

    private fun jitter(p: Pt, w: Float): Pt {
        if (tool != Tools.PENCIL) return p
        val j = w * 0.34f
        p.x += Random.nextFloat() * j * 2 - j
        p.y += Random.nextFloat() * j * 2 - j
        return p
    }

    private fun handleMove(sx: Float, sy: Float) {
        // el trazo se DETIENE en el borde del papel: jamás trasciende al margen
        val p = clampPaper(Pt(paperX(sx), paperY(sy)))
        if (abs(sx - downPt.x) + abs(sy - downPt.y) > 12f) movedFar = true
        when (mode) {
            MODE_DRAW -> {
                val s = liveStroke ?: return
                val last = s.points.lastOrNull() ?: p
                if (abs(last.x - p.x) + abs(last.y - p.y) > 1.2f) {
                    s.points.add(jitter(p, s.width))
                    postInvalidateOnAnimation()
                }
            }
            MODE_ERASE -> eraseAt(p)
            MODE_LASSO -> { lasso.add(p); invalidate() }
            MODE_MOVESEL -> moveSelectionBy(p.x - prevPt.x, p.y - prevPt.y)
            MODE_SCALEEL -> scaleSelection(p)
        }
        prevPt = p
    }

    private var prevPt = Pt()

    private fun beginSelectionGesture(sx: Float, p: Pt) {
        prevPt = p
        val b = selectionBounds()
        if (!selection.isEmpty() && b != null && hitBounds(b, p.x, p.y, 26f)) {
            mode = MODE_MOVESEL
            return
        }
        if (!selection.isEmpty() && lastSelected != null && !isStrokeEl(lastSelected!!)) {
            val hb = lastSelected!!.bounds()
            val hx = hb.right - 16f; val hy = hb.bottom - 16f
            if (hypot((p.x - hx).toDouble(), (p.y - hy).toDouble()) < 60f) {
                mode = MODE_SCALEEL
                // save start size
                a = lastSelected!!
                e0 = elemDims(a!!)
                eStart = p
                scaleAnchor = RectF(hb.left, hb.top, hb.right, hb.bottom)
                return
            }
        }
        mode = MODE_LASSO
        lasso.clear(); lasso.add(p)
        invalidate()
    }

    /** CURSOR: toca un objeto (texto, línea, imagen, sticker, enlace) y muévelo con libertad. */
    private fun beginSelectGesture(sx: Float, p: Pt) {
        prevPt = p
        val b = selectionBounds()
        if (!selection.isEmpty() && b != null && hitBounds(b, p.x, p.y, 30f)) {
            mode = MODE_MOVESEL          // arrastrar lo ya seleccionado
            return
        }
        // tapping: seleccionar el objeto tocado (también grupos sueltos del dibujo)
        val hit = elementAt(p)
        selection.clear()
        if (hit != null) {
            selection.add(hit)
            lastSelected = hit
        }
        host?.onSelectionChanged(!selection.isEmpty())
        // si nada golpeado: abrir el lazo para marcar el área a mano
        mode = if (hit == null) MODE_LASSO else MODE_MOVESEL
        if (mode == MODE_LASSO) {
            lasso.clear(); lasso.add(p); invalidate()
        }
    }

    private var a: PaperElement? = null
    private var e0 = Pair(1f, 1f)
    private var eStart = Pt()

    private fun elemDims(el: PaperElement): Pair<Float, Float> = when (el) {
        is TextElement -> el.size to 0f
        is ImageElement -> el.w to el.h
        is StickerElement -> el.size to 0f
        is LinkElement -> el.scale to 0f
        else -> 1f to 1f
    }

    private fun isStrokeEl(el: PaperElement) = el is Stroke

    private fun moveSelectionBy(dx: Float, dy: Float) {
        for (el in selection) el.moveTo(dx, dy)
        invalidate()
    }

    private fun scaleSelection(p: Pt) {
        val el = a ?: return
        val d0 = hypot((eStart.x - scaleAnchor.left).toDouble(), (eStart.y - scaleAnchor.top).toDouble()).toFloat().coerceAtLeast(40f)
        val d1 = hypot((p.x - scaleAnchor.left).toDouble(), (p.y - scaleAnchor.top).toDouble()).toFloat().coerceAtLeast(40f)
        val f = d1 / d0
        val (dw, _) = e0
        when (el) {
            is TextElement -> { el.size = (dw * f).coerceIn(10f, 160f); el.invalidatePathCache() }
            is ImageElement -> { el.w = dw * f; el.h = e0.second * f }
            is StickerElement -> el.size = (dw * f).coerceIn(16f, 320f)
            is LinkElement -> el.scale = (dw * f).coerceIn(0.5f, 3f)
            else -> {}
        }
        invalidate()
    }

    /* ===== AGRANDAR / ACHICAR / GIRAR la selección (botones del popup del cursor) ===== */

    /** f > 1 agranda, f < 1 achica. Cada objeto escala alrededor de su PROPIO centro. */
    fun selectionScale(f0: Float) {
        if (selection.isEmpty()) return
        record()
        val f = f0.coerceIn(0.3f, 3f)
        for (el in selection) when (el) {
            is TextElement -> { el.size = (el.size * f).coerceIn(9f, 220f); el.invalidatePathCache() }
            is ImageElement -> {
                val nw = (el.w * f).coerceIn(30f, PAPER_W * 1.8f)
                el.h = nw / el.w * el.h     // mantiene la proporción exacta
                el.w = nw
            }
            is StickerElement -> el.size = (el.size * f).coerceIn(14f, 400f)
            is LinkElement -> el.scale = (el.scale * f).coerceIn(0.5f, 3f)
            is Stroke -> scaleStrokePoints(el, f)
        }
        invalidate()
    }

    private fun scaleStrokePoints(s: Stroke, f: Float) {
        val b = s.bounds()
        val cx = (b.left + b.right) / 2f
        val cy = (b.top + b.bottom) / 2f
        for (p in s.points) {
            p.x = cx + (p.x - cx) * f
            p.y = cy + (p.y - cy) * f
        }
        s.width = (s.width * f).coerceIn(1f, 120f)
        s.invalidatePathCache()
    }

    /** Girar grados (positivo = horario). Un objeto gira sobre sí mismo;
     *  varios giran en órbita alrededor del centro del grupo. Los trazos giran punto a punto. */
    fun selectionRotate(deg: Float) {
        if (selection.isEmpty()) return
        record()
        val b = selectionBounds() ?: return
        val gx = b.centerX(); val gy = b.centerY()
        val rad = Math.toRadians(deg.toDouble())
        val cs = cos(rad).toFloat(); val sn = sin(rad).toFloat()
        for (el in selection) {
            if (el is Stroke) {
                for (p in el.points) {
                    val dx = p.x - gx; val dy = p.y - gy
                    p.x = gx + dx * cs - dy * sn
                    p.y = gy + dx * sn + dy * cs
                }
                el.invalidatePathCache()
            } else {
                val eb = el.bounds()
                val ecx = (eb.left + eb.right) / 2f; val ecy = (eb.top + eb.bottom) / 2f
                val dx = ecx - gx; val dy = ecy - gy
                val nx = gx + dx * cs - dy * sn    // órbita del centro propio
                val ny = gy + dx * sn + dy * cs
                el.moveTo(nx - ecx, ny - ecy)
                el.rot = ((el.rot + deg) % 360f + 360f) % 360f
            }
        }
        invalidate()
    }

    private fun selectionBounds(): RectF? {
        if (selection.isEmpty()) return null
        var l = Float.MAX_VALUE; var t = Float.MAX_VALUE; var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
        for (el in selection) {
            val bb = el.bounds()
            l = min(l, bb.left); t = min(t, bb.top); r = max(r, bb.right); b = max(b, bb.bottom)
        }
        return RectF(l, t, r, b)
    }

    private fun hitBounds(b: AuxRectF, x: Float, y: Float, pad: Float): Boolean =
        x >= b.left - pad && x <= b.right + pad && y >= b.top - pad && y <= b.bottom + pad

    private fun hitBounds(b: RectF, x: Float, y: Float, pad: Float): Boolean =
        x >= b.left - pad && x <= b.right + pad && y >= b.top - pad && y <= b.bottom + pad

    private fun handleUp(sx: Float, sy: Float) {
        val p = Pt(paperX(sx), paperY(sy))
        when (mode) {
            MODE_DRAW -> commitLiveStroke()
            MODE_LASSO -> {
                val little = abs(sx - downPt.x) + abs(sy - downPt.y) < 14f
                if (little) {
                    val hit = elementAt(p)
                    selection.clear()
                    if (hit != null) { selection.add(hit); lastSelected = hit }
                    host?.onSelectionChanged(!selection.isEmpty())
                } else {
                    lassoSelect()
                }
                lasso.clear()
                invalidate()
            }
            MODE_MOVESEL -> host?.onSelectionChanged(!selection.isEmpty())
        }
        mode = MODE_IDLE
        movedFar = false
        erasedDirty = false   // termina el arrastrón de borrado: el próximo vuelve a grabar
    }

    private fun commitLiveStroke() {
        val s = liveStroke ?: return
        liveStroke = null
        if (s.points.isEmpty()) return
        if (s.points.size == 1) {
            val p0 = s.points[0]
            s.points.add(Pt(p0.x + 0.6f, p0.y + 0.6f))
        }
        record()
        pageItems.add(s)
        invalidate()
    }

    private fun commitLiveStrokeCancelled() {
        liveStroke = null
        invalidate()
    }

    private fun lassoSelect() {
        if (lasso.size < 3) return
        val nsel = ArrayList<PaperElement>()
        for (el in pageItems) {
            when (el) {
                is Stroke -> {
                    var inside = 0
                    for (q in el.points) if (Geo.insidePolygon(lasso, q.x, q.y)) inside++
                    if (inside >= 2 || inside * 2 > el.points.size) nsel.add(el)
                }
                else -> {
                    val b = el.bounds()
                    val cx = (b.left + b.right) / 2; val cy = (b.top + b.bottom) / 2
                    if (Geo.insidePolygon(lasso, cx, cy)) nsel.add(el)
                }
            }
        }
        selection.clear(); selection.addAll(nsel); lastSelected = nsel.lastOrNull()
        host?.onSelectionChanged(!nsel.isEmpty())
    }

    private fun elementAt(p: Pt): PaperElement? {
        for (i in pageItems.indices.reversed()) {
            val el = pageItems[i]
            val b = el.bounds()
            val pad = if (el is TextElement) 10f else 14f
            if (hitBounds(b, p.x, p.y, pad)) return el
        }
        return null
    }

    private fun textHit(p: Pt): TextElement? = elementAt(p) as? TextElement
    private fun linkHit(p: Pt): LinkElement? = elementAt(p) as? LinkElement

    /* ================== dibujo ================== */

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2.2f
        pathEffect = DashPathEffect(floatArrayOf(14f, 9f), 0f)
        color = 0xFF7A6A5A.toInt()
    }
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2.6f; color = 0xFFED64A6.toInt()
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFED64A6.toInt(); style = Paint.Style.FILL
    }

    fun strokePath(s: Stroke): Path {
        // BUG Corregido: el trazo EN VIVO muta sus puntos cada fotograma, así que
        // nunca debe usar (ni almacenar en) el caché: si se cacheara en el primer
        // draw (1 punto), quedaría invisible mientras escribes y solo aparecería
        // tras guardar y volver a entrar (el caché no se serializa).
        val live = (s === liveStroke)
        if (!live) s.pathCache?.let { return it }
        val pth = Path()
        if (s.points.isNotEmpty()) {
            val p0 = s.points[0]
            pth.moveTo(p0.x, p0.y)
            for (i in 1 until s.points.size) {
                val pm = s.points[i - 1]; val pi = s.points[i]
                pth.quadTo(pm.x, pm.y, (pm.x + pi.x) / 2f, (pm.y + pi.y) / 2f)
            }
        }
        if (!live) s.pathCache = pth
        return pth
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(0xFFE7E0D0.toInt())
        if (!isSetup) return
        canvas.save()
        toCanvasCoord(canvas)
        drawPaper(canvas)
        drawElements(canvas, pageItems, 1f, curPage.template)
        liveStroke?.let { drawStroke(canvas, it) }
        // lazo y selección en coords papel
        if (lasso.isNotEmpty()) {
            val lp = Path()
            lp.moveTo(lasso[0].x, lasso[0].y)
            for (q in lasso) lp.lineTo(q.x, q.y)
            dashPaint.color = 0xFF7A6A5A.toInt()
            canvas.drawPath(lp, dashPaint)
        }
        if (selection.isNotEmpty()) {
            val b = selectionBounds()!!
            canvas.drawRoundRect(b, 6f, 6f, selPaint)
            val lsel = lastSelected
            if (lsel != null && !isStrokeEl(lsel)) {
                val hb = lsel.bounds()
                canvas.drawCircle(hb.right - 12f, hb.bottom - 12f, 12f, handlePaint)
                canvas.drawCircle(hb.right - 12f, hb.bottom - 12f, 5f, paperPaint)
            }
        }
        canvas.restore()
    }

    private var isSetup = false

    /** Render de exportación: dibuja una página completa en un bitmap (no toca el lienzo vivo). */
    fun renderPageBitmap(pageIdx: Int, outWidth: Int): Bitmap {
        val page = pages[pageIdx.coerceIn(0, pages.size - 1)]
        val w = outWidth.coerceIn(200, 3200)
        val h = (w * PAPER_H / PAPER_W).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(0xFFE7E0D0.toInt())
        c.save()
        c.scale(w / PAPER_W, h / PAPER_H)
        drawPaper(c, page)
        drawElements(c, page.items, 1f, page.template)
        c.restore()
        return bmp
    }

    private fun toCanvasCoord(c: Canvas) {
        c.translate(tx, ty)
        c.scale(scale, scale)
    }

    private fun drawPaper(c: Canvas, page: KPage = curPage) {
        val r = 10f
        val dark = page.paper.coerceIn(0, PAPER_COLORS.size - 1) == 2
        shadowPaint.color = 0x36000000
        c.drawRoundRect(8f, 10f, PAPER_W + 8, PAPER_H + 12, r, r, shadowPaint)
        paperPaint.color = PAPER_COLORS[page.paper.coerceIn(0, PAPER_COLORS.size - 1)]
        c.drawRoundRect(0f, 0f, PAPER_W, PAPER_H, r, r, paperPaint)

        linePaint.style = Paint.Style.STROKE
        linePaint.color = if (dark) 0x33FFFFFF.toInt() else 0xFFC7C1B2.toInt()
        linePaint.strokeWidth = 2.1f
        when (page.template) {
            0 -> {
                // línea roja del margen
                linePaint.color = if (dark) 0x22FFFFFF.toInt() else 0xFFE0C4B8.toInt()
                c.drawLine(86f, 40f, 86f, PAPER_H - 40f, linePaint)
                linePaint.color = if (dark) 0x33FFFFFF.toInt() else 0xFFC7C1B2.toInt()
                var y = 130f
                while (y < PAPER_H - 46f) { c.drawLine(36f, y, PAPER_W - 36f, y, linePaint); y += 76f }
            }
            1 -> {
                var y = 60f
                while (y < PAPER_H) { c.drawLine(40f, y, PAPER_W - 40f, y, linePaint); y += 74f }
                var x = 40f
                while (x < PAPER_W) { c.drawLine(x, 40f, x, PAPER_H - 40f, linePaint); x += 74f }
            }
            2 -> {
                val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = linePaint.color; style = Paint.Style.FILL }
                var y = 110f
                while (y < PAPER_H - 60f) {
                    var x = 70f
                    while (x < PAPER_W - 40f) { c.drawCircle(x, y, 3f, dot); x += 80f }
                    y += 80f
                }
            }
            else -> {}
        }
        // adorno de esquinas (flores, frutitas, corazones, ... )
        if (page.decor in 1..13) Decors.draw(c, page.decor, PAPER_W, PAPER_H)
    }

    private fun drawElements(c: Canvas, items: List<PaperElement>, dscale: Float, tpl: Int) {
        for (el in items) drawElement(c, el, dscale, tpl)
    }

    private fun drawElement(c: Canvas, el: PaperElement, ds: Float, tpl: Int) {
        when (el) {
            is Stroke -> drawStroke(c, el, ds)
            is TextElement -> drawText(c, el, tpl)
            is ImageElement -> drawImage(c, el)
            is LinkElement -> drawLink(c, el)
            is StickerElement -> drawSticker(c, el)
        }
    }

    private fun drawStroke(c: Canvas, s: Stroke, ds: Float = 1f) {
        strokePaint.color = s.color
        strokePaint.strokeWidth = max(1.4f, s.width * ds)
        strokePaint.isDither = false
        when (s.brush) {
            Brush.HIGHLIGHTER -> { strokePaint.alpha = 100; strokePaint.strokeCap = Paint.Cap.ROUND }
            Brush.MARKER -> { strokePaint.alpha = 205 }
            Brush.PENCIL -> strokePaint.alpha = 244
            else -> strokePaint.alpha = 255
        }
        c.drawPath(strokePath(s), strokePaint)
        strokePaint.alpha = 255
    }

    private fun drawText(c: Canvas, el: TextElement, tpl: Int) {
        if (el.text.isBlank()) return
        var layout = el.pathCache
        if (layout == null || el.cacheTpl != tpl) {
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = el.color; textSize = el.size; isAntiAlias = true; isFakeBoldText = false
            }
            val wmax = min(TextElement.wrapWidth(el.size), PAPER_W - 130f).toInt().coerceAtLeast(60)
            // espaciado que hace caer CADA renglón sobre la línea del papel
            val want = lineSpacingOf(tpl)
            val fm = tp.fontMetrics
            val natural = fm.descent - fm.ascent
            val extra = (want - natural)
            layout = StaticLayout.Builder.obtain(el.text, 0, el.text.length, tp, wmax)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(extra, 1f)
                .setIncludePad(false)
                .build()
            el.pathCache = layout
            el.cacheTpl = tpl
        }
        c.save()
        // GIRO alrededor del centro visual del texto
        if (el.rot != 0f) {
            val top = if (el.baseY >= 0f) el.y - layout.getLineBaseline(0) else el.y
            c.rotate(el.rot, el.x + layout.width / 2f, top + layout.height / 2f)
        }
        if (el.baseY >= 0f) {
            // longitud EXACTA: la primera línea base del LAYOUT cae justo en el renglón (el.y),
            // sin importar la fuente de respaldo (emoji/acentos) — siempre igual de alineada
            c.translate(el.x, el.y - layout.getLineBaseline(0))
        } else c.translate(el.x, el.y)   // textos antiguos: como estaban
        layout.draw(c)
        c.restore()
    }

    private fun drawImage(c: Canvas, el: ImageElement) {
        var bmp = el.bitmapCache
        if (bmp == null && !el.failedLoad) {
            val f = java.io.File(el.path)
            if (f.exists()) {
                bmp = BitmapFactory.decodeFile(f.absolutePath)
                if (bmp != null) el.bitmapCache = bmp else el.failedLoad = true
            } else el.failedLoad = true
        }
        bmp?.let {
            c.save()
            // GIRO alrededor del centro de la foto
            if (el.rot != 0f) c.rotate(el.rot, el.x + el.w / 2f, el.y + el.h / 2f)
            c.drawBitmap(it, null, RectF(el.x, el.y, el.x + el.w, el.y + el.h), paperPaint)
            c.restore()
        }
    }

    private fun drawLink(c: Canvas, el: LinkElement) {
        val sc = el.scale.coerceIn(0.5f, 3f)
        val b = el.bounds()
        c.save()
        // GIRO alrededor del centro del chip
        if (el.rot != 0f) c.rotate(el.rot, b.left + b.width() / 2f, b.top + b.height() / 2f)
        c.translate(b.left, b.top)
        c.scale(sc, sc)
        // medidas base (sin escala) — el lienzo ya viene escalado
        val w0 = (78f + el.label.length * 16f).coerceAtLeast(170f)
        val h0 = 74f
        // estilo "chip" sutil: papel suave + acento del enlace + NOMBRE visible
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xE6FFFFFF.toInt(); style = Paint.Style.FILL
        }
        c.drawRoundRect(0f, 0f, w0, h0, 14f, 14f, p)
        val soft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = el.color; style = Paint.Style.FILL; alpha = 40
        }
        c.drawRoundRect(0f, 0f, w0, h0, 14f, 14f, soft)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = el.color; style = Paint.Style.STROKE; strokeWidth = 1.7f; alpha = 200
        }
        c.drawRoundRect(0f, 0f, w0, h0, 14f, 14f, stroke)
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF33304A.toInt(); textSize = 27f
        }
        val labelWidth = w0 - 84f
        val shown = android.text.TextUtils.ellipsize(el.label, tp, labelWidth, android.text.TextUtils.TruncateAt.END).toString()
        // icono del enlace bien centrado
        val ip = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 24f }
        c.drawText("🔗", 20f, h0 / 2f + 9f, ip)
        // NOMBRE (lo que el usuario escribió) siempre visible
        c.drawText(shown, 56f, h0 / 2f + 10f, tp)
        // subrayado con el acento (se ve como enlace real)
        val lw = tp.measureText(shown).coerceAtMost(labelWidth)
        val und = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = el.color; style = Paint.Style.STROKE; strokeWidth = 3f; alpha = 180
        }
        c.drawLine(56f, h0 - 12f, 56f + lw, h0 - 12f, und)
        c.restore()
    }

    private fun drawSticker(c: Canvas, el: StickerElement) {
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = el.size }
        c.save()
        // GIRO alrededor del centro del sticker
        if (el.rot != 0f) c.rotate(el.rot, el.x + el.size * 0.6f, el.y + el.size * 0.7f)
        c.drawText(el.emoji, el.x, el.y + el.size, tp)
        c.restore()
    }
}