package ch.planetebowl.printagent.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ch.planetebowl.printagent.data.local.entity.PrintJobEntity
import ch.planetebowl.printagent.data.local.entity.PrintLogEntity

@Database(
    entities = [PrintJobEntity::class, PrintLogEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun printJobDao(): PrintJobDao
    abstract fun printLogDao(): PrintLogDao

    companion object {
        const val DATABASE_NAME = "print_agent.db"
    }
}
