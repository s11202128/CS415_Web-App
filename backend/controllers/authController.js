const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const { findUserByEmail, createUser, verifyUser } = require('../models/userModel');
const { saveOTP, getOTP, deleteOTP } = require('../models/otpModel');
const sendEmail = require('../utils/sendEmail');
const generateOTP = require('../utils/generateOTP');
require('dotenv').config();

/**
 * Signup controller
 */
exports.signup = async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password)
      return res.status(400).json({ message: 'Email and password required' });

    const existingUser = await findUserByEmail(email);
    if (existingUser)
      return res.status(400).json({ message: 'Email already registered' });

    const hashedPassword = await bcrypt.hash(password, 10);
    await createUser(email, hashedPassword);

    // Generate OTP
    const otp = generateOTP();
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 min

    await saveOTP(email, otp, 'signup', expiresAt);

    // Send OTP email
    await sendEmail(
      email,
      'Verify your email',
      `<p>Your signup OTP is: <b>${otp}</b>. It expires in 5 minutes.</p>`
    );

    res.status(201).json({ message: 'Signup successful, OTP sent to email' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: 'Signup failed' });
  }
};

/**
 * Verify signup OTP
 */
exports.verifySignup = async (req, res) => {
  try {
    const { email, otp } = req.body;
    if (!email || !otp)
      return res.status(400).json({ message: 'Email and OTP required' });

    const otpRecord = await getOTP(email, 'signup');
    if (!otpRecord)
      return res.status(400).json({ message: 'No OTP found. Please signup again.' });

    if (otpRecord.code !== otp)
      return res.status(400).json({ message: 'Invalid OTP' });

    if (new Date() > otpRecord.expires_at)
      return res.status(400).json({ message: 'OTP expired' });

    await verifyUser(email);
    await deleteOTP(otpRecord.id);

    res.json({ message: 'Email verified successfully' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: 'Verification failed' });
  }
};

/**
 * Login controller
 */
exports.login = async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password)
      return res.status(400).json({ message: 'Email and password required' });

    const user = await findUserByEmail(email);
    if (!user)
      return res.status(400).json({ message: 'User not found' });

    if (!user.is_verified)
      return res.status(400).json({ message: 'Email not verified' });

    const valid = await bcrypt.compare(password, user.password);
    if (!valid)
      return res.status(400).json({ message: 'Invalid password' });

    // Generate OTP
    const otp = generateOTP();
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 min

    await saveOTP(email, otp, 'login', expiresAt);

    // Send OTP email
    await sendEmail(
      email,
      'Login OTP',
      `<p>Your login OTP is: <b>${otp}</b>. It expires in 5 minutes.</p>`
    );

    res.json({ message: 'Login OTP sent to email' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: 'Login failed' });
  }
};

/**
 * Verify login OTP
 */
exports.verifyLogin = async (req, res) => {
  try {
    const { email, otp } = req.body;
    if (!email || !otp)
      return res.status(400).json({ message: 'Email and OTP required' });

    const otpRecord = await getOTP(email, 'login');
    if (!otpRecord)
      return res.status(400).json({ message: 'No OTP found. Please login again.' });

    if (otpRecord.code !== otp)
      return res.status(400).json({ message: 'Invalid OTP' });

    if (new Date() > otpRecord.expires_at)
      return res.status(400).json({ message: 'OTP expired' });

    // Generate JWT
    const token = jwt.sign({ email }, process.env.JWT_SECRET, { expiresIn: '1h' });

    await deleteOTP(otpRecord.id);

    res.json({ message: 'Login successful', token });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: 'OTP verification failed' });
  }
};
