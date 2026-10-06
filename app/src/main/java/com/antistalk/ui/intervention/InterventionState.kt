package com.antistalk.ui.intervention

data class InterventionState(
    val eventId: Long,
    val personName: String,
    val packageName: String,
    val trigger: String,
    val confidence: String,
    val countToday: Int
)

val REASONS = listOf("NHO", "TO_MO", "CO_DON", "NGUOI_MOI", "BUON", "KHONG_BIET")

fun reasonLabel(r: String, vi: Boolean): String = when (r) {
    "NHO" -> if (vi) "Nhớ họ" else "Miss them"
    "TO_MO" -> if (vi) "Tò mò" else "Curious"
    "CO_DON" -> if (vi) "Cô đơn" else "Lonely"
    "NGUOI_MOI" -> if (vi) "Xem có người mới chưa" else "Check if they moved on"
    "BUON" -> if (vi) "Buồn" else "Sad"
    else -> if (vi) "Không biết" else "Don't know"
}
