package com.gigahawk.posscanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit, vm: OnboardingViewModel = viewModel()) {
  val pagerState = rememberPagerState(pageCount = { 3 })
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween,
  ) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.weight(1f),
        userScrollEnabled = false,
    ) { page ->
      when (page) {
        0 -> OnboardingPage("Bluetooth Setup", "Bluetooth permissions are required")
        1 ->
            BluetoothPermissionPage(
                onGranted = {
                  scope.launch {
                    vm.bluetoothGranted = true
                    pagerState.animateScrollToPage(2)
                  }
                },
                onDenied = {
                  vm.bluetoothGranted = true
                  Log.e("APP", "Permission denied")
                },
            )
        2 -> OnboardingPage("Ready to go")
      }
    }
    val isNextEnabled =
        when (pagerState.currentPage) {
          1 -> vm.bluetoothGranted
          else -> true
        }

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.End,
    ) {
      Button(
          enabled = isNextEnabled,
          onClick = {
            if (pagerState.currentPage == 2) {
              CoroutineScope(Dispatchers.IO).launch {
                context.dataStore.edit { prefs ->
                  prefs[PreferencesKeys.ONBOARDING_COMPLETE] = true
                }
              }
              onFinish()
            } else {
              scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }
          },
      ) {
        Text(if (pagerState.currentPage == 2) "Finish" else "Next")
      }
    }
  }
}

class OnboardingViewModel : ViewModel() {
  var bluetoothGranted by mutableStateOf(false)
}

@Composable
fun OnboardingPage(title: String, description: String = "") {
  Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(16.dp))
    Text(description)
  }
}

@Composable
fun BluetoothPermissionPage(onGranted: () -> Unit, onDenied: () -> Unit = {}) {
  val context = LocalContext.current
  val permission = Manifest.permission.BLUETOOTH_CONNECT
  val activity: Activity = LocalActivity.current!!

  val launcher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
          isGranted ->
        if (isGranted) {
          Log.d("APP", "Bluetooth permission granted")
          onGranted()
        } else {
          Log.e("APP", "Bluetooth permission denied")
          onDenied()
          Toast.makeText(context, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show()
          val shouldShowRationale =
              ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
          if (!shouldShowRationale) {
            Toast.makeText(
                    context,
                    "Allow permission for 'Nearby devices' to continue",
                    Toast.LENGTH_LONG,
                )
                .show()
            val intent =
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
            context.startActivity(intent)
          }
        }
      }

  Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("We need Bluetooth permission to continue")

    Button(
        onClick = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            launcher.launch(permission)
          } else {
            // No runtime permission required pre-Android 12
            onGranted()
          }
        }
    ) {
      Text("Grant Permission")
    }
  }
}
