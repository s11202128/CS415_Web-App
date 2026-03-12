const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Customer = sequelize.define('Customer', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true,
  },
  fullName: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  email: {
    type: DataTypes.STRING,
    allowNull: false,
    unique: true,
  },
  mobile: {
    type: DataTypes.STRING,
    allowNull: false,
    unique: true,
  },
  password: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  status: {
    type: DataTypes.STRING,
    defaultValue: 'active',
  },
  tin: {
    type: DataTypes.STRING,
    defaultValue: '',
  },
  residencyStatus: {
    type: DataTypes.STRING,
    defaultValue: 'resident',
  },
  identityVerified: {
    type: DataTypes.BOOLEAN,
    defaultValue: false,
  },
  registrationStatus: {
    type: DataTypes.STRING,
    defaultValue: 'approved',
  },
}, {
  tableName: 'customers',
  timestamps: true,
});

module.exports = Customer;
