package com.gigahawk.posscanner

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
  val pagerState = rememberPagerState(pageCount = { 3 })
  val scope = rememberCoroutineScope()

  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween,
  ) {
    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
      when (page) {
        0 -> OnboardingPage("Page 1")
        1 -> OnboardingPage("Page 2")
        2 -> OnboardingPage("Page 3")
      }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      TextButton(onClick = onFinish) { Text("Skip") }

      Button(
          onClick = {
            if (pagerState.currentPage == 2) {
              onFinish()
            } else {
              scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }
          }
      ) {
        Text(if (pagerState.currentPage == 2) "Finish" else "Next")
      }
    }
  }
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
