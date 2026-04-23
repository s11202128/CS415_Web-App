#!/usr/bin/env node

const path = require("path");
require("dotenv").config({ path: path.resolve(__dirname, "../.env") });

const { sequelize, Customer, Account } = require("../src/models");
const { Op, fn, col, where } = require("sequelize");

function normalizeName(value) {
  return String(value || "").trim().toLowerCase();
}

function parseArgs(argv) {
  return {
    apply: argv.includes("--apply"),
    fullName: (() => {
      const idx = argv.indexOf("--full-name");
      if (idx === -1) return null;
      return String(argv[idx + 1] || "").trim() || null;
    })(),
  };
}

async function loadDuplicateNameGroups(targetName) {
  const all = await Customer.findAll({ attributes: ["id", "fullName", "email", "mobile"], raw: true });
  const grouped = new Map();

  all.forEach((row) => {
    const key = normalizeName(row.fullName);
    if (!key) return;
    if (targetName && key !== normalizeName(targetName)) return;
    if (!grouped.has(key)) grouped.set(key, []);
    grouped.get(key).push(row);
  });

  return Array.from(grouped.entries())
    .filter(([, rows]) => rows.length > 1)
    .map(([nameKey, rows]) => ({ nameKey, rows }));
}

async function loadAccountCounts(customerIds) {
  const rows = await Account.findAll({
    attributes: ["customerId", [fn("COUNT", col("id")), "accountCount"]],
    where: { customerId: { [Op.in]: customerIds } },
    group: ["customerId"],
    raw: true,
  });

  const map = new Map();
  rows.forEach((row) => map.set(Number(row.customerId), Number(row.accountCount || 0)));
  return map;
}

function chooseCanonicalCustomer(rows, countMap) {
  return rows
    .map((row) => ({ row, count: Number(countMap.get(Number(row.id)) || 0) }))
    .sort((a, b) => {
      if (b.count !== a.count) return b.count - a.count;
      return Number(a.row.id) - Number(b.row.id);
    })[0];
}

async function buildRepairPlan(targetName) {
  const groups = await loadDuplicateNameGroups(targetName);
  const plan = [];

  for (const group of groups) {
    const ids = group.rows.map((r) => Number(r.id));
    const countMap = await loadAccountCounts(ids);
    const canonical = chooseCanonicalCustomer(group.rows, countMap);

    const donors = group.rows
      .map((r) => ({ ...r, accountCount: Number(countMap.get(Number(r.id)) || 0) }))
      .filter((r) => Number(r.id) !== Number(canonical.row.id) && r.accountCount > 0);

    if (donors.length === 0) {
      continue;
    }

    plan.push({
      normalizedFullName: group.nameKey,
      canonical: {
        id: Number(canonical.row.id),
        fullName: canonical.row.fullName,
        email: canonical.row.email,
        mobile: canonical.row.mobile,
        accountCount: canonical.count,
      },
      donors: donors.map((d) => ({
        id: Number(d.id),
        fullName: d.fullName,
        email: d.email,
        mobile: d.mobile,
        accountCount: d.accountCount,
      })),
    });
  }

  return plan;
}

async function applyPlan(plan) {
  const tx = await sequelize.transaction();
  try {
    for (const group of plan) {
      const donorIds = group.donors.map((d) => d.id);
      await Account.update(
        { customerId: group.canonical.id, accountHolder: group.canonical.fullName },
        { where: { customerId: { [Op.in]: donorIds } }, transaction: tx }
      );
    }

    await tx.commit();
  } catch (error) {
    await tx.rollback();
    throw error;
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const plan = await buildRepairPlan(args.fullName);

  console.log("Duplicate-customer repair plan");
  console.log(JSON.stringify(plan, null, 2));

  if (!args.apply) {
    console.log("\nDry-run only. Re-run with --apply to execute.");
    return;
  }

  if (plan.length === 0) {
    console.log("\nNothing to repair.");
    return;
  }

  await applyPlan(plan);
  console.log("\nRepair applied successfully.");
}

main()
  .catch((error) => {
    console.error("Repair failed:", error.message);
    process.exitCode = 1;
  })
  .finally(async () => {
    await sequelize.close();
  });
