package com.gigahawk.posscanner

import android.Manifest
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
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
import com.google.zxing.NotFoundException
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
//import zxingcpp.BarcodeReader as ZXingCppBarcodeReader

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen2(
    navController: NavController,
    hidKeyboardManager: HidKeyboardManager,
    viewModel: CameraViewModel = viewModel(),
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
      BarcodeScannerViewWrapper(viewModel=viewModel)
    }
  }
}

