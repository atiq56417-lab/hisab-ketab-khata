package com.hisabkitab.khata.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.view.View
import androidx.core.content.FileProvider
import com.hisabkitab.khata.R
import com.hisabkitab.khata.data.CustomerBalance
import com.hisabkitab.khata.data.Formats
import com.hisabkitab.khata.data.LedgerTransaction
import com.hisabkitab.khata.data.Money
import com.hisabkitab.khata.data.TxType
import java.io.File
import kotlin.math.max

/**
 * Builds a customer account statement as a PDF with Android's PdfDocument.
 * Text is shaped by the platform text engine, so Urdu renders correctly; columns are mirrored in RTL.
 */
object StatementPdf {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f
    private val FRACTIONS = floatArrayOf(0.22f, 0.30f, 0.16f, 0.16f, 0.16f)

    fun create(ctx: Context, c: CustomerBalance, txs: List<LedgerTransaction>): File {
        val rtl = ctx.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val sym = ctx.getString(R.string.currency_symbol)
        fun money(p: Long) = Money.format(p)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; isFakeBoldText = true }
        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; isFakeBoldText = true }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        val line = Paint().apply { strokeWidth = 0.7f }

        val contentW = PAGE_W - 2 * MARGIN
        val starts = FloatArray(5)
        var acc = 0f
        for (i in 0..4) {
            val w = FRACTIONS[i] * contentW
            starts[i] = if (rtl) MARGIN + contentW - acc - w else MARGIN + acc
            acc += w
        }

        fun fit(text: String, p: Paint, width: Float): String {
            val n = p.breakText(text, true, width, null)
            return if (n < text.length) text.substring(0, max(0, n - 1)) + "…" else text
        }

        fun cell(canvas: Canvas, text: String, i: Int, y: Float, p: Paint) {
            val w = FRACTIONS[i] * contentW
            p.textAlign = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
            val x = if (rtl) starts[i] + w - 3f else starts[i] + 3f
            canvas.drawText(fit(text, p, w - 6f), x, y, p)
        }

        fun fullLine(canvas: Canvas, text: String, y: Float, p: Paint) {
            p.textAlign = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
            canvas.drawText(text, if (rtl) PAGE_W - MARGIN else MARGIN, y, p)
        }

        val headers = listOf(
            ctx.getString(R.string.date), ctx.getString(R.string.description),
            ctx.getString(R.string.credit_given), ctx.getString(R.string.payment), ctx.getString(R.string.balance)
        )

        val doc = PdfDocument()
        var pageNo = 0
        lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        var y = 0f

        fun tableHeader() {
            for (i in 0..4) cell(canvas, headers[i], i, y, bold)
            y += 6f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, line)
            y += 14f
        }

        fun newPage() {
            if (pageNo > 0) doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            canvas = page.canvas
            y = MARGIN + 10f
            if (pageNo == 1) {
                fullLine(canvas, ctx.getString(R.string.app_name), y, titlePaint); y += 22f
                fullLine(canvas, ctx.getString(R.string.statement), y, bold); y += 16f
                fullLine(canvas, ctx.getString(R.string.shops) + ": " + c.shopName, y, body); y += 14f
                fullLine(canvas, c.name + if (c.phone.isNotBlank()) "  |  " + c.phone else "", y, bold); y += 14f
                fullLine(canvas, ctx.getString(R.string.generated_on) + ": " + Formats.dateTime(System.currentTimeMillis()), y, body); y += 14f
                fullLine(canvas, ctx.getString(R.string.balance) + ": " + sym + " " + money(c.balance) + "  (" +
                    ctx.getString(
                        when {
                            c.balance > 0 -> R.string.customer_owes
                            c.balance < 0 -> R.string.you_owe
                            else -> R.string.settled
                        }
                    ) + ")", y, bold)
                y += 22f
            }
            tableHeader()
        }

        newPage()

        var running = c.openingBalance
        // Opening balance row
        cell(canvas, "", 0, y, body)
        cell(canvas, ctx.getString(R.string.opening_balance), 1, y, body)
        cell(canvas, money(running), 4, y, body)
        y += 16f

        var totalCredit = 0L
        var totalPaid = 0L
        for (t in txs) {
            if (t.voided) continue
            if (y > PAGE_H - MARGIN - 40f) newPage()
            val credit = t.type == TxType.GIVE_CREDIT
            if (credit) { running += t.amount; totalCredit += t.amount } else { running -= t.amount; totalPaid += t.amount }
            val desc = listOf(t.description, t.reference).filter { it.isNotBlank() }.joinToString(" - ")
            cell(canvas, Formats.dateTime(t.timestamp), 0, y, body)
            cell(canvas, desc, 1, y, body)
            cell(canvas, if (credit) money(t.amount) else "", 2, y, body)
            cell(canvas, if (credit) "" else money(t.amount), 3, y, body)
            cell(canvas, money(running), 4, y, body)
            y += 16f
        }

        if (y > PAGE_H - MARGIN - 50f) newPage()
        y += 4f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, line)
        y += 14f
        cell(canvas, "", 0, y, bold)
        cell(canvas, "", 1, y, bold)
        cell(canvas, money(totalCredit), 2, y, bold)
        cell(canvas, money(totalPaid), 3, y, bold)
        cell(canvas, money(running), 4, y, bold)
        doc.finishPage(page)

        val dir = File(ctx.cacheDir, "pdf").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "statement_${c.id}_${Formats.fileStamp()}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** Opens the system share sheet; the user can pick WhatsApp, email, Drive, a print service, etc. */
    fun share(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, null))
    }
}
