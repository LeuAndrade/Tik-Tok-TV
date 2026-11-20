# Relatório de Arquitetura Nativa, APIs TikTok, Limitações e Testes — TikTok Native TV

## 1. Conformidade com a Regra Absoluta (100% Nativo)

Uma auditoria direta no código-fonte (`/app/src/main`) confirma **zero ocorrências** de `WebView`, `android.webkit`, `CookieManager`, `evaluateJavascript`, `CustomTabs` ou `TrustedWebActivity`.
Todas as telas são construídas em **Kotlin + Jetpack Compose for TV**, a reprodução usa **Media3 / ExoPlayer + MediaCodec**, e toda comunicação remota ocorre via **HTTPS/JSON (Retrofit, OkHttp, Moshi)** e cache estruturado **Room + DataStore + Android Keystore**.

---

## 2. Estudo e Classificação Oficial das APIs do TikTok

| Funcionalidade | Endpoint / SDK Oficial Analisado | Status neste App | Restrição / Comportamento Real |
|---|---|---|---|
| **Login por QR Code na TV** | Login Kit QR v2 (`/v2/oauth/get_qrcode/`, `/check_qrcode/`, `/token/`, `/revoke/`) | **Implementado e funcional** | Fluxo 100% nativo para TV sem navegador. O usuário escaneia com o TikTok do celular e o `auth-backend` troca e renova os tokens com segurança. |
| **Login Direto pelo SDK Android** | TikTok Open SDK Android (`AuthApi`) | **Bloqueado na TV** | O `AuthApi` móvel depende do app TikTok instalado no celular ou Chrome Custom Tabs. Incompatível com Android TV/Fire TV sem navegador; a aba orienta e redireciona com 1 clique para o Login por QR Code. |
| **Perfil e Métricas Reais** | Display API v2 (`GET /v2/user/info/`) | **Implementado e funcional** | Exibe nome, `@username`, foto via Coil, selo verificado, biografia, seguidores, seguindo, curtidas e total de vídeos, com persistência offline no Room. |
| **Vídeos da Conta Autorizada** | Display API v2 (`POST /v2/video/list/`, `/v2/video/query/`) | **Implementado com transparência** | Lista vídeos reais da conta e consulta por IDs exatos. **Observação técnica da API:** a Display API v2 retorna `embed_link` (HTML) e `share_url`, **não** URLs diretas MP4/HLS. Sem WebView, o app exibe capa, métricas reais e QR Code "Abrir no celular". |
| **Publicação e Pré-visualização de Vídeos** | Content Posting API v2 (`/creator_info/query/`, `/video/init/`, `/inbox/video/init/`, `/status/fetch/`) | **Implementado e funcional** | Seleção via `PickVisualMedia` (zero permissão ampla de armazenamento), pré-visualização nativa no **Media3 ExoPlayer**, upload em chunks `PUT` para rascunho ou postagem direta e confirmação oficial por polling. |
| **Feed Para Você / Seguindo / Explorar** | Sem API pública de terceiros | **Bloqueado pela plataforma** | O TikTok não fornece API autorizada para recomendações personalizadas "Para Você", feed "Seguindo" ou URLs diretas de streaming MP4/HLS desse feed sem WebView. Não simulamos feed falso. |
| **Lives e Streams ao Vivo** | Sem API pública de descoberta ou stream | **Bloqueado pela plataforma** | Não há endpoint autorizado para listar transmissões ao vivo ou entregar streams HLS/FLV brutos em clientes nativos de TV. |
| **Curtir, Republicar e Favoritos (Escrita)** | Sem escopo público de escrita | **Bloqueado pela plataforma** | A Display API v2 retorna contadores reais de leitura, mas proíbe mutações de curtir/favoritar/republicar por terceiros. |
| **Comentários (Lista textual e Escrita)** | Research API (restrita) / Sem escrita pública | **Leitura restrita / Escrita bloqueada** | A Display API v2 exibe o contador real `comment_count`; leitura de textos via Research API exige vínculo acadêmico não comercial (EUA/EEE/RU/CA/CH) e não existe API pública para postar comentários. |
| **Pesquisa Global** | Research API (restrita) / `video/query` | **Parcial (IDs próprios)** | Pesquisa comercial global é bloqueada pela plataforma; o app suporta consulta real por IDs numéricos da conta via `/v2/video/query/` e filtro local no cache Room. |
| **Chat e Mensagens Privadas (DMs)** | Data Portability API (exportação DMA EEE/RU) | **Bloqueado pela plataforma** | Não existe API interativa de DMs ou envio de mensagens; a Data Portability API limita-se a exportação assíncrona no EEE/Reino Unido. |

---

## 3. Compatibilidade de Hardware e Desempenho

