const { Pool } = require("pg");

// Determine initial ssl option from env
const sslDisabled = process.env.PGSSL_DISABLE === "true";
let pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: sslDisabled ? false : { rejectUnauthorized: false }
});

// Export a stable wrapper that delegates to the current pool
const db = {
  query: (...args) => pool.query(...args),
  connect: (...args) => pool.connect(...args),
  // handy for swapping pool in tests/diagnostics
  _setPool: (p) => { pool = p; }
};

async function ensureConnected() {
  try {
    const client = await pool.connect();
    try {
      const res = await client.query("SELECT NOW() AS connected_at");
      console.log(`✅ Connected to PostgreSQL at ${res.rows[0].connected_at}`);
    } finally {
      client.release();
    }
  } catch (err) {
    console.error("❌ PostgreSQL validation/query failed:", err.message || err);
    // If error indicates server does not support SSL, retry without SSL
    const msg = (err && err.message) ? String(err.message) : '';
    if (!sslDisabled && /does not support SSL|server does not support SSL|SSL routines/i.test(msg)) {
      console.warn("Detected SSL unsupported by server, retrying connection with ssl:false");
      try {
        const newPool = new Pool({ connectionString: process.env.DATABASE_URL, ssl: false });
        // swap in the new pool so db.query/connect use it
        db._setPool(newPool);
        const client2 = await newPool.connect();
        try {
          const res2 = await client2.query("SELECT NOW() AS connected_at");
          console.log(`✅ Connected to PostgreSQL (no SSL) at ${res2.rows[0].connected_at}`);
        } finally {
          client2.release();
        }
      } catch (err2) {
        console.error("❌ Retry without SSL failed:", err2.message || err2);
      }
    }
  }
}

// Immediately attempt to connect (best-effort) so we log connection status on startup
ensureConnected();

module.exports = db;
