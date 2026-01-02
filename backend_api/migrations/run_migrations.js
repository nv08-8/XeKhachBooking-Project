/**
 * Auto-run migrations on server startup
 * This ensures database schema is always up-to-date
 */

const db = require("./db");
const fs = require("fs");
const path = require("path");

async function runMigrations() {
  try {
    console.log("\n🔧 [MIGRATIONS] Checking and running pending migrations...");

    // List all migration files
    const migrationsDir = path.join(__dirname, "migrations");
    const files = fs.readdirSync(migrationsDir)
      .filter(f => f.endsWith(".sql"))
      .sort();

    if (files.length === 0) {
      console.log("✅ No migrations found");
      return;
    }

    // Create migrations tracking table if not exists
    await db.query(`
      CREATE TABLE IF NOT EXISTS schema_migrations (
        id SERIAL PRIMARY KEY,
        name VARCHAR(255) UNIQUE NOT NULL,
        executed_at TIMESTAMP DEFAULT NOW()
      )
    `);

    // Run each migration
    for (const file of files) {
      const migrationPath = path.join(migrationsDir, file);

      // Check if already executed
      const existResult = await db.query(
        "SELECT id FROM schema_migrations WHERE name = $1",
        [file]
      );

      if (existResult.rows.length > 0) {
        console.log(`⏭️  [MIGRATIONS] ${file} - already executed`);
        continue;
      }

      // Read and execute migration
      try {
        const sql = fs.readFileSync(migrationPath, "utf-8");

        // Remove SQL comments and empty lines
        const cleanedSql = sql
          .split("\n")
          .filter(line => !line.trim().startsWith("--") && line.trim() !== "")
          .join("\n");

        if (cleanedSql.trim()) {
          await db.query(cleanedSql);

          // Track execution
          await db.query(
            "INSERT INTO schema_migrations (name) VALUES ($1)",
            [file]
          );

          console.log(`✅ [MIGRATIONS] ${file} - executed successfully`);
        }
      } catch (err) {
        console.error(`❌ [MIGRATIONS] ${file} - failed:`, err.message);
        // Don't throw - continue with other migrations
      }
    }

    console.log("✅ [MIGRATIONS] All migrations completed\n");
  } catch (err) {
    console.error("❌ [MIGRATIONS] Failed to run migrations:", err.message);
  }
}

module.exports = { runMigrations };

