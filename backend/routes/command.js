const express = require('express');
const router = express.Router();
const commandController = require('../controllers/commandController');
const { validateCommandInput } = require('../utils/validator');

router.post('/', validateCommandInput, commandController.executeCommand);

module.exports = router;
