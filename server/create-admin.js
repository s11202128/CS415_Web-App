/**
 * Create a test admin account for development/testing
 * Usage: node create-admin.js
 */
try {
  require('dotenv').config();
} catch (e) {
  // dotenv is optional
}

const bcrypt = require('bcryptjs');
const sequelize = require('./src/config/database');
const Admin = require('./src/models/Admin');

const TEST_ADMIN = {
  fullName: 'Test Admin',
  email: 'admin@bankoffiji.com',
  password: 'password123', // Will be hashed
  role: 'super_admin',
  status: 'active'
};

async function createAdmin() {
  try {
    await sequelize.authenticate();
    console.log('✓ Database connection successful');

    // Check if admin already exists
    const existing = await Admin.findOne({ where: { email: TEST_ADMIN.email } });
    if (existing) {
      console.log(`✓ Admin already exists with email: ${TEST_ADMIN.email}`);
      console.log(`  ID: ${existing.id}`);
      console.log(`  Status: ${existing.status}`);
      process.exit(0);
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(TEST_ADMIN.password, 10);

    // Create admin
    const admin = await Admin.create({
      fullName: TEST_ADMIN.fullName,
      email: TEST_ADMIN.email,
      password: hashedPassword,
      role: TEST_ADMIN.role,
      status: TEST_ADMIN.status
    });

    console.log('✓ Test admin created successfully!');
    console.log(`  ID: ${admin.id}`);
    console.log(`  Email: ${admin.email}`);
    console.log(`  Full Name: ${admin.fullName}`);
    console.log(`  Role: ${admin.role}`);
    console.log(`  Status: ${admin.status}`);
    console.log('\nYou can now login with:');
    console.log(`  Email: ${TEST_ADMIN.email}`);
    console.log(`  Password: ${TEST_ADMIN.password}`);

    process.exit(0);
  } catch (error) {
    console.error('✗ Error creating admin:', error.message);
    process.exit(1);
  }
}

createAdmin();
