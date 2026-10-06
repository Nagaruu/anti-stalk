package com.antistalk.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.antistalk.core.AppUpdater
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
    var goal by remember { mutableStateOf("Người yêu cũ") }
    val start = if (!onboarded) "onboarding" else "main"

    NavHost(navController = nav, startDestination = start) {
        composable("onboarding") {
            OnboardingScreen(onDone = { g -> goal = g; nav.navigate("permissions") })
        }
        composable("permissions") {
            PermissionsScreen(onDone = { vm.setOnboardingDone(); nav.navigate("main") { popUpTo(0) } })
        }
        composable("main") { MainTabs(vm, goal) }
    }
}

@Composable
private fun MainTabs(vm: MainViewModel, goal: String) {
    var tab by remember { mutableStateOf(0) }
    val ctx = LocalContext.current
    val update by vm.updateAvailable.collectAsState()

    // Silent check once per app start; dialog only shows when an update exists.
    LaunchedEffect(Unit) { vm.checkUpdate() }

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
                    AppUpdater.downloadAndInstall(ctx, update!!.apkUrl)
                    vm.skipUpdate()
                }) { Text("CẬP NHẬT") }
            },
            dismissButton = {
                TextButton(onClick = { vm.skipUpdate() }) { Text("ĐỂ SAU") }
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Hôm nay") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Person, null) }, label = { Text("Né ai") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2; vm.refreshStats() },
                    icon = { Icon(Icons.Filled.Star, null) }, label = { Text("Thống kê") })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("Cài đặt") })
            }
        }
    ) { pad ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(pad)) {
            when (tab) {
                0 -> HomeScreen(vm)
                1 -> PersonsScreen(vm, goal)
                2 -> StatsScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
    }
}
