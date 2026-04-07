const twilio = require("twilio");

function normalizePhoneNumber(phoneNumber) {
  return String(phoneNumber || "").trim();
}

function sanitizeMessage(message) {
  return String(message || "").replace(/[\r\n]+/g, " ").trim();
}

function getTwilioClient() {
  const accountSid = process.env.TWILIO_ACCOUNT_SID;
  const authToken = process.env.TWILIO_AUTH_TOKEN;
  if (!accountSid || !authToken) {
    throw new Error("Twilio is not fully configured");
  }
  return twilio(accountSid, authToken);
}

async function sendViaTwilio({ to, message }) {
  const from = process.env.TWILIO_FROM_NUMBER;

  if (!from) {
    throw new Error('Twilio is not fully configured');
  }

  const client = getTwilioClient();
  const data = await client.messages.create({
    to,
    from,
    body: sanitizeMessage(message),
  });

  return {
    provider: "twilio",
    providerMessageId: data?.sid || null,
    status: data?.status || "queued",
  };
}

async function sendSms({ to, message }) {
  const normalizedTo = normalizePhoneNumber(to);
  if (!normalizedTo) {
    throw new Error("Recipient phone number is required");
  }

  const provider = String(process.env.SMS_PROVIDER || "twilio").toLowerCase();

  if (provider === "twilio") {
    return sendViaTwilio({ to: normalizedTo, message: sanitizeMessage(message) });
  }

  console.log(`[SMS:mock] to=${normalizedTo} message=${sanitizeMessage(message)}`);
  return {
    provider: "mock",
    providerMessageId: null,
    status: "simulated",
  };
}

async function sendSMS(toPhoneNumber, message) {
  return sendSms({ to: toPhoneNumber, message });
}

module.exports = {
  sendSms,
  sendSMS,
};
