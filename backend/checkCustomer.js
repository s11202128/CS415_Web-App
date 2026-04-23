// Usage: node checkCustomer.js <email>
const sequelize = require('./src/config/database');
const { Customer } = require('./src/models');

async function checkCustomer(email) {
  if (!email) {
    console.error('Usage: node checkCustomer.js <email>');
    process.exit(1);
  }
  try {
    await sequelize.authenticate();
    const customer = await Customer.findOne({ where: { email } });
    if (!customer) {
      console.log('No customer found for email:', email);
      process.exit(0);
    }
    const fields = [
      'id', 'fullName', 'email', 'mobile', 'password',
      'emailVerified', 'isVerified', 'status',
      'lockedUntil', 'failedLoginAttempts', 'registrationStatus'
    ];
    const result = {};
    for (const field of fields) {
      result[field] = customer[field];
    }
    console.log('Customer record:', result);
  } catch (err) {
    console.error('Error querying customer:', err);
  } finally {
    await sequelize.close();
  }
}

checkCustomer(process.argv[2]);
