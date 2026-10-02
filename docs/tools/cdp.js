// Mini CDP helper: điều khiển Edge headless để chụp ảnh
const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
const PORT = 9333;
const sleep = ms => new Promise(r => setTimeout(r, ms));

async function launch() {
  const profile = path.join(__dirname, 'edge-profile');
  const proc = spawn(EDGE, ['--headless=new', `--remote-debugging-port=${PORT}`, `--user-data-dir=${profile}`,
    '--no-first-run', '--no-default-browser-check', '--disable-extensions', '--hide-scrollbars',
    '--autoplay-policy=no-user-gesture-required', 'about:blank'], { stdio: 'ignore' });
  let info;
  for (let i = 0; i < 60; i++) {
    try { info = await (await fetch(`http://127.0.0.1:${PORT}/json/version`)).json(); break; } catch (e) { await sleep(500); }
  }
  if (!info) throw new Error('Edge không khởi động được');
  const ws = new WebSocket(info.webSocketDebuggerUrl);
  await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
  const cdp = new Cdp(ws, proc);
  return cdp;
}

class Cdp {
  constructor(ws, proc) {
    this.ws = ws; this.proc = proc; this.id = 0; this.pending = new Map(); this.listeners = [];
    ws.onmessage = ev => {
      const msg = JSON.parse(ev.data);
      if (msg.id && this.pending.has(msg.id)) {
        const { res, rej } = this.pending.get(msg.id); this.pending.delete(msg.id);
        msg.error ? rej(new Error(JSON.stringify(msg.error))) : res(msg.result);
      } else if (msg.method) {
        this.listeners = this.listeners.filter(l => !(l.method === msg.method && l.sessionId === msg.sessionId && (l.fn(msg.params), true)));
      }
    };
  }
  send(method, params = {}, sessionId) {
    const id = ++this.id;
    this.ws.send(JSON.stringify({ id, method, params, sessionId }));
    return new Promise((res, rej) => this.pending.set(id, { res, rej }));
  }
  once(method, sessionId, timeout = 30000) {
    return new Promise((res) => {
      const t = setTimeout(() => res(null), timeout);
      this.listeners.push({ method, sessionId, fn: p => { clearTimeout(t); res(p); } });
    });
  }
  async newContext() { return (await this.send('Target.createBrowserContext')).browserContextId; }
  async newPage(contextId, width = 1366, height = 860, scale = 1) {
    const { targetId } = await this.send('Target.createTarget', { url: 'about:blank', browserContextId: contextId });
    const { sessionId } = await this.send('Target.attachToTarget', { targetId, flatten: true });
    await this.send('Page.enable', {}, sessionId);
    await this.send('Runtime.enable', {}, sessionId);
    await this.send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: scale, mobile: false }, sessionId);
    return new Page(this, sessionId, width, height);
  }
  async close() { try { await this.send('Browser.close'); } catch (e) {} this.proc.kill(); }
}

class Page {
  constructor(cdp, sessionId, width, height) { this.cdp = cdp; this.s = sessionId; this.width = width; this.height = height; }
  async goto(url, extraWait = 800) {
    const loaded = this.cdp.once('Page.loadEventFired', this.s);
    await this.cdp.send('Page.navigate', { url }, this.s);
    await loaded; await sleep(extraWait);
  }
  async eval(expression) {
    const r = await this.cdp.send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true }, this.s);
    if (r.exceptionDetails) throw new Error(JSON.stringify(r.exceptionDetails).slice(0, 400));
    return r.result.value;
  }
  // chạy JS gây chuyển trang (submit form) rồi chờ load
  async evalNav(expression, extraWait = 800) {
    const loaded = this.cdp.once('Page.loadEventFired', this.s);
    await this.eval(expression);
    await loaded; await sleep(extraWait);
  }
  async shotClip(file, clip, scale = 1) {
    const r = await this.cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true, clip: { ...clip, scale } }, this.s);
    fs.writeFileSync(file, Buffer.from(r.data, 'base64'));
  }
  async shotElement(file, selector, pad = 0) {
    const rect = await this.eval(`(() => { const r = document.querySelector(${JSON.stringify(selector)}).getBoundingClientRect();
      return { x: r.left + scrollX, y: r.top + scrollY, width: r.width, height: r.height }; })()`);
    await this.shotClip(file, { x: Math.max(rect.x - pad, 0), y: Math.max(rect.y - pad, 0), width: rect.width + pad * 2, height: rect.height + pad * 2 });
  }
  async shotPage(file, maxHeight = 1100) {
    const h = await this.eval('Math.max(document.body.scrollHeight, document.documentElement.scrollHeight)');
    await this.shotClip(file, { x: 0, y: 0, width: this.width, height: Math.min(h, maxHeight) });
  }
}

module.exports = { launch, sleep };
