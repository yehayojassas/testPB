package ch.planetebowl.printagent.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ch.planetebowl.printagent.data.local.entity.PrintLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintLogDao {

    @Insert
    suspend fun insert(entity: PrintLogEntity)

    @Query("SELECT * FROM print_logs ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<PrintLogEntity>>

    @Query("SELECT * FROM print_logs WHERE orderId = :orderId ORDER BY timestamp ASC")
    suspend fun findByOrderId(orderId: String): List<PrintLogEntity>

    @Query("DELETE FROM print_logs WHERE timestamp < :beforeEpochMillis")
    suspend fun deleteOlderThan(beforeEpochMillis: Long)
}
