package com.gigahawk.posscanner

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ExperimentalLensFacing
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gigahawk.posscanner.icons.MaterialSymbolsBarcodeReader
import com.gigahawk.posscanner.icons.MaterialSymbolsDevices
import com.gigahawk.posscanner.icons.MaterialSymbolsMenu
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.Hashtable
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    navController: NavController,
    hidKeyboardManager: HidKeyboardManager,
    viewModel: CameraPreviewViewModel = viewModel(),
) {
  val context = LocalContext.current
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  var showCameraDropdown by remember { mutableStateOf(false) }
  val connectedDevice by hidKeyboardManager.connectedDevice.collectAsState()

  ModalNavigationDrawer(
      drawerContent = {
        ModalDrawerSheet() {
          Column(
              modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
          ) {
            Spacer(Modifier.height(12.dp))
            Text(text = "PosScanner", style = MaterialTheme.typography.headlineSmall)
            HorizontalDivider()
            Text("Configuration")
            NavigationDrawerItem(
                label = { Text("Scanner settings") },
                selected = false,
                icon = {
                  Icon(imageVector = MaterialSymbolsBarcodeReader, contentDescription = "devices")
                },
                onClick = {
                  scope.launch {
                    drawerState.close()
                    navController.navigate("settings")
                  }
                },
            )
            NavigationDrawerItem(
                label = { Text("Bluetooth devices") },
                selected = false,
                icon = {
                  Icon(imageVector = MaterialSymbolsDevices, contentDescription = "devices")
                },
                onClick = {
                  scope.launch {
                    drawerState.close()
                    navController.navigate("devices")
                  }
                },
            )
          }
        }
      },
      drawerState = drawerState,
  ) {
    Scaffold(
        topBar = {
          TopAppBar(
              title = {
                Column {
                  Text("PosScanner")

                  Text(
                      text = "HID: ${connectedDevice?.name ?: "Disconnected"}",
                      style = MaterialTheme.typography.labelSmall,
                      color =
                          if (connectedDevice != null) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.outline,
                  )
                }
              },
              navigationIcon = {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                  Icon(imageVector = MaterialSymbolsMenu, contentDescription = "Menu")
                }
              },
              actions = {
                val availableCameras by viewModel.availableCameras.collectAsState()
                Box {
                  IconButton(
                      onClick = {
                        scope.launch { viewModel.refreshCameras(context.applicationContext) }
                        showCameraDropdown = true
                      }
                  ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                    )
                  }
                  DropdownMenu(
                      expanded = showCameraDropdown,
                      onDismissRequest = { showCameraDropdown = false },
                  ) {
                    if (availableCameras.isEmpty()) {
                      DropdownMenuItem(
                          text = { Text("No cameras found") },
                          onClick = { showCameraDropdown = false },
                          enabled = false,
                      )
                    } else {
                      availableCameras.forEach { cameraItem ->
                        DropdownMenuItem(
                            text = { Text(cameraItem.label) },
                            onClick = {
                              viewModel.setSelectedCameraItem(cameraItem)
                              showCameraDropdown = false
                            },
                        )
                      }
                    }
                  }
                }
              },
          )
        }
    ) { padding ->
      val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
      Box(modifier = Modifier.padding(padding).fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
          CameraPreviewContent(viewModel = viewModel, hidKeyboardManager = hidKeyboardManager)
        } else {
          Column(modifier = Modifier.padding(16.dp)) {
            val textToShow =
                if (cameraPermissionState.status.shouldShowRationale) {
                  "The camera is important for this app. Please grant the permission."
                } else {
                  "Camera permission required for this feature to be available. Please grant the permission."
                }
            Text(textToShow)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
              Text("Request permission")
            }
          }
        }
      }
    }
  }
}

