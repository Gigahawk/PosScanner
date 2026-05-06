package com.gigahawk.posscanner

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.markusfisch.android.barcodescannerview.widget.BarcodeScannerView
import de.markusfisch.android.zxingcpp.ZxingCpp

@Composable
fun BarcodeScannerViewWrapper(
  viewModel: CameraViewModel

) {

  val lifecycleOwner = LocalLifecycleOwner.current
  val selectedCamera by viewModel.selectedCameraItem.collectAsState()

  var scannerView by remember { mutableStateOf<BarcodeScannerView?>(null) }
  var lastOpenedId by remember {mutableStateOf<Int?>(null)}

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          val id = selectedCamera?.id
          if (id != null)
            scannerView?.openAsync(id)
          else
            scannerView?.openAsync()
          lastOpenedId = id
        }
        Lifecycle.Event.ON_PAUSE -> {
          scannerView?.close()
          lastOpenedId = null
        }
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      scannerView?.close()
    }
  }

  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
      BarcodeScannerView(ctx).apply {
        cropRatio = 0.75f
        // TODO: pull in from settings
        formats.add(
          ZxingCpp.BarcodeFormat.DATA_MATRIX
        )
        setOnBarcodeListener { barcode ->
          Log.d("MainScreen2", "Barcode found in barcode view: ${barcode.text}")
          true
        }

        scannerView = this
      }
    },
    update = { view ->
      val currentId = selectedCamera?.id
      if (currentId != null && currentId != lastOpenedId) {
        lastOpenedId = currentId
        view.close()
        view.openAsync(currentId)
      }
    }
  )
}