package cm.momoshield.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Insert
    suspend fun insert(scan: ScanEntity): Long

    @Query("SELECT * FROM scans ORDER BY createdAt DESC, id DESC LIMIT $MAX_HISTORY")
    fun observeRecent(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans ORDER BY createdAt DESC, id DESC")
    suspend fun getAllSync(): List<ScanEntity>

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM scans")
    suspend fun clear()

    /** Conserve uniquement les [MAX_HISTORY] analyses les plus récentes. */
    @Query(
        "DELETE FROM scans WHERE id NOT IN " +
            "(SELECT id FROM scans ORDER BY createdAt DESC, id DESC LIMIT $MAX_HISTORY)",
    )
    suspend fun trim()

    companion object {
        const val MAX_HISTORY = 200
    }
}

@Dao
abstract class ReportedNumberDao {

    @Query("SELECT * FROM reported_numbers WHERE number IN (:numbers)")
    abstract suspend fun findAmong(numbers: List<String>): List<ReportedNumberEntity>

    @Query("SELECT * FROM reported_numbers ORDER BY lastReportedAt DESC")
    abstract fun observeAll(): Flow<List<ReportedNumberEntity>>

    @Query("SELECT * FROM reported_numbers ORDER BY lastReportedAt DESC")
    abstract suspend fun getAllSync(): List<ReportedNumberEntity>

    @Query("SELECT * FROM reported_numbers WHERE number = :number")
    abstract suspend fun find(number: String): ReportedNumberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(entity: ReportedNumberEntity)

    @Query("DELETE FROM reported_numbers WHERE number = :number")
    abstract suspend fun delete(number: String)

    /** Ajoute un signalement : crée l'entrée ou incrémente son compteur, de façon atomique. */
    @Transaction
    open suspend fun report(number: String, now: Long) {
        val existing = find(number)
        val updated = existing?.copy(reportCount = existing.reportCount + 1, lastReportedAt = now)
            ?: ReportedNumberEntity(number = number, reportCount = 1, firstReportedAt = now, lastReportedAt = now)
        upsert(updated)
    }
}
