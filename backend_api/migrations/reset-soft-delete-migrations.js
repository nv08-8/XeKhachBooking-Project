/**
 * Reset migrations for soft delete fix
 * Removes migration records so they run again on server startup
 */

const db = require("./db");

async function resetMigrations() {
  try {
    console.log("🔧 Resetting soft delete migrations...\n");

    const migrationsToReset = [
      "20260104_add_soft_delete_support.sql",
      "20260104_fix_users_status_check_constraint.sql"
    ];

    for (const migration of migrationsToReset) {
      const result = await db.query(
        "DELETE FROM schema_migrations WHERE name = $1",
        [migration]
      );
      if (result.rowCount > 0) {
        console.log(`✅ Removed: ${migration}`);
      } else {
        console.log(`⏭️  Not found: ${migration}`);
      }
    }

    console.log("\n✅ Migrations reset successfully!");
    console.log("   Restart the server to run migrations again.\n");

  } catch (err) {
    console.error("❌ Error resetting migrations:", err.message);
  } finally {
    process.exit(0);
  }
}

resetMigrations();

