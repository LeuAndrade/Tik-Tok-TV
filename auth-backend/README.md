# TikTok TV Auth Backend (Login Kit QR Code v2)

Servidor Node.js/Express que mantém o `TIKTOK_CLIENT_SECRET` exclusivamente no servidor (fora do APK Android) e implementa o fluxo oficial **TikTok Login Kit QR Code Authorization v2**:

- `POST /api/tiktok/qr/start` -> `POST https://open.tiktokapis.com/v2/oauth/get_qrcode/` (gera `client_ticket` + `state` anti-CSRF e substitui `client_ticket=tobefilled` em `scan_qrcode_url`).
- `POST /api/tiktok/qr/status` -> `POST https://open.tiktokapis.com/v2/oauth/check_qrcode/` + troca automática de `code` em `POST https://open.tiktokapis.com/v2/oauth/token/` quando `status === "confirmed"`.
- `POST /api/tiktok/oauth/refresh` -> Rotação de `refresh_token`.
- `POST /api/tiktok/oauth/revoke` -> Revogação no logout.

## Execução

```bash
cp .env.example .env
# Preencha TIKTOK_CLIENT_KEY e TIKTOK_CLIENT_SECRET
npm install
npm start
```
