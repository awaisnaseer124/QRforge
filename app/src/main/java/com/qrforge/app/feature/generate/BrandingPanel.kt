package com.qrforge.app.feature.generate

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qrforge.app.core.qr.LogoOverlayProcessor

data class QRFrame(
    val id: String,
    val label: String,
    val cornerRadius: Float,
    val isPro: Boolean = true
)

val QR_FRAMES = listOf(
    QRFrame("none",    "None",    0f,   isPro = false),
    QRFrame("rounded", "Rounded", 12f),
    QRFrame("soft",    "Soft",    20f),
    QRFrame("circle",  "Circle",  500f)
)

@Composable
fun BrandingPanel(
    logoBitmap: Bitmap?,
    selectedFrameId: String,
    onLogoSelected: (Bitmap?) -> Unit,
    onFrameSelected: (String) -> Unit,
    onNavigateToPro: () -> Unit,
    isPro: Boolean
) {
    val context = LocalContext.current

    // Gallery launcher for logo picking
    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = LogoOverlayProcessor.loadFromUri(context, it)
            onLogoSelected(bitmap)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Logo section ──────────────────────────────────────────────
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Logo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (!isPro) {
                ProBadge(onClick = onNavigateToPro)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Current logo preview or add button
            if (logoBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Image(
                        bitmap = logoBitmap.asImageBitmap(),
                        contentDescription = "Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Remove button
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .clickable { onLogoSelected(null) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove logo",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            // Add logo button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        if (isPro) logoLauncher.launch("image/*")
                        else onNavigateToPro()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add logo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Logo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isPro) {
                Text(
                    "Add your brand logo inside the QR code",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Frame / corner style section ─────────────────────────────
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "QR Style",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(QR_FRAMES) { frame ->
                val isSelected = selectedFrameId == frame.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            if (!frame.isPro || isPro) onFrameSelected(frame.id)
                            else onNavigateToPro()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Mini QR preview with frame style
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(
                                    if (frame.cornerRadius > 100f) CircleShape
                                    else RoundedCornerShape(frame.cornerRadius.dp)
                                )
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        )
                        // Pro lock overlay
                        if (frame.isPro && !isPro) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔒", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        frame.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProBadge(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            "⚡ Pro",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
    }
}