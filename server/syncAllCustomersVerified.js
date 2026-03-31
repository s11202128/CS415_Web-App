// Usage: node syncAllCustomersVerified.js
// This script sets all customers to verified, approved, and active for admin monitoring consistency.
const sequelize = require('./src/config/database');
const { Customer } = require('./src/models');

async function syncAllCustomers() {
  try {
    await sequelize.authenticate();
    const [affectedRows] = await Customer.update(
      {
        emailVerified: true,
        isVerified: true,
        identityVerified: true,
        registrationStatus: 'approved',
        status: 'active',
      },
      { where: {} }
    );
    console.log(`Updated ${affectedRows} customers to verified, approved, and active.`);
  } catch (err) {
    console.error('Error updating customers:', err);
  } finally {
    await sequelize.close();
  }
}

syncAllCustomers();
