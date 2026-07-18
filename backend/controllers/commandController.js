const connectionManager = require('../services/mikrotikClient');
const logger = require('../utils/logger');

const executeCommand = async (req, res, next) => {
  try {
    const { deviceId, command } = req.body;

    logger.info(`Running command on device [${deviceId}]: "${command}"`);
    
    const output = await connectionManager.runCommand(deviceId, command);

    res.status(200).json({
      error: false,
      output
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  executeCommand
};
