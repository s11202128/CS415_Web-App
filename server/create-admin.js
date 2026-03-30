/**
 * Reset or create an admin account for development/testing.
 * Usage:
 *   node create-admin.js
 *   ADMIN_EMAIL=admin@bof.fj ADMIN_PASSWORD=secret node create-admin.js
 */
try {
  require('dotenv').config();
} catch (e) {
  // dotenv is optional
}

const bcrypt = require('bcryptjs');
const sequelize = require('./src/config/database');
const Admin = require('./src/models/Admin');

const ADMIN_EMAIL = String(process.env.ADMIN_EMAIL || 'admin@bof.fj').toLowerCase();
const ADMIN_PASSWORD = String(process.env.ADMIN_PASSWORD || 'admin12345');

async function upsertAdmin() {
  try {
    await sequelize.authenticate();
    console.log('✓ Database connection successful');

    const hashedPassword = await bcrypt.hash(ADMIN_PASSWORD, 10);
    const existing = await Admin.findOne({ where: { email: ADMIN_EMAIL } });

    if (existing) {
      await existing.update({
        password: hashedPassword,
        status: 'active',
        role: existing.role || 'super_admin',
      });
      console.log(`✓ Admin password reset and reactivated for ${ADMIN_EMAIL}`);
      console.log(`  ID: ${existing.id}`);
    } else {
      const admin = await Admin.create({
        fullName: 'System Admin',
        email: ADMIN_EMAIL,
        password: hashedPassword,
        role: 'super_admin',
        status: 'active',
      });
      console.log(`✓ Admin created: ${admin.email} (id=${admin.id})`);
    }

    console.log('\nLogin with:');
    console.log(`  Email: ${ADMIN_EMAIL}`);
    console.log(`  Password: ${ADMIN_PASSWORD}`);
    process.exit(0);
  } catch (error) {
    console.error('✗ Error creating/resetting admin:', error.message);
    process.exit(1);
  }
}

upsertAdmin();
