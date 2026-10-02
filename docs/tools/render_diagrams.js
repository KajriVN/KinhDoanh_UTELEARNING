// Render docs/diagrams/*.mmd -> docs/images/diagrams/*.png bằng Mermaid trong Edge headless
const fs = require('fs');
const path = require('path');
const { launch } = require('./cdp');

const PROJECT = 'E:/Document_3rd_grade/web_dev/Final_project/Kinh doanh khóa học trực tuyến và quản lý học tập số UTELearn';
const SRC = path.join(PROJECT, 'docs/diagrams');
const OUT = path.join(PROJECT, 'docs/images/diagrams');
fs.mkdirSync(OUT, { recursive: true });

const only = process.argv[2];

function addStyles(src) {
  if (!src.startsWith('flowchart')) return src;
  const ids = new Set();
  for (const m of src.matchAll(/\b(start|stop\d*)\b(?=\s*\(\(|\s*-->|\s*$)/gm)) ids.add(m[1]);
  const starts = [...ids].filter(i => i === 'start');
  const stops = [...ids].filter(i => i.startsWith('stop'));
  let extra = '\n    classDef startNode fill:#111827,stroke:#111827,color:#111827;\n    classDef stopNode fill:#111827,stroke:#111827,color:#111827;\n';
  if (starts.length) extra += `    class ${starts.join(',')} startNode;\n`;
  if (stops.length) extra += `    class ${stops.join(',')} stopNode;\n`;
  return src + extra;
}

(async () => {
  const cdp = await launch();
  const ctx = await cdp.newContext();
  const files = fs.readdirSync(SRC).filter(f => f.endsWith('.mmd') && (!only || f.includes(only)));
  for (const f of files) {
    const src = addStyles(fs.readFileSync(path.join(SRC, f), 'utf8'));
    const wide = /erDiagram|sequenceDiagram|flowchart LR/.test(src);
    const width = f.startsWith('03') ? 1500 : (wide ? 1250 : 980);
    const html = `<!doctype html><html><head><meta charset="utf-8">
<style>body{margin:0;background:#fff;font-family:"Segoe UI",Arial,sans-serif}#d{display:inline-block;padding:16px;background:#fff}</style>
<script src="https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"></script></head>
<body><div id="d"><pre class="mermaid">${src.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')}</pre></div>
<script>
mermaid.initialize({ startOnLoad: false, theme: 'default', securityLevel: 'loose',
  themeVariables: { fontFamily: '"Segoe UI", Arial, sans-serif', fontSize: '15px',
    primaryColor: '#e8efff', primaryBorderColor: '#2563eb', primaryTextColor: '#111827', lineColor: '#475569' },
  flowchart: { curve: 'basis', htmlLabels: true, useMaxWidth: false, nodeSpacing: 40, rankSpacing: 45 },
  sequence: { useMaxWidth: false, mirrorActors: false, actorMargin: 40 },
  er: { useMaxWidth: false } });
window.done = mermaid.run({ querySelector: '.mermaid' }).then(() => 'ok').catch(e => 'ERR ' + e.message);
</script></body></html>`;
    const tmp = path.join(__dirname, 'tmp-diagram.html');
    fs.writeFileSync(tmp, html);
    const page = await cdp.newPage(ctx, width, 900, 2);
    await page.goto('file:///' + tmp.replace(/\\/g, '/'), 300);
    const status = await page.eval('window.done');
    if (status !== 'ok') { console.log('FAIL', f, status); continue; }
    const out = path.join(OUT, f.replace('.mmd', '.png'));
    await page.shotElement(out, '#d');
    console.log('OK', f);
  }
  await cdp.close();
})().catch(e => { console.error(e); process.exit(1); });
