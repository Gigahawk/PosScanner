package com.gigahawk.posscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gigahawk.posscanner.ui.theme.PosScannerTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
  companion object {
    const val TAG = "MainActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // enableEdgeToEdge()

    setContent { PosScannerTheme { AppRoot() } }
  }
}

@Composable
fun AppRoot() {
  val context = LocalContext.current
  val navController = rememberNavController()
  val hidKeyboardManager = HidKeyboardManager(context)

  val onboardingFlow =
      remember(context) {
        context.dataStore.data.map { it[PreferencesKeys.ONBOARDING_COMPLETE] ?: false }
      }

  val onboardingDoneState by onboardingFlow.collectAsState(initial = null)

  if (onboardingDoneState == null) {
    // Wait for the initial state from DataStore to avoid flickering/wrong start destination
    return
  }

  val onboardingDone = onboardingDoneState!!
  val startDestination = remember { if (onboardingDone) "main" else "onboarding" }

  if (onboardingDone) {
    Log.d("MAIN", "Onboarding already complete, initing HID manager")
    hidKeyboardManager.init()
  }

  NavHost(
      navController = navController,
      startDestination = startDestination,
  ) {
    composable("onboarding") {
      OnboardingScreen(
          onFinish = {
            Log.d("MAIN", "Onboarding finished, initing HID manager")
            hidKeyboardManager.init()
            // TODO: Save state
            navController.navigate("main") { popUpTo("onboarding") { inclusive = true } }
          }
      )
    }

    composable("main") { MainScreen(navController, hidKeyboardManager) }
    composable("devices") { DevicesScreen(navController) }
    composable("new_device") {
      if (
          ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
              PackageManager.PERMISSION_GRANTED
      ) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        navController.navigate("main")
      }
      NewDeviceScreen(navController, hidKeyboardManager)
    }
  }
}