| Plataforma / Dispositivo | Suporte no Projeto | Otimizações Nativas Implementadas |
|---|---|---|
| **Android TV / Google TV** | `LEANBACK_LAUNCHER`, D-pad 100% | `FocusRequester`, `focusProperties`, banner `320x180`, ícone adaptativo. |
| **Amazon Fire TV Stick Lite / HD (1 GB RAM)** | `armeabi-v7a`, Perfil **Econômico** | Detecta RAM <= 1536 MB / `isLowRamDevice`, desativa animações/blur, limita buffers ExoPlayer (`1500-5000ms`) e cache `SimpleCache` (24 MB). |
| **Fire TV Stick 4K / 4K Max / Android TV 64-bit** | `arm64-v8a` / `armeabi-v7a` | Detecção de codecs MediaCodec (`H.264`, `H.265/HEVC`, `VP9`, `AV1`) e modos de decodificação (`Automático`, `Hardware padrão`, `Baixa latência`). |
| **Google Play Policy** | Totalmente aderente | Seleção de vídeo usa `ActivityResultContracts.PickVisualMedia` (zero `READ_MEDIA_VIDEO` / `READ_EXTERNAL_STORAGE`). |

---

## 4. Relatório de Testes Executados (Verificados após a edição final)

### A. Suíte Android (`gradle :app:testDebugUnitTest` — Robolectric SDK 36 + Compose UI Test + JUnit4)
**Resultado:** **APROVADO (`BUILD SUCCESSFUL`, 21 testes executados, 0 falhas, 0 erros, 0 ignorados)**

1. **`AuthRepositoryAndQrTest` (3 testes):**
   - Geração de QR Code ZXing para URLs oficiais do TikTok.
   - Fluxo completo de estado Login Kit QR v2 (`WaitingScan` -> `ScannedPendingConfirm` -> `Confirmed`), consulta a `GET /v2/user/info/` e `POST /v2/video/list/`, e limpeza no logout.
   - Rotação automática de `access_token` expirado utilizando `refresh_token`.
2. **`PerformanceAndNativeMediaTest` (6 testes):**
   - Seleção automática de `PerformanceProfile.ECONOMIC` (1 GB RAM), `BALANCED` (2 GB) e `QUALITY` (4 GB+).
   - `MediaSourceValidator`: rejeição estrita de `embed_link`, `/video/` HTML e `http://`; aceitação de `content://`, `file://`, `.mp4` e `.m3u8` para `ExoPlayer`.
   - `TikTokShareUtils.sanitizeShareUrl`: aceita apenas domínios oficiais HTTPS do TikTok e neutraliza esquemas inseguros.
   - `MemoryMonitor`: redação automática de tokens e segredos em logs de diagnóstico.
   - `TikTokPlatformCapabilities` & `DecoderMode`: validação da matriz oficial e dos 3 modos de decodificação.
   - `TikTokTokenBundle.hasScope`: validação de escopos separados por vírgula ou espaço.
3. **`TvRemoteAndUiTest` (10 testes em viewport TV `1280x720dp tvdpi`):**
   - Renderização do menu lateral nativo `TvNavigationDrawerOverlay` com todos os destinos (`Início`, `Para Você`, `Seguindo`, `Explorar`, `Pesquisar`, `Lives`, `Caixa de entrada`, `Favoritos`, `Perfil`, `Publicar vídeo`, `Configurações`).
   - Tela `LoginScreen` com opção principal `Entrar usando QR Code` e painel transparente sobre restrição do SDK móvel na TV.
   - Painel lateral `CommentsSidePanel` exibindo contador real e atalho para QR Code.
   - Painel `ShareVideoOverlay` com QR Code e cópia de link.
   - Telas nativas `InboxScreen` e `SearchScreen` (sem WebView) e disparo de consulta.
   - `SettingsScreen` com seleção de `DecoderMode` (`Baixa latência`), perfil `Equilibrado` e informações de hardware.
   - `FeedDpadFocusHost`: foco inicial, roteamento de teclas D-pad (`Down`, `Up`, `Center`, `Left`, `Right`, `Menu`), transferência de foco para overlays, restauração ao fechar e detecção de `FLAG_LONG_PRESS` no OK sem disparar Play/Pause ao soltar.
4. **`ExampleRobolectricTest` (1 teste) & `ExampleUnitTest` (1 teste):**
   - Validam `app_name = "TikTok Native TV"` e execução JVM.

### B. Suíte Node.js do Backend OAuth (`node --test auth-backend/server.test.js`)
**Resultado:** **APROVADO (1/1)**

### C. Build do APK (`compile_applet` e `gradle :app:assembleDebug`)
**Resultado:** **APROVADO (`BUILD SUCCESSFUL`, APK debug universal gerado em `app/build/outputs/apk/debug/`)**

### D. Testes Não Executados Neste Ambiente
- Testes interativos em aparelho físico (Amazon Fire TV Stick / Google TV real), streaming prolongado com MediaCodec real em GPU de hardware e fluxo end-to-end com credenciais reais de produção do TikTok Developer Portal não puderam ser executados devido à ausência de emulador Android TV / ADB neste ambiente servidor.
