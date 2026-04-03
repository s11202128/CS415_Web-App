const jwt = require("jsonwebtoken");
const authService = require("../services/authService");

const JWT_SECRET = process.env.JWT_SECRET || "bof-dev-secret-2026";

function buildAuthResponse(user) {
  const token = jwt.sign(
    {
      userId: user.userId,
      customerId: user.customerId,
      email: user.email,
      fullName: user.fullName,
      isAdmin: Boolean(user.isAdmin),
    },
    JWT_SECRET,
    { expiresIn: "8h" }
  );

  return {
    token,
    fullName: user.fullName,
    userId: user.userId,
    customerId: user.customerId,
    email: user.email,
    mobile: user.mobile,
    nationalId: user.nationalId,
    isAdmin: Boolean(user.isAdmin),
  };
}

/**
 * Auth HTTP controller layer.
 * Handles request/response mapping and delegates business rules to authService.
 */
const authController = {
  /**
   * Register endpoint handler.
   * @param {import('express').Request} req Express request
   * @param {import('express').Response} res Express response
   * @returns {Promise<void>}
   */
  async register(req, res) {
    const result = await authService.register(req.body || {});
    res.status(201).json(result);
  },

  /**
   * Login endpoint handler.
   * @param {import('express').Request} req Express request
   * @param {import('express').Response} res Express response
   * @returns {Promise<void>}
   */
  async login(req, res) {
    const user = await authService.login({
      ...(req.body || {}),
      ipAddress: req.ip,
      userAgent: req.get("user-agent") || "",
    });
    res.json(buildAuthResponse(user));
  },

  /**
   * Forgot password endpoint handler.
   * @param {import('express').Request} req Express request
   * @param {import('express').Response} res Express response
   * @returns {Promise<void>}
   */
  async forgotPassword(req, res) {
    const result = await authService.forgotPassword(req.body || {});
    res.json(result);
  },

  /**
   * Reset password endpoint handler.
   * @param {import('express').Request} req Express request
   * @param {import('express').Response} res Express response
   * @returns {Promise<void>}
   */
  async resetPassword(req, res) {
    const result = await authService.resetPassword(req.body || {});
    res.json(result);
  },

  /**
   * Verify email using token.
   * @param {import('express').Request} req Express request
   * @param {import('express').Response} res Express response
   * @returns {Promise<void>}
   */
  async verifyEmail(req, res) {
    const result = await authService.verifyEmail({ token: req.params.token });
    res.json(result);
  },

  /**
   * Resend verification email.
   * @param {import('express').Request} req Express request
   * @param {import('express').Response} res Express response
   * @returns {Promise<void>}
   */
  async resendVerification(req, res) {
    const result = await authService.resendVerification(req.body || {});
    res.json(result);
  },

  /**
   * Admin credential verification endpoint handler.
   * @param {import('express').Request} req Express request
   * @param {import('express').Response} res Express response
   * @returns {Promise<void>}
   */
  async verifyAdmin(req, res) {
    const result = await authService.verifyAdmin(req.body || {});
    res.json(result);
  },
};

module.exports = authController;