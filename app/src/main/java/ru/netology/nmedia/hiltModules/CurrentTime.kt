package ru.netology.nmedia.hiltModules

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.text.SimpleDateFormat
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

    fun differentHourFromCurrent(time: Long): Long {
        SimpleDateFormat("dd.MM.yy HH:mm:ss.SSS").let {
            it.timeZone = TimeZone.getTimeZone("UTC")
            Log.i("published - time", it.format(time).toString())
        }
        SimpleDateFormat("dd.MM.yy HH:mm:ss.SSS").let {
            it.timeZone = TimeZone.getTimeZone("UTC")
            Log.i("published - currentTime", it.format(currentTime).toString())
        }

        Log.i("published - time", time.toString())
        Log.i("published - currentTime", currentTime.toString())
        return (currentTime - time) / (MINUTE_PER_HOUR * SEC_PER_MINUTE * MS_PER_SEC)
    }
}