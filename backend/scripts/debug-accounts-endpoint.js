const http = require("http");
const path = require("path");
const jwt = require("jsonwebtoken");
require("dotenv").config({ path: path.resolve(__dirname, "../.env") });

const { Customer, sequelize } = require("../src/models");

const JWT_SECRET = process.env.JWT_SECRET || "bof-dev-secret-2026";

function makeRequest(method, path, data = null, token = null) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "localhost",
      port: 4000,
      path: path,
      method: method,
      headers: {
        "Content-Type": "application/json",
      },
    };

    if (token) {
      options.headers.Authorization = `Bearer ${token}`;
    }

    const req = http.request(options, (res) => {
      let responseData = "";
      res.on("data", (chunk) => {
        responseData += chunk;
      });
      res.on("end", () => {
        try {
          const parsed = JSON.parse(responseData);
          resolve({ status: res.statusCode, data: parsed });
        } catch (e) {
          resolve({ status: res.statusCode, data: responseData });
        }
      });
    });

    req.on("error", reject);

    if (data) {
      req.write(JSON.stringify(data));
    }
    req.end();
  });
}

function parseArgs(argv) {
  const args = { email: null, id: null };

  for (let i = 2; i < argv.length; i += 1) {
    const part = String(argv[i] || "");
    if (part === "--email") {
      args.email = argv[i + 1] || null;
      i += 1;
      continue;
    }
    if (part === "--id") {
      const parsedId = Number(argv[i + 1]);
      args.id = Number.isFinite(parsedId) ? parsedId : null;
      i += 1;
    }
  }

  return args;
}

async function resolveCustomer({ email, id }) {
  if (id) {
    return Customer.findByPk(id);
  }
  if (email) {
    return Customer.findOne({ where: { email } });
  }
  return null;
}

function buildTokenForCustomer(customer) {
  return jwt.sign(
    {
      userId: customer.id,
      customerId: customer.id,
      email: customer.email,
      fullName: customer.fullName,
      isAdmin: false,
    },
    JWT_SECRET,
    { expiresIn: "8h" }
  );
}

async function testDebugEndpoint() {
  console.log("Testing debug endpoint for account visibility...\n");

  try {
    const args = parseArgs(process.argv);
    const customer = await resolveCustomer(args);

    if (!customer) {
      console.error("Customer not found. Use --email or --id for an existing customer.");
      console.log("Example: node scripts/debug-accounts-endpoint.js --email peter@gmail.com");
      return;
    }

    const token = buildTokenForCustomer(customer);

    console.log("Step 1: Generated customer JWT");
    console.log(`  Customer ID: ${customer.id}`);
    console.log(`  Name: ${customer.fullName}`);
    console.log(`  Email: ${customer.email}`);
    console.log(`  Token: ${token.substring(0, 50)}...\n`);

    console.log("Step 2: Calling /api/debug/my-accounts");
    const debugRes = await makeRequest("GET", "/api/debug/my-accounts", null, token);
    console.log(`  Status: ${debugRes.status}`);
    console.log(JSON.stringify(debugRes.data, null, 2));

    console.log("\nStep 3: Calling /api/accounts");
    const accountsRes = await makeRequest("GET", "/api/accounts", null, token);
    console.log(`  Status: ${accountsRes.status}`);
    console.log(JSON.stringify(accountsRes.data, null, 2));
  } catch (error) {
    console.error("Error:", error.message);
  } finally {
    await sequelize.close();
  }
}

testDebugEndpoint();
