/**
 * Neon WebSocket -> local TCP bridge.
 *
 * Why this exists
 * ---------------
 * Neon speaks the PostgreSQL wire protocol over plain TCP on port 5432. Some
 * networks (corporate firewalls, sandboxed CI, this dev machine) permit only
 * outbound TCP on 80/443, so a direct JDBC connection to Neon fails with
 * "Connection refused" even though the credentials and hostname are correct.
 *
 * Neon also exposes a WebSocket proxy on 443 at /v2, which relays raw bytes.
 * This script accepts local TCP connections and forwards their bytes over that
 * WebSocket, giving JDBC a normal socket to talk to.
 *
 * Usage:
 *   node tools/neon-ws-bridge.mjs
 *   NEON_HOST=... LISTEN_PORT=5433 node tools/neon-ws-bridge.mjs
 *
 * Then point the application at the local port, e.g. in backend/.env:
 *   DB_URL=jdbc:postgresql://127.0.0.1:5433/neondb?sslmode=disable
 *
 * sslmode=disable is correct here: the wss:// hop to Neon is already TLS
 * terminated by Neon, and the 127.0.0.1 hop never leaves the machine.
 *
 * Requires Node 22+ (global WebSocket). No npm dependencies.
 */
import net from 'node:net';

const NEON_HOST = process.env.NEON_HOST;
const NEON_WS = process.env.NEON_WS
  ?? `wss://${NEON_HOST}/v2?sslmode=require`;
const LISTEN_PORT = Number(process.env.LISTEN_PORT ?? 5433);
const PING_MS = Number(process.env.PING_MS ?? 30_000);

if (!NEON_HOST && !process.env.NEON_WS) {
  console.error('Set NEON_HOST (e.g. ep-xxx.c-6.us-east-2.aws.neon.tech).');
  process.exit(1);
}

let seq = 0;
const log = (...a) => console.log(`[bridge ${new Date().toISOString()}]`, ...a);

const server = net.createServer((socket) => {
  const id = ++seq;
  socket.setNoDelay(true);

  const ws = new WebSocket(NEON_WS);
  ws.binaryType = 'arraybuffer';

  let ready = false;
  let closed = false;
  const backlog = [];

  const flush = () => {
    while (ready && !closed && backlog.length) ws.send(backlog.shift());
  };

  // Keep intermediaries from idling the tunnel out. NOTE: the global
  // WebSocket in Node has no ping() (that is a `ws` package extension), so this
  // must stay optional -- calling it unconditionally crashes the process.
  const keepalive = setInterval(() => {
    try {
      if (typeof ws.ping === 'function' && ws.readyState === WebSocket.OPEN) ws.ping();
    } catch { /* keepalive is best-effort only */ }
  }, PING_MS);

  const teardown = (why) => {
    if (closed) return;
    closed = true;
    clearInterval(keepalive);
    try { ws.close(); } catch { /* already gone */ }
    socket.destroy();
    log(`conn#${id} closed (${why})`);
  };

  ws.onopen = () => {
    ready = true;
    log(`conn#${id} websocket open -> ${NEON_WS}`);
    flush();
  };

  // local TCP -> websocket
  socket.on('data', (chunk) => {
    if (ready && ws.readyState === WebSocket.OPEN) ws.send(chunk);
    else backlog.push(chunk);
  });

  // websocket -> local TCP
  ws.onmessage = (e) => socket.write(Buffer.from(e.data));

  ws.onerror = () => teardown('websocket error');
  ws.onclose = (e) => teardown(`websocket close ${e.code} ${e.reason || ''}`.trim());
  socket.on('error', () => teardown('socket error'));
});

server.listen(LISTEN_PORT, '127.0.0.1', () => {
  log(`listening on 127.0.0.1:${LISTEN_PORT} -> ${NEON_WS}`);
});

server.on('error', (e) => {
  log(`server error: ${e.message}`);
  process.exit(1);
});

// The bridge is a long-lived helper that the whole application depends on, so
// a single unexpected throw must never take it down.
process.on('uncaughtException', (e) => log(`uncaught (ignored): ${e?.message ?? e}`));
process.on('unhandledRejection', (e) => log(`unhandled rejection (ignored): ${e?.message ?? e}`));

for (const sig of ['SIGINT', 'SIGTERM']) {
  process.on(sig, () => {
    log(`${sig} received, shutting down`);
    server.close(() => process.exit(0));
  });
}