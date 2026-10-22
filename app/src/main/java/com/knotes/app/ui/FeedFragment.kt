package com.knotes.app.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.knotes.app.EditorActivity
import com.knotes.app.MainActivity
import com.knotes.app.R
import com.knotes.app.data.Notebook
import com.knotes.app.data.Store

/** Feed genérico para Descubrir y Mercado, con pestañas y secciones. */
class FeedFragment : Fragment() {

    private var kind = "discover"
    private lateinit var tabs: TabLayout
    private lateinit var list: RecyclerView
    private lateinit var sectionAdapter: SectionAdapter
    private var sections: List<FeedSection> = ArrayList()

    companion object {
        fun newDiscover(): FeedFragment = FeedFragment().apply { arguments = Bundle().apply { putString("kind", "discover") } }
        fun newMarket(): FeedFragment = FeedFragment().apply { arguments = Bundle().apply { putString("kind", "market") } }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_feed, c, false)

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        kind = requireArguments().getString("kind", "discover")
        val titleView = v.findViewById<TextView>(R.id.feed_title)
        titleView.text = getString(if (kind == "discover") R.string.discover else R.string.market)

        tabs = v.findViewById(R.id.feed_tabs)
        list = v.findViewById(R.id.feed_list)
        sectionAdapter = SectionAdapter()
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = sectionAdapter

        val tabNames = if (kind == "discover") Catalog.discoverTabs else Catalog.marketTabs
        val sectionMap = if (kind == "discover") Catalog.discoverSections else Catalog.marketSections

        for (t in tabNames) tabs.addTab(tabs.newTab().setText(t))

        fun bindFor(tab: Int) {
            sections = sectionMap[tabNames[tab].lowercase()] ?: sectionMap["inicio"] ?: emptyList()
            sectionAdapter.notifyDataSetChanged()
            list.scrollToPosition(0)
        }
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(t: TabLayout.Tab) { bindFor(t.position) }
            override fun onTabUnselected(t: TabLayout.Tab) {}
            override fun onTabReselected(t: TabLayout.Tab) {}
        })
        bindFor(0)

        // regalo + pestañas con el acento del modo (rosa ↔ azul)
        v.findViewById<android.widget.ImageButton>(R.id.btn_gift).apply {
            background = requireContext().getDrawable(Gender.softCircleBg())
            setOnClickListener {
                Dialogs.light(requireContext())
                    .setTitle("🎁 Mercado de KNotes")
                    .setMessage("Todo el contenido es local y gratis: plantillas, stickers y diarios de muestra.")
                    .setPositiveButton("OK", null).show()
            }
        }
        tabs.setSelectedTabIndicatorColor(Gender.accent(requireContext()))
        v.findViewById<View>(R.id.btn_history).visibility = View.GONE
    }

    private fun useCard(card: FeedCard) {
        if (card.build == null) {
            // Sticker pack: vista previa
            val items = Catalog.stickerPacks.firstOrNull { it.first.lowercase() == card.title.lowercase() }?.second
                ?: Catalog.stickersFallback()
            Dialogs.light(requireContext())
                .setTitle("Stickers: ${card.title}")
                .setMessage(items.joinToString("  ") + "\n\nÚsalos desde el editor con la herramienta 😊")
                .setPositiveButton("OK", null).show()
            return
        }
        val nb: Notebook = Store.createNotebook(
            card.title,
            tabsUnitsToStyle(card.title),
            pages = card.build.invoke()
        )
        nb.tags.add(if (kind == "market") "mercado" else "descubrir")
        Store.saveMeta(nb)
        Toast.makeText(requireContext(), "Plantilla lista: ${card.title} ✨", Toast.LENGTH_SHORT).show()
        startActivity(Intent(requireContext(), EditorActivity::class.java).putExtra("id", nb.id))
    }

    private fun tabsUnitsToStyle(title: String): Int = (title.hashCode().let { if (it > 0) it else -it } % 12)

    private fun moreDialog(section: FeedSection) {
        val names = arrayOfNulls<String>(section.items.size)
        section.items.forEachIndexed { i, c -> names[i] = c.title }
        Dialogs.light(requireContext())
            .setTitle(section.title)
            .setItems(names) { _, which -> useCard(section.items[which]) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    /* ================= adapters ================= */

    inner class SectionAdapter : RecyclerView.Adapter<SectionHolder>() {
        override fun getItemCount() = sections.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionHolder {
            val v = LayoutInflater.from(requireContext()).inflate(R.layout.view_section, parent, false)
            return SectionHolder(v)
        }
        override fun onBindViewHolder(h: SectionHolder, pos: Int) {
            val s = sections[pos]
            h.title.text = s.title
            h.more.setOnClickListener { moreDialog(s) }
            h.more.visibility = if (s.more) View.VISIBLE else View.GONE
            val row = h.recycler
            if (row.tag != null && row.tag == kind) {
                (row.adapter as CardAdapter).submit(s.items)
            } else {
                row.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                val a = CardAdapter(kind)
                a.submit(s.items)
                row.adapter = a
                row.tag = kind
            }
        }
    }

    inner class SectionHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.section_title)
        val more: TextView = v.findViewById(R.id.section_more)
        val recycler: RecyclerView = v.findViewById(R.id.section_recycler)
    }

    inner class CardAdapter(private val cardKind: String) : RecyclerView.Adapter<CardHolder>() {
        private var items: List<FeedCard> = ArrayList()
        fun submit(next: List<FeedCard>) { items = next; notifyDataSetChanged() }
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder {
            val layout = if (cardKind == "market") R.layout.item_market_card else R.layout.item_discover_card
            val v = LayoutInflater.from(requireContext()).inflate(layout, parent, false)
            return CardHolder(v, cardKind)
        }
        override fun onBindViewHolder(h: CardHolder, pos: Int) { h.bind(items[pos]) }
    }

    inner class CardHolder(v: View, private val cardKind: String) : RecyclerView.ViewHolder(v) {
        // Descubrir usa item_discover_card (solo imagen+nombre); Mercado el completo.
        // Las vistas faltantes van NULABLES: findViewById devolvía null y TUMBABA la app.
        val art: ProceduralCardView = v.findViewById(
            if (cardKind == "market") R.id.mcard_image else R.id.card_image)
        val title: TextView = v.findViewById(
            if (cardKind == "market") R.id.mcard_title else R.id.card_title)
        val badge: TextView? =
            if (cardKind == "market") v.findViewById(R.id.mcard_badge) else null
        val useBtn: Button? =
            if (cardKind == "market") v.findViewById(R.id.mcard_use) else null
        val subtitle: TextView? =
            if (cardKind == "market") v.findViewById(R.id.mcard_subtitle) else null

        fun bind(card: FeedCard) {
            art.cardColor = card.bg
            art.emoji = card.emojiArt
            art.template = card.tpl
            art.invalidate()
            title.text = card.title
            badge?.visibility = if (card.badge) View.VISIBLE else View.GONE
            subtitle?.text = card.description
            useBtn?.visibility = if (card.build != null) View.VISIBLE else View.GONE
            useBtn?.setOnClickListener { useCard(card) }
            itemView.setOnClickListener { useCard(card) }
        }
    }
}
