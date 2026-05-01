package com.qrforge.app.feature.generate

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrforge.app.core.ads.HomeBannerAd
import com.qrforge.app.core.qr.QRType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPro: () -> Unit,
    viewModel: GenerateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPro   by viewModel.isPro.collectAsStateWithLifecycle(initialValue = false)
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(uiState.saveSuccessMessage)
            viewModel.dismissSaveSuccess()
        }
    }
    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSaveError()
        }
    }

    if (uiState.showRewardedAdOffer) {
        RewardedAdOfferDialog(
            onWatch   = { viewModel.watchRewardedAd(context as Activity) },
            onDismiss = { viewModel.dismissRewardedOffer() },
            onUpgrade = onNavigateToPro
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Create QR", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // ── Type Selector ─────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(QRType.entries) { type ->
                    val isSelected = uiState.selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onTypeSelected(type) },
                        label = { Text(type.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // ── Input Fields ──────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                uiState.fields.forEach { field ->
                    val value = uiState.fieldValues[field.key].orEmpty()
                    OutlinedTextField(
                        value = value,
                        onValueChange = { viewModel.onFieldChanged(field.key, it) },
                        label = { Text(field.label) },
                        placeholder = { Text(field.hint,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (field.isPassword)
                            PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = when (field.key) {
                                "phone" -> KeyboardType.Phone
                                "to"    -> KeyboardType.Email
                                else    -> KeyboardType.Text
                            }
                        ),
                        singleLine = field.key !in listOf("body", "message", "text")
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Color Picker ──────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("QR Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(QR_COLOR_PALETTE) { hex ->
                        val isSelected = uiState.selectedColorHex == hex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .then(if (isSelected) Modifier.border(3.dp,
                                    MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier)
                                .clickable { viewModel.onColorSelected(hex) }
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null,
                                    tint = if (hex in listOf("#000000","#6C63FF","#FF5C7A","#8B5CF6"))
                                        Color.White else Color.Black,
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Branding Panel (Pro) ──────────────────────────────────
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))

            BrandingPanel(
                logoBitmap       = uiState.logoBitmap,
                selectedFrameId  = uiState.selectedFrameId,
                onLogoSelected   = { viewModel.onLogoSelected(it) },
                onFrameSelected  = { viewModel.onFrameSelected(it) },
                onNavigateToPro  = onNavigateToPro,
                isPro            = isPro
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(20.dp))

            // ── QR Preview ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp))
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            uiState.isGenerating -> {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp))
                            }
                            uiState.finalBitmap != null -> {
                                Image(
                                    bitmap = uiState.finalBitmap!!.asImageBitmap(),
                                    contentDescription = "Generated QR Code",
                                    modifier = Modifier.fillMaxSize().padding(8.dp))
                            }
                            else -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📱", style = MaterialTheme.typography.displayLarge)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Fill in the fields above\nto generate your QR",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    // Export format buttons
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.saveToGallery(context) },
                            enabled = uiState.finalBitmap != null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("PNG", maxLines = 1, softWrap = false) }

                        OutlinedButton(
                            onClick = {
                                if (isPro) viewModel.exportSvg(context)
                                else onNavigateToPro()
                            },
                            enabled = uiState.finalBitmap != null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = if (isPro) ButtonDefaults.outlinedButtonColors()
                            else ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(if (isPro) "SVG" else "SVG Pro",
                                maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                if (isPro) viewModel.exportPdf(context)
                                else onNavigateToPro()
                            },
                            enabled = uiState.finalBitmap != null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = if (isPro) ButtonDefaults.outlinedButtonColors()
                            else ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(if (isPro) "PDF" else "PDF Pro",
                                maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Action Buttons ────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.saveToGallery(context) },
                    enabled = uiState.finalBitmap != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download PNG", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { viewModel.shareQR(context) },
                    enabled = uiState.finalBitmap != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Share", style = MaterialTheme.typography.titleMedium)
                }

                val effectiveLimit = uiState.dailyLimit + uiState.bonusQRs
                if (!isPro && (uiState.dailyQRCount > 0 || uiState.bonusQRs > 0)) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${uiState.dailyQRCount}/$effectiveLimit free QRs used today" +
                                        if (uiState.bonusQRs > 0) " (+${uiState.bonusQRs} bonus)" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onNavigateToPro,
                                contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text("Go Pro", style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }

            // Banner ad — free users only
            if (!isPro) {
                Spacer(Modifier.height(12.dp))
                HomeBannerAd(modifier = Modifier.padding(horizontal = 16.dp))
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RewardedAdOfferDialog(
    onWatch: () -> Unit,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Daily limit reached", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
        },
        text = {
            Text("You've used all 5 free QRs today. Watch a short ad to unlock 2 more, or upgrade to Pro for unlimited QRs.",
                style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(onClick = onWatch, shape = RoundedCornerShape(12.dp)) {
                Text("Watch Ad (+2 QRs)")
            }
        },
        dismissButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onUpgrade) {
                    Text("Upgrade to Pro", color = MaterialTheme.colorScheme.secondary)
                }
                TextButton(onClick = onDismiss) {
                    Text("Maybe later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}