package com.gigahawk.posscanner

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.alorma.compose.settings.ui.SettingsSlider
import com.alorma.compose.settings.ui.expressive.SettingsCheckbox
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink
import com.alorma.compose.settings.ui.expressive.SettingsRadioButton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel = viewModel()) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val triggerMode by viewModel.triggerMode.collectAsState()
  val scanSuffix by viewModel.scanSuffix.collectAsState()
  val reportDelayMs by viewModel.reportDelayMs.collectAsState()

  val scanBackend by viewModel.scanBackend.collectAsState()
  val mlkitBarcodeFormats by viewModel.mlkitBarcodeFormats.collectAsState()
  val zxingBarcodeFormat by viewModel.zxingBarcodeFormats.collectAsState()

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Scanner Settings") },
            navigationIcon = {
              IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            },
        )
      }
  ) { padding ->
    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(padding)) {
      Text(
          "Scanner Behavior",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(16.dp),
      )

      SettingsGroup(title = { Text("Trigger Mode:") }) {
        TriggerMode.entries.forEach { mode ->
          SettingsRadioButton(
              title = { Text(mode.displayName) },
              subtitle = { Text(mode.description) },
              state = mode.value == triggerMode.value,
              onClick = {
                scope.launch {
                  context.dataStore.edit { it[PreferencesKeys.TRIGGER_MODE] = mode.value }
                }
              },
          )
        }
      }

      SettingsGroup(title = { Text("Suffix:") }) {
        ScanSuffix.entries.forEach { mode ->
          SettingsRadioButton(
              title = { Text(mode.displayName) },
              subtitle = { Text(mode.description) },
              state = mode.value == scanSuffix.value,
              onClick = {
                scope.launch {
                  context.dataStore.edit { it[PreferencesKeys.SCAN_SUFFIX] = mode.value }
                }
              },
          )
        }
        if (scanSuffix == ScanSuffix.CUSTOM) {
          SettingsMenuLink(
              title = { Text("Set Custom Suffix") },
              onClick = {
                // navController.navigate("suffix")
              },
          )
        }
      }

      SettingsSlider(
          { Text("Keypress Delay") },
          subtitle = { Text("Delay between simulated keypress events: $reportDelayMs ms") },
          value = reportDelayMs.toFloat(),
          valueRange = 0f..250f,
          steps = 251,
          onValueChange = { newValue: Float ->
            scope.launch {
              context.dataStore.edit { it[PreferencesKeys.REPORT_DELAY_MS] = newValue.toInt() }
            }
          },
      )

      SettingsMenuLink(
          title = { Text("Output Format") },
          subtitle = { Text("Set the format of the output string") },
          onClick = {
            // navController.navigate("output_format")
          },
      )

      Text(
          "Decoder Settings",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(16.dp),
      )

      SettingsGroup(title = { Text("Decoder Library:") }) {
        ScanBackend.entries.forEach { backend ->
          SettingsRadioButton(
              title = { Text(backend.displayName) },
              state = backend.value == scanBackend.value,
              onClick = {
                scope.launch {
                  context.dataStore.edit { it[PreferencesKeys.SCAN_BACKEND] = backend.value }
                }
              },
          )
        }
      }

      SettingsGroup(title = { Text("Enabled Barcode Formats:") }) {
        when (scanBackend) {
          ScanBackend.MLKIT ->
              MlKitBarcodeFormat.entries
                  .sortedBy { it.displayName }
                  .forEach { format ->
                    SettingsCheckbox(
                        title = { Text(format.displayName) },
                        state = mlkitBarcodeFormats.any { it.value == format.value },
                        onCheckedChange = { newState: Boolean ->
                          scope.launch {
                            context.dataStore.edit { preferences ->
                              val newFormats = mlkitBarcodeFormats.toMutableSet()
                              if (newState) {
                                newFormats.add(format)
                              } else {
                                newFormats.remove(format)
                              }
                              if (newFormats.isEmpty()) {
                                Toast.makeText(
                                        context,
                                        "At least one format must be enabled!",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                              } else {
                                preferences[PreferencesKeys.MLKIT_BARCODE_FORMATS] =
                                    newFormats.map { it.displayName }.toSet()
                              }
                            }
                          }
                        },
                    )
                  }
          ScanBackend.ZXING ->
              ZxingBarcodeFormat.entries
                  .sortedBy { it.displayName }
                  .forEach { format ->
                    SettingsCheckbox(
                        title = { Text(format.displayName) },
                        state = zxingBarcodeFormat.any { it.zxingValue == format.zxingValue },
                        onCheckedChange = { newState: Boolean ->
                          scope.launch {
                            context.dataStore.edit { preferences ->
                              val newFormats = zxingBarcodeFormat.toMutableSet()
                              if (newState) {
                                newFormats.add(format)
                              } else {
                                newFormats.remove(format)
                              }
                              if (newFormats.isEmpty()) {
                                Toast.makeText(
                                        context,
                                        "At least one format must be enabled!",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                              } else {
                                preferences[PreferencesKeys.ZXING_BARCODE_FORMATS] =
                                    newFormats.map { it.displayName }.toSet()
                              }
                            }
                          }
                        },
                    )
                  }
        }
      }
    }
  }
}
