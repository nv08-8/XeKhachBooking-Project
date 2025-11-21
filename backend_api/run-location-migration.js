// Run migration to update location names to Vietnamese
require("dotenv").config();
const { Client } = require("pg");
const fs = require("fs");
const path = require("path");

async function runLocationMigration() {
  const client = new Client({
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.DATABASE_URL?.includes("localhost")
      ? false
      : { rejectUnauthorized: false },
  });

  try {
    await client.connect();
    console.log("✅ Connected to database");

    const migrationPath = path.join(
      __dirname,
      "migrations",
      "005_update_location_names_vietnamese.sql"
    );
    const sql = fs.readFileSync(migrationPath, "utf8");

    console.log("🚀 Running location name updates...");
    await client.query(sql);
    console.log("✅ Location names updated successfully!");

    // Show updated locations
    const result = await client.query(`
      SELECT DISTINCT origin FROM routes
      UNION
      SELECT DISTINCT destination FROM routes
      ORDER BY 1
    `);

    console.log("\n📍 Updated locations:");
    result.rows.forEach(row => console.log(`  - ${row.origin || row.destination}`));

  } catch (err) {
    console.error("❌ Migration failed:", err);
    process.exit(1);
  } finally {
    await client.end();
    console.log("\n✅ Migration complete!");
  }
}

runLocationMigration();

