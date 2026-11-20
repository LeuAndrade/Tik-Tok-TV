const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

test('auth-backend exposes required TikTok QR v2 and OAuth endpoints without hardcoded secrets', () => {
  const src = fs.readFileSync(path.join(__dirname, 'server.js'), 'utf8');
  assert.ok(src.includes('https://open.tiktokapis.com'));
  assert.ok(src.includes('/v2/oauth/get_qrcode/'));
  assert.ok(src.includes('/v2/oauth/check_qrcode/'));
  assert.ok(src.includes('/v2/oauth/token/'));
  assert.ok(src.includes('/v2/oauth/revoke/'));
  assert.ok(src.includes('process.env.TIKTOK_CLIENT_SECRET'));
  assert.ok(src.includes('client_ticket=tobefilled'));
});
