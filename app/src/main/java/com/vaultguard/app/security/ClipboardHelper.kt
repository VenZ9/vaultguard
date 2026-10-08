package com.vaultguard.app.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var clearJob: Job? = null

    fun copyToClipboard(
        label: String,
        text: String,
        isSensitive: Boolean = true,
        autoClearSeconds: Int = 30
    ) {
        val clipData = ClipData.newPlainText(label, text)

        if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val bundle = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clipData.description.extras = bundle
        }

        clipboardManager.setPrimaryClip(clipData)

        if (autoClearSeconds > 0) {
            scheduleClear(text, autoClearSeconds)
        }
    }

    private fun scheduleClear(expectedText: String, delaySeconds: Int) {
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(delaySeconds * 1000L)
            val currentClip = clipboardManager.primaryClip
            if (currentClip != null && currentClip.itemCount > 0) {
                val currentText = currentClip.getItemAt(0).text?.toString()
                if (currentText == expectedText) {
                    val emptyClip = ClipData.newPlainText("", "")
                    clipboardManager.setPrimaryClip(emptyClip)
                }
            }
        }
    }
}
