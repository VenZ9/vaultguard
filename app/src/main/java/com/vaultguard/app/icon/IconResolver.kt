package com.vaultguard.app.icon

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
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

    init {
        // Initialize background scan once so main thread is never blocked
        if (hasStartedScan.compareAndSet(false, true)) {
            CoroutineScope(Dispatchers.IO).launch {
                warmUpInstalledAppsCache()
            }
        }
    }

    companion object {
        @Volatile
        private var instance: IconResolver? = null

        fun getInstance(context: Context): IconResolver {
            return instance ?: synchronized(this) {
                instance ?: IconResolver(context.applicationContext).also { instance = it }
            }
        }

        private val iconCache = ConcurrentHashMap<String, ResolvedIcon>()
        private val appNameCache = ConcurrentHashMap<String, String>() // lowercase app name -> package name
        private val installedPackagesCache = ConcurrentHashMap<String, InstalledAppInfo>()
        private val hasStartedScan = AtomicBoolean(false)

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
    }

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
        val cacheKey = "$trimmedName|$trimmedTarget|$allowFaviconUrl"

        // Check memory cache first - O(1) instant return (0.001ms)
        iconCache[cacheKey]?.let { return it }

        // 2. Known service mapping - O(1) in-memory table lookup
        val knownService = ServiceMapping.findService(trimmedName)
            ?: ServiceMapping.findService(trimmedTarget)

        if (knownService != null) {
            // Check if known package is installed (O(1) cached lookup)
            if (!knownService.packageName.isNullOrBlank()) {
                val app = findInstalledAppByPackage(knownService.packageName)
                if (app != null) {
                    val resolved = ResolvedIcon.InstalledApp(app.icon, app.packageName)
                    iconCache[cacheKey] = resolved
                    return resolved
                }
            }

            if (allowFaviconUrl && knownService.domain.isNotBlank()) {
                val resolved = ResolvedIcon.FaviconUrl(
                    "https://www.google.com/s2/favicons?domain=${knownService.domain}&sz=128",
                    knownService.domain
                )
                iconCache[cacheKey] = resolved
                return resolved
            }
        }

        // 3. Direct package lookup if urlOrPackage is an explicit package name (O(1))
        if (isLikelyPackageName(trimmedTarget)) {
            val app = findInstalledAppByPackage(trimmedTarget)
            if (app != null) {
                val resolved = ResolvedIcon.InstalledApp(app.icon, app.packageName)
                iconCache[cacheKey] = resolved
                return resolved
            }
        }

        // 4. Quick match against cached installed app names (O(1) hash map lookup)
        val cachedPackage = appNameCache[trimmedName.lowercase()]
        if (cachedPackage != null) {
            val app = findInstalledAppByPackage(cachedPackage)
            if (app != null) {
                val resolved = ResolvedIcon.InstalledApp(app.icon, app.packageName)
                iconCache[cacheKey] = resolved
                return resolved
            }
        }

        // 5. Domain / URL check for favicon
        if (allowFaviconUrl && (trimmedTarget.contains(".") || trimmedName.contains("."))) {
            val domain = extractDomain(if (trimmedTarget.contains(".")) trimmedTarget else trimmedName)
            if (domain.isNotBlank()) {
                val resolved = ResolvedIcon.FaviconUrl(
                    "https://www.google.com/s2/favicons?domain=$domain&sz=128",
                    domain
                )
                iconCache[cacheKey] = resolved
                return resolved
            }
        }

        // 6. Instant Monogram fallback
        val firstLetter = trimmedName.firstOrNull()?.uppercaseChar()
            ?: trimmedTarget.firstOrNull()?.uppercaseChar()
            ?: 'V'

        val colorIndex = Math.abs(trimmedName.hashCode() % monogramColors.size)
        val resolved = ResolvedIcon.Monogram(firstLetter, monogramColors[colorIndex])
        iconCache[cacheKey] = resolved
        return resolved
    }

    private fun warmUpInstalledAppsCache() {
        try {
            val installed = packageManager.getInstalledApplications(0)
            for (info in installed) {
                val label = packageManager.getApplicationLabel(info).toString()
                appNameCache[label.lowercase()] = info.packageName
            }
        } catch (e: Exception) {
            // Ignore background scan exception
        }
    }

    fun getInstalledApps(): List<InstalledAppInfo> {
        val apps = mutableListOf<InstalledAppInfo>()
        try {
            val installed = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            for (info in installed) {
                if ((info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || packageManager.getLaunchIntentForPackage(info.packageName) != null) {
                    val appName = packageManager.getApplicationLabel(info).toString()
                    val icon = packageManager.getApplicationIcon(info)
                    val appInfo = InstalledAppInfo(appName, info.packageName, icon)
                    installedPackagesCache[info.packageName] = appInfo
                    appNameCache[appName.lowercase()] = info.packageName
                    apps.add(appInfo)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return apps.sortedBy { it.appName.lowercase() }
    }

    private fun findInstalledAppByPackage(packageName: String): InstalledAppInfo? {
        if (packageName.isBlank()) return null
        installedPackagesCache[packageName]?.let { return it }

        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            val name = packageManager.getApplicationLabel(info).toString()
            val icon = packageManager.getApplicationIcon(info)
            val appInfo = InstalledAppInfo(name, packageName, icon)
            installedPackagesCache[packageName] = appInfo
            appNameCache[name.lowercase()] = packageName
            appInfo
        } catch (e: Exception) {
            null
        }
    }

    private fun isLikelyPackageName(target: String): Boolean {
        return target.contains(".") && !target.contains("/") && !target.contains(":") &&
                (target.startsWith("com.") || target.startsWith("org.") || target.startsWith("net.") ||
                 target.startsWith("io.") || target.startsWith("app.") || target.startsWith("dev."))
    }

    private fun extractDomain(url: String): String {
        var clean = url.trim().lowercase()
        if (clean.startsWith("https://")) clean = clean.removePrefix("https://")
        if (clean.startsWith("http://")) clean = clean.removePrefix("http://")
        if (clean.startsWith("www.")) clean = clean.removePrefix("www.")
        return clean.substringBefore("/").substringBefore(":")
    }
}
