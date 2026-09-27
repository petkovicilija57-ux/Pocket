import http from 'node:http';
import crypto from 'node:crypto';
import { URL } from 'node:url';

const env = process.env;
const required = ['PAYPAL_CLIENT_ID','PAYPAL_CLIENT_SECRET','PUBLIC_BASE_URL'];
for (const k of required) if (!env[k]) console.warn(`[config] ${k} is not set`);
const authorizeUrl = env.PAYPAL_AUTHORIZE_URL || 'https://www.paypal.com/signin/authorize';
const tokenUrl = env.PAYPAL_TOKEN_URL || 'https://api-m.paypal.com/v1/oauth2/token';
const returnUriDefault = env.APP_RETURN_URI || 'pocket://oauth/paypal';
const sessions = new Map();
const ttlMs = 10 * 60 * 1000;

const random = (n=32) => crypto.randomBytes(n).toString('base64url');
const clean = () => { const now=Date.now(); for (const [k,v] of sessions) if (now-v.created > ttlMs) sessions.delete(k); };
setInterval(clean, 60_000).unref();

function send(res, code, body, type='text/plain; charset=utf-8') {
  res.writeHead(code, {'content-type':type,'cache-control':'no-store','x-content-type-options':'nosniff','referrer-policy':'no-referrer'}); res.end(body);
}
function redirect(res, url) { res.writeHead(302, {'location':url,'cache-control':'no-store'}); res.end(); }

async function exchangeCode(code, redirectUri) {
  const basic = Buffer.from(`${env.PAYPAL_CLIENT_ID}:${env.PAYPAL_CLIENT_SECRET}`).toString('base64');
  const body = new URLSearchParams({grant_type:'authorization_code', code, redirect_uri:redirectUri});
  const r = await fetch(tokenUrl, {method:'POST', headers:{'authorization':`Basic ${basic}`,'content-type':'application/x-www-form-urlencoded','accept':'application/json'}, body});
  if (!r.ok) throw new Error(`token exchange failed: ${r.status}`);
  return r.json();
}

const server = http.createServer(async (req,res) => {
  try {
    const u = new URL(req.url, env.PUBLIC_BASE_URL || `http://localhost:${env.PORT||8080}`);
    if (u.pathname === '/health') return send(res,200,'ok');
    if (u.pathname === '/oauth/paypal/start') {
      if (!env.PAYPAL_CLIENT_ID || !env.PUBLIC_BASE_URL) return send(res,503,'PayPal is not configured');
      const state = random(); const appReturn = u.searchParams.get('return_uri') || returnUriDefault;
      const callback = `${env.PUBLIC_BASE_URL.replace(/\/$/,'')}/oauth/paypal/callback`;
      sessions.set(state,{created:Date.now(), appReturn, callback, status:'pending'});
      const a = new URL(authorizeUrl);
      a.searchParams.set('client_id',env.PAYPAL_CLIENT_ID); a.searchParams.set('response_type','code');
      a.searchParams.set('scope','openid profile email'); a.searchParams.set('redirect_uri',callback); a.searchParams.set('state',state);
      return redirect(res,a.toString());
    }
    if (u.pathname === '/oauth/paypal/callback') {
      const state=u.searchParams.get('state'), code=u.searchParams.get('code'); const s=sessions.get(state);
      if (!state || !code || !s || s.status!=='pending') return send(res,400,'Invalid or expired OAuth state');
      const token = await exchangeCode(code,s.callback);
      // Do not return PayPal access tokens to the app. Store only a one-time Pocket session handle.
      const handle=random(24); sessions.delete(state); sessions.set(handle,{created:Date.now(),status:'verified',paypalTokenType:token.token_type||'Bearer'});
      const r = new URL(s.appReturn); r.searchParams.set('session',handle); return redirect(res,r.toString());
    }
    if (u.pathname.startsWith('/oauth/paypal/session/')) {
      const handle=decodeURIComponent(u.pathname.substring('/oauth/paypal/session/'.length)); const s=sessions.get(handle);
      if (!s || s.status!=='verified') return send(res,404,'not connected');
      return send(res,200,JSON.stringify({connected:true}), 'application/json; charset=utf-8');
    }
    return send(res,404,'not found');
  } catch (e) { console.error(e); return send(res,500,'server error'); }
});
server.listen(Number(env.PORT||8080),()=>console.log(`Pocket backend listening on ${env.PORT||8080}`));
