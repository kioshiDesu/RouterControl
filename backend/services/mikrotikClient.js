const { Client } = require('ssh2');
const logger = require('../utils/logger');

class ConnectionManager {
  constructor() {
    this.activeSessions = new Map(); // Keyed by device ID
    this.maxSessions = 5;
  }

  /**
   * Establish an SSH session and store it in-memory.
   */
  async connectDevice(device) {
    const { id, host, port, username, password, isDemo } = device;

    if (isDemo) {
      logger.info(`Simulated: Connected to Demo Router (id: ${id})`);
      this.activeSessions.set(id, { isDemo: true, username, connectedAt: new Date() });
      return { success: true, message: 'Connected to Demo Router (Simulated)' };
    }

    if (this.activeSessions.size >= this.maxSessions) {
      // Evict oldest session to stay below concurrent session threshold
      const oldestId = this.activeSessions.keys().next().value;
      this.disconnectDevice(oldestId);
      logger.info(`Session threshold exceeded. Evicted oldest session: ${oldestId}`);
    }

    return new Promise((resolve, reject) => {
      const conn = new Client();
      
      conn.on('ready', () => {
        logger.info(`SSH Connection established with ${username}@${host}:${port}`);
        this.activeSessions.set(id, {
          client: conn,
          isDemo: false,
          username,
          connectedAt: new Date()
        });
        resolve({ success: true, message: 'Successfully established SSH session.' });
      });

      conn.on('error', (err) => {
        logger.error(`SSH Connection Error [${host}]:`, err);
        reject(err);
      });

      conn.on('end', () => {
        logger.info(`SSH Connection ended for ${host}`);
        this.activeSessions.delete(id);
      });

      conn.connect({
        host,
        port: port || 22,
        username,
        password,
        readyTimeout: 8000 // 8 seconds timeout
      });
    });
  }

  /**
   * Disconnect an active session.
   */
  disconnectDevice(id) {
    const session = this.activeSessions.get(id);
    if (!session) return false;

    if (!session.isDemo && session.client) {
      session.client.end();
    }
    this.activeSessions.delete(id);
    logger.info(`Session closed for device ID: ${id}`);
    return true;
  }

  /**
   * Execute CLI command on specified active session.
   */
  async runCommand(id, command) {
    const session = this.activeSessions.get(id);
    if (!session) {
      throw new Error('Device is not connected. Please establish a session first.');
    }

    if (session.isDemo) {
      return this._getMockOutput(command);
    }

    return new Promise((resolve, reject) => {
      session.client.exec(command, (err, stream) => {
        if (err) return reject(err);

        let stdout = '';
        let stderr = '';

        stream.on('close', (code, signal) => {
          if (code === 0) {
            resolve(stdout || 'Command executed with no output.');
          } else {
            reject(new Error(stderr || `Process exited with code ${code}`));
          }
        });

        stream.on('data', (data) => {
          stdout += data.toString();
        });

        stream.stderr.on('data', (data) => {
          stderr += data.toString();
        });
      });
    });
  }

  /**
   * Stream command execution chunk-by-chunk for live WebSocket terminal feel.
   */
  runCommandStream(id, command, onChunk, onError, onClose) {
    const session = this.activeSessions.get(id);
    if (!session) {
      onError(new Error('Device is not connected. Please establish a session first.'));
      return;
    }

    if (session.isDemo) {
      const output = this._getMockOutput(command);
      const chunks = output.split('\n');
      let i = 0;
      const sendNextChunk = () => {
        if (i < chunks.length) {
          const suffix = i === chunks.length - 1 ? '' : '\n';
          onChunk(chunks[i] + suffix);
          i++;
          setTimeout(sendNextChunk, 150);
        } else {
          onClose(0);
        }
      };
      setTimeout(sendNextChunk, 50);
      return;
    }

    session.client.exec(command, (err, stream) => {
      if (err) {
        onError(err);
        return;
      }

      stream.on('data', (data) => {
        onChunk(data.toString());
      });

      stream.stderr.on('data', (data) => {
        onChunk(data.toString());
      });

      stream.on('close', (code, signal) => {
        onClose(code);
      });
    });
  }

  _getMockOutput(command) {
    const cmd = command.trim().toLowerCase();
    if (cmd.includes('ip address print')) {
      return `Flags: X - disabled, I - invalid, D - dynamic \n #   ADDRESS            NETWORK         INTERFACE\n 0   192.168.88.1/24    192.168.88.0    bridge\n 1 D 10.0.0.145/24      10.0.0.0        ether1`;
    }
    if (cmd.includes('interface print')) {
      return `Flags: D - dynamic, X - disabled, R - running, S - slave \n #     NAME                                TYPE         ACTUAL-MTU L2MTU  MAX-L2MTU\n 0  R  ether1                              ether              1500  1598       4074\n 1  RS ether2                              ether              1500  1598       4074\n 5  R  bridge                              bridge             1500  1598`;
    }
    if (cmd.includes('system resource print')) {
      return `                   uptime: 4w2d5h12m43s\n                  version: 7.12.1 (stable)\n              free-memory: 184.2MiB\n             total-memory: 256.0MiB\n                      cpu: MIPS 24Kc V7.4\n                cpu-load: 8%\n             architecture: mipsbe\n               board-name: hEX lite`;
    }
    if (cmd.includes('system reboot')) {
      return `Rebooting system... Connection closed.\nRouter restarting...`;
    }
    if (cmd.includes('log print')) {
      return `18:42:01 system,info,account user admin logged in from 192.168.88.243 via winbox\n18:44:00 system,info,account user admin logged in from 10.0.0.12 via ssh`;
    }
    return `[admin@DemoRouter] > ${command}\nCommand executed successfully in simulation mode.`;
  }
}

module.exports = new ConnectionManager();
