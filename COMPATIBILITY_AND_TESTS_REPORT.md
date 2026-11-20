# Relatório de Compatibilidade, Limitações e Testes — TikTok TV

## 1. Compatibilidade de Plataformas e Arquiteturas

| Plataforma / Dispositivo | Suporte no Projeto | Observações Técnicas |
|---|---|---|
| **Android TV / Google TV** | Compatível (`LEANBACK_LAUNCHER`) | Interface Compose 16:9 com foco D-pad, banner `320x180`, `touchscreen required="false"`. |
| **Amazon Fire TV Stick Lite / HD (1 GB RAM)** | Compatível (`armeabi-v7a`, perfil **Econômico**) | Detecta automaticamente RAM <= 1536 MB / `isLowRamDevice`, desativa animações pesadas, usa WebView única e limita cache. |
| **Amazon Fire TV Stick 4K / 4K Max / Fire TV Cube** | Compatível (`armeabi-v7a` / `arm64-v8a`) | Suporta reprodução Full HD/4K via aceleração de hardware e MediaCodec. |
| **Dependência de Google Play Services** | **Zero** | Autenticação usa Login Kit v2 (`auth-backend` + TikTok Web) e ZXing/WebView nativas, sem exigir GMS. |
| **Telas sensíveis ao toque** | **Não exigido** | Todos os fluxos operam por D-pad, teclado virtual da TV e Ponteiro Virtual D-pad como fallback. |

---

## 2. Matriz Transparente de Funcionalidades (API Oficial vs Sessão Web Oficial)

| Funcionalidade | Mecanismo Utilizado | Limitações Conhecidas da Plataforma TikTok |
|---|---|---|
| **Login Direto ("Entrar com TikTok")** | TikTok Web oficial (`https://www.tiktok.com/login`) em WebView protegida | Suporta e-mail, telefone, usuário, provedores externos, captcha e 2FA sem capturar senhas. |
| **Login por QR Code (Web)** | TikTok Web QR (`https://www.tiktok.com/login/qrcode`) | Concede sessão web real completa (cookies persistidos via `CookieManager`). |
| **Login por QR Code (Login Kit v2 API)** | `auth-backend` -> `POST /v2/oauth/get_qrcode/`, `POST /v2/oauth/check_qrcode/`, `POST /v2/oauth/token/` | Requer hospedagem do `auth-backend` com `TIKTOK_CLIENT_KEY` e `TIKTOK_CLIENT_SECRET`. Concede tokens OAuth para Display API v2 (não gera cookies web automaticamente). |
| **Feed Para Você, Seguindo e Lives** | Sessão web oficial do TikTok (`tiktok.com/foryou`, `/following`, `/live`) | A Display API v2 oficial não fornece endpoint público de feed personalizado "Para Você" ou descoberta de Lives; por isso o app utiliza a experiência web oficial autenticada. |
| **Curtir, Republicar, Salvar, Seguir, Comentar** | `TikTokWebBridge` acionando os controles reais do DOM autenticado + confirmação real | Requer sessão web autenticada no TikTok. Caso o TikTok altere seletores DOM ou exija verificação de segurança/login, o app informa o usuário sem simular sucesso e oferece o Ponteiro Virtual D-pad. |
| **Pesquisa (Vídeos, Usuários, Hashtags, Sons, Lives)** | TikTok Web Search + histórico local opcional em DataStore | Os resultados são exclusivamente os retornados pelo TikTok real; o histórico local pode ser limpo a qualquer momento. |
| **Caixa de Entrada (Mensagens & Notificações)** | TikTok Web Messages / Notifications (`tiktok.com/messages`, `/notifications`) | Respeita regras de privacidade, idade, disponibilidade regional e login web do TikTok. APIs de portabilidade não permitem envio de DMs. |
| **Perfil & Vídeos do Usuário** | Display API v2 (`GET /v2/user/info/`, `POST /v2/video/list/`) + Perfil Web por abas | Display API v2 exibe apenas vídeos públicos da conta autorizada pelos escopos aprovados. |
| **Compartilhar / Enviar ao Celular** | ZXing QR Code local + `ClipboardManager` + `Intent.ACTION_SEND` | Gera QR Code com a URL legítima `https://www.tiktok.com/...` sem servidores intermediários. |

