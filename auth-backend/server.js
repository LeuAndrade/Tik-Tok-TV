require('dotenv').config();
const crypto = require('crypto');
const express = require('express');
const helmet = require('helmet');

const app = express();
app.use(helmet());
app.use(express.json());

const PORT = process.env.PORT || 8787;
const CLIENT_KEY = process.env.TIKTOK_CLIENT_KEY || '';
const CLIENT_SECRET = process.env.TIKTOK_CLIENT_SECRET || '';
const TIKTOK_API_BASE = 'https://open.tiktokapis.com';

// Short-lived in-memory ticket store (use Redis in multi-instance production)
const pendingQrSessions = new Map();

function ensureCredentials(res) {
  if (!CLIENT_KEY || !CLIENT_SECRET) {
    res.status(503).json({
      error: 'TIKTOK_CLIENT_KEY and TIKTOK_CLIENT_SECRET must be configured on the auth-backend server.',
    });
    return false;
  }
  return true;
}

function cleanupExpiredSessions() {
  const now = Date.now();
  for (const [token, entry] of pendingQrSessions.entries()) {
    if (now > entry.expiresAt) {
      pendingQrSessions.delete(token);
    }
  }
}

app.get('/health', (_req, res) => {
  res.json({ ok: true, configured: Boolean(CLIENT_KEY && CLIENT_SECRET) });
});

// POST /api/tiktok/qr/start -> POST https://open.tiktokapis.com/v2/oauth/get_qrcode/
app.post('/api/tiktok/qr/start', async (req, res) => {
  if (!ensureCredentials(res)) return;
  cleanupExpiredSessions();

  const clientTicket = String(req.body?.client_ticket || '').trim() || crypto.randomBytes(8).toString('hex');
  const state = String(req.body?.state || '').trim() || crypto.randomBytes(12).toString('hex');
  const scope = String(req.body?.scope || 'user.info.basic,user.info.profile,user.info.stats,video.list');

  try {
    const body = new URLSearchParams({
      client_key: CLIENT_KEY,
      client_secret: CLIENT_SECRET,
      scope,
      state,
    });

    const ttResp = await fetch(`${TIKTOK_API_BASE}/v2/oauth/get_qrcode/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString(),
    });
    const payload = await ttResp.json();

    if (!ttResp.ok || !payload.scan_qrcode_url || !payload.token) {
      return res.status(ttResp.status || 502).json({
        error: payload.error_description || payload.error || 'Failed to obtain QR code from TikTok',
      });
    }

    const finalQrUrl = payload.scan_qrcode_url.includes('client_ticket=tobefilled')
      ? payload.scan_qrcode_url.replace('client_ticket=tobefilled', `client_ticket=${encodeURIComponent(clientTicket)}`)
      : payload.scan_qrcode_url;

    const expiresIn = Number(payload.expires_in || 300);
    pendingQrSessions.set(payload.token, {
      clientTicket,
      state,
      expiresAt: Date.now() + expiresIn * 1000,
    });

    return res.json({
      scan_qrcode_url: finalQrUrl,
      token: payload.token,
      client_ticket: clientTicket,
      state,
      expires_in: expiresIn,
    });
  } catch (err) {
    return res.status(502).json({ error: err.message || 'Network error contacting TikTok API' });
  }
});

// POST /api/tiktok/qr/status -> POST https://open.tiktokapis.com/v2/oauth/check_qrcode/
app.post('/api/tiktok/qr/status', async (req, res) => {
  if (!ensureCredentials(res)) return;

  const token = String(req.body?.token || '');
  const clientTicket = String(req.body?.client_ticket || '');
  const state = String(req.body?.state || '');
  const session = pendingQrSessions.get(token);

  if (!session || session.clientTicket !== clientTicket || session.state !== state) {
    return res.status(400).json({ status: 'expired', error: 'Invalid or expired QR session ticket/state.' });
  }

  try {
    const checkBody = new URLSearchParams({
      client_key: CLIENT_KEY,
      client_secret: CLIENT_SECRET,
      token,
    });

    const checkResp = await fetch(`${TIKTOK_API_BASE}/v2/oauth/check_qrcode/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: checkBody.toString(),
    });
    const checkPayload = await checkResp.json();

    if (checkPayload.status === 'confirmed' && checkPayload.code) {
      pendingQrSessions.delete(token);

      const tokenBody = new URLSearchParams({
        client_key: CLIENT_KEY,
        client_secret: CLIENT_SECRET,
        code: checkPayload.code,
        grant_type: 'authorization_code',
      });

      const tokenResp = await fetch(`${TIKTOK_API_BASE}/v2/oauth/token/`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: tokenBody.toString(),
      });
      const tokenBundle = await tokenResp.json();

      if (!tokenResp.ok || !tokenBundle.access_token) {
        return res.status(502).json({
          status: 'error',
          error: tokenBundle.error_description || 'Token exchange failed after QR confirmation.',
        });
      }

      return res.json({
        status: 'confirmed',
        client_ticket: clientTicket,
        state,
        token_bundle: tokenBundle,
      });
    }

    if (checkPayload.status === 'expired' || checkPayload.status === 'refused') {
      pendingQrSessions.delete(token);
    }

    return res.json({
      status: checkPayload.status || 'new',
      client_ticket: clientTicket,
      state,
      error: checkPayload.error_description || checkPayload.error || null,
    });
  } catch (err) {
    return res.status(502).json({ status: 'error', error: err.message || 'Polling error' });
  }
});

// POST /api/tiktok/oauth/refresh -> POST https://open.tiktokapis.com/v2/oauth/token/
app.post('/api/tiktok/oauth/refresh', async (req, res) => {
  if (!ensureCredentials(res)) return;
  const refreshToken = String(req.body?.refresh_token || '');
  if (!refreshToken) return res.status(400).json({ error: 'refresh_token is required' });

  try {
    const body = new URLSearchParams({
      client_key: CLIENT_KEY,
      client_secret: CLIENT_SECRET,
      grant_type: 'refresh_token',
      refresh_token: refreshToken,
    });
    const ttResp = await fetch(`${TIKTOK_API_BASE}/v2/oauth/token/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString(),
    });
    const payload = await ttResp.json();
    return res.status(ttResp.status).json(payload);
  } catch (err) {
    return res.status(502).json({ error: err.message });
  }
});

// POST /api/tiktok/oauth/revoke -> POST https://open.tiktokapis.com/v2/oauth/revoke/
app.post('/api/tiktok/oauth/revoke', async (req, res) => {
  if (!ensureCredentials(res)) return;
  const token = String(req.body?.token || '');
  if (!token) return res.status(400).json({ error: 'token is required' });

  try {
    const body = new URLSearchParams({
      client_key: CLIENT_KEY,
      client_secret: CLIENT_SECRET,
      token,
    });
    const ttResp = await fetch(`${TIKTOK_API_BASE}/v2/oauth/revoke/`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString(),
    });
    return res.status(ttResp.status).json({ revoked: ttResp.ok });
  } catch (err) {
    return res.status(502).json({ error: err.message });
  }
});

app.listen(PORT, () => {
  console.log(`TikTok TV auth-backend listening on port ${PORT}`);
});
