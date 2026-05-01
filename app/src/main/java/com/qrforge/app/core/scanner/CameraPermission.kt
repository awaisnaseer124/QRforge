package com.qrforge.app.core.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionHandler(
    onPermissionGranted: @Composable () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    when {
        cameraPermissionState.status.isGranted -> {
            onPermissionGranted()
        }
        cameraPermissionState.status.shouldShowRationale -> {
            // User denied once — explain why we need it
            PermissionRationaleScreen(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
            )
        }
        else -> {
            // First time asking
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
            PermissionRationaleScreen(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
            )
        }
    }
}

@Composable
private fun PermissionRationaleScreen(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Camera Access Needed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "QRForge needs camera access to scan QR codes. Your camera is only used for scanning — no photos are saved.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Grant Camera Permission",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}