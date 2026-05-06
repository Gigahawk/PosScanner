package com.gigahawk.posscanner

import android.app.Application
import android.content.Context
import android.hardware.Camera
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.adapter.CameraInfoAdapter.Companion.cameraId
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalLensFacing
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CameraViewModel(application: Application): SettingsViewModel(application) {

  private val _availableCameras = MutableStateFlow<List<CameraItem>>(emptyList())
  val availableCameras: StateFlow<List<CameraItem>> = _availableCameras

  private val _selectedCameraItem = MutableStateFlow<CameraItem?>(null)
  val selectedCameraItem: StateFlow<CameraItem?> = _selectedCameraItem

  private var cameraProvider: ProcessCameraProvider? = null

@OptIn(ExperimentalLensFacing::class, ExperimentalCamera2Interop::class)
suspend fun refreshCameras(appContext: Context) {

    // Legacy Camera API does not support all camera infos
    //val provider = cameraProvider
    //  ?: ProcessCameraProvider.awaitInstance(appContext).also {
    //    cameraProvider = it
    //  }
    //val cameraInfos = provider.availableCameraInfos

    val numCameraInfos = Camera.getNumberOfCameras()
    val cameraInfos = Array<Camera.CameraInfo>(numCameraInfos) { i ->
      val info = Camera.CameraInfo()
      Camera.getCameraInfo(i, info)
      info
    }
    val items =
      cameraInfos.mapIndexed { index, info ->
        //val facing = when (info.lensFacing) {
        val facing = when (info.facing) {
          CameraSelector.LENS_FACING_BACK -> "Rear"
          CameraSelector.LENS_FACING_FRONT -> "Front"
          CameraSelector.LENS_FACING_EXTERNAL -> "External"
          else -> "Unknown"
        }
        //val camType =
        //  if (info.intrinsicZoomRatio == 1.0.toFloat()) "Standard"
        //  else if (info.intrinsicZoomRatio < 1.0) "Wide" else "Zoom"
        val camType = "Standard"

        val label = "Camera $index ($facing $camType)"
        val selector =
          CameraSelector.Builder().addCameraFilter { it.filter { i -> i == info } }.build()
        CameraItem(
          label = label,
          selector =selector,
          id = index
        )
      }
    _availableCameras.value = items

    val currentSelection = _selectedCameraItem.value
    if (currentSelection != null && items.none { it.label == currentSelection.label }) {
      // If the current selection is not in the list of available cameras,
      // select a sensible default
      _selectedCameraItem.value = items.find {
        it.label.contains("Rear")
      } ?: items.firstOrNull()
    } else if (currentSelection == null && items.isNotEmpty()) {
      // If there is no current selection, select a sensible default
      _selectedCameraItem.value = items.find {
        it.label.contains("Rear")
      } ?: items.firstOrNull()
    }
  }

  fun setSelectedCameraItem(item: CameraItem) {
    if (_selectedCameraItem.value != item) {
      _selectedCameraItem.value = item
    }
  }
}

data class CameraItem(
  val label: String,
  val selector: CameraSelector,
  val id: Int
)
