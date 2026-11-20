package com.example.navigation

enum class TvDestination(
  val title: String,
  val group: String,
) {
  HOME("Início", "Descobrir"),
  FOR_YOU("Para Você", "Feed"),
  FOLLOWING("Seguindo", "Feed"),
  EXPLORE("Explorar", "Descobrir"),
  SEARCH("Pesquisar", "Descobrir"),
  LIVES("Lives", "Feed"),
  INBOX("Caixa de entrada", "Social"),
  FAVORITES("Favoritos", "Biblioteca"),
  PROFILE("Perfil", "Conta"),
  PUBLISH("Publicar vídeo", "Criar"),
  SETTINGS("Configurações", "Sistema"),
  LOGIN("Entrar", "Conta"),
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

enum class SearchCategory(val label: String) {
  VIDEO_IDS("Meus vídeos (IDs Display API)"),
  ALL("Pesquisa global (Research API)"),
  VIDEOS("Vídeos"),
  USERS("Usuários"),
  HASHTAGS("Hashtags"),
  SOUNDS("Sons"),
}

enum class LoginMethodTab(val label: String) {
  QR_CODE("Entrar usando QR Code"),
  TIKTOK_DIRECT("Entrar com TikTok"),
}
