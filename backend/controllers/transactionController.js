const { saveOTP, getOTP, deleteOTP } = require('../models/otpModel');
const sendEmail = require('../utils/sendEmail');
const generateOTP = require('../utils/generateOTP');

/**
 * Request transaction OTP
 */
exports.requestTransactionOTP = async (req, res) => {
  try {
    const email = req.user.email;
    const otp = generateOTP();
    const expiresAt = new Date(Date.now() + 3 * 60 * 1000); // 3 min

    await saveOTP(email, otp, 'transaction', expiresAt);

    await sendEmail(
      email,
      'Transaction OTP',
      `<p>Your transaction OTP is: <b>${otp}</b>. It expires in 3 minutes.</p>`
    );

    res.json({ message: 'Transaction OTP sent to email' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: 'Failed to send transaction OTP' });
  }
};

/**
 * Confirm transaction
 */
exports.confirmTransaction = async (req, res) => {
  try {
    const email = req.user.email;
    const { otp } = req.body;
    if (!otp)
      return res.status(400).json({ message: 'OTP required' });

    const otpRecord = await getOTP(email, 'transaction');
    if (!otpRecord)
      return res.status(400).json({ message: 'No OTP found. Please request again.' });

    if (otpRecord.code !== otp)
      return res.status(400).json({ message: 'Invalid OTP' });

    if (new Date() > otpRecord.expires_at)
      return res.status(400).json({ message: 'OTP expired' });

    await deleteOTP(otpRecord.id);

    // Simulate transaction success
    res.json({ message: 'Transaction confirmed successfully' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: 'Transaction confirmation failed' });
  }
};
