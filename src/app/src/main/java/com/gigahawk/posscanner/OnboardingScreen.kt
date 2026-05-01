package com.gigahawk.posscanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        0 ->
            OnboardingPage(
                "Welcome",
                "Bluetooth and Camera permissions are required to use this app.",
            )
        1 -> GrantPermissionsPage(vm)
        2 -> OnboardingPage("Ready to go", "You can now start using PosScanner.")
      }
    }

    val isNextEnabled =
        when (pagerState.currentPage) {
          1 -> vm.bluetoothGranted && vm.cameraGranted
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
  var cameraGranted by mutableStateOf(false)
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

/**
 * A reusable Composable that creates a permission launcher. It must be called directly within a
 * Composable function.
 */
@Composable
fun rememberPermissionLauncher(
    permission: String,
    onResult: (Boolean) -> Unit,
): ManagedActivityResultLauncher<String, Boolean> {
  val activity = LocalActivity.current!!
  val context = LocalContext.current
  return rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    onResult(isGranted)
    if (isGranted) {
      Log.d("APP", "Permission $permission granted")
    } else {
      Log.e("APP", "Permission $permission denied")
      val shouldShowRationale =
          ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
      if (!shouldShowRationale) {
        Toast.makeText(
                context,
                "Permission permanently denied. Please enable '$permission' in settings.",
                Toast.LENGTH_LONG,
            )
            .show()
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
              data = Uri.fromParts("package", context.packageName, null)
            }
        context.startActivity(intent)
      } else {
        Toast.makeText(context, "Permission is required to continue", Toast.LENGTH_SHORT).show()
      }
    }
  }
}

@Composable
fun GrantPermissionsPage(vm: OnboardingViewModel) {
  val context = LocalContext.current
  val btPermission = Manifest.permission.BLUETOOTH_CONNECT
  val camPermission = Manifest.permission.CAMERA

  // Check initial state when the page is displayed
  LaunchedEffect(Unit) {
    val btOk =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          ContextCompat.checkSelfPermission(context, btPermission) ==
              PackageManager.PERMISSION_GRANTED
        } else true
    val camOk =
        ContextCompat.checkSelfPermission(context, camPermission) ==
            PackageManager.PERMISSION_GRANTED

    vm.bluetoothGranted = btOk
    vm.cameraGranted = camOk
  }

  val bluetoothLauncher =
      rememberPermissionLauncher(
          permission = btPermission,
          onResult = { isGranted -> vm.bluetoothGranted = isGranted },
      )

  val cameraLauncher =
      rememberPermissionLauncher(
          permission = camPermission,
          onResult = { isGranted -> vm.cameraGranted = isGranted },
      )

  Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Required Permissions", style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.height(24.dp))

    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !vm.bluetoothGranted,
        onClick = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothLauncher.launch(btPermission)
          } else {
            vm.bluetoothGranted = true
          }
        },
    ) {
      Text(
          if (vm.bluetoothGranted) "Bluetooth Permission Granted" else "Grant Bluetooth Permission"
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = !vm.cameraGranted,
        onClick = { cameraLauncher.launch(camPermission) },
    ) {
      Text(if (vm.cameraGranted) "Camera Permission Granted" else "Grant Camera Permission")
    }
  }
}
