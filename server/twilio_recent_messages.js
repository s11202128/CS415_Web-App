const fs = require('fs');
if (fs.existsSync('.env')) {
  const content = fs.readFileSync('.env', 'utf8');
  content.split(/\r?\n/).forEach((line) => {
    const m = line.match(/^\s*([A-Z0-9_]+)=(.*)$/);
    if (!m) return;
    const key = m[1];
    const value = m[2].replace(/^"|"$/g, '');
    if (!process.env[key]) process.env[key] = value;
  });
}
const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
(async () => {
  const endpoint = `https://api.twilio.com/2010-04-01/Accounts/${accountSid}/Messages.json?PageSize=20&To=%2B6795023149`;
  const response = await fetch(endpoint, {
    headers: {
      Authorization: `Basic ${Buffer.from(`${accountSid}:${authToken}`).toString('base64')}`,
    },
  });
  const data = await response.json();
  if (!response.ok) {
    console.log('HTTP', response.status, JSON.stringify(data));
    process.exit(1);
  }
  const rows = (data.messages || []).map((m) => ({
    sid: m.sid,
    status: m.status,
    date_created: m.date_created,
    date_sent: m.date_sent,
    to: m.to,
    from: m.from,
    error_code: m.error_code,
    body: (m.body || '').slice(0, 80)
  }));
  console.log(JSON.stringify(rows, null, 2));
})();
