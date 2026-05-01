package com.gigahawk.posscanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gigahawk.posscanner.icons.MaterialSymbolsBarcodeReader
import com.gigahawk.posscanner.icons.MaterialSymbolsDevices
import com.gigahawk.posscanner.icons.MaterialSymbolsMenu
import kotlinx.coroutines.launch

@androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, hidKeyboardManager: HidKeyboardManager) {
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  ModalNavigationDrawer(
      drawerContent = {
        ModalDrawerSheet() {
          Column(
              modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
          ) {
            Spacer(Modifier.height(12.dp))
            Text(text = "PosScanner", style = MaterialTheme.typography.headlineSmall)
            HorizontalDivider()
            Text("Configuration")
            NavigationDrawerItem(
                label = { Text("Scanner settings") },
                selected = false,
                icon = {
                  Icon(imageVector = MaterialSymbolsBarcodeReader, contentDescription = "devices")
                },
                onClick = {},
            )
            NavigationDrawerItem(
                label = { Text("Bluetooth devices") },
                selected = false,
                icon = {
                  Icon(imageVector = MaterialSymbolsDevices, contentDescription = "devices")
                },
                onClick = {
                  scope.launch {
                    drawerState.close()
                    navController.navigate("devices")
                  }
                },
            )
          }
        }
      },
      drawerState = drawerState,
  ) {
    Scaffold(
        topBar = {
          TopAppBar(
              title = { Text("PosScanner") },
              navigationIcon = {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                  Icon(imageVector = MaterialSymbolsMenu, contentDescription = "Menu")
                }
              },
          )
        }
    ) { padding ->
      Column() {
        Text("asdf", modifier = Modifier.padding(padding))
        TextButton(onClick = { hidKeyboardManager.sendKeyA() }) { Text("Send key") }
        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = { hidKeyboardManager.sendKeyRelease() }) { Text("Send key release") }
      }
    }
  }
}
