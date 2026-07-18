const pino = require('pino');

/**
 * Configure lightweight, high-performance Pino logger with pretty logging for embedded CLI outputs.
 */
const logger = pino({
  level: process.env.LOG_LEVEL || 'info',
  transport: {
    target: 'pino-pretty',
    options: {
      colorize: true,
      translateTime: 'SYS:standard',
      ignore: 'pid,hostname'
    }
  }
});

module.exports = logger;
