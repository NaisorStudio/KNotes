package com.knotes.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.knotes.app.R
import com.knotes.app.data.Ids
import com.knotes.app.data.KPage
import com.knotes.app.data.Store
import com.knotes.app.canvas.CanvasPageView
import com.knotes.app.canvas.Decors
import com.knotes.app.EditorActivity

/* ============================================================
   TemplatesLibrary — la BRÚJULA del raíl
   Plantillas con VISTA PREVIA + adorno en las esquinas del papel
   (flores, fresitas, corazones...). «Descargar» crea el diario.
   Importar archivo (.json de plantilla/respaldo).
   ============================================================ */

class TemplatePreviewView(context: Context) : View(context) {

    var tpl = 0
    var paperIdx = 0
    var decorIdx = 0

    private val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadowPaint = Paint().apply { color = 0x28000000; style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        // papel
        paperPaint.color = CanvasPageView.PAPER_COLORS[paperIdx.coerceIn(0, CanvasPageView.PAPER_COLORS.size - 1)]
        c.drawRoundRect(3f, 5f, w + 1f, h + 3f, 10f, 10f, shadowPaint)
        c.drawRoundRect(0f, 0f, w, h, 10f, 10f, paperPaint)
        // dibuja en unidades de papel (igual que en el editor) escaladas a la miniatura
        c.save()
        c.scale(w / CanvasPageView.PAPER_W, h / CanvasPageView.PAPER_H)
        val dark = paperIdx == 2
        linePaint.color = if (dark) 0x33FFFFFF.toInt() else 0x44000000
        dotPaint.color = if (dark) 0x44FFFFFF.toInt() else 0x55000000
        when (tpl) {
            0 -> {
                val margin = Paint(linePaint).apply { color = if (dark) 0x22FFFFFF.toInt() else 0x55EE5F8B.toInt() }
                c.drawLine(86f, 40f, 86f, CanvasPageView.PAPER_H - 40f, margin)
                var y = 130f
                while (y < CanvasPageView.PAPER_H - 46f) {
                    c.drawLine(60f, y, CanvasPageView.PAPER_W - 60f, y, linePaint); y += 76f
                }
            }
            1 -> {
                var y = 60f
                while (y < CanvasPageView.PAPER_H) { c.drawLine(40f, y, CanvasPageView.PAPER_W - 40f, y, linePaint); y += 74f }
                var x = 40f
                while (x < CanvasPageView.PAPER_W) { c.drawLine(x, 40f, x, CanvasPageView.PAPER_H - 40f, linePaint); x += 74f }
            }
            2 -> {
                var y = 110f
                while (y < CanvasPageView.PAPER_H - 60f) {
                    var x = 70f
                    while (x < CanvasPageView.PAPER_W - 40f) { c.drawCircle(x, y, 8f, dotPaint); x += 80f }
                    y += 80f
                }
            }
            else -> {}
        }
        if (decorIdx in 1..13) Decors.draw(c, decorIdx, CanvasPageView.PAPER_W, CanvasPageView.PAPER_H)
        c.restore()
    }
}

object TemplatesLibrary {

    /** (tipo de página, papel, adorno) por cada página; man=true ⇒ se ve en modo HOMBRE */
    data class Tpl(
        val emoji: String,
        val name: String,
        val desc: String,
        val pages: List<Triple<Int, Int, Int>>,
        val man: Boolean = false
    )

