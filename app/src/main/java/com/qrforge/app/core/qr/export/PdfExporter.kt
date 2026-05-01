package com.qrforge.app.core.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    // A4 at 72 dpi
    private const val PAGE_WIDTH  = 595
    private const val PAGE_HEIGHT = 842

    /**
     * Renders the QR bitmap onto an A4 PDF page with a title label.
     * Saves to Downloads/QRForge/ and returns the Uri.
     *
     * @param qrBitmap  The QR code bitmap (with logo if applicable)
     * @param title     Label shown below the QR on the page
     * @param subtitle  Optional second line (e.g. URL)
     */
    fun export(
        context: Context,
        qrBitmap: Bitmap,
        fileName: String,
        title: String = "QRForge",
        subtitle: String = ""
    ): Uri? {
        return try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page     = document.startPage(pageInfo)
            val canvas   = page.canvas

            // White background
            canvas.drawColor(android.graphics.Color.WHITE)

            // QR size — 60% of page width, centered
            val qrSize = (PAGE_WIDTH * 0.6f).toInt()
            val qrLeft = (PAGE_WIDTH - qrSize) / 2
            val qrTop  = (PAGE_HEIGHT - qrSize) / 2 - 40   // slightly above center

            // Scale and draw QR bitmap
            val scaledQr = Bitmap.createScaledBitmap(qrBitmap, qrSize, qrSize, true)
            canvas.drawBitmap(
                scaledQr,
                null,
                Rect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
            scaledQr.recycle()

            // Title text
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color       = android.graphics.Color.BLACK
                textSize    = 22f
                isFakeBoldText = true
                textAlign   = Paint.Align.CENTER
            }
            canvas.drawText(title, PAGE_WIDTH / 2f, (qrTop + qrSize + 40).toFloat(), titlePaint)

            // Subtitle text
            if (subtitle.isNotBlank()) {
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color     = android.graphics.Color.GRAY
                    textSize  = 14f
                    textAlign = Paint.Align.CENTER
                }
                val subText = if (subtitle.length > 60)
                    subtitle.take(57) + "..." else subtitle
                canvas.drawText(subText, PAGE_WIDTH / 2f, (qrTop + qrSize + 64).toFloat(), subPaint)
            }

            // Footer
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color     = android.graphics.Color.LTGRAY
                textSize  = 10f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "Generated with QRForge",
                PAGE_WIDTH / 2f,
                (PAGE_HEIGHT - 20).toFloat(),
                footerPaint
            )

            document.finishPage(page)

            val displayName = "${fileName}_${System.currentTimeMillis()}.pdf"
            val uri = saveToDownloads(context, document, displayName)
            document.close()
            uri

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveToDownloads(
        context: Context,
        document: PdfDocument,
        displayName: String
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/QRForge")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return null
                context.contentResolver.openOutputStream(uri)?.use {
                    document.writeTo(it)
                }
                uri
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "QRForge"
                )
                dir.mkdirs()
                val file = File(dir, displayName)
                FileOutputStream(file).use { document.writeTo(it) }
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}