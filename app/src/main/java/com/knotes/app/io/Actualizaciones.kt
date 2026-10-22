package com.knotes.app.io

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.knotes.app.ui.Dialogs
import org.json.JSONObject
import java.net.URL

/* ============================================================
   Actualizaciones — canal de GitHub para ACTUALIZAR la app,
   con GIST para el control de versiones.

   ⚠️ AQUI VA TU GIST ⚠️⬇️
   1) Crea un gist (o un archivo raw en GitHub) con este JSON:

        { "version": 4, "name": "v1.3", "notas": "...", "apk": "https://…apk" }

       (la app compara «version» con SU versionCode)
   2) Copia la URL RAW del archivo y pégala en GIST_URL_HERE.
   3) El APK nuevo súbilo (GitHub Release / raw) y su URL directa
      déjala en APK_URL_HERE o en el campo «apk» del JSON.
   La app funciona PERFECTO aunque esté vacío: simplemente no busca.
   ============================================================ */
object Actualizaciones {

    /** ⇩ AQUI VA TU GIST (URL raw del JSON de versiones) */
    const val GIST_URL_HERE = ""

    /** ⇩ AQUI VA TU GITHUB (URL directa del .apk más reciente) */
    const val APK_URL_HERE = ""

    private const val CHECK_EVERY_MS = 12L * 60 * 60 * 1000   // 12 h

    fun status(): String = if (GIST_URL_HERE.isBlank())
        "Sin configurar — AQUI VA TU GIST en io/Actualizaciones.kt"
    else "Gist conectado ✔"

    /** Busca si hay versión nueva. auto=true = silencioso al abrir la app. */
    fun check(activity: Activity, auto: Boolean) {
        if (GIST_URL_HERE.isBlank()) {
            if (!auto) {
                Dialogs.light(activity)
                    .setTitle("Actualizaciones 🔔")
                    .setMessage("El canal de actualizaciones aún no está configurado.\n\n“Aqui va tu gist”: crea el gist con el JSON de versiones y pega su URL en io/Actualizaciones.kt ⇒ GIST_URL_HERE. Mientras tanto, la app funciona igual de bien offline.")
                    .setPositiveButton("OK", null).show()
            }
            return
        }
        val sp = activity.getSharedPreferences("knotes", Context.MODE_PRIVATE)
        val last = sp.getLong("updateLastCheck", 0)
        val now = System.currentTimeMillis()
        if (auto && now - last < CHECK_EVERY_MS) return
        sp.edit().putLong("updateLastCheck", now).apply()

        Thread {
            var errorUI = ""
            var newCode = -1
            var name = ""
            var notes = ""
            var apk = ""
            try {
                val json = JSONObject(URL(GIST_URL_HERE).readText())
                newCode = json.optInt("version", 0)
                name = json.optString("name", "nueva versión")
                notes = json.optString("notas", "")
                apk = json.optString("apk", APK_URL_HERE)
            } catch (e: Exception) {
                errorUI = e.message ?: "error de red"
            }
            activity.runOnUiThread {
                if (newCode >= 0) {
                    val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
                    val myCode = if (android.os.Build.VERSION.SDK_INT >= 28)
                        info.longVersionCode.toInt() else info.versionCode
                    if (newCode > myCode) {
                        Dialogs.light(activity)
                            .setTitle("Hay actualización: $name ⬇️")
                            .setMessage(if (notes.isBlank()) "Descarga la versión nueva y quédate con todos tus diarios (se actualiza encima)." else notes)
                            .setPositiveButton("Descargar") { _, _ -> download(activity, apk) }
                            .setNegativeButton("Más tarde", null)
                            .show()
                    } else if (!auto) {
                        Toast.makeText(activity, "Ya tienes la versión más reciente ✔", Toast.LENGTH_SHORT).show()
                    }
                } else if (!auto) {
                    Toast.makeText(activity, "No se pudo buscar: $errorUI", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun download(activity: Activity, apkUrl: String) {
        if (apkUrl.isBlank()) {
            Toast.makeText(activity, "URL del APK no configurada en Actualizaciones.kt", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("KNotes — actualización")
                .setDescription("Descargando la versión nueva…")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "KNotes-update.apk")
            dm.enqueue(req)
            Toast.makeText(activity, "Descargando… mira la barra de notificaciones 📥", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(activity, "No se pudo iniciar la descarga: ${e.message ?: ""}", Toast.LENGTH_SHORT).show()
        }
    }
}
