package com.antistalk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antistalk.BuildConfig
import com.antistalk.core.AppUpdater
import com.antistalk.core.suggestKeywords
import com.antistalk.data.AntiStalkRepository
import com.antistalk.data.local.entity.AvoidedPerson
import com.antistalk.data.local.entity.MonitoredAppEntity
import com.antistalk.data.local.entity.StalkEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repo: AntiStalkRepository) : ViewModel() {

    val persons: StateFlow<List<AvoidedPerson>> =
        repo.persons.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val apps: StateFlow<List<MonitoredAppEntity>> =
        repo.apps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val events: StateFlow<List<StalkEvent>> =
        repo.recentEvents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats = MutableStateFlow(AntiStalkRepository.TodayStats(0, 0, 0, "", 0))
    val roastLevel = MutableStateFlow(repo.roastLevel)
    val onboardingDone = MutableStateFlow(repo.onboardingDone)

    // Self-update state
    val updateAvailable = MutableStateFlow<AppUpdater.UpdateInfo?>(null)
    val checkingUpdate = MutableStateFlow(false)
    val versionLabel = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    init {
        viewModelScope.launch {
            repo.seedIfNeeded()
            roastLevel.value = repo.roastLevel
            refreshStats()
        }
    }

    fun refreshStats() {
        viewModelScope.launch { stats.value = repo.todayStats() }
    }

    fun setOnboardingDone() {
        repo.onboardingDone = true
        onboardingDone.value = true
    }

    fun setRoastLevel(l: Int) {
        repo.roastLevel = l
        roastLevel.value = l
    }

    fun addPerson(name: String, note: String, goal: String, extra: List<String>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.addPerson(name, note, goal, extra)
            refreshStats()
            onDone()
        }
    }

    fun deletePerson(id: Long) {
        viewModelScope.launch { repo.deletePerson(id) }
    }

    fun toggleApp(pkg: String, label: String, enabled: Boolean) {
        viewModelScope.launch { repo.setAppEnabled(pkg, label, enabled) }
    }

    fun decide(eventId: Long, decision: String, reason: String = "", onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.setDecision(eventId, decision, reason)
            refreshStats()
            onDone()
        }
    }

    fun previewTrigger(name: String, pkg: String) {
        viewModelScope.launch {
            repo.logEvent(name.ifBlank { "Nguyễn Văn A" }, null, pkg, "MANUAL", "LOW")
            refreshStats()
        }
    }

    fun wipeAll() {
        viewModelScope.launch {
            repo.wipeAll()
            roastLevel.value = 2
            onboardingDone.value = false
            refreshStats()
        }
    }

    fun suggestedKeywords(name: String): List<String> = suggestKeywords(name)

    /** Checks GitHub Releases for a newer tag. Silent unless [force] (manual tap). */
    fun checkUpdate(force: Boolean = false) {
        if (checkingUpdate.value) return
        viewModelScope.launch {
            checkingUpdate.value = true
            try {
                val info = AppUpdater.check(BuildConfig.VERSION_CODE)
                updateAvailable.value =
                    if (info != null && (force || repo.skippedUpdateTag != info.tag)) info else null
            } finally {
                checkingUpdate.value = false
            }
        }
    }

    fun skipUpdate() {
        updateAvailable.value?.let { repo.skippedUpdateTag = it.tag }
        updateAvailable.value = null
    }
}

class MainViewModelFactory(private val repo: AntiStalkRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repo) as T
}
