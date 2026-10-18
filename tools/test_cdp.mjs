import { spawn } from 'child_process';

const EDGE_PATH = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const PORT = 9225;

console.log('Spawning Edge...');
const proc = spawn(EDGE_PATH, [
  '--headless=new',
  '--no-sandbox',
  '--disable-gpu',
  `--remote-debugging-port=${PORT}`,
  'about:blank'
]);

proc.stderr.on('data', d => console.log('Edge stderr:', d.toString()));
proc.stdout.on('data', d => console.log('Edge stdout:', d.toString()));

async function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function run() {
  await sleep(1500);
  try {
    const res = await fetch(`http://127.0.0.1:${PORT}/json`);
    const json = await res.json();
    console.log('Targets:', json);
    if (json[0]?.webSocketDebuggerUrl) {
      console.log('Connecting ws:', json[0].webSocketDebuggerUrl);
      const ws = new WebSocket(json[0].webSocketDebuggerUrl);
      ws.onopen = () => {
        console.log('WS OPEN!');
        ws.send(JSON.stringify({ id: 1, method: 'Browser.getVersion' }));
      };
      ws.onmessage = (evt) => {
        console.log('WS MESSAGE received:', typeof evt.data, evt.data.slice(0, 80));
        ws.close();
        proc.kill();
      };
      ws.onerror = (e) => console.error('WS ERROR:', e);
    }
  } catch (err) {
    console.error('Fetch error:', err);
    proc.kill();
  }
}

run();
