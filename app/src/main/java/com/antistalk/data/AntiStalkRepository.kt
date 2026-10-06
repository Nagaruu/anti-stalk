package com.antistalk.data

import android.content.Context
import android.content.SharedPreferences
import com.antistalk.core.MonitoredPackages
import com.antistalk.core.normalizeText
import com.antistalk.core.suggestKeywords
import com.antistalk.data.local.AntiStalkDb
import com.antistalk.data.local.entity.AvoidedPerson
import com.antistalk.data.local.entity.Keyword
import com.antistalk.data.local.entity.MonitoredAppEntity
import com.antistalk.data.local.entity.StalkEvent
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class AntiStalkRepository(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private val db = AntiStalkDb.get(appCtx)
    private val prefs: SharedPreferences = appCtx.getSharedPreferences("antistalk", Context.MODE_PRIVATE)

    val persons: Flow<List<AvoidedPerson>> = db.personDao().observePersons()
    val apps: Flow<List<MonitoredAppEntity>> = db.appDao().observe()
    val recentEvents: Flow<List<StalkEvent>> = db.eventDao().observeRecent()

    var roastLevel: Int
        get() = prefs.getInt("roast_level", 2)
        set(v) { prefs.edit().putInt("roast_level", v).apply() }
    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(v) { prefs.edit().putBoolean("onboarding_done", v).apply() }
    var skippedUpdateTag: String
        get() = prefs.getString("skipped_update_tag", "") ?: ""
        set(v) { prefs.edit().putString("skipped_update_tag", v).apply() }

    suspend fun seedIfNeeded() {
        if (db.appDao().count() == 0) {
            db.appDao().insertAll(
                MonitoredPackages.DEFAULTS.map {
                    MonitoredAppEntity(it.packageName, it.label, it.defaultEnabled)
                }
            )
        }
    }

    suspend fun addPerson(displayName: String, note: String, goal: String, extraKeywords: List<String>): Long {
        val id = db.personDao().insertPerson(AvoidedPerson(displayName = displayName, note = note, goal = goal))
        val raws = (suggestKeywords(displayName) + extraKeywords.map { it.trim() }.filter { it.isNotEmpty() }).distinct()
        db.keywordDao().insertAll(raws.map { Keyword(personId = id, raw = it, normalized = normalizeText(it)) })
        return id
    }

    suspend fun deletePerson(id: Long) = db.personDao().deletePerson(id)
    suspend fun setAppEnabled(pkg: String, label: String, enabled: Boolean) =
        db.appDao().update(MonitoredAppEntity(pkg, label, enabled))

    suspend fun keywordsOnce() = db.keywordDao().getAllOnce()
    suspend fun personsOnce() = db.personDao().getPersonsOnce()
    suspend fun enabledPackagesOnce(): Set<String> =
        db.appDao().getEnabledOnce().map { it.packageName }.toSet()

    suspend fun logEvent(personName: String, personId: Long?, pkg: String, trigger: String, confidence: String): Long =
        db.eventDao().insert(StalkEvent(personName = personName, personId = personId, packageName = pkg, triggerType = trigger, confidence = confidence, decision = "DISMISSED"))

    suspend fun setDecision(eventId: Long, decision: String, reason: String = "") =
        db.eventDao().updateDecision(eventId, decision, reason)

    suspend fun todayStats(): TodayStats {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val total = db.eventDao().countSince(start)
        val stopped = db.eventDao().countStoppedSince(start)
        val continued = db.eventDao().countContinuedSince(start)
        val top = db.eventDao().topPersonSince(start)
        return TodayStats(total, stopped, continued, top?.personName ?: "", top?.c ?: 0)
    }

    suspend fun countTodayForPerson(name: String): Int {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return db.eventDao().countForPersonSince(name, start)
    }

    suspend fun wipeAll() {
        db.eventDao().deleteAll()
        db.keywordDao().deleteAll()
        db.personDao().deleteAllPersons()
        db.goalDao().deleteAll()
        prefs.edit().clear().apply()
        seedIfNeeded()
    }

    data class TodayStats(val total: Int, val stopped: Int, val continued: Int, val topName: String, val topCount: Int)
}
