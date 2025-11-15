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

app.use("/api/auth", authRoutes);
app.use("/api", tripRoutes);
app.use("/api", dataRoutes);
app.use("/api", bookingRoutes);
app.use("/api", promoRoutes);
app.use("/api", metaRoutes);

app.get("/", (req, res) => res.send("GoUTE API is running 🚍"));

const PORT = process.env.PORT;

app.listen(PORT, () => {
  console.log("Server started on port", PORT);
});
