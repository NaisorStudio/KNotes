package com.knotes.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.knotes.app.EditorActivity
import com.knotes.app.MainActivity
import com.knotes.app.R
import com.knotes.app.data.Notebook
import com.knotes.app.data.Prefs
import com.knotes.app.data.Store
import com.knotes.app.data.ThemeKit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MiscFragments {

    fun showCalendarSoon(activity: MainActivity) {
        val v = LinearLayout(activity).apply { setPadding(40, 30, 40, 20) }
        val t = TextView(activity).apply {
            text = "🗓 Vista de calendario:\n\nPróximamente. Por ahora tus diarios por plantilla semanal/mensual están en Descubrir."
            setTextColor(0xFFBBBBCC.toInt())
        }
        v.addView(t)
        Dialogs.light(activity)
            .setView(v)
            .setPositiveButton("OK", null)
            .show()
    }

    /* ============ Papelera ============ */
    class TrashFragment : Fragment() {
        private lateinit var list: RecyclerView
        private lateinit var empty: View
        private lateinit var emptyLabel: TextView
        private val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
            i.inflate(R.layout.fragment_trash, c, false)

        override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
            list = v.findViewById(R.id.trash_list)
            empty = v.findViewById(R.id.trash_empty_view)
            list.layoutManager = LinearLayoutManager(requireContext())
            list.adapter = TrashAdapter()
            // «Vaciar» con el acento del modo (rosa ↔ azul)
            v.findViewById<TextView>(R.id.trash_empty).apply {
                setTextColor(Gender.accentDeep(requireContext()))
                setOnClickListener {
                Dialogs.light(requireContext())
                    .setTitle("Vaciar papelera")
                    .setMessage("Se borrará para siempre. ¿Continuar?")
                    .setPositiveButton("Vaciar") { _, _ -> Store.emptyTrash(); refresh() }
                    .setNegativeButton("Cancelar", null).show()
                }
            }
            refresh()
        }

        fun refresh() {
            val items = Store.trashed()
            (list.adapter as TrashAdapter).items = items
            (list.adapter as TrashAdapter).notifyDataSetChanged()
            empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        inner class TrashAdapter : RecyclerView.Adapter<TrashHolder>() {
            var items: List<Notebook> = ArrayList()
            override fun getItemCount() = items.size
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                TrashHolder(LayoutInflater.from(requireContext()).inflate(R.layout.item_trash, parent, false))
            override fun onBindViewHolder(h: TrashHolder, pos: Int) {
                val nb = items[pos]
                h.cover.style = nb.coverStyle; h.cover.seed = nb.coverSeed; h.cover.invalidate()
                h.title.text = nb.title
                h.date.text = "Eliminado: ${fmt.format(Date(nb.deletedAt ?: 0L))}"
                h.restore.setOnClickListener {
                    Store.restore(nb); refresh()
                    Toast.makeText(requireContext(), "Restaurado ✅", Toast.LENGTH_SHORT).show()
                }
                h.del.setOnClickListener {
                    Dialogs.light(requireContext())
                        .setTitle("Borrar para siempre")
                        .setMessage("«${nb.title}» desaparecerá. ¿Seguir?")
                        .setPositiveButton("Borrar") { _, _ -> Store.deleteForever(nb); refresh() }
                        .setNegativeButton("Cancelar", null).show()
                }
            }
        }

        inner class TrashHolder(v: View) : RecyclerView.ViewHolder(v) {
            val cover: NotebookCoverView = v.findViewById(R.id.cover)
            val title: TextView = v.findViewById(R.id.tv_title)
            val date: TextView = v.findViewById(R.id.tv_date)
            val restore: TextView = v.findViewById(R.id.btn_restore)
            val del: ImageButton = v.findViewById(R.id.btn_delete_forever)
        }
    }

    /* ============ Ajustes ============ */
    class SettingsFragment : Fragment() {
        override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
            i.inflate(R.layout.fragment_settings, c, false)

        override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
            val rows = v.findViewById<LinearLayout>(R.id.settings_rows)
            fun addRow(label: String, detail: String, onClick: (() -> Unit)? = null) {
                val ctx = requireContext()
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    background = ctx.getDrawable(R.drawable.bg_notebook_card)
                    setPadding(44, 34, 44, 34)
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.bottomMargin = 20
                    layoutParams = lp
                }
                row.addView(TextView(ctx).apply { text = label; setTextColor(ctx.getColor(R.color.text_primary)); textSize = 15f })
                row.addView(TextView(ctx).apply { text = detail; setTextColor(0xFF8E8E99.toInt()); textSize = 12f; setPadding(0, 4, 0, 0) })
                onClick?.let { row.setOnClickListener { _ -> it() } }
                rows.addView(row)
            }

            val prefs = requireContext().getSharedPreferences("knotes", 0)

            addRow(
                "Tema de la app ☀️🌙",
                when (Prefs.theme) { 1 -> "Claro"; 2 -> "Oscuro"; else -> "Automático (sigue al sistema)" },
                onClick = {
                    Dialogs.light(requireContext())
                        .setTitle("Tema de la app")
                        .setItems(arrayOf("Automático (como tu sistema)", "Claro ☀️", "Oscuro 🌙")) { _, w ->
                            Prefs.theme = w
                            ThemeKit.apply()
                            Toast.makeText(requireContext(), "Tema aplicado — se recuerda aunque cierres la app ✔", Toast.LENGTH_SHORT).show()
                        }
                        .show()
                })
            addRow("Cambiar sexo", "Modo actual: ${Gender.label()} — mujer = rosa; hombre = azul + otro ícono", onClick = {
                (activity as? MainActivity)?.let { Gender.changeDialog(it) }
            })
            addRow("Tu nombre", Prefs.userName.ifBlank { "Sin definir — aparece en «Todos los diarios de …»" }, onClick = {
                val input = EditText(requireContext()).apply {
                    hint = "Tu nombre"
                    setText(Prefs.userName)
                    setTextColor(0xFF38322F.toInt())
                    background = requireContext().getDrawable(R.drawable.bg_cell_white)
                    setPadding(24, 20, 24, 20)
                }
                Dialogs.light(requireContext())
                    .setTitle("Tu nombre")
                    .setView(input)
                    .setPositiveButton("Guardar") { _, _ ->
                        Prefs.userName = input.text.toString().trim()
                        Toast.makeText(requireContext(), "¡Listo! 👋", Toast.LENGTH_SHORT).show()
                        // ACTUALIZA EL TÍTULO AL INSTANTE (sin salir y volver a entrar)
                        (activity as? MainActivity)?.recreate()
                    }
                    .setNegativeButton("Cancelar", null).show()
            })
            addRow("Buscar actualizaciones", com.knotes.app.io.Actualizaciones.status(), onClick = {
                (activity as? MainActivity)?.let { com.knotes.app.io.Actualizaciones.check(it, auto = false) }
            })
            addRow("Diarios ilimitados", "Crea todos los cuadernos que quieras, con portadas únicas generadas al azar.")
            addRow("Trazos siempre editables", "Guarda → sal → vuelve: goma por zona/trazo y lazo para mover o borrar TODO.",
                onClick = {
                    Dialogs.light(requireContext())
                        .setTitle("¿Cómo funciona?")
                        .setMessage("KNotes guarda cada trazo, texto, imagen, sticker y enlace como un objeto vectorial en JSON. Nada se aplasta en una imagen, por eso puedes seguir borrando o moviendo todo, incluso tras reabrir el cuaderno.")
                        .setPositiveButton("¡Genial!", null).show()
                })
            addRow("Tipos de página", "Rayado · Cuadrícula · Puntos · En blanco (por página)")

            val defaultTpl = intArrayOf(prefs.getInt("defaultTemplate", 0))
            addRow("Página por defecto", PAGE_NAMES[defaultTpl[0]], onClick = {
                Dialogs.light(requireContext())
                    .setTitle("Tipo de página por defecto")
                    .setItems(PAGE_NAMES) { _, w ->
                        defaultTpl[0] = w
                        prefs.edit().putInt("defaultTemplate", w).apply()
                    }.show()
            })
            addRow("Guardado", "Botón «guardar» + autoguardado al salir. 100 % local, sin cuentas.")
            addRow("𝖠𝖼𝖾𝗋𝖼𝖺", "KNotes v1.5.4 · Kotlin · hecho con ❤️")
        }

        companion object { val PAGE_NAMES = arrayOf("Rayado", "Cuadrícula", "Puntos", "En blanco") }
    }
}
