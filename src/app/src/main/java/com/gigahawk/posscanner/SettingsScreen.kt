package com.gigahawk.posscanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val triggerModeFlow =
      remember(context) {
        context.dataStore.data.map { preferences ->
          TriggerMode.fromInt(preferences[PreferencesKeys.TRIGGER_MODE] ?: TriggerMode.HOLD.value)
        }
      }
  val triggerMode by triggerModeFlow.collectAsState(initial = TriggerMode.HOLD)

  val scanBackendFlow =
      remember(context) {
        context.dataStore.data.map { preferences ->
          ScanBackend.fromInt(preferences[PreferencesKeys.SCAN_BACKEND] ?: ScanBackend.MLKIT.value)
        }
      }
  val scanBackend by scanBackendFlow.collectAsState(initial = ScanBackend.MLKIT)

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
    Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
      Text(text = "Trigger Mode", style = MaterialTheme.typography.titleMedium)
      Column(Modifier.selectableGroup()) {
        TriggerMode.entries.forEach { mode ->
          Row(
              Modifier.fillMaxWidth()
                  .selectable(
                      selected = (mode == triggerMode),
                      onClick = {
                        scope.launch {
                          context.dataStore.edit { settings ->
                            settings[PreferencesKeys.TRIGGER_MODE] = mode.value
                          }
                        }
                      },
                      role = Role.RadioButton,
                  )
                  .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = (mode == triggerMode), onClick = null)
            Text(
                text = mode.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp),
            )
          }
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

      Text(text = "Scan Backend", style = MaterialTheme.typography.titleMedium)
      Column(Modifier.selectableGroup()) {
        ScanBackend.entries.forEach { backend ->
          Row(
              Modifier.fillMaxWidth()
                  .selectable(
                      selected = (backend == scanBackend),
                      onClick = {
                        scope.launch {
                          context.dataStore.edit { settings ->
                            settings[PreferencesKeys.SCAN_BACKEND] = backend.value
                          }
                        }
                      },
                      role = Role.RadioButton,
                  )
                  .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = (backend == scanBackend), onClick = null)
            Text(
                text = backend.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp),
            )
          }
        }
      }
    }
  }
}
