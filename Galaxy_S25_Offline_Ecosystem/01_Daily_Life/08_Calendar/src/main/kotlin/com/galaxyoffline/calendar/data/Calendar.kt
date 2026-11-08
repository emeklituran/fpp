package com.galaxyoffline.calendar.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class EventRepeat(val label: String) {
    NONE("Tekrar yok"), WEEKLY("Her hafta"), MONTHLY("Her ay"), YEARLY("Her yıl");
    companion object { fun of(k: String?) = entries.firstOrNull { it.name == k } ?: NONE }
}

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    /** Başlangıç günü (epochDay). */
    val date: Long,
    @ColumnInfo(name = "all_day") val allDay: Boolean = true,
    /** Gün içindeki dakika (0..1439); tüm gün etkinliklerde null. */
    @ColumnInfo(name = "start_minute") val startMinute: Int? = null,
    @ColumnInfo(name = "end_minute") val endMinute: Int? = null,
    val color: Int = 0,
    val repeat: String = EventRepeat.NONE.name,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    val startDate: LocalDate get() = LocalDate.ofEpochDay(date)
    val startTime: LocalTime? get() = startMinute?.let { LocalTime.of(it / 60, it % 60) }
    val endTime: LocalTime? get() = endMinute?.let { LocalTime.of(it / 60, it % 60) }
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events") fun observeAll(): Flow<List<Event>>
    @Upsert suspend fun upsert(e: Event)
    @Query("DELETE FROM events WHERE id = :id") suspend fun delete(id: Long)
}

@Database(entities = [Event::class], version = 1, exportSchema = true)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun events(): EventDao
    companion object {
        @Volatile private var instance: CalendarDatabase? = null
        fun get(context: Context): CalendarDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, CalendarDatabase::class.java, "takvim.db").build().also { instance = it }
        }
    }
}

data class Occurrence(val event: Event, val date: LocalDate)

object CalendarLogic {
    /** Ay ızgarası: pazartesiden başlayan 6 hafta (42 gün). */
    fun monthGrid(month: YearMonth): List<LocalDate> {
        val start = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0 until 42).map { start.plusDays(it.toLong()) }
    }

    /** [from]..[to] (dahil) aralığındaki tüm etkinlik tekrarları, gün sırasına ve saate göre. */
    fun occurrences(events: List<Event>, from: LocalDate, to: LocalDate): List<Occurrence> {
        val out = ArrayList<Occurrence>()
        for (e in events) {
            val start = e.startDate
            if (start.isAfter(to)) continue
            when (EventRepeat.of(e.repeat)) {
                EventRepeat.NONE -> if (!start.isBefore(from)) out += Occurrence(e, start)
                EventRepeat.WEEKLY -> {
                    var d = if (start.isBefore(from)) start.plusWeeks(ChronoUnit.WEEKS.between(start, from)) else start
                    while (d.isBefore(from)) d = d.plusWeeks(1)
                    while (!d.isAfter(to)) { out += Occurrence(e, d); d = d.plusWeeks(1) }
                }
                EventRepeat.MONTHLY -> {
                    var k = maxOf(0L, ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(from)) - 1)
                    while (true) {
                        val d = start.plusMonths(k)
                        if (d.isAfter(to)) break
                        if (!d.isBefore(from)) out += Occurrence(e, d)
                        k++
                    }
                }
                EventRepeat.YEARLY -> {
                    var k = maxOf(0L, (from.year - start.year - 1).toLong())
                    while (true) {
                        val d = start.plusYears(k)
                        if (d.isAfter(to)) break
                        if (!d.isBefore(from)) out += Occurrence(e, d)
                        k++
                    }
                }
            }
        }
        return out.sortedWith(compareBy<Occurrence> { it.date }.thenBy { if (it.event.allDay) -1 else it.event.startMinute ?: 0 })
    }
}

/** Türkiye'nin sabit tarihli resmî tatil ve bayramları. Dinî bayramlar (tarihi her yıl değişir) dahil değildir. */
object TurkishHolidays {
    fun forYear(year: Int): Map<LocalDate, String> = buildMap {
        put(LocalDate.of(year, 1, 1), "Yılbaşı")
        put(LocalDate.of(year, 4, 23), "Ulusal Egemenlik ve Çocuk Bayramı")
        put(LocalDate.of(year, 5, 1), "Emek ve Dayanışma Günü")
        put(LocalDate.of(year, 5, 19), "Atatürk'ü Anma, Gençlik ve Spor Bayramı")
        if (year >= 2017) put(LocalDate.of(year, 7, 15), "Demokrasi ve Millî Birlik Günü")
        put(LocalDate.of(year, 8, 30), "Zafer Bayramı")
        put(LocalDate.of(year, 10, 29), "Cumhuriyet Bayramı")
    }

    fun between(from: LocalDate, to: LocalDate): Map<LocalDate, String> =
        (from.year..to.year).flatMap { forYear(it).entries }.filter { !it.key.isBefore(from) && !it.key.isAfter(to) }.associate { it.key to it.value }
}
