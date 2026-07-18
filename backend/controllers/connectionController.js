const connectionManager = require('../services/mikrotikClient');
const logger = require('../utils/logger');
const net = require('net');

const connect = async (req, res, next) => {
  try {
    const { id, name, host, port, username, password, isDemo } = req.body;

    logger.info(`Incoming connection request for ${username}@${host}:${port || 22}`);
    
    const result = await connectionManager.connectDevice({
      id,
      name,
      host,
      port,
      username,
      password,
      isDemo
    });

    res.status(200).json({
      error: false,
      message: result.message
    });
  } catch (error) {
    next(error);
  }
};

const disconnect = async (req, res, next) => {
  try {
    const { id } = req.body;
    
    const wasDisconnected = connectionManager.disconnectDevice(id);
    
    if (wasDisconnected) {
      res.status(200).json({ error: false, message: 'Successfully disconnected from router.' });
    } else {
      res.status(404).json({ error: true, message: 'No active session found for specified device ID.', code: 'SESSION_NOT_FOUND' });
    }
  } catch (error) {
    next(error);
  }
};

const pingDevice = async (req, res, next) => {
  try {
    const { host, port, isDemo, connectionType } = req.body;
    if (!host) {
      return res.status(400).json({ error: true, message: 'Host is required' });
    }

    if (isDemo) {
      // Demo is always alive!
      return res.status(200).json({
        error: false,
        online: true,
        latency: Math.floor(Math.random() * 15) + 5,
        method: 'demo'
      });
    }

    // Pure L2TP Tunnel Check (UDP 1701), completely bypassing ICMP and SSH
    if (connectionType === 'L2TP Tunnel') {
      const dgram = require('dgram');
      const startTime = Date.now();
      
      const udpPromise = new Promise((resolve) => {
        const client = dgram.createSocket('udp4');
        let resolved = false;

        client.on('error', () => {
          if (!resolved) {
            resolved = true;
            client.close();
            resolve({ online: false });
          }
        });
        
        // L2TP Control Connection Start packet payload
        const message = Buffer.from([0xc8, 0x02, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]);
        client.send(message, 0, message.length, 1701, host, (err) => {
          if (err) {
            if (!resolved) {
              resolved = true;
              client.close();
              resolve({ online: false });
            }
          } else {
            // L2TP port is UDP, wait briefly to ensure no Destination Unreachable ICMP error is thrown by the OS
            setTimeout(() => {
              if (!resolved) {
                resolved = true;
                client.close();
                resolve({ online: true });
              }
            }, 300);
          }
        });
      });

      const result = await udpPromise;
      const latency = Date.now() - startTime;
      return res.status(200).json({
        error: false,
        online: result.online,
        latency,
        method: 'l2tp_udp_1701'
      });
    }

    const targetPort = parseInt(port) || 22;
    const startTime = Date.now();

    // TCP connect check (very reliable and fast for SSH ports)
    const socketPromise = new Promise((resolve) => {
      const socket = new net.Socket();
      socket.setTimeout(1500);

      socket.on('connect', () => {
        socket.destroy();
        resolve({ online: true, type: 'tcp' });
      });

      socket.on('timeout', () => {
        socket.destroy();
        resolve({ online: false });
      });

      socket.on('error', (err) => {
        socket.destroy();
        if (err.code === 'ECONNREFUSED') {
          // If port is closed but refused, host is still up and responding
          resolve({ online: true, type: 'tcp_refused' });
        } else {
          resolve({ online: false });
        }
      });

      socket.connect(targetPort, host);
    });

    // ICMP Ping check as fallback
    const pingPromise = new Promise((resolve) => {
      const { exec } = require('child_process');
      exec(`ping -c 1 -W 2 ${host}`, (err) => {
        if (!err) {
          resolve({ online: true, type: 'icmp' });
        } else {
          resolve({ online: false });
        }
      });
    });

    const results = await Promise.all([socketPromise, pingPromise]);
    const onlineResult = results.find(r => r.online);
    const latency = Date.now() - startTime;

    if (onlineResult) {
      res.status(200).json({
        error: false,
        online: true,
        latency,
        method: onlineResult.type
      });
    } else {
      res.status(200).json({
        error: false,
        online: false,
        message: 'Host unreachable'
      });
    }
  } catch (error) {
    next(error);
  }
};

module.exports = {
  connect,
  disconnect,
  pingDevice
};

