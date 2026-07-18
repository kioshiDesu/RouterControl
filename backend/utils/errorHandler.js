const logger = require('./logger');

/**
 * Standard centralized express error handler.
 * Mandated response: { error: true, message, code }
 */
const errorHandler = (err, req, res, next) => {
  logger.error(err);
  
  const statusCode = err.status || 500;
  res.status(statusCode).json({
    error: true,
    message: err.message || 'An unexpected server error occurred.',
    code: err.code || 'INTERNAL_SERVER_ERROR'
  });
};

module.exports = errorHandler;
