package com.gigahawk.posscanner

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gigahawk.posscanner.icons.MaterialSymbolsBarcodeReader
import com.gigahawk.posscanner.icons.MaterialSymbolsDevices
import com.gigahawk.posscanner.icons.MaterialSymbolsMenu
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    navController: NavController,
    hidKeyboardManager: HidKeyboardManager,
    viewModel: CameraPreviewViewModel = viewModel(),
) {
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  var showCameraDropdown by remember { mutableStateOf(false) }

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
                onClick = {},
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
              title = { Text("PosScanner") },
              navigationIcon = {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                  Icon(imageVector = MaterialSymbolsMenu, contentDescription = "Menu")
                }
              },
              actions = {
                val availableCameras by viewModel.availableCameras.collectAsState()
                Box {
                  IconButton(onClick = { showCameraDropdown = true }) {
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
                          enabled = false
                      )
                    } else {
                      availableCameras.forEach { cameraItem ->
                        DropdownMenuItem(
                            text = { Text(cameraItem.label) },
                            onClick = {
                              viewModel.setCameraSelector(cameraItem.selector)
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
          CameraPreviewContent(viewModel = viewModel)
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

class CameraPreviewViewModel : ViewModel() {
  private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
  val cameraSelector: StateFlow<CameraSelector> = _cameraSelector

  private val _availableCameras = MutableStateFlow<List<CameraItem>>(emptyList())
  val availableCameras: StateFlow<List<CameraItem>> = _availableCameras

  private var preview: Preview? = null

  data class CameraItem(val label: String, val selector: CameraSelector)

  fun getPreview(): Preview {
    return preview ?: Preview.Builder().build().also { preview = it }
  }

  fun setCameraSelector(selector: CameraSelector) {
    if (_cameraSelector.value != selector) {
      _cameraSelector.value = selector
    }
  }

  suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
    val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)
    
    // Update available cameras list
    val cameraInfos = processCameraProvider.availableCameraInfos
    _availableCameras.value = cameraInfos.mapIndexed { index, info ->
        val facing = when (info.lensFacing) {
            CameraSelector.LENS_FACING_BACK -> "Back"
            CameraSelector.LENS_FACING_FRONT -> "Front"
            CameraSelector.LENS_FACING_EXTERNAL -> "External"
            else -> "Unknown"
        }
        CameraItem(
            label = "$facing Camera $index",
            selector = CameraSelector.Builder().addCameraFilter { it.filter { i -> i == info } }.build()
        )
    }

    cameraSelector.collect { selector ->
      Log.d("CameraPreview", "Binding to camera with selector: $selector")
      try {
        processCameraProvider.unbindAll()
        processCameraProvider.bindToLifecycle(lifecycleOwner, selector, getPreview())
        Log.d("CameraPreview", "Binding successful")
      } catch (e: Exception) {
        Log.e("CameraPreview", "Binding failed", e)
      }
    }
  }
}

@Composable
fun CameraPreviewContent(
    modifier: Modifier = Modifier,
    viewModel: CameraPreviewViewModel = viewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
  val context = LocalContext.current

  DisposableEffect(viewModel) {
    onDispose {
      Log.d("CameraPreview", "Disposing CameraPreviewContent, clearing surface provider")
      viewModel.getPreview().setSurfaceProvider(null)
    }
  }

  AndroidView(
      factory = { ctx ->
        Log.d("CameraPreview", "Creating PreviewView")
        PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
      },
      modifier = modifier.fillMaxSize(),
      update = { previewView ->
        Log.d("CameraPreview", "Updating surface provider")
        viewModel.getPreview().setSurfaceProvider(previewView.surfaceProvider)
      },
  )

  LaunchedEffect(lifecycleOwner, viewModel) {
    viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
  }
}
