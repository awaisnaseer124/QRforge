package com.qrforge.app.core.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream

object SvgExporter {

    /**
     * Generates an SVG string from QR content and saves it to Downloads/QRForge/.
     * SVG is infinitely scalable — perfect for print materials.
     *
     * @param content   The QR content string
     * @param darkColor Hex color string e.g. "#000000"
     * @param size      SVG canvas size in pixels (default 500 — scales infinitely)
     */
    fun export(
        context: Context,
        content: String,
        fileName: String,
        darkColor: String = "#000000",
        lightColor: String = "#FFFFFF",
        size: Int = 500
    ): Uri? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to
                        com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = MultiFormatWriter().encode(
                content, BarcodeFormat.QR_CODE, size, size, hints
            )

            val matrixSize = bitMatrix.width
            val cellSize   = size.toFloat() / matrixSize
            val sb         = StringBuilder()

            // SVG header
            sb.append("""<?xml version="1.0" encoding="UTF-8"?>""")
            sb.append("\n")
            sb.append(
                """<svg xmlns="http://www.w3.org/2000/svg" """ +
                        """viewBox="0 0 $size $size" width="$size" height="$size">"""
            )
            sb.append("\n")

            // Background
            sb.append("""  <rect width="$size" height="$size" fill="$lightColor"/>""")
            sb.append("\n")

            // QR modules — each dark cell becomes a rect
            for (x in 0 until matrixSize) {
                for (y in 0 until matrixSize) {
                    if (bitMatrix[x, y]) {
                        val px = (x * cellSize).toInt()
                        val py = (y * cellSize).toInt()
                        val pw = cellSize.toInt() + 1 // +1 avoids hairline gaps
                        sb.append(
                            """  <rect x="$px" y="$py" width="$pw" height="$pw" fill="$darkColor"/>"""
                        )
                        sb.append("\n")
                    }
                }
            }

            sb.append("</svg>")

            val svgContent  = sb.toString().toByteArray(Charsets.UTF_8)
            val displayName = "${fileName}_${System.currentTimeMillis()}.svg"

            saveToDownloads(context, svgContent, displayName)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveToDownloads(
        context: Context,
        data: ByteArray,
        displayName: String
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, "image/svg+xml")
                    put(MediaStore.Downloads.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/QRForge")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return null
                context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
                uri
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "QRForge"
                )
                dir.mkdirs()
                val file = File(dir, displayName)
                FileOutputStream(file).use { it.write(data) }
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}