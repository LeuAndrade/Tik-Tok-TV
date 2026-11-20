# TikTok TV — Cliente para Android TV, Google TV e Amazon Fire TV Stick

Cliente independente de televisão desenvolvido em **Kotlin + Jetpack Compose for TV + Media3 + WebView otimizada**, projetado para uso integral com **controle remoto (D-pad)** em telas 16:9 (Full HD / 4K), inclusive em dispositivos com **1 GB de RAM** (como Fire TV Stick Lite/HD).

> **Aviso de independência:** Este projeto é um cliente independente que interage com as interfaces oficiais do TikTok (TikTok Web e TikTok Open APIs / Login Kit v2 / Display API v2). Não utiliza o identificador de pacote nem a assinatura digital do aplicativo oficial do TikTok.

---

## 1. Arquitetura Híbrida

- **Camada A — Interface Nativa Android TV (`navigation/`, `settings/`, `comments/`, `search/`, `messaging/`, `profile/`, `ui/`):**
  - Menu lateral recolhível (`TvNavigationDrawerOverlay`) que desaparece automaticamente durante a reprodução.
  - Mapeamento completo de controle remoto (`TvRemoteKeyHandler`): `Cima` (vídeo anterior), `Baixo` (próximo vídeo), `OK` (pausa/play), `Segurar OK` (menu contextual), `Esquerda` (menu), `Direita` (ações do vídeo), `Voltar` (fecha painel / retorna / confirma saída) e **Ponteiro Virtual D-pad** como último recurso para elementos web não focáveis.
- **Camada B — Integração Oficial TikTok (`auth/` + `auth-backend/`):**
  - **Login Kit QR Code Authorization v2:** `POST /v2/oauth/get_qrcode/`, `POST /v2/oauth/check_qrcode/`, troca e rotação automática de `refresh_token` (`POST /v2/oauth/token/`) e revogação (`POST /v2/oauth/revoke/`) com `client_secret` mantido exclusivamente no `auth-backend/`.
  - **Display API v2:** `GET /v2/user/info/` e `POST /v2/video/list/` para exibir dados reais de perfil e vídeos públicos autorizados.
- **Camada C — Compatibilidade Web Segura (`web/`):**
  - **Instância única de `WebView`** (evita múltiplas WebViews simultâneas para proteger aparelhos de 1 GB de RAM).
  - Lista de domínios HTTPS permitidos (`TikTokWebDomainPolicy`), bloqueio de `http://` e domínios não confiáveis, proteção TLS estrita e recuperação automática contra `onRenderProcessGone`.
  - Ponte segura (`TikTokWebBridge`) para metadados reais e execução de ações na sessão web do usuário (curtir, comentar, republicar, salvar, seguir, pausa/play, silenciar).
- **Camada D — Gerenciamento de Sessões (`SecureTokenStore` & `TikTokWebSessionManager`):**
  - Separação explícita entre tokens OAuth da API (criptografados com **Android Keystore AES-256-GCM** + DataStore) e cookies da sessão web (`CookieManager` persistido via `flush()`).

---

## 2. Configuração de Credenciais Oficiais do TikTok

1. Crie um app no **TikTok for Developers** (`https://developers.tiktok.com/`) e habilite:
   - **Login Kit** (incluindo QR Code Authorization v2)
   - **Display API** (escopos: `user.info.basic`, `user.info.profile`, `user.info.stats`, `video.list`)
2. Configure o backend seguro (`auth-backend/`):
   ```bash
   cd auth-backend
   cp .env.example .env
   # Edite .env com TIKTOK_CLIENT_KEY e TIKTOK_CLIENT_SECRET
   npm install
   npm start
   ```
3. No painel **Secrets** do AI Studio (ou `.env` local em builds privados), configure:
   - `TIKTOK_CLIENT_KEY` (chave pública)
   - `TIKTOK_AUTH_BACKEND_URL` (URL HTTPS do seu `auth-backend`)
   - **Nunca** coloque `TIKTOK_CLIENT_SECRET` no APK Android.

> **Nota prática:** Mesmo antes de configurar o `auth-backend` para Login Kit v2, as abas **"Entrar com TikTok"** e **"Entrar usando o celular (Web QR)"** funcionam diretamente com a sessão web oficial do TikTok (`https://www.tiktok.com/login`).

---

## 3. Compilação dos APKs (Universal, ARM32 e ARM64)

O projeto mantém `abiFilters("armeabi-v7a", "arm64-v8a")` para gerar APK universal compacto para Android TV e Fire TV Stick.

```bash
# APK Universal de Desenvolvimento (armeabi-v7a + arm64-v8a)
gradle :app:assembleDebug

# APK Universal de Release (requer variáveis KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD)
gradle :app:assembleRelease

# Executar testes automatizados (Robolectric + JUnit)
gradle :app:testDebugUnitTest
```

Para gerar APKs específicos por ABI em builds dedicados (ex.: Fire TV Stick Lite 32-bit vs 4K Max 64-bit), configure `-Pandroid.injected.build.abi=armeabi-v7a` ou `arm64-v8a`.

---

## 4. Instalação Manual (Sideload) em Fire TV Stick e Android TV

1. Ative **Opções de desenvolvedor / Apps de fontes desconhecidas** na TV ou Fire TV Stick.
2. Envie o APK via `adb install -r app-debug.apk` ou aplicativo Downloader / gerenciador de arquivos na TV.
3. O aplicativo aparecerá no launcher da TV com ícone adaptativo e banner (`320x180`).
