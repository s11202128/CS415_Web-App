const express = require("express");
const authController = require("../controllers/authController");

const router = express.Router();

const withStatus = (statusCode, handler) => async (req, res) => {
  try {
    await handler(req, res);
  } catch (err) {
    const resolvedStatus = Number(err?.statusCode) || statusCode;
    res.status(resolvedStatus).json({ error: err.message });
  }
};

router.post("/auth/register", withStatus(400, authController.register));
router.post("/register", withStatus(400, authController.register));

router.post("/auth/login", withStatus(401, authController.login));
router.post("/login", withStatus(401, authController.login));

router.post("/auth/forgot-password", withStatus(400, authController.forgotPassword));
router.post("/auth/reset-password", withStatus(400, authController.resetPassword));

router.get("/auth/verify/:token", withStatus(400, authController.verifyEmail));
router.post("/auth/resend-verification", withStatus(400, authController.resendVerification));

router.post("/auth/admin-verify", withStatus(401, authController.verifyAdmin));

module.exports = router;
