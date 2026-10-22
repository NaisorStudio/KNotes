package com.knotes.app

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.knotes.app.canvas.CanvasHost
import com.knotes.app.canvas.CanvasPageView
import com.knotes.app.canvas.Tools
import com.knotes.app.data.LinkElement
import com.knotes.app.data.Notebook
import com.knotes.app.data.Pt
import com.knotes.app.data.Store
import com.knotes.app.data.TextElement
import com.knotes.app.io.ExportUtil
import com.knotes.app.ui.Panels
import com.knotes.app.ui.Dialogs
import com.knotes.app.ui.Gender

/**
 * Editor — papel interactivo.
 * Todo lo GUARDADO es editable/borrable a la vuelta: goma por zona o por trazo,
 * lazo para mover o borrar cualquier elemento (trazos, textos, stickers,
 * imágenes o enlaces). Además exporta TXT / PDF / Word / JPG / PNG.
 */
class EditorActivity : AppCompatActivity(), CanvasHost {

    private lateinit var canvas: CanvasPageView
    private lateinit var pageLabel: TextView
    private lateinit var popupSel: LinearLayout
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    private lateinit var toolRow: LinearLayout

    private var nb: Notebook? = null
    private var exportKind = 0 // 1 png, 2 jpg, 3 pdf, 4 txt, 5 docx

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val book = nb ?: return@registerForActivityResult
        if (uri == null) return@registerForActivityResult
        val triple = Store.importImage(this, book, uri)
        if (triple != null) {
            canvas.addImageElement(triple.first, triple.second, triple.third)
            Toast.makeText(this, "🖼 Imagen añadida en el centro del papel · muévela/escálala con el lazo", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "No se pudo cargar esa imagen 😕 (JPG, PNG, WEBP, HEIC, AVIF, BMP, GIF…)", Toast.LENGTH_LONG).show()
        }
    }

    /* destinaciones SAF para las exportaciones */
    private val exportCreate = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val book = nb ?: return@registerForActivityResult
        val u = uri ?: return@registerForActivityResult
        when (exportKind) {
            1 -> ExportUtil.writeImage(this, u, canvas.renderPageBitmap(canvas.pageIndex, 1400), true)
            2 -> ExportUtil.writeImage(this, u, canvas.renderPageBitmap(canvas.pageIndex, 1400), false)
            3 -> ExportUtil.writePdf(this, u, { i -> canvas.renderPageBitmap(i, 1190) }, canvas.pages.size)
            4 -> ExportUtil.writeTxt(this, u, ExportUtil.buildTxt(book, canvas.pages))
            else -> ExportUtil.writeBytes(this, u, ExportUtil.buildDocx(book, canvas.pages))
        }
    }

    private val TOOLS = arrayOf(
        Triple(Tools.SELECT, R.drawable.ic_cursor, "Cursor: toca un objeto y muévelo con libertad"),
        Triple(Tools.LASSO, R.drawable.ic_lasso, "Lazo / seleccionar zona"),
        Triple(Tools.PEN, R.drawable.ic_pen, "Escribir (bolígrafo / rotulador / lápiz / subrayador)"),
        Triple(Tools.ERASER, R.drawable.ic_eraser, "Goma"),
        Triple(Tools.TEXT, R.drawable.ic_text, "Texto"),
        Triple(Tools.IMAGE, R.drawable.ic_image, "Imagen"),
        Triple(Tools.LINK, R.drawable.ic_link, "Enlace"),
        Triple(Tools.STICKER, R.drawable.ic_sticker, "Stickers")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_KNotes_Editor)
        setContentView(R.layout.activity_editor)

        canvas = findViewById(R.id.canvas)
        canvas.host = this
        pageLabel = findViewById(R.id.page_label)
        popupSel = findViewById(R.id.selection_popup)
        btnUndo = findViewById(R.id.btn_undo)
        btnRedo = findViewById(R.id.btn_redo)
        toolRow = findViewById(R.id.tool_row)

        val id = intent.getStringExtra("id")
        val book: Notebook? = if (id != null) Store.byId(id) else null
        if (book == null) { finish(); return }
        nb = book
        canvas.setup(book, Store.loadPages(book), 0)

        // barra de herramientas compacta (cabe api 36 en móviles estrechos)
        for (t in TOOLS) {
            val b = ImageButton(this).apply {
                setImageResource(t.second)
                background = getDrawable(R.drawable.bg_circle_white)
                imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.text_chip))
                contentDescription = t.third
                layoutParams = LinearLayout.LayoutParams(dp(46), dp(46)).also { it.setMargins(dp(3), 0, dp(3), 0) }
                setPadding(dp(9), dp(9), dp(9), dp(9))
            }
            b.setOnClickListener {
                // si ya es una pluma activa, conserva el tipo (rotulador/lápiz/subrayador)
                if (t.first == Tools.PEN &&
                    canvas.tool in intArrayOf(Tools.PEN, Tools.MARKER, Tools.PENCIL, Tools.HIGHLIGHTER)) {
                    selectTool(canvas.tool, b)
                } else selectTool(t.first, b)
            }
            b.tag = t.first
            // MANTENER PRESIONADO el botón 🖊 de stickers → vuelve a abrir el selector
            if (t.first == Tools.STICKER) {
                b.setOnLongClickListener {
                    Panels.stickerPicker(this, canvas) { pickImage.launch("image/*") }
                    true
                }
            }
            toolRow.addView(b)
        }
        selectTool(Tools.PEN, null)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            saveAll(false)
            finish()
        }
        btnUndo.setOnClickListener { canvas.undo() }
        btnRedo.setOnClickListener { canvas.redo() }
        findViewById<ImageButton>(R.id.btn_save).setOnClickListener { saveAll(true) }
        // acentos del modo sexo en el editor (rosa ↔ azul)
        findViewById<ImageButton>(R.id.btn_save).imageTintList =
            android.content.res.ColorStateList.valueOf(Gender.accentDeep(this))
        findViewById<ImageButton>(R.id.btn_float_pen).background = getDrawable(Gender.circleBg())
        val pageTint = android.content.res.ColorStateList.valueOf(Gender.accentDeep(this))
        findViewById<ImageButton>(R.id.btn_prev_page).imageTintList = pageTint
        findViewById<ImageButton>(R.id.btn_next_page).imageTintList = pageTint
        findViewById<ImageButton>(R.id.btn_more).setOnClickListener { v -> moreMenu(v) }

        findViewById<ImageButton>(R.id.btn_prev_page).setOnClickListener { canvas.goToPage(canvas.pageIndex - 1) }
        findViewById<ImageButton>(R.id.btn_next_page).setOnClickListener { canvas.goToPage(canvas.pageIndex + 1) }

        val float = findViewById<ImageButton>(R.id.btn_float_pen)
        setupFloatPen(float)
        restoreFloatPos(float)

        findViewById<ImageButton>(R.id.sel_delete).setOnClickListener {
            canvas.deleteSelection(); popupSel.visibility = View.GONE
        }
        findViewById<ImageButton>(R.id.sel_close).setOnClickListener {
            canvas.clearSelection(); popupSel.visibility = View.GONE
        }
        // AGRANDAR / ACHICAR / GIRAR la selección a gusto
        findViewById<ImageButton>(R.id.sel_grow).setOnClickListener { canvas.selectionScale(1.15f) }
        findViewById<ImageButton>(R.id.sel_shrink).setOnClickListener { canvas.selectionScale(1f / 1.15f) }
        findViewById<ImageButton>(R.id.sel_rot_r).setOnClickListener { canvas.selectionRotate(15f) }
        findViewById<ImageButton>(R.id.sel_rot_l).setOnClickListener { canvas.selectionRotate(-15f) }

        setupTextBar()
    }

    /* ================== BARRA DE ESCRITURA INLINE ================== */

    private var textBarShown = false
    private var textTarget: com.knotes.app.data.TextElement? = null
    private var textPt = Pt(500f, 400f)

    private fun setupTextBar() {
        val bar = findViewById<View>(R.id.text_bar)
        val input = findViewById<android.widget.EditText>(R.id.text_input)
        // scroll interno: nunca pierdes de vista lo escrito arriba
        input.maxHeight = (150 * resources.displayMetrics.density).toInt()
        input.setHorizontallyScrolling(false)
        input.movementMethod = android.text.method.ScrollingMovementMethod()
        findViewById<ImageButton>(R.id.text_done).setOnClickListener {
            commitTextBar(input)
        }
        findViewById<ImageButton>(R.id.text_cancel).setOnClickListener {
            textBarShown = false
            bar.visibility = View.GONE
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(input.windowToken, 0)
        }
    }

    private fun commitTextBar(input: android.widget.EditText) {
        val txt = input.text.toString().trim()
        val bar = findViewById<View>(R.id.text_bar)
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(input.windowToken, 0)
        textBarShown = false
        bar.visibility = View.GONE
        if (txt.isNotEmpty()) {
            val target = textTarget
            if (target != null) canvas.replaceTextElement(target, txt, target.size, canvas.penColor)
            else canvas.addTextElement(txt, 34f, canvas.penColor, textPt.x, textPt.y)
            saveAll(false)
        } else if (textTarget != null) {
            canvas.deleteElement(textTarget!!)
        }
        textTarget = null
        input.setText("")
    }

    /** el host pide abrir el escritor: barra INLINE con las líneas del papel */
    private fun showTextBar(el: com.knotes.app.data.TextElement?, pt: Pt) {
        textTarget = el
        textPt = pt
        val bar = findViewById<View>(R.id.text_bar)
        val input = findViewById<TextView>(R.id.text_input) as android.widget.EditText
        input.setText(el?.text ?: "")
        textBarShown = true
        bar.visibility = View.VISIBLE
        input.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    /* ================== LÁPIZ FLOTANTE: mover + acceso rápido ================== */

    private fun setupFloatPen(float: ImageButton) {
        // Toque RÁPIDO → abre la configuración (rueda de color/tipo/grosor).
        // ARRASTRAR → SOLO mueve el botón (no habilita nada).
        // Mantener presionado → NO abre nada (así se pedía).
        var downX = 0f; var downY = 0f
        var tx = 0f; var ty = 0f
        var dragging = false
        var downAt = 0L
        val slop = 18 * resources.displayMetrics.density
        float.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    downAt = android.os.SystemClock.uptimeMillis()
                    downX = e.rawX; downY = e.rawY
                    tx = v.translationX; ty = v.translationY
                    dragging = false
                    v.isPressed = true
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (dragging || Math.hypot(dx.toDouble(), dy.toDouble()) > slop) {
                        dragging = true
                        v.isPressed = false
                        v.translationX = tx + dx
                        v.translationY = ty + dy
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    v.isPressed = false
                    if (dragging) {
                        val w = (v.rootView.width - v.width).coerceAtLeast(1)
                        val h = (v.rootView.height - v.height).coerceAtLeast(1)
                        com.knotes.app.data.Prefs.floatPenFx = (v.x / w).coerceIn(0f, 1f)
                        com.knotes.app.data.Prefs.floatPenFy = (v.y / h).coerceIn(0f, 1f)
                    } else if (android.os.SystemClock.uptimeMillis() - downAt < 350L) {
                        // solo un toque rápido habilita las herramientas
                        Panels.penOptions(this, canvas)
                    }
                    true
                }
                else -> { v.isPressed = false; dragging = false; true }
            }
        }
    }

    private fun restoreFloatPos(float: ImageButton) {
        val fx = com.knotes.app.data.Prefs.floatPenFx
        val fy = com.knotes.app.data.Prefs.floatPenFy
        if (fx >= 0f) {
            float.post {
                val w = (float.rootView.width - float.width).coerceAtLeast(1)
                val h = (float.rootView.height - float.height).coerceAtLeast(1)
                float.translationX = fx * w - float.left
                float.translationY = fy * h - float.top
            }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun selectTool(tool: Int, pressed: ImageButton?, silent: Boolean = false) {
        canvas.tool = tool
        // al pulsar los botones REALES: goma muestra opciones;
        // el lápiz abre SIEMPRE sus opciones (tipo, color, grosor) — es un solo botón
        if (pressed != null && tool == Tools.ERASER) Panels.eraserOptions(this, canvas)
        if (pressed != null && tool in intArrayOf(Tools.PEN, Tools.MARKER, Tools.PENCIL, Tools.HIGHLIGHTER)) {
            Panels.penOptions(this, canvas)
        }
        // stickers: toque corto en el botón → si hay sticker activo lo APAGA,
        // si no hay → abre el selector. (Mantener presionado SIEMPRE lo abre)
        if (pressed != null && tool == Tools.STICKER) {
            if (canvas.stickerPending != null) {
                canvas.stickerPending = null
                Toast.makeText(this, "Modo sticker apagado ✔ (los ya puestos siguen ahí)", Toast.LENGTH_SHORT).show()
            } else {
                Panels.stickerPicker(this, canvas) { pickImage.launch("image/*") }
            }
        }
        for (i in 0 until toolRow.childCount) {
            val c = toolRow.getChildAt(i) as ImageButton
            val tagged = (c.tag as Int)
            val active = tagged == tool ||
                (tagged == Tools.PEN && tool in intArrayOf(Tools.PEN, Tools.MARKER, Tools.PENCIL, Tools.HIGHLIGHTER))
            c.background = getDrawable(if (active) Gender.circleBg() else R.drawable.bg_circle_white)
        }
        canvas.clearSelection()
    }

    private fun export(book: Notebook, kind: Int) {
        exportKind = kind
        val safe = ExportUtil.sanit(book.title)
        val name = when (kind) {
            1 -> "KNotes_${safe}_p${canvas.pageIndex + 1}.png"
            2 -> "KNotes_${safe}_p${canvas.pageIndex + 1}.jpg"
            3 -> "KNotes_${safe}.pdf"
            4 -> "KNotes_${safe}.txt"
            else -> "KNotes_${safe}.docx"
        }
        exportCreate.launch(name)
    }

    private fun moreMenu(v: View) {
        val pm = PopupMenu(this, v)
        pm.menu.add("Nueva página…")
        pm.menu.add("Ver páginas")
        pm.menu.add("Cambiar tipo de página")
        pm.menu.add("Cambiar color del papel")
        val exportMenu = pm.menu.addSubMenu("Exportar…")
        exportMenu.add("📄 Página actual → PNG")
        exportMenu.add("🖼 Página actual → JPG")
        exportMenu.add("📕 Diario completo → PDF")
        exportMenu.add("📝 Diario completo → TXT")
        exportMenu.add("📘 Diario completo → Word (.docx)")
        pm.menu.add("Limpiar página")
        pm.menu.add("Eliminar esta página")
        pm.menu.add("Acerca del guardado editable")
        pm.setOnMenuItemClickListener { item ->
            val book = nb ?: return@setOnMenuItemClickListener true
            when (item.title?.toString()) {
                "Nueva página…" -> Panels.pageStylePicker(this, { t, dec -> canvas.addPage(t, dec) }, "Nueva página")
                "Ver páginas" -> Panels.pagesOverview(this, canvas)
                "Cambiar tipo de página" -> Panels.pageStylePicker(this, { t, dec ->
                    canvas.setTemplate(t); canvas.setPageDecor(dec)
                }, "Cambiar tipo de página", canvas.curPage.template, canvas.curPage.decor)
                "Cambiar color del papel" -> showPaperOptions()
                "📄 Página actual → PNG" -> export(book, 1)
                "🖼 Página actual → JPG" -> export(book, 2)
                "📕 Diario completo → PDF" -> export(book, 3)
                "📝 Diario completo → TXT" -> export(book, 4)
                "📘 Diario completo → Word (.docx)" -> export(book, 5)
                "Limpiar página" -> confirm("¿Vaciar la página?", "Se borrará todo su contenido (puedes deshacer).") { canvas.clearPage() }
                "Eliminar esta página" -> if (canvas.pages.size > 1) {
                    confirm("¿Eliminar la página?", "Puedes deshacer tras borrar.") { canvas.deleteCurrentPage() }
                } else Toast.makeText(this, "Debe existir al menos una página", Toast.LENGTH_SHORT).show()
                "Acerca del guardado editable" -> aboutEdits()
                else -> aboutEdits()
            }
            true
        }
        pm.show()
    }

    private fun aboutEdits() {
        Dialogs.light(this)
            .setTitle("Trazos siempre editables")
            .setMessage("KNotes guarda trazos, texto, imágenes, stickers y enlaces como objetos vectoriales en JSON. Al salir y volver, la goma y el lazo siguen funcionando: borra por zona o trazo completo, mueve, escala o elimina lo que quieras.")
            .setPositiveButton("OK", null).show()
    }

    private fun showPaperOptions() {
        Dialogs.light(this)
            .setTitle("Color del papel (esta página)")
            .setItems(arrayOf("Crema", "Blanco", "Oscuro", "Rosa 🌸", "Menta 🍃", "Lavanda 🌷", "Celeste 🧊")) { _, w ->
                canvas.setPaperColor(w)
            }
            .show()
    }

    private fun confirm(title: String, message: String, action: () -> Unit) {
        Dialogs.light(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Sí") { _, _ -> action() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /* ================== guardar ================== */

    private fun saveAll(toast: Boolean) {
        val book = nb ?: return
        val pages = canvas.pages
        Store.savePages(book, pages)
        Store.saveMeta(book)
        if (toast) Toast.makeText(this, "Guardado 💾", Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        saveAll(false)
    }

    /* ================== CanvasHost ================== */

    override fun onPageChanged(idx: Int, count: Int) {
        pageLabel.text = "${idx + 1}/$count"
        val ind = findViewById<View>(R.id.page_indicator)
        ind.visibility = if (count > 1) View.VISIBLE else View.GONE
    }

    override fun onHistoryChanged(canUndo: Boolean, canRedo: Boolean) {
        btnUndo.imageTintList = android.content.res.ColorStateList.valueOf(getColor(if (canUndo) R.color.text_chip else R.color.paper_margin))
        btnRedo.imageTintList = android.content.res.ColorStateList.valueOf(getColor(if (canRedo) R.color.text_chip else R.color.paper_margin))
    }

    override fun onSelectionChanged(hasSel: Boolean) {
        popupSel.visibility = if (canvas.selectedCount() > 0) View.VISIBLE else View.GONE
    }

    override fun editText(el: TextElement?, pt: Pt) {
        showTextBar(el, pt)   // escritura INLINE: sigue las líneas del papel
    }

    override fun pickImage() {
        pickImage.launch("image/*")
    }

    override fun linkAt(el: LinkElement?, pt: Pt) {
        Panels.linkDialog(this, canvas, el, pt)
    }

    override fun openStickers() {
        // stickers propios: emojis guardados + CARGAR imagen como sticker
        Panels.stickerPicker(this, canvas) { pickImage.launch("image/*") }
    }

    override fun saveNow() {
        saveAll(true)
    }
}
