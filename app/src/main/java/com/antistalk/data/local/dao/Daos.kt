package com.antistalk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antistalk.data.local.entity.AvoidedPerson
import com.antistalk.data.local.entity.Keyword
import com.antistalk.data.local.entity.MonitoredAppEntity
import com.antistalk.data.local.entity.StalkEvent
import com.antistalk.data.local.entity.StreakGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons ORDER BY createdAt DESC")
    fun observePersons(): Flow<List<AvoidedPerson>>

    @Query("SELECT * FROM persons ORDER BY createdAt DESC")
    suspend fun getPersonsOnce(): List<AvoidedPerson>

    @Insert suspend fun insertPerson(p: AvoidedPerson): Long
    @Query("DELETE FROM persons WHERE id = :id") suspend fun deletePerson(id: Long)
    @Query("DELETE FROM persons") suspend fun deleteAllPersons()
}

@Dao
interface KeywordDao {
    @Query("SELECT * FROM keywords") fun observeAll(): Flow<List<Keyword>>
    @Query("SELECT * FROM keywords") suspend fun getAllOnce(): List<Keyword>
    @Insert suspend fun insertAll(list: List<Keyword>)
    @Query("DELETE FROM keywords WHERE id IN (:ids)") suspend fun deleteByIds(ids: List<Long>)
    @Query("DELETE FROM keywords") suspend fun deleteAll()
}

@Dao
interface AppDao {
    @Query("SELECT * FROM monitored_apps") fun observe(): Flow<List<MonitoredAppEntity>>
    @Query("SELECT * FROM monitored_apps") suspend fun getAllOnce(): List<MonitoredAppEntity>
    @Query("SELECT * FROM monitored_apps WHERE enabled = 1") suspend fun getEnabledOnce(): List<MonitoredAppEntity>
    @Insert suspend fun insertAll(list: List<MonitoredAppEntity>)
    /** Upsert: keeps existing rows alive on installs that predate a DEFAULTS entry. */
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(app: MonitoredAppEntity)
    @Query("SELECT COUNT(*) FROM monitored_apps") suspend fun count(): Int
}

@Dao
interface EventDao {
    @Insert suspend fun insert(e: StalkEvent): Long
    @Query("UPDATE events SET decision = :decision, reason = :reason WHERE id = :id")
    suspend fun updateDecision(id: Long, decision: String, reason: String)

    @Query("SELECT * FROM events ORDER BY createdAt DESC LIMIT 200")
    fun observeRecent(): Flow<List<StalkEvent>>

    @Query("SELECT COUNT(*) FROM events WHERE createdAt >= :since") suspend fun countSince(since: Long): Int
    @Query("SELECT COUNT(*) FROM events WHERE decision = 'STOPPED' AND createdAt >= :since") suspend fun countStoppedSince(since: Long): Int
    @Query("SELECT COUNT(*) FROM events WHERE decision = 'CONTINUED' AND createdAt >= :since") suspend fun countContinuedSince(since: Long): Int
    @Query("SELECT personName, COUNT(*) as c FROM events WHERE createdAt >= :since GROUP BY personName ORDER BY c DESC LIMIT 1")
    suspend fun topPersonSince(since: Long): TopRow?
    @Query("SELECT COUNT(*) FROM events WHERE personName = :name AND createdAt >= :since") suspend fun countForPersonSince(name: String, since: Long): Int
    @Query("DELETE FROM events") suspend fun deleteAll()

    data class TopRow(val personName: String, val c: Int)
}

@Dao
interface GoalDao {
    @Insert suspend fun insert(g: StreakGoal): Long
    @Query("SELECT * FROM streak_goals ORDER BY id DESC LIMIT 1") suspend fun latest(): StreakGoal?
    @Query("DELETE FROM streak_goals") suspend fun deleteAll()
}
