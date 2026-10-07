package com.antistalk.detection

/**
 * In-memory ring buffer of recent accessibility events seen by
 * [StalkAccessibilityService]. Debug only: lets the user SEE whether events
 * arrive at all (service alive?), what text the service reads (detector
 * working?), and why nothing fired (no match? cooldown? app disabled?).
 *
 * - Never persisted, never leaves the device. Cleared on process death.
 * - Text snippets are truncated to 24 chars; message bodies are never read.
 */
object DetectionLog {
    data class Entry(
        val time: Long,
        val pkg: String,
        val event: String,
        val text: String,
        val result: String
    )

    const val MAX = 60

    private val lock = Any()
    private val entries = ArrayDeque<Entry>()

    fun add(pkg: String, event: String, text: String, result: String) {
        val e = Entry(System.currentTimeMillis(), pkg, event, text, result)
        synchronized(lock) {
            if (entries.size >= MAX) entries.removeFirst()
            entries.addLast(e)
        }
    }

    /** Newest first, for UI. */
    fun snapshot(): List<Entry> = synchronized(lock) { entries.reversed() }

    fun clear() = synchronized(lock) { entries.clear() }
}
