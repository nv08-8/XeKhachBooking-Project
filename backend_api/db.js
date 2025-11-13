const mysql = require("mysql2");

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
});

pool.getConnection((err, conn) => {
  if (err) {
    console.error("❌ MySQL connect error:", err.message);
  } else {
    console.log("✅ Connected to MySQL!");
    conn.release();
  }
});

module.exports = pool;
