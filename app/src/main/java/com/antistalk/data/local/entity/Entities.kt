package com.antistalk.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class AvoidedPerson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val note: String = "",
    val goal: String = "", // NYC / Crush / ...
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "keywords",
    foreignKeys = [ForeignKey(
        entity = AvoidedPerson::class,
        parentColumns = ["id"],
        childColumns = ["personId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("personId")]
)
data class Keyword(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val raw: String,
    val normalized: String
)

@Entity(tableName = "monitored_apps")
data class MonitoredAppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val enabled: Boolean = true
)

@Entity(
    tableName = "events",
    foreignKeys = [ForeignKey(
        entity = AvoidedPerson::class,
        parentColumns = ["id"],
        childColumns = ["personId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("personId"), Index("createdAt")]
)
data class StalkEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long?,
    val personName: String, // denormalized so stats survive deletion
    val packageName: String,
    val triggerType: String, // SEARCH_INPUT | SEARCH_SUBMITTED | PROFILE_TITLE | MANUAL
    val confidence: String, // HIGH | MEDIUM | LOW
    val decision: String, // STOPPED | CONTINUED | DISMISSED
    val reason: String = "", // NHO | TO_MO | CO_DON | NGUOI_MOI | BUON | KHONG_BIET
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Reserved for future target-streak milestones (e.g. 7-day or 30-day no-stalk challenge).
 * Kept in Room DB v1 schema for backward compatibility.
 */
@Entity(tableName = "streak_goals")
data class StreakGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val targetDays: Int = 7,
    val startDate: Long = System.currentTimeMillis(),
    val lastBreakAt: Long? = null
)
