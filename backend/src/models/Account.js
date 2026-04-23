const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Account = sequelize.define('Account', {
  id: {
    type: DataTypes.BIGINT.UNSIGNED,
    autoIncrement: true,
    primaryKey: true,
  },
  customerId: {
    type: DataTypes.BIGINT.UNSIGNED,
    allowNull: false,
  },
  accountNumber: {
    type: DataTypes.STRING,
    allowNull: false,
    unique: true,
    validate: {
      is: {
        args: /^\d{12}$/,
        msg: "Reenter 12 digit number",
      },
    },
  },
  accountPin: {
    type: DataTypes.STRING,
    allowNull: false,
    validate: {
      is: {
        args: /^\d{4}$/,
        msg: "Reenter 4 digit pin",
      },
    },
  },
  accountHolder: {
    type: DataTypes.STRING,
    allowNull: false,
    defaultValue: '',
  },
  accountType: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  requestedOpeningBalance: {
    type: DataTypes.DECIMAL(12, 2),
    allowNull: true,
  },
  approvedOpeningBalance: {
    type: DataTypes.DECIMAL(12, 2),
    allowNull: true,
  },
  approvedByAdminId: {
    type: DataTypes.BIGINT.UNSIGNED,
    allowNull: true,
  },
  approvedAt: {
    type: DataTypes.DATE,
    allowNull: true,
  },
  rejectionReason: {
    type: DataTypes.STRING,
    allowNull: true,
  },
  balance: {
    type: DataTypes.DECIMAL(12, 2),
    defaultValue: 0,
  },
  currency: {
    type: DataTypes.STRING,
    defaultValue: 'FJD',
  },
  status: {
    type: DataTypes.STRING,
    defaultValue: 'active',
  },
}, {
  tableName: 'accounts',
  timestamps: true,
});

module.exports = Account;