class CameraPreviewViewModel(application: Application) : SettingsViewModel(application) {
  init {
    viewModelScope.launch {
      mlkitBarcodeFormats.collect { formatsSet ->
        mlKitScanner?.close()

        val formats = MlKitBarcodeFormat.toFormats(formatsSet).toList()
        val options =
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(formats[0], *formats.drop(1).toIntArray())
                .build()
        mlKitScanner = BarcodeScanning.getClient(options)
      }
    }

    viewModelScope.launch {
      zxingBarcodeFormats.collect { formatsSet ->
        zxingReader?.reset()

        val formats = ZxingBarcodeFormat.toFormats(formatsSet).toList()
        val hints = Hashtable<DecodeHintType, Any?>(2)
        hints[DecodeHintType.POSSIBLE_FORMATS] = formats

        zxingReader = MultiFormatReader()
        zxingReader!!.setHints(hints)
      }
    }
  }

  private val _selectedCameraItem = MutableStateFlow<CameraItem?>(null)
  val selectedCameraItem: StateFlow<CameraItem?> = _selectedCameraItem

  private val _availableCameras = MutableStateFlow<List<CameraItem>>(emptyList())
  val availableCameras: StateFlow<List<CameraItem>> = _availableCameras

  private var preview: Preview? = null
  private var cameraProvider: ProcessCameraProvider? = null

  private val analysisExecutor = Executors.newSingleThreadExecutor()
  private val _scanResult = MutableStateFlow<String?>(null)
  val scanResult: StateFlow<String?> = _scanResult

  private var lastScanTime = 0L
  private var lastScanResult: String? = null
  private val COOLDOWN_MS = 2000L

  private var imageAnalysis: ImageAnalysis? = null

  private var imageCapture: ImageCapture? = null

  private var mlKitScanner: BarcodeScanner? = null
  private var zxingReader: MultiFormatReader? = null

  var scanningActive by mutableStateOf(false)
  var showPrompt by mutableStateOf(false)
  private var confirmDeferred: CompletableDeferred<Boolean>? = null

  suspend fun awaitPromptDismiss(): Boolean {
    scanningActive = false
    confirmDeferred = CompletableDeferred()
    showPrompt = true

    val result = confirmDeferred!!.await()

    showPrompt = false
    scanningActive = true
    confirmDeferred = null
    return result
  }

  fun onConfirmPromptResponse(confirmed: Boolean) {
    confirmDeferred?.complete(confirmed)
  }

