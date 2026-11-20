package com.example.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.auth.NativeAuthState
import com.example.auth.TikTokDisplayVideo
import com.example.navigation.SearchCategory
import com.example.ui.components.TvFocusableButton
import com.example.ui.components.TvFocusableTile
import com.example.ui.components.TvInfoBanner
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvBlack
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

@Composable
fun SearchScreen(
  initialQuery: String,
  selectedCategory: SearchCategory,
  searchHistory: List<String>,
  authState: NativeAuthState,
  results: List<TikTokDisplayVideo>,
  isSearching: Boolean,
  searchError: String?,
  onExecuteSearch: (String, SearchCategory) -> Unit,
  onSelectVideo: (TikTokDisplayVideo) -> Unit,
  onClearHistory: () -> Unit,
  onOpenMenu: () -> Unit,
  onOpenLogin: () -> Unit,
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
        label = { Text("Consultar ID(s) de vídeo (ex.: 7080217258545735586) ou filtro local") },
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
        text = "Consultar API",
        icon = Icons.Default.Search,
        isPrimary = true,
        onClick = { onExecuteSearch(queryText, activeCategory) },
        testTag = "search_submit_button",
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

    if (searchHistory.isNotEmpty()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      ) {
        Text(text = "Recentes:", color = TvTextSecondary, style = MaterialTheme.typography.labelLarge)
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

    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(TvSurface)
          .border(1.dp, TikTokCyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
          .padding(18.dp),
    ) {
      TvInfoBanner(
        message =
          if (activeCategory == SearchCategory.VIDEO_IDS) {
            "Display API v2 (/v2/video/query/): consulta vídeos específicos pertencentes à conta autorizada informando IDs numéricos separados por vírgula ou filtrando seus vídeos já sincronizados."
          } else {
            "Pesquisa global comercial (${activeCategory.label}) restrita: a API de busca global do TikTok (Research API) é limitada a pesquisadores acadêmicos não comerciais nos EUA/EEE/RU/CA/CH."
          },
        accentColor = if (activeCategory == SearchCategory.VIDEO_IDS) TikTokCyan else TikTokPink,
      )

      if (!authState.oauthConnected) {
        TvFocusableButton(
          text = "Conectar conta via QR Code para consultar Display API",
          isPrimary = true,
          onClick = onOpenLogin,
          testTag = "search_login_button",
        )
      }

      if (isSearching) {
        CircularProgressIndicator(color = TikTokCyan)
      } else if (!searchError.isNullOrBlank()) {
        TvInfoBanner(message = searchError, accentColor = TikTokPink)
      } else if (results.isEmpty()) {
        Text(
          text =
            "Nenhum resultado exibido. Digite IDs de vídeos da sua conta (ex.: 7080217258545735586) ou um termo presente no título dos seus vídeos cacheados.",
          color = TvTextSecondary,
          style = MaterialTheme.typography.bodyMedium,
        )
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          items(results, key = { it.id }) { video ->
            TvFocusableTile(
              title = video.title.ifBlank { video.videoDescription.ifBlank { "Vídeo #${video.id}" } },
              subtitle = "ID ${video.id} • ${video.viewCount} views • ${video.likeCount} curtidas",
              icon = Icons.Default.PlayArrow,
              trailingBadge = "${video.duration}s",
              onClick = { onSelectVideo(video) },
            )
          }
        }
      }
    }
  }
}
