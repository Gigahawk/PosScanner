package com.gigahawk.posscanner

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gigahawk.posscanner.icons.MaterialSymbolsAdd
import com.gigahawk.posscanner.icons.MaterialSymbolsArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(navController: NavController) {
  // val scope = rememberCoroutineScope()

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Bluetooth devices") },
            navigationIcon = {
              IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = MaterialSymbolsArrowBack, contentDescription = "Menu")
              }
            },
        )
      },
      floatingActionButton = {
        FloatingActionButton(onClick = { navController.navigate("new_device") }) {
          Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = MaterialSymbolsAdd, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Setup device")
          }
        }
      },
  ) { padding ->
    Text("asdf", modifier = Modifier.padding(padding))
  }
}
