package com.vaultguard.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vaultguard.app.icon.IconResolver
import com.vaultguard.app.icon.ResolvedIcon

@Composable
fun ServiceIconView(
    name: String,
    urlOrPackage: String = "",
    customIconUri: String? = null,
    allowFavicon: Boolean = true,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconResolver = remember(context) { IconResolver.getInstance(context) }
    val resolvedIcon = remember(name, urlOrPackage, customIconUri, allowFavicon) {
        iconResolver.resolveIcon(name, urlOrPackage, customIconUri, allowFavicon)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (resolvedIcon) {
            is ResolvedIcon.InstalledApp -> {
                val bitmap: Bitmap = remember(resolvedIcon, size) {
                    val targetPx = (size.value * 2).toInt().coerceAtLeast(48)
                    resolvedIcon.drawable.toBitmap(targetPx, targetPx)
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = name,
                    modifier = Modifier.size(size),
                    contentScale = ContentScale.Crop
                )
            }
            is ResolvedIcon.FaviconUrl -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(resolvedIcon.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    modifier = Modifier.size(size * 0.75f),
                    contentScale = ContentScale.Fit
                )
            }
            is ResolvedIcon.CustomUri -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(resolvedIcon.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    modifier = Modifier.size(size),
                    contentScale = ContentScale.Crop
                )
            }
            is ResolvedIcon.Monogram -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(resolvedIcon.color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = resolvedIcon.letter.toString(),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.45f).sp
                    )
                }
            }
        }
    }
}
