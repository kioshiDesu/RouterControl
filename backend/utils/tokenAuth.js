const logger = require('./logger');

/**
 * Express middleware to validate startup-generated Local API Token.
 * Restricts access to localhost clients with matching header.
 */
module.exports = (apiToken) => {
  return (req, res, next) => {
    const requestToken = req.headers['x-app-token'];

    if (!requestToken || requestToken !== apiToken) {
      logger.warn(`Unauthorized request rejected from origin ${req.ip}`);
      return res.status(401).json({
        error: true,
        message: 'Unauthorized. Invalid or missing local API token.',
        code: 'UNAUTHORIZED_ACCESS'
      });
    }

    next();
  };
};
