package com.knotes.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.knotes.app.R
import com.knotes.app.canvas.CanvasPageView
import com.knotes.app.canvas.Decors
import com.knotes.app.canvas.Tools
import com.knotes.app.data.LinkElement
import com.knotes.app.data.Pt
import com.knotes.app.data.TextElement

/* ============================================================
   Paneles del editor: lápiz, goma, texto, enlace, stickers, páginas
   Todos en tema CLARO fijo → legibles en modo oscuro del sistema.
   ============================================================ */
object Panels {

    fun label(text: String, ctx: Context): TextView = TextView(ctx).apply {
        this.text = text
        this.textSize = 13f
        this.setTextColor(Color.parseColor("#6E5D66"))
        this.setPadding(0, 18, 0, 8)
    }

    private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    /* ---------- paleta AMPLIA + feedback al elegir ---------- */
    private val WIDE_PALETTE = intArrayOf(
        Color.parseColor("#1C1C22"), Color.parseColor("#2E2A33"), Color.parseColor("#55555F"),
        Color.parseColor("#8A8A93"), Color.parseColor("#C0BFC7"), Color.parseColor("#FFFFFF"),
        Color.parseColor("#E8494E"), Color.parseColor("#C4383E"), Color.parseColor("#EE7C32"),
        Color.parseColor("#F5A65E"), Color.parseColor("#E9C400"), Color.parseColor("#F2CE5E"),
        Color.parseColor("#8A6D2B"), Color.parseColor("#55B24B"), Color.parseColor("#8CC98F"),
        Color.parseColor("#5FA366"), Color.parseColor("#2FA7C9"), Color.parseColor("#8CE0F2"),
        Color.parseColor("#3956C9"), Color.parseColor("#4A90D9"), Color.parseColor("#7FB3E8"),
        Color.parseColor("#1B3A6B"), Color.parseColor("#9C2BC9"), Color.parseColor("#B58ECC"),
        Color.parseColor("#6E3FA3"), Color.parseColor("#E9619A"), Color.parseColor("#EE8CAC"),
        Color.parseColor("#F7C6D8"), Color.parseColor("#8A5A3A"), Color.parseColor("#B98A6A"),
        Color.parseColor("#6B4423"), Color.parseColor("#D4B48C"), Color.parseColor("#FCEAEE"),
        Color.parseColor("#E7E0D0"), Color.parseColor("#FBEEDA"), Color.parseColor("#C9C3B4")
    )

    private fun smallRing(ctx: Context, c: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(c)
            setStroke(2, Color.parseColor("#C9B8A8"))
        }

