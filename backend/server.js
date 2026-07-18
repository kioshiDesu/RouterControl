const express = require('express');
const cors = require('cors');
const dotenv = require('dotenv');
const { crypto } = require('crypto');
const logger = require('./utils/logger');
const tokenAuth = require('./utils/tokenAuth');
const errorHandler = require('./utils/errorHandler');

// Load environment variables
dotenv.config();

const app = express();
const port = process.env.PORT || 3000;

// Generate secure Local API token for this runtime session
const localApiToken = require('crypto').randomBytes(16).toString('hex');
logger.info(`----------------------------------------`);
logger.info(`Session Token generated: ${localApiToken}`);
logger.info(`Use headers['x-app-token'] = "${localApiToken}" to query localhost`);
logger.info(`----------------------------------------`);

// Middleware
app.use(cors());
app.use(express.json());

// Token authorization protection on all operational APIs
app.use('/api', tokenAuth(localApiToken));

// Register Router Controllers
const connectRoutes = require('./routes/connect');
const commandRoutes = require('./routes/command');

app.use('/api/connect', connectRoutes);
app.use('/api/command', commandRoutes);

// Health-ping route (WebView checks if node.js is alive)
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'OK', token: localApiToken });
});

// Central error handler
app.use(errorHandler);

// Create HTTP server wrapper around Express
const http = require('http');
const { WebSocketServer } = require('ws');
const url = require('url');
const connectionManager = require('./services/mikrotikClient');

const server = http.createServer(app);
const wss = new WebSocketServer({ noServer: true });

// Handle WebSocket upgrade manually to perform local authentication check
server.on('upgrade', (request, socket, head) => {
  const parsedUrl = url.parse(request.url, true);
  const pathname = parsedUrl.pathname;

  if (pathname === '/api/terminal-ws') {
    const token = parsedUrl.query.token;
    const deviceId = parsedUrl.query.deviceId;

    if (token !== localApiToken) {
      logger.error('WebSocket Upgrade Failed: Unauthorized token');
      socket.write('HTTP/1.1 401 Unauthorized\r\n\r\n');
      socket.destroy();
      return;
    }

    if (!deviceId) {
      logger.error('WebSocket Upgrade Failed: Missing deviceId');
      socket.write('HTTP/1.1 400 Bad Request\r\n\r\n');
      socket.destroy();
      return;
    }

    wss.handleUpgrade(request, socket, head, (ws) => {
      wss.emit('connection', ws, request, deviceId);
    });
  } else {
    socket.destroy();
  }
});

// Handle active WebSocket terminal connections
wss.on('connection', (ws, request, deviceId) => {
  logger.info(`WebSocket Terminal session established for device: ${deviceId}`);

  ws.on('message', (message) => {
    try {
      const data = JSON.parse(message);
      const { command } = data;

      if (!command) {
        ws.send(JSON.stringify({ type: 'error', data: 'No command string specified' }));
        return;
      }

      logger.info(`WS Command executing on device [${deviceId}]: "${command}"`);

      // Stream command outputs in real-time
      connectionManager.runCommandStream(
        deviceId,
        command,
        (chunk) => {
          ws.send(JSON.stringify({ type: 'chunk', data: chunk }));
        },
        (err) => {
          ws.send(JSON.stringify({ type: 'error', data: err.message }));
        },
        (code) => {
          ws.send(JSON.stringify({ type: 'close', code }));
        }
      );
    } catch (err) {
      logger.error('WebSocket message parsing failed:', err);
      ws.send(JSON.stringify({ type: 'error', data: 'Invalid message structure' }));
    }
  });

  ws.on('close', () => {
    logger.info(`WebSocket Terminal session disconnected for device: ${deviceId}`);
  });
});

server.listen(port, '127.0.0.1', () => {
  logger.info(`RouterControl backend running on http://127.0.0.1:${port}`);
});

module.exports = app;
