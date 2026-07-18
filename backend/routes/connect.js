const express = require('express');
const router = express.Router();
const connectionController = require('../controllers/connectionController');
const { validateConnectionInput } = require('../utils/validator');

router.post('/', validateConnectionInput, connectionController.connect);
router.post('/disconnect', connectionController.disconnect);
router.post('/ping', connectionController.pingDevice);

module.exports = router;