    val TEMPLATES = listOf(
        Tpl("🌸", "Diario floral", "Rayado rosa con florecitas en las esquinas",
            listOf(Triple(0, 3, 1), Triple(0, 3, 1))),
        Tpl("💌", "Cartas de amor", "Blanco con corazones… escribe lo que sientes",
            listOf(Triple(0, 1, 3), Triple(3, 1, 3))),
        Tpl("🧸", "Osito abracadabras", "Crema con osito tierno: lo que abriga el corazón",
            listOf(Triple(0, 0, 13), Triple(0, 0, 13))),
        Tpl("🌙", "Diario nocturno", "Página oscura con luna y estrellitas",
            listOf(Triple(0, 2, 4), Triple(3, 2, 4))),
        Tpl("🍓", "Recetario fresita", "Blanco con fresitas: ingredientes y preparación",
            listOf(Triple(0, 1, 2), Triple(0, 1, 2))),
        Tpl("☕", "Cafecito matinal", "Crema con cafecito: cómo amaneció tu día",
            listOf(Triple(0, 0, 6))),
        Tpl("🧁", "Pastelería dulce", "Con pastelitos: recetas, antojos, festejos",
            listOf(Triple(0, 1, 7), Triple(3, 1, 7))),
        Tpl("🔫", "Bullet journal tierno", "Puntos dorados con estrellitas",
            listOf(Triple(2, 0, 11), Triple(2, 0, 11))),
        Tpl("🌈", "Semana arcoíris", "Cuadrícula con arcoíris: un color por día",
            listOf(Triple(1, 1, 5), Triple(1, 1, 5))),
        Tpl("🌻", "Girasoles", "Rayado con girasoles: lo bueno que pasó hoy",
            listOf(Triple(0, 0, 8))),
        Tpl("📚", "Estudio lavanda", "Cuadrícula lavanda, apuntes tiernos",
            listOf(Triple(1, 5, 9), Triple(1, 5, 9))),
        Tpl("✏️", "Clase menta", "Rayado menta con hojitas fresquitas",
            listOf(Triple(0, 4, 9), Triple(0, 4, 9))),
        Tpl("🦋", "Mariposas", "Blanco con mariposas: evoluciona conmigo",
            listOf(Triple(3, 1, 10), Triple(0, 1, 10))),
        Tpl("🐾", "Mascotas", "Crema con huellitas: salidas y ronroneos",
            listOf(Triple(0, 0, 12), Triple(0, 0, 12))),
        Tpl("♥", "Metas del corazón", "Puntos lavanda con estrellitas y corazones",
            listOf(Triple(2, 5, 3), Triple(0, 5, 11))),
        Tpl("✈️", "Diario de viaje", "Celeste con mariposas: tickets y recuerdos",
            listOf(Triple(0, 6, 10), Triple(0, 6, 10))),
        Tpl("🏋️", "Rutina de fuerza", "Cuadrícula con estrellitas por meta",
            listOf(Triple(1, 1, 11), Triple(1, 1, 11))),
        Tpl("📖", "Reseñas de libros", "Rayado crema con hojitas",
            listOf(Triple(0, 0, 9))),
        Tpl("🎂", "Cumpleaños", "Blanco con pastelitos: planes para festejar",
            listOf(Triple(3, 1, 7))),
        Tpl("🌷", "Semana floral", "Cuadrícula rosa con flores dobles",
            listOf(Triple(1, 3, 1), Triple(1, 3, 1))),
        Tpl("🎵", "Canciones", "Rayado lavanda con estrellitas para tus letras",
            listOf(Triple(0, 5, 11), Triple(0, 5, 11))),
        Tpl("👶", "Diario del bebé", "Menta con osito: primeros días",
            listOf(Triple(0, 4, 13), Triple(0, 4, 13))),
        Tpl("💰", "Gastos del mes", "Cuadrícula crema con granitos de café",
            listOf(Triple(1, 0, 6))),
        Tpl("📦", "Inventario de casa", "Cuadrícula blanca con huellitas",
            listOf(Triple(1, 1, 12)))
    )

    /* Plantillas para el modo HOMBRE 👨 — se ven en su biblioteca */
    val MAN_TEMPLATES = listOf(
        Tpl("🏋️", "Gym de fuerza", "Cuadrícula con estrellitas por serie",
            listOf(Triple(1, 1, 11), Triple(1, 1, 11)), man = true),
        Tpl("⚽", "Fútbol dominical", "Celeste con alineación y goles",
            listOf(Triple(0, 6, 11), Triple(3, 6, 11)), man = true),
        Tpl("🚀", "Reto 30 días", "Puntos sobre oscuro para disciplina",
            listOf(Triple(2, 2, 11), Triple(2, 2, 11)), man = true),
        Tpl("🎮", "Backlog gaming", "Cuadrícula crema para juegos y logros",
            listOf(Triple(1, 0, 11), Triple(1, 0, 11)), man = true),
        Tpl("📈", "Metas del año", "Cuadrícula con estrellitas por objetivo",
            listOf(Triple(1, 1, 11), Triple(0, 1, 11)), man = true),
        Tpl("🛠", "Garaje", "Rayado blanco con hojitas de taller",
            listOf(Triple(0, 1, 9), Triple(3, 1, 9)), man = true),
        Tpl("🍔", "Asados & grill", "Crema con cafecito para recetas de fuego",
            listOf(Triple(0, 0, 6), Triple(0, 0, 6)), man = true),
        Tpl("📚", "Study duro", "Cuadrícula lavanda para apuntes polyle",
            listOf(Triple(1, 5, 11), Triple(1, 5, 11)), man = true),
        Tpl("💼", "Trabajo & umbrales", "Rayado blanco para minutas",
            listOf(Triple(0, 1, 9), Triple(0, 1, 9)), man = true),
        Tpl("✈️", "Viaje mochilero", "Celeste para rutas, tickets y historias",
            listOf(Triple(0, 6, 10), Triple(0, 6, 10)), man = true)
    )

