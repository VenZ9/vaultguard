package com.vaultguard.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp), // Inputs & dialogs
    large = RoundedCornerShape(16.dp),  // Cards & sheets
    extraLarge = RoundedCornerShape(28.dp) // FABs & pill chips
)
