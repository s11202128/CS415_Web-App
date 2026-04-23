function errorHandler(err, req, res, next) {
  const statusCode = Number(err?.statusCode) || 500;
  const response = {
    error: {
      code: err?.code || "INTERNAL_SERVER_ERROR",
      message: err?.message || "Internal server error",
    },
  };

  if (err?.details) {
    response.error.details = err.details;
  }

  if (statusCode >= 500) {
    console.error(err);
  }

  res.status(statusCode).json(response);
}

module.exports = errorHandler;
