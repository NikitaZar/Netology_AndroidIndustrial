package ru.netology.nmedia.hiltModules

import ru.netology.nmedia.enumeration.SeparatorTimeType
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

const val HOUR_PER_DAY = 24
const val MINUTE_PER_HOUR = 60
const val SEC_PER_MINUTE = 60
const val MS_PER_SEC = 1000

@Singleton
class CurrentTime @Inject constructor() {

    val currentTime: Long
        get() = Calendar.getInstance().time.time

    fun differentHourFromCurrent(time: Long): Long =
        (currentTime - time * MS_PER_SEC) / (MINUTE_PER_HOUR * SEC_PER_MINUTE * MS_PER_SEC)

    fun getDaySeparatorType(time: Long?): SeparatorTimeType {
        if (time == null) {
            return SeparatorTimeType.NULL
        }
        val dif = differentHourFromCurrent(time)
        return when {
            dif in 0..23L -> SeparatorTimeType.TODAY
            dif in 24L..47L -> SeparatorTimeType.YESTERDAY
            dif >= 48L -> SeparatorTimeType.MORE_OLD
            else -> SeparatorTimeType.NULL
        }
    }
}