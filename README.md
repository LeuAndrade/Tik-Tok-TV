# TikTok Native TV — Cliente 100% Nativo para Android TV, Google TV e Amazon Fire TV Stick

Aplicativo cliente nativo de televisão desenvolvido exclusivamente em **Kotlin + Jetpack Compose for TV + Media3/ExoPlayer + Room + Retrofit/OkHttp/Moshi**, projetado para telas 16:9 (Full HD / 4K), navegação 100% espacial por **controle remoto (D-pad)** e dispositivos com **1 GB de RAM** (como Amazon Fire TV Stick Lite/HD).

> **Regra absoluta cumprida:** este projeto **NÃO utiliza** Android `WebView`, Chromium, PWA, HTML/CSS, JavaScript, Custom Tabs, Trusted Web Activity ou emulação de navegador. Toda interface e reprodução de mídia utilizam componentes nativos do Android e comunicação HTTP/JSON oficial.

---

## 1. Arquitetura 100% Nativa em Camadas

- **Camada de UI Nativa para TV (`feed/`, `navigation/`, `settings/`, `comments/`, `search/`, `messaging/`, `profile/`, `live/`, `sharing/`, `ui/`):**
  - Menu lateral recolhível (`TvNavigationDrawerOverlay`) e foco espacial explícito com `FocusRequester` e `focusProperties`.
  - Navegação completa por D-pad (`FeedDpadFocusHost` via `Modifier.onKeyEvent`): `Cima`/`Baixo` (trocar item), `OK` curto (pausa/play), `Segurar OK` ou `Menu` (opções do vídeo), `Esquerda` (gaveta de navegação), `Direita` (ações do vídeo), `Voltar` (fecha painel / retorna ao Início / confirma saída). Sem necessidade de mouse ou ponteiro virtual.
- **Camada de Integração Oficial TikTok (`auth/`, `api/`, `auth-backend/`):**
  - **Login Kit QR Code Authorization v2:** `POST /v2/oauth/get_qrcode/`, `POST /v2/oauth/check_qrcode/`, troca/rotação automática de `refresh_token` (`POST /v2/oauth/token/`) e revogação (`POST /v2/oauth/revoke/`) mantendo o `client_secret` exclusivamente no `auth-backend/`.
  - **Display API v2:** `GET /v2/user/info/`, `POST /v2/video/list/` e `POST /v2/video/query/` para dados reais de perfil, métricas e vídeos publicados da conta autorizada.
  - **Content Posting API v2:** `POST /v2/post/publish/creator_info/query/`, `POST /v2/post/publish/video/init/`, `POST /v2/post/publish/inbox/video/init/`, upload em chunks via HTTP `PUT` e verificação de status (`POST /v2/post/publish/status/fetch/`).
- **Camada de Reprodução Nativa (`player/`):**
  - `Media3 ExoPlayer` (`NativeMedia3PlayerSurface`) integrado a `MediaSession` e cache LRU delimitado (`VideoCacheManager` / `SimpleCache`).
  - Seleção de modo de decodificação MediaCodec (`Automático`, `Hardware padrão`, `Baixa latência`).
  - Validação estrita em `MediaSourceValidator`: apenas URIs locais (`content://`, `file://`) ou streams diretos verificados (`.mp4`, `.m3u8`, `.mpd`) entram no `ExoPlayer`; links HTML de incorporação da API (`embed_link` / `share_url`) nunca são forçados em players WebView e oferecem QR Code para abrir no celular.
- **Camada de Persistência Segura e Desempenho (`data/`, `auth/SecureTokenStore.kt`, `settings/`, `performance/`):**
  - Tokens OAuth criptografados via **Android Keystore AES-256-GCM** + DataStore.
  - Cache estruturado offline de perfil e vídeos em **Room Database (`AppCacheDatabase`)**.
  - `DeviceCapabilityDetector` analisa RAM total, `isLowRamDevice`, resolução/DPI, codecs de hardware MediaCodec (`H.264`, `H.265/HEVC`, `VP9`, `AV1`) e aplica automaticamente o perfil recomendado (`Econômico`, `Equilibrado` ou `Qualidade`).

---

## 2. Configuração de Credenciais Oficiais do TikTok

1. Crie um app no **TikTok for Developers** (`https://developers.tiktok.com/`) e habilite:
   - **Login Kit** (QR Code Authorization v2)
   - **Display API** (`user.info.basic`, `user.info.profile`, `user.info.stats`, `video.list`)
   - **Content Posting API** (`video.upload`, `video.publish`)
2. Configure o backend OAuth (`auth-backend/`):
   ```bash
   cd auth-backend
   cp .env.example .env
   # Edite .env com TIKTOK_CLIENT_KEY e TIKTOK_CLIENT_SECRET
   npm install
   npm start
   ```
3. No painel **Secrets** do AI Studio (ou `.env` local em builds privados), defina:
   - `TIKTOK_CLIENT_KEY` (chave pública)
   - `TIKTOK_AUTH_BACKEND_URL` (URL HTTPS do seu `auth-backend`)
   - **Nunca** inclua `TIKTOK_CLIENT_SECRET` no APK Android.

---

## 3. Compilação dos APKs e Testes Locais

O projeto configura `abiFilters("armeabi-v7a", "arm64-v8a")` para suportar Fire TV Stick 32-bit e Android TV 64-bit em um único APK compacto:

```bash
# Compilar APK Debug Universal (armeabi-v7a + arm64-v8a)
gradle :app:assembleDebug

# Compilar APK Release Universal (requer KEYSTORE_PATH, STORE_PASSWORD e KEY_PASSWORD)
gradle :app:assembleRelease

# Executar testes Android (Robolectric + Compose UI Test + JUnit4)
gradle :app:testDebugUnitTest

# Executar testes do servidor backend OAuth
node --test auth-backend/server.test.js
```

---

## 4. Geração Automática de APK via GitHub Actions

O workflow **`.github/workflows/build-apk.yml`** executa os testes (`gradle :app:testDebugUnitTest`) e gera o APK universal diretamente no GitHub:

1. Envie o projeto para `main` ou `master`, ou acione **Actions → Build & Test Android TV APK → Run workflow**.
2. Baixe o APK universal em **Artifacts** (`tiktok-tv-universal-debug-apk`).
3. Para gerar **Releases públicas automáticas** com o APK anexado, envie uma tag de versão (ex.: `git tag v1.0.0 && git push origin v1.0.0`).
4. *(Opcional)* Para assinatura Release e credenciais no CI, configure em **Settings → Secrets and variables → Actions**: `ANDROID_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`, `TIKTOK_CLIENT_KEY` e `TIKTOK_AUTH_BACKEND_URL`.

---

## 5. Instalação na TV (Sideload)

1. Habilite **Opções de desenvolvedor / Apps de fontes desconhecidas** na TV ou Fire TV Stick.
2. Instale via `adb install -r app/build/outputs/apk/debug/app-debug.apk` ou gerenciador de arquivos na TV.
3. O aplicativo aparecerá no launcher Android TV / Fire TV com ícone adaptativo e banner `320x180`.
