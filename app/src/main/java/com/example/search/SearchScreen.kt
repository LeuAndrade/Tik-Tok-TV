package com.example.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.navigation.SearchCategory
import com.example.performance.PerformanceProfile
import com.example.ui.components.TvFocusableButton
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TvBlack
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.web.TikTokWebController
import com.example.web.TikTokWebViewSurface

@Composable
fun SearchScreen(
  initialQuery: String,
  selectedCategory: SearchCategory,
  searchHistory: List<String>,
  webController: TikTokWebController,
  performanceProfile: PerformanceProfile,
  virtualPointerActive: Boolean,
  pointerX: Float,
  pointerY: Float,
  onExecuteSearch: (String, SearchCategory) -> Unit,
  onClearHistory: () -> Unit,
  onOpenMenu: () -> Unit,
  onToggleVirtualPointer: () -> Unit,
) {
  var queryText by remember(initialQuery) { mutableStateOf(initialQuery) }
  var activeCategory by remember(selectedCategory) { mutableStateOf(selectedCategory) }

  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvBlack)
        .padding(20.dp)
        .testTag("search_screen"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      TvFocusableButton(
        text = "Menu",
        icon = Icons.Default.Menu,
        onClick = onOpenMenu,
        testTag = "search_menu_button",
      )

      OutlinedTextField(
        value = queryText,
        onValueChange = { queryText = it },
        label = { Text("Pesquisar vídeos, usuários, hashtags ou sons no TikTok") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
          KeyboardActions(
            onSearch = { onExecuteSearch(queryText, activeCategory) },
          ),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TikTokCyan,
            unfocusedBorderColor = Color(0xFF38384A),
            focusedTextColor = TvTextPrimary,
            unfocusedTextColor = TvTextPrimary,
            focusedLabelColor = TikTokCyan,
            unfocusedLabelColor = TvTextSecondary,
          ),
        modifier = Modifier.weight(1f).testTag("search_input_field"),
      )

      TvFocusableButton(
        text = "Buscar",
        icon = Icons.Default.Search,
        isPrimary = true,
        onClick = { onExecuteSearch(queryText, activeCategory) },
        testTag = "search_submit_button",
      )

      TvFocusableButton(
        text = if (virtualPointerActive) "Ponteiro ON" else "Ponteiro",
        isSelected = virtualPointerActive,
        onClick = onToggleVirtualPointer,
        testTag = "search_pointer_button",
      )
    }

    // Category filter chips
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
      SearchCategory.entries.forEach { category ->
        TvFocusableButton(
          text = category.label,
          isSelected = activeCategory == category,
          onClick = {
            activeCategory = category
            if (queryText.isNotBlank()) {
              onExecuteSearch(queryText, category)
            }
          },
          testTag = "search_cat_${category.name.lowercase()}",
        )
      }
    }

    // Local optional search history
    if (searchHistory.isNotEmpty()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      ) {
        Text(
          text = "Recentes:",
          color = TvTextSecondary,
          style = MaterialTheme.typography.labelLarge,
        )
        searchHistory.forEach { term ->
          TvFocusableButton(
            text = term,
            onClick = {
              queryText = term
              onExecuteSearch(term, activeCategory)
            },
          )
        }
        TvFocusableButton(
          text = "Limpar histórico",
          onClick = onClearHistory,
          testTag = "search_clear_history_button",
        )
      }
    }

    // Real TikTok Search results in WebView
    Box(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(TvSurface)
          .border(1.dp, TikTokCyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
    ) {
      TikTokWebViewSurface(
        controller = webController,
        initialUrl = "https://www.tiktok.com/search",
        performanceProfile = performanceProfile,
        autoplay = true,
        virtualPointerActive = virtualPointerActive,
        pointerNormalizedX = pointerX,
        pointerNormalizedY = pointerY,
        onFullscreenViewChange = {},
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}
