require("dotenv").config();
const express = require("express");
const cors = require("cors");
const app = express();
const db = require("./db");
const authRoutes = require("./routes/authRoutes");

app.use(express.json());
app.use(cors());

// Gắn router
app.use("/api/auth", authRoutes);

// Test endpoint
app.get("/", (req, res) => res.send("GoUTE API is running 🚍"));

// Server start
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🚀 Server started at http://localhost:${PORT}`);
});
