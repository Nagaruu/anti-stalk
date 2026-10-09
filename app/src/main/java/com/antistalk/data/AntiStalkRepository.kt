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
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class AntiStalkRepository(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private val db = AntiStalkDb.get(appCtx)
    private val prefs: SharedPreferences = appCtx.getSharedPreferences("antistalk", Context.MODE_PRIVATE)

    val persons: Flow<List<AvoidedPerson>> = db.personDao().observePersons()
    val apps: Flow<List<MonitoredAppEntity>> = db.appDao().observe()
    val keywords: Flow<List<Keyword>> = db.keywordDao().observeAll()
    val recentEvents: Flow<List<StalkEvent>> = db.eventDao().observeRecent()

    var roastLevel: Int
        get() = prefs.getInt("roast_level", 2)
        set(v) { prefs.edit().putInt("roast_level", v).apply() }
    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(v) { prefs.edit().putBoolean("onboarding_done", v).apply() }
    /**
     * Prominent Accessibility disclosure acceptance (Play policy).
     * Shown as its own screen with a separate checkbox BEFORE the system
     * Accessibility settings — never bundled with ToS/privacy text.
     */
    var disclosureAccepted: Boolean
        get() = prefs.getBoolean("disclosure_accepted", false)
        set(v) { prefs.edit().putBoolean("disclosure_accepted", v).apply() }
    var skippedUpdateTag: String
        get() = prefs.getString("skipped_update_tag", "") ?: ""
        set(v) { prefs.edit().putString("skipped_update_tag", v).apply() }
    var updateWifiOnly: Boolean
        get() = prefs.getBoolean("update_wifi_only", true)
        set(v) { prefs.edit().putBoolean("update_wifi_only", v).apply() }
    /** "system" | "light" | "dark" */
    var themeMode: String
        get() = prefs.getString("theme_mode", "system") ?: "system"
        set(v) { prefs.edit().putString("theme_mode", v).apply() }
    var userGoal: String
        get() = prefs.getString("user_goal", "Người yêu cũ") ?: "Người yêu cũ"
        set(v) { prefs.edit().putString("user_goal", v).apply() }

    suspend fun seedIfNeeded() {
        // Insert-only-when-empty would strand existing installs forever: a new
        // MonitoredPackages.DEFAULTS entry would never reach their DB (and
        // setEnabled/replace by key would then silently do nothing).
        val existing = db.appDao().getAllOnce().map { it.packageName }.toSet()
        val missing = MonitoredPackages.DEFAULTS.filter { it.packageName !in existing }
        if (missing.isNotEmpty()) {
            db.appDao().insertAll(
                missing.map { MonitoredAppEntity(it.packageName, it.label, it.defaultEnabled) }
            )
        }
        cleanupDangerousKeywords()
    }

    /**
     * Purges dangerous single sub-words from legacy versions or over-eager imports
     * (e.g. "too", "jee" from "Kang Too Jee", or "van", "duc" from "Nguyễn Văn Đức").
     */
    suspend fun cleanupDangerousKeywords() {
        try {
            val persons = db.personDao().getPersonsOnce()
            val allKws = db.keywordDao().getAllOnce()
            val toDeleteIds = mutableListOf<Long>()
            for (p in persons) {
                val pWords = normalizeText(p.displayName).split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (pWords.size > 1) {
                    val pKws = allKws.filter { it.personId == p.id }
                    for (kw in pKws) {
                        val kwWords = kw.normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }
                        if (kwWords.size == 1 && (kw.normalized.length <= 4 || pWords.contains(kw.normalized))) {
                            toDeleteIds.add(kw.id)
                        }
                    }
                }
            }
            if (toDeleteIds.isNotEmpty()) {
                db.keywordDao().deleteByIds(toDeleteIds)
            }
        } catch (_: Exception) { }
    }

    suspend fun addPerson(displayName: String, note: String, goal: String, extraKeywords: List<String>): Long {
        val id = db.personDao().insertPerson(AvoidedPerson(displayName = displayName, note = note, goal = goal))
        val raws = (suggestKeywords(displayName) + extraKeywords.map { it.trim() }.filter { it.isNotEmpty() }).distinct()
        db.keywordDao().insertAll(raws.map { Keyword(personId = id, raw = it, normalized = normalizeText(it)) })
        return id
    }

    suspend fun deletePerson(id: Long) = db.personDao().deletePerson(id)
    suspend fun setAppEnabled(pkg: String, label: String, enabled: Boolean) =
        // Upsert, not update: an UPDATE on a row that was never seeded (old
        // install, new DEFAULTS entry) matches 0 rows and the toggle dies silently.
        db.appDao().upsert(MonitoredAppEntity(pkg, label, enabled))

    suspend fun keywordsOnce() = db.keywordDao().getAllOnce()
    suspend fun personsOnce() = db.personDao().getPersonsOnce()
    suspend fun enabledPackagesOnce(): Set<String> =
        db.appDao().getEnabledOnce().map { it.packageName }.toSet()
    suspend fun appCountOnce(): Int = db.appDao().count()

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

    data class ImportSummary(val persons: Int, val keywords: Int, val apps: Int, val skippedPersons: Int = 0)

    /**
     * Serialize everything worth keeping across an uninstall/reinstall:
     * persons, keywords (referenced by person index so ids can change),
     * monitored app toggles and the settings. Event history is deliberately
     * left out — it is "today" stats only and holds the sensitive trail.
     */
    suspend fun exportBackupJson(): String {
        val persons = db.personDao().getPersonsOnce()
        val keywords = db.keywordDao().getAllOnce()
        val apps = db.appDao().getAllOnce()
        val indexOf = persons.withIndex().associate { (i, p) -> p.id to i }

        val root = JSONObject().apply {
            put("app", "antistalk")
            put("version", BACKUP_VERSION)
            put("createdAt", System.currentTimeMillis())
            put("persons", JSONArray().apply {
                persons.forEach { p ->
                    put(JSONObject().apply {
                        put("name", p.displayName)
                        put("note", p.note)
                        put("goal", p.goal)
                        put("createdAt", p.createdAt)
                    })
                }
            })
            put("keywords", JSONArray().apply {
                keywords.forEach { k ->
                    val idx = indexOf[k.personId] ?: return@forEach
                    put(JSONObject().apply {
                        put("personIndex", idx)
                        put("raw", k.raw)
                        put("normalized", k.normalized)
                    })
                }
            })
            put("apps", JSONArray().apply {
                apps.forEach { a ->
                    put(JSONObject().apply {
                        put("packageName", a.packageName)
                        put("label", a.label)
                        put("enabled", a.enabled)
                    })
                }
            })
            put("prefs", JSONObject().apply {
                put("roast_level", prefs.getInt("roast_level", 2))
                put("theme_mode", prefs.getString("theme_mode", "system") ?: "system")
                put("update_wifi_only", prefs.getBoolean("update_wifi_only", true))
                put("onboarding_done", prefs.getBoolean("onboarding_done", false))
                put("disclosure_accepted", prefs.getBoolean("disclosure_accepted", false))
            })
        }
        return root.toString(2)
    }

    /** Restore a file written by [exportBackupJson]. Idempotent: existing display names are skipped. */
    suspend fun importBackupJson(json: String): ImportSummary {
        val root = JSONObject(json)
        if (root.optString("app") != "antistalk") {
            throw IllegalArgumentException("Đây không phải file sao lưu Anti-Stalk")
        }
        val existing = db.personDao().getPersonsOnce().map { it.displayName }.toSet()
        val newIds = HashMap<Int, Long>()
        var personsAdded = 0
        var personsSkipped = 0
        var keywordsAdded = 0
        var appsAdded = 0

        val pArr = root.optJSONArray("persons") ?: JSONArray()
        for (i in 0 until pArr.length()) {
            val o = pArr.getJSONObject(i)
            val name = o.optString("name").trim()
            if (name.isEmpty()) continue
            if (name in existing) { personsSkipped++; continue }
            val id = db.personDao().insertPerson(
                AvoidedPerson(
                    displayName = name,
                    note = o.optString("note"),
                    goal = o.optString("goal"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
            newIds[i] = id
            personsAdded++
        }

        val kArr = root.optJSONArray("keywords") ?: JSONArray()
        for (i in 0 until kArr.length()) {
            val o = kArr.getJSONObject(i)
            val personId = newIds[o.optInt("personIndex", -1)] ?: continue
            val raw = o.optString("raw").trim()
            if (raw.isEmpty()) continue
            db.keywordDao().insertAll(
                listOf(Keyword(personId = personId, raw = raw, normalized = normalizeText(raw)))
            )
            keywordsAdded++
        }

        val aArr = root.optJSONArray("apps") ?: JSONArray()
        for (i in 0 until aArr.length()) {
            val o = aArr.getJSONObject(i)
            val pkg = o.optString("packageName")
            if (pkg.isEmpty()) continue
            db.appDao().upsert(MonitoredAppEntity(pkg, o.optString("label"), o.optBoolean("enabled", true)))
            appsAdded++
        }

        val p = root.optJSONObject("prefs")
        if (p != null) {
            prefs.edit()
                .putInt("roast_level", p.optInt("roast_level", prefs.getInt("roast_level", 2)).coerceIn(1, 4))
                .putString("theme_mode", p.optString("theme_mode", prefs.getString("theme_mode", "system")))
                .putBoolean("update_wifi_only", p.optBoolean("update_wifi_only", true))
                .putBoolean("onboarding_done", p.optBoolean("onboarding_done", false) || prefs.getBoolean("onboarding_done", false))
                .putBoolean("disclosure_accepted", p.optBoolean("disclosure_accepted", false) || prefs.getBoolean("disclosure_accepted", false))
                .apply()
        }
        seedIfNeeded()
        return ImportSummary(personsAdded, keywordsAdded, appsAdded, personsSkipped)
    }

    companion object {
        private const val BACKUP_VERSION = 1
    }

    data class TodayStats(
        val total: Int,
        val stopped: Int,
        val continued: Int,
        val topName: String,
        val topCount: Int,
        val dismissed: Int = (total - stopped - continued).coerceAtLeast(0)
    )
}
