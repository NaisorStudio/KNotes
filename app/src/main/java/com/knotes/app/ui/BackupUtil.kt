package com.knotes.app.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.knotes.app.data.ImageElement
import com.knotes.app.data.KPage
import com.knotes.app.data.Notebook
import com.knotes.app.data.Store
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Base64

/* ============================================================
   BackupUtil — respaldo y restauración de TODOS los diarios
   · JSON : completo (metas + páginas + imágenes en base64) → restaurable
   · TXT  : legible con los textos de todo
   · HTML : legible, con imágenes embebidas
   Los diarios van en orden de CREACIÓN (createdAt ascendente).
   Restaurar: si el diario existe aquí, se importa como copia
   "(restaurado)" — nunca pisa lo que ya tienes.
   ============================================================ */
object BackupUtil {

    fun dateTag(): String {
        val c = java.util.Calendar.getInstance()
        fun p(n: Int) = n.toString().padStart(2, '0')
        return "${c.get(java.util.Calendar.YEAR)}-${p(c.get(java.util.Calendar.MONTH) + 1)}-${p(c.get(java.util.Calendar.DAY_OF_MONTH))}"
    }

    fun notify(ctx: Context) = Toast.makeText(ctx, "✅ Respaldo listo — revisa tus Descargas/Drive", Toast.LENGTH_SHORT).show()
    fun fail(ctx: Context, e: Exception) = Toast.makeText(ctx, "Error: ${e.message ?: "desconocido"}", Toast.LENGTH_SHORT).show()

    private fun allInOrder(): List<Notebook> = Store.metaValues().sortedBy { it.createdAt }

    /* ================= CREAR ================= */

    fun buildJson(nn: List<Notebook>): String {
        val root = JSONObject()
        root.put("app", "KNotes-backup"); root.put("v", 1)
        root.put("created", System.currentTimeMillis())
        val arr = JSONArray()
        for (nb in nn) {
            val pages = Store.loadPages(nb)
            val o = JSONObject()
            o.put("meta", nb.toJson())
            val parr = JSONArray()
            for (p in pages) parr.put(p.toJson())
            o.put("pages", parr)
            // imágenes → base64 para que el respaldo sea autónomo
            val imgs = JSONObject()
            for (p in pages) for (el in p.items) {
                if (el is ImageElement) {
                    val f = File(el.path)
                    if (f.exists() && f.length() < 6_000_000) {
                        try { imgs.put(el.id, Base64.getEncoder().encodeToString(f.readBytes())) } catch (_: Exception) {}
                    }
                }
            }
            o.put("images", imgs)
            arr.put(o)
        }
        root.put("notebooks", arr)
        return root.toString()
    }

    fun buildTxt(nn: List<Notebook>): String {
        val sb = StringBuilder()
        sb.append("KNotes · RESPALDO (${dateTag()})\n")
        sb.append("Diarios en orden de creación:\n\n")
        nn.forEachIndexed { idx, nb ->
            sb.append("════════ ${idx + 1}. ${nb.title} ════════\n\n")
            sb.append(noteAsText(nb, Store.loadPages(nb)))
            sb.append('\n')
        }
        return sb.toString()
    }

    fun buildHtml(nn: List<Notebook>): String {
        val sb = StringBuilder()
        sb.append("<html><head><meta charset='utf-8'><title>Respaldo KNotes</title></head>")
        sb.append("<body style='font-family:sans-serif;max-width:760px;margin:24px auto'>")
        sb.append("<h1>💾 Respaldo de KNotes</h1><p>${dateTag()} · ${nn.size} diarios en orden de creación</p><hr>")
        nn.forEachIndexed { idx, nb ->
            val pages = Store.loadPages(nb)
            sb.append(noteAsHtml(nb, pages, "$idx + 1. "))
        }
        sb.append("</body></html>")
        return sb.toString()
    }

