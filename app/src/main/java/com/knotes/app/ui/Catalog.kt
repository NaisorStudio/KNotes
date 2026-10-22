package com.knotes.app.ui

import com.knotes.app.data.KPage

/* ============================================================
   KNotes — catálogo de contenido local
   Todo el contenido es generativo/original: los «templates» son
   configuraciones de páginas y los stickers son emojis.
   ============================================================ */

class FeedCard(
    val title: String,
    val emojiArt: String,          // arte visual del card
    val bg: Int,                   // color de fondo del card (argb)
    val badge: Boolean = false,
    val description: String = "",
    val build: (() -> MutableList<KPage>)? = null,
    val tpl: Int = 0
)

class FeedSection(
    val title: String,
    val more: Boolean = true,
    val items: List<FeedCard>
)

object Catalog {

    /* --- packs de stickers (emojis) que también usa el editor --- */
    val stickerPacks = listOf(
        "Flores" to listOf("🌸", "🌷", "🌹", "🌺", "🌻", "🌼", "💐", "🪷", "🍀", "❀"),
        "Plantas" to listOf("🌵", "🪴", "🌿", "🍃", "🌱", "🌳", "🌴", "🫶", "🪹", "🪺"),
        "Cute" to listOf("🧸", "☕", "🍰", "🧁", "🍩", "🦄", "🐰", "🐱", "🐻", "🐼"),
        "Símbolos" to listOf("⭐", "✨", "🌙", "❤️", "🔥", "🎉", "📌", "✅", "➡️", "❓"),
        "Comida" to listOf("🍎", "🍕", "🥑", "🍩", "🍜", "🧋", "🥨", "🍫", "🥞", "🍯")
    )

    /* adorno según el tipo de página para que cada plantilla tenga su toque tierno */
    private val DECOR_FOR = mapOf(
        0 to intArrayOf(1, 3, 8, 13),  // rayado: flores, corazones, girasoles, osito
        1 to intArrayOf(11, 9, 2, 12), // cuadrícula: estrellitas, hojitas, fresas, huellitas
        2 to intArrayOf(3, 10, 1, 5),  // puntos: corazones, mariposas, flores, arcoíris
        3 to intArrayOf(10, 7, 4, 6)   // en blanco: mariposas, pastelito, luna, café
    )

    private fun planner(name: String, template: Int, pages: Int): () -> MutableList<KPage> = {
        val out = ArrayList<KPage>()
        val decor = DECOR_FOR[template.coerceIn(0, 3)]!![kotlin.math.abs(name.hashCode()) % 4]
        out.add(KPage("t${System.nanoTime()}", template, 0, decor))
        (1 until pages).forEach { i -> out.add(KPage("t${System.nanoTime()}_$i", template, 0, decor)) }
        out
    }

    /* ---------- Descubrir ---------- */
    val discoverTabs = listOf("inicio", "Templates", "Stickers", "Book Journal", "Sample Journals")

    val discoverSections: Map<String, List<FeedSection>> = mapOf(
        "inicio" to listOf(
            FeedSection("Templates", items = templateCards() + plannerCards()),
            FeedSection("Stickers", items = stickerCards()),
            FeedSection("Book Journal", items = bookCards()),
            FeedSection("Sample Journals", items = sampleCards())
        ),
        "Templates" to listOf(FeedSection("Todas las plantillas", more = false, items = templateCards() + plannerCards())),
        "Stickers" to listOf(FeedSection("Packs de stickers", more = false, items = stickerCards())),
        "Book Journal" to listOf(FeedSection("Book Journal", more = false, items = bookCards())),
        "Sample Journals" to listOf(FeedSection("Ejemplos", more = false, items = sampleCards()))
    )

    private fun templateCards() = listOf(
        FeedCard("All-in-One Planner", "🗓 📆 ☀︎", 0xFFE4E9F7.toInt(), build = planner("All-in-One Planner", 1, 6)),
        FeedCard("Daily organizer", "⏰ % ─ ☐", 0xFFFDF2F8.toInt(), build = planner("Daily organizer", 0, 4)),
        FeedCard("Weekly organizer", "▦ ▤ ▥", 0xFFEAF7F1.toInt(), build = planner("Weekly organizer", 1, 8)),
        FeedCard("To-Do listas", "☐ ✔ ☐", 0xFFFFF3E0.toInt(), build = planner("To-Do listas", 0, 5)),
        FeedCard("Habit tracker", "🟩 ⬜ ✗", 0xFFF1F8E9.toInt(), build = planner("Habit tracker", 2, 4)),
        FeedCard("Notes claras", "✍ ︎", 0xFFFBF7ED.toInt(), build = planner("Notes claras", 0, 3))
    )

