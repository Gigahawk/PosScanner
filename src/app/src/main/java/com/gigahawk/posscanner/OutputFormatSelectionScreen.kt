package com.gigahawk.posscanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gigahawk.posscanner.icons.MaterialSymbolsAdd
import com.gigahawk.posscanner.icons.MaterialSymbolsArrowBack
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutputFormatSelectionScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(),
) {
  // val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val outputFormats by viewModel.outputFormats.collectAsState()
  val outputFormatId by viewModel.outputFormatId.collectAsState()

  var localList by remember(outputFormats) { mutableStateOf(outputFormats) }

  val lazyListState = rememberLazyListState()
  val reorderableLazyListState =
      rememberReorderableLazyListState(lazyListState) { from, to ->
        localList = localList.toMutableList().apply { add(to.index, removeAt(from.index)) }
        viewModel.saveOutputFormats(localList)
      }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Output format devices") },
            navigationIcon = {
              IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = MaterialSymbolsArrowBack, contentDescription = "Back")
              }
            },
        )
      },
      floatingActionButton = {
        FloatingActionButton(onClick = { navController.navigate("new_output_format") }) {
          Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = MaterialSymbolsAdd, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("New")
          }
        }
      },
  ) { padding ->
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom =
                    padding.calculateBottomPadding() + 88.dp, // 80dp buffer for FAB + 8dp spacing
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      itemsIndexed(localList, { index, item -> item.id }) { index, item ->
        ReorderableItem(reorderableLazyListState, key = item.id) { isDragging ->
          val interactionSource = remember { MutableInteractionSource() }

          Card(
              modifier = Modifier.fillMaxWidth(),
              onClick = {},
              interactionSource = interactionSource,
          ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              // Text("$index", Modifier.padding(horizontal = 8.dp))
              Box(
                  Modifier.weight(1f).padding(horizontal = 8.dp).clickable {
                    navController.navigate("new_output_format?itemIndex=$index")
                  }
              ) {
                Text(item.text, Modifier.padding(horizontal = 8.dp))
              }
              IconButton(
                  onClick = {
                    if (localList.size == 1) {
                      showToast(context, "At least one output format must be enabled!")
                      return@IconButton
                    }
                    if (item.id == outputFormatId) {
                      if (index == 0) {
                        viewModel.saveOutputFormatId(outputFormats[1].id)
                      } else {
                        viewModel.saveOutputFormatId(outputFormats[0].id)
                      }
                    }
                    localList = localList.toMutableList().apply { removeAt(index) }
                    viewModel.saveOutputFormats(localList)
                  }
              ) {
                // Using standard Delete icon; ensure material-icons-extended is used or check
                // available symbols
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.error,
                )
              }
              RadioButton(
                  selected = item.id == outputFormatId,
                  onClick = { viewModel.saveOutputFormatId(item.id) },
              )
              IconButton(
                  modifier =
                      Modifier.draggableHandle(
                          interactionSource = interactionSource,
                      ),
                  onClick = {},
              ) {
                Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder")
              }
            }
          }
        }
      }
    }
  }
}
