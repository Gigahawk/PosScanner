package com.gigahawk.posscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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

  val onboardingFlow =
      remember(context) {
        context.dataStore.data.map { it[PreferencesKeys.ONBOARDING_COMPLETE] ?: false }
      }

  val onboardingDone by onboardingFlow.collectAsState(initial = false)

  NavHost(
      navController = navController,
      startDestination = if (onboardingDone) "main" else "onboarding",
  ) {
    composable("onboarding") {
      OnboardingScreen(
          onFinish = {
            // TODO: Save state
            navController.navigate("main") { popUpTo("onboarding") { inclusive = true } }
          }
      )
    }

    composable("main") { MainScreen() }
  }
}
