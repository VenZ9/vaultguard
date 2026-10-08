package com.vaultguard.app.domain.usecase

import com.vaultguard.app.domain.model.SecretType
import com.vaultguard.app.domain.model.VaultItem

enum class AuditSeverity {
    CRITICAL,
    WARNING,
    INFO
}

data class AuditIssue(
    val item: VaultItem,
    val severity: AuditSeverity,
    val title: String,
    val description: String,
    val issueType: AuditIssueType
)

enum class AuditIssueType {
    WEAK_PASSWORD,
    REUSED_PASSWORD,
    OLD_PASSWORD,
    MISSING_PASSKEY
}

data class VaultAuditReport(
    val overallScore: Int, // 0 to 100
    val totalItems: Int,
    val totalLogins: Int,
    val weakCount: Int,
    val reusedCount: Int,
    val staleCount: Int,
    val passkeyCount: Int,
    val issues: List<AuditIssue>
) {
    val rating: String = when {
        overallScore >= 90 -> "EXCELLENT"
        overallScore >= 75 -> "GOOD"
        overallScore >= 50 -> "NEEDS ATTENTION"
        else -> "AT RISK"
    }
}

object SecurityAuditor {

    private const val STALE_THRESHOLD_MS = 90L * 24 * 60 * 60 * 1000 // 90 days

    fun audit(items: List<VaultItem>): VaultAuditReport {
        if (items.isEmpty()) {
            return VaultAuditReport(
                overallScore = 100,
                totalItems = 0,
                totalLogins = 0,
                weakCount = 0,
                reusedCount = 0,
                staleCount = 0,
                passkeyCount = 0,
                issues = emptyList()
            )
        }

        val logins = items.filter { it.type == SecretType.LOGIN || it.type == SecretType.APP_PASSWORD }
        val passkeys = items.filter { it.type == SecretType.PASSKEY }
        val now = System.currentTimeMillis()

        val issues = mutableListOf<AuditIssue>()

        // 1. Weak password check
        val weakItems = mutableListOf<VaultItem>()
        for (item in logins) {
            val pass = item.password
            if (pass.isBlank()) continue
            val score = calculatePasswordScore(pass)
            if (score < 50 || pass.length < 10) {
                weakItems.add(item)
                issues.add(
                    AuditIssue(
                        item = item,
                        severity = if (pass.length < 8) AuditSeverity.CRITICAL else AuditSeverity.WARNING,
                        title = "Weak Password (${pass.length} chars)",
                        description = if (pass.length < 8) "Critically short. Vulnerable to brute-force." else "Low complexity. Recommended: 16+ chars with mixed case and symbols.",
                        issueType = AuditIssueType.WEAK_PASSWORD
                    )
                )
            }
        }

        // 2. Reused password check
        val passwordGroups = logins
            .filter { it.password.isNotBlank() }
            .groupBy { it.password }

        val reusedItems = mutableListOf<VaultItem>()
        for ((_, group) in passwordGroups) {
            if (group.size > 1) {
                for (item in group) {
                    reusedItems.add(item)
                    issues.add(
                        AuditIssue(
                            item = item,
                            severity = AuditSeverity.CRITICAL,
                            title = "Reused Password",
                            description = "Shared across ${group.size} accounts (${group.joinToString { it.name }}). A single breach puts all at risk.",
                            issueType = AuditIssueType.REUSED_PASSWORD
                        )
                    )
                }
            }
        }

        // 3. Stale password check (> 90 days)
        val staleItems = mutableListOf<VaultItem>()
        for (item in logins) {
            val age = now - item.updatedAt
            if (age > STALE_THRESHOLD_MS && item.password.isNotBlank()) {
                val days = (age / (24 * 60 * 60 * 1000)).toInt()
                staleItems.add(item)
                issues.add(
                    AuditIssue(
                        item = item,
                        severity = AuditSeverity.INFO,
                        title = "Old Password ($days days ago)",
                        description = "Not updated in over 3 months. Periodic rotation is recommended.",
                        issueType = AuditIssueType.OLD_PASSWORD
                    )
                )
            }
        }

        // Compute overall score
        val loginCount = logins.size.coerceAtLeast(1)
        val strongRatio = ((loginCount - weakItems.size).coerceAtLeast(0).toDouble() / loginCount)
        val uniqueRatio = ((loginCount - reusedItems.size).coerceAtLeast(0).toDouble() / loginCount)
        val freshRatio = ((loginCount - staleItems.size).coerceAtLeast(0).toDouble() / loginCount)

        // Passkeys give a security bonus (+5% up to 100%)
        val passkeyBonus = (passkeys.size * 5).coerceAtMost(15)

        val baseScore = (strongRatio * 50 + uniqueRatio * 35 + freshRatio * 15).toInt()
        val finalScore = (baseScore + passkeyBonus).coerceIn(0, 100)

        return VaultAuditReport(
            overallScore = finalScore,
            totalItems = items.size,
            totalLogins = logins.size,
            weakCount = weakItems.size,
            reusedCount = reusedItems.size,
            staleCount = staleItems.size,
            passkeyCount = passkeys.size,
            issues = issues.sortedBy { it.severity }
        )
    }

    fun calculatePasswordScore(password: String): Int {
        if (password.isBlank()) return 0
        var score = 0

        // Length
        when {
            password.length >= 16 -> score += 40
            password.length >= 12 -> score += 30
            password.length >= 8 -> score += 15
            else -> score += 5
        }

        // Variety
        if (password.any { it.isUpperCase() }) score += 15
        if (password.any { it.isLowerCase() }) score += 15
        if (password.any { it.isDigit() }) score += 15
        if (password.any { !it.isLetterOrDigit() }) score += 15

        return score.coerceIn(0, 100)
    }
}
