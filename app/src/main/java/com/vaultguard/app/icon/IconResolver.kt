package com.vaultguard.app.icon

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class ResolvedIcon {
    data class InstalledApp(val drawable: Drawable, val packageName: String) : ResolvedIcon()
    data class FaviconUrl(val url: String, val domain: String) : ResolvedIcon()
    data class Monogram(val letter: Char, val color: Color) : ResolvedIcon()
    data class CustomUri(val uri: String) : ResolvedIcon()
}

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)

@Singleton
class IconResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager

    private val monogramColors = listOf(
        Color(0xFF0284C7), // Blue
        Color(0xFF10B981), // Emerald
        Color(0xFF8B5CF6), // Purple
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFFEC4899), // Pink
        Color(0xFF06B6D4), // Cyan
        Color(0xFF6366F1), // Indigo
        Color(0xFF14B8A6), // Teal
        Color(0xFFF97316)  // Orange
    )

    fun resolveIcon(
        name: String,
        urlOrPackage: String = "",
        customIconUri: String? = null,
        allowFaviconUrl: Boolean = true
    ): ResolvedIcon {
        // 1. Custom Icon URI if explicitly chosen
        if (!customIconUri.isNullOrBlank()) {
            return ResolvedIcon.CustomUri(customIconUri)
        }

        val trimmedName = name.trim()
        val trimmedTarget = urlOrPackage.trim()

        // 2. Check if installed app matches package name or app label
        val appByPackage = findInstalledAppByPackage(trimmedTarget)
        if (appByPackage != null) {
            return ResolvedIcon.InstalledApp(appByPackage.icon, appByPackage.packageName)
        }

        val appByName = findInstalledAppByName(trimmedName)
        if (appByName != null) {
            return ResolvedIcon.InstalledApp(appByName.icon, appByName.packageName)
        }

        // 3. Map to known service
        val knownService = ServiceMapping.findService(trimmedName)
            ?: ServiceMapping.findService(trimmedTarget)

        if (knownService != null) {
            // Check if service's known package is installed
            if (!knownService.packageName.isNullOrBlank()) {
                val app = findInstalledAppByPackage(knownService.packageName)
                if (app != null) {
                    return ResolvedIcon.InstalledApp(app.icon, app.packageName)
                }
            }

            if (allowFaviconUrl && knownService.domain.isNotBlank()) {
                val faviconUrl = "https://www.google.com/s2/favicons?domain=${knownService.domain}&sz=128"
                return ResolvedIcon.FaviconUrl(faviconUrl, knownService.domain)
            }
        }

        // 4. If urlOrPackage looks like a domain / URL and favicon allowed
        if (allowFaviconUrl && (trimmedTarget.contains(".") || trimmedName.contains("."))) {
            val domain = extractDomain(if (trimmedTarget.contains(".")) trimmedTarget else trimmedName)
            if (domain.isNotBlank()) {
                val faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=128"
                return ResolvedIcon.FaviconUrl(faviconUrl, domain)
            }
        }

        // 5. Fallback: colored circle monogram
        val firstLetter = trimmedName.firstOrNull()?.uppercaseChar()
            ?: trimmedTarget.firstOrNull()?.uppercaseChar()
            ?: 'V'

        val colorIndex = Math.abs(trimmedName.hashCode() % monogramColors.size)
        val color = monogramColors[colorIndex]

        return ResolvedIcon.Monogram(firstLetter, color)
    }

    fun getInstalledApps(): List<InstalledAppInfo> {
        val apps = mutableListOf<InstalledAppInfo>()
        val installed = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (info in installed) {
            // Exclude system framework apps without launcher activity if desired, or keep user-facing
            if ((info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || packageManager.getLaunchIntentForPackage(info.packageName) != null) {
                val appName = packageManager.getApplicationLabel(info).toString()
                val icon = packageManager.getApplicationIcon(info)
                apps.add(InstalledAppInfo(appName, info.packageName, icon))
            }
        }
        return apps.sortedBy { it.appName.lowercase() }
    }

    private fun findInstalledAppByPackage(packageName: String): InstalledAppInfo? {
        if (packageName.isBlank()) return null
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            val name = packageManager.getApplicationLabel(info).toString()
            val icon = packageManager.getApplicationIcon(info)
            InstalledAppInfo(name, packageName, icon)
        } catch (e: Exception) {
            null
        }
    }

    private fun findInstalledAppByName(name: String): InstalledAppInfo? {
        if (name.isBlank()) return null
        val lowerName = name.lowercase()
        return try {
            val installed = packageManager.getInstalledApplications(0)
            for (info in installed) {
                val label = packageManager.getApplicationLabel(info).toString()
                if (label.equals(lowerName, ignoreCase = true) ||
                    label.lowercase().contains(lowerName) ||
                    lowerName.contains(label.lowercase())
                ) {
                    val icon = packageManager.getApplicationIcon(info)
                    return InstalledAppInfo(label, info.packageName, icon)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractDomain(url: String): String {
        var clean = url.trim().lowercase()
        if (clean.startsWith("https://")) clean = clean.removePrefix("https://")
        if (clean.startsWith("http://")) clean = clean.removePrefix("http://")
        if (clean.startsWith("www.")) clean = clean.removePrefix("www.")
        return clean.substringBefore("/").substringBefore(":")
    }
}