  fun getImageAnalysis(): ImageAnalysis {
    return imageAnalysis
        ?: ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
              it.setAnalyzer(analysisExecutor) { imageProxy -> processImageProxy(imageProxy) }
              imageAnalysis = it
            }
  }

  fun getImageCapture(): ImageCapture {
    return imageCapture
        ?: ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
            .also { imageCapture = it }
  }

  fun captureAndScan(context: Context) {
    Log.d("SCAN", "Triggering image capture")
    imageCapture?.takePicture(
        context.mainExecutor,
        object : ImageCapture.OnImageCapturedCallback() {
          override fun onCaptureSuccess(image: ImageProxy) {
            Log.d("SCAN", "processing image capture")
            processImageProxy(image, force = true)
          }

          override fun onError(exception: ImageCaptureException) {
            Log.e("CameraPreview", "Image capture failed", exception)
            super.onError(exception)
          }
        },
    )
  }

  @androidx.annotation.OptIn(ExperimentalGetImage::class)
  private fun processImageProxy(imageProxy: ImageProxy, force: Boolean = false) {
    if (!scanningActive && !force) {
      imageProxy.close()
      return
    }
    if (scanBackend.value == ScanBackend.MLKIT) {
      processWithMlKit(imageProxy)
    } else {
      processWithZxing(imageProxy)
    }
  }

  @androidx.annotation.OptIn(ExperimentalGetImage::class)
  private fun processWithMlKit(imageProxy: ImageProxy) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
      imageProxy.close()
      return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    mlKitScanner?.let { scanner ->
      scanner
          .process(image)
          .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let { result ->
              val currentTime = System.currentTimeMillis()
              if (result != lastScanResult || currentTime - lastScanTime > COOLDOWN_MS) {
                Log.d("CameraPreview", "Found barcode with MLKIT: $result")
                _scanResult.value = result
                lastScanResult = result
                lastScanTime = currentTime
              }
            }
          }
          .addOnCompleteListener { imageProxy.close() }
    } ?: imageProxy.close()
  }

  private fun processWithZxing(imageProxy: ImageProxy) {
    val buffer = imageProxy.planes[0].buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    val source =
        PlanarYUVLuminanceSource(
            data,
            imageProxy.width,
            imageProxy.height,
            0,
            0,
            imageProxy.width,
            imageProxy.height,
            false,
        )
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    zxingReader?.let { reader ->
      try {
        val result = reader.decodeWithState(bitmap)
        val currentTime = System.currentTimeMillis()
        if (result.text != lastScanResult || currentTime - lastScanTime > COOLDOWN_MS) {
          Log.d("CameraPreview", "Found barcode with ZXING: ${result.text}")
          _scanResult.value = result.text
          lastScanResult = result.text
          lastScanTime = currentTime
        }
      } catch (e: Exception) {
        if (triggerMode.value == TriggerMode.PRESS) {
          Log.d("CameraPreview", "No barcode found, sending empty string")
        }
      } finally {
        reader.reset()
        imageProxy.close()
      }
    }
  }

  data class CameraItem(val label: String, val selector: CameraSelector)

  fun getPreview(): Preview {
    return preview ?: Preview.Builder().build().also { preview = it }
  }

  fun setSelectedCameraItem(item: CameraItem) {
    if (_selectedCameraItem.value != item) {
      _selectedCameraItem.value = item
    }
  }

  @androidx.annotation.OptIn(ExperimentalLensFacing::class)
  suspend fun refreshCameras(appContext: Context) {
    val provider =
        cameraProvider
            ?: ProcessCameraProvider.awaitInstance(appContext).also { cameraProvider = it }

    // Update available cameras list
    val cameraInfos = provider.availableCameraInfos
    val items =
        cameraInfos.mapIndexed { index, info ->
          val facing =
              when (info.lensFacing) {
                CameraSelector.LENS_FACING_BACK -> "Rear"
                CameraSelector.LENS_FACING_FRONT -> "Front"
                CameraSelector.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
              }
          val camType =
              if (info.intrinsicZoomRatio == 1.0.toFloat()) "Standard"
              else if (info.intrinsicZoomRatio < 1.0) "Wide" else "Zoom"

          val label = "Camera $index ($facing $camType)"
          CameraItem(
              label = label,
              selector =
                  CameraSelector.Builder().addCameraFilter { it.filter { i -> i == info } }.build(),
          )
        }
    _availableCameras.value = items

    // Set initial selection or handle disconnection
    val currentSelection = _selectedCameraItem.value
    if (currentSelection != null && items.none { it.label == currentSelection.label }) {
      _selectedCameraItem.value = items.find { it.label.startsWith("Back") } ?: items.firstOrNull()
    } else if (currentSelection == null && items.isNotEmpty()) {
      val default = items.find { it.label.startsWith("Back") } ?: items.first()
      _selectedCameraItem.value = default
    }
  }

  suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
    try {
      refreshCameras(appContext)
    } catch (e: Exception) {
      Log.e("CameraPreview", "Failed to refresh cameras", e)
    }

    val provider = cameraProvider ?: return

    selectedCameraItem.collectLatest { item ->
      if (item == null) return@collectLatest

      // Debounce rapid switching and give the system time to release previous camera
      delay(500)

      if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
        Log.w("CameraPreview", "Lifecycle not initialized, skipping bind")
        return@collectLatest
      }

      Log.d("CameraPreview", "Binding to camera: ${item.label}")
      try {
        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            item.selector,
            getPreview(),
            getImageAnalysis(),
            getImageCapture(),
        )
        Log.d("CameraPreview", "Binding successful")
      } catch (e: Exception) {
        Log.e("CameraPreview", "Binding failed for ${item.label}", e)
        // If binding fails, it might be because the camera is being used by another app
        // or was just disconnected.
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun CameraPreviewContent(
    hidKeyboardManager: HidKeyboardManager,
    modifier: Modifier = Modifier,
    viewModel: CameraPreviewViewModel = viewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
  val context = LocalContext.current
  val selectedCamera by viewModel.selectedCameraItem.collectAsState()
  val scanResult by viewModel.scanResult.collectAsState()

  val triggerMode by viewModel.triggerMode.collectAsState()

  LaunchedEffect(triggerMode) {
    viewModel.scanningActive =
        when (triggerMode) {
          TriggerMode.CONTINUOUS -> true
          TriggerMode.PROMPT -> true
          TriggerMode.HOLD -> false
          TriggerMode.PRESS -> false
        }
  }

  LaunchedEffect(scanResult) {
    scanResult?.let {
      Log.d("SCAN", "Got scan result: ")
      Log.d("SCAN", it)
      var shouldSend = true
      if (triggerMode == TriggerMode.PROMPT) {
        Log.d("SCAN", "Trigger mode is PROMPT, showing confirmation dialog")
        viewModel.scanningActive = false
        shouldSend = viewModel.awaitPromptDismiss()
        viewModel.scanningActive = true
      }
      if (shouldSend) {
        Log.d("SCAN", "Sending scan result to HID")
        hidKeyboardManager.sendString(it)
      } else {
        Log.d("SCAN", "Scan result not sent to HID")
      }
    }
  }

  LaunchedEffect(lifecycleOwner, viewModel) {
    viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
  }

  DisposableEffect(viewModel) {
    onDispose {
      Log.d("CameraPreview", "Disposing CameraPreviewContent, clearing surface provider")
      viewModel.getPreview().setSurfaceProvider(null)
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    AndroidView(
        factory = { ctx ->
          Log.d("CameraPreview", "Creating PreviewView")
          PreviewView(ctx).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
          }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
          Log.d("CameraPreview", "Updating surface provider")
          viewModel.getPreview().setSurfaceProvider(previewView.surfaceProvider)
        },
    )

    selectedCamera?.let {
      Text(
          text = it.label,
          modifier = Modifier.padding(16.dp).padding(top = 8.dp),
          style =
              MaterialTheme.typography.labelLarge.copy(
                  shadow = Shadow(color = Color.Black, blurRadius = 8f)
              ),
          color = Color.White,
      )
    }

    if (triggerMode.needsTrigger) {
      IconButton(
          onClick =
              if (triggerMode == TriggerMode.PRESS) {
                { viewModel.captureAndScan(context) }
              } else {
                {}
              },
          modifier =
              Modifier.align(Alignment.BottomCenter)
                  .padding(bottom = 32.dp)
                  .size(80.dp)
                  .pointerInput(triggerMode) {
                    if (triggerMode == TriggerMode.HOLD) {
                      awaitPointerEventScope {
                        while (true) {
                          awaitFirstDown(pass = PointerEventPass.Initial)

                          Log.d("CameraPreview", "Shutter clicked")
                          viewModel.scanningActive = true

                          waitForUpOrCancellation(pass = PointerEventPass.Initial)
                          Log.d("CameraPreview", "Shutter released")
                          viewModel.scanningActive = false
                        }
                      }
                    }
                  },
      ) {
        Icon(
            imageVector = Icons.Default.RadioButtonChecked,
            contentDescription = "Shutter",
            modifier = Modifier.fillMaxSize(),
            tint = Color.White,
        )
      }
    }

    if (viewModel.showPrompt) {
      AlertDialog(
          title = { Text("Confirm Scan") },
          text = { Text("Send this value?\n\n${scanResult}") },
          onDismissRequest = {
            Log.d("SPROMPT", "Prompt dismissed")
            viewModel.onConfirmPromptResponse(false)
          },
          confirmButton = {
            TextButton(onClick = { viewModel.onConfirmPromptResponse(true) }) { Text("Yes") }
          },
          dismissButton = {
            TextButton(onClick = { viewModel.onConfirmPromptResponse(false) }) { Text("No") }
          },
      )
    }
  }
}
