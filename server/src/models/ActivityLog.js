const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const ActivityLog = sequelize.define('ActivityLog', {
  id: {
    type: DataTypes.BIGINT.UNSIGNED,
    autoIncrement: true,
    primaryKey: true,
  },
  userId: {
    type: DataTypes.BIGINT.UNSIGNED,
    allowNull: false,
    field: 'user_id',
  },
  activityType: {
    type: DataTypes.STRING,
    allowNull: false,
    field: 'activity_type',
  },
  description: {
    type: DataTypes.STRING,
    allowNull: false,
  },
  status: {
    type: DataTypes.STRING,
    allowNull: false,
    defaultValue: 'success',
  },
}, {
  tableName: 'activity_logs',
  timestamps: true,
  createdAt: 'timestamp',
  updatedAt: false,
});

module.exports = ActivityLog;
