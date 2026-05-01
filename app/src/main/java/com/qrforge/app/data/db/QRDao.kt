package com.qrforge.app.data.db

import androidx.room.*
import com.qrforge.app.core.qr.QRType
import kotlinx.coroutines.flow.Flow

@Dao
interface QRDao {

    // Observe all QRs sorted by newest first — auto-updates UI on change
    @Query("SELECT * FROM qr_codes ORDER BY createdAt DESC")
    fun getAllQRs(): Flow<List<QREntity>>

    // Search by label or content
    @Query("""
        SELECT * FROM qr_codes 
        WHERE label LIKE '%' || :query || '%' 
        OR content LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchQRs(query: String): Flow<List<QREntity>>

    // Filter by type
    @Query("SELECT * FROM qr_codes WHERE type = :type ORDER BY createdAt DESC")
    fun getQRsByType(type: QRType): Flow<List<QREntity>>

    // Filter by generated vs scanned
    @Query("SELECT * FROM qr_codes WHERE isScanned = :isScanned ORDER BY createdAt DESC")
    fun getQRsBySource(isScanned: Boolean): Flow<List<QREntity>>

    // Get single QR by id
    @Query("SELECT * FROM qr_codes WHERE id = :id")
    suspend fun getQRById(id: Long): QREntity?

    // Insert — returns new row id
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQR(qr: QREntity): Long

    // Increment scan count
    @Query("UPDATE qr_codes SET scanCount = scanCount + 1 WHERE id = :id")
    suspend fun incrementScanCount(id: Long)

    // Delete one
    @Delete
    suspend fun deleteQR(qr: QREntity)

    // Delete all
    @Query("DELETE FROM qr_codes")
    suspend fun deleteAll()

    // Total count
    @Query("SELECT COUNT(*) FROM qr_codes")
    fun getTotalCount(): Flow<Int>

    // Today's generated count (for free tier limit)
    @Query("""
        SELECT COUNT(*) FROM qr_codes 
        WHERE isScanned = 0 
        AND createdAt >= :startOfDay
    """)
    suspend fun getTodayGeneratedCount(startOfDay: Long): Int
}