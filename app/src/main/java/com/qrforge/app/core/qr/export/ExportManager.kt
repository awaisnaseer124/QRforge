package com.qrforge.app.core.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ExportManager {

    /**
     * Saves the QR Bitmap to the device gallery (Pictures/QRForge/).
     * Returns the Uri of the saved image, or null on failure.
     */
    fun savePngToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        return try {
            val displayName = "${fileName}_${System.currentTimeMillis()}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ — use MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/QRForge")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return null

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                uri

            } else {
                // Android 9 and below — write directly to file
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "QRForge"
                )
                dir.mkdirs()
                val file = File(dir, displayName)
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Opens the Android share sheet so the user can share the QR image
     * via WhatsApp, Gmail, Telegram etc.
     */
    fun shareQR(context: Context, bitmap: Bitmap, qrName: String) {
        try {
            // Save to cache first for sharing
            val cachePath = File(context.cacheDir, "shared_qr")
            cachePath.mkdirs()
            val file = File(cachePath, "qr_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "QR Code: $qrName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}