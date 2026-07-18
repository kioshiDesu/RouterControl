/**
 * Simple validation and sanitization helper.
 * Validates device details, port range and command structures.
 */
const validateConnectionInput = (req, res, next) => {
  const { host, port, username } = req.body;

  if (!host || typeof host !== 'string') {
    return res.status(400).json({ error: true, message: 'Host is required and must be a string', code: 'INVALID_HOST' });
  }

  if (port && (typeof port !== 'number' || port < 1 || port > 65535)) {
    return res.status(400).json({ error: true, message: 'Port must be a valid number between 1 and 65535', code: 'INVALID_PORT' });
  }

  if (!username || typeof username !== 'string') {
    return res.status(400).json({ error: true, message: 'Username is required and must be a string', code: 'INVALID_USERNAME' });
  }

  next();
};

const validateCommandInput = (req, res, next) => {
  const { command } = req.body;

  if (!command || typeof command !== 'string' || command.trim().length === 0) {
    return res.status(400).json({ error: true, message: 'Command is required and must be a non-empty string', code: 'INVALID_COMMAND' });
  }

  next();
};

module.exports = {
  validateConnectionInput,
  validateCommandInput
};
