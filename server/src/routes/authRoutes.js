const express = require("express");
const jwt = require("jsonwebtoken");
const { registerUser, loginUser, verifyAdminCredentials } = require("../store-mysql");

const JWT_SECRET = process.env.JWT_SECRET || "bof-dev-secret-2026";
const router = express.Router();

router.post("/auth/register", async (req, res) => {
  try {
    const result = await registerUser(req.body || {});
    res.status(201).json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.post("/auth/login", async (req, res) => {
  try {
    const user = await loginUser(req.body || {});
    const token = jwt.sign(
      { userId: user.userId, email: user.email, fullName: user.fullName, isAdmin: Boolean(user.isAdmin) },
      JWT_SECRET,
      { expiresIn: "8h" }
    );
    res.json({
      token,
      fullName: user.fullName,
      userId: user.userId,
      customerId: user.customerId,
      isAdmin: Boolean(user.isAdmin),
    });
  } catch (err) {
    res.status(401).json({ error: err.message });
  }
});

router.post("/auth/admin-verify", async (req, res) => {
  try {
    const result = await verifyAdminCredentials(req.body || {});
    res.json(result);
  } catch (err) {
    res.status(401).json({ error: err.message });
  }
});

module.exports = router;
