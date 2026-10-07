package com.antistalk.ui

import android.content.Context
import android.widget.Toast
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
import com.antistalk.detection.OverlayManager
import com.antistalk.detection.StalkAccessibilityService
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

    // Self-update state (legacy manual dialog path)
    val updateAvailable = MutableStateFlow<AppUpdater.UpdateInfo?>(null)
    val checkingUpdate = MutableStateFlow(false)
    val versionLabel = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    // Near-auto update state machine: Idle -> Downloading -> ReadyToInstall.
    sealed interface UpdateState {
        data object Idle : UpdateState
        data class Downloading(val tag: String) : UpdateState
        data class ReadyToInstall(val info: AppUpdater.UpdateInfo, val uri: android.net.Uri) : UpdateState
    }
    val updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)

    val updateWifiOnly = MutableStateFlow(repo.updateWifiOnly)
    fun setUpdateWifiOnly(v: Boolean) {
        repo.updateWifiOnly = v
        updateWifiOnly.value = v
    }

    val themeMode = MutableStateFlow(repo.themeMode)
    fun setThemeMode(m: String) {
        repo.themeMode = m
        themeMode.value = m
    }

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

    data class DetectionStatus(
        val serviceEnabled: Boolean = false,
        val canOverlay: Boolean = false,
        val personCount: Int = 0,
        val keywordCount: Int = 0,
        val enabledAppCount: Int = 0
    )

    val detectionStatus = MutableStateFlow(DetectionStatus())

    /** Re-read system permissions + local data so Home can show why nothing fires. */
    fun refreshDetectionStatus(ctx: Context) {
        viewModelScope.launch {
            val svc = try { StalkAccessibilityService.isEnabled(ctx) } catch (_: Exception) { false }
            val ov = try { OverlayManager.canDrawOverlays(ctx) } catch (_: Exception) { false }
            val persons = try { repo.personsOnce().size } catch (_: Exception) { 0 }
            val kws = try { repo.keywordsOnce().size } catch (_: Exception) { 0 }
            val apps = try { repo.enabledPackagesOnce().size } catch (_: Exception) { 0 }
            detectionStatus.value = DetectionStatus(svc, ov, persons, kws, apps)
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

    /**
     * Checks GitHub Releases for a newer tag. Silent unless [force] (manual tap).
     *
     * Near-auto flow: when a newer tag is found the APK starts downloading in
     * the background right away (Wi-Fi gate applies to automatic checks; a
     * manual tap always downloads). When the file is ready — now or from a
     * previous run, even offline — [updateState] becomes ReadyToInstall and
     * the UI opens the system installer itself. The user only taps Install.
     */
    fun checkUpdate(ctx: Context, force: Boolean = false) {
        if (checkingUpdate.value) return
        viewModelScope.launch {
            checkingUpdate.value = true
            try {
                // Offline fast-path: a previous download newer than installed?
                val savedTag = AppUpdater.savedTag(ctx)
                val savedNum = savedTag.removePrefix("v").toIntOrNull()
                if (savedNum != null && savedNum > BuildConfig.VERSION_CODE) {
                    val uri = AppUpdater.downloadedUriIfReady(ctx, savedTag)
                    if (uri != null) {
                        updateState.value = UpdateState.ReadyToInstall(
                            AppUpdater.UpdateInfo(savedTag, savedNum, "", ""), uri
                        )
                        updateAvailable.value = null
                    }
                }
                val info = AppUpdater.check(BuildConfig.VERSION_CODE)
                if (info == null) {
                    if (force && updateState.value is UpdateState.Idle) {
                        Toast.makeText(ctx, "Đang dùng bản mới nhất 😌", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                // Already have this exact build on disk? No need to re-download.
                val readyUri = AppUpdater.downloadedUriIfReady(ctx, info.tag)
                if (readyUri != null) {
                    updateState.value = UpdateState.ReadyToInstall(info, readyUri)
                    updateAvailable.value = null
                    return@launch
                }
                // Wi-Fi gate: automatic checks respect it, manual taps don't.
                if (!force && repo.updateWifiOnly && !AppUpdater.isOnWifi(ctx)) {
                    updateAvailable.value =
                        if (repo.skippedUpdateTag != info.tag) info else null
                    return@launch
                }
                val cur = updateState.value
                if (cur is UpdateState.Downloading && cur.tag == info.tag) return@launch
                updateState.value = UpdateState.Downloading(info.tag)
                updateAvailable.value = null
                Toast.makeText(ctx, "Đang tải bản ${info.tag} trong nền…", Toast.LENGTH_SHORT).show()
                AppUpdater.enqueueDownload(ctx, info.apkUrl, info.tag) { uri ->
                    updateState.value = UpdateState.ReadyToInstall(info, uri)
                }
            } finally {
                checkingUpdate.value = false
            }
        }
    }

    /** Open the installer for a downloaded update, once per tag (auto path). */
    fun consumeReadyToInstall(ctx: Context) {
        val s = updateState.value as? UpdateState.ReadyToInstall ?: return
        if (s.info.tagNumber <= BuildConfig.VERSION_CODE) return // already installed
        if (AppUpdater.wasPrompted(ctx, s.info.tag)) return
        AppUpdater.markPrompted(ctx, s.info.tag)
        openDownloadedInstaller(ctx)
    }

    /** Open the installer for a downloaded update (manual retry button). */
    fun openDownloadedInstaller(ctx: Context) {
        val s = updateState.value as? UpdateState.ReadyToInstall ?: return
        if (!AppUpdater.canInstall(ctx)) {
            AppUpdater.openInstallPermissionSettings(ctx)
            Toast.makeText(ctx, "Bật cho phép rồi bấm CÀI ĐẶT lại nhé", Toast.LENGTH_LONG).show()
            return
        }
        AppUpdater.openInstaller(ctx, s.uri)
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
