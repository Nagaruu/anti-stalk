package com.antistalk

import android.app.Application
import com.antistalk.data.AntiStalkRepository

class AntiStalkApp : Application() {
    lateinit var repo: AntiStalkRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repo = AntiStalkRepository(this)
    }
}
