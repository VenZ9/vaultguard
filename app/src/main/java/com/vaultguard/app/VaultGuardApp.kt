package com.vaultguard.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class VaultGuardApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Plant debug tree only during development
        Timber.plant(Timber.DebugTree())
    }
}