---

## 3. Relatório de Testes Executados

### Testes Automatizados Executados Neste Ambiente

1. **Suíte Android (Robolectric SDK 36 + Compose UI Test + JUnit4) — `gradle :app:testDebugUnitTest`**
   - **Resultado:** **APROVADO (`BUILD SUCCESSFUL`, 18 testes executados, 0 falhas)**
   - `AuthRepositoryAndQrTest`:
     - Geração de bitmap QR Code válido para URL TikTok via ZXing.
     - Máquina de estados QR Code v2 (`WaitingScan` -> `ScannedPendingConfirm` -> `Confirmed`), busca de perfil (`Display API v2`), listagem de vídeos e exclusão total no logout.
     - Rotação automática de `access_token` expirado via `refresh_token`.
   - `PerformanceAndWebSecurityTest`:
     - Seleção automática do perfil `ECONOMIC` para Fire TV Stick de 1 GB RAM (`isLowRamDevice = true`), `BALANCED` para 2 GB e `QUALITY` para 4 GB+.
     - Política HTTPS estrita na WebView (aceita domínios oficiais TikTok e provedores OAuth confiáveis; bloqueia `http://`, `javascript:` e domínios de phishing).
     - Detecção segura de cookies de sessão web e redação de tokens/segredos nos logs de diagnóstico.
     - Mapeamento contextual do controle remoto TV (`DPAD_UP`/`DOWN`, `OK`, `Segurar OK`, `BACK`, isolamento dentro de Comentários e movimento do Ponteiro Virtual).
     - Amostragem de memória RAM/Heap pelo `MemoryMonitor`.
   - `TvRemoteAndUiTest` (executado em viewport Android TV `1280x720dp tvdpi`):
     - Navegação D-pad por foco em Compose (`FeedDpadFocusHost` com `FocusRequester`, `Modifier.onKeyEvent` e `Modifier.onPreviewKeyEvent`): foco inicial no feed, troca de vídeo (`Up`/`Down`), Play/Pause (`Center`), abertura de gaveta/ações/menu contextual (`Left`/`Right`/`Menu`), detecção de `FLAG_LONG_PRESS` no OK sem disparar pausa ao soltar, transferência automática de foco para overlays e restauração do foco para o feed ao fechar.
     - Modo Ponteiro Virtual interceptando direcionais via `onPreviewKeyEvent` sem trocar vídeo do feed.
     - Renderização completa do menu lateral `TvNavigationDrawerOverlay` e navegação para `Configurações`.
     - Renderização do painel lateral de comentários (`CommentsSidePanel`) com comentários reais e campo de digitação.
     - Renderização do painel de compartilhamento (`ShareVideoOverlay`) com QR Code "Enviar ao celular" e cópia de link.
     - Renderização da tela de `Configurações` com perfis Fire TV e seleção de perfil de desempenho.
   - `ExampleRobolectricTest` & `ExampleUnitTest`: verificam recurso `app_name = "TikTok TV"` e execução JVM.

2. **Suíte Node.js do Backend (`node --test auth-backend/server.test.js`)**
   - **Resultado:** **APROVADO (1/1)**
   - Verifica exposição dos endpoints `/v2/oauth/get_qrcode/`, `/v2/oauth/check_qrcode/`, `/v2/oauth/token/`, `/v2/oauth/revoke/`, substituição de `client_ticket=tobefilled` e ausência de `client_secret` hardcoded.

### Testes Que Não Puderam Ser Executados Neste Ambiente (Registrados Explicitamente)

Devido à ausência de emulador Android TV / ADB / dispositivo físico Fire TV Stick neste ambiente de compilação em servidor:
- **Não executados automaticamente:** testes manuais interativos em streaming de vídeo real no `tiktok.com` autenticado com conta real do usuário (reprodução prolongada 4K, captura de FPS em GPU real, leitura de comentários reais ao vivo, envio real de curtida/comentário/republicação/DM, autenticação 2FA presencial em Fire TV Stick Lite/4K).
- **Observação sobre APK Release assinado:** a tarefa `assembleDebug` e a compilação completa foram validadas; para `assembleRelease` em produção, configure as variáveis de ambiente `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD` conforme `app/build.gradle.kts`.