    private fun stickerCards() = stickerPacks.map { (name, items) ->
        FeedCard(name, items.joinToString("") { it }, 0xFFFFFF, badge = false)
    }

    private fun bookCards() = listOf(
        FeedCard("Diario clásico", "📕", 0xFFE8EAF6.toInt(), build = planner("Diario clásico", 0, 10)),
        FeedCard("Cuaderno doble", "📗", 0xFFFDEFF4.toInt(), build = planner("Cuaderno doble", 1, 10)),
        FeedCard("Spiral rosa", "🧷", 0xFFFDEFF4.toInt(), build = planner("Spiral rosa", 1, 8)),
        FeedCard("Papel azul", "📘", 0xFFE3F2FD.toInt(), build = planner("Papel azul", 0, 10)),
        FeedCard("Páginas en blanco", "⬜", 0xFFF5F5F5.toInt(), build = planner("Páginas en blanco", 3, 6))
    )

    private fun sampleCards() = listOf(
        FeedCard("Mi primer diario", "太郎 🌷", 0xFFFDF6EC.toInt(), build = planner("Mi primer diario", 0, 4)),
        FeedCard("Work Flow", "💼", 0xFFF0F4FF.toInt(), build = planner("Work Flow", 1, 6)),
        FeedCard("Recetario", "🥞 🧋", 0xFFFFF8E1.toInt(), build = planner("Recetario", 0, 6)),
        FeedCard("Ejercicios ⛹", "🏀", 0xFFE8F5E9.toInt(), build = planner("Ejercicios", 2, 4))
    )

    /* ---------- Mercado ---------- */
    val marketTabs = listOf("inicio", "Diarios de muestra", "Portadas", "Planners con hipervínculos", "Plantillas")

    val marketSections: Map<String, List<FeedSection>> = mapOf(
        "inicio" to listOf(
            FeedSection("All-in-one Digital Planner", items = plannerCards()),
            FeedSection("Monthly Overview", items = monthlyCards()),
            FeedSection("Weekly Planners", items = weeklyCards()),
            FeedSection("Habit Tracker", items = habitCards())
        ),
        "Diarios de muestra" to listOf(FeedSection("Diarios de muestra", more = false, items = bookCards())),
        "Portadas" to listOf(FeedSection("Portadas", more = false, items = templateCards())),
        "Planners con hipervínculos" to listOf(FeedSection("Planners con hipervínculos", more = false, items = plannerCards())),
        "Plantillas" to listOf(FeedSection("Plantillas", more = false, items = plannerCards()))
    )

    private fun plannerCards() = listOf(
        FeedCard("Colorful Digital Planner 2026", "🗓 PACK ☼", 0xFFFDEFF0.toInt(), badge = true, description = "642 páginas · colorida"),
        FeedCard("Life-Work Digital Planner 2026", "🍂", 0xFFF6E9D9.toInt(), badge = true, description = "minimal · undated"),
        FeedCard("Pro Planner Sencillo", "📓", 0xFFEFF6FF.toInt(), badge = false, description = "en blanco")
    )

    private fun monthlyCards() = listOf(
        FeedCard("Productivity Planner 2026", "☁ ☼", 0xFFFDEEF3.toInt(), badge = true, description = "mensual"),
        FeedCard("Undated Planner 2026", "🌦 ✿ ✚", 0xFFEAF4F1.toInt(), badge = true, description = "sin fecha"),
        FeedCard("Hoja mensual simple", "⬒", 0xFFF6F7FB.toInt(), badge = false, description = "cuadrícula")
    )

    private fun weeklyCards() = listOf(
        FeedCard("Digital Weekly Planner 2026", "▤", 0xFFEAF1F7.toInt(), badge = true, description = "semanal"),
        FeedCard("Elegant Planner 2026", "✎ ✥", 0xFFF6EDE1.toInt(), badge = true, description = "elegante")
    )

    private fun habitCards() = listOf(
        FeedCard("Ultimate Habit Tracker", "🦄", 0xFFEFF7F5.toInt(), badge = true, description = "seguimiento"),
        FeedCard("Monthly Habit Tracker", "✗ ⚒", 0xFFFBE7E1.toInt(), badge = false, description = "mensual"),
        FeedCard("Rutina Fitness", "⚽", 0xFFE8F5E9.toInt(), badge = false, description = "semanal")
    )

    fun stickersFallback(): List<String> = stickerPacks.flatMap { it.second }
}
