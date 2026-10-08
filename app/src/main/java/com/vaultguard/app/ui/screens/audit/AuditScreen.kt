package com.vaultguard.app.ui.screens.audit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaultguard.app.domain.usecase.AuditIssue
import com.vaultguard.app.domain.usecase.AuditIssueType
import com.vaultguard.app.domain.usecase.AuditSeverity
import com.vaultguard.app.ui.components.ServiceIconView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(
    viewModel: AuditViewModel,
    onNavigateToItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val report = uiState.report
    val haptic = LocalHapticFeedback.current

    val filteredIssues = if (uiState.selectedFilter == null) {
        report.issues
    } else {
        report.issues.filter { it.issueType == uiState.selectedFilter }
    }

    val animatedScore by animateFloatAsState(
        targetValue = report.overallScore / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "scoreAnim"
    )

    val gaugeColor by animateColorAsState(
        targetValue = when {
            report.overallScore >= 90 -> Color(0xFF2E7D32) // Emerald green
            report.overallScore >= 75 -> Color(0xFF00897B) // Teal
            report.overallScore >= 50 -> Color(0xFFF57C00) // Orange
            else -> Color(0xFFD32F2F) // Red
        },
        label = "colorAnim"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Security Audit",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Health Gauge Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vault Health Score",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${report.totalItems} items audited",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(gaugeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = report.rating,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = gaugeColor
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(96.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { animatedScore },
                                modifier = Modifier.size(96.dp),
                                color = gaugeColor,
                                strokeWidth = 8.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${report.overallScore}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = gaugeColor
                                )
                            }
                        }
                    }
                }
            }

            // 2. Metrics Summary
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCounterCard(
                        title = "Weak",
                        count = report.weakCount,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setFilter(AuditIssueType.WEAK_PASSWORD) }
                    )
                    MetricCounterCard(
                        title = "Reused",
                        count = report.reusedCount,
                        color = Color(0xFFF57C00),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setFilter(AuditIssueType.REUSED_PASSWORD) }
                    )
                    MetricCounterCard(
                        title = "Old",
                        count = report.staleCount,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setFilter(AuditIssueType.OLD_PASSWORD) }
                    )
                    MetricCounterCard(
                        title = "Passkeys",
                        count = report.passkeyCount,
                        color = Color(0xFF388E3C),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setFilter(null) }
                    )
                }
            }

            // 3. Filter Chips
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedFilter == null,
                            onClick = { viewModel.setFilter(null) },
                            label = { Text("All (${report.issues.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.selectedFilter == AuditIssueType.WEAK_PASSWORD,
                            onClick = { viewModel.setFilter(AuditIssueType.WEAK_PASSWORD) },
                            label = { Text("Weak (${report.weakCount})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.selectedFilter == AuditIssueType.REUSED_PASSWORD,
                            onClick = { viewModel.setFilter(AuditIssueType.REUSED_PASSWORD) },
                            label = { Text("Reused (${report.reusedCount})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.selectedFilter == AuditIssueType.OLD_PASSWORD,
                            onClick = { viewModel.setFilter(AuditIssueType.OLD_PASSWORD) },
                            label = { Text("Old (${report.staleCount})") }
                        )
                    }
                }
            }

            // 4. Issues List or Clean State
            if (filteredIssues.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Vulnerabilities Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your passwords and passkeys are unique and resilient against attacks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredIssues) { issue ->
                    AuditIssueCard(
                        issue = issue,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateToItem(issue.item.id)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun MetricCounterCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AuditIssueCard(
    issue: AuditIssue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val severityColor = when (issue.severity) {
        AuditSeverity.CRITICAL -> Color(0xFFD32F2F)
        AuditSeverity.WARNING -> Color(0xFFF57C00)
        AuditSeverity.INFO -> Color(0xFF1976D2)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ServiceIconView(
                name = issue.item.name,
                urlOrPackage = issue.item.urlOrPackage,
                customIconUri = issue.item.customIconUri,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = issue.item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(severityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = issue.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = severityColor
                        )
                    }
                }

                if (issue.item.username.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = issue.item.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = issue.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Review",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
