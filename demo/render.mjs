// Renders index.html to an MP4: headless Chrome draws each frame (seek(t)), ffmpeg encodes them.
// Usage: node render.mjs preview 2,6,12      -> preview-<t>.png next to this file
//        node render.mjs video [out.mp4]     -> full film at 30 fps
import { spawn } from 'node:child_process';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const CHROME = process.env.CHROME ?? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const PORT = 9333, FPS = 30, DURATION = 50;
const [mode = 'preview', arg] = process.argv.slice(2);

const chrome = spawn(CHROME, [
  '--headless=new', `--remote-debugging-port=${PORT}`, `--user-data-dir=${mkdtempSync(join(tmpdir(), 'tact-chrome-'))}`,
  '--window-size=1920,1080', '--hide-scrollbars', '--force-device-scale-factor=1', '--no-first-run', 'about:blank',
], { stdio: 'ignore' });

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
async function pageSocket() {
  for (let i = 0; i < 100; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json();
      const page = list.find((p) => p.type === 'page');
      if (page) return page.webSocketDebuggerUrl;
    } catch { /* Chrome is still starting */ }
    await sleep(200);
  }
  throw new Error('Chrome did not start');
}

const ws = new WebSocket(await pageSocket());
await new Promise((r) => (ws.onopen = r));
let id = 0;
const pending = new Map();
const waiters = [];
ws.onmessage = (m) => {
  const msg = JSON.parse(m.data);
  if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); }
  else waiters.filter((w) => w.method === msg.method).forEach((w) => w.resolve());
};
const send = (method, params = {}) => new Promise((resolve, reject) => {
  const n = ++id;
  pending.set(n, (msg) => (msg.error ? reject(new Error(msg.error.message)) : resolve(msg.result)));
  ws.send(JSON.stringify({ id: n, method, params }));
});
const once = (method) => new Promise((resolve) => waiters.push({ method, resolve }));

await send('Page.enable');
await send('Emulation.setDeviceMetricsOverride', { width: 1920, height: 1080, deviceScaleFactor: 1, mobile: false });
const loaded = once('Page.loadEventFired');
await send('Page.navigate', { url: pathToFileURL(join(here, 'index.html')).href });
await loaded;
await sleep(500);

const frame = async (t, format = 'png') => {
  await send('Runtime.evaluate', {
    expression: `(async () => { seek(${t}); await new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r))); })()`,
    awaitPromise: true,
  });
  const { data } = await send('Page.captureScreenshot', { format });
  return Buffer.from(data, 'base64');
};

try {
  if (mode === 'preview') {
    for (const t of (arg ?? '2').split(',').map(Number)) writeFileSync(join(here, `preview-${t}.png`), await frame(t));
  } else {
    const out = arg ?? join(here, 'tact-demo.mp4');
    const ffmpeg = spawn('ffmpeg', [
      '-loglevel', 'error', '-y', '-f', 'image2pipe', '-framerate', String(FPS), '-vcodec', 'png', '-i', '-',
      '-c:v', 'libx264', '-preset', 'slow', '-crf', '15', '-pix_fmt', 'yuv420p', '-movflags', '+faststart', out,
    ], { stdio: ['pipe', 'ignore', 'inherit'] });
    const total = DURATION * FPS;
    for (let i = 0; i < total; i++) {
      if (!ffmpeg.stdin.write(await frame(i / FPS))) await new Promise((r) => ffmpeg.stdin.once('drain', r));
      if (i % 100 === 0) console.log(`frame ${i}/${total}`);
    }
    ffmpeg.stdin.end();
    await new Promise((r) => ffmpeg.on('close', r));
    console.log(`done: ${out}`);
  }
} finally {
  ws.close();
  chrome.kill();
}
