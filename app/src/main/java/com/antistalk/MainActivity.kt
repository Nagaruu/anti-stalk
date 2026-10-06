package com.antistalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antistalk.ui.AppNav
import com.antistalk.ui.MainViewModel
import com.antistalk.ui.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as AntiStalkApp).repo
        setContent {
            MaterialTheme {
                Surface {
                    val vm: MainViewModel = viewModel(factory = MainViewModelFactory(repo))
                    AppNav(vm)
                }
            }
        }
    }
}
