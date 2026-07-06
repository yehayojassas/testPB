package ch.planetebowl.printagent.data.local

import androidx.room.TypeConverter
import ch.planetebowl.printagent.data.local.entity.PrintLogEventType
import ch.planetebowl.printagent.data.local.entity.PrintLogResult
import ch.planetebowl.printagent.domain.model.PrintStatus
import java.time.Instant

class Converters {

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromPrintStatus(value: PrintStatus): String = value.name

    @TypeConverter
    fun toPrintStatus(value: String): PrintStatus = PrintStatus.valueOf(value)

    @TypeConverter
    fun fromEventType(value: PrintLogEventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): PrintLogEventType = PrintLogEventType.valueOf(value)

    @TypeConverter
    fun fromLogResult(value: PrintLogResult): String = value.name

    @TypeConverter
    fun toLogResult(value: String): PrintLogResult = PrintLogResult.valueOf(value)
}