    fun writeAll(ctx: Context, uri: Uri, kind: Int) = try {
        val nn = allInOrder()
        val data: ByteArray = when (kind) {
            1 -> buildTxt(nn).toByteArray(Charsets.UTF_8)
            2 -> buildHtml(nn).toByteArray(Charsets.UTF_8)
            else -> buildJson(nn).toByteArray(Charsets.UTF_8)
        }
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(data) }
        notify(ctx)
    } catch (e: Exception) { fail(ctx, e) }

    /* ================= RESTAURAR ================= */

    fun restoreFrom(ctx: Context, uri: Uri) {
        try {
        val text = ctx.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: run {
            Toast.makeText(ctx, "No se pudo leer el archivo", Toast.LENGTH_SHORT).show()
            return
        }
        if (!text.contains("KNotes-backup")) {
            Toast.makeText(ctx, "El archivo no es un respaldo de KNotes (.json)", Toast.LENGTH_SHORT).show()
            return
        }
        val root = JSONObject(text)
        val arr = root.optJSONArray("notebooks") ?: run {
            Toast.makeText(ctx, "El respaldo no tiene diarios", Toast.LENGTH_SHORT).show()
            return
        }
        var added = 0; var copied = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            try {
                val meta = Notebook.fromJson(o.getJSONObject("meta"))
                val parr = o.getJSONArray("pages")
                val pages = ArrayList<KPage>()
                for (j in 0 until parr.length()) pages.add(KPage.fromJson(parr.getJSONObject(j)))
                val imgs = o.optJSONObject("images")
                // reconstruir archivos de imagen
                if (imgs != null) {
                    val dir = Store.dirOf(meta.id)
                    val keys = imgs.keys()
                    while (keys.hasNext()) {
                        val elId = keys.next()
                        try {
                            val bytes = Base64.getDecoder().decode(imgs.getString(elId))
                            val f = File(dir, "img_r_${elId.takeLast(8)}.jpg")
                            f.writeBytes(bytes)
                            pages.forEach { pg -> pg.items.forEach { el ->
                                if (el is ImageElement && el.id == elId) el.path = f.absolutePath
                            } }
                        } catch (_: Exception) {}
                    }
                }
                if (Store.byId(meta.id) != null) {
                    // existe aquí → se importa como copia, sin pisar nada
                    val copy = Store.createNotebook(meta.title + " (restaurado)", meta.coverStyle,
                        meta.tags.ifEmpty { java.util.ArrayList() }, System.nanoTime())
                    Store.savePages(copy, pages)
                    copy.favorite = meta.favorite
                    Store.saveMeta(copy)
                    copied++
                } else {
                    Store.adoptNotebook(meta, java.util.ArrayList(pages))
                    added++
                }
            } catch (_: Exception) {}
        }
        Toast.makeText(ctx, "✅ Restaurado: $added nuevos, $copied copias", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(ctx, "No se pudo leer el respaldo: ${e.message ?: "error"}", Toast.LENGTH_SHORT).show()
        }
    }

    /* ---------------- respaldo mínimo de una nota (texto) ---------------- */

    fun noteAsText(nb: Notebook, pages: MutableList<KPage>): String {
        val sb = StringBuilder()
        sb.append("KNotes · ${nb.title}\n\n")
        pages.forEachIndexed { i, page ->
            sb.append("── Página ${i + 1} ──\n")
            page.items.forEach { el ->
                when (el) {
                    is com.knotes.app.data.TextElement -> sb.append(el.text).append('\n')
                    is com.knotes.app.data.StickerElement -> sb.append(el.emoji).append('\n')
                    is com.knotes.app.data.LinkElement -> sb.append("[enlace] ${el.label} → ${el.url}\n")
                    is com.knotes.app.data.ImageElement -> sb.append("[imagen]\n")
                    is com.knotes.app.data.Stroke -> sb.append("[trazo con ${el.points.size} puntos]\n")
                }
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    fun noteAsHtml(nb: Notebook, pages: MutableList<KPage>, headingStar: String = ""): String {
        val esc = { s: String -> s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") }
        val sb = StringBuilder()
        sb.append("<h2>${esc(headingStar + nb.title)}</h2>")
        pages.forEachIndexed { i, page ->
            sb.append("<h4>Página ${i + 1}</h4>")
            page.items.forEach { el ->
                when (el) {
                    is com.knotes.app.data.TextElement -> {
                        for (ln in el.text.split('\n')) sb.append("<p>${esc(ln)}</p>")
                    }
                    is com.knotes.app.data.StickerElement -> sb.append("<p style='font-size:26px'>${el.emoji}</p>")
                    is com.knotes.app.data.LinkElement -> sb.append("<p>🔗 <a href='${esc(el.url)}'>${esc(el.label)}</a></p>")
                    is com.knotes.app.data.ImageElement -> {
                        val f = File(el.path)
                        if (f.exists()) {
                            try {
                                val b64 = Base64.getEncoder().encodeToString(f.readBytes())
                                sb.append("<img src='data:image/jpeg;base64,$b64' style='max-width:100%'/><br>")
                            } catch (_: Exception) {}
                        }
                    }
                    else -> {}
                }
            }
            sb.append("<hr>")
        }
        return sb.toString()
    }
}
