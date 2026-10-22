package com.knotes.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.knotes.app.EditorActivity
import com.knotes.app.R
import com.knotes.app.data.KPage
import com.knotes.app.data.Notebook
import com.knotes.app.data.Prefs
import com.knotes.app.data.Store
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/** Portada: «Todos los diarios de ⟨nombre⟩» con rejilla, chips, banners, FAB
 *  y SELECCIÓN MÚLTIPLE (mantener pulsado): eliminar, mover, compartir, info. */
class HomeFragment : Fragment() {

    private lateinit var fabNew: View
    private lateinit var grid: RecyclerView
    private lateinit var chipsRow: LinearLayout
    private lateinit var selBar: LinearLayout
    private lateinit var tvSelCount: TextView
    private lateinit var adapter: HomeAdapter
    private var filterTag: String = "todos"
    private var searchText: String = ""
    private var sortMode: Int = 0  // 0 recientes, 1 favoritos primero, 2 alfabético

    /* selección múltiple */
    private var selectionMode = false
    private val selected = LinkedHashMap<String, Notebook>()
    private val sdfCard = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    private val sdfFull = SimpleDateFormat("d 'de' MMMM yyyy · HH:mm", Locale.getDefault())

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_home, c, false)

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        val prefs = requireContext().getSharedPreferences("knotes", Context.MODE_PRIVATE)
        sortMode = prefs.getInt("sort", 0)
        grid = v.findViewById(R.id.home_grid)
        chipsRow = v.findViewById(R.id.chips_row)
        selBar = v.findViewById(R.id.sel_bar)
        tvSelCount = v.findViewById(R.id.tv_sel_count)
        fabNew = v.findViewById(R.id.fab_new)
        adapter = HomeAdapter()
        val target = dp(200) // ancho deseado por tarjeta en px → adm tablet/móvil
        val span = (resources.displayMetrics.widthPixels / target).coerceIn(2, 4)
        grid.layoutManager = GridLayoutManager(requireContext(), span)
        grid.adapter = adapter

        /* Título personalizado con el nombre del primer arranque */
        val name = Prefs.userName
        val title = v.findViewById<TextView>(R.id.home_title)
        title.text = if (name.isBlank()) "Todos los diarios" else "Todos los diarios de $name"
        // «subir para ver»: toca el título y la rejilla sube a la primera
        title.setOnClickListener { grid.smoothScrollToPosition(0) }

        var bannersVisible = !prefs.getBoolean("banner_hidden", false)
        // el scroll deja VER los diarios de abajo: padding que despeja banner + FAB
        fun padGrid() {
            grid.setPadding(grid.paddingLeft, grid.paddingTop, grid.paddingRight, dp(if (bannersVisible) 170 else 110))
        }
        padGrid()

        /* FAB con el acento del modo (rosa mujer / azul hombre) */
        val fab = v.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_new)
        fab.backgroundTintList = android.content.res.ColorStateList.valueOf(Gender.accentSoft(requireContext()))
        fab.imageTintList = android.content.res.ColorStateList.valueOf(Gender.accentDeep(requireContext()))

        /* barra de selección múltiple */
        v.findViewById<ImageButton>(R.id.ib_sel_close).setOnClickListener { exitSelection() }
        v.findViewById<ImageButton>(R.id.ib_sel_delete).setOnClickListener { selDelete() }
        v.findViewById<ImageButton>(R.id.ib_sel_move).setOnClickListener { selMove() }
        v.findViewById<ImageButton>(R.id.ib_sel_share).setOnClickListener { selShare() }
        v.findViewById<ImageButton>(R.id.ib_sel_info).setOnClickListener { selInfo() }

        val banners = v.findViewById<HorizontalScrollView>(R.id.banners_scroll)
        if (prefs.getBoolean("banner_hidden", false)) banners.visibility = View.GONE
        v.findViewById<TextView>(R.id.banner1_close).setOnClickListener {
            bannersVisible = false; padGrid()
            prefs.edit().putBoolean("banner_hidden", true).apply(); banners.visibility = View.GONE
            Toast.makeText(requireContext(), "Ve a Descubrir con la brújula 🧭", Toast.LENGTH_SHORT).show()
        }
        v.findViewById<TextView>(R.id.banner2_close).setOnClickListener {
            bannersVisible = false; padGrid()
            prefs.edit().putBoolean("banner_hidden", true).apply(); banners.visibility = View.GONE
        }
        v.findViewById<LinearLayout>(R.id.banner_discover).setOnClickListener { goDiscover() }
        v.findViewById<LinearLayout>(R.id.banner_tutorial).setOnClickListener {
            Dialogs.light(requireContext())
                .setTitle("Tutorial breve")
                .setMessage("1. Crea un diario → 2. Bordea con el lápiz → 3. Guarda 💾\n\nCuando vuelvas a entrar podrás borrar cualquier trazo con la goma o el lazo.")
                .setPositiveButton("OK", null).show()
        }

        v.findViewById<ImageButton>(R.id.btn_search).setOnClickListener { openSearch() }
        v.findViewById<ImageButton>(R.id.btn_more).setOnClickListener { openMore(v) }
        // regalo con el acento del modo (rosa ↔ azul suave)
        v.findViewById<ImageButton>(R.id.btn_gift).apply {
            background = requireContext().getDrawable(Gender.softCircleBg())
            setOnClickListener {
                Dialogs.light(requireContext())
                    .setTitle("🎁 Sorpresas de KNotes")
                    .setMessage("Nuevas portadas y stickers locales desbloqueadas al crear tu primer diario. ¡Todo gratis y sin conexión!")
                    .setPositiveButton("Genial", null).show()
            }
        }
        // botón flotante «nuevo diario» con el acento del modo
        v.findViewById<ImageView>(R.id.fab_new).apply {
            backgroundTintList = android.content.res.ColorStateList.valueOf(Gender.accentSoft(requireContext()))
            imageTintList = android.content.res.ColorStateList.valueOf(Gender.accentDeep(requireContext()))
            setOnClickListener { createDialog() }
        }

        refreshChips()
        refresh()
    }

    private fun goDiscover() {
        (activity as? com.knotes.app.MainActivity)?.show("discover")
    }

        private fun noteList(): List<Notebook> {
        val base = Store.all()
        val searched = if (searchText.isBlank()) base else base.filter {
            it.title.contains(searchText, true) || it.tags.any { t -> t.contains(searchText, true) }
        }
        val tagged = if (filterTag == "todos") searched else searched.filter { nb -> nb.tags.contains(filterTag) }
        return when (sortMode) {
            1 -> tagged.sortedByDescending { it.favorite }
            2 -> tagged.sortedBy { it.title.lowercase() }
            else -> tagged
        }
    }

    private fun refresh() {
        if (!this::adapter.isInitialized) return
        adapter.notifyDataSetChanged()
    }

    private fun refreshChips() {
        chipsRow.removeAllViews()
        val tags = listOf("todos") + Store.allTags()
        var px = 0
        for (tag in tags) {
            if (px > 8) break
            val tv = TextView(requireContext())
            tv.text = if (tag == "todos") "todos" else tag
            tv.setPadding(dp(16), dp(8), dp(16), dp(8))
            tv.textSize = 15f
            tv.setTextColor(if (tag == filterTag) 0xFFFFFFFF.toInt() else 0xFF6E5D66.toInt())
            tv.background = requireContext().getDrawable(if (tag == filterTag) Gender.chipSelBg() else R.drawable.bg_chip_normal)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = dp(8)
            tv.layoutParams = lp
            tv.setOnClickListener { filterTag = tag; refreshChips(); refresh() }
            chipsRow.addView(tv)
            px++
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    /* ================== SELECCIÓN MÚLTIPLE ================== */

    private fun enterSelection(nb: Notebook) {
        selectionMode = true
        selected.clear()
        selected[nb.id] = nb
        selBar.visibility = View.VISIBLE
        fabNew.visibility = View.INVISIBLE
        // libera el fondo: banners abajo tapaban los botones de la selección
        view?.findViewById<View>(R.id.banners_scroll)?.visibility = View.GONE
        grid.setPadding(grid.paddingLeft, grid.paddingTop, grid.paddingRight, dp(110))
        updateSelBar(); refresh()
        Toast.makeText(requireContext(), "Toca tarjetas para sumar o quitar ✓", Toast.LENGTH_SHORT).show()
    }

    private fun exitSelection() {
        selectionMode = false
        selected.clear()
        selBar.visibility = View.GONE
        fabNew.visibility = View.VISIBLE
        // los banners regresan si aún no se cierran manualmente
        val hidden = requireContext().getSharedPreferences("knotes", Context.MODE_PRIVATE)
            .getBoolean("banner_hidden", false)
        if (!hidden) view?.findViewById<View>(R.id.banners_scroll)?.visibility = View.VISIBLE
        refresh()
    }

    private fun toggleSelect(nb: Notebook) {
        if (selected.containsKey(nb.id)) selected.remove(nb.id) else selected[nb.id] = nb
        if (selected.isEmpty()) exitSelection() else updateSelBar()
        refresh()
    }

    private fun updateSelBar() {
        tvSelCount.text = "${selected.size} seleccionado${if (selected.size == 1) "" else "s"}"
    }

    private fun selDelete() {
        if (selected.isEmpty()) return
        Dialogs.light(requireContext())
            .setTitle("¿Mover ${selected.size} ${if (selected.size == 1) "diario" else "diarios"} a la papelera?")
            .setMessage("Podrás restaurarlos desde la papelera.")
            .setPositiveButton("Mover") { _, _ ->
                for (nb in selected.values) Store.toTrash(nb)
                Toast.makeText(requireContext(), "A la papelera 🗑", Toast.LENGTH_SHORT).show()
                exitSelection(); refresh()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selMove() {
        Dialogs.light(requireContext())
            .setTitle("Mover ${selected.size}…")
            .setItems(arrayOf("🗑 A la papelera", "⭐ Marcar como favorito", "⤵ Duplicar")) { _, w ->
                for (nb in selected.values) when (w) {
                    0 -> Store.toTrash(nb)
                    1 -> { nb.favorite = true; Store.saveMeta(nb) }
                    else -> Store.duplicate(nb)
                }
                Toast.makeText(requireContext(), "Listo ✅", Toast.LENGTH_SHORT).show()
                exitSelection(); refresh()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selShare() {
        if (selected.isEmpty()) return
        com.knotes.app.io.ExportUtil.shareDiaries(requireActivity(), ArrayList(selected.keys))
    }

    private fun selInfo() {
        if (selected.isEmpty()) return
        val sb = StringBuilder()
        for (nb in selected.values) {
            val dir = java.io.File(com.knotes.app.data.Store.root(), nb.id)
            val pages = dir.listFiles { f -> f.name.startsWith("p") && f.name.endsWith(".json") }?.size ?: 0
            val bytes = dir.walkBottomUp().filter { it.isFile }.fold(0L) { acc, f -> acc + f.length() }
            val peso = when {
                bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
                bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
            sb.append("📔 ${nb.title}\n")
            sb.append("   📅 Creado: ${sdfFull.format(Date(nb.createdAt))}\n")
            sb.append("   ✏️ Última edición: ${sdfFull.format(Date(nb.updatedAt))}\n")
            sb.append("   📄 Páginas: ${pages.coerceAtLeast(0)}\n")
            sb.append("   ⚖️ Peso: $peso\n\n")
        }
        val tv = TextView(requireContext()).apply {
            text = sb.toString(); textSize = 14f
            setTextColor(0xFF2A2630.toInt())          // SIEMPRE oscuro: legible en modo claro/oscuro
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val sv = android.widget.ScrollView(requireContext()).apply { addView(tv) }
        Dialogs.light(requireContext())
            .setTitle("ℹ️ Información (${selected.size})")
            .setView(sv)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    /* ---------- acciones ---------- */

    private fun openSearch() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.search_hint); setPadding(dp(18), dp(14), dp(18), dp(14));
            background = requireContext().getDrawable(R.drawable.bg_cell_white);
            setTextColor(0xFF38322F.toInt())
            setHintTextColor(0xFF9A9186.toInt())
        }
        Dialogs.light(requireContext())
            .setTitle("Buscar")
            .setView(frame(input))
            .setPositiveButton("Buscar") { _, _ -> searchText = input.text.toString(); refresh() }
            .setNegativeButton("Limpiar") { _, _ -> searchText = ""; refresh() }
            .show()
    }

    private fun frame(input: View): FrameLayout {
        return FrameLayout(requireContext()).apply {
            setPadding(dp(18), dp(4), dp(18), 0)
            addView(input, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    private fun openMore(anchor: View) {
        val pm = PopupMenu(requireContext(), anchor.findViewById(R.id.btn_more), Gravity.END)
        pm.menu.add(0, 1, 0, "Nuevo diario")
        pm.menu.add(0, 2, 0, "Ordenar: recientes")
        pm.menu.add(0, 3, 0, "Ordenar: favoritos primero")
        pm.menu.add(0, 4, 0, "Ordenar: alfabético")
        pm.menu.add(0, 5, 0, "Acerca de KNotes")
        pm.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> createDialog()
                2, 3, 4 -> { sortMode = item.itemId - 2; requireContext().getSharedPreferences("knotes", Context.MODE_PRIVATE).edit().putInt("sort", sortMode).apply(); refresh() }
                5 -> Dialogs.light(requireContext()).setTitle("KNotes v1.5.4").setMessage("Diarios con trazos SIEMPRE editables.\nHecho con ❤ en Kotlin. Sin internet, 100 % local.").setPositiveButton("OK", null).show()
            }
            true
        }
        pm.show()
    }

    private var chosenStyle: Int = 0

    fun createDialog() {
        val ctx = requireContext()
        val nbName = EditText(ctx).apply {
            hint = "Nombre del diario"
            background = ctx.getDrawable(R.drawable.bg_cell_white)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setTextColor(0xFF38322F.toInt())
            setHintTextColor(0xFF9A9186.toInt())
        }
        chosenStyle = Random.nextInt(12)
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), 0)
            addView(nbName, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val coverGrid = GridLayout(ctx).apply { columnCount = 3 }
        for (i in 0 until 12) {
            val cell = FrameLayout(ctx).apply { setPadding(dp(6), dp(6), dp(6), dp(6)); tag = i }
            cell.addView(NotebookCoverView(ctx).apply { layoutParams = ViewGroup.LayoutParams(dp(84), dp(110)); style = i; seed = Random.nextLong() })
            cell.setOnClickListener { chosenStyle = i; high(coverGrid, chosenStyle) }
            coverGrid.addView(cell, GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)))
        }
        high(coverGrid, chosenStyle)
        root.addView(coverGrid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = dp(14) })
        val scroller = android.widget.ScrollView(ctx).apply { addView(root) }
        Dialogs.light(ctx)
            .setTitle("Nuevo Diario")
            .setView(scroller)
            .setPositiveButton("Crear") { _, _ ->
                val created = Store.createNotebook(
                    nbName.text.toString().ifBlank { "Nuevo Diario" },
                    chosenStyle
                )
                Store.saveMeta(created)
                Toast.makeText(requireContext(), "¡Diario creado! ✍️", Toast.LENGTH_SHORT).show()
                refreshChips(); refresh()
                openEditor(created)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun openEditor(nb: Notebook) {
        requireContext().startActivity(Intent(requireContext(), EditorActivity::class.java).putExtra("id", nb.id))
    }

    private fun high(grid: GridLayout, chosen: Int) {
        val ctx = requireContext()
        for (i in 0 until grid.childCount) {
            (grid.getChildAt(i) as FrameLayout).background =
                ctx.getDrawable(if ((grid.getChildAt(i).tag as Int) == chosen) Gender.chipSelBg() else R.drawable.bg_card_soft)
        }
    }

    /* ---------- Adapter ---------- */

    inner class HomeAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val TYPE_CREATE = 0
        val TYPE_NOTE = 1

        override fun getItemCount(): Int = noteList().size + 1

        override fun getItemViewType(position: Int) = if (position == 0) TYPE_CREATE else TYPE_NOTE

        override fun onCreateViewHolder(parent: ViewGroup, vt: Int): RecyclerView.ViewHolder = when (vt) {
            TYPE_CREATE -> CreateHolder(LayoutInflater.from(requireContext()).inflate(R.layout.item_create, parent, false))
            else -> NoteHolder(LayoutInflater.from(requireContext()).inflate(R.layout.item_notebook, parent, false))
        }

        override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
            when (h) {
                is CreateHolder -> h.itemView.setOnClickListener { createDialog() }
                is NoteHolder -> {
                    val nb = noteList()[pos - 1]
                    h.bind(nb)
                }
            }
        }
    }

    inner class CreateHolder(v: View) : RecyclerView.ViewHolder(v)

    inner class NoteHolder(v: View) : RecyclerView.ViewHolder(v) {
        val cover: NotebookCoverView = v.findViewById(R.id.cover)
        val tv: TextView = v.findViewById(R.id.tv_title)
        val date: TextView = v.findViewById(R.id.tv_date)
        val badge: TextView = v.findViewById(R.id.badge_sel)
        val heart: ImageButton = v.findViewById(R.id.btn_favorite)
        val menuBtn: ImageButton = v.findViewById(R.id.btn_menu)

        fun bind(nb: Notebook) {
            cover.style = nb.coverStyle
            cover.seed = nb.coverSeed
            cover.invalidate()
            tv.text = nb.title
            // fecha de CREACIÓN debajo del nombre
            date.text = sdfCard.format(Date(nb.createdAt))
            heart.setColorFilter(Gender.accent(requireContext()))

            // estado de selección múltiple
            val isSel = selected.containsKey(nb.id)
            if (selectionMode) {
                badge.visibility = View.VISIBLE
                badge.text = if (isSel) "✓" else ""
                badge.setTextColor(Color.WHITE)
                badge.background = requireContext().getDrawable(if (isSel) Gender.circleBg() else R.drawable.bg_circle_white)
            } else {
                badge.visibility = View.GONE
            }

            itemView.setOnClickListener { if (selectionMode) toggleSelect(nb) else openEditor(nb) }
            itemView.setOnLongClickListener { if (!selectionMode) enterSelection(nb); true }
            menuBtn.setOnClickListener { if (selectionMode) toggleSelect(nb) else showMenu(it, nb) }
            heart.setOnClickListener {
                nb.favorite = !nb.favorite; Store.saveMeta(nb); heart.setImageResource(if (nb.favorite) R.drawable.ic_heart_fill else R.drawable.ic_heart); refresh()
            }
        }

        fun showMenu(anchor: View, nb: Notebook) {
            val pm = PopupMenu(requireContext(), anchor, Gravity.NO_GRAVITY)
            pm.menu.add(0, 1, 0, "Renombrar")
            pm.menu.add(0, 2, 0, "Cambiar portada")
            pm.menu.add(0, 3, 0, "Etiquetas")
            pm.menu.add(0, 4, 0, if (nb.favorite) "Quitar de favoritos" else "Favorito ⭐")
            pm.menu.add(0, 5, 0, "Duplicar")
            pm.menu.add(0, 6, 0, "Mover a la papelera")
            pm.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> renameDialog(nb)
                    2 -> coverDialog(nb)
                    3 -> tagsDialog(nb)
                    4 -> { nb.favorite = !nb.favorite; Store.saveMeta(nb); refresh() }
                    5 -> { Store.duplicate(nb); refresh() }
                    6 -> { Store.toTrash(nb); Toast.makeText(requireContext(), "Diario en la papelera 🗑", Toast.LENGTH_SHORT).show(); refresh() }
                }
                true
            }
            pm.show()
        }

        fun renameDialog(nb: Notebook) {
            val input = EditText(requireContext()).apply { setText(nb.title); setTextColor(0xFF38322F.toInt()); background = requireContext().getDrawable(R.drawable.bg_cell_white); setPadding(dp(14), dp(10), dp(14), dp(10)) }
            Dialogs.light(requireContext())
                .setTitle("Renombrar diario")
                .setView(frame(input))
                .setPositiveButton("Guardar") { _, _ ->
                    nb.title = input.text.toString().ifBlank { nb.title }
                    Store.saveMeta(nb); refresh()
                }.setNegativeButton("Cancelar", null).show()
        }

        fun coverDialog(nb: Notebook) {
            val ctx = requireContext()
            val gridLayout = GridLayout(ctx).apply { columnCount = 3; setPadding(dp(14), dp(10), dp(14), 0) }
            val chosen = intArrayOf(nb.coverStyle)
            for (i in 0 until 12) {
                val cell = FrameLayout(ctx).apply { setPadding(dp(6), dp(6), dp(6), dp(6)); tag = i }
                val c = NotebookCoverView(ctx).apply { layoutParams = ViewGroup.LayoutParams(dp(84), dp(110)); style = i; seed = if (i == nb.coverStyle) nb.coverSeed else Random.nextLong() }
                cell.addView(c)
                cell.setOnClickListener { chosen[0] = i; high(gridLayout, chosen[0]) }
                gridLayout.addView(cell, GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)))
            }
            high(gridLayout, nb.coverStyle)
            val sc = android.widget.ScrollView(ctx).apply { addView(gridLayout) }
            Dialogs.light(ctx)
                .setTitle("Cambiar portada")
                .setView(sc)
                .setPositiveButton("Aplicar") { _, _ ->
                    Store.saveMeta(nb.also { it.coverStyle = chosen[0] })
                    refresh()
                }.setNegativeButton("Cancelar", null).show()
        }

        fun high(grid: GridLayout, chosen: Int) {
            val ctx = requireContext()
            for (i in 0 until grid.childCount) {
                (grid.getChildAt(i) as FrameLayout).background = ctx.getDrawable(if ((grid.getChildAt(i).tag as Int) == chosen) Gender.chipSelBg() else R.drawable.bg_card_soft)
            }
        }

        fun tagsDialog(nb: Notebook) {
            val input = EditText(requireContext()).apply {
                hint = "separadas por comas: trabajo, ideas..."
                setText(nb.tags.joinToString())
                setTextColor(0xFF38322F.toInt()); background = requireContext().getDrawable(R.drawable.bg_cell_white); setPadding(dp(14), dp(10), dp(14), dp(10)); setRawInputType(android.text.InputType.TYPE_CLASS_TEXT)
            }
            Dialogs.light(requireContext())
                .setTitle("Etiquetas")
                .setView(frame(input))
                .setPositiveButton("Guardar") { _, _ ->
                    nb.tags = input.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                    Store.saveMeta(nb); refreshChips(); refresh()
                }.setNegativeButton("Cancelar", null).show()
        }
    }
}
