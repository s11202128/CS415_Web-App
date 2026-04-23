const sequelize = require('./config/database');

async function run() {
  try {
    await sequelize.authenticate();

    const [fks] = await sequelize.query(`
      SELECT
        TABLE_NAME,
        COLUMN_NAME,
        REFERENCED_TABLE_NAME,
        REFERENCED_COLUMN_NAME,
        CONSTRAINT_NAME
      FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
      WHERE TABLE_SCHEMA = DATABASE()
        AND REFERENCED_TABLE_NAME IS NOT NULL
      ORDER BY TABLE_NAME, COLUMN_NAME
    `);

    console.log('Foreign keys found:', fks.length);
    console.table(fks);

    const checks = [
      {
        name: 'transactions.accountId -> accounts.id',
        sql: `
          SELECT COUNT(*) AS orphans
          FROM transactions t
          LEFT JOIN accounts a ON a.id = t.accountId
          WHERE t.accountId IS NOT NULL AND a.id IS NULL
        `,
      },
      {
        name: 'statement_requests.accountId -> accounts.id',
        sql: `
          SELECT COUNT(*) AS orphans
          FROM statement_requests sr
          LEFT JOIN accounts a ON a.id = sr.accountId
          WHERE sr.accountId IS NOT NULL AND a.id IS NULL
        `,
      },
      {
        name: 'statement_requests.reviewedByAdminId -> admins.id',
        sql: `
          SELECT COUNT(*) AS orphans
          FROM statement_requests sr
          LEFT JOIN admins a ON a.id = sr.reviewedByAdminId
          WHERE sr.reviewedByAdminId IS NOT NULL AND a.id IS NULL
        `,
      },
      {
        name: 'login_logs.customerId -> customers.id',
        sql: `
          SELECT COUNT(*) AS orphans
          FROM login_logs ll
          LEFT JOIN customers c ON c.id = ll.customerId
          WHERE ll.customerId IS NOT NULL AND c.id IS NULL
        `,
      },
      {
        name: 'login_logs.adminId -> admins.id',
        sql: `
          SELECT COUNT(*) AS orphans
          FROM login_logs ll
          LEFT JOIN admins a ON a.id = ll.adminId
          WHERE ll.adminId IS NOT NULL AND a.id IS NULL
        `,
      },
      {
        name: 'registrations.customerId -> customers.id',
        sql: `
          SELECT COUNT(*) AS orphans
          FROM registrations r
          LEFT JOIN customers c ON c.id = r.customerId
          WHERE r.customerId IS NOT NULL AND c.id IS NULL
        `,
      },
    ];

    for (const check of checks) {
      const [[row]] = await sequelize.query(check.sql);
      console.log(`${check.name}: ${Number(row.orphans || 0)} orphan row(s)`);
    }

    process.exitCode = 0;
  } catch (error) {
    console.error('Relationship verification failed:', error.message);
    process.exitCode = 1;
  } finally {
    await sequelize.close();
  }
}

run();
