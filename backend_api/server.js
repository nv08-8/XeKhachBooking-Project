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
const PORT = process.env.PORT || 8080;

app.listen(PORT, "0.0.0.0", () => {
  console.log("Server started on port", PORT);
});

