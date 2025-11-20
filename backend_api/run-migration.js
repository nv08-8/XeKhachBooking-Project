require("dotenv").config();
const { Pool } = require("pg");
const fs = require("fs");
const path = require("path");

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false }
});

async function run() {
  try {
    await pool.query("SELECT NOW() AS connected_at");
    console.log("✅ Connected to PostgreSQL");
  } catch (err) {
    console.error("❌ Database connection error:", err);
    process.exit(1);
  }

  const envFile = process.env.MIGRATION_FILE;
  const argFile = process.argv[2];
  const fileName = envFile || argFile || "add_bus_type_to_trips.sql";

  const migrationPath = path.join(__dirname, "migrations", fileName);
  if (!fs.existsSync(migrationPath)) {
    console.error(`❌ Migration file not found: ${migrationPath}`);
    process.exit(1);
  }

  console.log(`🔄 Running migration: ${fileName}`);

  const sql = fs.readFileSync(migrationPath, "utf8");
  const statements = sql
    .split(/;\s*$/m)
    .map(stmt => stmt.trim())
    .filter(Boolean);

  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    for (const statement of statements) {
      await client.query(statement);
    }
    await client.query("COMMIT");
    console.log("✅ Migration completed successfully!");
    console.log(`📊 Statements executed: ${statements.length}`);
  } catch (err) {
    await client.query("ROLLBACK");
    console.error("❌ Migration failed:", err);
    process.exitCode = 1;
  } finally {
    client.release();
  }

  try {
    if (fileName.includes("bus_type")) {
      const { rows } = await pool.query("SELECT id, operator, bus_type FROM trips LIMIT 5");
      console.table(rows);
    } else if (fileName.includes("phone")) {
      const { rows } = await pool.query("SELECT id, name, email, sdt FROM users WHERE status = 'active' LIMIT 5");
      console.table(rows);
    }
  } catch (err) {
    console.error("❌ Verification query failed:", err);
  }

  await pool.end();
  console.log("\n✅ Migration script completed. Please restart your backend server if needed.");
}

run().catch(err => {
  console.error("❌ Unexpected migration error:", err);
  process.exit(1);
});
