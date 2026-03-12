const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Account = sequelize.define('Account', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  customerId: {
    type: DataTypes.UUID,
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
  accountType: {
    type: DataTypes.STRING,
    allowNull: false,
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
