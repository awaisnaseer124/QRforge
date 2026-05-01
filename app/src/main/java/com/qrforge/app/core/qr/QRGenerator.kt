package com.qrforge.app.core.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QRGenerator {

    private const val DEFAULT_SIZE = 1024

    /**
     * Generates a QR code Bitmap from the given content string.
     * @param content   The string to encode (URL, vCard, Wi-Fi etc.)
     * @param size      Output bitmap size in pixels (square)
     * @param darkColor QR module color (ARGB int)
     * @param lightColor Background color (ARGB int)
     * @param cornerRadius Rounded corner radius for modules (0 = square)
     */
    fun generate(
        content: String,
        size: Int = DEFAULT_SIZE,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE,
        cornerRadius: Float = 0f
    ): Bitmap? {
        if (content.isBlank()) return null

        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )

            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            renderBitmap(bitMatrix, size, darkColor, lightColor, cornerRadius)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun renderBitmap(
        matrix: BitMatrix,
        size: Int,
        darkColor: Int,
        lightColor: Int,
        cornerRadius: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Fill background
        canvas.drawColor(lightColor)

        val matrixSize = matrix.width
        val cellSize = size.toFloat() / matrixSize

        paint.color = darkColor

        for (x in 0 until matrixSize) {
            for (y in 0 until matrixSize) {
                if (matrix[x, y]) {
                    val left   = x * cellSize
                    val top    = y * cellSize
                    val right  = left + cellSize
                    val bottom = top + cellSize

                    if (cornerRadius > 0f) {
                        val rect = RectF(left, top, right, bottom)
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                    } else {
                        canvas.drawRect(left, top, right, bottom, paint)
                    }
                }
            }
        }

        return bitmap
    }
}