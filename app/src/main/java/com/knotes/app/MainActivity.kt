package com.knotes.app

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode
import com.knotes.app.ui.BackupUtil
import com.knotes.app.ui.FeedFragment
import com.knotes.app.ui.HomeFragment
import com.knotes.app.ui.MiscFragments
import com.knotes.app.ui.TemplatesLibrary
import com.knotes.app.ui.Dialogs
import com.knotes.app.ui.Gender
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

/**
 * MainActivity — raíl lateral izquierdo + navegación inferior.
 * · raíl: diarios / 🧭 plantillas externas / 💾 respaldo / ayuda / papelera
 * · bottom: Diarios / Calendario (pronto) / Descubrir / Mercado / Ajustes
 */
class MainActivity : AppCompatActivity() {

    lateinit var railNotes: ImageButton
    lateinit var railCompass: ImageButton
    lateinit var railStore: ImageButton
    lateinit var railTrash: ImageButton
    lateinit var railHelp: ImageButton
    lateinit var railGear: ImageButton
    lateinit var bottomNav: BottomNavigationView

    var currentTab = "home"

    /* destinación del respaldo (0 JSON 1 TXT 2 HTML) */
    private var backupKind = 0

    private val backupCreate = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { BackupUtil.writeAll(this, it, backupKind) }
    }

    private val importOpen = registerForActivityResult(
        ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { BackupUtil.restoreFrom(this, it) }
    }

    /** para la biblioteca de plantillas: importar archivo externo */
    fun launchImportFile() {
        importOpen.launch(arrayOf("*/*"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        paintStatus()

        railNotes = findViewById(R.id.rail_notes)
        railCompass = findViewById(R.id.rail_compass)
        railStore = findViewById(R.id.rail_store)
        railTrash = findViewById(R.id.rail_trash)
        railHelp = findViewById(R.id.rail_help)
        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_LABELED
        bottomNav.itemIconTintList = Gender.navTint(this)
        bottomNav.itemTextColor = Gender.navTint(this)

        railGear = findViewById(R.id.rail_gear)

        railLogoClick()
        paintRailLogo()
        railNotes.setOnClickListener { show("home") }
        railCompass.setOnClickListener { TemplatesLibrary.open(this) { launchImportFile() } }
        railStore.setOnClickListener { openBackup() }
        railTrash.setOnClickListener { show("trash") }
        railHelp.setOnClickListener { showHelp() }
        railGear.setOnClickListener { show("settings") }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notes -> show("home")
                R.id.nav_calendar -> { MiscFragments.showCalendarSoon(this); false }
                R.id.nav_discover -> show("discover")
                R.id.nav_market -> show("market")
                R.id.nav_settings -> show("settings")
            }
            true
        }

        if (savedInstanceState == null) show("home")
        // Primer arranque: ¿mujer u hombre? + tu nombre (se recuerda para siempre)
        if (!Gender.configured) Gender.onboarding(this)
    }

    /** status bar con el color y el contraste del tema ACTIVO (claro/oscuro) */
    fun paintStatus() {
        window.statusBarColor = resources.getColor(R.color.bg_app, theme)
        val night = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        window.decorView.systemUiVisibility = if (night)
            window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        else
            window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    private fun showHelp() {
        Dialogs.light(this)
            .setTitle("Ayuda de KNotes")
            .setMessage(
                "• Pulsa «+» o «Nuevo Diario» para crear cuadernos ilimitados.\n" +
                "• 🧭 Brújula: biblioteca de plantillas con vista previa; «Descargar» las incorpora. Ahí también importas plantillas o respaldos de archivo.\n" +
                "• ✉️ Sobre: respalda TODOS tus diarios (JSON restaurable, TXT o HTML) en tu teléfono o Drive con el selector de Archivos.\n" +
                "• En el editor: lápiz, rotulador, lápiz suave, subrayador, goma, texto, imágenes, enlaces, stickers y exportar a TXT/PDF/Word/JPG/PNG.\n" +
                "• GUARDA y vuelve a entrar: tus trazos siguen siendo editables y borrables. La goma borra por trazo o por zona, y con el lazo mueves o eliminas cualquier elemento.\n" +
                "• Dos dedos para zoom / desplazarte. Cambia el tipo de página: rayado, cuadrícula, puntos o en blanco."
            )
            .setPositiveButton("¡Entendido!", null)
            .show()
    }

    private fun railLogoClick() {
        findViewById<ImageButton>(R.id.rail_logo).setOnClickListener { show("home") }
    }

    /* ================== respaldo ================== */

    private fun openBackup() {
        val items = arrayOf(
            "✉️ Crear respaldo (JSON · restaurable)",
            "📝 Crear respaldo (TXT legible)",
            "🌐 Crear respaldo (HTML legible)",
            "♻️ Restaurar respaldo (.json)",
            "☁️ Subir respaldo a Drive (Google)",
            "ℹ️ ¿Qué contiene el respaldo?"
        )
        Dialogs.light(this)
            .setTitle("💾 Respaldo de todos tus diarios")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> { backupKind = 0; backupCreate.launch("KNotes-respaldo-${BackupUtil.dateTag()}.json") }
                    1 -> { backupKind = 1; backupCreate.launch("KNotes-respaldo-${BackupUtil.dateTag()}.txt") }
                    2 -> { backupKind = 2; backupCreate.launch("KNotes-respaldo-${BackupUtil.dateTag()}.html") }
                    3 -> importOpen.launch(arrayOf("*/*"))
                    4 -> backupToDrive()
                    else -> Dialogs.light(this)
                        .setTitle("ℹ️ Sobre el respaldo")
                        .setMessage(
                            "El respaldo guarda TODOS tus diarios y páginas en el mismo ORDEN en que fueron creados.\n\n" +
                            "· JSON: copia completa (trazos, textos, stickers, enlaces e imágenes) — este es el que puedes RESTAURAR.\n" +
                            "· TXT/HTML: solo lectura para archivar o imprimir.\n\n" +
                            "ElSelector de Archivos te deja guardar en Descargas o en Google Drive. Restaurar añade los diarios sin pisar los que ya existen."
                        )
                        .setPositiveButton("OK", null).show()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    /** Genera el JSON restaurable y lo ENVÍA a Drive (compartiendo, sin cuenta) */
    private fun backupToDrive() {
        try {
            val dir = java.io.File(cacheDir, "export").apply { mkdirs() }
            val f = java.io.File(dir, "KNotes-respaldo-${BackupUtil.dateTag()}.json")
            f.writeText(BackupUtil.buildJson(com.knotes.app.data.Store.all()))
            val u = androidx.core.content.FileProvider.getUriForFile(
                this, "$packageName.files", f)
            val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(android.content.Intent.EXTRA_STREAM, u)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Respaldo KNotes")
                putExtra(android.content.Intent.EXTRA_TITLE, "Respaldo KNotes · sube tu Google Drive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(share, "Subir a Drive ☁️ (elige Drive entre tus apps)"))
        } catch (_: Exception) {
            Toast.makeText(this, "No se pudo preparar el respaldo 😕", Toast.LENGTH_SHORT).show()
        }
    }

    fun show(tab: String) {
        currentTab = tab
        val f: Fragment = when (tab) {
            "discover" -> FeedFragment.newDiscover()
            "market" -> FeedFragment.newMarket()
            "trash" -> MiscFragments.TrashFragment()
            "settings" -> MiscFragments.SettingsFragment()
            else -> HomeFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_host, f)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
        paintRail()
        paintBottomNav()
    }

    private fun paintRail() {
        fun bind(btn: ImageButton, active: Boolean, iconTintRoot: Int) {
            btn.background = if (active) getDrawable(Gender.circleBg()) else null
            btn.imageTintList = ColorStateList.valueOf(
                if (active) resources.getColor(R.color.bg_rail, theme) else iconTintRoot
            )
        }
        val dim = resources.getColor(R.color.text_dim, theme)
        bind(railNotes, currentTab == "home", dim)
        bind(railCompass, false, dim)
        bind(railStore, false, dim)
        bind(railTrash, currentTab == "trash", dim)
        bind(railHelp, false, dim)
        bind(railGear, currentTab == "settings", dim)
    }

    /** logo del raíl con el acento del sexo: MUJER ♥ corazón dorado · HOMBRE ⚔ espada blanca */
    private fun paintRailLogo() {
        val logo = findViewById<ImageButton>(R.id.rail_logo)
        logo.background = getDrawable(Gender.circleBg())
        if (Gender.isMan) {
            logo.setImageResource(R.drawable.ic_sword)
            logo.imageTintList = ColorStateList.valueOf(android.graphics.Color.WHITE)
        } else {
            logo.setImageResource(R.drawable.ic_heart)
            logo.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.accent_yellow, theme))
        }
    }

    private fun paintBottomNav() {
        when (currentTab) {
            "discover" -> bottomNav.selectedItemId = R.id.nav_discover
            "market" -> bottomNav.selectedItemId = R.id.nav_market
            "settings" -> bottomNav.selectedItemId = R.id.nav_settings
            "home", "trash" -> bottomNav.selectedItemId = R.id.nav_notes
        }
    }

    override fun onResume() {
        super.onResume()
        paintStatus()
        // refresca al volver del editor
        supportFragmentManager.findFragmentById(R.id.content_host)?.let {
            supportFragmentManager.beginTransaction().detach(it).attach(it).commitAllowingStateLoss()
        }
        // buscador de actualizaciones en silencio (aún si GIST_URL_HERE está configurado)
        com.knotes.app.io.Actualizaciones.check(this, auto = true)
    }
}
