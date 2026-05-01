package com.qrforge.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrforge.app.data.db.QREntity

@Composable
fun HomeScreen(
    onNavigateToGenerate: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToPro: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    uiState.greeting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "QRForge",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "QR",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Pro promo banner ──────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onNavigateToPro() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡", style = MaterialTheme.typography.titleMedium)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Unlock Pro — free 3 days",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Unlimited QRs · Analytics · No ads",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                Text(
                    "Try free →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── Quick Actions ─────────────────────────────────────────────
        SectionLabel("Quick actions")
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                title = "Create QR",
                subtitle = "7 types supported",
                icon = Icons.Default.QrCode,
                iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                iconTint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToGenerate
            )
            QuickActionCard(
                title = "Scan QR",
                subtitle = "Camera or gallery",
                icon = Icons.Default.CameraAlt,
                iconBgColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                iconTint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToScanner
            )
        }

        // ── Stats Row ─────────────────────────────────────────────────
        SectionLabel("Your stats")
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                value = "${uiState.generatedCount}",
                label = "Generated",
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${uiState.scannedCount}",
                label = "Scanned",
                valueColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${uiState.dailyLimit - uiState.todayUsed}/${uiState.dailyLimit}",
                label = "Today left",
                valueColor = if (uiState.todayUsed >= uiState.dailyLimit)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Daily Tip ─────────────────────────────────────────────────
        if (uiState.dailyTip.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        "Tip: ${uiState.dailyTip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
            }
        }

        // ── Recent QRs ────────────────────────────────────────────────
        if (uiState.recentItems.isNotEmpty()) {
            SectionLabel("Recent")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.recentItems.forEach { qr ->
                    RecentQRItem(qr = qr)
                }
            }
        } else {
            // Empty state nudge
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📱", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No QR codes yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Create or scan your first QR code to get started.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToGenerate,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create your first QR")
                    }
                }
            }
        }
    }
}

// ── Reusable components ───────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun RecentQRItem(qr: QREntity) {
    val typeColor = when (qr.type.name) {
        "URL"   -> Color(0xFF00C07A)
        "WIFI"  -> Color(0xFF6C63FF)
        "VCARD" -> Color(0xFFFF5C7A)
        "EMAIL" -> Color(0xFF4CA8FF)
        "PHONE" -> Color(0xFFFFAA40)
        "SMS"   -> Color(0xFFFF6B35)
        else    -> Color(0xFF8B5CF6)
    }
    val typeEmoji = when (qr.type.name) {
        "URL"   -> "🔗"
        "WIFI"  -> "📶"
        "VCARD" -> "👤"
        "EMAIL" -> "📧"
        "PHONE" -> "📞"
        "SMS"   -> "💬"
        else    -> "📝"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(typeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(typeEmoji, style = MaterialTheme.typography.titleSmall)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    qr.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${qr.type.label} · ${formatAge(qr.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (qr.scanCount > 0) {
                Text(
                    "${qr.scanCount} scans",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = typeColor
                )
            }
        }
    }
}

private fun formatAge(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000         -> "Just now"
        diff < 3_600_000      -> "${diff / 60_000}m ago"
        diff < 86_400_000     -> "${diff / 3_600_000}h ago"
        diff < 86_400_000 * 7 -> "${diff / 86_400_000}d ago"
        else                  -> "${diff / (86_400_000 * 7)}w ago"
    }
}