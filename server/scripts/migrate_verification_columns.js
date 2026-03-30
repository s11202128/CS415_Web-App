/*
 * Migration: add email verification columns to customers table.
 * Usage:
 *   DB_HOST=localhost DB_USER=root DB_PASSWORD=secret DB_NAME=bof_banking_db \
 *   node scripts/migrate_verification_columns.js
 */
const { sequelize } = require('../src/models');

async function run() {
  console.log('Running migration: add verification columns to customers');
  const addColumnIfMissing = async (column, sql) => {
    const [cols] = await sequelize.query(
      `SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = '${column}'`
    );
    if (Array.isArray(cols) && cols.length > 0) {
      console.log(`Column ${column} already exists; skipping`);
      return;
    }
    await sequelize.query(sql);
    console.log(`OK: ${sql}`);
  };

  // Rename legacy misspelling if present (MySQL CHANGE COLUMN lacks IF EXISTS)
  const [columns] = await sequelize.query(
    "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'is_varified'"
  );
  if (Array.isArray(columns) && columns.length > 0) {
    const renameSql =
      "ALTER TABLE customers CHANGE COLUMN is_varified is_verified TINYINT(1) NOT NULL DEFAULT 0;";
    await sequelize.query(renameSql);
    console.log(`OK: ${renameSql}`);
  } else {
    console.log('Legacy column is_varified not found; no rename needed');
  }

  await addColumnIfMissing(
    'is_verified',
    "ALTER TABLE customers ADD COLUMN is_verified TINYINT(1) NOT NULL DEFAULT 0 AFTER emailVerified;"
  );

  await addColumnIfMissing(
    'verification_token',
    "ALTER TABLE customers ADD COLUMN verification_token VARCHAR(255) NULL AFTER is_verified;"
  );

  await addColumnIfMissing(
    'verification_token_expires',
    "ALTER TABLE customers ADD COLUMN verification_token_expires DATETIME NULL AFTER verification_token;"
  );

  // Optional backfill from legacy column if it exists
  const [legacyExpiry] = await sequelize.query(
    "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customers' AND COLUMN_NAME = 'token_expiry'"
  );
  if (Array.isArray(legacyExpiry) && legacyExpiry.length > 0) {
    await sequelize.query(
      "UPDATE customers SET verification_token_expires = token_expiry WHERE verification_token_expires IS NULL AND token_expiry IS NOT NULL;"
    );
    console.log('OK: backfill verification_token_expires from token_expiry');
  } else {
    console.log('Legacy column token_expiry not found; skipping backfill');
  }

  await sequelize.close();
  console.log('Migration completed');
}

run().catch((err) => {
  console.error('Migration failed:', err.message);
  process.exit(1);
});
