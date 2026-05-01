package com.qrforge.app.core.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import kotlin.math.min

object LogoOverlayProcessor {

    /**
     * Composites a logo bitmap onto the center of a QR code bitmap.
     * The logo occupies ~22% of the QR width — safe zone for error correction.
     *
     * @param qrBitmap   The generated QR code bitmap
     * @param logoBitmap The user's logo bitmap
     * @param logoSizeRatio  Logo size as fraction of QR width (default 0.22)
     * @param cornerRadius   Logo corner radius in pixels (0 = square, -1 = circle)
     * @param addWhitePadding Whether to add white padding ring around logo
     */
    fun overlay(
        qrBitmap: Bitmap,
        logoBitmap: Bitmap,
        logoSizeRatio: Float = 0.22f,
        cornerRadius: Float = 16f,
        addWhitePadding: Boolean = true
    ): Bitmap {
        val result = qrBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val qrSize    = min(qrBitmap.width, qrBitmap.height)
        val logoSize  = (qrSize * logoSizeRatio).toInt()
        val padding   = if (addWhitePadding) (logoSize * 0.15f).toInt() else 0
        val totalSize = logoSize + padding * 2

        // Center position
        val left = (qrBitmap.width  - totalSize) / 2f
        val top  = (qrBitmap.height - totalSize) / 2f

        // Draw white padding background
        if (addWhitePadding) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
            }
            val bgRect = RectF(left, top, left + totalSize, top + totalSize)
            canvas.drawRoundRect(bgRect, cornerRadius + padding, cornerRadius + padding, bgPaint)
        }

        // Scale logo to target size
        val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, logoSize, logoSize, true)

        // Draw rounded logo
        val logoLeft = left + padding
        val logoTop  = top  + padding
        val logoRect = RectF(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)

        val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val saveCount = canvas.saveLayer(logoRect, null)

        canvas.drawRoundRect(logoRect, cornerRadius, cornerRadius, logoPaint)
        logoPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaledLogo, null, logoRect, logoPaint)
        logoPaint.xfermode = null

        canvas.restoreToCount(saveCount)
        scaledLogo.recycle()

        return result
    }

    /**
     * Loads a logo bitmap from a content URI (from gallery picker)
     */
    fun loadFromUri(context: Context, uri: Uri, maxSize: Int = 512): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val raw = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (raw == null) return null

            // Scale down if needed
            val scale = min(maxSize.toFloat() / raw.width, maxSize.toFloat() / raw.height)
            if (scale < 1f) {
                val scaled = Bitmap.createScaledBitmap(
                    raw,
                    (raw.width * scale).toInt(),
                    (raw.height * scale).toInt(),
                    true
                )
                raw.recycle()
                scaled
            } else {
                raw
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}