package com.gigahawk.posscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gigahawk.posscanner.ui.theme.PosScannerTheme
import kotlinx.coroutines.flow.map
import java.util.prefs.Preferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // enableEdgeToEdge()

        setContent {
            PosScannerTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val navController = rememberNavController()

    val onboardingFlow = remember(context) {
        context.dataStore.data
            .map { it[PreferencesKeys.ONBOARDING_COMPLETE] ?: false }
    }

    val onboardingDone by onboardingFlow.collectAsState(initial = false)

    NavHost(
        navController = navController,
        startDestination = if (onboardingDone) "main" else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    // TODO: Save state
                    navController.navigate("main") {
                        popUpTo("onboarding") {inclusive = true}
                    }
                }
            )
        }

        composable("main") {
            MainScreen()
        }
    }
}
