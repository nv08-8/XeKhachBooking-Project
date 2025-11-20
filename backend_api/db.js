const { Pool } = require("pg");

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.PGSSL_DISABLE === "true" ? false : { rejectUnauthorized: false }
});

pool.connect()
  .then(client => {
    return client
      .query("SELECT NOW() AS connected_at")
      .then(res => {
        console.log(`✅ Connected to Render PostgreSQL at ${res.rows[0].connected_at}`);
        client.release();
      })
      .catch(err => {
        client.release();
        console.error("❌ PostgreSQL validation query failed:", err.message);
      });
  })
  .catch(err => {
    console.error("❌ PostgreSQL connection error:", err.message);
  });

module.exports = pool;
