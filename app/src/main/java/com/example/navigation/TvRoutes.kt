package com.example.navigation

enum class TvDestination(
  val title: String,
  val group: String,
  val webUrl: String,
) {
  FOR_YOU("Para Você", "Início", "https://www.tiktok.com/foryou"),
  FOLLOWING("Seguindo", "Início", "https://www.tiktok.com/following"),
  LIVES("Lives", "Início", "https://www.tiktok.com/live"),
  SEARCH("Pesquisar", "Descobrir", "https://www.tiktok.com/search"),
  INBOX("Caixa de entrada", "Comunicação", "https://www.tiktok.com/messages"),
  FAVORITES("Favoritos", "Biblioteca", "https://www.tiktok.com/profile"),
  PROFILE("Perfil", "Conta", "https://www.tiktok.com/profile"),
  SETTINGS("Configurações", "Sistema", "https://www.tiktok.com"),
  LOGIN("Entrar", "Conta", "https://www.tiktok.com/login"),
}

enum class ActiveOverlay {
  NONE,
  NAVIGATION_MENU,
  ACTION_BAR,
  COMMENTS_PANEL,
  SHARE_PANEL,
  CONTEXT_MENU,
  EXIT_CONFIRM,
}

enum class SearchCategory(val label: String, val pathSegment: String) {
  ALL("Todos", "search"),
  VIDEOS("Vídeos", "search/video"),
  USERS("Usuários", "search/user"),
  HASHTAGS("Hashtags", "tag"),
  SOUNDS("Sons", "search"),
  LIVES("Lives", "search/live"),
}

enum class LoginMethodTab(val label: String) {
  TIKTOK_DIRECT("Entrar com TikTok"),
  QR_WEB("QR Code da TV (Web)"),
  QR_OAUTH_API("QR Code Login Kit (API)"),
}
