package com.gigahawk.posscanner

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HidKeyboardManager(private val context: Context) {

  @Suppress("VariableNaming") private val TAG = "HID"
  private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)

  private val adapter = bluetoothManager.adapter

  private var hidDevice: BluetoothHidDevice? = null
  private var targetDevice: BluetoothDevice? = null

  private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
  val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

  private val callback =
      object : BluetoothHidDevice.Callback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
          Log.d(TAG, "State: $state, Device: ${device?.name}")
          if (state == BluetoothProfile.STATE_CONNECTED) {
            _connectedDevice.value = device
            targetDevice = device
          } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
            if (_connectedDevice.value == device) {
              _connectedDevice.value = null
            }
          }
          super.onConnectionStateChanged(device, state)
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
          Log.d(TAG, "Registered: $registered, Device: ${pluggedDevice?.name}")
          super.onAppStatusChanged(pluggedDevice, registered)
        }
      }

  private val sdpSettings by lazy {
    BluetoothHidDeviceAppSdpSettings(
        "PosScanner HID",
        "Mobile Barcode Scanner",
        "Gigahawk",
        BluetoothHidDevice.SUBCLASS1_KEYBOARD,
        DescriptorCollection.KEYBOARD,
    )
  }

  @Suppress("MagicNumber")
  private val qosSettings by lazy {
    BluetoothHidDeviceAppQosSettings(
        BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
        800,
        9,
        0,
        11250,
        BluetoothHidDeviceAppQosSettings.MAX,
    )
  }

  fun init() {
    adapter.getProfileProxy(
        context,
        object : BluetoothProfile.ServiceListener {
          @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
          override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            Log.d(TAG, "Service Connected, profile: $profile")
            val proxyHid = proxy as? BluetoothHidDevice
            hidDevice = proxyHid

            if (proxyHid != null) {
              // Check for already connected devices
              val connected = proxyHid.connectedDevices
              if (connected.isNotEmpty()) {
                Log.d(TAG, "Found already connected device: ${connected[0].name}")
                _connectedDevice.value = connected[0]
                targetDevice = connected[0]
              }

              Log.d(TAG, "Calling registerApp")
              val result =
                  proxyHid.registerApp(
                      sdpSettings,
                      null,
                      qosSettings,
                      ContextCompat.getMainExecutor(context),
                      callback,
                  )
              if (result) {
                Log.d(TAG, "command successful")
              } else {
                Log.e(TAG, "Command failed")
              }
            } else {
              Log.e(TAG, "hidDevice is null")
            }
          }

          override fun onServiceDisconnected(profile: Int) {
            hidDevice = null
          }
        },
        BluetoothProfile.HID_DEVICE,
    )
  }

  @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
  fun connect(device: BluetoothDevice) {
    Log.d(TAG, "Attempting to connect to ${device.name}")
    targetDevice = device
    hidDevice?.connect(device) ?: Log.e(TAG, "HidDevice is null")
  }

  @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
  @Suppress("MagicNumber")
  fun sendKeyA() {
    if (targetDevice == null) {
      Log.e(TAG, "Target device is null")
      return
    }
    if (hidDevice == null) {
      Log.e(TAG, "HID device is null")
      return
    }

    Log.d(TAG, "Sending keypress A to device ${targetDevice!!.name}")
    val report = KeyboardReport()
    report.key1 = 0x04.toByte() // KEYCODE_A
    val result =
        hidDevice!!.sendReport(
            targetDevice,
            KeyboardReport.ID,
            report.bytes,
        )
    if (result) {
      Log.d(TAG, "Send successful")
    } else {
      Log.e(TAG, "send failed")
    }
  }

  @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
  fun sendKeyRelease() {
    if (targetDevice == null) {
      Log.e(TAG, "Target device is null")
      return
    }

    Log.d(TAG, "Sending keypress released to device ${targetDevice!!.name}")
    val report = KeyboardReport()
    hidDevice?.sendReport(
        targetDevice,
        KeyboardReport.ID,
        report.bytes,
    )
  }
}
