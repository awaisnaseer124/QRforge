package com.qrforge.app.core.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QRAnalyzer(
    private val onQRCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_DATA_MATRIX
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    // Throttle: only process one frame per 500ms to save battery
    private var lastAnalyzedTimestamp = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < 500L) {
            imageProxy.close()
            return
        }
        lastAnalyzedTimestamp = currentTimestamp

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        if (value.isNotBlank()) {
                            onQRCodeDetected(value)
                        }
                    }
                }
            }
            .addOnFailureListener {
                // Silently ignore — next frame will try again
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}