package com.antistalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antistalk.ui.AppNav
import com.antistalk.ui.MainViewModel
import com.antistalk.ui.MainViewModelFactory
import com.antistalk.ui.theme.AntiStalkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as AntiStalkApp).repo
        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModelFactory(repo))
            val mode by vm.themeMode.collectAsState()
            AntiStalkTheme(
                darkTheme = mode == "dark" || (mode != "light" && isSystemInDarkTheme())
            ) {
                AppNav(vm)
            }
        }
    }
}