    /** Rejilla 8 columnas + «Tu color» (RGB) + animación al ELEGIR el color. */
    fun colorGrid(ctx: Context, initial: Int, onPick: (Int) -> Unit): View {
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        val preview = View(ctx)
        val hex = TextView(ctx).apply {
            text = "#%06X".format(initial and 0xFFFFFF)
            textSize = 12f; setTextColor(Color.parseColor("#9A9186")); setPadding(dp(ctx, 8), 0, 0, 0)
        }
        val prevRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(ctx, 4), 0, dp(ctx, 2))
            addView(View(ctx).apply { background = smallRing(ctx, initial) },
                LinearLayout.LayoutParams(dp(ctx, 20), dp(ctx, 20)))
            addView(TextView(ctx).apply {
                text = "Color elegido"; textSize = 12.5f; setTextColor(Color.parseColor("#6E5D66"))
                setPadding(dp(ctx, 8), 0, 0, 0)
            })
            addView(hex)
        }
        root.addView(prevRow)

        var selected: TextView? = null
        fun updatePreview(c: Int) {
            preview.background = smallRing(ctx, c)
            hex.text = "#%06X".format(c and 0xFFFFFF)
        }
        fun mark(cell: TextView?, c: Int) {
            selected?.background = ctx.getDrawable(R.drawable.bg_cell_white)
            selected = cell
            selected?.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0x00000000)
                setStroke(3, Gender.accent(ctx))
            }
            updatePreview(c)
            // la animación de que YA ELEGISTE ese color
            cell?.animate()?.scaleX(1.35f)?.scaleY(1.35f)?.setDuration(85)
                ?.withEndAction { cell?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(85)?.start() }
        }

        val grid = GridLayout(ctx).apply { columnCount = 8; setPadding(0, dp(ctx, 8), 0, 0) }
        for (c in WIDE_PALETTE) {
            val cell = TextView(ctx).apply {
                text = "●"; textSize = 20f; setTextColor(c)
                gravity = Gravity.CENTER
                background = ctx.getDrawable(R.drawable.bg_cell_white)
                setOnClickListener {
                    mark(this, c)
                    onPick(c)
                }
            }
            grid.addView(cell, GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED)
            ).also {
                it.width = dp(ctx, 30); it.height = dp(ctx, 30)
                it.setMargins(3, 6, 3, 6)
            })
            if (c == initial) selected = cell
        }
        // marca el elegido inicial SIN animación
        selected?.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(0x00000000)
            setStroke(3, Gender.accent(ctx))
        }
        root.addView(grid)

        /* color PERSONALIZADO */
        root.addView(label("🎨 Tu color (desliza)", ctx))
        var custom = initial
        val customPreview = View(ctx).apply { background = smallRing(ctx, initial) }
        val customRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        customRow.addView(customPreview, LinearLayout.LayoutParams(dp(ctx, 26), dp(ctx, 26)).also { it.setMargins(0, 0, dp(ctx, 10), 0) })
        val bars = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; weightSum = 3f }
        val names = arrayOf("R", "G", "B")
        for (i in 0 until 3) {
            val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
            row.addView(TextView(ctx).apply { text = names[i]; textSize = 12f; setTextColor(Color.parseColor("#6E5D66")) },
                LinearLayout.LayoutParams(dp(ctx, 22), -2).apply {})
            val sb = SeekBar(ctx).apply { max = 255; progress = when (i) { 0 -> Color.red(initial); 1 -> Color.green(initial); else -> Color.blue(initial) } }
            sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) {
                    val r = if (i == 0) p else Color.red(custom)
                    val g = if (i == 1) p else Color.green(custom)
                    val bl = if (i == 2) p else Color.blue(custom)
                    custom = Color.rgb(r, g, bl)
                    customPreview.background = smallRing(ctx, custom)
                    updatePreview(custom)
                    onPick(custom)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
            row.addView(sb, LinearLayout.LayoutParams(0, -2, 1f))
            bars.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        customRow.addView(bars, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(customRow, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = dp(ctx, 6) })
        return root
    }

    /* ---------- plumas: herramienta de escritura ---------- */
    fun penOptions(ctx: Context, canvas: CanvasPageView) {
        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 22), dp(ctx, 14), dp(ctx, 22), dp(ctx, 8))
        }
        // —— RUEDA DE COLORES (fuera del papel: vive aquí, en la configuración) ——
        val wheel = com.knotes.app.ui.ColorWheelView(ctx)
        inner.addView(wheel, LinearLayout.LayoutParams(dp(ctx, 118), dp(ctx, 118)).also {
            it.gravity = android.view.Gravity.CENTER_HORIZONTAL
        })

        // —— Color elegido: pelotita + HEX que cambian AL INSTANTE ——
        inner.addView(label("Color elegido", ctx))
        val dot = android.view.View(ctx)
        val hex = TextView(ctx).apply {
            textSize = 12f; setTextColor(Color.parseColor("#6E5D66"))
        }
        fun applyPicked(c: Int) {
            canvas.penColor = c
            dot.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(c); setStroke(2, Color.WHITE)
            }
            hex.text = "#%06X".format(c and 0xFFFFFF)
        }
        val swrow = LinearLayout(ctx).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        swrow.addView(dot, LinearLayout.LayoutParams(dp(ctx, 30), dp(ctx, 30)).also { it.setMargins(0, 0, dp(ctx, 10), 0) })
        swrow.addView(hex)
        inner.addView(swrow)
        applyPicked(canvas.penColor)          // estado actual
        wheel.onPick = { applyPicked(it) }    // en vivo al tocar la rueda ✨

        inner.addView(label("Tipo de trazo", ctx))
        val types = arrayOf("Bolígrafo  ✍️", "Rotulador  🖊", "Lápiz suave  ✏️", "Subrayador  🖍")
        val rg = RadioGroup(ctx)
        for (i in types.indices) {
            val rb = RadioButton(ctx).apply { text = types[i]; setTextColor(Color.parseColor("#3A3A42")) }
            rb.id = View.generateViewId()
            rg.addView(rb)
            if (canvas.tool == i) rb.isChecked = true
        }
        inner.addView(rg)
        inner.addView(label("Paleta rápida", ctx))
        inner.addView(colorGrid(ctx, canvas.penColor) { applyPicked(it) })
        inner.addView(label("Grosor del trazo", ctx))
        val slider = SeekBar(ctx).apply { max = 40; progress = canvas.penWidth.toInt().coerceIn(1, 40) }
        inner.addView(slider, LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 6, 0, 12) })

        val root = android.widget.ScrollView(ctx).apply { addView(inner) }
        Dialogs.light(ctx)
            .setTitle("✍️ Herramienta de escritura")
            .setView(root)
            .setPositiveButton("Listo") { _, _ -> }
            .create()
            .show()

        rg.setOnCheckedChangeListener { _, id ->
            val idx = rg.indexOfChild(rg.findViewById(id))
            canvas.tool = idx
            canvas.brushType = idx
        }
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, value: Int, user: Boolean) {
                canvas.penWidth = value.coerceAtLeast(1).toFloat()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    /* ---------- goma ---------- */
    fun eraserOptions(ctx: Context, canvas: CanvasPageView) {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 22), dp(ctx, 14), dp(ctx, 22), 0)
        }
        val cbWhole = CheckBox(ctx).apply {
            text = "Borrar trazos completos (si no: borra solo la zona que tocas)"
            setTextColor(Color.parseColor("#3A3A42"))
            isChecked = canvas.eraserWhole
        }
        root.addView(cbWhole)
        root.addView(label("Tamaño de la goma", ctx))
        val slider = SeekBar(ctx).apply { max = 60; progress = canvas.eraserRadius.toInt().coerceIn(1, 60) }
        root.addView(slider)
        var r = canvas.eraserRadius
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, value: Int, user: Boolean) { r = value.coerceAtLeast(6).toFloat() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        Dialogs.light(ctx)
            .setTitle("🧽 Goma")
            .setView(root)
            .setPositiveButton("Listo") { _, _ ->
                canvas.eraserWhole = cbWhole.isChecked
                canvas.eraserRadius = r
            }
            .show()
    }

    /* ---------- texto ---------- */
    fun textDialog(ctx: Context, canvas: CanvasPageView, existing: TextElement?, pt: Pt) {
        val target = existing
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 22), dp(ctx, 14), dp(ctx, 22), 0)
        }
        val input = EditText(ctx).apply {
            hint = "Escribe tu texto..."
            setTextColor(Color.parseColor("#2B2B2B")); setHintTextColor(Color.parseColor("#9A9288"))
            minLines = 2; maxLines = 6
            setSingleLine(false)
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            background = ctx.getDrawable(R.drawable.bg_cell_white)
            if (target != null) setText(target.text)
        }
        root.addView(input)
        root.addView(label("Color", ctx))
        var chosen = target?.color ?: canvas.penColor
        root.addView(colorGrid(ctx, chosen) { chosen = it })
        root.addView(label("Tamaño", ctx))
        var size = target?.size ?: 34f
        val slider = SeekBar(ctx).apply { max = 96; progress = size.toInt().coerceIn(12, 96) }
        root.addView(slider)
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) { size = p.coerceAtLeast(12).toFloat() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        Dialogs.light(ctx)
            .setTitle(if (target == null) "Añadir texto" else "Editar texto")
            .setView(root)
            .setPositiveButton("Guardar") { _, _ ->
                val txt = input.text.toString().trim()
                if (txt.isNotEmpty()) {
                    if (target != null) canvas.replaceTextElement(target, txt, size, chosen)
                    else canvas.addTextElement(txt, size, chosen, (pt.x - 40f).coerceAtLeast(60f), pt.y)
                } else if (target != null) canvas.deleteSelection()
            }
            .setNeutralButton(if (target == null) null else "Eliminar") { _, _ ->
                canvas.deleteElement(target!!)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /* ---------- enlace ---------- */
    fun linkDialog(ctx: Context, canvas: CanvasPageView, existing: LinkElement?, pt: Pt) {
        if (existing != null) {
            Dialogs.light(ctx)
                .setTitle("🔗 ${existing.label}")
                .setMessage(existing.url)
                .setPositiveButton("Abrir") { _, _ ->
                    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(existing.url))) }
                    catch (_: Exception) { Toast.makeText(ctx, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show() }
                }
                .setNeutralButton("Copiar URL") { _, _ ->
                    val cb = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("url", existing.url))
                    Toast.makeText(ctx, "URL copiada", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cerrar", null)
                .show()
            return
        }
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 22), dp(ctx, 14), dp(ctx, 22), 0)
        }
        val name = EditText(ctx).apply {
            hint = "Texto del enlace (ej. Mi web)"; setTextColor(Color.parseColor("#2B2B2B"))
            background = ctx.getDrawable(R.drawable.bg_cell_white)
            setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8))
        }
        val url = EditText(ctx).apply {
            hint = "https://..."; setText("https://"); setTextColor(Color.parseColor("#2B2B2B"))
            background = ctx.getDrawable(R.drawable.bg_cell_white)
            setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8))
        }
        root.addView(name); root.addView(url, LinearLayout.LayoutParams(-1, -2).also { it.topMargin = dp(ctx, 8) })
        Dialogs.light(ctx)
            .setTitle("🔗 Añadir enlace")
            .setView(root)
            .setPositiveButton("Añadir") { _, _ ->
                if (url.text.toString().startsWith("http")) {
                    canvas.addLinkElement(name.text.toString().ifBlank { "Enlace" }, url.text.toString(), pt.x - 60f, pt.y - 30f)
                    Toast.makeText(ctx, "Enlace añadido · tócalo para abrir", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(ctx, "La URL debe empezar por http(s)://", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /* ---------- stickers ---------- */
    fun stickerPicker(ctx: Context, canvas: CanvasPageView, onPickImage: (() -> Unit)? = null) {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), 0)
        }
        root.addView(TextView(ctx).apply {
            text = "Elige un sticker → luego toca el papel para colocarlo"
            setTextColor(Color.parseColor("#6E5D66")); setPadding(dp(ctx, 8), dp(ctx, 8), dp(ctx, 8), 0)
        })
        val tabsScroll = HorizontalScrollView(ctx).apply { scrollBarDefaultDelayBeforeFade = 0 }
        val tabsRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(ctx, 8), dp(ctx, 4), dp(ctx, 8), dp(ctx, 4)) }
        tabsScroll.addView(tabsRow)

        val scroll = android.widget.ScrollView(ctx).apply {
            scrollBarDefaultDelayBeforeFade = 0
            isVerticalScrollBarEnabled = true
        }

        val names = ArrayList<String>()
        val packs = ArrayList<List<String>>()
        names.add("Míos ✚"); packs.add(emptyList())           // se rellena al abrir
        Catalog.stickerPacks.forEach { (n, l) -> names.add(n); packs.add(l) }

        val dialog = Dialogs.light(ctx).setView(root).setPositiveButton("Listo", null).create()

        fun fill(idx: Int) {
            tabsRow.removeAllViews()
            names.forEachIndexed { i, name ->
                val chip = TextView(ctx).apply {
                    text = name; textSize = 13f
                    setTextColor(if (i == idx) Color.WHITE else Color.parseColor("#6E5D66"))
                    setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), dp(ctx, 8))
                    background = ctx.getDrawable(if (i == idx) Gender.chipStrongBg() else R.drawable.bg_chip_light)
                    setSingleLine(true)
                    setOnClickListener { fill(i) }
                }
                tabsRow.addView(chip, LinearLayout.LayoutParams(-2, -2).also { it.setMargins(4, 0, 4, 0) })
            }
            scroll.removeAllViews()
            val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 4), dp(ctx, 8), dp(ctx, 4), dp(ctx, 16)) }

            if (idx == 0) {
                // —— MÍOS: emojis guardados + agregar libre + cargar imagen como sticker —— //
                val input = EditText(ctx).apply {
                    hint = "Pega o escribe tu emoji (ej. 🐶) y toca AGREGAR"
                    setTextColor(Color.parseColor("#2B2B2B")); setHintTextColor(Color.parseColor("#9A9288"))
                    background = ctx.getDrawable(R.drawable.bg_cell_white)
                    setSingleLine(true)
                    setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
                }
                box.addView(input, LinearLayout.LayoutParams(-1, -2).also { it.setMargins(0, 0, 0, dp(ctx, 8)) })
                val rowBtns = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                fun chipBtn(t: String, bgInt: Int, act: () -> Unit): TextView = TextView(ctx).apply {
                    text = t; textSize = 13f; gravity = Gravity.CENTER
                    setPadding(dp(ctx, 14), dp(ctx, 9), dp(ctx, 14), dp(ctx, 9))
                    setTextColor(Color.WHITE)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 20f; setColor(bgInt)
                    }
                    setOnClickListener { act() }
                }
                rowBtns.addView(chipBtn("✚ AGREGAR", 0xFF3956C9.toInt()) {
                    val e = input.text.toString().trim()
                    if (e.isNotEmpty()) {
                        com.knotes.app.data.Prefs.addCustomSticker(e)
                        Toast.makeText(ctx, "Sticker guardado en «Míos» ✅", Toast.LENGTH_SHORT).show()
                        input.setText("")
                        fill(0)
                    } else Toast.makeText(ctx, "Escribe o pega un emoji primero", Toast.LENGTH_SHORT).show()
                }, LinearLayout.LayoutParams(-2, -2).also { it.setMargins(0, 0, dp(ctx, 8), 0) })
                rowBtns.addView(chipBtn("🖼 CARGAR IMAGEN (PNG/JPG)",
                    if (onPickImage != null) Gender.accentDeep(ctx) else 0xFFB0B0B8.toInt()) {
                    if (onPickImage == null) Toast.makeText(ctx, "Solo dentro del editor", Toast.LENGTH_SHORT).show()
                    else {
                        dialog.dismiss(); onPickImage.invoke()
                    }
                }, LinearLayout.LayoutParams(-2, -2))
                box.addView(rowBtns)

                box.addView(TextView(ctx).apply {
                    text = "Mis stickers guardados"
                    textSize = 12.5f; setTextColor(Color.parseColor("#6E5D66"))
                    setPadding(dp(ctx, 4), dp(ctx, 12), dp(ctx, 4), dp(ctx, 6))
                })
                val mine = com.knotes.app.data.Prefs.customStickers().toList()
                if (mine.isEmpty()) box.addView(TextView(ctx).apply {
                    text = "Aún no tienes: agrega emojis libres o carga una imagen ☝️"
                    textSize = 13f; setTextColor(Color.parseColor("#9A9288")); setPadding(dp(ctx, 4), dp(ctx, 2), dp(ctx, 4), dp(ctx, 10))
                })
                // filas de 4 celdas — SIEMPRE con el emoji a la vista
                val list = mine
                var r = 0
                while (r * 4 < list.size) {
                    val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                    for (c in 0 until 4) {
                        val i = r * 4 + c
                        val cell = TextView(ctx).apply {
                            gravity = Gravity.CENTER
                            background = ctx.getDrawable(R.drawable.bg_cell_white)
                            textSize = 30f
                            setTextColor(Color.parseColor("#2B2B2B"))
                            text = if (i < list.size) list[i] else ""
                            setOnClickListener {
                                if (i < list.size) {
                                    canvas.stickerPending = list[i]
                                    dialog.dismiss()
                                    Toast.makeText(ctx, "Toca el papel cuantas veces quieras ✓ · mantén presionado el botón 🖊 para cambiarlo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        row.addView(cell, LinearLayout.LayoutParams(0, dp(ctx, 64), 1f).also { it.setMargins(5, 5, 5, 5) })
                    }
                    box.addView(row)
                    r++
                }
            } else {
                val items = packs[idx]
                var r = 0
                while (r * 4 < items.size) {
                    val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                    for (c in 0 until 4) {
                        val i = r * 4 + c
                        val cell = TextView(ctx).apply {
                            gravity = Gravity.CENTER
                            background = ctx.getDrawable(R.drawable.bg_cell_white)
                            textSize = 30f
                            setTextColor(Color.parseColor("#2B2B2B"))
                            text = if (i < items.size) items[i] else ""
                            setOnClickListener {
                                if (i < items.size) {
                                    canvas.stickerPending = items[i]
                                    dialog.dismiss()
                                    Toast.makeText(ctx, "Toca el papel cuantas veces quieras ✓ · mantén presionado el botón 🖊 para cambiarlo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        row.addView(cell, LinearLayout.LayoutParams(0, dp(ctx, 64), 1f).also { it.setMargins(5, 5, 5, 5) })
                    }
                    box.addView(row)
                    r++
                }
            }
            scroll.addView(box)
        }

        root.addView(tabsScroll)
        root.addView(scroll, LinearLayout.LayoutParams(-1, dp(ctx, 330)))
        fill(0)
        dialog.show()
    }

    /* ---------- páginas ---------- */
    val TPL_NAMES = arrayOf("Rayado", "Cuadrícula", "Puntos", "En blanco")

    /** Selector combinado: tipo de página + adorno de esquinas. */
    fun pageStylePicker(ctx: Context, onPick: (tpl: Int, decor: Int) -> Unit,
                        title: String = "Estilo de página",
                        curTpl: Int = 0, curDecor: Int = 0) {
        var selTpl = curTpl
        var selDecor = curDecor
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 22), dp(ctx, 14), dp(ctx, 22), 0)
        }
        root.addView(label("Tipo de página", ctx))
        val tplRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val tplChips = arrayOfNulls<TextView>(4)
        for (i in 0 until 4) {
            val chip = TextView(ctx).apply {
                text = TPL_NAMES[i]; textSize = 13f
                setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), dp(ctx, 8))
                background = ctx.getDrawable(if (i == selTpl) Gender.chipStrongBg() else R.drawable.bg_chip_light)
                setTextColor(if (i == selTpl) Color.WHITE else Color.parseColor("#6E5D66"))
                setOnClickListener {
                    selTpl = i
                    for (j in 0 until 4) tplChips[j]!!.apply {
                        background = ctx.getDrawable(if (j == i) Gender.chipStrongBg() else R.drawable.bg_chip_light)
                        setTextColor(if (j == i) Color.WHITE else Color.parseColor("#6E5D66"))
                    }
                }
            }
            tplChips[i] = chip
            tplRow.addView(chip, LinearLayout.LayoutParams(-2, -2).also { it.setMargins(0, 0, 8, 0) })
        }
        root.addView(tplRow)
        root.addView(label("Adorno de esquinas 🌸🍓💕", ctx))
        val decorRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val decorChips = arrayOfNulls<TextView>(14)
        for (d in 0..13) {
            val chip = TextView(ctx).apply {
                text = if (d == 0) "Ninguna" else "${Decors.shortEmoji(d)} ${Decors.name(d).substringAfter(' ')}"
                textSize = 12.5f
                setPadding(dp(ctx, 12), dp(ctx, 7), dp(ctx, 12), dp(ctx, 7))
                background = ctx.getDrawable(if (d == selDecor) Gender.chipStrongBg() else R.drawable.bg_chip_light)
                setTextColor(if (d == selDecor) Color.WHITE else Color.parseColor("#6E5D66"))
                setOnClickListener {
                    selDecor = d
                    for (j in 0..13) decorChips[j]!!.apply {
                        background = ctx.getDrawable(if (j == d) Gender.chipStrongBg() else R.drawable.bg_chip_light)
                        setTextColor(if (j == d) Color.WHITE else Color.parseColor("#6E5D66"))
                    }
                }
            }
            decorChips[d] = chip
            decorRow.addView(chip, LinearLayout.LayoutParams(-2, -2).also { it.setMargins(0, 0, 6, 6) })
        }
        val decorScroll = HorizontalScrollView(ctx).apply { addView(decorRow); scrollBarDefaultDelayBeforeFade = 0 }
        root.addView(decorScroll)

        Dialogs.light(ctx)
            .setTitle(title)
            .setView(root)
            .setPositiveButton("Listo") { _, _ -> onPick(selTpl, selDecor) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun pagesOverview(ctx: Context, canvas: CanvasPageView) {
        val n = canvas.pages.size
        val names = arrayOfNulls<String>(n)
        for (i in 0 until n) {
            val d = canvas.pages[i].decor
            names[i] = "Página ${i + 1} — ${TPL_NAMES[canvas.pages[i].template.coerceIn(0, 3)]}" +
                (if (d in 1..13) " ${Decors.shortEmoji(d)}" else "")
        }
        Dialogs.light(ctx)
            .setTitle("Páginas ($n)")
            .setItems(names) { _, w -> canvas.goToPage(w) }
            .setPositiveButton("Nueva página") { _, _ ->
                pageStylePicker(ctx, { t, dec -> canvas.addPage(t, dec) }, "Nueva página")
            }
            .setNeutralButton("Cambiar estilo") { _, _ ->
                pageStylePicker(ctx, { t, dec ->
                    canvas.setTemplate(t); canvas.setPageDecor(dec)
                }, "Cambiar estilo", canvas.curPage.template, canvas.curPage.decor)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }
}
