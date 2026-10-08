package com.vaultguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaultguard.app.domain.usecase.PasswordStrength

@Composable
fun PasswordStrengthBar(
    strength: PasswordStrength,
    modifier: Modifier = Modifier
) {
    val barColor = when (strength.score) {
        0 -> Color(0xFFEF4444) // Red
        1 -> Color(0xFFF97316) // Orange
        2 -> Color(0xFFFBBF24) // Yellow
        3 -> Color(0xFF10B981) // Emerald
        else -> Color(0xFF06B6D4) // Cyan
    }

    val animatedColor by animateColorAsState(targetValue = barColor, label = "StrengthColor")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (step in 0..3) {
                val isFilled = strength.score >= step
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isFilled) animatedColor else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Strength: ${strength.label}",
                style = MaterialTheme.typography.labelMedium,
                color = animatedColor
            )
            if (strength.entropyBits > 0) {
                Text(
                    text = "${strength.entropyBits.toInt()} bits entropy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
