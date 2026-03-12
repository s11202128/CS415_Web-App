const mysql = require("mysql2/promise");
const { DataTypes } = require("sequelize");
const sequelize = require("./config/database");
const {
  Customer,
  Account,
} = require("./models");

const DB_NAME = process.env.DB_NAME || "bof_banking_db";
const DB_USER = process.env.DB_USER || "root";
const DB_PASSWORD = process.env.DB_PASSWORD || "root";
const DB_HOST = process.env.DB_HOST || "localhost";
const DB_PORT = Number(process.env.DB_PORT || 3306);
const DB_SYNC_ALTER = String(process.env.DB_SYNC_ALTER || "false").toLowerCase() === "true";

const initializeDatabase = async () => {
  try {
    // First, create the database if it doesn't exist
    const connection = await mysql.createConnection({
      host: DB_HOST,
      port: DB_PORT,
      user: DB_USER,
      password: DB_PASSWORD,
    });

    await connection.query(`CREATE DATABASE IF NOT EXISTS ${DB_NAME}`);
    await connection.end();

    // Avoid repeated ALTER operations that can create excess indexes in MySQL.
    await sequelize.sync({ alter: DB_SYNC_ALTER });

    // Ensure customer admin/compliance fields exist even when DB_SYNC_ALTER is false.
    const queryInterface = sequelize.getQueryInterface();
    const customerColumns = await queryInterface.describeTable("customers");

    if (!customerColumns.tin) {
      await queryInterface.addColumn("customers", "tin", {
        type: DataTypes.STRING,
        allowNull: true,
        defaultValue: "",
      });
    }
    if (!customerColumns.residencyStatus) {
      await queryInterface.addColumn("customers", "residencyStatus", {
        type: DataTypes.STRING,
        allowNull: true,
        defaultValue: "resident",
      });
    }
    if (!customerColumns.identityVerified) {
      await queryInterface.addColumn("customers", "identityVerified", {
        type: DataTypes.BOOLEAN,
        allowNull: true,
        defaultValue: false,
      });
    }
    if (!customerColumns.registrationStatus) {
      await queryInterface.addColumn("customers", "registrationStatus", {
        type: DataTypes.STRING,
        allowNull: true,
        defaultValue: "approved",
      });
    }

    console.log("Database tables synchronized");

    // Check if database already has data
    const customerCount = await Customer.count();
    if (customerCount === 0) {
      // Seed initial data
      await seedDatabase();
    } else {
      console.log("Database already populated");
    }
  } catch (error) {
    console.error("Database initialization error:", error.message);
    throw error;
  }
};

const seedDatabase = async () => {
  try {
    // Create sample customers
    const customer1 = await Customer.create({
      fullName: "Litia Narikoso",
      email: "litia@example.com",
      mobile: "+679812345",
      password: "$2b$10$example_hashed_password_1",
      status: "active",
    });

    const customer2 = await Customer.create({
      fullName: "Aman Patel",
      email: "aman@example.com",
      mobile: "+679823456",
      password: "$2b$10$example_hashed_password_2",
      status: "active",
    });

    const customer3 = await Customer.create({
      fullName: "Mere Tikoisuva",
      email: "mere@example.com",
      mobile: "+679834567",
      password: "$2b$10$example_hashed_password_3",
      status: "active",
    });

    // Create accounts for each customer
    await Account.create({
      customerId: customer1.id,
      accountNumber: "235673489789",
      accountType: "Savings",
      balance: 15000,
      currency: "FJD",
      status: "active",
    });

    await Account.create({
      customerId: customer2.id,
      accountNumber: "918274635402",
      accountType: "Checking",
      balance: 25000,
      currency: "FJD",
      status: "active",
    });

    await Account.create({
      customerId: customer3.id,
      accountNumber: "603957214886",
      accountType: "Savings",
      balance: 18500,
      currency: "FJD",
      status: "active",
    });

    console.log("Database seeded with initial data");
  } catch (error) {
    console.error("Database seeding error:", error.message);
    throw error;
  }
};

module.exports = initializeDatabase;
