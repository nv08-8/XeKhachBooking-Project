require("dotenv").config();
const express = require("express");
const cors = require("cors");
const app = express();
const db = require("./db");

const authRoutes = require("./routes/authRoutes");
const tripRoutes = require("./routes/tripRoutes");
const dataRoutes = require("./routes/dataRoutes");
const bookingRoutes = require("./routes/bookingRoutes");
const promoRoutes = require("./routes/promoRoutes");
const metaRoutes = require("./routes/metaRoutes");

app.use(express.json());
app.use(cors());

// Middleware UTF-8
app.use((req, res, next) => {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    next();
});

app.use("/api/auth", authRoutes);
app.use("/api", tripRoutes);
app.use("/api", dataRoutes);
app.use("/api", bookingRoutes);
app.use("/api", promoRoutes);
app.use("/api", metaRoutes);

app.get("/", (req, res) => res.send("GoUTE API is running 🚍"));

// FIX PORT CHO RAILWAY
const PORT = process.env.PORT || 8080;

app.listen(PORT, () => {
    console.log("Server started on port", PORT);
});
