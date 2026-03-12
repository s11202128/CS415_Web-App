const sequelize = require('../config/database');
const Customer = require('./Customer');
const Account = require('./Account');
const Transaction = require('./Transaction');
const Bill = require('./Bill');
const Investment = require('./Investment');
const Loan = require('./Loan');
const OtpVerification = require('./OtpVerification');
const Registration = require('./Registration');

// Define associations
Customer.hasMany(Account, { foreignKey: 'customerId' });
Account.belongsTo(Customer, { foreignKey: 'customerId' });

Account.hasMany(Transaction, { foreignKey: 'accountId' });
Transaction.belongsTo(Account, { foreignKey: 'accountId' });

Customer.hasMany(Bill, { foreignKey: 'customerId' });
Bill.belongsTo(Customer, { foreignKey: 'customerId' });

Customer.hasMany(Investment, { foreignKey: 'customerId' });
Investment.belongsTo(Customer, { foreignKey: 'customerId' });

Customer.hasMany(Loan, { foreignKey: 'customerId' });
Loan.belongsTo(Customer, { foreignKey: 'customerId' });

Customer.hasMany(OtpVerification, { foreignKey: 'customerId' });
OtpVerification.belongsTo(Customer, { foreignKey: 'customerId' });

module.exports = {
  sequelize,
  Customer,
  Account,
  Transaction,
  Bill,
  Investment,
  Loan,
  OtpVerification,
  Registration,
};
