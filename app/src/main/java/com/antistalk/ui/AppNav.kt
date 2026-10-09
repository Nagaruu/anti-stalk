package com.antistalk.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.antistalk.ui.screens.BackupSection
import com.antistalk.ui.screens.DisclosureScreen
import com.antistalk.ui.screens.HomeScreen
import com.antistalk.ui.screens.OnboardingScreen
import com.antistalk.ui.screens.PermissionsScreen
import com.antistalk.ui.screens.PersonsScreen
import com.antistalk.ui.screens.SettingsScreen
import com.antistalk.ui.screens.StatsScreen

@Composable
fun AppNav(vm: MainViewModel) {
    val nav = rememberNavController()
    val onboarded by vm.onboardingDone.collectAsState()
    var goal by rememberSaveable { mutableStateOf(vm.userGoal) }
    val start = if (!onboarded) "onboarding" else "main"

    // startDestination is only read once per NavHost, so a wipe (which clears
    // onboarding_done) would otherwise leave the user on "main" with empty data
    // while the flag says onboarding is still pending.
    LaunchedEffect(onboarded) {
        val route = nav.currentDestination?.route ?: return@LaunchedEffect
        if (!onboarded && route == "main") {
            nav.navigate("onboarding") {
                popUpTo(nav.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(navController = nav, startDestination = start) {
        composable("onboarding") {
            OnboardingScreen(onDone = { g ->
                goal = g
                vm.userGoal = g
                nav.navigate("disclosure")
            })
        }
        composable("disclosure") {
            DisclosureScreen(
                onAccept = {
                    vm.acceptDisclosure()
                    nav.navigate("permissions")
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable("permissions") {
            PermissionsScreen(onDone = { vm.setOnboardingDone(); nav.navigate("main") { popUpTo(0) } })
        }
        composable("main") { MainTabs(vm, goal) }
    }
}

@Composable
private fun MainTabs(vm: MainViewModel, goal: String) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val update by vm.updateAvailable.collectAsState()
    val updState by vm.updateState.collectAsState()
    val dismissedTag by vm.migrationDismissedTag.collectAsState()

    // Silent check once per app start (auto-downloads in background);
    // dialog only shows when an update exists.
    LaunchedEffect(Unit) {
        vm.refreshSignature(ctx)
        vm.checkUpdate(ctx)
    }

    // Near-auto update: download finished -> open the system installer
    // by itself, once per tag. The user only taps the Install button.
    LaunchedEffect(updState) {
        if (updState is MainViewModel.UpdateState.ReadyToInstall) {
            vm.consumeReadyToInstall(ctx)
        }
    }

    // APK downloaded but its signer differs from the installed app's:
    // Android would fail the install with "gói xung đột", so we walk the
    // user through the one-off uninstall/reinstall instead of failing there.
    val migration = updState as? MainViewModel.UpdateState.NeedsMigration
    if (migration != null && migration.info.tag != dismissedTag) {
        AlertDialog(
            onDismissRequest = { vm.dismissMigration() },
            title = { Text("Cần chuyển đổi 1 lần 😅") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Bản ${migration.info.tag} dùng khoá ký mới, còn app đang cài (khoá ký cũ từ bản " +
                            "build trước) nên Android không cho ghi đè lên. Chỉ cần gỡ rồi cài lại đúng 1 lần — " +
                            "từ bản này trở đi cập nhật chạy bình thường.\n\n" +
                            "Làm theo 4 bước dưới đây, bấm nút là xong:"
                    )
                    Text("1. Xuất sao lưu JSON — giữ file này để lấy dữ liệu về sau.")
                    Text("2. Lưu bản cài đặt APK — file nằm ngoài bộ nhớ app nên sống qua lần gỡ.")
                    Text("3. Bấm GỠ CÀI ĐẶT.")
                    Text("4. Mở file APK vừa lưu (trong Tải xuống) để cài, rồi mở app → Cài đặt → Khôi phục.")
                    Spacer(Modifier.height(2.dp))
                    BackupSection(vm, showRestore = false)
                }
            },
            confirmButton = {
                Button(onClick = { vm.openUninstallForMigration(ctx) }) { Text("GỠ CÀI ĐẶT") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissMigration() }) { Text("ĐỂ SAU") }
            }
        )
    }

    if (update != null) {
        AlertDialog(
            onDismissRequest = { vm.skipUpdate() },
            title = { Text("Có đồ mới nè 😏") },
            text = {
                Text(
                    "Bản ${update!!.tag} có rồi (bạn đang dùng ${vm.versionLabel}). " +
                        "Update không bạn thân?\n\n${update!!.notes.take(300)}"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // Tag matters: an empty KEY_DOWNLOADED_TAG breaks the
                    // resume/offline fast-path (savedTag -> Int parse fails).
                    // The VM path is used instead of AppUpdater.downloadAndInstall
                    // so a signer mismatch lands in the migration dialog.
                    vm.skipUpdate()
                    vm.startDownload(ctx, update ?: return@TextButton)
                }) { Text("CẬP NHẬT") }
            },
            dismissButton = {
                TextButton(onClick = { vm.skipUpdate() }) { Text("ĐỂ SAU") }
            }
        )
    }

    val isDark = com.antistalk.ui.theme.LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    val navColors = androidx.compose.material3.NavigationBarItemDefaults.colors(
        selectedIconColor = if (isDark) com.antistalk.ui.theme.BrandViolet else com.antistalk.ui.theme.WarmSage,
        selectedTextColor = if (isDark) com.antistalk.ui.theme.BrandViolet else com.antistalk.ui.theme.WarmSage,
        indicatorColor = if (isDark) Color(0x338B5CF6L) else com.antistalk.ui.theme.WarmSageLight,
        unselectedIconColor = cs.onSurfaceVariant,
        unselectedTextColor = cs.onSurfaceVariant
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = cs.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.border(
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) cs.outlineVariant else cs.outline
                    )
                )
            ) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, null) },
                    label = { Text("Hôm nay", fontWeight = if (tab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Person, null) },
                    label = { Text("Né ai", fontWeight = if (tab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2; vm.refreshStats() },
                    icon = { Icon(Icons.Filled.Star, null) },
                    label = { Text("Thống kê", fontWeight = if (tab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Settings, null) },
                    label = { Text("Cài đặt", fontWeight = if (tab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = navColors
                )
            }
        }
    ) { pad ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(pad)) {
            Crossfade(
                targetState = tab,
                animationSpec = tween(220),
                label = "tab_crossfade"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HomeScreen(vm)
                    1 -> PersonsScreen(vm, goal)
                    2 -> StatsScreen(vm)
                    else -> SettingsScreen(vm)
                }
            }
        }
    }
}

