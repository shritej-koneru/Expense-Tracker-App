package fm.mrc.expensetracker.data

import android.os.Build
import androidx.room.TypeConverter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let {
            val date = Date(it * 1000)
            date.toLocalDateTime()
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.toEpochSecond(ZoneOffset.UTC)
    }

    private fun Date.toLocalDateTime(): LocalDateTime {
        val calendar = Calendar.getInstance()
        calendar.time = this
        return LocalDateTime(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
        )
    }
}

data class LocalDateTime(
    val year: Int,
    val monthValue: Int,
    val dayOfMonth: Int,
    val hour: Int,
    val minute: Int,
    val second: Int
) {
    companion object {
        fun now(): LocalDateTime {
            val calendar = Calendar.getInstance()
            return LocalDateTime(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
            )
        }

        fun ofEpochSecond(epochSecond: Long, nanoOfSecond: Int, offset: ZoneOffset): LocalDateTime {
            val date = Date(epochSecond * 1000)
            val calendar = Calendar.getInstance()
            calendar.time = date
            return LocalDateTime(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
            )
        }
    }

    fun format(formatter: DateTimeFormatter): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, monthValue - 1, dayOfMonth, hour, minute, second)
        return formatter.format(calendar.time)
    }

    fun toEpochSecond(offset: ZoneOffset): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, monthValue - 1, dayOfMonth, hour, minute, second)
        return calendar.timeInMillis / 1000
    }
}

class DateTimeFormatter private constructor(private val pattern: String) {
    fun format(date: Date): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(date)
    }

    companion object {
        fun ofPattern(pattern: String): DateTimeFormatter {
            return DateTimeFormatter(pattern)
        }
    }
}

class ZoneOffset private constructor() {
    companion object {
        val UTC = ZoneOffset()
    }
} 