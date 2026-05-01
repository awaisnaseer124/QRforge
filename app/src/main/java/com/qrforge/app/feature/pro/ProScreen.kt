package com.qrforge.app.feature.pro

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrforge.app.core.billing.ProductInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var selectedId by remember { mutableStateOf("qrforge_pro_yearly") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Already Pro banner ────────────────────────────────────
            if (uiState.isPro) {
                AlreadyProBanner(onNavigateBack)
                return@Column
            }

            // ── Header ────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", style = MaterialTheme.typography.displayLarge)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "QRForge Pro",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Unlock the full experience",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Features list ─────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProFeatureRow("Unlimited QR codes — no daily limit")
                ProFeatureRow("Custom logo & branding inside QR")
                ProFeatureRow("Scan analytics — who, when, where")
                ProFeatureRow("Dynamic QRs — change destination anytime")
                ProFeatureRow("Export as SVG and PDF")
                ProFeatureRow("Bulk create up to 100 QRs at once")
                ProFeatureRow("No ads — completely clean experience")
                ProFeatureRow("All QR types including payments & events")
            }

            // ── Pricing plans ─────────────────────────────────────────
            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.products.isEmpty()) {
                    // Shows until real products load from Play Console
                    FallbackPricingCards(
                        selectedId = selectedId,
                        onSelect = { selectedId = it }
                    )
                } else {
                    // Real Play Store pricing
                    uiState.products
                        .sortedBy { it.priceMicros }
                        .forEach { product ->
                            PricingCard(
                                product = product,
                                isSelected = selectedId == product.productId,
                                isFeatured = product.billingPeriod == "yearly",
                                onClick = { selectedId = product.productId }
                            )
                        }
                }
            }

            // ── CTA Button ────────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val product = uiState.products.firstOrNull { it.productId == selectedId }
                    if (product != null) {
                        viewModel.purchase(context as Activity, product)
                    }
                    // In testing mode with no real products, button is visually active
                    // but does nothing until Play Console products are created
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Start 3-Day Free Trial",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Then billed as selected. Cancel anytime before trial ends.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            // ── Restore + Legal ───────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = { viewModel.restorePurchases() }) {
                    Text("Restore purchase",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp).align(Alignment.CenterVertically))
                TextButton(onClick = {}) {
                    Text("Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp).align(Alignment.CenterVertically))
                TextButton(onClick = {}) {
                    Text("Terms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────

@Composable
private fun ProFeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun PricingCard(
    product: ProductInfo,
    isSelected: Boolean,
    isFeatured: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        when (product.billingPeriod) {
                            "monthly"  -> "Pro Monthly"
                            "yearly"   -> "Pro Annual"
                            "lifetime" -> "Pro Lifetime"
                            else       -> product.title
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        when (product.billingPeriod) {
                            "monthly"  -> "Billed monthly"
                            "yearly"   -> "Billed once a year · Save 44%"
                            "lifetime" -> "One-time payment · Forever"
                            else       -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        product.formattedPrice,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.billingPeriod != "lifetime") {
                        Text(
                            "/ ${product.billingPeriod}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        // "Best value" badge on yearly
        if (isFeatured) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp)
                    .offset(y = (-10).dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    "BEST VALUE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FallbackPricingCards(
    selectedId: String,
    onSelect: (String) -> Unit
) {
    // Shown during testing when Play Store products aren't available
    listOf(
        Triple("qrforge_pro_monthly", "Pro Monthly", "$2.99 / month"),
        Triple("qrforge_pro_yearly", "Pro Annual", "$19.99 / year · Save 44%"),
        Triple("qrforge_pro_lifetime", "Pro Lifetime", "$29.99 one-time")
    ).forEach { (id, title, price) ->
        val isSelected = selectedId == id
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(16.dp)
                )
                .clickable { onSelect(id) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(price, style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AlreadyProBanner(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("You're Pro!", style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "You already have QRForge Pro. Enjoy unlimited QRs, analytics, and no ads!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Back to App", style = MaterialTheme.typography.titleMedium)
        }
    }
}