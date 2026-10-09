package com.antistalk.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antistalk.BuildConfig
import com.antistalk.core.AppUpdater
import com.antistalk.core.BackupFiles
import com.antistalk.core.SigningInfo
import com.antistalk.core.suggestKeywords
import com.antistalk.data.AntiStalkRepository
import com.antistalk.data.local.entity.AvoidedPerson
import com.antistalk.data.local.entity.MonitoredAppEntity
import com.antistalk.data.local.entity.StalkEvent
import com.antistalk.detection.DetectionLog
import com.antistalk.detection.OverlayManager
import com.antistalk.detection.StalkAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val disclosureAccepted = MutableStateFlow(repo.disclosureAccepted)

    fun acceptDisclosure() {
        repo.disclosureAccepted = true
        disclosureAccepted.value = true
    }

    // Self-update state (legacy manual dialog path)
    val updateAvailable = MutableStateFlow<AppUpdater.UpdateInfo?>(null)
    val checkingUpdate = MutableStateFlow(false)
    /** True when the last check couldn't reach the API at all (offline/private repo). */
    val updateCheckFailed = MutableStateFlow(false)
    val versionLabel = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    // Near-auto update state machine: Idle -> Downloading -> ReadyToInstall.
    sealed interface UpdateState {
        data object Idle : UpdateState
        data class Downloading(val tag: String) : UpdateState
        data class ReadyToInstall(val info: AppUpdater.UpdateInfo, val uri: android.net.Uri) : UpdateState
        /** Downloaded APK's signer differs from the installed app's: Android will reject the install. */
        data class NeedsMigration(val info: AppUpdater.UpdateInfo, val uri: android.net.Uri) : UpdateState
    }
    val updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)

    data class SignatureStatus(
        val installedSha: String?,
        val expectedSha: String,
        val isReleaseKey: Boolean
    )
    val signature = MutableStateFlow<SignatureStatus?>(null)

    fun refreshSignature(ctx: Context) {
        viewModelScope.launch {
            val installed = SigningInfo.installedCertSha256(ctx)
            signature.value = SignatureStatus(
                installedSha = installed,
                expectedSha = SigningInfo.EXPECTED_CERT_SHA256,
                isReleaseKey = installed != null &&
                    installed.equals(SigningInfo.EXPECTED_CERT_SHA256, ignoreCase = true)
            )
        }
    }

    /**
     * Park a freshly downloaded APK in either ReadyToInstall or NeedsMigration:
     * comparing signers up-front is the difference between one tap on Install
     * and the system failing with "gói xung đột với một gói hiện có".
     * The comparison copies + parses the APK, so it runs off the main thread.
     */
    private fun armReady(ctx: Context, info: AppUpdater.UpdateInfo, uri: Uri) {
        viewModelScope.launch {
            val match = withContext(Dispatchers.IO) {
                SigningInfo.archiveMatchesInstalled(ctx, uri)
            }
            updateState.value = if (match == false) {
                updateAvailable.value = null
                UpdateState.NeedsMigration(info, uri)
            } else {
                UpdateState.ReadyToInstall(info, uri)
            }
        }
    }

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

    var userGoal: String
        get() = repo.userGoal
        set(value) { repo.userGoal = value }

    fun setRoastLevel(l: Int) {
        repo.roastLevel = l
        roastLevel.value = l
    }

    fun addPerson(name: String, note: String, goal: String, extra: List<String>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repo.addPerson(name, note, goal, extra)
            StalkAccessibilityService.invalidateCache()
            refreshStats()
            onDone()
        }
    }

    fun deletePerson(id: Long) {
        viewModelScope.launch {
            repo.deletePerson(id)
            StalkAccessibilityService.invalidateCache()
        }
    }

    fun deletePersonWithUndo(person: AvoidedPerson, onDeleted: (restoreAction: () -> Unit) -> Unit) {
        viewModelScope.launch {
            val allKeywords = repo.keywordsOnce().filter { it.personId == person.id }.map { it.raw }
            repo.deletePerson(person.id)
            StalkAccessibilityService.invalidateCache()
            refreshStats()
            onDeleted {
                viewModelScope.launch {
                    repo.addPerson(person.displayName, person.note, person.goal, allKeywords)
                    StalkAccessibilityService.invalidateCache()
                    refreshStats()
                }
            }
        }
    }

    fun toggleApp(pkg: String, label: String, enabled: Boolean) {
        viewModelScope.launch {
            repo.setAppEnabled(pkg, label, enabled)
            StalkAccessibilityService.invalidateCache()
        }
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

    // Raw event debug log (in-memory only, never leaves the device).
    val eventLog = MutableStateFlow<List<DetectionLog.Entry>>(emptyList())
    val keywordsPreview = MutableStateFlow<List<String>>(emptyList())
    private val logTimeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun refreshEventLog() {
        viewModelScope.launch {
            eventLog.value = DetectionLog.snapshot()
            try {
                keywordsPreview.value = repo.keywordsOnce()
                    .map { it.normalized }.filter { it.length >= 3 }.distinct().take(12)
            } catch (_: Exception) { }
        }
    }

    fun eventLogLine(e: DetectionLog.Entry): String {
        val shortPkg = e.pkg.substringAfterLast('.').take(12)
        return "${logTimeFmt.format(Date(e.time))} $shortPkg ${e.event} '${e.text}' → ${e.result}"
    }

    /** Force-show the roast overlay to verify the overlay path end-to-end. */
    fun testOverlay(ctx: Context) {
        if (OverlayManager.isShowing()) {
            Toast.makeText(ctx, "Đang có overlay hiện rồi", Toast.LENGTH_SHORT).show()
            return
        }
        if (!OverlayManager.canDrawOverlays(ctx)) {
            OverlayManager.openOverlaySettings(ctx)
            Toast.makeText(ctx, "Cấp quyền Vẽ rồi bấm TEST lại nhé", Toast.LENGTH_LONG).show()
            return
        }
        OverlayManager.show(ctx, -1L, "Nguyễn Văn A (test)", ctx.packageName, "TEST", "HIGH", 1)
        Toast.makeText(ctx, "Đã bung overlay test — mở Facebook để thấy nó nổi lên", Toast.LENGTH_LONG).show()
    }

    fun wipeAll() {
        viewModelScope.launch {
            repo.wipeAll()
            StalkAccessibilityService.invalidateCache()
            // repo.wipeAll() clears the prefs file, so every mirrored StateFlow
            // must fall back to its default too — otherwise the chips keep
            // showing the pre-wipe values while the prefs already say "system"/true.
            roastLevel.value = 2
            themeMode.value = "system"
            updateWifiOnly.value = true
            onboardingDone.value = false
            disclosureAccepted.value = false
            detectionStatus.value = DetectionStatus()
            refreshStats()
        }
    }

    fun suggestedKeywords(name: String): List<String> = suggestKeywords(name)

    /** Kick off the APK download for [info]; completion lands in armReady. */
    fun startDownload(ctx: Context, info: AppUpdater.UpdateInfo) {
        // Play builds update via Play — never download APKs (policy).
        if (BuildConfig.IS_PLAY) return
        val cur = updateState.value
        if (cur is UpdateState.Downloading && cur.tag == info.tag) return
        updateState.value = UpdateState.Downloading(info.tag)
        updateAvailable.value = null
        AppUpdater.enqueueDownload(ctx, info.apkUrl, info.tag) { uri ->
            armReady(ctx, info, uri)
        }
    }

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
        // Play builds update via Play — no GitHub self-update check.
        if (BuildConfig.IS_PLAY) {
            if (force) {
                Toast.makeText(ctx, "Bản Play cập nhật qua CH Play nhé 😌", Toast.LENGTH_SHORT).show()
            }
            return
        }
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
                        armReady(ctx, AppUpdater.UpdateInfo(savedTag, savedNum, "", ""), uri)
                        updateAvailable.value = null
                        // Already have a newer APK on disk: don't hit the network,
                        // otherwise an offline check would end with BOTH a ready
                        // install and a "couldn't check for updates" warning.
                        return@launch
                    }
                }
                val res = AppUpdater.checkResult(BuildConfig.VERSION_CODE)
                if (res is AppUpdater.CheckResult.Unreachable) {
                    updateCheckFailed.value = true
                    if (force) {
                        Toast.makeText(
                            ctx,
                            "Không kiểm tra được cập nhật (mất mạng hoặc repo chưa Public)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }
                updateCheckFailed.value = false
                val info = (res as? AppUpdater.CheckResult.Update)?.info
                if (info == null) {
                    if (force && updateState.value is UpdateState.Idle) {
                        Toast.makeText(ctx, "Đang dùng bản mới nhất 😌", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                // Already have this exact build on disk? No need to re-download.
                val readyUri = AppUpdater.downloadedUriIfReady(ctx, info.tag)
                if (readyUri != null) {
                    armReady(ctx, info, readyUri)
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
                Toast.makeText(ctx, "Đang tải bản ${info.tag} trong nền…", Toast.LENGTH_SHORT).show()
                startDownload(ctx, info)
            } finally {
                checkingUpdate.value = false
            }
        }
    }

    /** Open the installer for a downloaded update, once per tag (auto path). */
    fun consumeReadyToInstall(ctx: Context) {
        if (BuildConfig.IS_PLAY) return
        val s = updateState.value as? UpdateState.ReadyToInstall ?: return
        if (s.info.tagNumber <= BuildConfig.VERSION_CODE) return // already installed
        if (AppUpdater.wasPrompted(ctx, s.info.tag)) return
        AppUpdater.markPrompted(ctx, s.info.tag)
        openDownloadedInstaller(ctx)
    }

    /** Open the installer for a downloaded update (manual retry button). */
    fun openDownloadedInstaller(ctx: Context) {
        if (BuildConfig.IS_PLAY) return
        if (updateState.value is UpdateState.NeedsMigration) {
            migrationDismissedTag.value = "" // re-offer the guided migration
            return
        }
        val s = updateState.value as? UpdateState.ReadyToInstall ?: return
        viewModelScope.launch {
            // Re-check here: the file may have been replaced since download time.
            val match = withContext(Dispatchers.IO) {
                SigningInfo.archiveMatchesInstalled(ctx, s.uri)
            }
            if (match == false) {
                updateState.value = UpdateState.NeedsMigration(s.info, s.uri)
                updateAvailable.value = null
                return@launch
            }
            if (!AppUpdater.canInstall(ctx)) {
                AppUpdater.openInstallPermissionSettings(ctx)
                Toast.makeText(ctx, "Bật cho phép rồi bấm CÀI ĐẶT lại nhé", Toast.LENGTH_LONG).show()
                return@launch
            }
            AppUpdater.openInstaller(ctx, s.uri, verifySigner = false)
        }
    }

    // ─── One-time signer migration ────────────────────────────────────────────
    // Android refuses updates signed by a different key (pre-keystore CI builds
    // each had a throwaway debug key), so the only way forward is: save data →
    // save the new APK somewhere that survives uninstall → uninstall → install
    // the saved APK → restore. Each step is a button here, no typing.

    /** Tag of a migration dialog the user already dismissed this session. */
    val migrationDismissedTag = MutableStateFlow("")

    fun dismissMigration() {
        migrationDismissedTag.value =
            (updateState.value as? UpdateState.NeedsMigration)?.info?.tag ?: ""
    }

    fun showMigrationAgain() {
        migrationDismissedTag.value = ""
    }

    fun exportBackupTo(ctx: Context, dst: Uri) {
        viewModelScope.launch {
            val json = try { repo.exportBackupJson() } catch (_: Exception) { null }
            val ok = json != null && BackupFiles.writeText(ctx, dst, json)
            Toast.makeText(
                ctx,
                if (ok) "Đã xuất sao lưu — giữ file này đến khi cài lại xong"
                else "Xuất sao lưu thất bại, thử lại nhé",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun importBackupFrom(ctx: Context, src: Uri) {
        viewModelScope.launch {
            val json = BackupFiles.readText(ctx, src)
            if (json == null) {
                Toast.makeText(ctx, "Không đọc được file sao lưu", Toast.LENGTH_LONG).show()
                return@launch
            }
            try {
                val s = repo.importBackupJson(json)
                StalkAccessibilityService.invalidateCache()
                refreshStats()
                Toast.makeText(
                    ctx,
                    "Đã khôi phục ${s.persons} người né, ${s.keywords} từ khóa, ${s.apps} app" +
                        if (s.skippedPersons > 0) " (bỏ qua ${s.skippedPersons} tên đã có)" else "",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                Toast.makeText(ctx, "File không phải bản sao lưu Anti-Stalk", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Copy the downloaded APK to a user-picked location that survives uninstall. */
    fun saveUpdateApkTo(ctx: Context, dst: Uri) {
        val s = when (val st = updateState.value) {
            is UpdateState.NeedsMigration -> st.uri
            is UpdateState.ReadyToInstall -> st.uri
            else -> null
        }
        if (s == null) {
            Toast.makeText(ctx, "Chưa có bản cập nhật nào để lưu", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { BackupFiles.copy(ctx, s, dst) }
            Toast.makeText(
                ctx,
                if (ok) "Đã lưu bản cài đặt — mở file đó để cài sau khi gỡ app"
                else "Lưu file thất bại, thử lại nhé",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun openUninstallForMigration(ctx: Context) {
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            Toast.makeText(ctx, "Không mở được màn hình gỡ cài đặt", Toast.LENGTH_LONG).show()
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
