// backend_api/routes/driversRoutes.js
const express = require("express");
const router = express.Router();
const db = require("../db");

// Middleware (simplified for now, you can add checkAdminRole later)
const checkAdminRole = (req, res, next) => next();

// GET all drivers
router.get("/", checkAdminRole, async (req, res) => {
  try {
    const { rows } = await db.query("SELECT * FROM drivers ORDER BY id ASC");
    res.json(rows);
  } catch (err) {
    console.error("Error fetching drivers:", err);
    res.status(500).json({ message: "Lỗi khi lấy danh sách tài xế" });
  }
});

// GET a single driver by ID
router.get("/:id", checkAdminRole, async (req, res) => {
  const { id } = req.params;
  try {
    const { rows } = await db.query("SELECT * FROM drivers WHERE id = $1", [id]);
    if (rows.length === 0) {
      return res.status(404).json({ message: "Không tìm thấy tài xế" });
    }
    res.json(rows[0]);
  } catch (err) {
    console.error(`Error fetching driver ${id}:`, err);
    res.status(500).json({ message: "Lỗi khi lấy thông tin tài xế" });
  }
});

// POST a new driver
router.post("/", checkAdminRole, async (req, res) => {
  const { name, phone, license_number } = req.body;
  if (!name || !phone || !license_number) {
    return res.status(400).json({ message: "Vui lòng nhập đủ thông tin" });
  }
  try {
    const { rows } = await db.query(
      "INSERT INTO drivers (name, phone, license_number) VALUES ($1, $2, $3) RETURNING *",
      [name, phone, license_number]
    );
    res.status(201).json(rows[0]);
  } catch (err) {
    console.error("Error creating driver:", err);
    res.status(500).json({ message: "Lỗi khi thêm tài xế" });
  }
});

// PUT (Update) a driver
router.put("/:id", checkAdminRole, async (req, res) => {
  const { id } = req.params;
  const { name, phone, license_number } = req.body;
  if (!name || !phone || !license_number) {
    return res.status(400).json({ message: "Vui lòng nhập đủ thông tin" });
  }
  try {
    const { rows } = await db.query(
      "UPDATE drivers SET name = $1, phone = $2, license_number = $3 WHERE id = $4 RETURNING *",
      [name, phone, license_number, id]
    );
    if (rows.length === 0) {
      return res.status(404).json({ message: "Không tìm thấy tài xế" });
    }
    res.json(rows[0]);
  } catch (err) {
    console.error(`Error updating driver ${id}:`, err);
    res.status(500).json({ message: "Lỗi khi cập nhật tài xế" });
  }
});


// DELETE a driver
router.delete("/:id", checkAdminRole, async (req, res) => {
  const { id } = req.params;
  try {
    const result = await db.query("DELETE FROM drivers WHERE id = $1 RETURNING *", [id]);
    if (result.rowCount === 0) {
      return res.status(404).json({ message: "Không tìm thấy tài xế" });
    }
    res.status(200).json({ message: "Xóa tài xế thành công" });
  } catch (err) {
    console.error("Error deleting driver:", err);
    res.status(500).json({ message: "Lỗi khi xóa tài xế" });
  }
});

module.exports = router;
