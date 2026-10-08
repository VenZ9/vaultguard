package com.vaultguard.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.vaultguard.app.domain.usecase.LockManager
import com.vaultguard.app.security.BiometricHelper
import com.vaultguard.app.ui.navigation.VaultNavGraph
import com.vaultguard.app.ui.theme.VaultGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var lockManager: LockManager

    @Inject
    lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Block screenshots and screen recording across the entire app
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        setContent {
            VaultGuardTheme {
                VaultNavGraph(
                    lockManager = lockManager,
                    biometricHelper = biometricHelper
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lockManager.checkAutoLock()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        lockManager.recordUserInteraction()
    }
}
