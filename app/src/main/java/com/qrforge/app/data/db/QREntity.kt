package com.qrforge.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.qrforge.app.core.qr.QRType

@Entity(tableName = "qr_codes")
data class QREntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: QRType,                    // URL, WIFI, VCARD etc.
    val content: String,                 // Raw QR string content
    val label: String,                   // User-friendly name (domain, ssid etc.)
    val colorHex: String = "#000000",    // QR color
    val isScanned: Boolean = false,      // true = scanned, false = generated
    val scanCount: Int = 0,              // how many times this was scanned/viewed
    val createdAt: Long = System.currentTimeMillis()
)