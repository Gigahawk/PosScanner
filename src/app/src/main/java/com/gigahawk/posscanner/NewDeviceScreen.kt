package com.gigahawk.posscanner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gigahawk.posscanner.icons.MaterialSymbolsAdd
import com.gigahawk.posscanner.icons.MaterialSymbolsArrowBack
import com.gigahawk.posscanner.icons.MaterialSymbolsComputer
import com.gigahawk.posscanner.icons.MaterialSymbolsDeskphone
import com.gigahawk.posscanner.icons.MaterialSymbolsDesktopWindows
import com.gigahawk.posscanner.icons.MaterialSymbolsDevicesFold2
import com.gigahawk.posscanner.icons.MaterialSymbolsEyeglasses2
import com.gigahawk.posscanner.icons.MaterialSymbolsHeadphones
import com.gigahawk.posscanner.icons.MaterialSymbolsKeyboard
import com.gigahawk.posscanner.icons.MaterialSymbolsMouse
import com.gigahawk.posscanner.icons.MaterialSymbolsPhoneAndroid
import com.gigahawk.posscanner.icons.MaterialSymbolsQuestionMark
import com.gigahawk.posscanner.icons.MaterialSymbolsSpeaker
import com.gigahawk.posscanner.icons.MaterialSymbolsTouchpadMouse
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NewDeviceScreen(navController: NavController, hidKeyboardManager: HidKeyboardManager) {
  val context = LocalContext.current
  val bluetoothPermissionState =
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        rememberPermissionState(Manifest.permission.BLUETOOTH_CONNECT)
      } else {
        null
      }

  val permissionGranted = bluetoothPermissionState?.status?.isGranted ?: true

  val devices =
      remember(permissionGranted) {
        if (permissionGranted) {
          getPairedDevices(context)
              .sortedWith(
                  compareByDescending<BluetoothDevice> { isControllableDevice(getDeviceType(it)) }
                      .thenBy { it.name ?: "" }
              )
        } else {
          emptyList()
        }
      }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("New connection") },
            navigationIcon = {
              IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = MaterialSymbolsArrowBack, contentDescription = "Menu")
              }
            },
        )
      },
      floatingActionButton = {
        if (permissionGranted) {
          FloatingActionButton(onClick = {}) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(imageVector = MaterialSymbolsAdd, contentDescription = "Add")
              Spacer(modifier = Modifier.width(8.dp))
              Text("New device")
            }
          }
        }
      },
  ) { padding ->
    if (permissionGranted) {
      LazyColumn(contentPadding = padding) {
        items(devices.size) { index ->
          val device = devices[index]
          DeviceListItem(
              device = device,
              onClick = { dev ->
                hidKeyboardManager.connect(dev)

                navController.navigate("main") { popUpTo("new_device") { inclusive = true } }
              },
          )
        }
      }
    } else {
      Column(
          modifier = Modifier.padding(padding).padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        val textToShow =
            if (bluetoothPermissionState!!.status.shouldShowRationale) {
              "Bluetooth connection permission is needed to list paired devices. Please grant it."
            } else {
              "Bluetooth connection permission is required. Please grant it in settings or here."
            }
        Text(textToShow)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { bluetoothPermissionState.launchPermissionRequest() }) {
          Text("Grant Permission")
        }
      }
    }
  }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun DeviceListItem(device: BluetoothDevice, onClick: (BluetoothDevice) -> Unit) {
  val deviceType = getDeviceType(device)
  Row(
      modifier = Modifier.clickable { onClick(device) }.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    DeviceIcon(deviceType)
    Spacer(modifier = Modifier.width(16.dp))
    Column() {
      Text(device.name ?: "Unknown device")
      Text(
          if (isControllableDevice(deviceType)) "Connection should be possible"
          else "Probably not supported",
          style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

enum class DeviceType {
  SMARTPHONE,
  DUMBPHONE,
  HOMEPHONE,
  LAPTOP,
  DESKTOP,
  HEADPHONES,
  SPEAKER,
  KEYBOARD,
  MOUSE,
  KEYBOARD_MOUSE,
  INPUT,
  WATCH,
  GLASSES,
  UNKNOWN,
}

fun isControllableDevice(deviceType: DeviceType): Boolean {
  return when (deviceType) {
    DeviceType.SMARTPHONE -> true
    DeviceType.LAPTOP -> true
    DeviceType.DESKTOP -> true
    else -> false
  }
}

@Composable
fun DeviceIcon(deviceType: DeviceType) {
  val vector =
      when (deviceType) {
        DeviceType.SMARTPHONE -> MaterialSymbolsPhoneAndroid
        DeviceType.DUMBPHONE -> MaterialSymbolsDevicesFold2
        DeviceType.HOMEPHONE -> MaterialSymbolsDeskphone
        DeviceType.LAPTOP -> MaterialSymbolsComputer
        DeviceType.DESKTOP -> MaterialSymbolsDesktopWindows
        DeviceType.HEADPHONES -> MaterialSymbolsHeadphones
        DeviceType.SPEAKER -> MaterialSymbolsSpeaker
        DeviceType.KEYBOARD -> MaterialSymbolsKeyboard
        DeviceType.MOUSE -> MaterialSymbolsMouse
        DeviceType.KEYBOARD_MOUSE -> MaterialSymbolsTouchpadMouse
        DeviceType.INPUT -> MaterialSymbolsKeyboard
        DeviceType.WATCH -> MaterialSymbolsEyeglasses2
        else -> MaterialSymbolsQuestionMark
      }
  Box(
      modifier =
          Modifier.size(48.dp)
              .background(
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                  shape = CircleShape,
              ),
      contentAlignment = Alignment.Center,
  ) {
    Icon(imageVector = vector, contentDescription = vector.name, modifier = Modifier.size(32.dp))
  }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Suppress("CyclomaticComplexMethod")
fun getDeviceType(device: BluetoothDevice): DeviceType {
  val devClass = device.bluetoothClass ?: return DeviceType.UNKNOWN

  return when (devClass.majorDeviceClass) {
    BluetoothClass.Device.Major.COMPUTER -> {
      when (devClass.deviceClass) {
        BluetoothClass.Device.COMPUTER_LAPTOP -> DeviceType.LAPTOP
        BluetoothClass.Device.COMPUTER_DESKTOP -> DeviceType.DESKTOP
        BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA -> DeviceType.SMARTPHONE
        BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA -> DeviceType.SMARTPHONE
        else -> DeviceType.DESKTOP
      }
    }
    BluetoothClass.Device.Major.PHONE -> {
      when (devClass.deviceClass) {
        BluetoothClass.Device.PHONE_SMART -> DeviceType.SMARTPHONE
        BluetoothClass.Device.PHONE_CELLULAR -> DeviceType.DUMBPHONE
        BluetoothClass.Device.PHONE_ISDN -> DeviceType.HOMEPHONE
        BluetoothClass.Device.PHONE_CORDLESS -> DeviceType.HOMEPHONE
        BluetoothClass.Device.PHONE_UNCATEGORIZED -> DeviceType.DUMBPHONE
        BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY -> DeviceType.UNKNOWN
        else -> DeviceType.UNKNOWN
      }
    }

    BluetoothClass.Device.Major.AUDIO_VIDEO -> {
      when (devClass.deviceClass) {
        BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> DeviceType.HEADPHONES
        BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> DeviceType.HEADPHONES
        BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> DeviceType.SPEAKER
        BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE -> DeviceType.SPEAKER
        else -> DeviceType.UNKNOWN
      }
    }

    BluetoothClass.Device.Major.PERIPHERAL -> {
      when (devClass.deviceClass) {
        BluetoothClass.Device.PERIPHERAL_KEYBOARD -> DeviceType.KEYBOARD
        BluetoothClass.Device.PERIPHERAL_POINTING -> DeviceType.MOUSE
        BluetoothClass.Device.PERIPHERAL_KEYBOARD_POINTING -> DeviceType.KEYBOARD_MOUSE
        BluetoothClass.Device.PERIPHERAL_NON_KEYBOARD_NON_POINTING -> DeviceType.INPUT
        else -> DeviceType.INPUT
      }
    }
    BluetoothClass.Device.Major.WEARABLE -> {
      when (devClass.deviceClass) {
        BluetoothClass.Device.WEARABLE_WRIST_WATCH -> DeviceType.WATCH
        BluetoothClass.Device.WEARABLE_GLASSES -> DeviceType.GLASSES
        else -> DeviceType.UNKNOWN
      }
    }
    else -> DeviceType.UNKNOWN
  }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun getPairedDevices(context: Context): List<BluetoothDevice> {
  val adapter = context.getSystemService(BluetoothManager::class.java).adapter ?: return emptyList()
  return adapter.bondedDevices.toList()
}

fun bluetoothDeviceClassName(value: Int): String {
  val fields = BluetoothClass.Device::class.java.fields

  for (field in fields) {
    if (field.type == Int::class.javaPrimitiveType) {
      val fieldValue = field.getInt(null)
      if (fieldValue == value) {
        return field.name
      }
    }
  }
  return "UNKNOWN($value)"
}

fun bluetoothMajorDeviceClassName(value: Int): String {
  val fields = BluetoothClass.Device.Major::class.java.fields

  for (field in fields) {
    if (field.type == Int::class.javaPrimitiveType) {
      val fieldValue = field.getInt(null)
      if (fieldValue == value) {
        return field.name
      }
    }
  }
  return "UNKNOWN($value)"
}
