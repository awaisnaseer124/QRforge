package com.qrforge.app.data

import android.content.Context
import androidx.room.Room
import com.qrforge.app.core.qr.QRType
import com.qrforge.app.data.db.AppDatabase
import com.qrforge.app.data.db.QREntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

class QRRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val dao = database.qrDao()

    // ── Read ─────────────────────────────────────────────────────────
    fun getAllQRs() = dao.getAllQRs()
    fun searchQRs(query: String) = dao.searchQRs(query)
    fun getQRsByType(type: QRType) = dao.getQRsByType(type)
    fun getTotalCount() = dao.getTotalCount()

    // ── Write ────────────────────────────────────────────────────────
    suspend fun saveGeneratedQR(
        type: QRType,
        content: String,
        colorHex: String
    ): Long {
        val label = extractLabel(type, content)
        return dao.insertQR(
            QREntity(
                type = type,
                content = content,
                label = label,
                colorHex = colorHex,
                isScanned = false
            )
        )
    }

    suspend fun saveScannedQR(content: String, type: QRType): Long {
        val label = extractLabel(type, content)
        return dao.insertQR(
            QREntity(
                type = type,
                content = content,
                label = label,
                isScanned = true
            )
        )
    }

    suspend fun deleteQR(qr: QREntity) = dao.deleteQR(qr)
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun incrementScanCount(id: Long) = dao.incrementScanCount(id)

    // ── Free tier limit ──────────────────────────────────────────────
    suspend fun getTodayGeneratedCount(): Int {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dao.getTodayGeneratedCount(calendar.timeInMillis)
    }

    // ── Helpers ──────────────────────────────────────────────────────
    private fun extractLabel(type: QRType, content: String): String {
        return when (type) {
            QRType.URL -> {
                // Extract domain from URL
                content.removePrefix("https://")
                    .removePrefix("http://")
                    .split("/").firstOrNull()
                    ?.take(30) ?: content.take(30)
            }
            QRType.WIFI -> {
                // Extract SSID from WIFI:T:WPA;S:MyNetwork;...
                val ssid = content.split(";")
                    .firstOrNull { it.startsWith("S:") }
                    ?.removePrefix("S:") ?: "Wi-Fi"
                ssid.take(30)
            }
            QRType.VCARD -> {
                // Extract FN (full name) from vCard
                content.lines()
                    .firstOrNull { it.startsWith("FN:") }
                    ?.removePrefix("FN:") ?: "Contact"
            }
            QRType.EMAIL -> content.removePrefix("mailto:").split("?").first().take(30)
            QRType.PHONE -> content.removePrefix("tel:").take(20)
            QRType.SMS   -> content.removePrefix("sms:").split("?").first().take(20)
            QRType.TEXT  -> content.take(40)
        }
    }
}

// ── Hilt DI Module ────────────────────────────────────────────────────
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideQRRepository(database: AppDatabase): QRRepository {
        return QRRepository(database)
    }
}