package com.warehouse.scanner.ui.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScanDialog(
    onScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("扫描二维码看板")
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭") }
            }
        },
        text = {
            if (!hasPermission) {
                Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    Text("需要相机权限")
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val analyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                var scanned = false
                                analyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy ->
                                    if (scanned) {
                                        proxy.close()
                                        return@setAnalyzer
                                    }
                                    val img = proxy.image ?: run { proxy.close(); return@setAnalyzer }
                                    try {
                                        val input = InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees)
                                        BarcodeScanning.getClient().process(input)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    val value = barcode.displayValue
                                                    Log.d("QrScan", "Barcode type: ${barcode.format}, value: $value")
                                                    if (!value.isNullOrBlank()) {
                                                        scanned = true
                                                        onScanned(value.trim())
                                                        return@addOnSuccessListener
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("QrScan", "Scan error", e)
                                            }
                                            .addOnCompleteListener {
                                                img.close()
                                                proxy.close()
                                            }
                                    } catch (e: Exception) {
                                        Log.e("QrScan", "Image analysis error", e)
                                        img.close()
                                        proxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview, analyzer
                                    )
                                } catch (e: Exception) {
                                    Log.e("QrScan", "Camera bind error", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(350.dp).background(Color.Black)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("手动输入") }
        }
    )
}
