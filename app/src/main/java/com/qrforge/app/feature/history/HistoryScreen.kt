package com.qrforge.app.feature.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import com.qrforge.app.core.ads.HistoryBannerAd
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrforge.app.data.db.QREntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToGenerate: () -> Unit,
    onNavigateToPro: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Selected item for detail sheet
    var selectedItem by remember { mutableStateOf<QREntity?>(null) }

    LaunchedEffect(uiState.deletedItem) {
        uiState.deletedItem?.let {
            val result = snackbarHostState.showSnackbar(
                message = "\"${it.label}\" deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
            else viewModel.dismissUndo()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "History",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.totalCount > 0) {
                            Text(
                                "${uiState.totalCount} total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search QR codes...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(HistoryFilter.entries) { filter ->
                    val isSelected = uiState.selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onFilterSelected(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                uiState.isEmpty -> {
                    EmptyHistoryState(onNavigateToGenerate)
                }
                uiState.items.isNullOrEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No results found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = uiState.items ?: emptyList(), key = { it.id }) { qr ->
                            HistoryItem(
                                qr = qr,
                                onClick = { selectedItem = qr },
                                onDelete = { viewModel.deleteQR(qr) }
                            )
                        }
                        item {
                            HistoryBannerAd(modifier = Modifier.padding(vertical = 8.dp))
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Detail bottom sheet — shown when user taps an item
    selectedItem?.let { item ->
        QRDetailSheet(
            qr = item,
            context = context,
            onDismiss = { selectedItem = null },
            onDelete = {
                viewModel.deleteQR(item)
                selectedItem = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QRDetailSheet(
    qr: QREntity,
    context: Context,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }

    val typeColor = typeColor(qr.type.name)
    val typeEmoji = typeEmoji(qr.type.name)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Type header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(typeEmoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Text(
                    qr.type.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = typeColor
                )
            }

            Text(
                formatFullDate(qr.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Label
            Text(
                qr.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Full raw content — fully visible, no truncation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Full Content",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    // Show entire content — NO maxLines limit
                    Text(
                        text = qr.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetaChip(
                    label = if (qr.isScanned) "Scanned" else "Generated",
                    color = if (qr.isScanned) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                if (qr.scanCount > 0) {
                    MetaChip(
                        label = "${qr.scanCount} scans",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action buttons — icon buttons in a row, clean and never wrap
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Copy
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("QR Content", qr.content)
                        )
                        copied = true
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null,
                        modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (copied) "Copied!" else "Copy",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Open in browser (URLs only)
                if (qr.type.name == "URL") {
                    Button(
                        onClick = {
                            try {
                                val url = if (qr.content.startsWith("http")) qr.content
                                else "https://${qr.content}"
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (e: Exception) { }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null,
                            modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Open",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Share
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, qr.content)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }, "Share"
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null,
                        modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Share",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Delete
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete this QR", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun MetaChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HistoryItem(
    qr: QREntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val color = typeColor(qr.type.name)
    val emoji = typeEmoji(qr.type.name)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, style = MaterialTheme.typography.titleMedium) }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = qr.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(qr.type.label, style = MaterialTheme.typography.labelSmall,
                            color = color, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        if (qr.isScanned) "Scanned" else "Generated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatDate(qr.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Preview of content — 1 line only, full content shown in detail sheet
                Text(
                    text = qr.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (qr.scanCount > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${qr.scanCount}", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("scans", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(onNavigateToGenerate: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("📋", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text("No QR codes yet", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Generate or scan your first QR code\nand it will appear here automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onNavigateToGenerate,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create your first QR", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────

private fun typeColor(typeName: String) = when (typeName) {
    "URL"   -> Color(0xFF00C07A)
    "WIFI"  -> Color(0xFF6C63FF)
    "VCARD" -> Color(0xFFFF5C7A)
    "EMAIL" -> Color(0xFF4CA8FF)
    "PHONE" -> Color(0xFFFFAA40)
    "SMS"   -> Color(0xFFFF6B35)
    else    -> Color(0xFF8B5CF6)
}

private fun typeEmoji(typeName: String) = when (typeName) {
    "URL"   -> "🔗"
    "WIFI"  -> "📶"
    "VCARD" -> "👤"
    "EMAIL" -> "📧"
    "PHONE" -> "📞"
    "SMS"   -> "💬"
    else    -> "📝"
}

private fun formatDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000         -> "Just now"
        diff < 3_600_000      -> "${diff / 60_000}m ago"
        diff < 86_400_000     -> "${diff / 3_600_000}h ago"
        diff < 86_400_000 * 7 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatFullDate(timestamp: Long): String =
    SimpleDateFormat("EEEE, MMM d yyyy  •  h:mm a", Locale.getDefault())
        .format(Date(timestamp))