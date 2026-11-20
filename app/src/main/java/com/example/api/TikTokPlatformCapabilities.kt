package com.example.api

enum class ApiSupportLevel(val label: String) {
  NATIVE_SUPPORTED("Disponível via API oficial"),
  RESTRICTED_ACADEMIC_OR_REGION("Restrito (Acadêmico / Regional)"),
  PLATFORM_BLOCKED("Não disponibilizado pela API oficial"),
}

data class TikTokCapabilityRecord(
  val featureName: String,
  val apiNameOrScope: String,
  val supportLevel: ApiSupportLevel,
  val technicalSummary: String,
)

object TikTokPlatformCapabilities {

  val matrix: List<TikTokCapabilityRecord> =
    listOf(
      TikTokCapabilityRecord(
        featureName = "Autenticação por QR Code na TV",
        apiNameOrScope = "Login Kit QR v2 (/v2/oauth/get_qrcode/, /check_qrcode/, /token/, /revoke/)",
        supportLevel = ApiSupportLevel.NATIVE_SUPPORTED,
        technicalSummary =
          "Fluxo oficial sem navegador na TV: o código QR é gerado nativamente, confirmado no app TikTok do celular e renovado automaticamente via refresh_token.",
      ),
      TikTokCapabilityRecord(
        featureName = "Login direto pelo SDK Android na TV",
        apiNameOrScope = "TikTok Open SDK Android (AuthApi)",
        supportLevel = ApiSupportLevel.PLATFORM_BLOCKED,
        technicalSummary =
          "O AuthApi móvel oficial depende do aplicativo móvel do TikTok ou Chrome Custom Tabs, indisponíveis/incompatíveis em Android TV/Fire TV sem navegador. Por isso o app prioriza Login por QR Code v2.",
      ),
      TikTokCapabilityRecord(
        featureName = "Perfil do usuário conectado",
        apiNameOrScope = "Display API v2 (GET /v2/user/info/ — user.info.basic, profile, stats)",
        supportLevel = ApiSupportLevel.NATIVE_SUPPORTED,
        technicalSummary =
          "Retorna nome, @usuário, foto, selo verificado, biografia, seguidores, seguindo, curtidas e total de vídeos diretamente em JSON.",
      ),
      TikTokCapabilityRecord(
        featureName = "Vídeos publicados da conta conectada",
        apiNameOrScope = "Display API v2 (POST /v2/video/list/ e /v2/video/query/ — video.list)",
        supportLevel = ApiSupportLevel.NATIVE_SUPPORTED,
        technicalSummary =
          "Lista vídeos públicos do usuário autorizado com estatísticas reais e capa. Observação da API: retorna embed_link/share_url em vez de MP4/HLS direto; vídeos locais/streams compatíveis rodam no ExoPlayer e embed-only exibe QR para celular.",
      ),
      TikTokCapabilityRecord(
        featureName = "Publicação / Envio de vídeos da TV",
        apiNameOrScope = "Content Posting API v2 (/creator_info/query/, /video/init/, /inbox/video/init/)",
        supportLevel = ApiSupportLevel.NATIVE_SUPPORTED,
        technicalSummary =
          "Permite selecionar vídeo local com Media3 preview e enviar via FILE_UPLOAD para rascunho (video.upload) ou publicação direta (video.publish).",
      ),
      TikTokCapabilityRecord(
        featureName = "Feed Para Você / Seguindo / Explorar",
        apiNameOrScope = "Sem endpoint público oficial de terceiros",
        supportLevel = ApiSupportLevel.PLATFORM_BLOCKED,
        technicalSummary =
          "O TikTok não expõe API nativa autorizada para o algoritmo personalizado Para Você, feed Seguindo ou URLs diretas de streaming MP4/HLS desse feed sem WebView.",
      ),
      TikTokCapabilityRecord(
        featureName = "Lives e streams ao vivo",
        apiNameOrScope = "Sem API pública de descoberta ou stream bruto",
        supportLevel = ApiSupportLevel.PLATFORM_BLOCKED,
        technicalSummary =
          "O TikTok não fornece API autorizada para listar transmissões ao vivo em clientes nativos de TV ou fornecer URLs diretas HLS/FLV.",
      ),
      TikTokCapabilityRecord(
        featureName = "Curtir, Republicar e Favoritos (Escrita)",
        apiNameOrScope = "Sem escopo OAuth público de escrita",
        supportLevel = ApiSupportLevel.PLATFORM_BLOCKED,
        technicalSummary =
          "A Display API v2 fornece contadores reais de leitura, mas não autoriza chamadas de curtida, republicação ou salvamento em favoritos.",
      ),
      TikTokCapabilityRecord(
        featureName = "Comentários (leitura detalhada e publicação)",
        apiNameOrScope = "Research API (leitura acadêmica) / Sem escrita pública",
        supportLevel = ApiSupportLevel.RESTRICTED_ACADEMIC_OR_REGION,
        technicalSummary =
          "A Display API v2 retorna comment_count. Consulta de comentários pela Research API exige vínculo acadêmico não comercial (EUA/EEE/RU/CA/CH); não existe API pública para postar ou responder comentários.",
      ),
      TikTokCapabilityRecord(
        featureName = "Pesquisa global (vídeos, usuários, hashtags, sons)",
        apiNameOrScope = "Research API (restrita) / video/query (IDs próprios)",
        supportLevel = ApiSupportLevel.RESTRICTED_ACADEMIC_OR_REGION,
        technicalSummary =
          "Pesquisa global comercial não é liberada na Display API v2; suporta consulta por IDs exatos dos vídeos da conta conectada via POST /v2/video/query/.",
      ),
      TikTokCapabilityRecord(
        featureName = "Chat e Mensagens Privadas (DMs)",
        apiNameOrScope = "Data Portability API (exportação DMA EEE/RU apenas)",
        supportLevel = ApiSupportLevel.PLATFORM_BLOCKED,
        technicalSummary =
          "Não existe API oficial de chat interativo ou envio de mensagens privadas; a Data Portability API limita-se a exportações de histórico no EEE/Reino Unido.",
      ),
    )
}
