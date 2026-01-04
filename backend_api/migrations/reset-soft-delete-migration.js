/**
 * Reset migration tracking for soft delete feature
 * Run this if the migration failed
 */

const db = require("./db");

async function resetMigration() {
  try {
    console.log("Resetting soft delete migration...");

    // Delete the failed migration record so it runs again
    const result = await db.query(
      "DELETE FROM schema_migrations WHERE name = $1",
      ["20260104_add_soft_delete_support.sql"]
    );

    console.log(`✅ Removed ${result.rowCount} migration record(s)`);
    console.log("The migration will run again on next server restart");

  } catch (err) {
    console.error("❌ Error resetting migration:", err.message);
  } finally {
    process.exit(0);
  }
}

resetMigration();

