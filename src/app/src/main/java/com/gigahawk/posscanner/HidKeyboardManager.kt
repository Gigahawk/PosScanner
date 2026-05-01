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

class HidKeyboardManager(private val context: Context) {

  @Suppress("VariableNaming") private val TAG = "HID"
  private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)

  private val adapter = bluetoothManager.adapter

  private var hidDevice: BluetoothHidDevice? = null
  private var targetDevice: BluetoothDevice? = null

  private val callback =
      object : BluetoothHidDevice.Callback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
          Log.d(TAG, "State: $state, Device: ${device?.name}")
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
        BluetoothHidDevice.SUBCLASS1_COMBO,
        DescriptorCollection.KEYBOARD_MODIFIED,
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
            hidDevice = proxy as BluetoothHidDevice

            if (hidDevice != null) {
              Log.d(TAG, "Calling registerApp")
              val result =
                  hidDevice!!.registerApp(
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
    val result =
        hidDevice!!.sendReport(
            targetDevice,
            KeyboardReport.ID,
            byteArrayOf(0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00),
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
    hidDevice?.sendReport(
        targetDevice,
        KeyboardReport.ID,
        byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
    )
  }
}
