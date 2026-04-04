const { ActivityLog } = require('../models');

function normalizeActivityType(value) {
  const raw = String(value || 'ACTIVITY').trim().toUpperCase();
  return raw.replace(/[^A-Z0-9_]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 64) || 'ACTIVITY';
}

async function logCustomerActivity({ userId, activityType, description, status = 'success' }) {
  const numericUserId = Number(userId || 0);
  if (!Number.isFinite(numericUserId) || numericUserId <= 0) {
    return null;
  }

  try {
    return await ActivityLog.create({
      userId: numericUserId,
      activityType: normalizeActivityType(activityType),
      description: String(description || 'Activity recorded').slice(0, 255),
      status: String(status || 'success').slice(0, 32),
    });
  } catch (error) {
    // Never block core business actions due to logging failures.
    console.warn('Activity log write skipped:', error.message);
    return null;
  }
}

module.exports = {
  normalizeActivityType,
  logCustomerActivity,
};
