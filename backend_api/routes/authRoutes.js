const express = require("express");
const router = express.Router();
const db = require("../db");
const bcrypt = require("bcrypt");
const sendEmail = require("../utils/sendEmail");
const SALT_ROUNDS = 10;

/* ============================================================
    1. SEND OTP (REGISTER)
   ============================================================ */

router.post("/send-otp", (req, res) => {
    const { email } = req.body;

    if (!email)
        return res.status(400).json({ message: "Thiếu email!" });

    const otp = Math.floor(100000 + Math.random() * 900000);

    db.query("SELECT * FROM users WHERE email=?", [email], (err, rows) => {
        if (err) return res.status(500).json({ message: err.message });

        if (rows.length === 0) {
            db.query(
                "INSERT INTO users (email, otp_code, status) VALUES (?, ?, 'pending')",
                [email, otp],
                (err2) => {
                    if (err2) return res.status(500).json({ message: err2.message });

                    res.json({ message: "OTP đã được gửi đến email!" });
                    sendEmail(email, "Mã OTP đăng ký", `Mã OTP của bạn là: ${otp}`)
                        .catch(emailErr => console.error("Gửi email thất bại:", emailErr));
                }
            );
        } else {
            const user = rows[0];
            if (user.status === "active") {
                return res.status(400).json({ message: "Email đã tồn tại!" });
            }
            db.query(
                "UPDATE users SET otp_code=?, status='pending' WHERE email=?",
                [otp, email],
                (err3) => {
                    if (err3) return res.status(500).json({ message: err3.message });

                    res.json({ message: "OTP đã được gửi đến email!" });
                    sendEmail(email, "Mã OTP đăng ký", `Mã OTP của bạn là: ${otp}`)
                        .catch(emailErr => console.error("Gửi email thất bại:", emailErr));
                }
            );
        }
    });
});

/* ============================================================
    2. VERIFY OTP
   ============================================================ */

router.post("/verify-otp", (req, res) => {
    const { email, otp } = req.body;

    db.query(
        "SELECT * FROM users WHERE email=? AND otp_code=?",
        [email, otp],
        (err, rows) => {
            if (err) return res.status(500).json({ success: false, message: err.message });
            if (rows.length === 0) return res.status(400).json({ success: false, message: "OTP sai!" });
            return res.json({ success: true, message: "OTP hợp lệ!" });
        }
    );
});


/* ============================================================
    3. FINISH REGISTER - ĐÃ SỬA LỖI
   ============================================================ */

router.post("/finish-register", async (req, res) => {
    const { name, phone, email, password } = req.body;

    if (!name || !phone || !email || !password)
        return res.status(400).json({ message: "Thiếu thông tin!" });

    // Validate phone number format (keep as string to preserve leading zero)
    const phoneStr = phone.toString().trim();
    if (!/^\d{9,11}$/.test(phoneStr)) {
        return res.status(400).json({ message: "Số điện thoại không hợp lệ." });
    }

    db.query("SELECT * FROM users WHERE sdt=? AND status='active'", [phoneStr], async (err, phoneRows) => {
        if (err) {
            console.error("Lỗi kiểm tra SĐT:", err);
            return res.status(500).json({ message: "Lỗi phía server." });
        }

        if (phoneRows.length > 0) {
            return res.status(400).json({ message: "Số điện thoại đã được sử dụng!" });
        }

        const hashed = await bcrypt.hash(password, SALT_ROUNDS);

        db.query(
            "UPDATE users SET name=?, sdt=?, password=?, otp_code=NULL, status='active' WHERE email=?",
            [name, phoneStr, hashed, email], // Store phone as string to keep leading zero
            (err2) => {
                if (err2) {
                    console.error("Lỗi cập nhật user:", err2);
                    return res.status(500).json({ message: "Lỗi khi tạo tài khoản." });
                }

                return res.json({ message: "Tạo tài khoản thành công!" });
            }
        );
    });
});

/* ============================================================
    4. LOGIN (Chỉ active)
   ============================================================ */

router.post("/login", (req, res) => {
    const { email, password } = req.body;

    db.query(
        "SELECT * FROM users WHERE email=?",
        [email],
        async (err, rows) => {
            if (err) return res.status(500).json({ message: err.message });

            if (rows.length === 0)
                return res.status(401).json({ message: "Sai email hoặc mật khẩu!" });

            const user = rows[0];

            if (user.status !== "active") {
                return res.status(403).json({ message: "Tài khoản chưa xác thực email!" });
            }

            const isMatch = await bcrypt.compare(password, user.password);
            if (!isMatch)
                return res.status(401).json({ message: "Sai email hoặc mật khẩu!" });

            delete user.password;

            return res.json({
                message: "Đăng nhập thành công!",
                user
            });
        }
    );
});

/* ============================================================
    5. FORGOT PASSWORD → SEND OTP
   ============================================================ */

