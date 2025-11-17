require('dotenv').config();
const mysql = require("mysql2");
const fs = require('fs');
const path = require('path');

const connection = mysql.createConnection({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  port: process.env.DB_PORT,
  multipleStatements: true
});

connection.connect((err) => {
  if (err) {
    console.error("❌ Database connection error:", err);
    process.exit(1);
  }
  console.log("✅ Connected to database");
});

// Allow dynamic migration file selection via env or CLI arg
const envFile = process.env.MIGRATION_FILE; // e.g. MIGRATION_FILE=add_bus_type_to_trips.sql
const argFile = process.argv[2]; // node run-migration.js add_bus_type_to_trips.sql
const fileName = envFile || argFile || 'add_bus_type_to_trips.sql';

const migrationPath = path.join(__dirname, 'migrations', fileName);
if (!fs.existsSync(migrationPath)) {
  console.error(`❌ Migration file not found: ${migrationPath}`);
  process.exit(1);
}

console.log(`🔄 Running migration: ${fileName}`);

const sql = fs.readFileSync(migrationPath, 'utf8');

connection.query(sql, (err, results) => {
  if (err) {
    console.error("❌ Migration failed:", err);
    connection.end();
    process.exit(1);
  }

  console.log("✅ Migration completed successfully!");
  console.log("📊 Results:", Array.isArray(results) ? results.length + ' statements executed' : results);

  // Simple verification depending on file
  if (fileName.includes('bus_type')) {
    connection.query("SELECT id, operator, bus_type FROM trips LIMIT 5", (vErr, rows) => {
      if (vErr) {
        console.error("❌ Verification query failed:", vErr);
      } else {
        console.log("\n📋 Sample trips after migration:");
        console.table(rows);
      }
      connection.end();
      console.log("\n✅ Migration script completed. Please restart your backend server if needed.");
    });
  } else if (fileName.includes('phone')) {
    connection.query("SELECT id, name, email, sdt FROM users WHERE status = 'active' LIMIT 5", (vErr, rows) => {
      if (vErr) {
        console.error("❌ Verification query failed:", vErr);
      } else {
        console.log("\n📋 Sample users after migration:");
        console.table(rows);
      }
      connection.end();
      console.log("\n✅ Migration script completed. Please restart your backend server if needed.");
    });
  } else {
    connection.end();
    console.log("\n✅ Migration script completed. Please restart your backend server if needed.");
  }
});
