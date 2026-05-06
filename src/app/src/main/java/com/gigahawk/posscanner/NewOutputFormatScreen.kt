package com.gigahawk.posscanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gigahawk.posscanner.icons.MaterialSymbolsArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOutputFormatScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(),
    itemIndex: Int = -1,
) {
  // val scope = rememberCoroutineScope()
  val outputFormats by viewModel.outputFormats.collectAsState()
  var text by
      remember(outputFormats) {
        val initialText =
            if (itemIndex < 0) {
              DEFAULT_OUTPUT_FORMAT.withNewId()
            } else {
              outputFormats.getOrNull(itemIndex) ?: DEFAULT_OUTPUT_FORMAT.withNewId()
            }
        mutableStateOf(initialText)
      }
  val context = LocalContext.current

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("New Output Format") },
            navigationIcon = {
              IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = MaterialSymbolsArrowBack, contentDescription = "Back")
              }
            },
        )
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = {
              if (text.text.isBlank()) {
                showToast(context, "Output format cannot be empty!")
                return@FloatingActionButton
              }
              if (itemIndex < 0) {
                viewModel.saveOutputFormats(outputFormats + text)
              } else {
                viewModel.saveOutputFormats(
                    outputFormats.toMutableList().apply { set(itemIndex, text) }
                )
              }
              navController.popBackStack()
            }
        ) {
          Icon(imageVector = Icons.Default.Save, contentDescription = "save")
        }
      },
  ) { padding ->
    Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
      OutlinedTextField(
          value = text.text,
          onValueChange = { text = text.copy(text = it) },
          modifier = Modifier.fillMaxSize(),
      )
    }
  }
}