router.post("/forgot-password", (req, res) => {
    const { email } = req.body;

    const otp = Math.floor(100000 + Math.random() * 900000);

    db.query(
        "UPDATE users SET otp_code=? WHERE email=? AND status='active'",
        [otp, email],
        (err, result) => {
            if (err) return res.status(500).json({ message: err.message });

            if (result.affectedRows === 0)
                return res.status(404).json({ message: "Email không tồn tại hoặc chưa active!" });

            res.json({ success: true, message: "OTP đã được gửi đến email!" });

            sendEmail(email, "Mã OTP đặt lại mật khẩu", `Mã OTP của bạn là: ${otp}`)
                .catch(emailErr => console.error("Gửi email quên mật khẩu thất bại:", emailErr));
        }
    );
});

/* ============================================================
    6. RESET PASSWORD
   ============================================================ */

router.post("/reset-password", async (req, res) => {
    const { email, newPassword } = req.body;

    const hashed = await bcrypt.hash(newPassword, SALT_ROUNDS);

    db.query(
        "UPDATE users SET password=?, otp_code=NULL WHERE email=? AND status='active'",
        [hashed, email],
        (err) => {
            if (err) return res.status(500).json({ message: err.message });

            res.json({ success: true, message: "Đặt lại mật khẩu thành công!" });
        }
    );
});

/* ============================================================
    7. GET USER INFO
   ============================================================ */

router.get("/user/:id", (req, res) => {
    const { id } = req.params;

    db.query(
        "SELECT id, name, email, sdt, status FROM users WHERE id=? AND status='active'",
        [id],
        (err, rows) => {
            if (err) return res.status(500).json({ message: err.message });
            if (rows.length === 0) return res.status(404).json({ message: "User not found" });
            res.json(rows[0]);
        }
    );
});

/* ============================================================
    8. CHANGE PASSWORD (for app - using email)
   ============================================================ */

router.post("/change-password", async (req, res) => {
    const { email, currentPassword, newPassword } = req.body;

    if (!email || !currentPassword || !newPassword) {
        return res.status(400).json({ success: false, message: "Thiếu thông tin!" });
    }

    db.query(
        "SELECT id, password FROM users WHERE email=? AND status='active'",
        [email],
        async (err, rows) => {
            if (err) return res.status(500).json({ success: false, message: err.message });
            if (rows.length === 0) return res.status(404).json({ success: false, message: "User not found" });

            const user = rows[0];
            const isMatch = await bcrypt.compare(currentPassword, user.password);

            if (!isMatch) {
                return res.status(401).json({ success: false, message: "Mật khẩu hiện tại không đúng!" });
            }

            const hashed = await bcrypt.hash(newPassword, SALT_ROUNDS);
            db.query(
                "UPDATE users SET password=? WHERE id=?",
                [hashed, user.id],
                (err2) => {
                    if (err2) return res.status(500).json({ success: false, message: err2.message });
                    res.json({ success: true, message: "Đổi mật khẩu thành công!" });
                }
            );
        }
    );
});

/* ============================================================
    9. UPDATE USER PROFILE (Only name and phone)
   ============================================================ */

router.put("/user/:id", async (req, res) => {
    const { id } = req.params;
    const { name, sdt } = req.body;

    if (!name && !sdt) {
        return res.status(400).json({ success: false, message: "Không có thông tin để cập nhật!" });
    }

    let updateFields = [];
    let updateValues = [];

    if (name) {
        updateFields.push("name=?");
        updateValues.push(name);
    }

    if (sdt) {
        // Validate phone number format (keep as string to preserve leading zero)
        const phoneStr = sdt.toString().trim();
        if (!/^\d{9,11}$/.test(phoneStr)) {
            return res.status(400).json({ success: false, message: "Số điện thoại không hợp lệ." });
        }

        // Check if phone is already used by another user
        const checkPhone = await new Promise((resolve, reject) => {
            db.query(
                "SELECT id FROM users WHERE sdt=? AND id!=? AND status='active'",
                [phoneStr, id],
                (err, rows) => {
                    if (err) reject(err);
                    else resolve(rows);
                }
            );
        });

        if (checkPhone.length > 0) {
            return res.status(400).json({ success: false, message: "Số điện thoại đã được sử dụng!" });
        }

        updateFields.push("sdt=?");
        updateValues.push(phoneStr);
    }

    updateValues.push(id);

    const sql = `UPDATE users SET ${updateFields.join(", ")} WHERE id=? AND status='active'`;

    db.query(sql, updateValues, (err, result) => {
        if (err) return res.status(500).json({ success: false, message: err.message });
        if (result.affectedRows === 0) {
            return res.status(404).json({ success: false, message: "User not found" });
        }
        res.json({ success: true, message: "Cập nhật thông tin thành công!" });
    });
});

module.exports = router;
