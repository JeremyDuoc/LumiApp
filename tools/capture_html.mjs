import { spawn } from 'child_process';
import { writeFileSync, mkdirSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const worktreeDir = resolve(__dirname, '..');
const htmlPath = resolve(worktreeDir, 'reference', 'lumi_onboarding_v7_luz_cromatica.html');
const outDir = resolve(worktreeDir, 'visual-qa', 'evidence', 'html');
mkdirSync(outDir, { recursive: true });

const EDGE_PATH = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const PORT = 9226;

console.log('Launching headless Edge on port', PORT);
const edgeProc = spawn(EDGE_PATH, [
  '--headless=new',
  '--no-sandbox',
  '--disable-gpu',
  `--remote-debugging-port=${PORT}`,
  '--window-size=430,932',
  '--hide-scrollbars',
  `file:///${htmlPath.replace(/\\/g, '/')}`
]);

async function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function getPageWsUrl() {
  for (let i = 0; i < 40; i++) {
    try {
      const res = await fetch(`http://127.0.0.1:${PORT}/json`);
      const data = await res.json();
      const pageTarget = data.find(t => t.type === 'page');
      if (pageTarget && pageTarget.webSocketDebuggerUrl) {
        return pageTarget.webSocketDebuggerUrl;
      }
    } catch (_) {}
    await sleep(250);
  }
  throw new Error('Could not find page target in Edge CDP');
}

async function run() {
  try {
    const wsUrl = await getPageWsUrl();
    console.log('Connected to Page CDP at:', wsUrl);

    const ws = new WebSocket(wsUrl);
    await new Promise((resolve, reject) => {
      ws.onopen = resolve;
      ws.onerror = reject;
    });

    let msgId = 1;
    function sendCommand(method, params = {}) {
      return new Promise((resolve, reject) => {
        const id = msgId++;
        const handler = (evt) => {
          const resp = JSON.parse(typeof evt.data === 'string' ? evt.data : evt.data.toString());
          if (resp.id === id) {
            ws.removeEventListener('message', handler);
            if (resp.error) reject(resp.error);
            else resolve(resp.result);
          }
        };
        ws.addEventListener('message', handler);
        ws.send(JSON.stringify({ id, method, params }));
      });
    }

    await sendCommand('Page.enable');
    await sendCommand('Runtime.enable');

    // Emular viewport móvil (Pixel 7 Pro: 412x915)
    await sendCommand('Emulation.setDeviceMetricsOverride', {
      width: 412,
      height: 915,
      deviceScaleFactor: 2.0,
      mobile: true
    });

    await sleep(600);

    async function evaluate(code) {
      return sendCommand('Runtime.evaluate', { expression: code, awaitPromise: true });
    }

    async function captureScreen(fileName) {
      const { data } = await sendCommand('Page.captureScreenshot', { format: 'png' });
      const targetPath = resolve(outDir, fileName);
      writeFileSync(targetPath, Buffer.from(data, 'base64'));
      console.log(`[HTML Evidence Saved] -> ${fileName}`);
    }

    // 1. Capturar los 8 pasos del Onboarding (Tema Noir por defecto)
    console.log('\n--- Capturando 8 Pasos en Modo Track (HTML) ---');
    for (let step = 0; step <= 7; step++) {
      await evaluate(`goto(${step})`);
      await sleep(350);
      await captureScreen(`step_${step}.png`);
    }

    // 2. Capturar los 8 temas
    console.log('\n--- Capturando los 8 Temas (HTML) ---');
    const themes = ['noir', 'ciruela', 'bosque', 'indigo', 'petalo', 'marfil', 'salvia', 'lavanda'];
    await evaluate(`goto(0)`);
    for (const t of themes) {
      await evaluate(`selectTheme('${t}')`);
      await sleep(350);
      await captureScreen(`theme_${t}.png`);
    }

    // 3. Capturar fotogramas clave de la Transición Atmosférica
    console.log('\n--- Capturando Transición Atmosférica (HTML) ---');
    await evaluate(`selectTheme('noir'); goto(7);`);
    await sleep(300);
    await evaluate(`startTransition();`);
    
    // t ~ 1000ms: Resplandor cromático y respiración
    await sleep(1000);
    await captureScreen('transition_01_glow.png');

    // t ~ 3500ms: Segundo mensaje contemplativo
    await sleep(2500);
    await captureScreen('transition_02_message_2.png');

    // t ~ 7800ms: Cuarto mensaje contemplativo
    await sleep(4300);
    await captureScreen('transition_03_message_4.png');

    // t ~ 10850ms: Vuelo del logo hacia cabecera (~590ms)
    await sleep(3100);
    await captureScreen('transition_04_flight_header.png');

    console.log('\n✅ Todas las capturas del HTML de referencia han sido guardadas en visual-qa/evidence/html/\n');
    ws.close();
  } finally {
    edgeProc.kill();
  }
}

run().catch(err => {
  console.error('Error running capture_html:', err);
  edgeProc.kill();
  process.exit(1);
});