    fun visibleTemplates(): List<Tpl> =
        (TEMPLATES + MAN_TEMPLATES).filter { Gender.isMan == it.man }

    private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    /** Abre la galería; el callback conecta el buscador de archivos de MainActivity. */
    fun open(ctx: Context, onImportFile: () -> Unit) {
        val scroll = ScrollView(ctx)
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), dp(ctx, 10))
        }

        /* fila de importar archivo */
        val import = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 12), dp(ctx, 12), dp(ctx, 12), dp(ctx, 12))
            background = ctx.getDrawable(R.drawable.bg_chip_light)
            addView(TextView(ctx).apply { text = "📥"; textSize = 22f; setPadding(0, 0, dp(ctx, 10), 0) })
            addView(TextView(ctx).apply {
                text = "Importar archivo de plantilla/respaldo (.json)\nSe incorpora a tus Diarios"
                textSize = 13f; setTextColor(0xFF5A4A52.toInt())
            })
            setOnClickListener { onImportFile() }
        }
        root.addView(import, LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = dp(ctx, 10) })

        for (t in visibleTemplates()) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(ctx, 8), dp(ctx, 8), dp(ctx, 8), dp(ctx, 8))
            }
            val pv = TemplatePreviewView(ctx).apply {
                tpl = t.pages[0].first
                paperIdx = t.pages[0].second
                decorIdx = t.pages[0].third
            }
            row.addView(pv, LinearLayout.LayoutParams(dp(ctx, 66), dp(ctx, 92)))

            val mid = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(ctx, 12), 0, dp(ctx, 8), 0)
                addView(TextView(ctx).apply {
                    text = "${t.emoji} ${t.name}"
                    textSize = 15f; setTextColor(0xFF38322F.toInt())
                })
                addView(TextView(ctx).apply {
                    text = "${t.desc} · ${t.pages.size} ${if (t.pages.size == 1) "página" else "páginas"}"
                    textSize = 12f; setTextColor(0xFF948A80.toInt())
                })
            }
            row.addView(mid, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val btn = TextView(ctx).apply {
                text = "Descargar"; textSize = 13f
                setTextColor(0xFFFFFFFF.toInt())
                gravity = Gravity.CENTER
                background = ctx.getDrawable(R.drawable.bg_pill_use)
                setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), dp(ctx, 8))
            }
            btn.setOnClickListener { download(ctx, t) }
            row.addView(btn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT))
            root.addView(row, LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = dp(ctx, 4) })
        }

        scroll.addView(root)
        Dialogs.light(ctx)
            .setTitle(if (Gender.isMan) "🧭 Biblioteca de Hombre 👨" else "🧭 Biblioteca de plantillas")
            .setView(scroll)
            .setNegativeButton("Cerrar", null)
            .show()
    }

    /** «Descargar» = incorporar la plantilla como un diario nuevo. */
    fun download(ctx: Context, t: Tpl) {
        val style = (t.name.hashCode() % 12).let { if (it < 0) it + 12 else it }
        val pages: MutableList<KPage> = ArrayList()
        for ((tplIdx, paperIdx, decorIdx) in t.pages) {
            pages.add(KPage(Ids.next(), tplIdx.coerceIn(0, 3), paperIdx.coerceIn(0, 6), decorIdx.coerceIn(0, 13)))
        }
        val nb = Store.createNotebook(t.name, style, java.util.ArrayList(), t.emoji.hashCode().toLong(), pages)
        Toast.makeText(ctx, "✅ «${t.name}» añadida a tus Diarios", Toast.LENGTH_SHORT).show()
        if (ctx is android.app.Activity) {
            ctx.startActivity(Intent(ctx, EditorActivity::class.java).putExtra("id", nb.id))
        }
    }
}
