// delete-user-data.js

// Load .env if present
const fs = require("fs");
const path = require("path");
const dotenvPath = path.resolve(__dirname, ".env");
if (fs.existsSync(dotenvPath)) {
  require("dotenv").config({ path: dotenvPath });
}

const { Customer, Account, Transaction, Bill, Investment, Loan, OtpVerification, Registration, NotificationLog, StatementRequest, LoginLog } = require("./src/models");

async function deleteUserData(email) {
  const customer = await Customer.findOne({ where: { email } });
  if (!customer) {
    console.log("No customer found for email:", email);
    return;
  }
  const customerId = customer.id;
  const accountNumbers = (await Account.findAll({ where: { customerId } })).map(a => a.accountNumber);

  await Promise.all([
    Transaction.destroy({ where: { accountNumber: accountNumbers } }),
    Account.destroy({ where: { customerId } }),
    Bill.destroy({ where: { customerId } }),
    Investment.destroy({ where: { customerId } }),
    Loan.destroy({ where: { customerId } }),
    OtpVerification.destroy({ where: { customerId } }),
    Registration.destroy({ where: { email } }),
    NotificationLog.destroy({ where: { userId: customerId } }),
    StatementRequest.destroy({ where: { customerId } }),
    LoginLog.destroy({ where: { userId: customerId } }),
    Customer.destroy({ where: { id: customerId } })
  ]);
  console.log("Deleted all data for:", email);
}

if (require.main === module) {
  const email = process.argv[2];
  if (!email) {
    console.error("Usage: node delete-user-data.js <email>");
    process.exit(1);
  }
  require("./src/config/database") // ensure DB connection
    .authenticate()
    .then(() => deleteUserData(email))
    .then(() => process.exit(0))
    .catch(e => { console.error(e); process.exit(1); });
}
