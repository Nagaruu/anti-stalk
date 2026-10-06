package com.antistalk.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.antistalk.data.local.dao.AppDao
import com.antistalk.data.local.dao.EventDao
import com.antistalk.data.local.dao.GoalDao
import com.antistalk.data.local.dao.KeywordDao
import com.antistalk.data.local.dao.PersonDao
import com.antistalk.data.local.entity.AvoidedPerson
import com.antistalk.data.local.entity.Keyword
import com.antistalk.data.local.entity.MonitoredAppEntity
import com.antistalk.data.local.entity.StalkEvent
import com.antistalk.data.local.entity.StreakGoal

@Database(
    entities = [AvoidedPerson::class, Keyword::class, MonitoredAppEntity::class, StalkEvent::class, StreakGoal::class],
    version = 1, exportSchema = false
)
abstract class AntiStalkDb : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun keywordDao(): KeywordDao
    abstract fun appDao(): AppDao
    abstract fun eventDao(): EventDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile private var I: AntiStalkDb? = null
        fun get(ctx: Context): AntiStalkDb = I ?: synchronized(this) {
            I ?: Room.databaseBuilder(ctx.applicationContext, AntiStalkDb::class.java, "antistalk.db")
                .fallbackToDestructiveMigration()
                .build().also { I = it }
        }
    }
}
