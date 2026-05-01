package com.qrforge.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.qrforge.app.core.qr.QRType

@Database(
    entities = [QREntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(QRTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun qrDao(): QRDao

    companion object {
        const val DATABASE_NAME = "qrforge_db"
    }
}

// Converts QRType enum to/from String for Room storage
class QRTypeConverter {
    @TypeConverter
    fun fromQRType(type: QRType): String = type.name

    @TypeConverter
    fun toQRType(value: String): QRType = try {
        QRType.valueOf(value)
    } catch (e: Exception) {
        QRType.URL
    }
}