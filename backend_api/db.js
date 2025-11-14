const mysql = require("mysql2");

const pool = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME,
  port: process.env.DB_PORT,
  charset: 'utf8mb4' // Thêm dòng này để hỗ trợ tiếng Việt
});

pool.getConnection((err, conn) => {
  if (err) {
    console.error("❌ MySQL connect error:", err);
  } else {
    console.log("✅ Connected to Railway MySQL!");
    conn.release();
  }
});

module.exports = pool;
