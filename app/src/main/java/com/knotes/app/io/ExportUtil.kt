package com.knotes.app.io

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import com.knotes.app.data.KPage
import com.knotes.app.data.Notebook
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/* ============================================================
   ExportUtil — guarda las notas en TXT / PDF / Word / JPG / PNG
   · PNG & JPG  → página actual renderizada a imagen
   · PDF        → diario completo (una hoja A4 por página)
   · TXT        → diario completo legible con los textos
   · DOCX       → Word mínimo (zip + WordprocessingML) sin librerías
   ============================================================ */
object ExportUtil {

    fun sanit(name: String): String =
        name.filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }.trim().take(28)
            .ifBlank { "KNotes" }

    fun dateTag(): String {
        val c = java.util.Calendar.getInstance()
        fun p(n: Int) = n.toString().padStart(2, '0')
        return "${c.get(java.util.Calendar.YEAR)}-${p(c.get(java.util.Calendar.MONTH) + 1)}-${p(c.get(java.util.Calendar.DAY_OF_MONTH))}"
    }

    fun notify(ctx: Context) = Toast.makeText(ctx, "✅ Exportado — revisa tus Descargas/Drive", Toast.LENGTH_SHORT).show()

    fun fail(ctx: Context, e: Exception) {
        Toast.makeText(ctx, "No se pudo exportar: ${e.message ?: "error"}", Toast.LENGTH_SHORT).show()
    }

    private fun os(ctx: Context, uri: Uri) = ctx.contentResolver.openOutputStream(uri)

    /* ---------------- imagen (pagina actual) ---------------- */

    fun writeImage(ctx: Context, uri: Uri, bmp: Bitmap, png: Boolean) = try {
        os(ctx, uri)?.use {
            bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                if (png) 100 else 92, it)
        }
        notify(ctx)
    } catch (e: Exception) { fail(ctx, e) } finally { bmp.recycle() }

    /* ---------------- PDF (diario completo) ---------------- */

    const val A4_W = 595
    const val A4_H = 842

    fun writePdf(ctx: Context, uri: Uri, draw: (index: Int) -> Bitmap, pages: Int) = try {
        os(ctx, uri)?.use { writePdfTo(it, draw, pages) }
        notify(ctx)
    } catch (e: Exception) { fail(ctx, e) }

    /** escribe un PDF A4 multipágina en el stream (usado por exportar y compartir) */
    private fun writePdfTo(out: java.io.OutputStream, draw: (index: Int) -> Bitmap, pages: Int) {
        val doc = PdfDocument()
        try {
            for (i in 0 until pages) {
                val info = PdfDocument.PageInfo.Builder(A4_W, A4_H, i + 1).create()
                val pg = doc.startPage(info)
                val c = pg.canvas
                c.drawColor(0xFFF6F1E7.toInt())
                val bmp = draw(i)
                // centrar la hoja preservando margen
                val m = 40f
                val availW = A4_W - m * 2
                val availH = A4_H - m * 2
                val sw = availW / bmp.width
                val sh = availH / bmp.height
                val s = minOf(sw, sh)
                val dw = bmp.width * s; val dh = bmp.height * s
                val dx = (A4_W - dw) / 2f; val dy = (A4_H - dh) / 2f
                c.drawBitmap(bmp, null, android.graphics.RectF(dx, dy, dx + dw, dy + dh), null)
                // pie de página
                val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF9A9288.toInt(); textSize = 9f; textAlign = android.graphics.Paint.Align.CENTER
                }
                c.drawText("KNotes · pág. ${i + 1}/$pages", A4_W / 2f, A4_H - 16f, p)
                doc.finishPage(pg)
                bmp.recycle()
            }
            doc.writeTo(out)
        } finally { doc.close() }
    }

    /* ---------------- Compartir (pdf/png/jpg/txt) ----------------
       Se usa en la selección múltiple de «Todos los diarios»:
       genera los archivos en cacheDir/export y los comparte,
       funcionando igual con 1 o con varios diarios seleccionados. */
    fun shareDiaries(activity: android.app.Activity, ids: List<String>) {
        try {
            val dir = java.io.File(activity.cacheDir, "export")
            dir.mkdirs()
            val uris = ArrayList<Uri>()
            val store = com.knotes.app.data.Store
            for (id in ids.take(10)) {
                val nb = store.byId(id) ?: continue
                val pages = store.loadPages(nb)
                val base = sanit(nb.title).replace(' ', '_')
                // instance detached para renderizar páginas fuera de la pantalla
                val v = com.knotes.app.canvas.CanvasPageView(activity)
                v.setup(nb, pages, 0)

                val txtF = java.io.File(dir, "$base.txt")
                java.io.FileOutputStream(txtF).use {
                    it.write(buildTxt(nb, pages).toByteArray(Charsets.UTF_8))
                }
                val pdfF = java.io.File(dir, "$base.pdf")
                java.io.FileOutputStream(pdfF).use {
                    writePdfTo(it, { i -> v.renderPageBitmap(i, 1000) }, pages.size)
                }
                val pngF = java.io.File(dir, "$base.png")
                java.io.FileOutputStream(pngF).use {
                    val b = v.renderPageBitmap(0, 1200)
                    b.compress(Bitmap.CompressFormat.PNG, 100, it)
                    b.recycle()
                }
                val jpgF = java.io.File(dir, "${base}_p1.jpg")
                java.io.FileOutputStream(jpgF).use {
                    val b = v.renderPageBitmap(0, 1200)
                    b.compress(Bitmap.CompressFormat.JPEG, 90, it)
                    b.recycle()
                }
                val auth = "${activity.packageName}.files"
                uris.add(androidx.core.content.FileProvider.getUriForFile(activity, auth, txtF))
                uris.add(androidx.core.content.FileProvider.getUriForFile(activity, auth, pdfF))
                uris.add(androidx.core.content.FileProvider.getUriForFile(activity, auth, pngF))
                uris.add(androidx.core.content.FileProvider.getUriForFile(activity, auth, jpgF))
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            intent.clipData = android.content.ClipData.newRawUri("KNotes", uris.first())
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.type = "*/*"
            activity.startActivity(Intent.createChooser(intent, "Compartir diarios"))
            Toast.makeText(activity, "📄 PDF · 🖼 PNG/JPG · 📝 TXT listos para compartir", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            fail(activity, e)
        }
    }

    /* ---------------- TXT (diario completo) ---------------- */

    fun buildTxt(nb: Notebook, pages: MutableList<KPage>): String {
        val sb = StringBuilder()
        sb.append("KNotes · ${nb.title}\n")
        sb.append(java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(System.currentTimeMillis())))
        sb.append("\nCreado: ")
        sb.append(java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(nb.createdAt)))
        sb.append("\n\n")
        val tpls = arrayOf("Rayado", "Cuadrícula", "Puntos", "En blanco")
        pages.forEachIndexed { i, page ->
            sb.append("────── Página ${i + 1} (${tpls[page.template.coerceIn(0, 3)]}) ──────\n")
            var strokes = 0; var imgs = 0
            for (el in page.items) {
                when (el) {
                    is com.knotes.app.data.Stroke -> strokes++
                    is com.knotes.app.data.ImageElement -> imgs++
                    is com.knotes.app.data.StickerElement -> sb.append("[sticker] ${el.emoji}\n")
                    is com.knotes.app.data.LinkElement -> sb.append("[enlace] ${el.label} → ${el.url}\n")
                    is com.knotes.app.data.TextElement -> sb.append(el.text).append('\n')
                }
            }
            if (strokes > 0 || imgs > 0) sb.append("(✏️ $strokes trazos, 🖼 $imgs imágenes)\n")
            sb.append('\n')
        }
        return sb.toString()
    }

    fun writeTxt(ctx: Context, uri: Uri, text: String) = try {
        os(ctx, uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        notify(ctx)
    } catch (e: Exception) { fail(ctx, e) }

    /* ---------------- DOCX (diario completo, Word) ---------------- */

    private fun esc(x: String) = x
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    fun buildDocx(nb: Notebook, pages: MutableList<KPage>): ByteArray {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>""")
        sb.append("""<w:p><w:pPr><w:jc w:val="center"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val="40"/></w:rPr><w:t xml:space="preserve">${esc(nb.title)}</w:t></w:r></w:p>""")
        pages.forEachIndexed { i, page ->
            sb.append("""<w:p><w:pPr><w:spacing w:before="240"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val="26"/></w:rPr><w:t xml:space="preserve">Página ${i + 1}</w:t></w:r></w:p>""")
            for (el in page.items) {
                when (el) {
                    is com.knotes.app.data.TextElement ->
                        for (ln in el.text.split('\n'))
                            sb.append("""<w:p><w:r><w:t xml:space="preserve">${esc(ln)}</w:t></w:r></w:p>""")
                    is com.knotes.app.data.StickerElement ->
                        sb.append("""<w:p><w:r><w:t xml:space="preserve">${esc(el.emoji)}</w:t></w:r></w:p>""")
                    is com.knotes.app.data.LinkElement ->
                        sb.append("""<w:p><w:r><w:t xml:space="preserve">🔗 ${esc(el.label)} — ${esc(el.url)}</w:t></w:r></w:p>""")
                    else -> {}
                }
            }
            val counts = page.items.count { it is com.knotes.app.data.Stroke }
            val imgs = page.items.count { it is com.knotes.app.data.ImageElement }
            if (counts > 0 || imgs > 0)
                sb.append("""<w:p><w:r><w:rPr><w:i/><w:color w:val="9A9288"/></w:rPr><w:t xml:space="preserve">($counts trazos, $imgs imágenes incluidas en esta página)</w:t></w:r></w:p>""")
        }
        sb.append("""</w:body></w:document>""")

        val cts = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""
        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            z.putNextEntry(ZipEntry("[Content_Types].xml")); z.write(cts.toByteArray(Charsets.UTF_8)); z.closeEntry()
            z.putNextEntry(ZipEntry("_rels/.rels")); z.write(rels.toByteArray(Charsets.UTF_8)); z.closeEntry()
            z.putNextEntry(ZipEntry("word/document.xml")); z.write(sb.toString().toByteArray(Charsets.UTF_8)); z.closeEntry()
        }
        return bos.toByteArray()
    }

    fun writeBytes(ctx: Context, uri: Uri, data: ByteArray) = try {
        os(ctx, uri)?.use { it.write(data) }
        notify(ctx)
    } catch (e: Exception) { fail(ctx, e) }
}
