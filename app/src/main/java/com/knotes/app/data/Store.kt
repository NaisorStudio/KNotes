package com.knotes.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import org.json.JSONObject
import java.io.File
import kotlin.random.Random
import kotlin.math.min

/* ============================================================
   KNotes — almacenamiento
   Estructura en el almacenamiento privado de la app:
     notebooks/<id>/meta.json      (título, portada, etiquetas...)
     notebooks/<id>/p0.json ...    (una página = un JSON de elementos)
     notebooks/<id>/img_xxx.jpg    (imágenes importadas)
   ============================================================ */

object Store {

    private lateinit var appCtx: Context
    private val meta: LinkedHashMap<String, Notebook> = LinkedHashMap()

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        reload()
    }

    val context: Context get() = appCtx

    fun root(): File = File(appCtx.filesDir, "notebooks").apply { mkdirs() }

    fun dirOf(id: String): File = File(root(), id).apply { mkdirs() }

    fun reload() {
        meta.clear()
        for (d in root().listFiles() ?: emptyArray()) {
            val f = File(d, "meta.json")
            if (!f.exists()) continue
            try { meta.put(d.name, Notebook.fromJson(JSONObject(f.readText()))) } catch (_: Exception) { }
        }
    }

    fun all(): List<Notebook> = meta.values.filter { it.deletedAt == null }.sortedBy { it.updatedAt }.asReversed()

    fun trashed(): List<Notebook> = meta.values.filter { it.deletedAt != null }.sortedBy { it.deletedAt ?: 0 }.asReversed()

    fun byId(id: String): Notebook? = meta[id]

    /** Todos los metas registrados (incluida la papelera), para el respaldo. */
    fun metaValues(): List<Notebook> = ArrayList(meta.values)

    /** Registra y escribe un cuaderno que vino de un respaldo (conserva id y fechas). */
    fun adoptNotebook(nb: Notebook, pages: MutableList<KPage>) {
        dirOf(nb.id)
        File(dirOf(nb.id), "meta.json").writeText(nb.toJson().toString())
        savePages(nb, pages)
        meta[nb.id] = nb
    }

    /* ---------- creación / guardado ---------- */

    fun newNotebookId(): String = "n${System.nanoTime().toString(36)}${Random.nextInt(0, 46656).toString(36)}"

    fun createNotebook(title: String, coverStyle: Int, tags: List<String> = ArrayList(),
                       coverSeed: Long = Random.nextLong(),
                       pages: MutableList<KPage>? = null): Notebook {
        val now = System.currentTimeMillis()
        val nb = Notebook(newNotebookId(), title, coverStyle, coverSeed, ArrayList(tags), false, now, now)
        dirOf(nb.id)
        File(dirOf(nb.id), "meta.json").writeText(nb.toJson().toString())
        savePages(nb, pages ?: mutableListOf(KPage(Ids.next(), 0)))
        meta[nb.id] = nb
        return nb
    }

    fun saveMeta(nb: Notebook) {
        nb.updatedAt = System.currentTimeMillis()
        File(dirOf(nb.id), "meta.json").writeText(nb.toJson().toString())
        meta[nb.id] = nb
    }

    fun savePages(nb: Notebook, pages: MutableList<KPage>) {
        val dir = dirOf(nb.id)
        for (f in dir.listFiles() ?: emptyArray()) if (f.name.startsWith("p") && f.name.endsWith(".json")) f.delete()
        pages.forEachIndexed { i, p -> File(dir, "p$i.json").writeText(p.toJson().toString()) }
        nb.updatedAt = System.currentTimeMillis()
        File(dir, "meta.json").writeText(nb.toJson().toString())
    }

    fun loadPages(nb: Notebook): MutableList<KPage> {
        val out = ArrayList<KPage>()
        val dir = dirOf(nb.id)
        var i = 0
        while (true) {
            val f = File(dir, "p$i.json")
            if (!f.exists()) break
            try { out.add(KPage.fromJson(JSONObject(f.readText()))) } catch (_: Exception) { }
            i++
            if (i > 500) break
        }
        if (out.isEmpty()) out.add(KPage(Ids.next(), 0))
        return out
    }

    /** Importa una imagen del dispositivo al cuaderno (JPG, PNG, WEBP, HEIC/HEIF, AVIF, BMP, GIF…)
     *  Devuelve (rutaArchivo, anchoPapel, altoPapel). Gira automáticamente fotos con EXIF. */
    fun importImage(ctx: Context, nb: Notebook, uri: Uri): Triple<String, Float, Float>? {
        return try {
            var bmp: Bitmap? = null

            /* 1) Descodificador MODERNO (Android 9+): lee HEIC/HEIF, AVIF, WEBP,
               BMP, GIF (primer fotograma) y JPGs que BitmapFactory no logró decodificar */
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                try {
                    val src = ImageDecoder.createSource(ctx.contentResolver, uri)
                    bmp = ImageDecoder.decodeBitmap(src) { dec, info, _ ->
                        try {
                            val longest = maxOf(info.size.width, info.size.height)
                            var sample = 1
                            while (longest / (sample * 2) >= 1400) sample *= 2
                            dec.setTargetSampleSize(sample)
                            // software: necesario para rotar/comprimir después
                            dec.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }

            /* 2) Clásico (API 24-27) y reintento si el moderno falló */
            if (bmp == null) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                var sample = 1
                val longest = maxOf(opts.outWidth, opts.outHeight)
                if (longest > 0) while (longest / (sample * 2) >= 1400) sample *= 2
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                bmp = ctx.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }
            }
            if (bmp == null) return null

            /* 3) EXIF: las fotos de la cámara pueden venir GIRADAS — se enderezan */
            var orientation = 0
            try {
                ctx.contentResolver.openInputStream(uri)?.use {
                    orientation = android.media.ExifInterface(it)
                        .getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 0)
                }
            } catch (_: Exception) {}
            val degrees = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            /* 4) Lienzo: endereza con el giro y deja fondo BLANCO (PNG con transparencia
               no se pone negra al compactar a JPG) */
            val w0 = bmp.width; val h0 = bmp.height
            val out = Bitmap.createBitmap(w0, h0, Bitmap.Config.ARGB_8888)
            val cv = android.graphics.Canvas(out)
            cv.drawColor(android.graphics.Color.WHITE)
            val mx = android.graphics.Matrix().apply {
                postRotate(degrees, w0 / 2f, h0 / 2f)
            }
            cv.drawBitmap(bmp, mx, null)
            bmp.recycle()
            var final = out
            // el giro puede agrandar el lienzo: escala final al límite 1400
            val fl = maxOf(final.width, final.height)
            if (fl > 1400) {
                val s = 1400f / fl
                val scaled = Bitmap.createScaledBitmap(final, (final.width * s).toInt().coerceAtLeast(1),
                    (final.height * s).toInt().coerceAtLeast(1), true)
                final = scaled
            }

            val dir = dirOf(nb.id)
            val f = File(dir, "img_${System.nanoTime().toString(36)}.jpg")
            f.outputStream().use { final.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            val w = final.width.toFloat(); val h = final.height.toFloat()
            final.recycle()
            // en el PAPEl se muestra a máx 620 de su lado mayor (regla que usa el editor)
            val t620 = min(1f, 620f / maxOf(w, h))
            Triple(f.absolutePath, w * t620, h * t620)
        } catch (_: Exception) { null }
    }

    /* ---------- papelera / papeleo ---------- */

    fun toTrash(nb: Notebook) {
        nb.deletedAt = System.currentTimeMillis()
        saveMeta(nb)
    }

    fun restore(nb: Notebook) {
        nb.deletedAt = null
        saveMeta(nb)
    }

    fun deleteForever(nb: Notebook) {
        val dir = File(root(), nb.id)
        dir.deleteRecursively()
        meta.remove(nb.id)
    }

    fun emptyTrash() {
        trashed().forEach { deleteForever(it) }
    }

    fun duplicate(nb: Notebook): Notebook? {
        val me = meta[nb.id] ?: return null
        val pages = loadPages(me).let { list -> ArrayList(list.map { KPage(it.id, it.template, it.paper, it.decor, ArrayList(it.items.map { el -> el.clone() })) }) }
        val clone = createNotebook(if (me.title.endsWith(" (copia)")) me.title else me.title + " (copia)",
            me.coverStyle, me.tags, Random.nextLong())
        savePages(clone, pages.toMutableList())
        saveMeta(clone)
        return clone
    }

    fun allTags(): List<String> {
        val s = LinkedHashSet<String>()
        all().forEach { nb -> nb.tags.forEach { if (it.isNotBlank()) s.add(it) } }
        return ArrayList(s)
    }
}
