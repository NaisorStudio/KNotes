package com.knotes.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.knotes.app.MainActivity
import com.knotes.app.R
import com.knotes.app.data.Prefs
import com.knotes.app.data.ThemeKit

/* ============================================================
   Modo Hombre / Mujer — se pregunta UNA vez al abrir la app por
   primera vez y queda guardado aunque se cierre (SharedPreferences).
   · Mujer 👩: acentos rosa, cuaderno ROSA del lanzador y la
     biblioteca de plantillas románticas/tiernas.
   · Hombre 👨: acentos AZUL oscuro, cuaderno NEGRO con AZUL y
     biblioteca con plantillas gym / fútbol / retos / gaming…
   · Se puede cambiar después en Ajustes ⇒ «Cambiar sexo».
   ============================================================ */
object Gender {
    const val UNSET = 0
    const val WOMAN = 1
    const val MAN = 2

    val isMan get() = Prefs.gender == MAN
    val configured get() = Prefs.gender != UNSET
    fun label(): String = if (isMan) "Hombre 👨" else "Mujer 👩"

    /* --------- acentos por modo --------- */
    fun accent(ctx: Context) = ctx.getColor(if (isMan) R.color.accent_blue else R.color.accent_pink)
    fun accentDeep(ctx: Context) = ctx.getColor(if (isMan) R.color.accent_blue_deep else R.color.accent_pink_deep)
    fun accentSoft(ctx: Context) = ctx.getColor(if (isMan) R.color.accent_blue_soft else R.color.accent_pink_soft)

    /* círculo activo de herramientas / raíl (rosa ↔ azul) */
    fun circleBg() = if (isMan) R.drawable.bg_circle_blue else R.drawable.bg_circle_pink
    /* chip suave seleccionado (listas, portadas, chips de etiquetas) */
    fun chipSelBg() = if (isMan) R.drawable.bg_chip_selected_blue else R.drawable.bg_chip_selected
    /* circle suave (regalos / tableros pequeños) */
    fun softCircleBg() = if (isMan) R.drawable.bg_circle_blue_soft else R.drawable.bg_circle_pink_soft
/* chip fuerte de los diálogos (rosa vivo ↔ azul vivo) */
    fun chipStrongBg() = if (isMan) R.drawable.bg_chip_blue else R.drawable.bg_chip_pink

    /* tinte de la barra inferior */
    fun navTint(ctx: Context): ColorStateList = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(accentDeep(ctx), ctx.getColor(R.color.text_dim))
    )

    /* --------- ÍCONO del lanzador --------- */
    fun syncLauncher(ctx: Context) {
        val pm = ctx.packageManager
        val w = ComponentName(ctx.packageName, "${ctx.packageName}.WomanLauncher")
        val m = ComponentName(ctx.packageName, "${ctx.packageName}.ManLauncher")
        try {
            pm.setComponentEnabledSetting(w,
                if (isMan) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(m,
                if (isMan) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
        } catch (_: Exception) { /* nunca rompe la app */ }
    }

    /* --------- Primer arranque: PANTALLA COMPLETA en dos mitades --------- */
    fun onboarding(activity: MainActivity) = sexDialog(activity, first = true)

    /* Desde Ajustes ⇒ «Cambiar sexo»: la MISMA pantalla, cambia al instante */
    fun changeDialog(activity: MainActivity) = sexDialog(activity, first = false)

    private fun sexDialog(activity: MainActivity, first: Boolean) {
        fun px(v: Int) = (v * activity.resources.displayMetrics.density).toInt()

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(activity.getColor(R.color.bg_app))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        root.addView(TextView(activity).apply {
            text = "Elije tu Sexo"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(activity.getColor(R.color.text_primary))
            gravity = Gravity.CENTER
            setPadding(0, px(26), 0, px(4))
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(TextView(activity).apply {
            text = if (first)
                "Esto solo define el color de la app, el ícono y las plantillas.\nPodrás cambiarlo después en Ajustes ⚙."
            else
                "Toca una mitad y toda la app cambia al instante (ícono incluido)."
            textSize = 13f
            setTextColor(activity.getColor(R.color.text_dim))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            it.setMargins(px(26), 0, px(26), px(10))
        })

        var dlg: android.app.Dialog? = null

        fun half(man: Boolean): LinearLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(activity.getColor(if (man) R.color.accent_blue_soft else R.color.accent_pink_soft))
            addView(TextView(activity).apply {
                text = if (man) "👨" else "👩"
                textSize = 72f
                gravity = Gravity.CENTER
            })
            addView(TextView(activity).apply {
                text = if (man) "Hombre" else "Mujer"
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(activity.getColor(if (man) R.color.accent_blue_deep else R.color.accent_pink_deep))
                gravity = Gravity.CENTER
                setPadding(0, px(8), 0, 0)
            })
            addView(TextView(activity).apply {
                text = if (man)
                    "Acentos azul 💙\nÍcono azul y negro\nPlantillas gym · fútbol · retos"
                else
                    "Acentos rosa 💗\nÍcono rosa\nBiblioteca romántica"
                textSize = 13f
                setTextColor(activity.getColor(R.color.text_chip))
                gravity = Gravity.CENTER
                setLineSpacing(0f, 1.3f)
                setPadding(0, px(10), 0, 0)
            })
            setOnClickListener {
                dlg?.dismiss()
                Prefs.gender = if (man) MAN else WOMAN
                if (Prefs.userName.isBlank()) applyAndAskName(activity)
                else finishChange(activity)
            }
        }

        val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(half(false), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        row.addView(half(true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        root.addView(row, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        root.addView(TextView(activity).apply {
            text = if (first) "💡 Se cambia en Ajustes cuando quieras" else "Pulsa atrás para cancelar"
            textSize = 12f
            setTextColor(activity.getColor(R.color.text_dim))
            gravity = Gravity.CENTER
            setPadding(0, px(12), 0, px(14))
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        dlg = Dialogs.light(activity)
            .setView(root)
            .create()
            .also {
                it.setCancelable(!first)
                it.setCanceledOnTouchOutside(!first)
                it.show()
                it.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
    }

    private fun applyAndAskName(activity: MainActivity) {
        ThemeKit.apply()
        syncLauncher(activity)
        askName(activity)
    }

    fun askName(activity: MainActivity) {
        val input = EditText(activity).apply {
            hint = "Tu nombre (ej. Ana)"
            setHintTextColor(0xFF9A9288.toInt())
            setTextColor(0xFF2B2B2B.toInt())
            setPadding(24, 20, 24, 20)
            background = activity.getDrawable(R.drawable.bg_cell_white)
        }
        Dialogs.light(activity)
            .setTitle("¿Cómo te llamas? 📔")
            .setMessage("Así el inicio dirá «Todos los diarios de ⟨tu nombre⟩».")
            .setCancelable(false)
            .setView(input)
            .setPositiveButton("Listo") { _, _ ->
                Prefs.userName = input.text.toString().trim()
                ThemeKit.apply()
                syncLauncher(activity)
                activity.recreate()   // pinta tus acentos y tu ícono
            }
            .create()
            .also { it.setCanceledOnTouchOutside(false); it.show() }
    }

    private fun finishChange(activity: MainActivity) {
        ThemeKit.apply()
        syncLauncher(activity)
        Toast.makeText(activity, "Modo ${label()} aplicado ✅", Toast.LENGTH_SHORT).show()
        activity.recreate()
    }
}
