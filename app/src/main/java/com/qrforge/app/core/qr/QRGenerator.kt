package com.qrforge.app.core.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QRGenerator {

    fun generate(
        content: String,
        size: Int = 1024,
        darkColor: Int = android.graphics.Color.BLACK,
        lightColor: Int = android.graphics.Color.WHITE,
        cornerRadius: Float = 0f
    ): Bitmap? {
        if (content.isBlank()) return null

        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to
                        com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )

            // Request size=1 → ZXing returns the native module-size matrix
            // e.g. a 29-module QR with margin=1 on each side → bitMatrix.width = 31
            // Each pixel in bitMatrix = exactly 1 QR module
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                1, 1,   // ← forces native module resolution
                hints
            )

            // bitMatrix.width is now the actual module count (e.g. 31, 33, 37…)
            // We render each module at size/numModules pixels — cornerRadius works correctly
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
        val numModules = matrix.width          // actual module count, e.g. 33
        val cellSize   = size.toFloat() / numModules  // e.g. 1024/33 ≈ 31px

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        canvas.drawColor(lightColor)

        paint.color = darkColor

        for (x in 0 until numModules) {
            for (y in 0 until numModules) {
                if (matrix[x, y]) {
                    val left   = x * cellSize
                    val top    = y * cellSize
                    val right  = left + cellSize
                    val bottom = top  + cellSize
                    val rect   = RectF(left, top, right, bottom)

                    if (cornerRadius > 0f) {
                        // Clamp radius to half cell so it can't exceed circle
                        val r = cornerRadius.coerceAtMost(cellSize / 2f)
                        canvas.drawRoundRect(rect, r, r, paint)
                    } else {
                        canvas.drawRect(rect, paint)
                    }
                }
            }
        }

        return bitmap
    }
}